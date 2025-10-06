# OpenAI Integration Research & Implementation

## OpenAI GPT Models Overview

### Available Models

#### GPT-4o (Recommended for Production)

- **Model Name:** `gpt-4o`
- **Context Window:** 128,000 tokens
- **Max Output:** 16,384 tokens
- **Best For:** Complex reasoning, accurate math tutoring
- **Cost:**
  - Input: $2.50 per 1M tokens
  - Output: $10.00 per 1M tokens

#### GPT-4o-mini (Recommended for Development/Budget)

- **Model Name:** `gpt-4o-mini`
- **Context Window:** 128,000 tokens
- **Max Output:** 16,384 tokens
- **Best For:** Cost-effective tutoring, still highly capable
- **Cost:**
  - Input: $0.150 per 1M tokens
  - Output: $0.600 per 1M tokens

#### GPT-3.5-turbo (Budget Option)

- **Model Name:** `gpt-3.5-turbo`
- **Context Window:** 16,385 tokens
- **Max Output:** 4,096 tokens
- **Cost:**
  - Input: $0.50 per 1M tokens
  - Output: $1.50 per 1M tokens

### Key Features

- ✅ **Function Calling:** Built-in structured outputs
- ✅ **JSON Mode:** Guaranteed valid JSON responses
- ✅ **Streaming:** Real-time response streaming
- ✅ **Vision:** Image understanding (GPT-4o models)
- ✅ **Consistent:** Very stable and reliable
- ✅ **Well-Documented:** Extensive documentation

## Cost Comparison

### Monthly Cost for 30 Students (12,000 requests)

**GPT-4o-mini (Recommended):**

- 12,000 requests × 700 tokens = 8.4M tokens
- Input: 8.4M × $0.150/1M = $1.26
- Output: 8.4M × $0.600/1M = $5.04
- **Total: ~$6.30/month**

**GPT-4o (Premium):**

- Input: 8.4M × $2.50/1M = $21.00
- Output: 8.4M × $10.00/1M = $84.00
- **Total: ~$105/month**

**GPT-3.5-turbo (Budget):**

- Input: 8.4M × $0.50/1M = $4.20
- Output: 8.4M × $1.50/1M = $12.60
- **Total: ~$16.80/month**

**Comparison with Gemini Flash:**

- Gemini 1.5 Flash: **FREE** (up to 1,500 requests/day)
- Gemini paid: ~$3.15/month

## API Integration

### Authentication

- Uses API keys (starts with `sk-...`)
- Key management via platform.openai.com
- Organization ID optional

### Rate Limits (Tier 1 - New Users)

- **RPM (Requests Per Minute):** 500
- **TPM (Tokens Per Minute):** 30,000
- **TPD (Tokens Per Day):** No limit

Higher tiers available with usage history.

### Endpoints

- Base URL: `https://api.openai.com/v1`
- Chat Completions: `/chat/completions`
- Models: `/models`

## Implementation Strategy

### Dependencies

Using JAX-RS client (already available in Quarkus):

```xml
<!-- No additional dependencies needed! -->
<!-- quarkus-rest-client-jackson already included -->
```

### Configuration

```properties
openai.api.key=${OPENAI_API_KEY:your-api-key-here}
openai.model=gpt-4o-mini
openai.api.base-url=https://api.openai.com/v1
openai.temperature=0.7
openai.max-tokens=1000
openai.organization-id=  # Optional
```

### JSON Mode

OpenAI supports forcing JSON responses:

```json
{
  "model": "gpt-4o-mini",
  "response_format": { "type": "json_object" },
  "messages": [...]
}
```

This guarantees valid JSON output!

## Advantages vs Gemini

**OpenAI Advantages:**

- ✅ More stable/consistent (longer track record)
- ✅ Better function calling support
- ✅ JSON mode guarantees valid JSON
- ✅ Better for complex reasoning
- ✅ More control over system prompts

**Gemini Advantages:**

- ✅ FREE tier (1,500 requests/day)
- ✅ Cheaper even when paid
- ✅ Longer context window (1M tokens vs 128k)
- ✅ Faster (Flash model optimized for speed)

## Use Cases

**Use OpenAI when:**

- Need guaranteed JSON responses
- Require maximum accuracy for complex problems
- Budget allows for premium service
- Need extensive function calling
- Require image/vision capabilities

**Use Gemini when:**

- Budget-conscious or starting out (free tier)
- Speed is priority
- Simple to moderate complexity
- Free tier limits are sufficient

## Getting Started

1. Sign up at [platform.openai.com](https://platform.openai.com)
2. Add payment method (required even for API testing)
3. Generate API key
4. Set usage limits (recommended: $10-50/month)
5. Use API key in application

## Security Best Practices

1. ✅ Never commit API keys to Git
2. ✅ Use environment variables
3. ✅ Set usage limits in OpenAI dashboard
4. ✅ Monitor usage regularly
5. ✅ Rotate keys periodically
6. ✅ Use project-specific keys
7. ✅ Enable MFA on OpenAI account

## Resources

- **Platform:** [https://platform.openai.com](https://platform.openai.com)
- **API Documentation:** [https://platform.openai.com/docs](https://platform.openai.com/docs)
- **Pricing:** [https://openai.com/api/pricing](https://openai.com/api/pricing)
- **Status:** [https://status.openai.com](https://status.openai.com)
- **API Keys:** [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)

## Recommendation

**For AIMathTutor:**

- **Development/Testing:** Use Gemini Flash (free tier)
- **Small Classrooms (<30 students):** Use Gemini Flash (free tier sufficient)
- **Medium Classrooms (30-100 students):** Use GPT-4o-mini (~$20-60/month)
- **Large Schools/Districts:** Use GPT-4o-mini with volume pricing
- **Premium Features:** Use GPT-4o if budget allows and accuracy is critical
