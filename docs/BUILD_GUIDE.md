# Build Guide

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## ⚠️ Requirements

| Name | Build | Run | Download |
|----------|----------|----------|----------|
| JDK 21 | ✅ | ❌ | [Adoptium](https://adoptium.net/temurin/releases/?os=any&arch=any&version=21) |
| Maven | ✅ | ❌ | [Apache](https://maven.apache.org/download.cgi) |
| Docker | ❌ | ✅ | (see below) |

For Docker, you have 2 options:

- [Docker Engine](https://docs.docker.com/engine/install/)
- or [Docker Desktop](https://docs.docker.com/desktop/) (includes Docker Engine)

## 🔧 Setup

Edit `src/main/resources/application.properties` as needed, then run:

```shell script
./mvnw clean install -DskipTests
```

### 🧪 Tests

To run the tests, you can either omit the `-DskipTests` flag when using the command above, or execute the following command:

```shell script
./mvnw test
```

## 🚀 Running the application

### 🧑‍💻 Development mode

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:9001/q/dev/>.

### 🏭 Production mode

> **_NOTE:_** The application must be packaged for production mode **before** doing this (see below).

It is recommended to use Docker Compose to run the application in production mode:

```shell script
docker compose up -d --build
```

## 📦 Packaging the application

The application can be packaged using:

```shell script
./mvnw package -Pproduction
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar -Pproduction
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

### 🐳 Creating a Docker image

> **_NOTE:_** The application needs to be packaged for production mode **before** doing this (see above).

You can create a Docker image using:

```shell script
docker build . -f src/main/docker/Dockerfile.jvm -t aimathtutor:1.0.0
```

If you want to learn more about building Docker images, please consult <https://quarkus.io/guides/container-image>.

## 📖 Related Guides

- Quarkus ([guide](https://quarkus.io/guides/)): The main framework for building Java applications with a focus on cloud-native and microservices architectures.
- Vaadin Flow ([guide](https://vaadin.com/docs/latest/flow/integrations/quarkus)): Vaadin Flow is a unique framework that lets you build web apps without writing HTML or JavaScript
- ArC ([guide](https://quarkus.io/guides/cdi-reference)): A dependency injection framework that is part of Quarkus, providing support for CDI (Contexts and Dependency Injection).
- Datasource ([guide](https://quarkus.io/guides/datasource)): A Quarkus extension for connecting to databases using JDBC, JPA, Hibernate ORM, and more.
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): A Quarkus extension that simplifies the use of Hibernate ORM with a focus on ease of use and productivity.
- Hibernate Validator ([guide](https://quarkus.io/guides/hibernate-validator)): A Quarkus extension that integrates Hibernate Validator for bean validation, allowing you to validate your data models easily.
