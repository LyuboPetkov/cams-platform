# sentence-transformers 5.7.0 officially supports up to Python 3.13 (not yet
# 3.14) - verified against its PyPI classifiers, not assumed.
FROM python:3.13-slim

WORKDIR /app

# curl is needed for the container healthcheck defined in docker-compose.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
# torch's regular PyPI Linux wheel has hard-pinned nvidia-cu13/cuda-toolkit
# dependencies (~2GB) regardless of whether a GPU is present. This container
# never has one, so torch is installed first from PyTorch's dedicated CPU
# wheel index; requirements.txt's own torch==2.13.0 pin (installed next) is
# then already satisfied and pip does not re-resolve it against the GPU build.
RUN pip install --no-cache-dir torch==2.13.0 --index-url https://download.pytorch.org/whl/cpu \
    && pip install --no-cache-dir -r requirements.txt

RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')"

# Force fully offline mode at runtime: without this, sentence-transformers
# still fires a HEAD request to huggingface.co on every container start to
# check for a newer version, even though the weights are already baked into
# this image above. Behind a TLS-intercepting proxy that request fails and
# retries with backoff for 60+ seconds - long enough to blow past the
# healthcheck's grace period and block dependent services from starting.
# Nothing to check for once the weights are already baked in - skip straight
# to the local cache.
ENV HF_HUB_OFFLINE=1

COPY app.py .

EXPOSE 8000

# Model weights are downloaded from Hugging Face on first run of this image,
# so the first `docker compose up --build` needs network access.
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
