"""
HTTP client for embedding-service, with an on-disk cache.

Talks to the same container the backend does (host port 8000 per
docker-compose.local.yml) over the same POST /embed contract. Every embedded
text is cached in .embedding_cache.json under sha256(text), so a second run with
unchanged fixtures makes no HTTP calls and reproduces the previous run exactly.
The /health probe is lazy for the same reason: it only fires when a cache miss is
about to force a real request.
"""

import atexit
import hashlib
import json
import os
import sys
from pathlib import Path

import numpy as np
import requests

DEFAULT_URL = "http://localhost:8000"
CACHE_PATH = Path(__file__).parent / ".embedding_cache.json"

# embedding-service loads all-MiniLM-L6-v2, which gives 384 dims. Checked rather
# than assumed, so a swapped model fails loudly instead of quietly producing
# numbers that cannot be compared with these.
EXPECTED_DIMS = 384


class EmbeddingServiceUnavailable(RuntimeError):
    pass


class EmbeddingClient:
    def __init__(self, base_url: str | None = None, cache_path: Path = CACHE_PATH):
        self.base_url = (base_url or os.environ.get("EMBEDDING_SERVICE_URL") or DEFAULT_URL).rstrip("/")
        self.cache_path = cache_path
        self.session = requests.Session()

        self.cache = self._load_cache()
        self._dirty = False
        self._health_checked = False

        # The runner prints these to show a warm run hits no network.
        self.http_calls = 0
        self.cache_hits = 0

        atexit.register(self.flush)

    def _load_cache(self) -> dict:
        if not self.cache_path.exists():
            return {}
        try:
            return json.loads(self.cache_path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError) as exc:
            print(f"WARNING: could not read {self.cache_path.name} ({exc}) - starting from an empty cache",
                  file=sys.stderr)
            return {}

    def flush(self) -> None:
        """Write the cache once, at exit, and only if something was added."""
        if not self._dirty:
            return
        self.cache_path.write_text(json.dumps(self.cache, sort_keys=True), encoding="utf-8")
        self._dirty = False

    def _ensure_healthy(self) -> None:
        if self._health_checked:
            return
        try:
            response = self.session.get(f"{self.base_url}/health", timeout=10)
            response.raise_for_status()
        except requests.RequestException as exc:
            raise EmbeddingServiceUnavailable(
                f"embedding-service is not reachable at {self.base_url} ({exc}).\n"
                f"Start it with:\n"
                f"    docker compose -f docker-compose.local.yml up embedding-service\n"
                f"Nothing else needs to be running - no Postgres, no backend."
            ) from exc
        self._health_checked = True
        self.http_calls += 1

    def embed(self, text: str) -> list:
        """
        The 384-dim embedding for `text`, from cache when possible.

        embedding-service rejects blank text with HTTP 400, so blank input is
        handled here rather than sent. Callers that build text out of optional
        fixture fields can legitimately produce an empty string.
        """
        if not text or not text.strip():
            return [0.0] * EXPECTED_DIMS

        key = hashlib.sha256(text.encode("utf-8")).hexdigest()
        cached = self.cache.get(key)
        if cached is not None:
            self.cache_hits += 1
            return cached

        self._ensure_healthy()
        try:
            response = self.session.post(f"{self.base_url}/embed", json={"text": text}, timeout=60)
            response.raise_for_status()
        except requests.RequestException as exc:
            raise EmbeddingServiceUnavailable(f"POST {self.base_url}/embed failed: {exc}") from exc
        self.http_calls += 1

        vector = response.json()["embedding"]
        if len(vector) != EXPECTED_DIMS:
            raise EmbeddingServiceUnavailable(
                f"embedding-service returned {len(vector)} dims, expected {EXPECTED_DIMS}. "
                f"The model behind /embed is not the one these results were measured on."
            )

        self.cache[key] = vector
        self._dirty = True
        return vector


def cosine(a, b) -> float:
    """
    Same formula as cams.util.EmbeddingSimilarity.cosineSimilarity,
    dot(a, b) / (||a|| * ||b||). Zero vectors score 0.0 rather than dividing by
    zero.
    """
    vec_a = np.asarray(a, dtype=np.float64)
    vec_b = np.asarray(b, dtype=np.float64)
    norm_a = np.linalg.norm(vec_a)
    norm_b = np.linalg.norm(vec_b)
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return float(np.dot(vec_a, vec_b) / (norm_a * norm_b))
