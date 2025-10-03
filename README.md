# AI Math Tutor

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

## 🔧 Preparation

Edit `src/main/resources/application.properties` to set the backend URL if needed.

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

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:9069/q/dev/>.

### 🏭 Production mode

> **_NOTE:_** The application must be packaged for production mode **before** doing this (see below).

It is recommended to use Docker Compose to run the application in production mode:

```shell script
mkdir -p logs/aimathtutor # only required once, before first launch
sudo chown -R 185:185 logs # only required once, before first launch
docker compose up -d --build
```

> _**NOTE:**_ You need to create the directory `logs/aimathtutor` relative to where the Compose file is located.

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
docker build . -f src/main/docker/Dockerfile.jvm -t aimathtutor:1.0.0-SNAPSHOT
```

If you want to learn more about building Docker images, please consult <https://quarkus.io/guides/container-image>.

### 🐇 Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative -Pproduction
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true -Pproduction
```

You can then execute your native executable with: `./target/aimathtutor-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## 📖 Related Guides

- Quarkus ([guide](https://quarkus.io/guides/)): The main framework for building Java applications with a focus on cloud-native and microservices architectures.
- ArC ([guide](https://quarkus.io/guides/cdi-reference)): A dependency injection framework that is part of Quarkus, providing support for CDI (Contexts and Dependency Injection).
- REST Client ([guide](https://quarkus.io/guides/rest-client)): A client for making RESTful calls to other services.
- Vaadin Flow ([guide](https://vaadin.com/docs/latest/flow/integrations/quarkus)): Vaadin Flow is a unique framework that lets you build web apps without writing HTML or JavaScript
