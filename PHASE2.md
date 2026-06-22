# Phase 2 — Streaming + Angular UI (checklist)

Goal: **token streaming** from the LLM to the browser over **SSE**, plus an **Angular** UI built into the Quarkus app via **Quinoa**. Same RAG stack as Phase 1; this phase is mostly **transport + UX**.

Reference: `quarkus-rag-resume-plan.md` (Phase 2, §1 architecture, §2 stack, §6 frontend, §12 gotchas).

**Delivered in repo:** `quarkus-quinoa`, `ResumeAssistant` returns `Multi<String>`, `POST /api/chat` as **SSE** (`text/event-stream`), Angular `webui` with **chat** screen, **fetch + stream reader** client (POST + JSON body; `EventSource` is GET-only), **ngx-markdown** for rendered replies, suggested-question chips + transparency footer, Quinoa paths aligned with `ng build` output under `dist/webui/browser`, **`quarkus.quinoa.package-manager-install.node-version`** set so **`mvn package`** can install Node when needed.

---

## Prerequisites

- [ ] Phase 1 path complete: RAG ingestion, OpenRouter config, **`OPENROUTER_API_KEY`** for real answers.
- [ ] **Node.js 20+** and **npm** (Quinoa runs `npm install` / `npm run build` during `mvn package` when enabled).
- [ ] Read plan **§6** (UI polish) and **§12** (Quinoa + native build order later).

---

## Backend

- [ ] **`ResumeAssistant`**: streaming method returns **`io.smallrye.mutiny.Multi<String>`** (Quarkus LangChain4j convention).
- [ ] **`ChatResource`**: **`POST /api/chat`**, **`@Consumes(APPLICATION_JSON)`**, **`@Produces(SERVER_SENT_EVENTS)`**, **`@RestStreamElementType(TEXT_PLAIN)`**, validate question (blank / **500** char cap) then return **`Multi<String>`**.
- [ ] **Smoke with curl**: `curl -N -H 'Content-Type: application/json' -d '{"question":"..."}' http://localhost:8080/api/chat` and confirm **`data:`** lines stream in.

---

## Quinoa + build

- [ ] **`quarkus-quinoa`** on the classpath (version compatible with your Quarkus stream).
- [ ] **`application.properties`**: `quarkus.quinoa.ui-dir=src/main/webui`, `quarkus.quinoa.build-dir=dist/webui/browser`, `quarkus.quinoa.package-manager-install=true` (if true, set **`quarkus.quinoa.package-manager-install.node-version`** — required by Quinoa when it installs Node), `quarkus.quinoa.enable-spa-routing=true` for Angular deep links.
- [ ] **`angular.json`**: `outputPath` / browser output matches **`quarkus.quinoa.build-dir`** (this repo: **`dist/webui`** → **`dist/webui/browser`**).
- [ ] **`./mvnw package`** runs the UI build and includes assets in the Quarkus artifact (verify `target/` contains Quinoa output when enabled).

---

## Angular UI

- [ ] **Dev server proxy**: with `ng serve` on **:4200**, relative `/api/...` calls would hit the wrong port; `proxy.conf.json` forwards **`/api` → `http://localhost:8080`**, and `angular.json` **`serve.options.proxyConfig`** enables it. Prefer **`http://localhost:8080`** when using **`quarkus dev`** so Quinoa proxies the UI and APIs share one origin.
- [ ] **Routing**: default route loads the **chat** experience.
- [ ] **Streaming client**: **`fetch`** + **`ReadableStream`** reading **`text/event-stream`**; append token text to the active assistant message (do not use `EventSource` for POST bodies).
- [ ] **Markdown**: **`ngx-markdown`** (or equivalent) so lists/code/bold render after streaming settles (or live-update if performance is acceptable).
- [ ] **Suggested questions** (chips) wired to the same send path as the text box.
- [ ] **Footer**: short note that answers are **grounded in `resume.md`** + **last updated** date you maintain.
- [ ] **Optional**: **`public/resume.pdf`** + “Download PDF” link (plan §6); add the real file when ready.

---

## Tests and CI

- [ ] **`./mvnw test`**: unit tests unchanged; **`ChatResourceIT`** updated for **SSE** (or split JSON vs stream tests clearly).
- [ ] **`%test` Quinoa**: if tests fail because the UI is not built, either run **`npm run build`** in `src/main/webui` before tests or set **`%test.quarkus.quinoa.enabled=false`** (exact property name per Quinoa version) so IT focuses on the API.

---

## Explicitly still out of scope (later phases)

- [ ] **Native image** tuning and **Quinoa-before-native** verification (Phase 3 / plan §12).
- [ ] **Cloud Run**, secrets, domain (Phase 4).
- [ ] **Rate limits / daily caps** (Phase 5).

---

## Phase 2 “done” criteria

1. **`./mvnw quarkus:dev`**: SPA loads at **`/`**, questions stream into the page, errors show sensibly.
2. **`./mvnw package`**: production bundle is embedded; hitting **`/`** on the runnable jar serves the UI and **`/api/chat`** streams.
3. You can explain why the client uses **fetch streaming** instead of **`EventSource`** for this POST API.
