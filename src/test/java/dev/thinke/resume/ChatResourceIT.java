package dev.thinke.resume;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.quarkus.test.junit.QuarkusTest;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ChatResourceIT {

    @Test
    void chatStreamsAnswerWhenOpenRouterKeyPresent() {
        Assumptions.assumeTrue(
                System.getenv("OPENROUTER_API_KEY") != null && !System.getenv("OPENROUTER_API_KEY").isBlank());

        String raw = given().contentType("application/json")
                .body(Map.of("question", "What JVM frameworks are mentioned?"))
                .when()
                .post("/api/chat")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        String text = Arrays.stream(raw.split("\\R"))
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).stripLeading())
                .collect(Collectors.joining());

        assertFalse(text.isBlank());
    }

    @Test
    void chatRejectsOversizedQuestion() {
        given().contentType("application/json")
                .body(Map.of("question", "x".repeat(501)))
                .when()
                .post("/api/chat")
                .then()
                .statusCode(400);
    }
}
