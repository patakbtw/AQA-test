package tests;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import util.Config;
import util.WireMockAdmin;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Security")
@Feature("API Key Validation")
public class ApiKeyTest {

    static final String VALID_TOKEN = "ABCDEF1234567890ABCDEF1234567890";

    @BeforeEach
    void reset() {
        WireMockAdmin.reset();
    }

    @Test
    @Story("Missing key")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Request without X-Api-Key header is rejected with 401 or 403")
    void test1() throws Exception {
        Thread.sleep(500);

        // no X-Api-Key header
        int code = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().statusCode();

        assertTrue(code == 401 || code == 403);
    }

    @Test
    @Story("Wrong key")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Request with an incorrect API key is rejected")
    void test2() throws Exception {
        Thread.sleep(500);

        int code = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "wrongkey")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().statusCode();

        assertTrue(code == 401 || code == 403);
    }

    @Test
    @Story("Empty key")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Request with an empty API key is rejected")
    void test3() throws Exception {
        Thread.sleep(500);

        int code = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().statusCode();

        assertTrue(code == 401 || code == 403);
    }

    @Test
    @Story("Correct key")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Request with the correct API key is accepted and processed")
    void test4() throws Exception {
        WireMockAdmin.stubAuthSuccess();
        Thread.sleep(500);

        int code = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().statusCode();

        // valid key must not result in 401 or 403
        assertTrue(code != 401 && code != 403,
                "Valid API key should not be rejected, got: " + code);

        // clean up
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint");
    }

    @Test
    @Story("Case sensitivity")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("API key check is case-sensitive — wrong casing is rejected")
    void test5() throws Exception {
        Thread.sleep(500);

        // correct key is "qazWSXedc" — all uppercase should not work
        int code = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "QAZWSXEDC")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().statusCode();

        assertTrue(code == 401 || code == 403);
    }
}
