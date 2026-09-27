package org.springframework.samples.petclinic.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles({"h2", "spring-data-jpa"})
@TestPropertySource(properties = {
    "petclinic.security.enable=true"
})
class RestAssuredOwnerE2ETests {

    @LocalServerPort
    int port;


    @BeforeEach
    void setUp() {

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
    void shouldGetExistingOwner() {

        authenticatedRequest()
            .when()
            .get("/api/owners/1")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(1))
            .body("firstName", notNullValue())
            .body("lastName", notNullValue());
    }



    @Test
    void shouldReturn404ForMissingOwner() {

        authenticatedRequest()
            .when()
            .get("/api/owners/999999")
            .then()
            .statusCode(404);
    }



    @Test
    void shouldReturn401WithoutAuthentication() {

        given()
            .accept(ContentType.JSON)
            .when()
            .get("/api/owners/1")
            .then()
            .statusCode(401);
    }


    @Test
    void shouldListOwners() {

        authenticatedRequest()
            .when()
            .get("/api/owners")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", greaterThan(0));
    }



    @Test
    void shouldCreateOwner() {

        String newOwner = """
            {
                "firstName": "Test",
                "lastName": "Owner",
                "address": "Test Street 1",
                "city": "Skopje",
                "telephone": "1112223333"
            }
            """;

        authenticatedRequest()
            .contentType(ContentType.JSON)
            .body(newOwner)
            .when()
            .post("/api/owners")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("id", notNullValue())
            .body("firstName", equalTo("Test"))
            .body("lastName", equalTo("Owner"))
            .body("city", equalTo("Skopje"));
    }



    @Test
    void shouldReturn400ForInvalidOwner() {

        String invalidOwner = """
            {
                "firstName": "",
                "lastName": "Owner",
                "address": "Test Street 1",
                "city": "Skopje",
                "telephone": "1112223333"
            }
            """;

        authenticatedRequest()
            .contentType(ContentType.JSON)
            .body(invalidOwner)
            .when()
            .post("/api/owners")
            .then()
            .statusCode(400);
    }


    @Test
    void shouldCreateUpdateAndDeleteOwner() {


        String newOwner = """
            {
                "firstName": "Rest",
                "lastName": "Assured",
                "address": "123 Test Street",
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
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("firstName", equalTo("Rest"))
                .body("lastName", equalTo("Assured"))
                .body("city", equalTo("Skopje"))
                .extract()
                .path("id");



        authenticatedRequest()
            .when()
            .get("/api/owners/{ownerId}", ownerId)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(ownerId))
            .body("firstName", equalTo("Rest"))
            .body("lastName", equalTo("Assured"));



        String updatedOwner = """
            {
                "firstName": "Rest",
                "lastName": "Updated",
                "address": "456 Updated Street",
                "city": "Bitola",
                "telephone": "0987654321"
            }
            """;

        authenticatedRequest()
            .contentType(ContentType.JSON)
            .body(updatedOwner)
            .when()
            .put("/api/owners/{ownerId}", ownerId)
            .then()
            .statusCode(204);

        authenticatedRequest()
            .when()
            .get("/api/owners/{ownerId}", ownerId)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(ownerId))
            .body("firstName", equalTo("Rest"))
            .body("lastName", equalTo("Updated"))
            .body("city", equalTo("Bitola"));



        authenticatedRequest()
            .when()
            .delete("/api/owners/{ownerId}", ownerId)
            .then()
            .statusCode(204);


        authenticatedRequest()
            .when()
            .get("/api/owners/{ownerId}", ownerId)
            .then()
            .statusCode(404);
    }
}
