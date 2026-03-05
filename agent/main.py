import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from rag import rag_service
from recommendation import recommendation_service

app = FastAPI(title="Community RAG Agent")


class QueryRequest(BaseModel):
    query: str
    session_id: str = "default"


class RecommendRequest(BaseModel):
    user_id: int
    top_k: int = 5


class SyncRequest(BaseModel):
    content: str
    source: str


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
    response = rag_service.query(request.query, session_id=request.session_id)
    return ChatResponse(response=response, session_id=request.session_id)


@app.delete("/chat/session/{session_id}")
async def clear_session(session_id: str):
    rag_service.clear_session(session_id)
    return {"status": "cleared", "session_id": session_id}


@app.post("/recommend")
async def recommend(request: RecommendRequest):
    try:
        club_ids = recommendation_service.get_recommendations(request.user_id, request.top_k)
        return {"club_ids": club_ids}
    except Exception as e:
        print(f"Recommendation error: {e}")
        return {"club_ids": []}


@app.post("/sync")
async def sync_knowledge(request: SyncRequest):
    try:
        chunks_added = rag_service.add_document(request.content, request.source)
        return {"status": "success", "chunks_added": chunks_added}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
