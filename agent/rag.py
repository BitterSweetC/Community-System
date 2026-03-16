"""
RAG Service with LangChain Agent, Tool Calling, and Conversation Memory.
"""
import os
import re
from typing import Any, Optional

from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough
from langchain_core.messages import HumanMessage, AIMessage
from langchain.agents import create_agent
from langchain_text_splitters import RecursiveCharacterTextSplitter

from tools import (
    search_clubs,
    get_upcoming_activities,
    get_club_activities,
    check_recruit_status,
    get_club_member_count,
    get_activity_details,
    get_recruit_progress,
    get_resource_status,
    get_finance_summary,
    get_recent_notices,
)
from local_embeddings import create_embeddings

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
PERSIST_DIRECTORY = os.path.join(os.path.dirname(__file__), "chroma_db")
EMBEDDING_MODEL = "all-MiniLM-L6-v2"
MAX_HISTORY = 10  # keep last 10 turns per session


class RAGService:
    def __init__(self):
        self.vectorstore: Optional[Chroma] = None
        self.retriever = None
        self._embeddings = None

        api_key = os.getenv("DEEPSEEK_API_KEY")
        if not api_key:
            raise ValueError("DEEPSEEK_API_KEY is not set.")

        self.llm = ChatOpenAI(
            model="deepseek-chat",
            openai_api_key=api_key,
            openai_api_base=os.getenv("DEEPSEEK_API_URL", "https://api.deepseek.com"),
            temperature=0.7,
        )

        self._sessions: dict[str, list] = {}
        self._tools = [
            search_clubs,
            get_upcoming_activities,
            get_club_activities,
            check_recruit_status,
            get_club_member_count,
            get_activity_details,
            get_recruit_progress,
            get_resource_status,
            get_finance_summary,
            get_recent_notices,
        ]

        self._agent_executor: Optional[Any] = None
        self._build_agent()

    @staticmethod
    def _contains_any(text: str, keywords: list[str]) -> bool:
        return any(keyword in text for keyword in keywords)

    @staticmethod
    def _strip_filler(text: str) -> str:
        cleaned = text.strip()
        cleaned = re.sub(r"^(请问|请|麻烦|帮我|我想|我想问下|我想了解|帮忙|查询|查看)+", "", cleaned)
        cleaned = re.sub(r"[？?！!。]+$", "", cleaned)
        cleaned = re.sub(r"\s+", " ", cleaned)
        return cleaned.strip(" ：:，,")

    def _extract_club_name(self, question: str) -> str:
        matches = re.findall(
            r"([\u4e00-\u9fa5A-Za-z0-9#·\-]{2,}(?:协会|实验室|俱乐部|社团|学院|社|队))",
            question,
        )
        return max(matches, key=len) if matches else ""

    def _extract_resource_keyword(self, question: str) -> str:
        colon_parts = re.split(r"[：:]", question, maxsplit=1)
        candidate = colon_parts[1] if len(colon_parts) == 2 else question
        match = re.search(
            r"([\u4e00-\u9fa5A-Za-z0-9#·\- ]*(?:多媒体教室|报告厅|会议室|篮球场|排练厅|教室|场地|器材|资源)[\u4e00-\u9fa5A-Za-z0-9#·\- ]*)",
            candidate,
        )
        if match:
            candidate = match.group(1)
        candidate = re.sub(
            r"(现在.*|目前.*|能不能用.*|可不可以用.*|后面.*|有没有.*|占用.*|预约.*|状态.*)$",
            "",
            candidate,
        )
        return self._strip_filler(candidate)

    def _extract_activity_keyword(self, question: str) -> str:
        match = re.search(r"([A-Za-z0-9\u4e00-\u9fa5#·\-]{1,20})(?:相关)?的?活动", question)
        if match:
            return self._strip_filler(match.group(1))
        match = re.search(r"(?:关于|有关|查询|查看)([A-Za-z0-9\u4e00-\u9fa5#·\-]{1,20})", question)
        if match:
            return self._strip_filler(match.group(1))
        return ""

    def _extract_notice_keyword(self, question: str) -> str:
        club_name = self._extract_club_name(question)
        if club_name:
            return club_name
        match = re.search(r"(?:关于|有关)([A-Za-z0-9\u4e00-\u9fa5#·\-]{1,20})", question)
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
            return f"查询出错：{exc}"

    def _route_intent(self, question: str) -> Optional[str]:
        text = self._strip_filler(question)
        if not text:
            return None

        club_name = self._extract_club_name(text)

        if self._contains_any(text, ["公告", "通知", "通告"]):
            return self._invoke_tool(get_recent_notices, {"keyword": self._extract_notice_keyword(text)})

        if self._contains_any(text, ["资源", "教室", "场地", "器材", "会议室", "报告厅", "篮球场", "排练厅", "预约"]):
            keyword = self._extract_resource_keyword(text)
            if keyword:
                return self._invoke_tool(get_resource_status, {"keyword": keyword})

        if self._contains_any(text, ["财务", "经费", "收入", "支出", "报销", "结余"]):
            if club_name:
                return self._invoke_tool(get_finance_summary, {"club_name": club_name})
            return "请说明要查询哪个社团的财务信息。"

        if self._contains_any(text, ["招新", "报名", "申请", "初审", "终审", "名额", "审核"]):
            if club_name:
                if self._contains_any(text, ["进度", "多少申请", "多少通过", "审核", "初审", "终审", "通过", "拒绝"]):
                    return self._invoke_tool(get_recruit_progress, {"club_name": club_name})
                return self._invoke_tool(check_recruit_status, {"club_name": club_name})

        if self._contains_any(text, ["活动", "讲座", "比赛", "沙龙", "分享会"]):
            if club_name:
                return self._invoke_tool(get_club_activities, {"club_name": club_name})
            keyword = self._extract_activity_keyword(text)
            if keyword:
                return self._invoke_tool(get_activity_details, {"keyword": keyword})

        if self._contains_any(text, ["成员数", "成员数量", "多少成员", "几个人"]) and club_name:
            return self._invoke_tool(get_club_member_count, {"club_name": club_name})

        if self._contains_any(text, ["社团", "协会", "实验室", "俱乐部"]) and not self._contains_any(text, ["公告", "通知", "活动", "招新", "财务"]):
            keyword = club_name or text
            return self._invoke_tool(search_clubs, {"keyword": keyword})

        return None

    def initialize_knowledge_base(self):
        print("Initializing knowledge base...")
        os.makedirs(DATA_DIR, exist_ok=True)

        from langchain_community.document_loaders import DirectoryLoader, TextLoader

        loader = DirectoryLoader(DATA_DIR, glob="*.md", loader_cls=TextLoader, silent_errors=True)
        documents = loader.load()

        splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        chunks = splitter.split_documents(documents) if documents else []

        try:
            embeddings, provider = create_embeddings(EMBEDDING_MODEL)
            self._embeddings = embeddings
            if chunks:
                self.vectorstore = Chroma.from_documents(
                    chunks, embeddings, persist_directory=PERSIST_DIRECTORY
                )
            else:
                self.vectorstore = Chroma(
                    embedding_function=embeddings, persist_directory=PERSIST_DIRECTORY
                )

            self.retriever = self.vectorstore.as_retriever(search_kwargs={"k": 3})
            print(f"Knowledge base ready. Chunks: {len(chunks)}, provider: {provider}")
        except Exception as e:
            self.vectorstore = None
            self.retriever = None
            print(f"Knowledge base initialization degraded: {e}")

    def add_document(self, content: str, source: str) -> int:
        from langchain_core.documents import Document

        doc = Document(page_content=content, metadata={"source": source})
        splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        chunks = splitter.split_documents([doc])
        if self.vectorstore:
            self.vectorstore.add_documents(chunks)
        else:
            embeddings = self._embeddings
            if embeddings is None:
                embeddings, _ = create_embeddings(EMBEDDING_MODEL)
                self._embeddings = embeddings
            self.vectorstore = Chroma.from_documents(
                chunks, embeddings, persist_directory=PERSIST_DIRECTORY
            )
            self.retriever = self.vectorstore.as_retriever(search_kwargs={"k": 3})
            self._build_agent()
        return len(chunks)

    def _build_agent(self):
        system_prompt = (
            "你是校园社团管理系统的智能助手，负责回答社团、活动、招新、资源、财务和公告相关问题。"
            "优先调用工具获取实时业务数据；若工具无法回答，再参考知识库。"
            "涉及人数、状态、时间、金额、预约占用等信息时，默认先查工具，不要凭空猜测。"
            "请使用中文，回答简洁友好。"
        )
        self._agent_executor = create_agent(
            model=self.llm,
            tools=self._tools,
            system_prompt=system_prompt,
        )

    def _get_history(self, session_id: str) -> list:
        return self._sessions.get(session_id, [])

    def _append_history(self, session_id: str, human: str, ai: str):
        history = self._sessions.setdefault(session_id, [])
        history.append(HumanMessage(content=human))
        history.append(AIMessage(content=ai))
        if len(history) > MAX_HISTORY * 2:
            self._sessions[session_id] = history[-(MAX_HISTORY * 2):]

    def clear_session(self, session_id: str):
        self._sessions.pop(session_id, None)

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

    def _extract_agent_answer(self, result: Any) -> str:
        if not isinstance(result, dict):
            return ""

        messages = result.get("messages")
        if not isinstance(messages, list):
            return ""

        for msg in reversed(messages):
            if isinstance(msg, AIMessage):
                text = self._content_to_text(msg.content)
                if text:
                    return text

            text = self._content_to_text(getattr(msg, "content", None))
            if text:
                return text
        return ""

    def query(self, question: str, session_id: str = "default") -> str:
        routed_answer = self._route_intent(question)
        if routed_answer:
            answer = routed_answer
        else:
            if self._agent_executor is None:
                return self._rag_fallback(question)

            history = self._get_history(session_id)
            try:
                result = self._agent_executor.invoke({
                    "messages": history + [HumanMessage(content=question)],
                })
                answer = self._extract_agent_answer(result) or "抱歉，我无法回答这个问题。"
            except Exception as e:
                print(f"Agent error: {e}")
                answer = self._rag_fallback(question)

        self._append_history(session_id, question, answer)
        return answer

    def _rag_fallback(self, question: str) -> str:
        if not self.retriever:
            try:
                response = self.llm.invoke(
                    "你是校园社团助手。知识库当前不可用，请基于常识给出谨慎建议，并明确说明仅供参考。"
                    f"\n\n用户问题：{question}"
                )
                text = self._content_to_text(getattr(response, "content", response))
                return text or "AI服务暂时不可用，请稍后再试。"
            except Exception:
                return "AI服务暂时不可用，请稍后再试。"
        template = (
            "根据以下背景资料回答问题，如果资料中没有相关信息请如实说明。\n\n"
            "背景资料：\n{context}\n\n"
            "问题：{question}"
        )
        prompt = ChatPromptTemplate.from_template(template)
        chain = (
            {
                "context": self.retriever | (lambda docs: "\n\n".join(d.page_content for d in docs)),
                "question": RunnablePassthrough(),
            }
            | prompt
            | self.llm
            | StrOutputParser()
        )
        try:
            return chain.invoke(question)
        except Exception as e:
            return f"查询出错：{e}"


rag_service = RAGService()
