---
description: "Use when writing, modifying, or reviewing the AI provider layer (Google, OpenAI, Ollama, mock) or its tests. Covers the class layout, retry and timeout handling, configuration and keys."
paths:
  - "src/main/java/de/vptr/aimathtutor/service/ai/**"
  - "src/main/java/de/vptr/aimathtutor/exception/*ProviderException.java"
  - "src/main/java/de/vptr/aimathtutor/dto/{Google,OpenAi,Ollama}*Dto.java"
  - "src/test/java/de/vptr/aimathtutor/service/ai/**"
  - "src/test/java/de/vptr/aimathtutor/util/RetryAnnotationVerifier.java"
---

# AI Providers

- **Layout:** `service/ai/` holds the REST-calling services (`GoogleService`,
  `OpenAiService`, `OllamaService`) on the shared base
  `AbstractProviderService`. `service/ai/provider/` holds the
  `ProviderInterface` implementations: the Google, OpenAI and Ollama providers
  extend `AbstractProvider`, and `MockProvider` implements the interface
  directly.
- **Retry:** each content-generating method (`generateContent`,
  `generateJsonContent`) carries
  `@Retry(..., abortOn = NonRetryableProviderException.class)` with the
  `AppConstants.RETRY_*` values; health checks such as
  `OllamaService.isAvailable()` do not retry. Throw `ProviderException` for a
  transient failure and `NonRetryableProviderException` for one a retry cannot
  fix (missing key, blocked content, misconfiguration). Which HTTP statuses
  count as transient is open in #199.
- **Timeouts** are set on the JAX-RS client that `AbstractProviderService`
  builds, from `getConnectTimeoutSeconds()`/`getReadTimeoutSeconds()`; the
  code uses no `@Timeout`.
- **Test profile:** `application.properties` zeroes `maxRetries` and `delay`
  per method under `%test.<fully.qualified.Class>/<method>/Retry/`. A new
  `@Retry` method needs its own pair there, or its failing tests wait out every
  retry.
- **Configuration:** model, temperature, max tokens and prompts are DB-backed
  through `AiConfigService` (Admin Settings UI); API keys come only from
  properties (see `AGENTS.md`, "AI Configuration") and never appear in logs or
  error messages.
