# Ollama Model Benchmark Script

Automated benchmark to compare `qwen3:4b` vs `qwen3:8b` (or other Ollama models) on **latency**, **correctness**, and **resource efficiency** for AIMathTutor.

## Quick Start

### Prerequisites

1. **Ollama installed and running:**

   ```bash
   # On Windows: Download from https://ollama.com/download and run
   # On macOS: brew install ollama && ollama serve
   # On Linux: curl -fsSL https://ollama.com/install.sh | sh && ollama serve
   
   # Verify it's running:
   curl http://localhost:11434/api/tags
   ```

2. **Python 3.8+** with `requests` library:

   ```bash
   pip install requests
   ```

### Run the Benchmark

```bash
# Test both qwen3:4b and qwen3:8b (default)
python3 scripts/benchmark-ollama-models.py

# Test only qwen3:4b on first 5 problems
python3 scripts/benchmark-ollama-models.py --models qwen3:4b --num-problems 5

# Test custom models
python3 scripts/benchmark-ollama-models.py --models qwen3:8b qwen3:14b --num-problems 10
```

## What It Tests

**10 representative math problems** (easy → medium difficulty):

- Linear algebra (solve for x)
- Quadratic equations
- Calculus (derivatives)
- Geometry (area/circumference)
- Trigonometry (trig functions)
- Factoring and polynomial algebra
- Probability
- Systems of equations

Each problem is evaluated on:

- **Latency:** Wall-clock time to generate response (seconds)
- **Throughput:** Tokens generated per second
- **Correctness:** Heuristic check if response contains expected answer (0–100%)

## Output

### Console Summary

```txt
================================================================================
📈 BENCHMARK SUMMARY
================================================================================

qwen3:4b:
  Problems tested: 10
  Avg latency: 2.34s
  Avg throughput: 45.2 tokens/sec
  Avg correctness: 92.0%

qwen3:8b:
  Problems tested: 10
  Avg latency: 4.56s
  Avg throughput: 43.8 tokens/sec
  Avg correctness: 94.0%

================================================================================
⚖️  COMPARISON: qwen3:4b vs qwen3:8b
================================================================================

Latency:
  qwen3:4b is 1.95x faster (2.34s vs 4.56s)

Correctness:
  qwen3:8b slightly better (92.0% vs 94.0%)

Recommendation:
  ✓ qwen3:4b is significantly faster with similar correctness.
    → Best choice for AIMathTutor (speed + efficiency)
```

### Detailed Results

Results are saved to `benchmark-results.json` with per-problem details:

```json
{
  "qwen3:4b": {
    "model": "qwen3:4b",
    "problems_tested": 10,
    "avg_latency_sec": 2.34,
    "avg_tokens_per_sec": 45.2,
    "correctness_score": 92.0,
    "details": [
      {
        "problem_id": "algebra_1",
        "latency_sec": 1.89,
        "tokens_per_sec": 48.3,
        "correctness_score": 100,
        "response_excerpt": "To solve 2x + 3 = 11, we need to isolate x...",
      },
      ...
    ]
  }
}
```

## Interpreting Results

### Latency

- **Lower is better.** Typical range for Ollama on CPU: 1–10+ seconds per response.
- If `qwen3:4b` is 2–3× faster, it's a significant win for user experience (shorter feedback loop).

### Correctness

- **0–100% score** based on heuristic detection of expected answers.
- **80%+** generally means the model is consistently providing correct step-by-step solutions.
- Small differences (< 5%) are likely within measurement noise; larger gaps (> 10%) suggest real differences.

### Recommendation Logic

- **If 4B is significantly faster (> 1.5×) with similar correctness (< 5% diff):** Use `qwen3:4b` as default.
- **If 8B is notably more correct (> 10% higher):** Consider offering a "high-confidence mode" with 8B.
- **If latency is close (< 1.5×) but 4B is better:** Still prefer 4B for resource efficiency.

## Customization

### Add Custom Test Problems

Edit the script and modify `TEST_PROBLEMS`:

```python
TEST_PROBLEMS = [
    {
        "id": "custom_1",
        "prompt": "Your custom problem here",
        "expected_answer": "Expected answer or key phrase",
        "difficulty": "easy|medium|hard",
    },
    ...
]
```

### Adjust Timeout or Retries

In `query_ollama()`, modify the `timeout` parameter (default: 120s):

```python
response = requests.post(url, json=payload, timeout=60)  # 60 seconds
```

### Run on Different Hardware

The benchmark is portable. Run it on:

- **CPU-only machine:** Results will be slower, but relative comparison still valid.
- **GPU machine:** Results will be faster; use to identify which model fits your deployment.
- **Raspberry Pi / edge device:** Useful for checking feasibility.

## Troubleshooting

### "Ollama not running"

```bash
# Start Ollama server
ollama serve
# Or on macOS:
brew services start ollama
```

### "Model not found"

```bash
# Pull models manually
ollama pull qwen3:4b
ollama pull qwen3:8b

# Verify
ollama list
```

### Requests timeout

- Increase timeout in script (line 92, `timeout=120`).
- Or run on fewer problems: `--num-problems 3`

### Script runs very slowly

- This is normal on CPU-only hardware (can take 1–2 minutes per model).
- For faster iteration, use `--num-problems 3` to test just 3 problems.

## Integration with AIMathTutor

Once you've verified that `qwen3:4b` is optimal:

1. **Set as default in `application.properties`:**

   ```properties
   ollama.model=qwen3:4b
   ```

2. **Document in admin UI:** Add a note that `qwen3:4b` is the recommended model for low-resource deployments.

3. **Optional A/B testing:** Use the results to justify a feature flag for switching models per request.

## Example Run (Expected Output)

```txt
✓ Ollama is running

Checking qwen3:4b...
  ✓ Model available
Checking qwen3:8b...
  ✓ Model available

================================================================================
🧮 OLLAMA MODEL BENCHMARK
================================================================================
Models: qwen3:4b, qwen3:8b
Problems: 10
================================================================================

📊 Testing qwen3:4b...
--------------------------------------------------------------------------------
  [algebra_1] (easy) ✓ CORRECT | 1.89s | 48.3 tok/s
  [algebra_2] (easy) ✓ CORRECT | 1.92s | 47.1 tok/s
  [algebra_3] (medium) ⚠ LIKELY CORRECT | 2.45s | 44.8 tok/s
  [calculus_1] (medium) ✓ CORRECT | 3.12s | 43.5 tok/s
  ...

📊 Testing qwen3:8b...
...

📈 BENCHMARK SUMMARY
...
```

---

**Questions or issues?** Review the script's inline comments or check the Ollama documentation at <https://docs.ollama.com>.
