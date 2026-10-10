FROM redhat/ubi9-minimal:9.8@sha256:5ed244b62bbf4095080144d9d35eb8fcd3d39a9801f94aadd63b9d10978a01ae

ENV LANGUAGE='en_US:en'

ENV OLLAMA_HOST=0.0.0.0:11434
EXPOSE 11434

COPY payload/ /usr

ENTRYPOINT ["/bin/sh", "-c", "exec ollama serve 2>&1 | tee /var/log/ollama.log"]
CMD []
