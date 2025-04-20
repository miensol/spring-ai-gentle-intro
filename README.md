# Support Assistant

A Spring Boot application that uses AI to provide support assistance based on previous support tickets.

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose (for running with containers)
- Ollama (for Mac OSX users running without Docker)

## Running with Docker Compose

The easiest way to run the application is using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database with pgvector extension
- Ollama AI service
- The Support Assistant application

The application will be available at http://localhost:8080

## Running on Mac OSX

Due to limitations with GPU sharing in Docker on Mac OSX, it's recommended to run Ollama natively on Mac instead of using the Docker container.

### Installing Ollama on Mac

1. Download Ollama from the official website: https://ollama.ai/download
2. Install the application
3. Start Ollama

### Running the application with native Ollama

1. Start Ollama natively on your Mac
2. Start the PostgreSQL database using Docker:
   ```bash
   docker-compose up -d postgres
   ```
3. Run the application with the correct Ollama URL:
   ```bash
   OLLAMA_BASE_URL=http://localhost:11434 ./gradlew bootRun
   ```

## Configuration

The application can be configured using environment variables:

- `OLLAMA_BASE_URL`: URL of the Ollama service (default: http://localhost:11434)
- `SPRING_DATASOURCE_URL`: JDBC URL for the PostgreSQL database
- `SPRING_DATASOURCE_USERNAME`: Username for the PostgreSQL database
- `SPRING_DATASOURCE_PASSWORD`: Password for the PostgreSQL database

## Models

By default, Ollama will use the "llama2" model. You can pull additional models using the Ollama CLI:

```bash
ollama pull mistral
ollama pull mxbai-embed-large
```

To use a different model, you can configure it in the application.yml file:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      model: mistral
      embedding:
        model: mxbai-embed-large
```