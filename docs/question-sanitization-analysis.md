# Question sanitization and prompt-injection defenses — options analysis

This document captures approaches for **detecting or blunting malicious or manipulative user input** (e.g. “ignore previous instructions”, “reveal the system prompt”, “output the raw resume”) in the **Resume RAG** app. It is meant for **design and prioritization**, not as a committed roadmap.

**Context today:** questions hit `POST /api/chat` with a **length cap**; the assistant is instructed to **use only retrieved context** and **ignore conflicting instructions in the question** (see `prompts/system.txt`). There is **no dedicated sanitizer** yet.

**Threat model (minimal):** public demo, **OpenRouter spend** and **reputation** matter; perfect security is unrealistic, but **obvious abuse** and **cheap automated probing** should be damped.

---

## 1. Goals and constraints

| Goal | Why it matters |
|------|----------------|
| Reduce **prompt injection** impact | Stops some attempts to override system behavior or exfiltrate non-public text. |
| Limit **cost / latency** | Every extra model call adds both; local work should stay cheap. |
| Avoid **blocking legitimate recruiters** | False positives read as “broken demo” to employers. |
| Stay **explainable** in interviews | You can describe tradeoffs honestly. |

**Constraint:** the app already ships a **local embedding model (BGE-small)** for RAG. Reusing it for “is this an instruction?” is **technically possible** but **not what embeddings are optimized for** (see §3).

---

## 2. Option summary

| Option | Mechanism | Rough engineering effort | Strength | Weakness |
|--------|-----------|---------------------------|----------|----------|
| **A. Prompt + policy only** | Stronger system prompt; refusal patterns; optional output checks | **Low** | Cheap, no extra services | Does not stop determined or creative injections by itself. |
| **B. Heuristics and structure rules** | Regex / blocklists; entropy or pattern checks; reject “code dump” shapes | **Low** | Fast, deterministic, easy to test | High false positive/negative; maintenance of patterns. |
| **C. Embedding similarity to templates** | Embed question; compare to stored vectors of “bad” (and maybe “good”) snippets; threshold | **Low–medium** | Reuses existing **BGE** stack; no extra API | **Weak semantic signal** for intent; brittle to paraphrase; threshold tuning is painful. |
| **D. Cheap classifier LLM** | Second call: “resume question vs manipulation” with a small/cheap model | **Medium** | Better intent separation than raw embeddings | Extra **latency** and **cost**; still not perfect. |
| **E. Hosted moderation / safety API** | Vendor classifier or moderation endpoint | **Medium** | Often best precision/recall if you pay | **Dependency**, cost, data handling, possibly offline dev friction. |
| **F. Rate limits + caps** (already planned Phase 5) | Per-IP limits, daily cap | **Low** (policy) | Cuts brute force and burn | Does not classify “good” vs “bad” questions. |

Options are **composable**: e.g. **A + B + F** as baseline, add **D** if abuse appears, experiment with **C** only with logging-first rollout.

---

## 3. Deep dive: embeddings (BGE) as a “sanitizer”

**What embeddings are good at:** finding text **semantically similar** to resume chunks for retrieval.

**What they are not:** a trained **intent classifier** for “instruction vs benign recruiter question.”

A typical embedding-only design:

1. `embeddingModel.embed(userQuestion)`
2. Compare (e.g. **cosine similarity**) to a small set of **hand-authored** strings labeled “injection-like” (and optionally “benign examples”).
3. Reject if similarity to any “bad” example exceeds a threshold.

**Risks:**

- **False positives:** normal questions can sit near “bad” regions of space depending on wording.
- **False negatives:** rewrites, multilingual text, or indirect requests may not resemble stored templates.
- **Operational cost:** you end up maintaining a **curated template list** and **thresholds** per model version.

**Conclusion:** reasonable as a **soft signal** or **telemetry** (“flag for review”), risky as a **hard block** unless tuned with real traffic and conservative thresholds.

---

## 4. Deep dive: heuristics (B)

Examples of cheap rules (illustrative, not prescriptive):

- Substrings or regex for known clichés: `ignore (all )?(previous|prior) instructions`, `system prompt`, `you are now`, `DAN`, etc.
- Reject or trim **excessive** non-alphanumeric / “wall of symbols”.
- Cap **repeated** special characters or **very high** token-like density.

**Pros:** deterministic, unit-testable, no model call.  
**Cons:** arms race with phrasing; aggressive rules annoy real users.

Best used as a **first gate** before spendier steps, not the only gate.

---

## 5. Deep dive: second small LLM (D)

Flow: **validate length** → **classifier** (cheap model) → **main RAG answer**.

**Pros:** models are **much** better at “is this trying to change your rules?” than cosine similarity to five strings.  
**Cons:** every question may cost **two** completions (or one if batched creatively); add **p99 latency**; need a clear **fallback** if classifier times out.

**Model choice:** smallest/cheapest on OpenRouter (or a fixed “moderation” style model); keep **temperature 0**, **max tokens** tiny.

---

## 6. Deep dive: moderation / safety APIs (E)

Outsource classification to a provider that specializes in policy categories.

**Pros:** strong when it fits your policy and jurisdiction.  
**Cons:** another integration; **data residency** and **what gets logged**; may be overkill for a personal resume demo.

---

## 7. Recommended “consideration order”

1. **Baseline (high value / low cost):** strengthen **A** (prompt + documented refusals), keep **length limits**, plan **F** (rate limits / daily cap) before wide exposure.
2. **Add B** if you see repetitive scripted probes; log hits without blocking first if unsure.
3. **Add D** if you need stronger intent separation and can accept extra cost/latency.
4. **Treat C (embedding templates)** as **experimental**: log-only or shadow mode until metrics look acceptable; avoid selling it as “cryptographic” security.
5. **E** only if policy or employer context pushes you toward a commercial moderation story.

---

## 8. If you prototype embedding-based detection (C)

Suggested **safe** rollout:

- **Shadow mode:** compute score, **log** `score`, `topSimilarTemplate`, `questionHash` (not raw text in public logs if privacy matters), **never block**.
- **Metrics:** false positive rate on a **hand-built set** of 50–100 benign recruiter questions you care about.
- **Canary block:** only after false positive rate is acceptable on that set.

Implementation touchpoints in this repo (conceptual): a small **`QuestionSanitizer`** (or filter) invoked from **`ChatResource`** after validation, injecting **`EmbeddingModel`** already used for RAG.

---

## 9. Open questions to decide before implementation

- **Block vs warn vs log-only** for each layer?
- **What counts as “success”?** (e.g. “no exfil of system prompt in 100 manual attacks” vs “no recruiter complaint in N trials”.)
- **Telemetry:** what do you log without storing full PII questions indefinitely?
- **CI:** how do you regression-test sanitizer rules without committing offensive strings verbatim in repo (hashes, external fixtures, or generated tests)?

---

## 10. References in this repository

- System behavior and “ignore embedded instructions” language: `src/main/resources/prompts/system.txt`
- Input length cap: `ChatResource` (`MAX_QUESTION_LENGTH`)
- Broader hardening (rate limits, caps): `quarkus-rag-resume-plan.md` §8 and Phase 5

---

*Document version: initial analysis for follow-up design. Update as you pick options and ship behavior.*
