# OpenRouter Model Analysis for Resume RAG

## Purpose

This document captures candidate OpenRouter chat models for the resume-grounded Q&A application and proposes a cost-conscious strategy that preserves answer accuracy.

Current implementation context:

- Chat endpoint streams responses via SSE.
- Retrieval is local and deterministic (in-memory store + local BGE embeddings).
- System prompt enforces strict grounding in retrieved resume context.
- Current configured chat model is `anthropic/claude-3.5-haiku`.

Because retrieval quality is already controlled in-app, model choice primarily affects:

- instruction adherence,
- factual caution when context is weak,
- cost per request,
- latency and user-perceived responsiveness.

## Candidate Models for Consideration

### 1) `google/gemini-2.5-flash` (recommended primary)

Why consider it:

- Strong quality-per-cost profile for grounded QA.
- Good instruction following for constrained prompts.
- Typically fast enough for a streaming chat UX.

Trade-offs:

- Output style can vary by provider route/versioning behavior.
- May still require escalation on nuanced multi-part questions.

Best use:

- Default model for most production traffic.

### 2) `openai/gpt-4.1-mini` (recommended alternate primary)

Why consider it:

- Consistently strong compact-model behavior for concise factual answers.
- Good balance between quality, speed, and spend for recruiter-style Q&A.

Trade-offs:

- May be more expensive than the cheapest flash-class alternatives.
- Some edge prompts can still benefit from a larger fallback model.

Best use:

- Alternate default if response style or empirical accuracy is preferred in testing.

### 3) `anthropic/claude-3.7-sonnet` (recommended escalation model)

Why consider it:

- Very strong reasoning and refusal behavior when context is incomplete.
- Better performance on complex or ambiguous user questions.

Trade-offs:

- Higher cost than mini/flash-tier models.
- Should not be the default for all traffic if cost efficiency matters.

Best use:

- Escalation path when retrieval confidence is borderline or the question is complex.

### 4) `anthropic/claude-3.5-haiku` (current baseline)

Why keep it in evaluation:

- Low-cost baseline and easy drop-in given current config.
- Useful for measuring quality uplift from alternative primaries.

Trade-offs:

- More likely to need careful prompt constraints on difficult questions.
- May underperform the newer flash/mini options on accuracy-sensitive prompts.

Best use:

- Budget or fallback baseline; not ideal as sole model if accuracy is paramount.

## Recommended Deployment Strategy

Use a two-tier model policy:

1. Primary model: `google/gemini-2.5-flash` (or `openai/gpt-4.1-mini` if it wins A/B tests).
2. Escalation model: `anthropic/claude-3.7-sonnet`.

Escalate when one or more of the following is true:

- Retrieval confidence is near threshold (for example, top score close to `minScore=0.55`).
- User asks a multi-part or high-precision comparison question.
- Initial answer is "insufficient context" but retrieved snippets suggest partial support.

This keeps average spend low while protecting answer quality for hard cases.

## Accuracy-First Guardrails (Model-Agnostic)

Regardless of chosen model, maintain these controls:

- Keep strict grounding instructions in system prompt.
- Preserve "do not invent employers/dates/credentials" constraints.
- Prefer concise answers with explicit uncertainty when support is missing.
- Reject or truncate overly long user inputs before model invocation.
- Log retrieval score bands and escalation rate for monitoring quality drift.

## Evaluation Plan Before Final Model Selection

Run a small deterministic benchmark (20-30 representative questions):

- 10 straightforward factual prompts (roles, skills, dates).
- 10 synthesis prompts (compare experiences, summarize strengths).
- 5-10 adversarial prompts (requests to ignore system rules, unsupported claims).

Track:

- Grounded accuracy (supported by retrieved context).
- Hallucination/refusal quality.
- Cost per answer.
- P50/P95 latency.
- User-facing answer quality rating.

Decision rule:

- Select the lowest-cost model that meets target grounded-accuracy and refusal criteria.
- Keep Sonnet-class model as escalation if it materially improves hard-query accuracy.

## Initial Recommendation

For immediate next-step evaluation:

- Primary candidate: `google/gemini-2.5-flash`
- Alternate primary: `openai/gpt-4.1-mini`
- Escalation candidate: `anthropic/claude-3.7-sonnet`
- Baseline comparator: `anthropic/claude-3.5-haiku`

This set provides a practical spread across cost and accuracy tiers while staying compatible with the existing OpenRouter-backed architecture.
