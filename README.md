# PizzaDronz

![Java](https://img.shields.io/badge/Java-18-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven)
![Testing](https://img.shields.io/badge/testing-JUnit%205%20%7C%20WireMock-25A162?logo=junit5)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?logo=githubactions)

PizzaDronz is a Java application that validates pizza orders and plans safe drone deliveries across a constrained geographic area. It combines REST-style data ingestion, domain validation, computational geometry, graph search, JSON/GeoJSON serialization, automated testing, mutation testing, and continuous integration into one end-to-end system.

The project was designed as a practical software engineering exercise: the goal is not only to produce a route, but to build a maintainable and testable pipeline from external data through to delivery and flight-path output.

## Why this project is interesting

This project demonstrates several skills that transfer directly to production software:

- Designing a small application around clear responsibilities and domain objects.
- Consuming and deserializing external JSON data with Jackson.
- Validating business rules and returning explicit domain-level error codes.
- Implementing A* search with a priority queue, heuristic scoring, visited-node tracking, and obstacle avoidance.
- Applying computational geometry to point-in-polygon checks and line intersection tests.
- Producing machine-readable JSON and GeoJSON outputs.
- Testing at unit, integration, system, boundary, and algorithmic levels.
- Using WireMock to create deterministic REST-service test environments.
- Measuring quality with code coverage, mutation testing, and performance profiling.
- Automating build, test, mutation analysis, and packaging with GitHub Actions.

## Features

### Order validation

`OrderValidator` validates each order against the supplied restaurant data and business rules, including:

- 16-digit card number format.
- Three-digit CVV format.
- `MM/YY` expiry format and expiry-date comparison.
- Maximum pizzas per order.
- Pizza existence in a restaurant menu.
- All pizzas belonging to the same restaurant.
- Restaurant opening days.
- Total price, including the fixed order charge.

Valid orders are marked as `DELIVERED` with `NO_ERROR`. Invalid orders receive a specific `OrderValidationCode`, such as `CARD_NUMBER_INVALID`, `PIZZA_NOT_DEFINED`, `RESTAURANT_CLOSED`, or `TOTAL_INCORRECT`.

### Safe route planning

`PathFinder` uses an A* search over a grid of 16 compass directions. The implementation:

- Uses a `PriorityQueue` ordered by the A* score `f = g + h`.
- Uses Euclidean distance as the heuristic for the local coordinate system.
- Prevents nodes from entering no-fly zones.
- Handles the central area constraint by preventing a route from re-entering after it has left.
- Reconstructs the final route through parent-node links.
- Stops safely if a search exceeds its node-processing limit.
- Caches routes to restaurants so multiple orders from the same restaurant do not repeat the same expensive search.

### Geospatial calculations

`LngLatHandler` provides reusable geographic operations:

- Euclidean distance between longitude/latitude points.
- Proximity checks using the drone's allowed distance.
- Next-position calculation for the 16 compass directions.
- Point-in-polygon detection using a bounding-box fast path and ray casting.
- Line-intersection checks with care for horizontal and collinear polygon edges.

The local Cartesian approximation is appropriate for the small geographic area used by the delivery problem.

### Output generation

For a successful run, the application creates a `resultfiles/` directory containing:

- `deliveries-<date>.json`: order numbers, statuses, validation codes, and costs.
- `flightpath-<date>.json`: drone moves with start position, end position, and direction.
- `drone-<date>.geojson`: a `FeatureCollection` containing the complete flight path as a `LineString`.

These outputs make the result easy to consume by other applications and easy to visualise in GIS or GeoJSON-compatible tools.

## Architecture

The main processing pipeline is:

```text
CLI arguments
     |
     v
Main
     |
     +--> IOHandler --------> REST endpoints
     |          |
     |          +-----------> restaurants, orders, central area, no-fly zones
     |
     +--> OrderValidator ---> validated order statuses and codes
     |
     +--> PathFinder -------> safe route to and from each restaurant
     |
     +--> IOHandler --------> delivery JSON, flight-path JSON, GeoJSON
```

Key classes:

| Class | Responsibility |
| --- | --- |
| `Main` | Validates command-line arguments and orchestrates the application. |
| `IOHandler` | Retrieves REST data, deserializes JSON, caches routes, and writes outputs. |
| `OrderValidator` | Applies order and business-rule validation. |
| `PathFinder` | Calculates shortest safe paths using A* search. |
| `LngLatHandler` | Implements distance, movement, and polygon operations. |
| `Node` | Represents a search node and its A* state. |
| `Move` | Represents one drone movement in the output flight path. |
| `Delivery` | Represents the delivery summary written to JSON. |

## Technologies and tools

- **Java 18** for the application and use of modern language features such as records.
- **Maven** for dependency management, compilation, testing, and packaging.
- **Jackson** for JSON deserialization, Java time support, and output serialization.
- **GeoJSON-Jackson** for generating GeoJSON flight paths.
- **JUnit 5** for unit, integration, and system tests.
- **WireMock** for deterministic HTTP stubs that simulate restaurants, orders, central areas, and no-fly zones.
- **Log4j 2 and SLF4J** for structured application logging.
- **PIT Mutation Testing** for evaluating whether tests detect deliberately injected faults.
- **GitHub Actions** for automated verification on pushes and pull requests.
- **IntelliJ IDEA project configuration** for local development.

## Getting started

### Prerequisites

- JDK 18 or newer.
- Apache Maven 3.8+.
- Access to a REST service exposing the endpoints described below.

The project includes a local Maven repository under `repo/` for the supplied `IlpDataObjects` dependency.

### Build and test

```bash
mvn clean verify
```

This compiles the application, runs the test suite, and verifies the packaged project.

### Run the application

The command-line interface expects exactly two arguments:

```bash
java -jar target/PizzaDronz-1.0-SNAPSHOT.jar <date> <base-url>
```

Example:

```bash
java -jar target/PizzaDronz-1.0-SNAPSHOT.jar 2025-01-13 http://localhost:8080/
```

The application normalises the base URL so a trailing slash is optional. It validates the date and URL before requesting any data.

### Package the executable JAR

```bash
mvn package
```

The Maven Assembly Plugin creates an executable JAR containing the application dependencies. The configured entry point is `uk.ac.ed.inf.Main`.

## REST API contract

The base URL must provide these resources:

| Endpoint | Expected data |
| --- | --- |
| `GET /restaurants` | Restaurant definitions and menus. |
| `GET /orders/<date>` | Orders for the requested ISO-8601 date. |
| `GET /centralArea` | The central-area polygon. |
| `GET /noFlyZones` | An array of no-fly-zone polygons. |

The date should use `YYYY-MM-DD` format, for example `2025-01-13`.

## Testing strategy

The test suite is intentionally layered:

- **Unit tests** exercise coordinate calculations, polygon containment, angle validation, and order rules.
- **Integration tests** use WireMock to verify HTTP retrieval and JSON deserialization.
- **Path-finding tests** check no-fly-zone avoidance, multiple-order routes, difficult obstacle layouts, and an intentionally unreachable destination.
- **System tests** execute the complete application pipeline against easy, medium, and hard mocked environments with both small and larger order sets.
- **Boundary and negative cases** cover invalid payment data, expired cards, incorrect totals, undefined pizzas, closed restaurants, too many pizzas, and multi-restaurant orders.

Run the tests directly with:

```bash
mvn test
```

## Quality and engineering evidence

The repository includes supporting evidence from the development and testing process:

- `code_coverage.png` — code coverage results.
- `mutation_testing.png` — mutation-testing results.
- `performance_chart.png` — performance comparison chart.
- `performance_times.png` — measured execution times.
- `performance_profiler.png` — profiler output.
- `test_plan.pdf` — testing strategy and planned coverage.
- `Software_Testing_2022_3_Portfolio-1.pdf` — testing portfolio and analysis.
- `requirements.pdf` — original project requirements.

Mutation testing can be run locally with:

```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

The CI workflow runs verification, PIT mutation testing, and packaging automatically:

```text
GitHub Actions
  -> mvn clean verify
  -> mvn org.pitest:pitest-maven:mutationCoverage
  -> mvn package -DskipTests
```

## Project structure

```text
.
├── .github/workflows/maven.yml       # Continuous integration workflow
├── src/main/java/uk/ac/ed/inf/       # Application source
├── src/main/resources/               # Logging configuration
├── src/test/java/uk/ac/ed/inf/       # Unit, integration, path, and system tests
├── src/test/resources/__files/       # WireMock JSON fixtures
├── repo/                             # Local IlpDataObjects Maven dependency
├── META-INF/MANIFEST.MF              # JAR metadata
├── pom.xml                           # Maven build and dependency configuration
└── resultfiles/                      # Generated delivery outputs
```

## Design decisions worth highlighting

### Reusable domain boundaries

Input/output concerns, validation, geometry, and route planning are kept in separate classes. This makes the core logic easier to test independently from the REST service and file system.

### Deterministic test environments

WireMock fixtures allow tests to run without relying on a live external service. Different obstacle configurations provide repeatable easy, medium, and hard routing scenarios.

### Performance-conscious route generation

Routes are cached per restaurant because several orders may target the same location. The path finder also uses a bounding-box rejection step before the more expensive polygon test, reducing unnecessary geometry work.

### Explicit failure handling

Invalid command-line input, malformed dates, invalid URLs, REST failures, and output failures are surfaced with clear log messages and exit codes rather than being silently ignored.

## Potential extensions

The current implementation provides a strong baseline for a constrained delivery planner. Natural next steps would include:

- Adding a richer command-line interface for output-directory and logging configuration.
- Replacing the local planar approximation with a projection designed for larger geographic areas.
- Improving route-state deduplication and priority-queue updates for very large search spaces.
- Adding visualisation or a small web dashboard for the generated GeoJSON.
- Publishing coverage and mutation reports as CI artifacts.
- Adding property-based tests for polygon and route invariants.

## Author perspective

PizzaDronz is a compact example of turning a specification into a complete, verifiable software system. It showcases algorithmic problem-solving alongside practical engineering habits: dependency management, layered testing, deterministic integration environments, performance awareness, logging, and automated delivery through CI.

