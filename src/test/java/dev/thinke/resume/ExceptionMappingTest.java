package dev.thinke.resume;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ExceptionMappingTest {

    @Test
    void unhandledExceptionReturnsSafeBodyWithoutStackTrace() {
        String body = given()
                .when()
                .get("/api/test/throw")
                .then()
                .statusCode(500)
                .extract()
                .asString();

        assertTrue(body.contains("An unexpected error occurred"));
        assertFalseContains(body, "Exception");
        assertFalseContains(body, "StackTrace");
        assertFalseContains(body, "dev.thinke");
        assertFalseContains(body, "Deliberate");
    }

    private static void assertFalseContains(String body, String value) {
        org.junit.jupiter.api.Assertions.assertFalse(
                body.contains(value),
                "Response body should not contain '%s' but was: %s".formatted(value, body));
    }
}
