<div align="center">
  <img src='https://eosc.eu/wp-content/uploads/2024/02/EOSC-Beyond-logo.png'></img>
</div>

# Resource Catalogue

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)
<a href="https://confluence.egi.eu/display/EOSCBeyond/Software+and+Services+Quality+Assurance+%28SQA%29+guidelines">
<img src="https://img.shields.io/badge/SQAaaS-Bronze-CD7F32"/></a>

---

## Description
The Resource Catalogue is a Java-based platform designed to manage and organize a diverse range of resources. 
It provides a comprehensive collection of research services developed collaboratively by various research communities 
and IT service providers. 

The project operates under the EOSC Beyond initiative, which aims to promote Open Science 
and foster innovation within the framework of the European Open Science Cloud (EOSC).
EOSC Beyond overall objective is to advance Open Science and innovation in research in the context of the European Open Science Cloud (EOSC) by providing new EOSC Core capabilities allowing scientific applications to find, compose and access multiple Open Science resources and offer them as integrated capabilities to researchers.

---

## Table of Contents
- [Description](#description)
- [Getting Started](#getting-started)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Testing](#testing)
- [Documentation Links](#documentation-links)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Packaging and running the application](#packaging-and-running-the-application)
- [Restore database](#restore-database)
- [Versioning](#versioning)
- [License](#license)
- [Authors](#authors)
- [Acknowledgements](#acknowledgements)


## Getting Started

Follow these steps to set up a development environment for Resource Catalogue:

## Prerequisites

* Java 21
* Maven 3.9+
* ActiveMQ 5.x.x
* Elasticsearch 7.17.x
* PostgreSQL 9.5+

### Installation

1. **Create Database and necessary extension**
   ```sql
   CREATE USER <user> WITH PASSWORD 'your-password'; -- or use an existing user
   CREATE DATABASE <db> WITH OWNER <user>;
   ```
2. **Clone the repository**:
   ```bash
   git clone https://github.com/madgeek-arc/resource-catalogue.git
   ```
3. **Create a file named `secret.properties` and populate it to resolve `application.properties` placeholders.
   You can view an example at [Secret Properties Example](#Secret-Properties-Example).**
4. **Build and Package**  
   To build the project and package the code into an executable .jar file with an embedded Tomcat server:
   1. _Navigate_ to the project directory
   2. _Execute_ the following Maven command
   ```bash
   mvn clean package
   ```

5. **Run**  
   ```bash
   java -jar resource-catalogue-service/target/resource-catalogue-service-X.X.X-SNAPSHOT.jar \
   --spring.config.additional-location=file:/path/to/secret.properties
   ```

---

## Testing
```bash
# Unit tests only 
mvn -P lot1 -DskipITs verify

# Integration tests only 
mvn -P lot1,it-tests -Dspringdoc.skip=true verify

# Full service-module run (unit + integration)
mvn -P lot1 verify
```
Test results will be displayed in the terminal.

---

## Documentation Links
For extensive and detailed documentation, please refer to
[Resource Catalogue Documentation](https://github.com/EOSC-Lot-1/een-resource-catalogue-docs).

---

## Project Structure
This repository is organized as a multi-module Maven project. Key modules and folders include:

- `resource-catalogue-service`: main Spring Boot service and executable JAR.
- `resource-catalogue-rest`: REST controllers and API wiring.
- `resource-catalogue-api`: API interfaces and shared DTOs.
- `resource-catalogue-model` / `resource-catalogue-model-lot1`: data model and generated artifacts.
- `resource-catalogue-elastic`: Elasticsearch index mappings and related utilities.
- `resource-catalogue-jms`: JMS/ActiveMQ integration code.
- `resource-catalogue-service/src/main/resources` and `config/`: application properties and runtime configuration.
- `docs/` and `docs/openapi.json`: generated API documentation and OpenAPI spec.

## Configuration
Configuration files are located under the `config/` directory and the service module's `src/main/resources`.
- Primary config: `config/application.properties`.
- Secret and environment-specific values: `config/secret.properties` (not committed; use your own copy).
- PID and runtime settings: `config/pid.yml`.

Runtime overrides: supply `--spring.config.additional-location=file:/path/to/secret.properties` when starting the JAR, or use environment variables to override specific Spring properties.

## API Reference
- The OpenAPI specification is available at `docs/openapi.json` and the `resource-catalogue-api` module contains API interfaces and DTOs.
- For interactive API docs, build and run the service and consult the embedded SpringDoc or the generated docs site if deployed.

## Packaging and running the application
Build and package the project with Maven:

```bash
mvn clean package
```

Run the service JAR (example):

```bash
java -jar resource-catalogue-service/target/resource-catalogue-service-X.X.X-SNAPSHOT.jar \
   --spring.config.additional-location=file:/path/to/secret.properties
```

---

## Restore database

1. Prepare a POST request for "resource-catalogue-url/restore"

2. Add as a body a zip `datafile` containing a folder for each of the resources to restore

---

## Versioning
This project adheres to [Semantic Versioning](https://semver.org/). For the available versions, see the 
[tags](https://github.com/madgeek-arc/resource-catalogue/tags).

---

## License
This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.


## Authors
- **Konstantinos Spyrou** - Development - [GitHub](https://github.com/spyroukostas)
- **Michael Zouros** - Development - [GitHub](https://github.com/mzouros)
- **Nikiforos Leonidakis** - Development - [GitHub](https://github.com/nleond)

See the [contributors list](https://github.com/madgeek-arc/resource-catalogue/graphs/contributors) 
for a full list of contributors.

---

## Acknowledgements

Special thanks to all contributors, testers and the open-source community for their invaluable support and resources.
