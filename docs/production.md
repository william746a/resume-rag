# Production checklist

Use this after packaging and before public Cloud Run traffic.

## One-time GCP setup

- [ ] Enable APIs: Cloud Run, Cloud Build, Artifact Registry, Secret Manager, Cloud Storage
- [ ] Create Artifact Registry Docker repo (e.g. `resume`)
- [ ] Create GCS bucket for corpus; upload `resume.md` and `context/*.md`
- [ ] Create Secret Manager secret `openrouter-key` with OpenRouter API key
- [ ] Note the Cloud Run runtime service account; grant `roles/storage.objectViewer` on the bucket
- [ ] Set OpenRouter dashboard spend limit as a backstop

## Build & deploy

- [ ] `gcloud builds submit --config=cloudbuild.yaml`
- [ ] `gcloud run deploy` with secrets + `DOCUMENTS_GCS_BUCKET` + `RESUME_PUBLIC_URL`. Pin to `--max-instances=1`.
- [ ] Confirm `/q/health` returns UP
- [ ] Confirm `/q/health/ready` becomes UP after corpus ingestion
- [ ] Confirm `/` serves the Angular UI
- [ ] Confirm `POST /api/chat` streams SSE answers
- [ ] Confirm rate limit returns 429 after burst (optional load check)
- [ ] Confirm `/q/metrics` exposes `resume_*` Micrometer metrics

## Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `OPENROUTER_API_KEY` | Required for real answers; **required** in prod (no default) | — |
| `DOCUMENTS_GCS_BUCKET` | GCS bucket for corpus (required in prod) | — |
| `DOCUMENTS_GCS_CONTEXT_PREFIX` | Prefix for context docs | `context/` |
| `DOCUMENTS_GCS_CONTEXT_PREFIX_ENABLED` | Auto-ingest `.md` under prefix | prod: `true` |
| `RESUME_PUBLIC_URL` | Public site URL for OpenRouter `HTTP-Referer` | — |
| `RESUME_GLOBAL_DAILY_CAP` | Global chat cap across all clients | `500` |
| `LOG_LEVEL` | Application log level | `INFO` |
| `RESUME_SANITIZER_BLOCKED_PATTERNS` | Comma-separated regex blocklist for prompt injection | sensible default |

See `.env.example` for local copy-paste setup.

## Ops notes

- Corpus updates require uploading to GCS then restarting the revision (startup ingestion).
- Per-IP limits: 10/min and 100/day.
- Global daily cap: defaults to 500/day UTC (`RESUME_GLOBAL_DAILY_CAP`).
- **Single-instance caveat:** The global daily cap is stored in-memory and resets at UTC midnight. Always deploy with `--max-instances=1`. If you ever raise it, switch to an external counter (Redis, Firestore, etc.) before doing so.
- Prompt-injection defenses: regex blocklist in `QuestionSanitizer`, tightened system prompt, question length cap, and safe error response mapping.
- Observability: `/q/metrics` exposes `resume.chat.requests`, `resume.chat.errors`, `resume.chat.duration`, `resume.rate-limit.global-cap.exhausted`, and `resume.rate-limit.global-cap.remaining`.
- Native cold starts are usually fine with `--min-instances=0`; raise to 1 only if needed. Start with `--memory=512Mi`; raise to 1Gi if load tests show memory pressure.
