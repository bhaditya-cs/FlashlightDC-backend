# CLAUDE.md — FlashlightDC Backend

## Project Overview

Spring Boot 3.4.5 middleware (Java 17, Maven) that proxies the [Congress.gov API v3](https://api.congress.gov/v3), persists congressional bill/member data to Oracle, and provides AI bill summarization via Google Cloud Vertex AI (Gemini 2.5 Flash).

## Directory Structure

```
src/main/java/org/flashlightdc/flashlight/
  FlashlightDcApplication.java    — @SpringBootApplication entry point; loads .env before Spring starts
  client/
    CongressApiClient.java         — Reactive (WebClient) client for Congress.gov API
    VertexAiClient.java            — Synchronous Vertex AI client, wrapped in Mono for reactivity
    CongressApiException.java      — Simple RuntimeException for API errors
  config/
    CorsConfig.java                — CORS: GET-only on /api/** from ${frontend.url}
    WebClientConfig.java           — WebClient bean for Congress.gov (appends api_key + format=json params)
    VertexAiConfig.java            — VertexAI + GenerativeModel beans
  controller/
    BillController.java            — /api/bills/*
    MemberController.java          — /api/members/*
    SummarizationController.java   — /api/v1/bills/{congress}/{type}/{number}/summary
  service/
    BillService.java               — Fetch (from API) + persist (to DB); upserts by (congress, billType, billNumber)
    MemberService.java             — Fetch + persist members/terms; string PK (bioguideId)
    SummarizationService.java      — Orchestrates bill fetch → context extraction → Vertex AI summary
  entity/
    Bill.java                      — JPA entity, table "bills", unique on (congress, bill_type, bill_number)
    Member.java                    — JPA entity, table "members", PK is bioguideId (String)
    Sponsor.java                   — Join entity between Bill and Member, table "sponsors"
    Term.java                      — Entity, table "terms", child of Member
  repository/
    BillRepository.java            — JpaRepository<Bill, Long>
    MemberRepository.java          — JpaRepository<Member, String>
    SponsorRepository.java         — JpaRepository<Sponsor, Long>
    TermRepository.java            — JpaRepository<Term, Long>
  dto/                             — Java records for API response deserialization; @JsonIgnoreProperties(ignoreUnknown = true)
  util/
    TermListDeserializer.java      — Custom Jackson deserializer for nested {"item": [...]} in terms
    YesNoDeserializer.java         — Custom Jackson deserializer for "Y"/"N" → boolean
src/test/java/org/flashlightdc/flashlight/
  controller/                      — @WebFluxTest slice tests
  service/                         — @ExtendWith(MockitoExtension) unit tests + one @SpringBootTest integration
src/test/resources/
  application-test.properties      — Test profile (dummy API key)
  mockito-extensions/              — Configures mock-maker-subclass for mocking final classes
  samples/                         — Sample bill text for summarization tests
  output/                          — Expected AI output samples
```

Key config files:
- `pom.xml` — Maven build, dependencies, plugins
- `compose.yaml` — Local Oracle Free DB container
- `.env` — Secrets (gitignored): CONGRESS_API_KEY, DB_URL, PASSWORD, GOOGLE_CLOUD_PROJECT_ID, etc.
- `src/main/resources/application.properties` — Spring Boot config (secrets via env vars)

## Entry Points & Core Files

- **Main entry**: `FlashlightDcApplication.java` — loads `.env` into System properties, then `SpringApplication.run()`
- **REST API**: `BillController` (`/api/bills`), `MemberController` (`/api/members`), `SummarizationController` (`/api/v1/bills`)
- **External APIs**: `CongressApiClient` (reactive, `https://api.congress.gov/v3`), `VertexAiClient` (blocking, Gemini)
- **Persistence**: Spring Data JPA repositories under `repository/`, entities under `entity/`
- **Response deserialization**: DTOs in `dto/` — Java records with `@JsonIgnoreProperties(ignoreUnknown = true)`

## Common Development Commands

```bash
# Build (compile + test)
./mvnw clean package

# Compile only
./mvnw compile

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=BillControllerTest

# Run a single test method
./mvnw test -Dtest=BillControllerTest#testGetBills

# Skip tests
./mvnw package -DskipTests

# Run the application (requires .env with valid credentials)
./mvnw spring-boot:run

# Start local Oracle DB (Docker)
docker compose up -d
```

## Code Conventions & Patterns

- **Naming**: Java standard (PascalCase classes, camelCase methods/fields). Controllers named `*Controller`, services `*Service`, repositories `*Repository`.
- **DTOs**: Java records (`record`) for API response deserialization, annotated `@JsonIgnoreProperties(ignoreUnknown = true)`. Only `SummaryResponse` uses `@Data` + `@Builder` (Lombok class).
- **Entities**: `@Data` (Lombok), `@Entity`, `@Table`. PK generation is `GenerationType.IDENTITY`.
- **Reactive pattern**: All `CongressApiClient` methods return `Mono<T>` (Project Reactor). Controllers return `Mono<ResponseEntity<T>>`. Tests use `StepVerifier`.
- **Upsert pattern**: `BillService.saveBill()` checks for existing by unique key and sets `updatedAt`; `MemberService.saveMember()` deletes all existing terms and re-inserts.
- **Error handling**: No global `@ControllerAdvice`. `CongressApiClient` uses `.onStatus()` → `CongressApiException`. `SummarizationService` uses `.onErrorResume()` returning `status: "ERROR"`. Controllers return `ResponseEntity.notFound()` for missing DB entities.
- **Debug logging**: `System.out.println` used for raw API responses and deserializer verification (should eventually move to SLF4J).
- **No security framework** — no Spring Security, OAuth, or JWT. CORS is GET-only.
- **Lombok**: Annotation processor only (excluded from fat JAR). Requires `annotationProcessorPaths` in maven-compiler-plugin.

## Environment & Configuration

Required environment variables (loaded from `.env` by `Dotenv` in main method):

| Variable | Purpose | Default |
|----------|---------|---------|
| `CONGRESS_API_KEY` | Congress.gov API key | (required) |
| `DB_URL` | Oracle JDBC URL | (required) |
| `PASSWORD` | Oracle DB password | (required) |
| `FRONTEND_URL` | CORS allowed origin | `http://localhost:3000` |
| `GOOGLE_CLOUD_PROJECT_ID` | Vertex AI GCP project | (required for summarization) |
| `GOOGLE_CLOUD_LOCATION` | Vertex AI region | `us-central1` |
| `VERTEX_AI_MODEL_NAME` | Gemini model name | `gemini-2.5-flash` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to GCP service account JSON | (optional, for local dev) |

- `application.properties` resolves values from env vars/system properties via `${...}` placeholders.
- Test profile (`application-test.properties`) sets `congress.api.key=dummy-test-key` and `frontend.url=http://localhost:3000`.
- Hibernate DDL: `validate` — schema must exist before running (no auto-create/migration).

## Testing

- **Framework**: JUnit Jupiter 5 + Mockito (inline mock maker) + Spring Boot Test + Reactor `StepVerifier`
- **Controller tests**: `@WebFluxTest(ControllerClass.class)` with `@MockitoBean` on services
- **Service tests**: `@ExtendWith(MockitoExtension.class)` with `@Mock` / `@InjectMocks`
- **Integration test**: `@SpringBootTest` with `@ActiveProfiles("test")`
- **Mockito inline**: Configured via `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` (content: `mock-maker-subclass`); also duplicated in surefire `<argLine>`
- **Run single test**: `./mvnw test -Dtest=TestClassName`
- **Run single method**: `./mvnw test -Dtest=TestClassName#methodName`
- Tests use `StepVerifier.create(mono).expectNext(...).verifyComplete()` for reactive assertions

## Common Pitfalls & Gotchas

- **`.env` must exist** at project root with valid credentials, or the app will fail on startup with missing DB URL / API key.
- **Oracle wallet required**: The Oracle Cloud ATP connection requires a wallet directory (`Wallet_FLASHLIGHTDC`). The `@PostConstruct checkWallet()` method logs `TNS_ADMIN` path for debugging.
- **Two bills base paths**: `BillController` uses `/api/bills`, `SummarizationController` uses `/api/v1/bills`. CORS only allows `GET` requests, so POST endpoints in `BillController`/`MemberController` may work from `curl` but not from browsers.
- **No DB migrations**: Schema must be created manually on the Oracle instance. `hibernate.ddl-auto=validate` will fail if tables don't match entities.
- **Raw JSON deserialization**: Some endpoints (bill detail, members) pull raw JSON strings and deserialize manually — API schema changes may break them silently.
- **`MemberDetailResponse` is a plain class** (not a record) with a package-private field — Jackson can deserialize into it but serialization may not work as expected.
- **`VertexAiClient` uses blocking calls** (`generateContent()`) wrapped in `Mono.fromCallable()` on bounded elastic scheduler — this is intentional but means low throughput for summarization.
- **Debug `System.out.println`** statements are left in production code in deserializers and the API client.
- **Maven wrapper** (`./mvnw`) is checked in — always use it instead of system `mvn`.

## How to Extend / Add New Features

### Add a new API endpoint
1. Create DTO record in `dto/` if new response shape is needed
2. Add method in `CongressApiClient` following the pattern: return `Mono<T>`, use `.onStatus()` for errors
3. Add service method in appropriate `*Service` class
4. Add controller endpoint in `*Controller` (annotate with `@GetMapping`/`@PostMapping` etc.)
5. Write tests: `@WebFluxTest` for controller, `@ExtendWith(MockitoExtension)` for service

### Add a new entity/table
1. Create entity class in `entity/` with `@Entity`, `@Table`, `@Data`, `@Id`, etc.
2. Create repository interface in `repository/` extending `JpaRepository`
3. Create the table manually on the Oracle DB (remember: `ddl-auto=validate`)
4. If fetched from Congress.gov, add the API client method and DTO record

### Add a new service/integration
1. Create config class in `config/` for client beans
2. Create client class in `client/`
3. Add required dependencies to `pom.xml`
4. Add env vars to `.env` and note them in this document

### Add DB migrations
Currently no migration framework exists. Either:
- Manually run DDL on the Oracle instance, or
- Add Flyway/Liquibase and switch `ddl-auto` appropriately
