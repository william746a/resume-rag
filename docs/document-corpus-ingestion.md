# Document corpus ingestion

The resume RAG app ingests a **document corpus** at startup: one required **resume** document plus optional **context** documents (project write-ups, writing samples, FAQs). Each document can be loaded from the **classpath** or **Google Cloud Storage (GCS)**.

## Classpath layout (local dev)

```
src/main/resources/
├── resume.md
├── context/
│   ├── writing-samples.md
│   └── project-migration.md
└── prompts/system.txt
```

With `quarkus dev` (`%dev` profile), supplemental context files under `context/` are loaded automatically. Tests and non-dev runs use the resume only unless additional sources are configured.

## Configuration

### Per-document sources

```properties
documents.sources[0].id=resume
documents.sources[0].role=resume
documents.sources[0].source=classpath
documents.sources[0].classpath.path=resume.md

documents.sources[1].id=writing-samples
documents.sources[1].role=context
documents.sources[1].source=classpath
documents.sources[1].classpath.path=context/writing-samples.md
```

GCS-backed entry:

```properties
documents.sources[0].id=resume
documents.sources[0].role=resume
documents.sources[0].source=gcs
documents.sources[0].gcs.bucket=${DOCUMENTS_GCS_BUCKET}
documents.sources[0].gcs.object=resume.md
```

Global GCS defaults:

```properties
documents.gcs.bucket=${DOCUMENTS_GCS_BUCKET:}
documents.gcs.context-prefix=${DOCUMENTS_GCS_CONTEXT_PREFIX:context/}
documents.gcs.context-prefix-enabled=${DOCUMENTS_GCS_CONTEXT_PREFIX_ENABLED:false}
```

When `context-prefix-enabled=true`, all `.md` objects under the prefix are ingested as `role=context`. Objects already listed in `documents.sources[]` are skipped.

### Validation

Startup fails if:

- The corpus does not contain exactly one `role=resume` document
- Any document `id` is duplicated
- A classpath path or GCS object is missing or empty

## GCS bucket layout (production)

```
gs://<bucket>/resume.md
gs://<bucket>/context/writing-samples.md
gs://<bucket>/context/project-migration.md
gs://<bucket>/context/faq.md
```

Example production config:

```properties
documents.gcs.bucket=resume-rag-docs-prod

documents.sources[0].id=resume
documents.sources[0].role=resume
documents.sources[0].source=gcs
documents.sources[0].gcs.object=resume.md

documents.gcs.context-prefix=context/
documents.gcs.context-prefix-enabled=true
```

Environment variables:

| Variable | Purpose |
|---|---|
| `DOCUMENTS_GCS_BUCKET` | Default bucket for GCS sources and prefix expansion |
| `DOCUMENTS_GCS_CONTEXT_PREFIX` | Prefix for auto-discovered context docs (default `context/`) |
| `DOCUMENTS_GCS_CONTEXT_PREFIX_ENABLED` | Enable prefix expansion (`true` / `false`) |

## IAM (Cloud Run)

1. Create a GCS bucket for the corpus.
2. Grant the Cloud Run runtime service account **`roles/storage.objectViewer`** on that bucket (or a narrower custom role with `storage.objects.get` and `storage.objects.list`).
3. Do not embed service-account JSON in the container; use Application Default Credentials on Cloud Run.

Local GCS testing:

```bash
gcloud auth application-default login
export DOCUMENTS_GCS_BUCKET=your-bucket
# configure documents.sources[] for gcs and run the app
```

## Updating content

Ingestion runs at **startup only**. After uploading new markdown to GCS, restart the Cloud Run revision (or restart the local JVM) to rebuild the in-memory embedding store.

## Chunk metadata

Each embedded segment is tagged with:

- `document_id` — configured document id
- `document_role` — `resume` or `context`
- `source_uri` — e.g. `classpath:resume.md` or `gcs://bucket/context/faq.md`
