# Ollama Setup Guide for AI Math Tutor

This guide explains how to configure and use **Ollama** as the AI provider for the AI Math Tutor application.

## Overview

Ollama enables you to run large language models **locally** on your own hardware. It's ideal when you need:

- **100% free operation** (no per-request costs)
- **Complete privacy** (data never leaves your network)
- **No rate limits** (unlimited requests)
- **Offline capability** (works without internet)
- **Full control** (customize models, fine-tune, etc.)

## Prerequisites

### Hardware Requirements

**Minimum (for testing):**

- 8 GB RAM
- 4 CPU cores
- 10 GB disk space
- No GPU required (uses CPU, slower)

**Recommended (for production):**

- 16 GB RAM
- 8 CPU cores
- NVIDIA GPU with 8+ GB VRAM (GTX 1080, RTX 3060, or better)
- 20 GB disk space
- Good cooling solution

**Optimal (for large models):**

- 32+ GB RAM
- Modern CPU (Intel i7/i9, AMD Ryzen 7/9)
- NVIDIA GPU with 16+ GB VRAM (RTX 4070 Ti, RTX 4080, etc.)
- 50+ GB disk space for multiple models

### Software Requirements

- **Linux:** Ubuntu 20.04+, Fedora, or any modern distro
- **macOS:** 10.15+ (Catalina or newer)
- **Windows:** Windows 10/11 with WSL2 (or native)
- **Docker:** Optional but recommended for production

## Installation

### Linux

```bash
# Download and install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Verify installation
ollama --version

# Start Ollama service (runs on port 11434)
ollama serve
```

To run as a system service:

```bash
# Create systemd service
sudo systemctl enable ollama
sudo systemctl start ollama
sudo systemctl status ollama
```

### macOS

```bash
# Download from website
# Visit: https://ollama.com/download/mac

# Or use Homebrew
brew install ollama

# Start Ollama
ollama serve
```

### Windows

**Option 1: Native (Recommended)**

1. Download installer from <https://ollama.com/download/windows>
2. Run `OllamaSetup.exe`
3. Ollama runs automatically in the background
4. Check system tray for Ollama icon

**Option 2: WSL2**

```bash
# In WSL2 terminal
curl -fsSL https://ollama.com/install.sh | sh
ollama serve
```

### Docker

```bash
# Pull Ollama image
docker pull ollama/ollama

# Run Ollama container
docker run -d \
  --name ollama \
  -p 11434:11434 \
  -v ollama:/root/.ollama \
  ollama/ollama

# With GPU support (NVIDIA)
docker run -d \
  --gpus all \
  --name ollama \
  -p 11434:11434 \
  -v ollama:/root/.ollama \
  ollama/ollama
```

## Model Selection and Download

### Recommended Models for Math Tutoring

| Model                   | Size   | RAM Needed | Quality     | Speed     | Use Case                              |
| ----------------------- | ------ | ---------- | ----------- | --------- | ------------------------------------- |
| **llama3.1:8b**         | 4.7 GB | 8 GB       | Excellent   | Fast      | **Recommended** - Best balance        |
| **qwen2.5:7b**          | 4.4 GB | 8 GB       | Excellent   | Fast      | Math-specialized, excellent reasoning |
| **phi3:mini**           | 2.3 GB | 4 GB       | Good        | Very Fast | Quick responses, basic math           |
| **deepseek-coder:6.7b** | 3.8 GB | 8 GB       | Excellent   | Fast      | Code + math, good at algebra          |
| **llama3.1:70b**        | 40 GB  | 64 GB      | Outstanding | Slow      | Premium quality, needs powerful GPU   |

### Download Models

```bash
# Recommended: Llama 3.1 8B (best balance)
ollama pull llama3.1:8b

# Alternative: Qwen 2.5 7B (math specialist)
ollama pull qwen2.5:7b

# Lightweight: Phi-3 Mini (fast, basic)
ollama pull phi3:mini

# Code + Math: DeepSeek Coder
ollama pull deepseek-coder:6.7b

# Premium: Llama 3.1 70B (requires powerful hardware)
# Only download if you have 64+ GB RAM and good GPU
ollama pull llama3.1:70b
```

**Download time:** Expect 5-30 minutes depending on model size and internet speed.

### Verify Model Installation

```bash
# List installed models
ollama list

# Test a model
ollama run llama3.1:8b "What is 2 + 2?"

# Exit test mode
/bye
```

## Configuration

### 1. Configure application.properties

Edit `src/main/resources/application.properties`:

```properties
# Enable Ollama as AI provider
ai.tutor.enabled=true
ai.tutor.provider=ollama

# Ollama Configuration
ollama.api.url=http://localhost:11434
ollama.model=llama3.1:8b
ollama.temperature=0.7
ollama.max-tokens=1000
ollama.timeout-seconds=30
```

**Configuration Options:**

- `ollama.api.url`: Ollama server address
  - `http://localhost:11434` - Local installation
  - `http://your-server:11434` - Remote server
- `ollama.model`: Model to use (must be downloaded first)
  - `llama3.1:8b` - Recommended (excellent quality)
  - `qwen2.5:7b` - Math specialist
  - `phi3:mini` - Lightweight
- `ollama.temperature`: Controls randomness (0.0-1.0)
  - `0.7` - Recommended (balanced)
  - `0.3` - More focused responses
  - `0.9` - More creative
- `ollama.max-tokens`: Maximum response length
  - `1000` - Default
  - `500` - Shorter, faster
  - `2000` - Longer explanations
- `ollama.timeout-seconds`: Request timeout in seconds
  - `30` - Default (good for most systems)
  - `60` - Slower CPUs or large models
  - `10` - Fast GPUs with small models

### 2. Performance Tuning

Create `~/.ollama/config.json` for advanced settings:

```json
{
  "num_gpu": 1,
  "num_thread": 8,
  "num_ctx": 2048,
  "num_batch": 512,
  "num_predict": 1000
}
```

**Options:**

- `num_gpu`: Number of GPUs to use (0 for CPU only)
- `num_thread`: CPU threads (match your CPU cores)
- `num_ctx`: Context window size (higher = more memory)
- `num_batch`: Batch size (higher = faster on GPU)
- `num_predict`: Max tokens to generate

### 3. GPU Acceleration (NVIDIA)

Verify GPU is detected:

```bash
# Check if GPU is being used
ollama run llama3.1:8b "Test" --verbose

# Should show: "offload_layers": 32 (or similar)
# If 0, GPU is not being used
```

If GPU is not detected:

```bash
# Install NVIDIA drivers
sudo apt install nvidia-driver-545  # Ubuntu

# Install CUDA toolkit
wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/cuda-ubuntu2204.pin
sudo mv cuda-ubuntu2204.pin /etc/apt/preferences.d/cuda-repository-pin-600
sudo apt-key adv --fetch-keys https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/3bf863cc.pub
sudo add-apt-repository "deb https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/ /"
sudo apt update
sudo apt install cuda

# Restart Ollama
sudo systemctl restart ollama
```

## Testing

### 1. Test Ollama Server

```bash
# Check if Ollama is running
curl http://localhost:11434/api/version

# Should return: {"version":"0.x.x"}
```

### 2. Test Model Generation

```bash
# Simple test
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.1:8b",
  "prompt": "What is 2 + 2? Respond with just the number.",
  "stream": false
}'

# Should return JSON with "response": "4"
```

### 3. Start Application

```bash
./mvnw quarkus:dev
```

Check logs for:

```
✓ Ollama service initialized
✓ Model: llama3.1:8b
✓ Ollama server is available at http://localhost:11434
✓ Model llama3.1:8b is installed
```

### 4. Test AI Feedback

1. Log in to the application
2. Open an exercise with Graspable Math
3. Perform a math action
4. Observe AI feedback (may take 1-5 seconds)

## Performance Benchmarks

### Speed Comparison (llama3.1:8b)

| Hardware                | Tokens/Second | Response Time | Quality    |
| ----------------------- | ------------- | ------------- | ---------- |
| **CPU Only (i7-10700)** | 8-12 tok/s    | 5-10s         | ⭐⭐⭐⭐⭐ |
| **RTX 3060 (12GB)**     | 40-60 tok/s   | 1-2s          | ⭐⭐⭐⭐⭐ |
| **RTX 4070 Ti (12GB)**  | 80-100 tok/s  | 0.5-1s        | ⭐⭐⭐⭐⭐ |
| **RTX 4090 (24GB)**     | 120-150 tok/s | 0.3-0.8s      | ⭐⭐⭐⭐⭐ |

### Model Comparison (on RTX 3060)

| Model                   | Load Time | Response Time | Quality    | Math Skills |
| ----------------------- | --------- | ------------- | ---------- | ----------- |
| **phi3:mini**           | 1s        | 0.5-1s        | ⭐⭐⭐     | ⭐⭐⭐      |
| **qwen2.5:7b**          | 2s        | 1-2s          | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐  |
| **llama3.1:8b**         | 2s        | 1-2s          | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐    |
| **deepseek-coder:6.7b** | 2s        | 1-2s          | ⭐⭐⭐⭐   | ⭐⭐⭐⭐⭐  |
| **llama3.1:70b**        | 10s       | 3-5s          | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐  |

## Troubleshooting

### Error: "Ollama server not available"

**Problem:** Ollama service is not running
**Solution:**

```bash
# Check if Ollama is running
curl http://localhost:11434/api/version

# If no response, start Ollama
ollama serve

# Or restart service
sudo systemctl restart ollama
sudo systemctl status ollama
```

### Error: "Model not found"

**Problem:** Model is not downloaded
**Solution:**

```bash
# List installed models
ollama list

# Download the model
ollama pull llama3.1:8b

# Verify it appears in list
ollama list
```

### Slow Performance (>10s per response)

**Problem:** Running on CPU without GPU acceleration
**Solution:**

```bash
# Check GPU usage
nvidia-smi

# If GPU not detected:
1. Install NVIDIA drivers
2. Install CUDA toolkit
3. Restart Ollama service
4. Try smaller model (phi3:mini)
5. Reduce max-tokens to 500
```

### High Memory Usage

**Problem:** Model too large for available RAM
**Solution:**

```bash
# Switch to smaller model
ollama pull phi3:mini

# Update application.properties
ollama.model=phi3:mini

# Or add more RAM/SWAP
sudo fallocate -l 8G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### Error: "Connection timeout"

**Problem:** Request taking too long
**Solution:**

```properties
# Increase timeout in application.properties
ollama.timeout-seconds=60

# Or optimize model settings
ollama.max-tokens=500
```

### Poor Quality Responses

**Problem:** Model not suitable for math
**Solution:**

1. Switch to `qwen2.5:7b` (math specialist)
2. Or use `llama3.1:8b` (general purpose)
3. Increase temperature to 0.8 for creativity
4. Ensure model is fully loaded (check logs)

### GPU Not Being Used

**Problem:** Ollama defaulting to CPU
**Solution:**

```bash
# Check NVIDIA driver
nvidia-smi

# If not working, reinstall drivers
sudo apt purge nvidia-*
sudo apt install nvidia-driver-545
sudo reboot

# Verify CUDA
nvcc --version

# Reinstall Ollama
curl -fsSL https://ollama.com/install.sh | sh
```

## Cost Analysis

### Hardware Investment

**Entry Level (CPU Only):**

- Existing hardware: $0
- Performance: Acceptable (5-10s responses)
- Suitable for: Small classrooms (1-10 students)

**Mid-Range (with GPU):**

- RTX 3060 (12GB): $300-400
- Performance: Good (1-2s responses)
- Suitable for: Medium classrooms (10-30 students)

**High-End (for large models):**

- RTX 4070 Ti (12GB): $700-900
- Or RTX 4090 (24GB): $1500-2000
- Performance: Excellent (0.5-1s responses)
- Suitable for: Large classrooms (30+ students) or multiple classrooms

### Break-Even Analysis vs Cloud

**Compared to OpenAI ($6-105/month):**

- RTX 3060 ($350): Break-even in 6-12 months
- RTX 4070 Ti ($800): Break-even in 12-24 months
- Free operation after break-even point

**Compared to Gemini ($0-7/month):**

- Harder to justify purely on cost
- Main benefit is privacy and unlimited usage

**Best for:**

- Schools with existing GPUs
- Privacy-critical environments
- High-volume usage (>1000 requests/day)
- Long-term deployment (2+ years)

## Remote Server Setup

### Deploy on Separate Server

1. **Install Ollama on server:**

```bash
ssh user@server-ip
curl -fsSL https://ollama.com/install.sh | sh
ollama serve
```

2. **Configure firewall:**

```bash
# Allow port 11434
sudo ufw allow 11434/tcp
sudo ufw reload
```

3. **Update application.properties:**

```properties
ollama.api.url=http://server-ip:11434
```

### Docker Deployment

```yaml
# docker-compose.yml
version: "3.8"
services:
  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

volumes:
  ollama-data:
```

Start with:

```bash
docker-compose up -d
docker exec -it ollama ollama pull llama3.1:8b
```

## Security Considerations

### Network Security

**Local only (default):**

- Ollama binds to `localhost:11434`
- Not accessible from network
- Most secure option

**Network accessible:**

```bash
# Allow network access (use with caution)
OLLAMA_HOST=0.0.0.0:11434 ollama serve

# Recommend using reverse proxy with authentication
# Example: nginx with basic auth or OAuth
```

### Data Privacy

**Advantages:**

- ✓ All data stays on your hardware
- ✓ No data sent to third parties
- ✓ Compliant with strict privacy regulations (GDPR, FERPA)
- ✓ No internet required after model download

**Best practices:**

- Keep models updated: `ollama pull llama3.1:8b`
- Monitor disk usage: `du -sh ~/.ollama`
- Regular backups of model customizations
- Secure server access with SSH keys

## Advanced Topics

### Fine-Tuning Models

```bash
# Export base model
ollama show llama3.1:8b --modelfile > Modelfile

# Edit Modelfile to add math examples
# Then create custom model
ollama create math-tutor -f Modelfile
```

### Multiple Models

```bash
# Download multiple models
ollama pull llama3.1:8b
ollama pull qwen2.5:7b
ollama pull phi3:mini

# Switch between them in application.properties
ollama.model=qwen2.5:7b  # For math-heavy lessons
ollama.model=phi3:mini    # For quick feedback
```

### Load Balancing

For high-volume environments:

```bash
# Run multiple Ollama instances
ollama serve --port 11434  # Instance 1
ollama serve --port 11435  # Instance 2
ollama serve --port 11436  # Instance 3

# Configure load balancer (nginx, HAProxy)
```

## Comparison with Cloud Providers

| Feature          | Ollama         | Gemini     | OpenAI      |
| ---------------- | -------------- | ---------- | ----------- |
| **Monthly Cost** | $0             | $0-7       | $6-105      |
| **Setup Time**   | 30-60 min      | 5 min      | 5 min       |
| **Privacy**      | 100% Private   | Cloud      | Cloud       |
| **Rate Limits**  | None           | 15-360 RPM | 500-10K RPM |
| **Offline**      | Yes ✓          | No         | No          |
| **Hardware**     | Required       | None       | None        |
| **Quality**      | Good-Excellent | Excellent  | Excellent   |
| **Speed**        | 1-10s          | 0.5-2s     | 0.5-2s      |
| **Maintenance**  | Self-managed   | None       | None        |

**Use Ollama when:**

- You have GPU hardware available
- Privacy is critical (FERPA, GDPR compliance)
- High request volume (>1000/day)
- Long-term deployment (2+ years)
- Offline capability needed
- Want to avoid recurring cloud costs

**Use Gemini when:**

- No GPU hardware
- Low request volume (<500/day)
- Want zero setup complexity
- Budget is very tight ($0-7/month)

**Use OpenAI when:**

- Premium quality required
- Enterprise SLA needed
- JSON mode reliability critical
- Budget allows $6-105/month

## Getting Help

### Official Resources

- **Website:** <https://ollama.com>
- **Documentation:** <https://github.com/ollama/ollama/tree/main/docs>
- **Discord:** <https://discord.gg/ollama>
- **GitHub Issues:** <https://github.com/ollama/ollama/issues>
- **Model Library:** <https://ollama.com/library>

### Common Questions

**Q: Do I need a GPU?**
A: No, but highly recommended. CPU-only works but is 5-10x slower.

**Q: How much does it cost?**
A: $0/month after initial hardware investment. Models are free to download and use.

**Q: Which model should I choose?**
A: Start with `llama3.1:8b` - excellent balance of quality and speed. Use `qwen2.5:7b` if focusing on math.

**Q: Can I use multiple models?**
A: Yes! Download multiple and switch via configuration. Useful for different use cases.

**Q: How do I update models?**
A: Run `ollama pull llama3.1:8b` again to get the latest version.

**Q: Is it really private?**
A: Yes, 100%. All processing happens locally. No data is sent to external servers.

## Next Steps

1. **Install Ollama** following instructions for your OS
2. **Download recommended model:** `ollama pull llama3.1:8b`
3. **Test installation:** `ollama run llama3.1:8b "What is 2+2?"`
4. **Configure application** with `ai.tutor.provider=ollama`
5. **Start application** and test AI feedback
6. **Monitor performance** and adjust model/settings as needed
7. **Consider GPU upgrade** if responses are too slow

## Troubleshooting Checklist

- [ ] Ollama is installed (`ollama --version`)
- [ ] Ollama service is running (`curl http://localhost:11434/api/version`)
- [ ] Model is downloaded (`ollama list`)
- [ ] GPU is detected if available (`nvidia-smi`)
- [ ] Application is configured (`ai.tutor.provider=ollama`)
- [ ] Model name matches configuration (`ollama.model=llama3.1:8b`)
- [ ] Sufficient disk space (10+ GB free)
- [ ] Sufficient RAM (8+ GB available)

If issues persist, check application logs at `logs/aimathtutor.log` for detailed error messages.
