# AGENTS.md

Poja-generated Spring Boot 3 school-management project ("PROG4 final exam"). Base package `org.cocojojo.mg`.

## Commands

- Run all tests: `./gradlew test` (JUnit 5). Requires **Docker** — tests boot a shared Testcontainers Postgres 13.9. CI also runs this.
- Single test class: `./gradlew test --tests org.cocojojo.mg.conf.FacadeIT`
- Format: `./format.sh` (runs `google-java-format` 1.23 on all `src/**/*.java`). Google style, 2-space indent. CI fails if `./format.sh && git diff --exit-code` differs, so run it before committing.
- Java 21, Gradle 8.5 wrapper, Spring Boot 3.2.2. Do not bump these.

## Architecture constraints

- Files marked `@PojaGenerated` (Poja template: `endpoint/event`, `endpoint/rest/controller/health`, `file/*`, `handler/*`, `mail/*`, `repository/model` templates, `conf/*` test classes) are generated scaffolding — do not edit unless fixing a real bug.
- Three data layers, name them accordingly:
  - `model/` — domain records and enums (not `@Entity`).
  - `repository/model/J*.java` — JPA entities (J-prefix) using constructor-less Lombok `@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`.
  - `endpoint/rest/controller/dto/*Request|*Response` — REST DTO records, Lombok `@Builder`.
- Controllers live in `endpoint/rest/controller/`; only `health/` exists so far (application controllers are the actual exam work).
- Enums map to Postgres native enums: `@Enumerated(STRING)` + `@JdbcTypeCode(NAMED_ENUM)`. DB enum types created in migration `V43__Create_enum_types.sql`.
- Soft delete via `@SQLDelete` + `@SQLRestriction("is_deleted=false")` on entities with an `is_deleted` column. `@SQLRestriction` auto-filters every query incl. joins — entities referencing a soft-deleted table keep working, but a raw type-unsafe deletion is preferred for join-heavy reads. Prefer `@SQLDelete` pattern over manual deletion.
- JPA models read the nightly-driven `gradle`-generated schema; keep `repository/model` in sync with `db/migration`.

## DB / Flyway

- Migrations in `src/main/resources/db/migration/V<number>__<name>.sql`, numbered sequentially (currently through V60). Always append a new `V<next>__`; never edit applied migrations.
- Local/test DB has no extra config file — datasource comes from `PostgresConf` (Testcontainers) via `FacadeIT`'s `@DynamicPropertySource`.

## Testing quirks

- In tests, never stop the shared Postgres in `@AfterAll` (see comment in `FacadeIT`) — it's shared across subclasses; use the shutdown hook already defined or let it run.
- Coverage: `test` is `finalizedBy` `jacocoTestCoverageVerification` (excludes `**/gen/**`); `jacocoTestReport` requires running tests and prints coverage rate.

## Fraction

- `model/Fraction` — a `record` normalizing to a reduced fraction (twos-complement-safe gcd) and throwing on a zero denominator; deserializes via `FractionDeserializer`. Validated by `FractionValidator` (coefficient in (0,1]).

## Workflow

- Branch-per-feature (`feat/*`); `preprod`/`prod` are deployed by Poja CD on push (no manual deploy).