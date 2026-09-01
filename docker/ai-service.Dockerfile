# FastAPI AI service. Build context: ./ai-service
# Build:  docker build -f docker/ai-service.Dockerfile -t nia-ai-service ./ai-service
FROM python:3.12-slim
WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# Run as an unprivileged user: the service only reads its own code and opens
# port 8000, so root buys nothing and costs isolation.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin nia \
    && chown -R nia:nia /app
USER nia

EXPOSE 8000
# Shell form so Render's injected $PORT is honored (default 8000 for local docker).
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
