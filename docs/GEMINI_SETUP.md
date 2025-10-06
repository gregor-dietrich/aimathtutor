# Setting Up Google Gemini AI for AIMathTutor

This guide will help you set up Google Gemini 1.5 Flash as the AI provider for the AIMathTutor application.

## Why Gemini 1.5 Flash?

- ✅ **Free Tier Available:** 15 requests/minute, 1M tokens/minute, 1,500 requests/day
- ✅ **Fast:** Optimized for low latency
- ✅ **Affordable:** Even paid usage is very cheap ($0.075 per 1M tokens)
- ✅ **Smart:** Great at math and reasoning tasks
- ✅ **Easy Setup:** Just need an API key, no complex configuration

## Step 1: Get Your Gemini API Key

### 1.1 Go to Google AI Studio

Visit: [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)

### 1.2 Sign In

Sign in with your Google account

### 1.3 Create API Key

1. Click "Get API key" or "Create API key"
2. Select "Create API key in new project" or choose an existing project
3. Copy the generated API key (starts with `AIza...`)

⚠️ **Important:** Keep your API key secure! Don't commit it to version control.

## Step 2: Configure Your Application

### Option A: Environment Variable (Recommended)

#### Linux/Mac

```bash
export GEMINI_API_KEY="AIzaSyD...your-key-here"
```

Add to your `~/.bashrc` or `~/.zshrc` to make it permanent:

```bash
echo 'export GEMINI_API_KEY="AIzaSyD...your-key-here"' >> ~/.bashrc
source ~/.bashrc
```

#### Windows (PowerShell)

```powershell
$env:GEMINI_API_KEY="AIzaSyD...your-key-here"
```

Permanent (System Environment Variable):

1. Search for "Environment Variables" in Windows
2. Click "Environment Variables..."
3. Under "User variables", click "New"
4. Variable name: `GEMINI_API_KEY`
5. Variable value: Your API key

### Option B: application.properties File

Edit `src/main/resources/application.properties`:

```properties
# Change provider from mock to gemini
ai.tutor.provider=gemini

# Add your API key (NOT RECOMMENDED for production)
gemini.api.key=AIzaSyD...your-key-here
```

⚠️ **Security Warning:** Never commit API keys to version control! Use environment variables instead.

### Option C: .env File (Best for Development)

1. Create a `.env` file in project root (this file is git-ignored):

```bash
GEMINI_API_KEY=AIzaSyD...your-key-here
```

2. The application will read from environment variables automatically

## Step 3: Enable Gemini in Configuration

Edit `src/main/resources/application.properties`:

```properties
# Change this line from 'mock' to 'gemini'
ai.tutor.provider=gemini
```

That's it! The application will now use Gemini AI.

## Step 4: Verify Setup

### 4.1 Check Configuration

Start the application:

```bash
./mvnw quarkus:dev
```

Look for logs:

```
INFO  [de.vpt.aim.ser.AITutorService] Analyzing math action with Gemini AI
```

### 4.2 Test in Application

1. Log in as a student
2. Navigate to an exercise with Graspable Math enabled
3. Perform a math action (e.g., simplify an expression)
4. You should see AI feedback from Gemini!

### 4.3 Test API Key (Optional)

Test your API key directly:

```bash
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=YOUR_API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{
    "contents": [{
      "parts": [{"text": "Say hello!"}]
    }]
  }'
```

Should return:

```json
{
  "candidates": [{
    "content": {
      "parts": [{"text": "Hello! 👋 How can I help you today?"}],
      "role": "model"
    },
    "finishReason": "STOP",
    ...
  }]
}
```

## Troubleshooting

### Error: "Gemini API key not configured"

**Solution:** Ensure you've set the `GEMINI_API_KEY` environment variable and restarted the application.

### Error: "403 Forbidden" or "API key not valid"

**Solutions:**

- Verify your API key is correct (no extra spaces)
- Check the key hasn't been deleted or restricted in Google AI Studio
- Ensure the key has permissions for the Generative Language API

### Error: "429 Too Many Requests"

**Solution:** You've hit the rate limit. Free tier limits:

- 15 requests per minute
- 1,500 requests per day

Wait a minute or upgrade to paid tier if needed.

### Error: "Response blocked by safety filters"

**Solution:** The student's input or Gemini's response triggered safety filters. This is rare for math content but can happen. The system will fall back to mock AI automatically.

### Gemini not responding / Very slow

**Check:**

- Your internet connection
- Google AI service status: [https://status.cloud.google.com/](https://status.cloud.google.com/)
- API quota: [https://aistudio.google.com/](https://aistudio.google.com/)

## Advanced Configuration

### Adjust Temperature (Creativity)

In `application.properties`:

```properties
gemini.temperature=0.7  # 0.0 = deterministic, 1.0 = creative
```

For math tutoring:

- **0.3-0.5:** More consistent, focused feedback
- **0.7-0.8:** More varied, creative explanations (default)
- **0.9-1.0:** Very creative, may be less accurate

### Adjust Max Tokens (Response Length)

```properties
gemini.max-tokens=1000  # Maximum response length
```

Typical usage:

- **500:** Short, concise feedback
- **1000:** Standard (default)
- **2000:** Detailed explanations

### Change Model Version

```properties
gemini.model=gemini-1.5-flash  # Fast and efficient (default)
# gemini.model=gemini-1.5-pro   # More capable, slower, more expensive
```

## Switching Back to Mock AI

If you want to temporarily disable Gemini:

```properties
ai.tutor.provider=mock
```

Or disable AI entirely:

```properties
ai.tutor.enabled=false
```

## Free Tier Limits

**Per Minute:**

- 15 requests
- 1 million tokens

**Per Day:**

- 1,500 requests

**For a classroom:**

- 30 students × 20 actions/session = 600 requests
- Well within free tier limits!

## Cost Calculator (If Upgrading to Paid)

**Monthly estimate for 30 students:**

- 30 students × 20 actions/session × 20 days = 12,000 requests/month
- 12,000 requests × 700 tokens/request = 8.4M tokens
- Input cost: 8.4M × $0.075/1M = $0.63
- Output cost: 8.4M × $0.30/1M = $2.52
- **Total: ~$3.15/month**

Very affordable!

## Support & Resources

- **Gemini Documentation:** [https://ai.google.dev/docs](https://ai.google.dev/docs)
- **API Key Management:** [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)
- **Pricing:** [https://ai.google.dev/pricing](https://ai.google.dev/pricing)
- **Rate Limits:** [https://ai.google.dev/pricing#1_5flash](https://ai.google.dev/pricing#1_5flash)

## Security Best Practices

1. ✅ **Never** commit API keys to Git
2. ✅ **Use** environment variables for API keys
3. ✅ **Add** `.env` to `.gitignore`
4. ✅ **Rotate** API keys regularly
5. ✅ **Restrict** API key to specific IPs (in Google Cloud Console)
6. ✅ **Monitor** usage in Google AI Studio

## What's Next?

Once Gemini is configured, you can:

- Create exercises in Admin Panel
- Students get intelligent, personalized feedback
- View session analytics to see AI interactions
- Adjust prompts in `AITutorService.java` for better feedback

Happy tutoring! 🎓📐
