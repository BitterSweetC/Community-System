from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from rag import rag_service
from recommendation import recommendation_service

app = FastAPI(title="Community RAG Agent")


class QueryRequest(BaseModel):
    query: str
    session_id: str = "default"
    user_context: dict[str, Any] = Field(default_factory=dict)


class RecommendRequest(BaseModel):
    user_id: int
    top_k: int = 5
    mode: str = "hybrid"


class SyncRequest(BaseModel):
    operation: str = "upsert"
    source: str
    content: str | None = None
    title: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class ChatResponse(BaseModel):
    response: str
    session_id: str


@app.on_event("startup")
async def startup_event():
    try:
        rag_service.initialize_knowledge_base()
    except Exception as e:
        print(f"Failed to initialize knowledge base: {e}")


@app.post("/chat", response_model=ChatResponse)
async def chat(request: QueryRequest):
    if not request.query:
        raise HTTPException(status_code=400, detail="Query cannot be empty")
    response = rag_service.query(
        request.query,
        session_id=request.session_id,
        user_context=request.user_context,
    )
    return ChatResponse(response=response, session_id=request.session_id)


@app.delete("/chat/session/{session_id}")
async def clear_session(session_id: str):
    rag_service.clear_session(session_id)
    return {"status": "cleared", "session_id": session_id}


@app.post("/recommend")
async def recommend(request: RecommendRequest):
    try:
        club_ids = recommendation_service.get_recommendations_by_mode(
            request.user_id,
            request.top_k,
            request.mode,
        )
        return {"club_ids": club_ids, "mode": request.mode}
    except Exception as e:
        print(f"Recommendation error: {e}")
        return {"club_ids": []}


@app.post("/sync")
async def sync_knowledge(request: SyncRequest):
    operation = request.operation.strip().lower()
    if not request.source or not request.source.strip():
        raise HTTPException(status_code=400, detail="source cannot be empty")

    try:
        if operation == "delete":
            deleted = rag_service.delete_document(
                source=request.source,
                metadata=request.metadata,
            )
            return {
                "status": "success",
                "operation": operation,
                "deleted": deleted,
            }

        if operation != "upsert":
            raise HTTPException(status_code=400, detail="operation must be upsert or delete")

        if not request.content or not request.content.strip():
            raise HTTPException(status_code=400, detail="content cannot be empty for upsert")

        chunks_added = rag_service.upsert_document(
            content=request.content,
            source=request.source,
            title=request.title,
            metadata=request.metadata,
        )
        return {
            "status": "success",
            "operation": operation,
            "chunks_added": chunks_added,
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/knowledge/reload")
async def reload_knowledge():
    try:
        rag_service.initialize_knowledge_base()
        return {
            "status": "success",
            "document_count": rag_service.get_knowledge_document_count(),
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
