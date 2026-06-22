# Phase 1 — JVM-mode skeleton (checklist)

Goal: a **Quarkus JVM** app with **`resume.md` on the classpath**, **local BGE embeddings**, **in-memory vector store**, **non-streaming** `POST /api/chat`, **no UI**. Validate behavior with **curl** and **automated tests** where practical.

Reference: `quarkus-rag-resume-plan.md` (Phase 1 section and §4 backend, §8 input cap, §9 testing).

**Delivered in repo:** Quarkus Maven project, embedding producers, startup ingestion, RAG augmentor, AI service interface, JSON chat resource, sample `resume.md` / `prompts/system.txt`, `application.properties` template, unit test for chunking, optional IT gated on `OPENROUTER_API_KEY`.

---

## Prerequisites

- [ ] JDK **21** installed (`java -version`).
- [ ] **OpenRouter** account and API key for manual smoke tests (optional for CI if tests skip LLM calls).
- [ ] Read **§11** in the plan (model, resume structure, assistant voice, refusals) and align `resume.md` + `prompts/system.txt`.

---

## Repository and build

- [ ] `./mvnw verify` passes locally (compile + unit tests; full-stack IT skips without `OPENROUTER_API_KEY`; Quarkus still needs a **non-empty** `quarkus.langchain4j.openai.api-key` at startup — the repo default is a placeholder until you export a real key).

---

## Configuration (`src/main/resources/application.properties`)

- [ ] Set OpenRouter **`base-url`**, **`api-key`** from **`OPENROUTER_API_KEY`**, chat model name, temperature, timeout (defaults are present; replace placeholder **Referer** with your URL).
- [ ] Confirm **`quarkus.langchain4j.openai.embedding-model.enabled=false`** (local ONNX embeddings only).
- [ ] (Optional this phase) **`quarkus.native.resources.includes=resume.md,prompts/*.txt`** for later native builds.

---

## Content

- [ ] Replace **`src/main/resources/resume.md`** with your real resume; use **`##` per role/section** for cleaner chunks (plan §11).
- [ ] Tune **`src/main/resources/prompts/system.txt`** (voice, “context only”, refusals).

---

## Backend wiring (verify / extend)

- [ ] Confirm **`ResumeEmbeddingProducer`**, **`RagBootstrap`**, **`ResumeRetrievalAugmentor`**, **`ResumeAssistant`**, **`ChatResource`** match your naming and RAG tuning needs.
- [ ] After smoke tests, tune **`minScore`** / **`maxResults`** in **`ResumeRetrievalAugmentor`** if retrieval is too loose or too strict.

---

## Manual verification

- [ ] Export **`OPENROUTER_API_KEY`**, run **`./mvnw quarkus:dev`**.
- [ ] **`curl`** against **`/api/chat`**: on-topic question, off-topic / not-in-resume question, blank body, **>500** chars (expect **400**).
- [ ] Inspect logs: no API key material; optional: log **question** + **chunk count** only (plan §8.7).

---

## Automated tests

- [ ] Run **`./mvnw test`** after content changes (chunking unit test should stay green).
- [ ] With **`OPENROUTER_API_KEY`** set, run tests so **`ChatResourceIT`** exercises the full stack.

---

## Explicitly not in Phase 1

- [ ] No **Quinoa** / **Angular**; no **SSE** / **`Multi<String>`**.
- [ ] No **native image** or **Cloud Run** (Phases 3–4).
- [ ] No **Bucket4j** / daily cap (Phase 5) unless you deliberately pull forward a small slice.

---

## Phase 1 “done” criteria

1. **`./mvnw test`** (and ideally **`verify`**) succeeds.
2. With a valid key, **`curl`** returns sensible **grounded** answers and honest **“not in resume”** behavior when appropriate.
3. You can explain **ingestion vs retrieval**, **why local embeddings**, and **what Phase 2** adds (streaming + UI).
