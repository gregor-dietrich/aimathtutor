# AI Math Tutor

AI Math Tutor is a full-stack web application that combines a Quarkus backend with a Vaadin frontend to deliver interactive math exercises. It embeds Graspable Math for hands-on symbolic manipulation and adds an AI tutor layer that answers questions and provides automated feedback to support learning.

## 🌟 Features

- Embedded Graspable Math workspace for interactive, manipulable math expressions and step-by-step student actions.
- Real-time AI tutor layer that analyzes student actions and provides feedback, hints, and congratulatory responses.
  - Can be disabled by either setting `ai.tutor.provider=mock` or even `ai.tutor.enabled=false`.
- Problem generation and management: generate and load problems into the Graspable canvas with categories, difficulty and hints.
- Lesson and exercise management: author and organize lessons and exercises, and expose exercise views for students and teachers.
- Comments attached to exercises for students and teachers to discuss solution steps.
- Session and event tracking: records student sessions and Graspable events for analytics and progress monitoring.
- Granular user management: support for users, user groups, and ranks/permissions to enable differentiated access and progress tracking.
- Vaadin + Quarkus architecture: server-rendered UI with direct CDI-injected services and tight integration between frontend and backend for low-latency feedback.

## 🤖 Supported AI Providers

- [Google](https://aistudio.google.com/api-keys)
- [Ollama](https://ollama.com/download) (untested)
- [OpenAI](https://platform.openai.com/api-keys) (untested)

## 📖 Documentation

- [Quickstart](https://github.com/gregor-dietrich/aimathtutor/blob/main/docs/QUICKSTART.md)
- [Build Guide](https://github.com/gregor-dietrich/aimathtutor/blob/main/docs/BUILD_GUIDE.md)
