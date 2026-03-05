"""
LangChain Tools for the Campus Club Agent.
Each tool queries the MySQL database and returns structured text.
"""
import os
from datetime import datetime
from sqlalchemy import create_engine, text
from langchain_core.tools import tool

_engine = None

def get_engine():
    global _engine
    if _engine is None:
        _engine = create_engine(os.getenv("DB_URL", "mysql+pymysql://root:password@localhost:3306/community_db"))
    return _engine


@tool
def search_clubs(keyword: str) -> str:
    """Search for clubs by name or category keyword. Use this when the user asks about clubs."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(text("""
                SELECT c.id, c.name, c.category, c.description, c.status,
                       COUNT(m.id) as member_count
                FROM t_club c
                LEFT JOIN t_member m ON m.club_id = c.id AND m.status = 'ACTIVE'
                WHERE c.status = 'ACTIVE'
                  AND (c.name LIKE :kw OR c.category LIKE :kw OR c.description LIKE :kw)
                GROUP BY c.id
                LIMIT 5
            """), {"kw": f"%{keyword}%"}).fetchall()

        if not rows:
            return f"未找到与「{keyword}」相关的社团。"

        result = []
        for r in rows:
            result.append(
                f"【{r.name}】分类：{r.category}，成员数：{r.member_count}，"
                f"简介：{(r.description or '暂无')[:80]}"
            )
        return "\n".join(result)
    except Exception as e:
        return f"查询失败：{e}"


@tool
def get_upcoming_activities(days: int = 7) -> str:
    """Get upcoming activities in the next N days. Use this when user asks about recent or upcoming activities."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(text("""
                SELECT a.title, a.location, a.start_time, a.end_time,
                       a.max_participants, c.name as club_name,
                       COUNT(s.id) as signup_count
                FROM t_activity a
                JOIN t_club c ON c.id = a.club_id
                LEFT JOIN t_activity_signup s ON s.activity_id = a.id
                WHERE a.start_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL :days DAY)
                  AND a.status IN ('PUBLISHED', 'ONGOING')
                GROUP BY a.id
                ORDER BY a.start_time
                LIMIT 10
            """), {"days": days}).fetchall()

        if not rows:
            return f"未来 {days} 天内暂无活动。"

        result = []
        for r in rows:
            start = r.start_time.strftime("%m-%d %H:%M") if r.start_time else "待定"
            quota = f"{r.signup_count}/{r.max_participants}" if r.max_participants else f"{r.signup_count}人已报名"
            result.append(f"【{r.title}】{r.club_name} | {start} | {r.location or '地点待定'} | {quota}")
        return "\n".join(result)
    except Exception as e:
        return f"查询失败：{e}"


@tool
def get_club_activities(club_name: str) -> str:
    """Get recent and upcoming activities for a specific club by name."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(text("""
                SELECT a.title, a.location, a.start_time, a.status,
                       COUNT(s.id) as signup_count
                FROM t_activity a
                JOIN t_club c ON c.id = a.club_id
                LEFT JOIN t_activity_signup s ON s.activity_id = a.id
                WHERE c.name LIKE :name AND a.status != 'CANCELLED'
                GROUP BY a.id
                ORDER BY a.start_time DESC
                LIMIT 8
            """), {"name": f"%{club_name}%"}).fetchall()

        if not rows:
            return f"未找到「{club_name}」的活动记录。"

        result = []
        for r in rows:
            start = r.start_time.strftime("%Y-%m-%d %H:%M") if r.start_time else "待定"
            result.append(f"【{r.title}】{start} | {r.location or '地点待定'} | 状态：{r.status} | {r.signup_count}人报名")
        return "\n".join(result)
    except Exception as e:
        return f"查询失败：{e}"


@tool
def check_recruit_status(club_name: str) -> str:
    """Check if a club is currently recruiting and get quota info."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(text("""
                SELECT rb.title, rb.start_time, rb.end_time, rb.quota,
                       c.name as club_name,
                       COUNT(ra.id) as applied_count
                FROM t_recruit_batch rb
                JOIN t_club c ON c.id = rb.club_id
                LEFT JOIN t_recruit_application ra ON ra.batch_id = rb.id
                WHERE c.name LIKE :name
                  AND rb.status = 'OPEN'
                  AND rb.end_time >= NOW()
                GROUP BY rb.id
                ORDER BY rb.end_time
                LIMIT 3
            """), {"name": f"%{club_name}%"}).fetchall()

        if not rows:
            return f"「{club_name}」当前没有开放的招新批次。"

        result = []
        for r in rows:
            end = r.end_time.strftime("%m-%d %H:%M") if r.end_time else "待定"
            remaining = (r.quota - r.applied_count) if r.quota else "不限"
            result.append(
                f"【{r.club_name}】{r.title} | 截止：{end} | "
                f"已报名：{r.applied_count} | 剩余名额：{remaining}"
            )
        return "\n".join(result)
    except Exception as e:
        return f"查询失败：{e}"


@tool
def get_club_member_count(club_name: str) -> str:
    """Get the member count and basic info for a club."""
    try:
        with get_engine().connect() as conn:
            row = conn.execute(text("""
                SELECT c.name, c.category, c.founded_year,
                       COUNT(m.id) as member_count
                FROM t_club c
                LEFT JOIN t_member m ON m.club_id = c.id AND m.status = 'ACTIVE'
                WHERE c.name LIKE :name AND c.status = 'ACTIVE'
                GROUP BY c.id
                LIMIT 1
            """), {"name": f"%{club_name}%"}).fetchone()

        if not row:
            return f"未找到社团「{club_name}」。"

        return (f"【{row.name}】分类：{row.category} | "
                f"成立年份：{row.founded_year or '未知'} | "
                f"当前成员数：{row.member_count}")
    except Exception as e:
        return f"查询失败：{e}"
