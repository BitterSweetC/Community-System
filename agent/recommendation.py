"""
Hybrid Recommendation Service: SVD Collaborative Filtering + Semantic Content-Based.
Fixes: cold-start handling, base_score bug, proper score normalization.
"""
import os
import numpy as np
import pandas as pd
from datetime import datetime
from sqlalchemy import create_engine, text
from sklearn.decomposition import TruncatedSVD
from sklearn.preprocessing import MinMaxScaler
from langchain_community.embeddings import SentenceTransformerEmbeddings

EMBEDDING_MODEL = "all-MiniLM-L6-v2"

# Hybrid weights
W_CF = 0.6
W_CB = 0.4


class RecommendationService:
    def __init__(self):
        self.engine = create_engine(
            os.getenv("DB_URL", "mysql+pymysql://root:password@localhost:3306/community_db")
        )
        print("Loading embedding model...")
        self.embeddings = SentenceTransformerEmbeddings(model_name=EMBEDDING_MODEL)
        self._club_emb_cache: dict[int, np.ndarray] = {}

    # ── Data Loading ──────────────────────────────────────────────────────────

    def _load_interactions(self) -> pd.DataFrame:
        """
        Build interaction matrix with time-decayed scores.
        Sources:
          - t_member join  → base 5.0
          - t_activity_signup → base 2.0
          - t_activity_attendance → base 3.0  (stronger signal than just signup)
        """
        query = text("""
            SELECT user_id, club_id, 5.0 AS base_score, join_at AS action_time
            FROM t_member
            UNION ALL
            SELECT s.user_id, a.club_id, 2.0, s.created_at
            FROM t_activity_signup s
            JOIN t_activity a ON a.id = s.activity_id
            UNION ALL
            SELECT att.user_id, a.club_id, 3.0, att.sign_time
            FROM t_activity_attendance att
            JOIN t_activity a ON a.id = att.activity_id
        """)
        with self.engine.connect() as conn:
            df = pd.read_sql(query, conn)

        if df.empty:
            return df

        df["score"] = df.apply(
            lambda r: self._time_decay(r["base_score"], r["action_time"]), axis=1
        )
        # Aggregate: sum scores per (user, club)
        df = df.groupby(["user_id", "club_id"])["score"].sum().reset_index()
        return df

    def _load_clubs(self) -> pd.DataFrame:
        query = text("""
            SELECT c.id, c.name, c.category, c.description,
                   c.visit_count,
                   GROUP_CONCAT(ct.tag SEPARATOR ' ') AS tags
            FROM t_club c
            LEFT JOIN t_club_tag ct ON ct.club_id = c.id
            WHERE c.status = 'ACTIVE'
            GROUP BY c.id
        """)
        with self.engine.connect() as conn:
            df = pd.read_sql(query, conn)
        df.fillna("", inplace=True)
        df["combined_text"] = (
            df["name"] + " " + df["category"] + " " +
            df["description"].str[:200] + " " + df["tags"]
        )
        return df

    @staticmethod
    def _time_decay(base: float, ts, half_life_days: int = 30) -> float:
        if pd.isna(ts):
            return base
        try:
            ts = pd.to_datetime(ts)
        except Exception:
            return base
        days = max((datetime.now() - ts.to_pydatetime().replace(tzinfo=None)).days, 0)
        return base * (0.5 ** (days / half_life_days))

    # ── Embeddings ────────────────────────────────────────────────────────────

    def _ensure_embeddings(self, df_clubs: pd.DataFrame):
        missing = df_clubs[~df_clubs["id"].isin(self._club_emb_cache)]
        if missing.empty:
            return
        vecs = self.embeddings.embed_documents(missing["combined_text"].tolist())
        for cid, vec in zip(missing["id"], vecs):
            self._club_emb_cache[int(cid)] = np.array(vec)

    # ── Collaborative Filtering (SVD) ─────────────────────────────────────────

    def _cf_scores(self, user_id: int, df: pd.DataFrame,
                   candidates: list[int]) -> dict[int, float]:
        if df.empty or user_id not in df["user_id"].values:
            return {}

        matrix = df.pivot(index="user_id", columns="club_id", values="score").fillna(0)
        n_users, n_items = matrix.shape
        n_components = min(max(1, min(n_users, n_items) - 1), 20)

        svd = TruncatedSVD(n_components=n_components, random_state=42)
        reduced = svd.fit_transform(matrix)
        reconstructed = pd.DataFrame(
            svd.inverse_transform(reduced),
            index=matrix.index,
            columns=matrix.columns,
        )

        user_row = reconstructed.loc[user_id]
        return {
            cid: float(user_row[cid])
            for cid in candidates
            if cid in user_row.index
        }

    # ── Content-Based (Semantic) ───────────────────────────────────────────────

    def _cb_scores(self, user_id: int, df: pd.DataFrame,
                   candidates: list[int]) -> dict[int, float]:
        user_hist = df[df["user_id"] == user_id]
        if user_hist.empty:
            return {}

        # Build user profile vector: weighted average of interacted club embeddings
        dim = len(next(iter(self._club_emb_cache.values())))
        profile = np.zeros(dim)
        total_w = 0.0
        for _, row in user_hist.iterrows():
            cid = int(row["club_id"])
            if cid in self._club_emb_cache:
                w = row["score"]
                profile += self._club_emb_cache[cid] * w
                total_w += w

        if total_w == 0:
            return {}
        profile /= total_w

        scores = {}
        norm_p = np.linalg.norm(profile)
        for cid in candidates:
            if cid not in self._club_emb_cache:
                continue
            vec = self._club_emb_cache[cid]
            norm_v = np.linalg.norm(vec)
            if norm_p == 0 or norm_v == 0:
                scores[cid] = 0.0
            else:
                scores[cid] = float(np.dot(profile, vec) / (norm_p * norm_v))
        return scores

    # ── Cold Start ────────────────────────────────────────────────────────────

    def _cold_start(self, df_clubs: pd.DataFrame, top_k: int) -> list[int]:
        """Return top clubs by visit_count for new users."""
        top = df_clubs.nlargest(top_k, "visit_count")
        return top["id"].astype(int).tolist()

    # ── Normalise ─────────────────────────────────────────────────────────────

    @staticmethod
    def _normalise(scores: dict[int, float]) -> dict[int, float]:
        if not scores:
            return scores
        vals = np.array(list(scores.values())).reshape(-1, 1)
        scaled = MinMaxScaler().fit_transform(vals).flatten()
        return dict(zip(scores.keys(), scaled))

    # ── Public API ────────────────────────────────────────────────────────────

    def get_recommendations(self, user_id: int, top_k: int = 5) -> list[int]:
        try:
            df_interactions = self._load_interactions()
            df_clubs = self._load_clubs()

            if df_clubs.empty:
                return []

            # Clubs the user has already joined (highest-signal interactions)
            if not df_interactions.empty:
                joined = set(
                    df_interactions[
                        (df_interactions["user_id"] == user_id) &
                        (df_interactions["score"] >= 4.0)
                    ]["club_id"].astype(int).tolist()
                )
            else:
                joined = set()

            candidates = [
                int(cid) for cid in df_clubs["id"].tolist()
                if int(cid) not in joined
            ]

            if not candidates:
                return []

            # Cold start: user has no interaction history
            has_history = (
                not df_interactions.empty and
                user_id in df_interactions["user_id"].values
            )
            if not has_history:
                return self._cold_start(df_clubs, top_k)

            # Precompute embeddings for all clubs
            self._ensure_embeddings(df_clubs)

            # Compute and normalise both scores
            cf_raw = self._cf_scores(user_id, df_interactions, candidates)
            cb_raw = self._cb_scores(user_id, df_interactions, candidates)

            cf_norm = self._normalise(cf_raw)
            cb_norm = self._normalise(cb_raw)

            # Hybrid fusion
            final: list[tuple[int, float]] = []
            for cid in candidates:
                s = W_CF * cf_norm.get(cid, 0.0) + W_CB * cb_norm.get(cid, 0.0)
                final.append((cid, s))

            final.sort(key=lambda x: x[1], reverse=True)
            return [cid for cid, _ in final[:top_k]]

        except Exception as e:
            import traceback
            print(f"Recommendation error: {e}")
            traceback.print_exc()
            return []


recommendation_service = RecommendationService()
