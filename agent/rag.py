"""
RAG service that combines realtime business facts with local knowledge retrieval.
"""
from __future__ import annotations

from dataclasses import dataclass, field
import json
import os
import re
import shutil
from pathlib import Path
from typing import Any, Optional

from langchain_community.vectorstores import Chroma
from langchain_core.documents import Document
from langchain_core.messages import AIMessage, HumanMessage
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain_text_splitters import RecursiveCharacterTextSplitter

from knowledge_store import (
    DATA_DIR,
    delete_markdown_document,
    load_documents_from_dir,
    serialise_metadata_value,
    upsert_markdown_document,
)
from local_embeddings import create_embeddings
from tools import (
    check_recruit_status,
    get_activity_details,
    get_club_activities,
    get_club_member_count,
    get_finance_summary,
    get_recent_notices,
    get_recruit_progress,
    get_resource_status,
    get_upcoming_activities,
    search_clubs,
)

PERSIST_DIRECTORY = Path(__file__).resolve().parent / "chroma_db"
EMBEDDING_MODEL = "all-MiniLM-L6-v2"
MAX_HISTORY = 10
ANSWER_HISTORY_MESSAGES = 6
RETRIEVAL_K = 4
RETRIEVAL_CANDIDATE_K = 12
FILLER_PREFIXES = (
    "请问",
    "麻烦问下",
    "帮我",
    "我想",
    "我想问下",
    "我想了解",
    "查询",
    "查看",
)


@dataclass
class UserAccessContext:
    authenticated: bool = False
    user_id: Optional[int] = None
    username: str = ""
    role_codes: set[str] = field(default_factory=set)
    managed_club_ids: set[str] = field(default_factory=set)
    managed_club_names: set[str] = field(default_factory=set)

    @staticmethod
    def _to_bool(value: Any) -> bool:
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            return value.strip().lower() in {"1", "true", "yes", "y", "on"}
        return bool(value)

    @staticmethod
    def _to_iterable(value: Any) -> list[Any]:
        if value is None:
            return []
        if isinstance(value, (list, tuple, set, frozenset)):
            return list(value)
        if isinstance(value, str):
            text = value.strip()
            if not text:
                return []
            if text.startswith("["):
                try:
                    parsed = json.loads(text)
                except Exception:
                    parsed = None
                if isinstance(parsed, list):
                    return parsed
            return [item.strip() for item in text.split(",") if item.strip()]
        return [value]

    @staticmethod
    def _normalise_token(value: Any) -> str:
        return re.sub(r"\s+", "", str(value).strip())

    @classmethod
    def _normalise_name(cls, value: Any) -> str:
        return cls._normalise_token(value).lower()

    @classmethod
    def from_payload(cls, payload: Optional[dict[str, Any]]) -> "UserAccessContext":
        data = payload or {}
        role_codes = {
            cls._normalise_token(item).upper()
            for item in cls._to_iterable(data.get("role_codes"))
            if cls._normalise_token(item)
        }
        managed_club_ids = {
            cls._normalise_token(item)
            for item in cls._to_iterable(data.get("managed_club_ids"))
            if cls._normalise_token(item)
        }
        managed_club_names = {
            cls._normalise_name(item)
            for item in cls._to_iterable(data.get("managed_club_names"))
            if cls._normalise_name(item)
        }

        user_id = data.get("user_id")
        try:
            user_id = int(user_id) if user_id is not None else None
        except Exception:
            user_id = None

        return cls(
            authenticated=cls._to_bool(data.get("authenticated")),
            user_id=user_id,
            username=str(data.get("username") or "").strip(),
            role_codes=role_codes,
            managed_club_ids=managed_club_ids,
            managed_club_names=managed_club_names,
        )

    @property
    def is_admin(self) -> bool:
        return "ADMIN" in self.role_codes

    def can_manage_club_id(self, club_id: Any) -> bool:
        if self.is_admin:
            return True
        club_key = self._normalise_token(club_id)
        return bool(club_key) and club_key in self.managed_club_ids

    def can_manage_club_name(self, club_name: Any) -> bool:
        if self.is_admin:
            return True
        target = self._normalise_name(club_name)
        if not target:
            return False
        return any(
            target == managed_name or target in managed_name or managed_name in target
            for managed_name in self.managed_club_names
        )

    def can_access_club(self, club_id: Any = None, club_name: Any = None) -> bool:
        return self.can_manage_club_id(club_id) or self.can_manage_club_name(club_name)


class RAGService:
    def __init__(self):
        self.vectorstore: Optional[Chroma] = None
        self.retriever = None
        self._embeddings = None
        self._sessions: dict[str, list] = {}

        api_key = os.getenv("DEEPSEEK_API_KEY")
        if not api_key:
            raise ValueError("DEEPSEEK_API_KEY is not set.")

        self.llm = ChatOpenAI(
            model="deepseek-chat",
            openai_api_key=api_key,
            openai_api_base=os.getenv("DEEPSEEK_API_URL", "https://api.deepseek.com"),
            temperature=0.2,
        )

    @staticmethod
    def _contains_any(text: str, keywords: list[str]) -> bool:
        return any(keyword in text for keyword in keywords)

    def _strip_filler(self, text: str) -> str:
        cleaned = text.strip()
        for prefix in FILLER_PREFIXES:
            if cleaned.startswith(prefix):
                cleaned = cleaned[len(prefix):].strip()
                break
        cleaned = re.sub(r"\s+", " ", cleaned)
        return cleaned.strip(" ，。！？?!.、；;：:")

    def _extract_club_name(self, question: str) -> str:
        matches = re.findall(
            r"([\u4e00-\u9fa5A-Za-z0-9#·\-\s]{2,30}(?:协会|社团|俱乐部|实验室|学院|队|社))",
            question,
        )
        if not matches:
            return ""
        return max((self._strip_filler(item) for item in matches), key=len, default="")

    def _extract_resource_keyword(self, question: str) -> str:
        match = re.search(
            r"([\u4e00-\u9fa5A-Za-z0-9#·\-\s]*(?:多媒体教室|报告厅|会议室|篮球场|排练厅|教室|场地|器材|资源)[\u4e00-\u9fa5A-Za-z0-9#·\-\s]*)",
            question,
        )
        if match:
            return self._strip_filler(match.group(1))
        return self._strip_filler(question)

    def _extract_activity_keyword(self, question: str) -> str:
        match = re.search(r"([A-Za-z0-9\u4e00-\u9fa5#·\-]{1,30})(?:相关)?的?活动", question)
        if match:
            return self._strip_filler(match.group(1))
        match = re.search(r"(?:关于|有关|查询|查看)([A-Za-z0-9\u4e00-\u9fa5#·\-]{1,30})", question)
        if match:
            return self._strip_filler(match.group(1))
        return ""

    def _extract_notice_keyword(self, question: str) -> str:
        club_name = self._extract_club_name(question)
        if club_name:
            return club_name
        match = re.search(r"(?:关于|有关)([A-Za-z0-9\u4e00-\u9fa5#·\-]{1,30})", question)
        if match:
            return self._strip_filler(match.group(1))
        return ""

    @staticmethod
    def _tool_text(result: Any) -> str:
        if isinstance(result, str):
            return result.strip()
        return str(result).strip()

    def _invoke_tool(self, tool_obj: Any, payload: dict[str, Any]) -> str:
        try:
            return self._tool_text(tool_obj.invoke(payload))
        except Exception as exc:
            return f"查询实时数据失败：{exc}"

    def _reset_vectorstore(self):
        self.vectorstore = None
        self.retriever = None
        if PERSIST_DIRECTORY.exists():
            shutil.rmtree(PERSIST_DIRECTORY)
        PERSIST_DIRECTORY.mkdir(parents=True, exist_ok=True)

    def initialize_knowledge_base(self):
        print("Initializing knowledge base...")
        documents = load_documents_from_dir(DATA_DIR)
        splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        chunks = splitter.split_documents(documents) if documents else []

        try:
            embeddings, provider = create_embeddings(EMBEDDING_MODEL)
            self._embeddings = embeddings
            self._reset_vectorstore()
            if chunks:
                self.vectorstore = Chroma.from_documents(
                    chunks,
                    embeddings,
                    persist_directory=str(PERSIST_DIRECTORY),
                )
            else:
                self.vectorstore = Chroma(
                    embedding_function=embeddings,
                    persist_directory=str(PERSIST_DIRECTORY),
                )

            if hasattr(self.vectorstore, "persist"):
                self.vectorstore.persist()
            self.retriever = self.vectorstore.as_retriever(search_kwargs={"k": RETRIEVAL_K})
            print(f"Knowledge base ready. Chunks: {len(chunks)}, provider: {provider}")
        except Exception as exc:
            self.vectorstore = None
            self.retriever = None
            print(f"Knowledge base initialization degraded: {exc}")

    def upsert_document(
        self,
        content: str,
        source: str,
        title: Optional[str] = None,
        metadata: Optional[dict[str, Any]] = None,
    ) -> int:
        if not source or not source.strip():
            raise ValueError("source is required")
        if not content or not content.strip():
            raise ValueError("content is required")

        payload = metadata or {}
        upsert_markdown_document(
            content=content,
            source=source.strip(),
            title=title,
            metadata=payload,
            data_dir=DATA_DIR,
        )

        doc = Document(page_content=content.strip(), metadata={"source": source.strip(), **{
            str(key): serialise_metadata_value(value)
            for key, value in payload.items()
            if value is not None
        }})
        if title:
            doc.metadata["title"] = title.strip()
        splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        chunk_count = len(splitter.split_documents([doc]))
        self.initialize_knowledge_base()
        return chunk_count

    def add_document(
        self,
        content: str,
        source: str,
        title: Optional[str] = None,
        metadata: Optional[dict[str, Any]] = None,
    ) -> int:
        return self.upsert_document(content=content, source=source, title=title, metadata=metadata)

    def delete_document(self, source: str, metadata: Optional[dict[str, Any]] = None) -> int:
        if not source or not source.strip():
            raise ValueError("source is required")

        deleted = delete_markdown_document(source=source, metadata=metadata, data_dir=DATA_DIR)
        if deleted:
            self.initialize_knowledge_base()
        return deleted

    def _get_history(self, session_id: str) -> list:
        return self._sessions.get(session_id, [])

    def _append_history(self, session_id: str, human: str, ai: str):
        history = self._sessions.setdefault(session_id, [])
        history.append(HumanMessage(content=human))
        history.append(AIMessage(content=ai))
        if len(history) > MAX_HISTORY * 2:
            self._sessions[session_id] = history[-(MAX_HISTORY * 2) :]

    def clear_session(self, session_id: str):
        self._sessions.pop(session_id, None)

    def get_knowledge_document_count(self) -> int:
        return len(load_documents_from_dir(DATA_DIR))

    @staticmethod
    def _content_to_text(content: Any) -> str:
        if content is None:
            return ""
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                elif isinstance(item, dict):
                    text = item.get("text") or item.get("content")
                    if text:
                        parts.append(str(text))
            return "\n".join(parts).strip()
        return str(content)

    def _format_recent_history(self, session_id: str) -> str:
        history = self._get_history(session_id)[-ANSWER_HISTORY_MESSAGES:]
        if not history:
            return "无"

        lines = []
        for message in history:
            role = "用户" if isinstance(message, HumanMessage) else "助手"
            text = self._content_to_text(getattr(message, "content", ""))
            if text:
                lines.append(f"{role}: {text}")
        return "\n".join(lines) if lines else "无"

    def _plan_tool_calls(self, question: str) -> list[dict[str, Any]]:
        text = self._strip_filler(question)
        if not text:
            return []

        club_name = self._extract_club_name(text)
        plans: list[dict[str, Any]] = []

        def add_plan(
            name: str,
            tool_obj: Any,
            payload: dict[str, Any],
            access_requirement: str = "public",
            denial_message: str = "",
        ):
            signature = json.dumps({"name": name, "payload": payload}, ensure_ascii=False, sort_keys=True)
            if any(item["signature"] == signature for item in plans):
                return
            plans.append({
                "name": name,
                "tool": tool_obj,
                "payload": payload,
                "access_requirement": access_requirement,
                "denial_message": denial_message.strip(),
                "club_id": payload.get("club_id"),
                "club_name": payload.get("club_name", ""),
                "signature": signature,
            })

        if self._contains_any(text, ["公告", "通知", "通告"]):
            add_plan("公告查询", get_recent_notices, {"keyword": self._extract_notice_keyword(text)})

        if self._contains_any(text, ["资源", "教室", "场地", "器材", "会议室", "报告厅", "篮球场", "排练厅", "预约"]):
            keyword = self._extract_resource_keyword(text)
            if keyword:
                add_plan("资源状态", get_resource_status, {"keyword": keyword})

        if self._contains_any(text, ["财务", "经费", "收入", "支出", "报销", "结余"]):
            if club_name:
                add_plan(
                    "财务概况",
                    get_finance_summary,
                    {"club_name": club_name},
                    access_requirement="club_admin",
                    denial_message="财务信息仅限该社团管理员或系统管理员查看。",
                )

        if self._contains_any(text, ["招新", "报名", "申请", "初审", "终审", "名额", "审核"]):
            if club_name:
                if self._contains_any(text, ["进度", "多少申请", "多少通过", "审核", "初审", "终审", "通过", "拒绝"]):
                    add_plan(
                        "招新进度",
                        get_recruit_progress,
                        {"club_name": club_name},
                        access_requirement="club_admin",
                        denial_message="招新审核进度仅限该社团管理员或系统管理员查看。",
                    )
                else:
                    add_plan("招新状态", check_recruit_status, {"club_name": club_name})

        if self._contains_any(text, ["活动", "讲座", "比赛", "沙龙", "分享"]):
            if club_name:
                add_plan("社团活动", get_club_activities, {"club_name": club_name})
            else:
                keyword = self._extract_activity_keyword(text)
                if keyword:
                    add_plan("活动详情", get_activity_details, {"keyword": keyword})
                if self._contains_any(text, ["最近", "近期", "热门", "本周", "这周", "Upcoming", "upcoming"]) or not keyword:
                    add_plan("近期活动", get_upcoming_activities, {"days": 7})

        if self._contains_any(text, ["成员数", "成员数量", "多少成员", "几个人"]) and club_name:
            add_plan("成员数量", get_club_member_count, {"club_name": club_name})

        if self._contains_any(text, ["社团", "协会", "俱乐部", "实验室"]) and not self._contains_any(
            text, ["公告", "通知", "活动", "招新", "财务", "成员"]
        ):
            keyword = club_name or text
            add_plan("社团搜索", search_clubs, {"keyword": keyword})

        return plans

    @staticmethod
    def _normalise_visibility(value: Any) -> str:
        visibility = str(value or "").strip().lower()
        return visibility or "public"

    def _is_allowed(
        self,
        requirement: str,
        access_context: UserAccessContext,
        club_id: Any = None,
        club_name: Any = None,
    ) -> bool:
        if requirement == "public":
            return True
        if requirement == "authenticated":
            return access_context.authenticated
        if requirement == "admin":
            return access_context.is_admin
        if requirement == "club_admin":
            return access_context.can_access_club(club_id=club_id, club_name=club_name)
        return False

    def _collect_realtime_context(
        self,
        question: str,
        access_context: UserAccessContext,
    ) -> tuple[list[dict[str, str]], list[str]]:
        sections: list[dict[str, str]] = []
        denied_messages: list[str] = []
        for plan in self._plan_tool_calls(question):
            requirement = self._normalise_visibility(plan.get("access_requirement"))
            if not self._is_allowed(
                requirement,
                access_context,
                club_id=plan.get("club_id"),
                club_name=plan.get("club_name"),
            ):
                denied_message = (plan.get("denial_message") or "当前无权限查看该信息。").strip()
                if denied_message not in denied_messages:
                    denied_messages.append(denied_message)
                continue
            text = self._invoke_tool(plan["tool"], plan["payload"])
            if not text:
                continue
            sections.append({"name": plan["name"], "content": text})
        return sections, denied_messages

    def _can_access_document(self, document: Document, access_context: UserAccessContext) -> bool:
        metadata = document.metadata or {}
        visibility = self._normalise_visibility(metadata.get("visibility"))
        return self._is_allowed(
            visibility,
            access_context,
            club_id=metadata.get("club_id"),
            club_name=metadata.get("club_name"),
        )

    def _retrieve_documents(
        self,
        question: str,
        access_context: UserAccessContext,
        top_k: int = RETRIEVAL_K,
    ) -> list[Document]:
        if not self.vectorstore:
            return []

        query = self._strip_filler(question)
        club_name = self._extract_club_name(query)
        if club_name:
            query = f"{query}\n相关社团：{club_name}"

        try:
            candidates = self.vectorstore.similarity_search(query, k=max(top_k * 3, RETRIEVAL_CANDIDATE_K))
        except Exception as exc:
            print(f"Knowledge retrieval failed: {exc}")
            return []
        return [doc for doc in candidates if self._can_access_document(doc, access_context)][:top_k]

    @staticmethod
    def _format_realtime_context(sections: list[dict[str, str]]) -> str:
        if not sections:
            return "无实时业务数据。"

        lines = []
        for index, section in enumerate(sections, start=1):
            lines.append(f"[实时数据 {index}] {section['name']}")
            lines.append(section["content"])
        return "\n".join(lines)

    @staticmethod
    def _format_knowledge_context(documents: list[Document]) -> str:
        if not documents:
            return "无本地知识片段。"

        lines = []
        for index, doc in enumerate(documents, start=1):
            metadata = doc.metadata or {}
            title = metadata.get("title") or metadata.get("source") or f"文档 {index}"
            source = metadata.get("source") or metadata.get("source_path") or "未知来源"
            snippet = doc.page_content.strip().replace("\n", " ")
            lines.append(f"[知识片段 {index}] 标题: {title}")
            lines.append(f"来源: {source}")
            lines.append(f"内容: {snippet[:600]}")
        return "\n".join(lines)

    def _build_answer_prompt(self) -> ChatPromptTemplate:
        template = """
你是校园社团管理系统的智能助手。请始终使用中文回答。

回答规则：
1. 涉及人数、状态、时间、名额、余额、预约占用等“当前事实”，必须优先依据实时业务数据。
2. 本地知识库只用于补充背景、规则、流程和说明；如果与实时业务数据冲突，以实时业务数据为准。
3. 如果信息不足，请明确说明“不确定”或“当前没有查到”，不要编造。
4. 尽量先给直接结论，再补充必要解释。
5. 如果本地知识片段有帮助，可以简短提及来源标题，但不要堆砌引用。

近期会话：
{history}

实时业务数据：
{realtime_context}

本地知识片段：
{knowledge_context}

用户问题：
{question}
"""
        return ChatPromptTemplate.from_template(template.strip())

    def _generate_answer(
        self,
        question: str,
        session_id: str,
        realtime_sections: list[dict[str, str]],
        documents: list[Document],
    ) -> str:
        prompt = self._build_answer_prompt()
        chain = prompt | self.llm | StrOutputParser()
        try:
            return chain.invoke(
                {
                    "history": self._format_recent_history(session_id),
                    "realtime_context": self._format_realtime_context(realtime_sections),
                    "knowledge_context": self._format_knowledge_context(documents),
                    "question": question,
                }
            ).strip()
        except Exception as exc:
            print(f"Answer generation failed: {exc}")
            return self._fallback_answer(realtime_sections, documents)

    @staticmethod
    def _fallback_answer(realtime_sections: list[dict[str, str]], documents: list[Document]) -> str:
        if realtime_sections:
            first = realtime_sections[0]
            return f"根据当前系统数据，先给你结论：\n{first['content']}"

        if documents:
            first = documents[0]
            title = (first.metadata or {}).get("title") or "本地知识库"
            snippet = first.page_content.strip().replace("\n", " ")
            return f"我只查到了本地知识库中的相关内容（{title}）：\n{snippet[:300]}"

        return "抱歉，我暂时没有查到足够的信息，请稍后再试。"

    def query(
        self,
        question: str,
        session_id: str = "default",
        user_context: Optional[dict[str, Any]] = None,
    ) -> str:
        if not question or not question.strip():
            return "请输入问题。"

        access_context = UserAccessContext.from_payload(user_context)
        realtime_sections, denied_messages = self._collect_realtime_context(question, access_context)
        if denied_messages:
            answer = denied_messages[0]
            self._append_history(session_id, question.strip(), answer)
            return answer

        documents = self._retrieve_documents(question, access_context)
        answer = self._generate_answer(
            question=question.strip(),
            session_id=session_id,
            realtime_sections=realtime_sections,
            documents=documents,
        )
        self._append_history(session_id, question.strip(), answer)
        return answer


rag_service = RAGService()
