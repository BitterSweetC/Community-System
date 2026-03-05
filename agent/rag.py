"""
RAG Service with LangChain Agent, Tool Calling, and Conversation Memory.
"""
import os
from typing import Optional

from langchain_community.embeddings import SentenceTransformerEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough
from langchain_core.messages import HumanMessage, AIMessage
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_text_splitters import RecursiveCharacterTextSplitter

from tools import (
    search_clubs,
    get_upcoming_activities,
    get_club_activities,
    check_recruit_status,
    get_club_member_count,
)

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
PERSIST_DIRECTORY = os.path.join(os.path.dirname(__file__), "chroma_db")
EMBEDDING_MODEL = "all-MiniLM-L6-v2"
MAX_HISTORY = 10  # keep last 10 turns per session


class RAGService:
    def __init__(self):
        self.vectorstore: Optional[Chroma] = None
        self.retriever = None

        api_key = os.getenv("DEEPSEEK_API_KEY")
        if not api_key:
            raise ValueError("DEEPSEEK_API_KEY is not set.")

        self.llm = ChatOpenAI(
            model="deepseek-chat",
            openai_api_key=api_key,
            openai_api_base=os.getenv("DEEPSEEK_API_URL", "https://api.deepseek.com"),
            temperature=0.7,
        )

        # In-memory session store: {session_id: [HumanMessage|AIMessage, ...]}
        self._sessions: dict[str, list] = {}

        self._tools = [
            search_clubs,
            get_upcoming_activities,
            get_club_activities,
            check_recruit_status,
            get_club_member_count,
        ]

        self._agent_executor: Optional[AgentExecutor] = None

    # ── Knowledge Base ────────────────────────────────────────────────────────

    def initialize_knowledge_base(self):
        print("Initializing knowledge base...")
        os.makedirs(DATA_DIR, exist_ok=True)

        from langchain_community.document_loaders import DirectoryLoader, TextLoader
        loader = DirectoryLoader(DATA_DIR, glob="*.md", loader_cls=TextLoader,
                                 silent_errors=True)
        documents = loader.load()

        splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        chunks = splitter.split_documents(documents) if documents else []

        embeddings = SentenceTransformerEmbeddings(model_name=EMBEDDING_MODEL)
        if chunks:
            self.vectorstore = Chroma.from_documents(
                chunks, embeddings, persist_directory=PERSIST_DIRECTORY)
        else:
            self.vectorstore = Chroma(
                embedding_function=embeddings, persist_directory=PERSIST_DIRECTORY)

        self.retriever = self.vectorstore.as_retriever(search_kwargs={"k": 3})
        self._build_agent()
        print(f"Knowledge base ready. Chunks: {len(chunks)}")

    def add_document(self, content: str, source: str) -> int:
        from langchain_core.documents import Document
        doc = Document(page_content=content, metadata={"source": source})
        splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        chunks = splitter.split_documents([doc])
        if self.vectorstore:
            self.vectorstore.add_documents(chunks)
        else:
            embeddings = SentenceTransformerEmbeddings(model_name=EMBEDDING_MODEL)
            self.vectorstore = Chroma.from_documents(
                chunks, embeddings, persist_directory=PERSIST_DIRECTORY)
            self.retriever = self.vectorstore.as_retriever(search_kwargs={"k": 3})
            self._build_agent()
        return len(chunks)

    # ── Agent ─────────────────────────────────────────────────────────────────

    def _build_agent(self):
        system_prompt = (
            "你是校园社团管理系统的智能助手，可以回答关于社团、活动、招新的问题。\n"
            "你有以下能力：\n"
            "1. 调用工具实时查询数据库获取最新数据\n"
            "2. 参考知识库中的背景资料回答问题\n"
            "3. 记住本次对话的上下文\n\n"
            "回答时优先使用工具获取实时数据。如果工具无法回答，再参考知识库。\n"
            "回答请使用中文，保持简洁友好。"
        )

        prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt),
            MessagesPlaceholder("chat_history"),
            ("human", "{input}"),
            MessagesPlaceholder("agent_scratchpad"),
        ])

        agent = create_tool_calling_agent(self.llm, self._tools, prompt)
        self._agent_executor = AgentExecutor(
            agent=agent,
            tools=self._tools,
            verbose=False,
            max_iterations=5,
            handle_parsing_errors=True,
        )

    # ── Session Memory ────────────────────────────────────────────────────────

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

    # ── Query ─────────────────────────────────────────────────────────────────

    def query(self, question: str, session_id: str = "default") -> str:
        if self._agent_executor is None:
            return self._rag_fallback(question)

        history = self._get_history(session_id)
        try:
            result = self._agent_executor.invoke({
                "input": question,
                "chat_history": history,
            })
            answer = result.get("output", "抱歉，我无法回答这个问题。")
        except Exception as e:
            print(f"Agent error: {e}")
            answer = self._rag_fallback(question)

        self._append_history(session_id, question, answer)
        return answer

    def _rag_fallback(self, question: str) -> str:
        if not self.retriever:
            return "知识库尚未初始化，请稍后再试。"
        template = (
            "根据以下背景资料回答问题，如果资料中没有相关信息请如实说明。\n\n"
            "背景资料：\n{context}\n\n"
            "问题：{question}"
        )
        prompt = ChatPromptTemplate.from_template(template)
        chain = (
            {"context": self.retriever | (lambda docs: "\n\n".join(d.page_content for d in docs)),
             "question": RunnablePassthrough()}
            | prompt
            | self.llm
            | StrOutputParser()
        )
        try:
            return chain.invoke(question)
        except Exception as e:
            return f"查询出错：{e}"


rag_service = RAGService()
