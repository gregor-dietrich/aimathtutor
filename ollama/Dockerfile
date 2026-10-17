FROM redhat/ubi9-minimal:9.8@sha256:c4c285d42941ebb2bd4da878479008e1e4536bb599aad1dbe568f89de9053661

ENV LANGUAGE='en_US:en'

ENV OLLAMA_HOST=0.0.0.0:11434
EXPOSE 11434

COPY payload/ /usr

ENTRYPOINT ["/bin/sh", "-c", "exec ollama serve 2>&1 | tee /var/log/ollama.log"]
CMD []
