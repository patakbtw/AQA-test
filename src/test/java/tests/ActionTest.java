package tests;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.ApiResponse;
import org.junit.jupiter.api.*;
import util.Config;
import util.WireMockAdmin;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("User Actions")
@Feature("ACTION")
public class ActionTest {

    static final String VALID_TOKEN   = "ABCDEF1234567890ABCDEF1234567890";
    static final String VALID_TOKEN_B = "FEDCBA0987654321FEDCBA0987654321";

    @BeforeEach
    void reset() {
        WireMockAdmin.reset();
    }

    // helper method — performs LOGIN for a given token
    void doLogin(String token) throws Exception {
        WireMockAdmin.stubAuthSuccess();
        Thread.sleep(500);
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", token)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");
    }

    void doLogout(String token) {
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", token)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint");
    }

    @Test
    @Story("Successful action")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("ACTION succeeds for a logged-in token when external service responds with success")
    void test1() throws Exception {
        doLogin(VALID_TOKEN);
        WireMockAdmin.reset();
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

        System.out.println("result: " + response.getResult());
        assertEquals("OK", response.getResult());
        doLogout(VALID_TOKEN);
    }

    @Test
    @Story("Access without authentication")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ACTION is rejected for a token that has not logged in")
    void test2() throws Exception {
        doLogout(VALID_TOKEN); // ensure clean state
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

        assertEquals("ERROR", response.getResult());
    }

    @Test
    @Story("Access after session ends")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ACTION is rejected after LOGOUT")
    void test3() throws Exception {
        doLogin(VALID_TOKEN);
        doLogout(VALID_TOKEN);
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

        assertEquals("ERROR", response.getResult());
    }

    @Test
    @Story("External service unavailable")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ACTION returns ERROR when external service responds with an error")
    void test4() throws Exception {
        doLogin(VALID_TOKEN);
        WireMockAdmin.reset();
        WireMockAdmin.stubDoActionFailure();
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

        assertEquals("ERROR", response.getResult());
        doLogout(VALID_TOKEN);
    }

    @Test
    @Story("Repeated actions")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("A logged-in token can perform ACTION multiple times in a row")
    void test5() throws Exception {
        doLogin(VALID_TOKEN);
        WireMockAdmin.reset();
        WireMockAdmin.stubDoActionSuccess();

        // call ACTION three times in a row
        for (int i = 0; i < 3; i++) {
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
        }
        doLogout(VALID_TOKEN);
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ACTION rejects a malformed token")
    void test6() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", "ABC123")
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Session isolation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Authenticating one token does not grant access to another")
    void test7() throws Exception {
        doLogout(VALID_TOKEN_B); // ensure B is logged out
        doLogin(VALID_TOKEN);
        Thread.sleep(500);

        ApiResponse response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN_B)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);

        assertEquals("ERROR", response.getResult());
        doLogout(VALID_TOKEN);
    }
}
