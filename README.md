# Product Domain Product Catalog

A reactive domain-layer microservice that orchestrates the full product catalog lifecycle -- from registration through publication, suspension, and retirement. Built on [FireflyFramework](https://github.com/fireflyframework/) and Spring WebFlux, this service uses CQRS and Saga orchestration to coordinate up to 19 registration steps in a single atomic workflow with automatic compensation on failure, delegating persistence to the **common-platform-product-mgmt** platform service.

> **Repository:** [https://github.com/firefly-oss/product-domain-product-catalog](https://github.com/firefly-oss/product-domain-product-catalog)

---

## Overview

Product Domain Product Catalog is the domain orchestration layer responsible for:

- **Product registration** -- orchestrates the creation of a complete product definition including category, subtype, fee structure, bundle, pricing, relationships, documentation, features, lifecycle, limits, localization, and versioning in a single transactional saga.
- **Product lifecycle management** -- publish, suspend, resume, and retire products through status transitions.
- **Fee structure linking** -- associate general ledger (GL) posting rule sets to products.
- **Product information retrieval** -- query full product details via the CQRS `QueryBus`.
- **Event-driven architecture** -- every saga step emits domain events to Kafka for downstream consumers.
- **SDK generation** -- auto-generates a reactive Java client SDK from the OpenAPI specification.

---

## Architecture

### Module Structure

```
product-domain-product-catalog (parent POM)
|-- product-domain-product-catalog-core         # Domain logic: commands, handlers, queries, services, sagas, constants
|-- product-domain-product-catalog-interfaces   # Interface/contract layer between core and web
|-- product-domain-product-catalog-infra        # Infrastructure: API client factory, configuration properties
|-- product-domain-product-catalog-web          # Spring Boot application, REST controllers, OpenAPI config
|-- product-domain-product-catalog-sdk          # Auto-generated reactive client SDK (OpenAPI Generator)
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

The `RegisterProductSaga` supports **ExpandEach** for collection-type inputs, allowing multiple fee structures, bundle items, relationships, documentation entries, features, lifecycle stages, limits, localizations, and versions to be registered as parallel sub-steps.

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

---

## Setup

### Prerequisites

- **Java 25** (JDK)
- **Apache Maven 3.9+**
- **Apache Kafka** (default: `localhost:9092`)
- **common-platform-product-mgmt** service running (default: `http://localhost:8082`)

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
./mvnw -pl product-domain-product-catalog-web spring-boot:run

# Run with dev profile
./mvnw -pl product-domain-product-catalog-web spring-boot:run -Dspring-boot.run.profiles=dev

# Run as JAR
java -jar product-domain-product-catalog-web/target/product-domain-product-catalog.jar
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
