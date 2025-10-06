# Phase 4: Gemini AI Integration Research & Implementation

## Gemini 1.5 Flash Overview

### Model Information

- **Model Name:** `gemini-1.5-flash`
- **Provider:** Google AI
- **Free Tier:** ✅ Available via Google AI Studio
- **Rate Limits (Free Tier):**
  - 15 requests per minute (RPM)
  - 1 million tokens per minute (TPM)
  - 1,500 requests per day (RPD)
- **Pricing (Paid):**
  - Input: $0.075 per 1M tokens (up to 128k context)
  - Output: $0.30 per 1M tokens
  - Very affordable for educational use

### Key Features

- **Fast:** Optimized for speed
- **Multimodal:** Text, images, video, audio
- **Long Context:** Up to 1M tokens context window
- **Function Calling:** Supports structured outputs
- **JSON Mode:** Can output structured JSON
- **Safety Settings:** Configurable content filtering

## API Integration Options

### Option 1: Google AI Java SDK (Recommended)

```xml
<dependency>
    <groupId>com.google.ai.generativelanguage</groupId>
    <artifactId>generativelanguage</artifactId>
    <version>0.3.0</version>
</dependency>
```

### Option 2: Vertex AI Java SDK (For GCP Integration)

```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-aiplatform</artifactId>
    <version>3.36.0</version>
</dependency>
```

### Option 3: REST API (Simplest, No Dependencies)

Direct HTTP calls to: `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent`

## Implementation Strategy

### Step 1: Add Dependencies (Using REST API)

We'll use Quarkus REST Client for simplicity - no new dependencies needed!

### Step 2: Configuration Properties

```properties
# Gemini AI Configuration
gemini.api.key=${GEMINI_API_KEY:your-api-key-here}
gemini.model=gemini-1.5-flash
gemini.api.base-url=https://generativelanguage.googleapis.com/v1beta
gemini.temperature=0.7
gemini.max-tokens=1000
```

### Step 3: Create Gemini Client Service

- REST client interface using Quarkus REST Client
- Request/Response DTOs for Gemini API
- Error handling and retry logic

### Step 4: Update AITutorService

- Add `analyzeWithGemini()` method
- Create math-tutoring specific prompts
- Switch from mock AI to Gemini based on configuration

### Step 5: Prompt Engineering

Design effective prompts for math tutoring:

```
You are an AI math tutor helping a student learn algebra. The student just performed the following action:

Action: {eventType}
Expression Before: {expressionBefore}
Expression After: {expressionAfter}

Analyze this step and provide feedback:
1. Is the step mathematically correct?
2. Provide encouraging feedback if correct, or gentle correction if wrong
3. Suggest the next logical step
4. If applicable, offer a helpful hint

Respond in JSON format:
{
  "type": "POSITIVE" | "CORRECTIVE" | "HINT" | "SUGGESTION",
  "message": "Your main feedback message",
  "hints": ["optional", "additional hints"],
  "suggestedNextSteps": ["what to try next"],
  "confidence": 0.0-1.0
}
```

## Getting Started with Gemini

### 1. Get API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with Google account
3. Click "Get API key"
4. Create key for new or existing project
5. Copy the API key

### 2. Test API Key

```bash
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=YOUR_API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{
    "contents": [{
      "parts": [{"text": "Explain how to solve 2x + 5 = 15"}]
    }]
  }'
```

### 3. Set Environment Variable

```bash
export GEMINI_API_KEY="your-api-key-here"
```

Or add to `.env` file:

```
GEMINI_API_KEY=your-api-key-here
```

## Implementation Plan

### Files to Create/Modify

1. **New: `dto/GeminiRequestDto.java`** - Request structure for Gemini API
2. **New: `dto/GeminiResponseDto.java`** - Response structure from Gemini API
3. **New: `service/GeminiAIService.java`** - REST client for Gemini API
4. **Modify: `service/AITutorService.java`** - Add Gemini integration
5. **Modify: `application.properties`** - Add Gemini configuration
6. **New: `GEMINI_SETUP.md`** - Setup instructions for users

### Testing Strategy

1. **Unit Tests:** Mock Gemini responses
2. **Integration Tests:** Use test API key
3. **Manual Testing:** Real student scenarios
4. **Fallback:** Keep mock AI as fallback if Gemini is unavailable

## Safety Considerations

### Content Filtering

Gemini has built-in safety filters for:

- Harassment
- Hate speech
- Sexually explicit content
- Dangerous content

### Rate Limiting

- Implement exponential backoff for rate limit errors
- Cache common feedback patterns
- Queue requests if needed

### Error Handling

- Graceful degradation to mock AI if Gemini fails
- Clear error messages for students
- Logging for debugging

## Cost Estimation

For a classroom of 30 students:

- Average session: 20 actions × 30 students = 600 actions/day
- Average prompt: ~500 tokens in + ~200 tokens out = 700 tokens
- Daily usage: 600 × 700 = 420,000 tokens ≈ 0.42M tokens
- Monthly cost (if paid): 0.42M × 30 days × ($0.075 input + $0.30 output) / 1M ≈ $4.73/month

**Free tier is sufficient for small classrooms!**

## Next Steps

1. ✅ Research complete
2. ⏳ Get Gemini API key
3. ⏳ Create Gemini client service
4. ⏳ Integrate with AITutorService
5. ⏳ Test with real exercises
6. ⏳ Document setup process
