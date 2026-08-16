from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

# Loaded once at import time, not per-request — sentence-transformers takes
# a couple of seconds to load the model into memory.
model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")

app = FastAPI()


class EmbedRequest(BaseModel):
    text: str | None = None


class EmbedResponse(BaseModel):
    embedding: list[float]


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest):
    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="text must not be empty")

    vector = model.encode(request.text).tolist()
    return EmbedResponse(embedding=vector)
