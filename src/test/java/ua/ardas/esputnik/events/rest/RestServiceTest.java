package ua.ardas.esputnik.events.rest;

import org.junit.Test;
import ua.ardas.esputnik.events.BaseServiceTest;

import static com.jayway.restassured.RestAssured.given;

public class RestServiceTest extends BaseServiceTest {


    @Test
    public void testHealthCheck() {
        given()
            .when().get("/monitoring/healthcheck")
            .then()
            .log().all()
            .statusCode(200);
    }
}
