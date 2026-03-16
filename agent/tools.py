"""
LangChain tools for the campus club agent.
Each tool reads business data from MySQL and returns compact Chinese summaries.
"""
import os
from typing import Any

from langchain_core.tools import tool
from sqlalchemy import create_engine, text

_engine = None


def get_engine():
    global _engine
    if _engine is None:
        _engine = create_engine(
            os.getenv(
                "DB_URL",
                "mysql+pymysql://root:password@localhost:3306/community_db",
            )
        )
    return _engine


def _to_text(value: Any, default: str = "暂无") -> str:
    if value is None:
        return default
    text_value = str(value).strip()
    return text_value if text_value else default


def _fmt_time(value: Any, default: str = "待定", pattern: str = "%Y-%m-%d %H:%M") -> str:
    if value is None:
        return default
    try:
        return value.strftime(pattern)
    except Exception:
        return _to_text(value, default)


@tool
def search_clubs(keyword: str) -> str:
    """Search for clubs by name, category, or description."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT c.id, c.name, c.category, c.description,
                           COUNT(m.id) AS member_count
                    FROM t_club c
                    LEFT JOIN t_member m ON m.club_id = c.id AND m.status = 'ACTIVE'
                    WHERE c.status = 'ACTIVE'
                      AND (c.name LIKE :kw OR c.category LIKE :kw OR c.description LIKE :kw)
                    GROUP BY c.id, c.name, c.category, c.description
                    ORDER BY member_count DESC, c.id DESC
                    LIMIT 5
                    """
                ),
                {"kw": f"%{keyword}%"},
            ).fetchall()

        if not rows:
            return f"未找到与“{keyword}”相关的社团。"

        result = []
        for row in rows:
            result.append(
                f"【{row.name}】分类：{_to_text(row.category)} | 成员数：{row.member_count} | "
                f"简介：{_to_text((row.description or '')[:80], '暂无简介')}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询社团信息失败：{exc}"


@tool
def get_upcoming_activities(days: int = 7) -> str:
    """Get upcoming published or ongoing activities in the next N days."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT a.title, a.location, a.start_time, a.end_time,
                           a.max_participants, c.name AS club_name,
                           COUNT(s.id) AS signup_count
                    FROM t_activity a
                    JOIN t_club c ON c.id = a.club_id
                    LEFT JOIN t_activity_signup s ON s.activity_id = a.id
                    WHERE a.start_time BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL :days DAY)
                      AND a.status IN ('PUBLISHED', 'ONGOING')
                    GROUP BY a.id, a.title, a.location, a.start_time, a.end_time,
                             a.max_participants, c.name
                    ORDER BY a.start_time ASC
                    LIMIT 10
                    """
                ),
                {"days": days},
            ).fetchall()

        if not rows:
            return f"未来 {days} 天内暂无已发布活动。"

        result = []
        for row in rows:
            quota = (
                f"{row.signup_count}/{row.max_participants}"
                if row.max_participants
                else f"{row.signup_count} 人已报名"
            )
            result.append(
                f"【{row.title}】{row.club_name} | {_fmt_time(row.start_time, pattern='%m-%d %H:%M')} | "
                f"{_to_text(row.location, '地点待定')} | 报名：{quota}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询活动信息失败：{exc}"


@tool
def get_club_activities(club_name: str) -> str:
    """Get recent and upcoming activities for a specific club."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT a.title, a.location, a.start_time, a.status,
                           COUNT(s.id) AS signup_count
                    FROM t_activity a
                    JOIN t_club c ON c.id = a.club_id
                    LEFT JOIN t_activity_signup s ON s.activity_id = a.id
                    WHERE c.name LIKE :name
                      AND a.status <> 'CANCELLED'
                    GROUP BY a.id, a.title, a.location, a.start_time, a.status
                    ORDER BY a.start_time DESC
                    LIMIT 8
                    """
                ),
                {"name": f"%{club_name}%"},
            ).fetchall()

        if not rows:
            return f"未找到“{club_name}”的活动记录。"

        result = []
        for row in rows:
            result.append(
                f"【{row.title}】{_fmt_time(row.start_time)} | {_to_text(row.location, '地点待定')} | "
                f"状态：{row.status} | 报名数：{row.signup_count}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询社团活动失败：{exc}"


@tool
def check_recruit_status(club_name: str) -> str:
    """Check open recruitment batches and remaining quota for a club."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT rb.title, rb.end_time, rb.quota, c.name AS club_name,
                           COUNT(ra.id) AS applied_count
                    FROM t_recruit_batch rb
                    JOIN t_club c ON c.id = rb.club_id
                    LEFT JOIN t_recruit_application ra ON ra.batch_id = rb.id
                    WHERE c.name LIKE :name
                      AND rb.status = 'OPEN'
                      AND rb.end_time >= NOW()
                    GROUP BY rb.id, rb.title, rb.end_time, rb.quota, c.name
                    ORDER BY rb.end_time ASC
                    LIMIT 3
                    """
                ),
                {"name": f"%{club_name}%"},
            ).fetchall()

        if not rows:
            return f"“{club_name}”当前没有开放中的招新批次。"

        result = []
        for row in rows:
            remaining = row.quota - row.applied_count if row.quota is not None else "不限"
            result.append(
                f"【{row.club_name}】{row.title} | 截止：{_fmt_time(row.end_time, pattern='%m-%d %H:%M')} | "
                f"已报名：{row.applied_count} | 剩余名额：{remaining}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询招新状态失败：{exc}"


@tool
def get_club_member_count(club_name: str) -> str:
    """Get active member count and basic info for a club."""
    try:
        with get_engine().connect() as conn:
            row = conn.execute(
                text(
                    """
                    SELECT c.name, c.category, c.founded_year,
                           COUNT(m.id) AS member_count
                    FROM t_club c
                    LEFT JOIN t_member m ON m.club_id = c.id AND m.status = 'ACTIVE'
                    WHERE c.name LIKE :name
                      AND c.status = 'ACTIVE'
                    GROUP BY c.id, c.name, c.category, c.founded_year
                    LIMIT 1
                    """
                ),
                {"name": f"%{club_name}%"},
            ).fetchone()

        if not row:
            return f"未找到社团“{club_name}”。"

        return (
            f"【{row.name}】分类：{_to_text(row.category)} | "
            f"成立年份：{_to_text(row.founded_year, '未知')} | "
            f"当前活跃成员数：{row.member_count}"
        )
    except Exception as exc:
        return f"查询成员数量失败：{exc}"


@tool
def get_activity_details(keyword: str) -> str:
    """Get detailed activity info by activity title, club name, or description keyword."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT a.title, a.type, a.location, a.start_time, a.end_time,
                           a.signup_end_time, a.max_participants, a.status,
                           c.name AS club_name, COUNT(s.id) AS signup_count
                    FROM t_activity a
                    JOIN t_club c ON c.id = a.club_id
                    LEFT JOIN t_activity_signup s ON s.activity_id = a.id
                    WHERE a.title LIKE :kw
                       OR c.name LIKE :kw
                       OR a.description LIKE :kw
                    GROUP BY a.id, a.title, a.type, a.location, a.start_time, a.end_time,
                             a.signup_end_time, a.max_participants, a.status, c.name
                    ORDER BY a.start_time DESC
                    LIMIT 5
                    """
                ),
                {"kw": f"%{keyword}%"},
            ).fetchall()

        if not rows:
            return f"未找到与“{keyword}”相关的活动。"

        result = []
        for row in rows:
            quota = (
                f"{row.signup_count}/{row.max_participants}"
                if row.max_participants
                else f"{row.signup_count} 人已报名"
            )
            result.append(
                f"【{row.title}】主办：{row.club_name} | 类型：{_to_text(row.type)} | "
                f"时间：{_fmt_time(row.start_time)} 至 {_fmt_time(row.end_time)} | "
                f"地点：{_to_text(row.location, '地点待定')} | 状态：{row.status} | 报名：{quota} | "
                f"报名截止：{_fmt_time(row.signup_end_time)}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询活动详情失败：{exc}"


@tool
def get_recruit_progress(club_name: str) -> str:
    """Get recruitment review progress summary for a club."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT rb.title, rb.status, rb.end_time,
                           COUNT(ra.id) AS total_count,
                           SUM(CASE WHEN ra.first_review_status = 'PENDING' THEN 1 ELSE 0 END) AS first_pending,
                           SUM(CASE WHEN ra.final_review_status = 'PENDING' THEN 1 ELSE 0 END) AS final_pending,
                           SUM(CASE WHEN ra.final_review_status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
                           SUM(CASE WHEN ra.final_review_status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count
                    FROM t_recruit_batch rb
                    JOIN t_club c ON c.id = rb.club_id
                    LEFT JOIN t_recruit_application ra ON ra.batch_id = rb.id
                    WHERE c.name LIKE :name
                    GROUP BY rb.id, rb.title, rb.status, rb.end_time
                    ORDER BY rb.end_time DESC
                    LIMIT 5
                    """
                ),
                {"name": f"%{club_name}%"},
            ).fetchall()

        if not rows:
            return f"未找到“{club_name}”的招新批次。"

        result = []
        for row in rows:
            result.append(
                f"【{row.title}】状态：{row.status} | 截止：{_fmt_time(row.end_time)} | "
                f"总申请：{row.total_count} | 初审待处理：{row.first_pending or 0} | "
                f"终审待处理：{row.final_pending or 0} | 终审通过：{row.approved_count or 0} | "
                f"终审拒绝：{row.rejected_count or 0}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询招新进度失败：{exc}"


@tool
def get_resource_status(keyword: str) -> str:
    """Get resource availability and upcoming approved bookings by resource name or location."""
    try:
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT r.name, r.type, r.location, r.capacity, r.total_quantity, r.status,
                           SUM(
                               CASE
                                   WHEN ra.status = 'APPROVED'
                                    AND NOW() BETWEEN ra.start_time AND ra.end_time
                                   THEN ra.quantity
                                   ELSE 0
                               END
                           ) AS in_use_quantity,
                           COUNT(
                               CASE
                                   WHEN ra.status = 'APPROVED'
                                    AND ra.end_time >= NOW()
                                   THEN 1
                                   ELSE NULL
                               END
                           ) AS future_booking_count,
                           MIN(
                               CASE
                                   WHEN ra.status = 'APPROVED'
                                    AND ra.end_time >= NOW()
                                   THEN ra.start_time
                                   ELSE NULL
                               END
                           ) AS next_booking_start
                    FROM t_resource r
                    LEFT JOIN t_resource_application ra ON ra.resource_id = r.id
                    WHERE r.name LIKE :kw
                       OR r.location LIKE :kw
                       OR r.description LIKE :kw
                       OR r.type LIKE :kw
                    GROUP BY r.id, r.name, r.type, r.location, r.capacity, r.total_quantity, r.status
                    ORDER BY future_booking_count DESC, r.id DESC
                    LIMIT 5
                    """
                ),
                {"kw": f"%{keyword}%"},
            ).fetchall()

        if not rows:
            return f"未找到与“{keyword}”相关的资源。"

        result = []
        for row in rows:
            stock_text = (
                f"容量：{row.capacity}"
                if row.type == "VENUE"
                else f"总量：{_to_text(row.total_quantity, '未知')}"
            )
            result.append(
                f"【{row.name}】类型：{row.type} | 位置：{_to_text(row.location, '未配置')} | "
                f"状态：{row.status} | {stock_text} | 当前占用：{row.in_use_quantity or 0} | "
                f"后续已批准预约：{row.future_booking_count or 0} | "
                f"下一次预约：{_fmt_time(row.next_booking_start, '暂无')}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询资源状态失败：{exc}"


@tool
def get_finance_summary(club_name: str) -> str:
    """Get club finance summary including approved income, expense, and pending requests."""
    try:
        with get_engine().connect() as conn:
            summary = conn.execute(
                text(
                    """
                    SELECT c.name AS club_name,
                           COALESCE(SUM(CASE WHEN f.status = 'APPROVED' AND f.type = 'INCOME' THEN f.amount ELSE 0 END), 0) AS income_total,
                           COALESCE(SUM(CASE WHEN f.status = 'APPROVED' AND f.type = 'EXPENSE' THEN f.amount ELSE 0 END), 0) AS expense_total,
                           SUM(CASE WHEN f.status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
                           COUNT(f.id) AS record_count
                    FROM t_club c
                    LEFT JOIN t_club_finance f ON f.club_id = c.id
                    WHERE c.name LIKE :name
                    GROUP BY c.id, c.name
                    LIMIT 1
                    """
                ),
                {"name": f"%{club_name}%"},
            ).fetchone()

            recent_rows = conn.execute(
                text(
                    """
                    SELECT f.title, f.type, f.amount, f.status, f.created_at
                    FROM t_club_finance f
                    JOIN t_club c ON c.id = f.club_id
                    WHERE c.name LIKE :name
                    ORDER BY f.created_at DESC
                    LIMIT 3
                    """
                ),
                {"name": f"%{club_name}%"},
            ).fetchall()

        if not summary:
            return f"未找到社团“{club_name}”的财务记录。"

        balance = summary.income_total - summary.expense_total
        lines = [
            f"【{summary.club_name}】财务概览：已审批收入 {summary.income_total} 元 | "
            f"已审批支出 {summary.expense_total} 元 | 结余 {balance} 元 | "
            f"待审批 {summary.pending_count or 0} 条 | 记录总数 {summary.record_count}"
        ]
        if recent_rows:
            lines.append("最近财务记录：")
            for row in recent_rows:
                lines.append(
                    f"【{row.title}】{row.type} {row.amount} 元 | 状态：{row.status} | 时间：{_fmt_time(row.created_at)}"
                )
        return "\n".join(lines)
    except Exception as exc:
        return f"查询财务信息失败：{exc}"


@tool
def get_recent_notices(keyword: str) -> str:
    """Get recently published notices by title, content, or club keyword."""
    try:
        like_value = f"%{keyword}%"
        with get_engine().connect() as conn:
            rows = conn.execute(
                text(
                    """
                    SELECT n.title, n.scope, n.published_at, n.content,
                           COALESCE(c.name, '系统') AS club_name
                    FROM t_notice n
                    LEFT JOIN t_club c ON c.id = n.club_id
                    WHERE n.status = 'PUBLISHED'
                      AND (
                          :keyword = ''
                          OR n.title LIKE :kw
                          OR n.content LIKE :kw
                          OR c.name LIKE :kw
                      )
                    ORDER BY n.published_at DESC, n.id DESC
                    LIMIT 5
                    """
                ),
                {"keyword": keyword.strip(), "kw": like_value},
            ).fetchall()

        if not rows:
            target = f"与“{keyword}”相关的" if keyword.strip() else ""
            return f"未找到{target}已发布公告。"

        result = []
        for row in rows:
            result.append(
                f"【{row.title}】发布方：{row.club_name} | 范围：{row.scope} | "
                f"发布时间：{_fmt_time(row.published_at)} | 摘要：{_to_text((row.content or '')[:80], '暂无内容')}"
            )
        return "\n".join(result)
    except Exception as exc:
        return f"查询公告失败：{exc}"
