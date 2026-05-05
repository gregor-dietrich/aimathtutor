package de.vptr.aimathtutor.service.ai;

import de.vptr.aimathtutor.service.AiConfigService;

/**
 * Constants for the {@code ai_config} configuration keys persisted via
 * {@link AiConfigService}. Centralised here so
 * services, the admin UI, and seed data all reference identical strings.
 */
public final class AiConfigKeys {

    private AiConfigKeys() {
        // Constants holder
    }

    // General tutor settings
    public static final String AI_TUTOR_ENABLED = "ai.tutor.enabled";
    public static final String AI_TUTOR_PROVIDER = "ai.tutor.provider";

    // Per-provider config key suffixes
    public static final String SUFFIX_MODEL = ".model";
    public static final String SUFFIX_TEMPERATURE = ".temperature";
    public static final String SUFFIX_MAX_TOKENS = ".max-tokens";
    public static final String SUFFIX_API_BASE_URL = ".api.base-url";

    // Gemini
    public static final String GEMINI_PREFIX = "gemini";
    public static final String GEMINI_MODEL = GEMINI_PREFIX + SUFFIX_MODEL;
    public static final String GEMINI_API_BASE_URL = GEMINI_PREFIX + SUFFIX_API_BASE_URL;
    public static final String GEMINI_TEMPERATURE = GEMINI_PREFIX + SUFFIX_TEMPERATURE;
    public static final String GEMINI_MAX_TOKENS = GEMINI_PREFIX + SUFFIX_MAX_TOKENS;

    // OpenAI
    public static final String OPENAI_PREFIX = "openai";
    public static final String OPENAI_MODEL = OPENAI_PREFIX + SUFFIX_MODEL;
    public static final String OPENAI_API_BASE_URL = OPENAI_PREFIX + SUFFIX_API_BASE_URL;
    public static final String OPENAI_TEMPERATURE = OPENAI_PREFIX + SUFFIX_TEMPERATURE;
    public static final String OPENAI_MAX_TOKENS = OPENAI_PREFIX + SUFFIX_MAX_TOKENS;
    public static final String OPENAI_ORGANIZATION_ID = OPENAI_PREFIX + ".organization-id";

    // Ollama
    public static final String OLLAMA_PREFIX = "ollama";
    public static final String OLLAMA_API_URL = OLLAMA_PREFIX + ".api.url";
    public static final String OLLAMA_MODEL = OLLAMA_PREFIX + SUFFIX_MODEL;
    public static final String OLLAMA_TEMPERATURE = OLLAMA_PREFIX + SUFFIX_TEMPERATURE;
    public static final String OLLAMA_MAX_TOKENS = OLLAMA_PREFIX + SUFFIX_MAX_TOKENS;
    public static final String OLLAMA_TIMEOUT_SECONDS = OLLAMA_PREFIX + ".timeout-seconds";

    // Prompts
    public static final String PROMPT_QUESTION_PREFIX = "ai.prompt.question.answering.prefix";
    public static final String PROMPT_QUESTION_POSTFIX = "ai.prompt.question.answering.postfix";
    public static final String PROMPT_TUTORING_PREFIX = "ai.prompt.math.tutoring.prefix";
    public static final String PROMPT_TUTORING_POSTFIX = "ai.prompt.math.tutoring.postfix";
}
