---
description: "Use when writing, modifying, or reviewing the AI provider layer (Google, OpenAI, Ollama, mock) or its tests. Covers the class layout, retry and timeout handling, configuration and keys."
paths:
  - "src/main/java/de/vptr/aimathtutor/service/ai/**"
  - "src/main/java/de/vptr/aimathtutor/exception/*ProviderException.java"
  - "src/test/java/de/vptr/aimathtutor/service/ai/**"
---

# AI Providers

- **Layout:** `service/ai/` holds the REST-calling services (`GoogleService`,
  `OpenAiService`, `OllamaService`) on the shared base
  `AbstractProviderService`; `service/ai/provider/` holds the
  `ProviderInterface` implementations on `AbstractProvider`, including
  `MockProvider`.
- **Retry:** each REST-calling method carries
  `@Retry(..., abortOn = NonRetryableProviderException.class)` with the
  `AppConstants.RETRY_*` values. Throw `ProviderException` for a transient
  failure and `NonRetryableProviderException` for one a retry cannot fix
  (missing key, blocked content, misconfiguration). Which HTTP statuses count
  as transient is open in #199.
- **Timeouts** are set on the JAX-RS client that `AbstractProviderService`
  builds, from `getConnectTimeoutSeconds()`/`getReadTimeoutSeconds()`; the
  code uses no `@Timeout`.
- **Test profile:** `application.properties` zeroes `maxRetries` and `delay`
  per method under `%test.<class>/<method>/Retry/`. A new `@Retry` method
  needs its own pair there, or its failing tests wait out every retry.
- **Configuration:** model, temperature, max tokens and prompts are DB-backed
  through `AiConfigService` (Admin Settings UI); API keys come only from
  properties (see `AGENTS.md`, "AI Configuration") and never appear in logs
  or error messages.
