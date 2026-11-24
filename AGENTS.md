# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot application built with Maven. Application code lives under `src/main/java/com/smartclassroom/smart_classroom`, organized by responsibility: `controller`, `service`, `repository`, `entity`, `dto`, and `config`. Thymeleaf pages are in `src/main/resources/templates`; shared CSS and JavaScript are in `src/main/resources/static`. Runtime configuration is in `src/main/resources/application.properties`. Tests mirror the application package under `src/test/java`.

## Build, Test, and Development Commands

Use the Maven wrapper so the project’s Maven version is consistent across environments:

- `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`) starts the app locally.
- `./mvnw test` (Windows: `mvnw.cmd test`) compiles and runs tests.
- `./mvnw package` (Windows: `mvnw.cmd package`) builds the deployable artifact in `target/`.

The application uses MySQL at runtime; configure connection details through local environment/configuration before starting it. Do not commit credentials.

## Coding Style & Naming Conventions

Follow the existing Java package layout and standard Java conventions: four-space indentation, `UpperCamelCase` types, `lowerCamelCase` methods and fields, and uppercase underscore-separated constants. Keep controllers focused on web requests, put business logic in services, and use repositories for persistence. Name Thymeleaf files with lowercase kebab-case (for example, `student-dashboard.html`) and static assets by purpose (such as `dashboard.css`). No formatter or linter is configured in `pom.xml`; keep changes consistent with neighboring files.

## Testing Guidelines

Tests use JUnit 5 and Spring Boot’s test support. Name test classes `*Tests` and test methods for the behavior they verify. Place tests in the matching package under `src/test/java`. Run `mvnw.cmd test` from the repository root; add focused tests for changes to services, controllers, persistence, or security behavior.

## Commit & Pull Request Guidelines

Git metadata is not available in this checkout, so repository-specific commit conventions could not be confirmed. Use short imperative commit subjects (for example, `Add parent progress view`). Pull requests should explain the user-visible change, list relevant verification commands and results, link related issues when applicable, and include screenshots for template or styling changes.

## Security & Configuration Tips

Keep secrets and machine-specific database settings out of version control. Review `SecurityConfig` when changing access control, and validate role-specific behavior for student, teacher, parent, and admin pages.