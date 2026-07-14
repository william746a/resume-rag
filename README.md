# resume-rag

Interactive resume Q&A: Quarkus + LangChain4j RAG backend with an Angular UI (Quinoa). Answers stream over SSE from a corpus (resume + optional context docs) loaded from classpath or GCS.

## Local development

```bash
export OPENROUTER_API_KEY=sk-or-...
./mvnw quarkus:dev
```

Open http://localhost:8080. Dev profile loads `resume.md` plus `context/*.md` from the classpath.

## Local setup

Copy `.env.example` to `.env` and fill in your OpenRouter key:

```bash
cp .env.example .env
# edit .env
source .env && ./mvnw quarkus:dev
```

## Configuration

| Variable | Purpose |
|---|---|
| `OPENROUTER_API_KEY` | Required for real answers; **required** in prod (no default) |
| `DOCUMENTS_GCS_BUCKET` | GCS bucket for corpus (required in prod) |
| `DOCUMENTS_GCS_CONTEXT_PREFIX` | Prefix for context docs (default `context/`) |
| `DOCUMENTS_GCS_CONTEXT_PREFIX_ENABLED` | Auto-ingest `.md` under prefix (prod default: true) |
| `RESUME_PUBLIC_URL` | Public site URL for OpenRouter `HTTP-Referer` |
| `RESUME_GLOBAL_DAILY_CAP` | Global chat cap across all clients (default 500/day UTC) |
| `LOG_LEVEL` | Application log level (default `INFO`) |
| `RESUME_SANITIZER_BLOCKED_PATTERNS` | Comma-separated regex blocklist (sensible default included) |

See [docs/document-corpus-ingestion.md](docs/document-corpus-ingestion.md) for corpus layout and IAM.

## Production profile

Packaged / native runs use `quarkus.profile=prod` by default (set explicitly in the container entrypoint). Prod:

- Requires a real `OPENROUTER_API_KEY`
- Loads the resume from GCS (`resume.md`) and expands `context/*.md` under the bucket
- Fails fast at startup if GCS bucket is missing

## Security & hardening

- Per-IP rate limits: **10 requests/minute**, **100 requests/day** (Bucket4j)
- Global daily chat cap: **500** by default (`RESUME_GLOBAL_DAILY_CAP`); single-instance only (`--max-instances=1`)
- Question length capped at **500** characters
- Prompt-injection guard via configurable regex blocklist (`QuestionSanitizer`)
- Safe error responses: generic 500/429 bodies; stack traces never leak to clients
- Security headers: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, CSP
- Health endpoints: `/q/health`, `/q/health/live`, `/q/health/ready` (ready becomes UP after corpus ingestion)
- Metrics endpoint: `/q/metrics` exposes `resume.chat.*` and `resume.rate-limit.global-cap.*`

## Native image

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Or multi-stage Docker (recommended for Cloud Run):

```bash
docker build -f src/main/docker/Dockerfile.multistage-native -t resume-rag .
docker run --rm -p 8080:8080 \
  -e OPENROUTER_API_KEY=... \
  -e DOCUMENTS_GCS_BUCKET=... \
  -e RESUME_PUBLIC_URL=https://example.com \
  resume-rag
```

## Cloud Build + Cloud Run

1. Create Artifact Registry repo, GCS bucket, and Secret Manager secret `openrouter-key`.
2. Grant the Cloud Run service account `roles/storage.objectViewer` on the bucket.
3. Build:

```bash
gcloud builds submit --config=cloudbuild.yaml \
  --substitutions=_REGION=us-central1,_REPO=resume,_SERVICE=resume-rag
```

4. Deploy:

```bash
gcloud run deploy resume-rag \
  --image REGION-docker.pkg.dev/PROJECT/resume/resume-rag:latest \
  --region REGION \
  --memory 512Mi --cpu 1 \
  --min-instances 0 --max-instances 1 \
  --concurrency 100 \
  --allow-unauthenticated \
  --set-secrets OPENROUTER_API_KEY=openrouter-key:latest \
  --set-env-vars DOCUMENTS_GCS_BUCKET=YOUR_BUCKET,RESUME_PUBLIC_URL=https://YOUR_DOMAIN,QUARKUS_PROFILE=prod
```

Upload corpus objects before the first deploy:

```text
gs://YOUR_BUCKET/resume.md
gs://YOUR_BUCKET/context/*.md
```

After updating corpus files, restart the Cloud Run revision (ingestion is startup-only).

## Tests

```bash
./mvnw test
```

SSE smoke test against OpenRouter runs only when `OPENROUTER_API_KEY` is set.
