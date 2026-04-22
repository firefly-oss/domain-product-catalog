# Product Domain Product Catalog

A reactive domain-layer microservice that orchestrates the full product catalog lifecycle -- from registration through publication, suspension, and retirement. Built on [FireflyFramework](https://github.com/fireflyframework/) and Spring WebFlux, this service uses CQRS and Saga orchestration to coordinate up to 19 registration steps in a single atomic workflow with automatic compensation on failure, delegating persistence to the **core-common-product-mgmt** platform service.

> **Repository:** [https://github.com/firefly-oss/domain-product-catalog](https://github.com/firefly-oss/domain-product-catalog)

---

## Overview

Product Domain Product Catalog is the domain orchestration layer responsible for:

- **Product registration** -- orchestrates the creation of a complete product definition including category, subtype, fee structure, bundle, pricing, relationships, documentation, features, lifecycle, limits, localization, and versioning in a single transactional saga.
- **Product lifecycle management** -- publish, suspend, resume, and retire products through status transitions.
- **Product cloning** -- atomically duplicate an existing product into DRAFT status with a new code, including its configurations, localizations, relationships and documentation requirements (`CloneProductSaga`, with per-step compensation).
- **Retirement with migration** -- atomically flip a product to RETIRED while attaching a `MIGRATION_POINTER` configuration that references an active replacement for the grace period (`RetireWithMigrationSaga`).
- **What-if simulation** -- project eligibility and pricing for a product against a customer profile by reading the product's existing configurations locally, without calling any pricing service.
- **Catalog aggregation** -- serve a nested category-and-product tree for a tenant, plus version list and naive JSON-path-based version comparison.
- **Fee structure linking** -- associate general ledger (GL) posting rule sets to products.
- **Product information retrieval** -- query full product details via the CQRS `QueryBus`.
- **Event-driven architecture** -- every saga step emits domain events to Kafka for downstream consumers.
- **SDK generation** -- auto-generates a reactive Java client SDK from the OpenAPI specification.

---

## Architecture

### Module Structure

```
domain-product-catalog (parent POM)
|-- domain-product-catalog-core         # Domain logic: commands, handlers, queries, services, sagas, constants
|-- domain-product-catalog-interfaces   # Interface/contract layer between core and web
|-- domain-product-catalog-infra        # Infrastructure: API client factory, configuration properties
|-- domain-product-catalog-web          # Spring Boot application, REST controllers, OpenAPI config
|-- domain-product-catalog-sdk          # Auto-generated reactive client SDK (OpenAPI Generator)
```

### Tech Stack

| Layer              | Technology                                                                                            |
|--------------------|-------------------------------------------------------------------------------------------------------|
| Language           | Java 25                                                                                               |
| Framework          | Spring Boot, Spring WebFlux (reactive)                                                                |
| Virtual Threads    | Enabled (`spring.threads.virtual.enabled: true`)                                                      |
| CQRS / Saga       | [FireflyFramework Transactional Saga Engine](https://github.com/fireflyframework/) with `CommandBus` and `QueryBus` |
| Event Streaming    | Kafka (via FireflyFramework EDA publisher)                                                            |
| API Documentation  | SpringDoc OpenAPI (Swagger UI)                                                                        |
| Metrics            | Micrometer + Prometheus                                                                               |
| Mapping            | MapStruct + Lombok                                                                                    |
| SDK Generation     | OpenAPI Generator Maven Plugin (webclient / reactive)                                                 |
| Build              | Maven (multi-module)                                                                                  |
| BOM                | `fireflyframework-bom:26.01.01`                                                                       |

### Key Dependencies (FireflyFramework)

| Artifact                        | Purpose                                     |
|---------------------------------|---------------------------------------------|
| `fireflyframework-parent`       | Parent POM with managed dependency versions |
| `fireflyframework-bom`          | Bill of Materials for all framework modules  |
| `fireflyframework-web`          | Common web configuration and filters         |
| `fireflyframework-domain`       | Domain building blocks (Saga, CQRS)          |
| `fireflyframework-utils`        | Shared utility classes                       |
| `fireflyframework-validators`   | Reusable validation components               |

### Sagas (Workflow Orchestrations)

| Saga                            | Steps                                                                                                                                                                                                     | Compensation |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| `RegisterProductSaga`           | registerProductCategory -> registerProductSubtype -> registerFeeStructure -> registerProductBundle -> registerFeeComponent -> registerFeeApplicationRule -> registerProduct -> registerProductFeeStructure -> registerProductBundleItems -> registerProductPricing -> registerProductRelationship -> registerProductDocumentation -> registerProductDocumentationRequirement -> registerProductFeatures -> registerProductLifecycle -> registerProductLimits -> registerProductLocalization -> registerVersion -> registerProductPricingLocalization | Full rollback via compensate methods for every step |
| `UpdateProductSaga`             | updateProduct                                                                                                                                                                                             | None         |
| `RegisterProductFeeStructureSaga` | registerProductFeeStructure                                                                                                                                                                             | None         |
| `GetProductInfoSaga`            | getProductInfo (query)                                                                                                                                                                                    | None         |
| `CloneProductSaga`              | loadSourceProduct -> createClonedProduct -> cloneConfigurations -> cloneLocalizations -> cloneRelationships -> cloneDocumentationRequirements                                                             | Step 2 deletes the cloned product; steps 3-6 iterate the stored IDs and delete each child. Read-only step 1 has an explicit no-op compensation. All create steps use `requireId(...)` so a null id in the SDK response fails the step and triggers rollback instead of silently dropping state. |
| `RetireWithMigrationSaga`       | validateTargetProduct -> retireSourceProduct -> createMigrationPointer                                                                                                                                    | Step 2 re-applies the captured previous status via **in-place mutation** of the fetched DTO (preserves server-managed fields); step 3 deletes the `MIGRATION_POINTER` configuration. Read-only step 1 has an explicit no-op compensation. |

The `RegisterProductSaga` supports **ExpandEach** for collection-type inputs, allowing multiple fee structures, bundle items, relationships, documentation entries, features, lifecycle stages, limits, localizations, and versions to be registered as parallel sub-steps.

### Read-only services (no saga)

| Service | Purpose |
|---|---|
| `ProductSimulationService` | Projects eligibility and a pricing projection by loading the product's existing configurations via `ProductConfigurationApi`. Eligibility rules are parsed from JSON config values (`minAge`, `maxAge`, `minIncome`, `maxIncome`, `allowedSegments`); pricing uses a local amortization formula (`monthlyPayment = amount * r / (1 - (1+r)^-n)`). Fail-open per-rule on parse errors, fail-closed on status != ACTIVE or missing pricing scheme. No PII logged. |
| `ProductCatalogTreeService` | Builds a nested category tree via `ProductCategoryApi.filterCategories` then fans out `ProductApi.filterProducts` per category (bounded concurrency). Enforces a max depth of 5 and logs a warning on detected cycles. |
| `ProductVersionService` | Lists versions via `ProductVersionApi.filterProductVersions`; compares two versions by snapshotting only business-meaningful fields (`versionNumber`, `versionDescription`, `effectiveDate`, and attached configurations) and emitting a naive JSON-path diff (`added` / `removed` / `changed`). Audit fields and IDs are excluded from the diff to avoid noise. |

### Domain Events

All events are published to the `domain-layer` Kafka topic:

- `productCategory.registered`
- `productSubtype.registered`
- `feeStructure.registered`
- `productBundle.registered`
- `feeComponent.registered`
- `feeApplicationRule.registered`
- `product.registered`
- `productFeeStructure.registered`
- `productBundleItems.registered`
- `productPricing.registered`
- `productRelationship.registered`
- `productDocumentation.registered`
- `productDocumentationRequirement.registered`
- `productFeatures.registered`
- `productLifecycle.registered`
- `productLimits.registered`
- `productLocalization.registered`
- `version.registered`
- `productPricingLocalization.registered`
- `product.updated`
- `product.retrieved`
- `catalog.clone.source-loaded`
- `catalog.clone.product-created`
- `catalog.clone.configurations-created`
- `catalog.clone.localizations-created`
- `catalog.clone.relationships-created`
- `catalog.clone.documentation-requirements-created`
- `catalog.retire-with-migration.target-validated`
- `catalog.retire-with-migration.source-retired`
- `catalog.retire-with-migration.migration-pointer-created`
- `catalog.tree.requested`
- `catalog.versions.listed`
- `catalog.versions.compared`
- `catalog.product.simulated`

---

## Setup

### Prerequisites

- **Java 25** (JDK)
- **Apache Maven 3.9+**
- **Apache Kafka** (default: `localhost:9092`)
- **core-common-product-mgmt** service running (default: `http://localhost:8082`)

### Environment Variables

| Variable         | Default       | Description                            |
|------------------|---------------|----------------------------------------|
| `SERVER_ADDRESS` | `localhost`   | Server bind address                    |
| `SERVER_PORT`    | `8080`        | Server listening port                  |

### Application Configuration (application.yaml)

| Property                                                | Default              | Description                         |
|---------------------------------------------------------|----------------------|-------------------------------------|
| `firefly.cqrs.command.timeout`                          | `30s`                | Command execution timeout           |
| `firefly.cqrs.query.timeout`                            | `15s`                | Query execution timeout             |
| `firefly.cqrs.query.cache-ttl`                          | `15m`                | Query cache time-to-live            |
| `firefly.eda.publishers.kafka.default.bootstrap-servers`| `localhost:9092`     | Kafka bootstrap servers             |
| `firefly.eda.publishers.kafka.default.default-topic`    | `domain-layer`       | Default Kafka topic                 |
| `api-configuration.common-platform.product-mgmt.base-path` | `http://localhost:8082` | Product management service URL |

### Spring Profiles

| Profile   | Logging Behavior                             | Swagger UI |
|-----------|----------------------------------------------|------------|
| `dev`     | DEBUG for `com.firefly`, R2DBC, Flyway       | Enabled    |
| `testing` | DEBUG for `com.firefly`, INFO for R2DBC      | Enabled    |
| `prod`    | WARN for root, INFO for `com.firefly`        | Disabled   |

### Build

```bash
# Full build (all modules)
./mvnw clean install

# Build skipping tests
./mvnw clean install -DskipTests
```

### Run

```bash
# Run with default profile
./mvnw -pl domain-product-catalog-web spring-boot:run

# Run with dev profile
./mvnw -pl domain-product-catalog-web spring-boot:run -Dspring-boot.run.profiles=dev

# Run as JAR
java -jar domain-product-catalog-web/target/domain-product-catalog.jar
```

---

## API Endpoints

**Base path:** `/api/v1/products`

### Product Catalog

| Method | Endpoint                                    | Description                                                        |
|--------|---------------------------------------------|--------------------------------------------------------------------|
| POST   | `/api/v1/products`                          | Register a complete product definition (category, subtype, fees, pricing, features, etc.) |
| GET    | `/api/v1/products/{productId}`              | Retrieve product information by ID                                 |
| POST   | `/api/v1/products/{productId}/publish`      | Publish a sellable version (freeze linked configurations, status = ACTIVE) |
| POST   | `/api/v1/products/{productId}/suspend`      | Temporarily suspend product eligibility (status = PROPOSED)        |
| POST   | `/api/v1/products/{productId}/resume`       | Resume product eligibility (status = ACTIVE)                       |
| POST   | `/api/v1/products/{productId}/retire`       | Retire product; existing accounts/loans remain (status = RETIRED)  |
| POST   | `/api/v1/products/{productId}/posting-rule-set` | Link a GL posting rule set (fee structure) to the product      |
| POST   | `/api/v1/products/{productId}/clone`        | Atomically duplicate the product into DRAFT with a new code (body: `CloneProductRequest { newProductCode, tenantId? }`). Runs `CloneProductSaga`. |
| POST   | `/api/v1/products/{productId}/simulate`     | What-if projection of eligibility + pricing for a customer profile (body: `SimulateProductRequest`). Read-only; never an error signal — missing data maps to structured ineligibility reasons. |
| POST   | `/api/v1/products/{productId}/retire-with-migration` | Flip the source to RETIRED and attach a `MIGRATION_POINTER` pointing to `targetProductId` (body: `RetireWithMigrationRequest { targetProductId, gracePeriodEndDate, reason }`). Runs `RetireWithMigrationSaga`. |
| GET    | `/api/v1/products/catalog-tree?tenantId={uuid}` | Nested tree of categories, subcategories and products scoped to a tenant. |
| GET    | `/api/v1/products/{productId}/versions`     | List all versions of a product (summary form).                     |
| GET    | `/api/v1/products/{productId}/versions/compare?v1={uuid}&v2={uuid}` | JSON-path diff (`added` / `removed` / `changed`) between two versions including their attached configurations. |

### Product Lifecycle State Machine

```
PROPOSED  --publish-->  ACTIVE  --suspend-->  PROPOSED
                        ACTIVE  --retire-->   RETIRED
PROPOSED  <--resume--   PROPOSED (from suspend)
```

### OpenAPI / Swagger UI

- **API Docs (JSON):** `GET /v3/api-docs`
- **Swagger UI:** `GET /swagger-ui.html`

---

## Development Guidelines

### Project Conventions

- **Reactive programming** -- all service methods return `Mono<T>` or `Flux<T>`. Never block.
- **CQRS pattern** -- commands mutate state via `CommandBus`; queries read state via `QueryBus`.
- **Saga orchestration** -- multi-step workflows are defined as `@Saga` classes with `@SagaStep` methods and compensation handlers for rollback.
- **Step events** -- every saga step is annotated with `@StepEvent` to publish domain events automatically.
- **ExpandEach** -- use `ExpandEach.of(collection)` in `StepInputs` to fan-out a single saga step across multiple items in a collection.
- **Immutable commands** -- use Lombok `@With` for creating modified copies (e.g., `command.withProductId(id)`).
- **Constants** -- all saga names, step IDs, compensation method names, and event types are defined in `RegisterProductConstants` and `GlobalConstants`.

### Module Responsibilities

- **core** -- pure domain logic; no Spring Web dependencies. Contains commands, handlers, queries, services (interfaces and implementations), sagas/workflows, and constants.
- **interfaces** -- connects core to the outside world; depends on core.
- **infra** -- infrastructure concerns: API client factory for downstream service communication (19 API clients for the product-mgmt platform), configuration properties.
- **web** -- Spring Boot application entry point, REST controllers, OpenAPI definition. Depends on interfaces.
- **sdk** -- auto-generated client SDK from the OpenAPI spec. Consumers use this to call the service programmatically.

### Downstream API Clients (Infra Layer)

The `ClientFactory` creates beans for all platform service APIs:

| API Client                          | Purpose                              |
|-------------------------------------|--------------------------------------|
| `ProductApi`                        | Product CRUD                         |
| `ProductCategoryApi`                | Product category management          |
| `ProductSubtypeApi`                 | Product subtype management           |
| `FeeStructureApi`                   | Fee structure definitions            |
| `ProductFeeStructureApi`            | Product-to-fee-structure linkage     |
| `ProductFeeComponentApi`            | Fee component management             |
| `FeeApplicationRuleApi`             | Fee calculation rule management      |
| `ProductBundleApi`                  | Product bundle management            |
| `ProductBundleItemApi`              | Bundle item management               |
| `ProductPricingApi`                 | Product pricing management           |
| `ProductPricingLocalizationApi`     | Pricing localization management      |
| `ProductRelationshipApi`            | Product relationship management      |
| `ProductDocumentationApi`           | Product documentation management     |
| `ProductDocumentationRequirementsApi` | Documentation requirement management |
| `ProductFeatureApi`                 | Product feature management           |
| `ProductLifecycleApi`               | Product lifecycle management         |
| `ProductLimitApi`                   | Product limit management             |
| `ProductLocalizationApi`            | Product localization management      |
| `ProductVersionApi`                 | Product versioning management        |

### Adding a New Domain Operation

1. Create a command class in `core/products/commands/`.
2. Create a handler in `core/products/handlers/`.
3. If multi-step, create or extend a saga in `core/products/workflows/`.
4. Add constants (saga name, step ID, compensate method, event type) to `RegisterProductConstants`.
5. Wire through the `ProductCatalogService` interface and `ProductCatalogServiceImpl`.
6. Expose via a controller endpoint in the `web` module.

---

## Monitoring

### Health and Readiness

| Endpoint                              | Description                |
|---------------------------------------|----------------------------|
| `GET /actuator/health`                | Overall application health |
| `GET /actuator/health/liveness`       | Kubernetes liveness probe  |
| `GET /actuator/health/readiness`      | Kubernetes readiness probe |

### Metrics

| Endpoint                     | Description                         |
|------------------------------|-------------------------------------|
| `GET /actuator/info`         | Application build information       |
| `GET /actuator/prometheus`   | Prometheus-format metrics scraping  |

CQRS command and query metrics are enabled via:
```yaml
firefly.cqrs.command.metrics-enabled: true
firefly.cqrs.command.tracing-enabled: true
```

---

## License

Proprietary -- Firefly Software Solutions Inc.
