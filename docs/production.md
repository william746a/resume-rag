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
- [ ] `gcloud run deploy` with secrets + `DOCUMENTS_GCS_BUCKET` + `RESUME_PUBLIC_URL`
- [ ] Confirm `/q/health` returns UP
- [ ] Confirm `/` serves the Angular UI
- [ ] Confirm `POST /api/chat` streams SSE answers
- [ ] Confirm rate limit returns 429 after burst (optional load check)

## Ops notes

- Corpus updates require uploading to GCS then restarting the revision (startup ingestion).
- Per-IP limits: 10/min and 100/day; global daily cap defaults to 500 (`RESUME_GLOBAL_DAILY_CAP`).
- Native cold starts are usually fine with `--min-instances=0`; raise to 1 only if needed.
