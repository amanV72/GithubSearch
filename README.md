# GitHub Repository Searcher

A production-ready Spring Boot REST API that integrates with the GitHub REST API to search repositories, persist and upsert repository metadata into a PostgreSQL database, and query stored repositories with database-level filtering and sorting.

---

## Tech Stack

* **Language:** Java 21
* **Framework:** Spring Boot 4.1.1
  * Spring Web (Spring MVC)
  * Spring Data JPA / Hibernate
  * Spring Validation (Jakarta Validation)
* **Database:** PostgreSQL 16
* **API Documentation:** OpenAPI 3 / Swagger UI (`springdoc-openapi-starter-webmvc-ui:3.1.1`)
* **Build Tool:** Gradle (Gradle Wrapper included)
* **Containerization:** Docker & Docker Compose
* **Testing:** JUnit 5, Mockito, AssertJ, Spring WebMvcTest

---

## Features & Architecture

* **RESTful Architecture:** Strictly REST-compliant endpoints returning and consuming standard JSON (no frontend/UI dependencies).
* **Layered Design & Separation of Concerns:**
  * `controller`: Handles HTTP requests, parameter extraction, and request validation.
  * `services`: Business logic, GitHub query construction, and DTO/entity transformations.
  * `repositories`: Spring Data JPA DAO executing database-level parameterized queries.
  * `clients`: Encapsulated REST client communication with the external GitHub API using Spring's `RestClient`.
  * `models`: Persistent PostgreSQL entity definitions.
  * `dto`: Request, response, and error transfer objects cleanly separated from database entities.
  * `exception`: Centralized `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` for uniform error responses.
* **Upsert Semantics (Deduplication):** Uses the GitHub repository ID as the primary key (`@Id`). Subsequent searches for previously saved repositories automatically update existing records in PostgreSQL rather than inserting duplicates.
* **Database-Level Filtering & Sorting:** Filtering by `language` and `minStars`, and sorting by `stars`, `forks`, or `updated` (`lastUpdated`) are executed directly inside PostgreSQL via JPQL and Spring Data `Sort`, avoiding in-memory stream processing.
* **Resilient Error Handling:** Centralized handling for input validation, illegal arguments, GitHub rate limits (`403`), upstream API errors (`502`), network failures (`503`), and database errors (`500`).

---

## Project Structure

```text
c:/Users/Dell/Downloads/githubsearch
├── src
│   ├── main
│   │   ├── java/com/freightfox/githubsearch
│   │   │   ├── clients
│   │   │   │   ├── GithubApiClient.java
│   │   │   │   └── GithubClientConfig.java
│   │   │   ├── controller
│   │   │   │   └── GithubSearchController.java
│   │   │   ├── dto
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── GithubApiResponse.java
│   │   │   │   ├── GithubSearchRequest.java
│   │   │   │   ├── GithubSearchResponse.java
│   │   │   │   └── Repositories.java
│   │   │   ├── exception
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── models
│   │   │   │   └── GithubRepositoryEntity.java
│   │   │   ├── repositories
│   │   │   │   └── GithubRepositoryDAO.java
│   │   │   ├── services
│   │   │   │   └── GithubSearchService.java
│   │   │   └── GithubsearchApplication.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── application-prod.yml
│   └── test
│       └── java/com/freightfox/githubsearch
│           ├── controller
│           │   └── GithubSearchControllerTest.java
│           ├── exception
│           │   └── GlobalExceptionHandlerTest.java
│           ├── services
│           │   └── GithubSearchServiceTest.java
│           └── GithubsearchApplicationTests.java
├── build.gradle
├── settings.gradle
├── Dockerfile
├── compose.yml
└── README.md
```

---

## Configuration & Environment Variables

The application can be configured via `application.yml` or environment variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/github_search` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | `github_user` | PostgreSQL username |
| `DB_PASSWORD` | `local_dev_password` | PostgreSQL password |
| `GITHUB_TOKEN` | *(empty)* | Optional GitHub Personal Access Token (recommended to avoid GitHub rate limits: 60 req/hr unauthenticated vs 5,000 req/hr authenticated) |
| `SERVER_PORT` | `8080` | Application HTTP server port |

---

## How to Run the Project

### Option 1: Running with Docker Compose (Recommended)

Spins up both the PostgreSQL database and the containerized Spring Boot service:

```bash
# 1. Start the stack in background
docker compose up --build -d

# 2. View container logs
docker compose logs -f app

# 3. Stop containers
docker compose down
```

The application will be accessible at `http://localhost:8080`.

---

### Option 2: Running Locally with Gradle

#### 1. Start PostgreSQL
Ensure PostgreSQL is running locally on port `5432` with database `github_search`, user `github_user`, and your password.

If using Docker for just PostgreSQL:
```bash
docker run --name github-search-postgres -e POSTGRES_DB=github_search -e POSTGRES_USER=github_user -e POSTGRES_PASSWORD=local_dev_password -p 5432:5432 -d postgres:16-alpine
```

#### 2. Build and Run the Application
```bash
# Set optional GitHub token for higher rate limits (Windows PowerShell)
$env:GITHUB_TOKEN="your_personal_access_token_here"

# Run the Spring Boot application
./gradlew bootRun
```

Or on Linux/macOS:
```bash
export GITHUB_TOKEN="your_personal_access_token_here"
./gradlew bootRun
```

---

## API Documentation (Swagger / OpenAPI)

Once the application is running, interactive API documentation is available at:

* **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Specification (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## API Reference

### 1. Search GitHub Repositories
Searches repositories on GitHub via the GitHub REST API, upserts the results into PostgreSQL, and returns the saved repositories.

* **Method:** `POST`
* **URL:** `/api/github/search`
* **Headers:** `Content-Type: application/json`
* **Request Body:**
  ```json
  {
    "query": "spring boot",
    "language": "Java",
    "sort": "stars"
  }
  ```
  * `query` *(required, string)*: Keyword or phrase to search. Cannot be blank.
  * `language` *(optional, string)*: Programming language filter (e.g. `Java`, `Python`, `Rust`).
  * `sort` *(optional, string)*: Sort field. Allowed values: `stars`, `forks`, `updated`. Defaults to `stars`.

* **Success Response (`200 OK`):**
  ```json
  {
    "message": "Repositories fetched and saved successfully",
    "repositories": [
      {
        "id": 10270250,
        "name": "spring-boot",
        "description": "Spring Boot helps you create production-grade applications",
        "owner": "spring-projects",
        "language": "Java",
        "stars": 75000,
        "forks": 12000,
        "lastUpdated": "2026-10-14T10:15:30Z"
      }
    ]
  }
  ```

* **Error Responses:**
  * `400 Bad Request` — Missing or blank `query`, or invalid `sort` parameter:
    ```json
    {
      "timestamp": "2026-10-14T10:15:30.123Z",
      "status": 400,
      "error": "Validation failed",
      "details": {
        "query": "Search query cannot be empty"
      }
    }
    ```
  * `403 Forbidden` — GitHub API rate limit exceeded:
    ```json
    {
      "timestamp": "2026-10-14T10:15:30.123Z",
      "status": 403,
      "error": "GitHub API rate limit exceeded or access forbidden"
    }
    ```
  * `502 Bad Gateway` — Upstream GitHub API returned an error.
  * `503 Service Unavailable` — Network connection to GitHub failed or timed out.
  * `500 Internal Server Error` — Database or internal error.

---

### 2. Retrieve Stored Repositories
Retrieves previously saved repositories from PostgreSQL with optional filtering and sorting applied at the database level.

* **Method:** `GET`
* **URL:** `/api/github/repositories`
* **Query Parameters:**
  * `language` *(optional, string)*: Filter by programming language (case-insensitive).
  * `minStars` *(optional, integer)*: Filter repositories having at least this many stars.
  * `sort` *(optional, string)*: Sort field: `stars`, `forks`, or `updated`. Defaults to `stars` (descending).

* **Example Request:**
  ```text
  GET /api/github/repositories?language=Java&minStars=500&sort=stars
  ```

* **Success Response (`200 OK`):**
  ```json
  [
    {
      "id": 10270250,
      "name": "spring-boot",
      "description": "Spring Boot helps you create production-grade applications",
      "owner": "spring-projects",
      "language": "Java",
      "stars": 75000,
      "forks": 12000,
      "lastUpdated": "2026-10-14T10:15:30Z"
    }
  ]
  ```

* **Error Responses:**
  * `400 Bad Request` — Invalid `sort` parameter (e.g. `?sort=invalid`):
    ```json
    {
      "timestamp": "2026-10-14T10:15:30.123Z",
      "status": 400,
      "error": "Invalid sort value. Allowed values: stars, forks, or updated"
    }
    ```
  * `500 Internal Server Error` — Database connectivity issue.

---

## Testing

The project includes unit test suites using **JUnit 5**, **Mockito**, and **Spring WebMvcTest**.

### Running Tests
To execute all unit tests:
```bash
./gradlew test --tests "com.freightfox.githubsearch.services.*" \
               --tests "com.freightfox.githubsearch.controller.*" \
               --tests "com.freightfox.githubsearch.exception.*"
```

### Test Suites Overview

| Test Class | Target Layer | Tests | Description |
| :--- | :--- | :---: | :--- |
| `GithubSearchServiceTest` | Service | 19 | Tests search flow, query construction, sorting defaults, empty responses, invalid response handling, argument capture for upsert, and DB query delegation. |
| `GithubSearchControllerTest` | Controller | 14 | MockMvc slice tests verifying HTTP contracts, request validation, query parameters, sort validation, and error payload structures. |
| `GlobalExceptionHandlerTest` | Exception | 9 | Tests mapping of validation errors, rate limits (403), upstream errors (502), connection timeouts (503), database errors (500), and generic exceptions. |

---

## Testing with Postman / cURL

### cURL Examples

#### Search and Save Repositories:
```bash
curl -X POST "http://localhost:8080/api/github/search" \
     -H "Content-Type: application/json" \
     -d '{
       "query": "spring-boot",
       "language": "Java",
       "sort": "stars"
     }'
```

#### Query Saved Repositories:
```bash
curl -X GET "http://localhost:8080/api/github/repositories?language=Java&minStars=1000&sort=stars"
```
