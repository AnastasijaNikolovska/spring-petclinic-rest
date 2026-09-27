package org.springframework.samples.petclinic.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("spring-data-jpa")
@Testcontainers
@TestPropertySource(properties = {
    "petclinic.security.enable=true",

    "spring.sql.init.mode=always",

    "spring.sql.init.schema-locations=classpath:db/postgres/schema.sql",

    "spring.sql.init.data-locations=classpath:db/postgres/data.sql"
})
class RestAssuredPostgresE2ETests {


    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16.3")
            .withDatabaseName("petclinic")
            .withUsername("petclinic")
            .withPassword("petclinic");


    @DynamicPropertySource
    static void configurePostgres(
        DynamicPropertyRegistry registry
    ) {

        registry.add(
            "spring.datasource.url",
            postgres::getJdbcUrl
        );

        registry.add(
            "spring.datasource.username",
            postgres::getUsername
        );

        registry.add(
            "spring.datasource.password",
            postgres::getPassword
        );

        registry.add(
            "spring.datasource.driver-class-name",
            postgres::getDriverClassName
        );
    }

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {

        RestAssured.baseURI = "http://localhost";

        RestAssured.port = port;

        RestAssured.basePath = "/petclinic";
    }


    private RequestSpecification authenticatedRequest() {

        return given()
            .auth()
            .preemptive()
            .basic("admin", "admin")
            .accept(ContentType.JSON);
    }


    @Test
    void shouldCreateUpdateAndDeleteOwnerUsingPostgres() {


        String newOwner = """
            {
                "firstName": "Postgres",
                "lastName": "Owner",
                "address": "123 Integration Street",
                "city": "Skopje",
                "telephone": "1234567890"
            }
            """;


        Integer ownerId =
            authenticatedRequest()
                .contentType(ContentType.JSON)
                .body(newOwner)

                .when()
                .post("/api/owners")

                .then()

                .log()
                .ifValidationFails()

                .statusCode(201)
                .contentType(ContentType.JSON)

                .body(
                    "id",
                    notNullValue()
                )

                .body(
                    "firstName",
                    equalTo("Postgres")
                )

                .body(
                    "lastName",
                    equalTo("Owner")
                )

                .body(
                    "city",
                    equalTo("Skopje")
                )

                .extract()
                .path("id");


        authenticatedRequest()

            .when()
            .get(
                "/api/owners/{ownerId}",
                ownerId
            )

            .then()
            .log()
            .ifValidationFails()

            .statusCode(200)
            .contentType(ContentType.JSON)

            .body(
                "id",
                equalTo(ownerId)
            )

            .body(
                "firstName",
                equalTo("Postgres")
            )

            .body(
                "lastName",
                equalTo("Owner")
            )

            .body(
                "address",
                equalTo("123 Integration Street")
            )

            .body(
                "city",
                equalTo("Skopje")
            )

            .body(
                "telephone",
                equalTo("1234567890")
            );


        String updatedOwner = """
            {
                "firstName": "Postgres",
                "lastName": "Updated",
                "address": "456 Integration Avenue",
                "city": "Bitola",
                "telephone": "0987654321"
            }
            """;


        authenticatedRequest()
            .contentType(ContentType.JSON)
            .body(updatedOwner)

            .when()
            .put(
                "/api/owners/{ownerId}",
                ownerId
            )

            .then()
            .log()
            .ifValidationFails()

            .statusCode(204);


        authenticatedRequest()

            .when()
            .get(
                "/api/owners/{ownerId}",
                ownerId
            )

            .then()
            .log()
            .ifValidationFails()

            .statusCode(200)
            .contentType(ContentType.JSON)

            .body(
                "id",
                equalTo(ownerId)
            )

            .body(
                "firstName",
                equalTo("Postgres")
            )

            .body(
                "lastName",
                equalTo("Updated")
            )

            .body(
                "address",
                equalTo("456 Integration Avenue")
            )

            .body(
                "city",
                equalTo("Bitola")
            )

            .body(
                "telephone",
                equalTo("0987654321")
            );


        authenticatedRequest()

            .when()
            .delete(
                "/api/owners/{ownerId}",
                ownerId
            )

            .then()
            .log()
            .ifValidationFails()

            .statusCode(204);


        authenticatedRequest()

            .when()
            .get(
                "/api/owners/{ownerId}",
                ownerId
            )

            .then()
            .log()
            .ifValidationFails()

            .statusCode(404);
    }
}
