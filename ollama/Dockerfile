FROM redhat/ubi9-minimal:9.8@sha256:bba2b4bbf0ed15cfe7dd72a1e17ae7d2015a141f306b304e1a2785c41400e39c

ENV LANGUAGE='en_US:en'

ENV OLLAMA_HOST=0.0.0.0:11434
EXPOSE 11434

COPY payload/ /usr

ENTRYPOINT ["/bin/sh", "-c", "exec ollama serve 2>&1 | tee /var/log/ollama.log"]
CMD []
