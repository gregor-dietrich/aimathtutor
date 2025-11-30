FROM redhat/ubi9-minimal:9.7

ENV LANGUAGE='en_US:en'

ENV OLLAMA_HOST=0.0.0.0:11434
EXPOSE 11434

COPY payload/ /usr

ENTRYPOINT ["/bin/sh", "-c", "exec ollama serve 2>&1 | tee /var/log/ollama.log"]
CMD []
