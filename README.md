# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**




## Design choices
### Validation

Validated incoming body as per the request spec with @RequestBody and Validate with custom validators.
Added a global exception handler for request body validation.

### Client integration
Added a client for integrating with the acquiring bank.
Added request ids for request tracking.


### Error handling
Added a generic global exception hanlder on unmanaged errors.

Integration tests need docker container running in background to pass. Did not have enough time to get testcontainers to work with the project.
