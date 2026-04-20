# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven multi-module Java project for SeaDAS/SNAP integrations. The root `pom.xml` builds the active reactor modules: `seadas-processing`, `seadas-watermask-operator`, `seadas-bathymetry-operator`, `seadas-contour-operator`, `seadas-image-animator`, `seadas-earthdata-cloud-toolbox`, `seadas-metadata-tools`, and `seadas-kit`. Java sources live under `src/main/java`, tests under `src/test/java`, and module resources under `src/main/resources`. Top-level `docs/` holds release and developer notes; `bin/` and `keystore/` contain support assets.

## Build, Test, and Development Commands
Use Java and Maven versions compatible with the root build properties (`maven.compiler.release=17`; some modules, such as `seadas-earthdata-cloud-toolbox`, compile with Java 21 preview features).

- `mvn clean install -DskipTests`: full reactor build for packaging and local integration checks.
- `mvn test`: run the enabled unit tests across the reactor.
- `mvn -pl seadas-processing -am test`: test one module and any required upstream modules.
- `mvn -pl seadas-earthdata-cloud-toolbox -am package`: rebuild a single feature module quickly.

`README.md` also expects sibling checkouts of `snap-engine`, `snap-desktop`, and `optical-toolbox` for full IDE and application-level development.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, braces on the same line, and descriptive method names. Keep package names lowercase (`gov.nasa.gsfc.seadas...`), classes in PascalCase, methods and fields in camelCase, and constants in `UPPER_SNAKE_CASE`. No formatter or lint plugin is enforced in the root build, so match surrounding code closely and keep imports tidy.

## Testing Guidelines
The repository uses JUnit 4-style tests (`@Test`, `org.junit.Assert`). Name test classes with the `*Test` suffix and place them beside the code they cover in the matching module. Add or update focused tests with each behavior change; there is no configured coverage gate, so contributors are responsible for maintaining meaningful test depth.

## Commit & Pull Request Guidelines
Recent history uses short, imperative summaries such as `Rename panopoly to metadata in folder and files` and `Removed the print statements for debug.` Keep commits scoped to one logical change. Pull requests should identify affected modules, describe build/test commands run, link related issues, and include screenshots when UI dialogs or rendered outputs change.

## Configuration Tips
Do not commit local machine paths, Earthdata credentials, or ad hoc config edits. Treat files under `config/`, `keystore/`, and runtime user data such as `target/userdir/` as environment-specific unless the change is intentionally part of the product.
