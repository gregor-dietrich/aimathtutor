# OpenAI Setup Guide for AI Math Tutor

This guide explains how to configure and use **OpenAI** as the AI provider for the AI Math Tutor application.

## Overview

OpenAI provides premium AI models (GPT-4o, GPT-4o-mini, GPT-3.5-turbo) with excellent reasoning capabilities, JSON mode support, and consistent quality. It's ideal when you need:

- **High-quality responses** for complex math tutoring
- **Reliable JSON formatting** with native JSON mode
- **Enterprise-grade service** with SLA and support
- **Budget flexibility** with multiple model tiers

## Prerequisites

- OpenAI account (sign up at <https://platform.openai.com/signup>)
- Payment method added to your account (no free tier, but GPT-4o-mini is very affordable)
- Active API key with usage limits configured

## Cost Estimates

For a classroom with **30 students**, assuming **10 interactions per student per session**:

| Model             | Input Cost | Output Cost | Monthly Cost (20 sessions) | Use Case                  |
| ----------------- | ---------- | ----------- | -------------------------- | ------------------------- |
| **gpt-4o-mini**   | $0.150/1M  | $0.600/1M   | **$6.30/month**            | Recommended - Best value  |
| **gpt-3.5-turbo** | $0.50/1M   | $1.50/1M    | $16.80/month               | Budget option             |
| **gpt-4o**        | $2.50/1M   | $10/1M      | **$105/month**             | Premium - Highest quality |

**Recommendation:** Start with **gpt-4o-mini** for excellent quality at minimal cost.

## Step-by-Step Setup

### 1. Create OpenAI Account and Get API Key

1. Go to <https://platform.openai.com/signup>
2. Sign up with email or Google/Microsoft account
3. Verify your email address
4. Add payment method at <https://platform.openai.com/account/billing>
5. Navigate to <https://platform.openai.com/api-keys>
6. Click "Create new secret key"
7. Give it a name (e.g., "AI Math Tutor")
8. Copy the key immediately (you won't see it again!)
9. Store it securely (password manager recommended)

### 2. Configure Usage Limits (Optional but Recommended)

1. Go to <https://platform.openai.com/account/limits>
2. Set monthly spend limits to prevent unexpected bills
3. Recommended starting limit: $20/month
4. Enable email notifications for spend thresholds

### 3. Set Environment Variable

**Linux/macOS:**

```bash
# Add to ~/.bashrc or ~/.zshrc
export OPENAI_API_KEY="sk-your-actual-api-key-here"

# Reload shell configuration
source ~/.bashrc  # or source ~/.zshrc
```

**Windows (Command Prompt):**

```cmd
setx OPENAI_API_KEY "sk-your-actual-api-key-here"
```

**Windows (PowerShell):**

```powershell
[System.Environment]::SetEnvironmentVariable('OPENAI_API_KEY', 'sk-your-actual-api-key-here', 'User')
```

### 4. Configure application.properties

Edit `src/main/resources/application.properties`:

```properties
# Enable OpenAI as AI provider
ai.tutor.enabled=true
ai.tutor.provider=openai

# OpenAI Configuration
openai.api.key=${OPENAI_API_KEY:your-api-key-here}
openai.model=gpt-4o-mini
openai.api.base-url=https://api.openai.com/v1
openai.temperature=0.7
openai.max-tokens=1000

# Optional: Add organization ID if you have one
# openai.organization=org-your-org-id
```

**Configuration Options:**

- `openai.model`: Choose your model
  - `gpt-4o-mini` - Recommended (fast, affordable, high quality)
  - `gpt-4o` - Premium (highest quality, most expensive)
  - `gpt-3.5-turbo` - Budget (good for simple tasks)
- `openai.temperature`: Controls randomness (0.0-2.0)
  - `0.7` - Recommended (balanced creativity)
  - `0.3` - More focused and deterministic
  - `1.0` - More creative responses
- `openai.max-tokens`: Maximum response length
  - `1000` - Default (good for most tutoring responses)
  - `500` - Shorter responses (faster, cheaper)
  - `2000` - Longer explanations (slower, more expensive)

### 5. Test Configuration

Start the application:

```bash
./mvnw quarkus:dev
```

Check the logs for:

```
✓ OpenAI service initialized successfully
✓ Model: gpt-4o-mini
✓ OpenAI is configured and ready
```

### 6. Verify AI Functionality

1. Log in to the application
2. Navigate to a lesson with exercises
3. Open an exercise with Graspable Math
4. Perform a math action (e.g., simplify an equation)
5. You should see AI feedback appear within 1-2 seconds

**Expected behavior:**

- Fast responses (typically 500ms-2s)
- Structured JSON feedback with:
  - `success`: true/false
  - `feedback`: Natural language explanation
  - `hint`: (optional) Next step suggestion
  - `encouragement`: Motivational message

## Model Selection Guide

### gpt-4o-mini (Recommended)

**Best for:** Most classrooms, excellent value

- **Speed:** Very fast (500ms-1s)
- **Quality:** Excellent for math tutoring
- **Cost:** $6.30/month for 30 students
- **Advantages:** JSON mode, function calling, 128K context
- **Use when:** You want premium quality at minimal cost

### gpt-3.5-turbo (Budget)

**Best for:** Simple tutoring, budget constraints

- **Speed:** Fast (500ms-1.5s)
- **Quality:** Good for basic algebra/arithmetic
- **Cost:** $16.80/month for 30 students
- **Advantages:** Proven track record, widely used
- **Use when:** Budget is tight or tasks are simple

### gpt-4o (Premium)

**Best for:** Advanced math, complex reasoning

- **Speed:** Fast (1-2s)
- **Quality:** Highest quality, best reasoning
- **Cost:** $105/month for 30 students
- **Advantages:** Superior at calculus, proofs, explanations
- **Use when:** Teaching advanced topics or need best quality

## Troubleshooting

### Error: "OpenAI API key not configured"

**Problem:** Environment variable not set or incorrect
**Solution:**

```bash
# Verify environment variable
echo $OPENAI_API_KEY

# Should output: sk-...
# If empty, set it again and restart terminal
export OPENAI_API_KEY="sk-your-key"
```

### Error: "401 Unauthorized"

**Problem:** Invalid or expired API key
**Solution:**

1. Verify API key at <https://platform.openai.com/api-keys>
2. Check if key was revoked or deleted
3. Create new key if needed
4. Update environment variable

### Error: "429 Rate limit exceeded"

**Problem:** Too many requests per minute
**Solution:**

1. Check usage at <https://platform.openai.com/account/usage>
2. Tier 1 limit: 500 RPM (requests per minute)
3. Wait 1 minute and try again
4. Consider upgrading to higher tier if needed
5. Implement request throttling if using heavily

### Error: "insufficient_quota"

**Problem:** No credits remaining or billing issue
**Solution:**

1. Check balance at <https://platform.openai.com/account/billing>
2. Add payment method if missing
3. Purchase additional credits
4. Verify payment method is valid

### Slow Responses (>5 seconds)

**Problem:** Network latency or server overload
**Solution:**

1. Check OpenAI status: <https://status.openai.com>
2. Reduce `max-tokens` to 500 for faster responses
3. Use `gpt-4o-mini` instead of `gpt-4o` (faster)
4. Check your internet connection

### Poor Quality Responses

**Problem:** Model not suitable for math tasks
**Solution:**

1. Switch to `gpt-4o-mini` or `gpt-4o`
2. Avoid using `gpt-3.5-turbo` for complex math
3. Increase `temperature` to 0.7-0.8 for more creativity
4. Verify exercise has clear math expression

## Monitoring and Cost Control

### Track Usage

Monitor usage at: <https://platform.openai.com/account/usage>

**Key metrics:**

- **Requests per day:** Should match student activity
- **Tokens per request:** Average 500-1000 tokens
- **Daily cost:** Multiply requests × average cost per request

### Set Budget Alerts

1. Go to <https://platform.openai.com/account/limits>
2. Set "Soft limit" (email notification): $10
3. Set "Hard limit" (stops requests): $20
4. Adjust based on classroom size and usage

### Estimate Monthly Cost

**Formula:**

```
Monthly Cost = Students × Sessions × Interactions × Cost per Interaction

Example (30 students, gpt-4o-mini):
= 30 students × 20 sessions × 10 interactions × $0.0001
= $6.00/month
```

## Security Best Practices

### Protect Your API Key

- **Never commit** API keys to git repositories
- **Use environment variables** instead of hardcoding
- **Rotate keys** every 90 days
- **Use separate keys** for dev/test/production
- **Revoke immediately** if compromised

### Limit Key Permissions

1. Create separate keys for different environments
2. Name keys descriptively: "AI-Tutor-Production"
3. Track which keys are used where
4. Delete unused keys immediately

### Monitor for Abuse

- Check usage dashboard daily during testing
- Set up billing alerts
- Review request logs for unusual patterns
- Investigate unexpected cost spikes

## Comparison with Other Providers

| Feature         | OpenAI          | Gemini          | Ollama             |
| --------------- | --------------- | --------------- | ------------------ |
| **Cost**        | $6-105/month    | Free-$7/month   | $0 (hardware cost) |
| **Quality**     | Excellent       | Very Good       | Good               |
| **Speed**       | Fast (500ms-2s) | Fast (500ms-2s) | Varies (1-10s)     |
| **JSON Mode**   | Native ✓        | Prompt-based    | Prompt-based       |
| **Privacy**     | Cloud (secure)  | Cloud (secure)  | Local (private)    |
| **Setup**       | Easy            | Easy            | Moderate           |
| **Rate Limits** | 500-10K RPM     | 15-360 RPM      | None               |
| **Support**     | Enterprise SLA  | Community       | Community          |

**When to use OpenAI:**

- You need consistent premium quality
- Budget allows $6-105/month
- Enterprise SLA is important
- JSON mode reliability is critical
- Using advanced math topics (calculus, proofs)

## Advanced Configuration

### Using Organization ID

If you're part of an OpenAI organization:

```properties
openai.organization=org-your-org-id
```

Find it at: <https://platform.openai.com/account/organization>

### Adjusting Timeout

For slower networks, increase timeout:

```properties
# Default is 30 seconds, increase if needed
openai.timeout=60
```

### Development vs Production Keys

Use different keys for different environments:

```bash
# .env.development
OPENAI_API_KEY=sk-dev-key-here

# .env.production
OPENAI_API_KEY=sk-prod-key-here
```

### Fallback to Other Providers

Configure multiple providers for redundancy:

```properties
# Primary provider
ai.tutor.provider=openai

# If OpenAI fails, system falls back to mock AI automatically
# No additional configuration needed
```

## Getting Help

### Official Resources

- **Documentation:** <https://platform.openai.com/docs>
- **API Reference:** <https://platform.openai.com/docs/api-reference>
- **Community Forum:** <https://community.openai.com>
- **Status Page:** <https://status.openai.com>
- **Support:** <https://help.openai.com>

### Common Questions

**Q: Can I use OpenAI for free?**
A: No, OpenAI requires payment. However, new accounts get $5 free credit valid for 3 months.

**Q: Which model should I choose?**
A: Start with `gpt-4o-mini` - it's fast, affordable ($6/month), and excellent quality.

**Q: How do I reduce costs?**
A: Use `gpt-4o-mini`, reduce `max-tokens` to 500, or switch to Gemini (free tier).

**Q: Is my data private?**
A: OpenAI states they don't use API data for training. See: <https://openai.com/policies/api-data-usage-policies>

**Q: What if I hit rate limits?**
A: Tier 1 allows 500 RPM. Upgrade at <https://platform.openai.com/account/limits> or implement request throttling.

**Q: Can I use Azure OpenAI instead?**
A: Yes, change `openai.api.base-url` to your Azure endpoint. Requires separate Azure setup.

## Next Steps

1. **Test thoroughly** with sample exercises
2. **Monitor costs** for first week
3. **Adjust model/tokens** based on quality needs
4. **Compare** with Gemini and Ollama if desired
5. **Scale up** once satisfied with quality and cost

## Troubleshooting Checklist

- [ ] API key is valid (test at <https://platform.openai.com/api-keys>)
- [ ] Environment variable is set (`echo $OPENAI_API_KEY`)
- [ ] Payment method is added and valid
- [ ] Usage limits are not exceeded
- [ ] Application is configured with `ai.tutor.provider=openai`
- [ ] Model name is correct (`gpt-4o-mini`, not `gpt4o-mini`)
- [ ] Internet connection is stable
- [ ] OpenAI service is operational (check status.openai.com)

If issues persist, check application logs at `logs/aimathtutor.log` for detailed error messages.
