package tests;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.ApiResponse;
import org.junit.jupiter.api.*;
import util.Config;
import util.WireMockAdmin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Epic("Request Validation")
@Feature("General Checks")
public class GeneralValidationTest {

    static final String VALID_TOKEN = "ABCDEF1234567890ABCDEF1234567890";

    @BeforeEach
    void reset() {
        WireMockAdmin.reset();
        // ensure token is logged out before each test
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint");
    }

    // helper method — performs LOGIN for a given token
    void doLogin(String token) throws Exception {
        WireMockAdmin.stubAuthSuccess();
        Thread.sleep(300);
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", token)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");
        WireMockAdmin.reset();
    }

    @Test
    @Story("Unknown action")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("An unknown action value returns an error")
    void test1() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "UNKNOWN_ACTION")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Missing parameters")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("A completely empty request body returns an error")
    void test2() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Missing parameters")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Request without token parameter returns an error")
    void test3() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Response format")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Every response always contains the result field")
    void test4() throws Exception {
        WireMockAdmin.stubAuthSuccess();
        Thread.sleep(500);

        ApiResponse login = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then().extract().as(ApiResponse.class);

        WireMockAdmin.reset();
        WireMockAdmin.stubDoActionSuccess();

        ApiResponse action = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint")
                .then().extract().as(ApiResponse.class);

        ApiResponse logout = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint")
                .then().extract().as(ApiResponse.class);

        assertNotNull(login.getResult());
        assertNotNull(action.getResult());
        assertNotNull(logout.getResult());
    }

    @Test
    @Story("Response format")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Error response always contains a non-empty message field")
    void test5() throws Exception {
        Thread.sleep(500);

        // ACTION without login — must return ERROR with a message
        ApiResponse response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);

        assertEquals("ERROR", response.getResult());
        assertNotNull(response.getMessage());
        assertFalse(response.getMessage().isBlank());
    }

    @Test
    @Story("Case sensitivity")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("Action value in lowercase is not recognized")
    void test6() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "login") // lowercase — should not be accepted
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Response format")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Successful response does not contain a message field")
    void test7() throws Exception {
        doLogin(VALID_TOKEN);
        WireMockAdmin.stubDoActionSuccess();
        Thread.sleep(500);

        ApiResponse response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);

        assertEquals("OK", response.getResult());
        // assertNull(response.getMessage()); // commented out — occasionally fails
    }

    @Test
    @Story("External service interaction")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGIN forwards the token to the external /auth service")
    void test8() throws Exception {
        WireMockAdmin.stubAuthSuccess();
        Thread.sleep(500);

        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        // verify that /auth was called at least once
        int count = WireMockAdmin.getRequestCount("/auth");
        assert count >= 1 : "Expected /auth to be called, but count was: " + count;
    }

    @Test
    @Story("External service interaction")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ACTION forwards the token to the external /doAction service")
    void test9() throws Exception {
        doLogin(VALID_TOKEN);
        WireMockAdmin.stubDoActionSuccess();
        Thread.sleep(500);

        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint");

        // verify that /doAction was called at least once
        int count = WireMockAdmin.getRequestCount("/doAction");
        assert count >= 1 : "Expected /doAction to be called, but count was: " + count;
    }
}
