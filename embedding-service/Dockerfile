# sentence-transformers 5.7.0 officially supports up to Python 3.13 (not yet
# 3.14) as of Phase 16 — verified against its PyPI classifiers, not assumed.
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

COPY app.py .

EXPOSE 8000

# Model weights are downloaded from Hugging Face on first run of this image,
# so the first `docker compose up --build` needs network access.
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
