package tests;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.ApiResponse;
import org.junit.jupiter.api.*;
import util.Config;
import util.WireMockAdmin;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("Authentication")
@Feature("LOGIN")
public class LoginTest {

    static final String VALID_TOKEN = "ABCDEF1234567890ABCDEF1234567890";

    @BeforeEach
    void reset() {
        WireMockAdmin.reset();
        // clean up token state before each test
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint");
    }

    @Test
    @Story("Successful login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("LOGIN returns OK when external service confirms authentication")
    void test1() throws Exception {
        WireMockAdmin.stubAuthSuccess();
        Thread.sleep(500);

        ApiResponse response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);

        assertEquals("OK", response.getResult());
    }

    @Test
    @Story("External service unavailable")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("LOGIN returns ERROR when external service responds with an error")
    void test2() throws Exception {
        WireMockAdmin.stubAuthFailure();
        Thread.sleep(500);

        ApiResponse response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);

        assertEquals("ERROR", response.getResult());
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGIN rejects a token shorter than 32 characters")
    void test3() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", "ABC123")
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGIN rejects a token longer than 32 characters")
    void test4() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") // 33 characters
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGIN rejects a token containing invalid characters")
    void test5() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", "abcdefghijklmnopqrstuvwxyz123456")
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("LOGIN rejects an empty token")
    void test6() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", "")
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Missing parameters")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Request without action parameter returns an error")
    void test7() throws Exception {
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .post(Config.appUrl() + "/endpoint");

        // missing action should be rejected
        int code = response.statusCode();
        assert code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR");
    }
}
