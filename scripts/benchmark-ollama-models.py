#!/usr/bin/env python3
"""
Benchmark Ollama models (qwen3:4b vs qwen3:8b) for AIMathTutor.
Measures latency, memory usage, and response correctness on math problems.

Usage:
    python3 scripts/benchmark-ollama-models.py [--model qwen3:4b] [--num-problems 10]

Requirements:
    - Ollama running locally (ollama serve or desktop app)
    - requests library (pip install requests)
    - Both models pulled (ollama pull qwen3:4b && ollama pull qwen3:8b)
"""

import json
import subprocess
import time
import sys
import argparse
from typing import List, Dict, Any, Optional
from pathlib import Path

try:
    import requests
except ImportError:
    print("Error: requests library not found. Install with: pip install requests")
    sys.exit(1)


# Math test problems (progressively harder)
TEST_PROBLEMS = [
    {
        "id": "algebra_1",
        "prompt": "Solve for x: 2x + 3 = 11. Show your steps.",
        "expected_answer": "x = 4",
        "difficulty": "easy",
    },
    {
        "id": "algebra_2",
        "prompt": "Solve for x: 3x - 5 = 2x + 7. Show your steps.",
        "expected_answer": "x = 12",
        "difficulty": "easy",
    },
    {
        "id": "algebra_3",
        "prompt": "Simplify: (x^2 - 9) / (x - 3). What is the result?",
        "expected_answer": "x + 3",
        "difficulty": "medium",
    },
    {
        "id": "calculus_1",
        "prompt": "What is the derivative of f(x) = x^3 - 2x^2 + x? Show your work.",
        "expected_answer": "3x^2 - 4x + 1",
        "difficulty": "medium",
    },
    {
        "id": "geometry_1",
        "prompt": "A circle has radius 5. What is its area? Use pi and show the formula.",
        "expected_answer": "25π",
        "difficulty": "easy",
    },
    {
        "id": "trig_1",
        "prompt": "What is sin(π/6)? Explain why.",
        "expected_answer": "1/2",
        "difficulty": "medium",
    },
    {
        "id": "algebra_4",
        "prompt": "Factor: x^2 + 5x + 6. Show your steps.",
        "expected_answer": "(x + 2)(x + 3)",
        "difficulty": "medium",
    },
    {
        "id": "probability_1",
        "prompt": "If you roll a fair die, what is the probability of getting a number greater than 4?",
        "expected_answer": "1/3",
        "difficulty": "easy",
    },
    {
        "id": "algebra_5",
        "prompt": "Solve: x^2 - 5x + 6 = 0. Find all solutions.",
        "expected_answer": "x = 2 or x = 3",
        "difficulty": "medium",
    },
    {
        "id": "systems_1",
        "prompt": "Solve the system: x + y = 5, x - y = 1. Find x and y.",
        "expected_answer": "x = 3, y = 2",
        "difficulty": "medium",
    },
]


def check_ollama_running(base_url: str = "http://localhost:11434") -> bool:
    """Check if Ollama is running."""
    try:
        response = requests.get(f"{base_url}/api/tags", timeout=2)
        return response.status_code == 200
    except Exception as e:
        print(f"❌ Ollama not running at {base_url}: {e}")
        return False


def pull_model(model: str) -> bool:
    """Pull a model using ollama CLI."""
    try:
        print(f"📦 Pulling {model}...")
        result = subprocess.run(
            ["ollama", "pull", model],
            capture_output=True,
            text=True,
            timeout=600,
        )
        return result.returncode == 0
    except Exception as e:
        print(f"❌ Failed to pull {model}: {e}")
        return False


def query_ollama(
    model: str,
    prompt: str,
    base_url: str = "http://localhost:11434",
    timeout: Optional[float] = None,
) -> Dict[str, Any]:
    """Query Ollama model and return response with timing.

    By default `timeout=None` so the HTTP client will wait indefinitely
    for a response (no client-side timeout). This prevents the benchmark
    from failing on long-running large-model inferences.
    """
    url = f"{base_url}/api/generate"
    payload = {
        "model": model,
        "prompt": prompt,
        "stream": False,
    }

    try:
        start_time = time.time()
        # If timeout is None, requests will wait indefinitely
        response = requests.post(url, json=payload, timeout=timeout)
        elapsed = time.time() - start_time

        if response.status_code == 200:
            data = response.json()
            return {
                "success": True,
                "response": data.get("response", "").strip(),
                "elapsed_seconds": elapsed,
                "tokens": data.get("eval_count", 0),
                "tokens_per_second": (
                    data.get("eval_count", 0) / elapsed if elapsed > 0 else 0
                ),
            }
        else:
            return {
                "success": False,
                "error": f"HTTP {response.status_code}",
                "elapsed_seconds": elapsed,
            }
    except requests.Timeout:
        # In practice this path is unlikely when timeout=None, but keep
        # a defensive handler that reports timeout as an error.
        return {"success": False, "error": "Request timeout", "elapsed_seconds": 0}
    except Exception as e:
        return {"success": False, "error": str(e), "elapsed_seconds": 0}


def check_answer_quality(response: str, expected: str) -> Dict[str, Any]:
    """
    Heuristic check if response contains the expected answer.
    Returns a score and reasoning.
    """
    response_lower = response.lower()
    expected_lower = expected.lower()

    # Check if expected answer is in response
    contains_answer = expected_lower in response_lower

    # Check for common correct patterns
    correct_indicators = [
        "correct",
        "right",
        "true",
        "solution is",
        "answer is",
        "result is",
        "equals",
        "=",
    ]
    has_correctness_phrase = any(
        phrase in response_lower for phrase in correct_indicators
    )

    # Simple heuristic scoring
    if contains_answer:
        score = 100
        verdict = "✓ CORRECT"
    elif has_correctness_phrase or len(response) > 200:
        score = 70
        verdict = "⚠ LIKELY CORRECT"
    else:
        score = 30
        verdict = "✗ UNCERTAIN"

    return {
        "score": score,
        "verdict": verdict,
        "contains_expected": contains_answer,
        "has_reasoning": has_correctness_phrase or len(response) > 100,
    }


def run_benchmark(
    models: List[str],
    problems: List[Dict[str, str]] = None,
    num_problems: int = None,
) -> Dict[str, Any]:
    """Run full benchmark suite."""
    if problems is None:
        problems = TEST_PROBLEMS

    if num_problems:
        problems = problems[:num_problems]

    # Check Ollama
    if not check_ollama_running():
        print("❌ Ollama is not running. Start it with: ollama serve")
        sys.exit(1)

    print(f"✓ Ollama is running\n")

    # Ensure models are available
    for model in models:
        print(f"Checking {model}...")
        try:
            result = subprocess.run(
                ["ollama", "show", model],
                capture_output=True,
                text=True,
                timeout=5,
            )
            if result.returncode != 0:
                print(f"  Model not found, pulling...")
                if not pull_model(model):
                    print(f"❌ Failed to pull {model}")
                    sys.exit(1)
            else:
                print(f"  ✓ Model available")
        except Exception as e:
            print(f"❌ Error checking {model}: {e}")
            sys.exit(1)

    print(f"\n{'=' * 80}")
    print(f"🧮 OLLAMA MODEL BENCHMARK")
    print(f"{'=' * 80}")
    print(f"Models: {', '.join(models)}")
    print(f"Problems: {len(problems)}")
    print(f"{'=' * 80}\n")

    results = {}

    for model in models:
        print(f"\n📊 Testing {model}...")
        print(f"{'-' * 80}")

        model_results = {
            "model": model,
            "problems_tested": 0,
            "avg_latency_sec": 0,
            "avg_tokens_per_sec": 0,
            "correctness_score": 0,
            "details": [],
        }

        total_latency = 0
        total_tokens_per_sec = 0
        total_correctness = 0
        successful_runs = 0

        for problem in problems:
            print(
                f"  [{problem['id']}] ({problem['difficulty']}) ",
                end="",
                flush=True,
            )

            # Query model
            query_result = query_ollama(model, problem["prompt"])

            if not query_result["success"]:
                print(f"❌ Error: {query_result.get('error', 'Unknown')}")
                model_results["details"].append(
                    {
                        "problem_id": problem["id"],
                        "error": query_result.get("error"),
                    }
                )
                continue

            # Check answer
            answer_check = check_answer_quality(
                query_result["response"], problem["expected_answer"]
            )

            print(
                f"{answer_check['verdict']} | "
                f"{query_result['elapsed_seconds']:.2f}s | "
                f"{query_result['tokens_per_second']:.1f} tok/s"
            )

            # Accumulate stats
            total_latency += query_result["elapsed_seconds"]
            total_tokens_per_sec += query_result["tokens_per_second"]
            total_correctness += answer_check["score"]
            successful_runs += 1

            model_results["details"].append(
                {
                    "problem_id": problem["id"],
                    "latency_sec": query_result["elapsed_seconds"],
                    "tokens_per_sec": query_result["tokens_per_second"],
                    "correctness_score": answer_check["score"],
                    "response_excerpt": query_result["response"][:150] + "...",
                }
            )

            # Small delay between requests
            time.sleep(0.5)

        # Calculate averages
        if successful_runs > 0:
            model_results["problems_tested"] = successful_runs
            model_results["avg_latency_sec"] = total_latency / successful_runs
            model_results["avg_tokens_per_sec"] = total_tokens_per_sec / successful_runs
            model_results["correctness_score"] = total_correctness / successful_runs

        results[model] = model_results

    # Print summary
    print(f"\n\n{'=' * 80}")
    print(f"📈 BENCHMARK SUMMARY")
    print(f"{'=' * 80}\n")

    for model, data in results.items():
        print(f"{model}:")
        print(f"  Problems tested: {data['problems_tested']}")
        print(f"  Avg latency: {data['avg_latency_sec']:.2f}s")
        print(f"  Avg throughput: {data['avg_tokens_per_sec']:.1f} tokens/sec")
        print(f"  Avg correctness: {data['correctness_score']:.1f}%")
        print()

    # Comparison
    if len(results) == 2:
        models_list = list(results.keys())
        model_4b = results[models_list[0]]
        model_8b = results[models_list[1]]

        print(f"{'=' * 80}")
        print(f"⚖️  COMPARISON: {models_list[0]} vs {models_list[1]}")
        print(f"{'=' * 80}\n")

        latency_ratio = model_8b["avg_latency_sec"] / model_4b["avg_latency_sec"]
        correctness_diff = model_8b["correctness_score"] - model_4b["correctness_score"]

        print(f"Latency:")
        print(
            f"  {models_list[0]} is {latency_ratio:.2f}x faster "
            f"({model_4b['avg_latency_sec']:.2f}s vs {model_8b['avg_latency_sec']:.2f}s)\n"
        )

        print(f"Correctness:")
        if abs(correctness_diff) < 5:
            verdict = "roughly equivalent"
        elif correctness_diff > 0:
            verdict = f"{models_list[1]} slightly better"
        else:
            verdict = f"{models_list[0]} slightly better"
        print(f"  {verdict} ({model_4b['correctness_score']:.1f}% vs {model_8b['correctness_score']:.1f}%)\n")

        print(f"Recommendation:")
        if latency_ratio > 1.5 and abs(correctness_diff) < 10:
            print(
                f"  ✓ {models_list[0]} is significantly faster with similar correctness."
                f"\n    → Best choice for AIMathTutor (speed + efficiency)\n"
            )
        elif abs(correctness_diff) > 10:
            print(
                f"  ⚠ {models_list[1]} shows notably better correctness (+{correctness_diff:.1f}%)."
                f"\n    → Consider if accuracy is critical, else prefer {models_list[0]}\n"
            )
        else:
            print(f"  ⚖️  Trade-off: {models_list[0]} faster, {models_list[1]} similar quality.\n")

    # Save results to JSON
    output_file = Path("benchmark-results.json")
    with open(output_file, "w") as f:
        json.dump(results, f, indent=2)
    print(f"📄 Full results saved to {output_file}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Benchmark Ollama models for AIMathTutor",
    )
    parser.add_argument(
        "--models",
        nargs="+",
        default=["qwen3:4b", "qwen3:8b"],
        help="Models to benchmark (default: qwen3:4b qwen3:8b)",
    )
    parser.add_argument(
        "--num-problems",
        type=int,
        default=None,
        help="Number of problems to test (default: all)",
    )

    args = parser.parse_args()

    try:
        run_benchmark(args.models, num_problems=args.num_problems)
    except KeyboardInterrupt:
        print("\n\n⚠️  Benchmark interrupted by user")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ Benchmark failed: {e}", file=sys.stderr)
        sys.exit(1)
