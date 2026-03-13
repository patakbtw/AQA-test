package tests;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.ApiResponse;
import org.junit.jupiter.api.*;
import util.Config;
import util.WireMockAdmin;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("Session Management")
@Feature("LOGOUT")
public class LogoutTest {

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
        WireMockAdmin.reset();
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
    @Story("Successful logout")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("LOGOUT successfully terminates an active session")
    void test1() throws Exception {
        doLogin(VALID_TOKEN);
        Thread.sleep(500);

        ApiResponse response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);

        assertEquals("OK", response.getResult());
    }

    @Test
    @Story("Logout without login")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGOUT for a token that never logged in does not crash the application")
    void test2() throws Exception {
        doLogout(VALID_TOKEN); // ensure logged out first
        Thread.sleep(500);

        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint");

        // application should not crash — 200 with ERROR body or 403 are both acceptable
        int code = response.statusCode();
        assert code == 200 || code == 403;
    }

    @Test
    @Story("Session state after logout")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("After LOGOUT the token is removed — ACTION is no longer available")
    void test3() throws Exception {
        doLogin(VALID_TOKEN);
        doLogout(VALID_TOKEN);
        Thread.sleep(500);

        // attempt ACTION after logout — must be rejected
        Response actionResponse = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint");

        int code = actionResponse.statusCode();
        assert code == 403 || actionResponse.as(ApiResponse.class).getResult().equals("ERROR");
    }

    @Test
    @Story("Re-login after logout")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("A token can log in again after it has logged out")
    void test4() throws Exception {
        doLogin(VALID_TOKEN);
        doLogout(VALID_TOKEN);

        // second login after logout
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
        doLogout(VALID_TOKEN);
    }

    @Test
    @Story("Double logout")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("A second LOGOUT on the same token does not crash the application")
    void test5() throws Exception {
        doLogin(VALID_TOKEN);
        doLogout(VALID_TOKEN);
        Thread.sleep(500);

        // second logout of the same (now non-existent) session
        Response second = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint");

        int code = second.statusCode();
        assert code == 200 || code == 403;
    }

    @Test
    @Story("Session isolation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Logging out one token does not terminate another token's session")
    void test6() throws Exception {
        // log in both tokens
        WireMockAdmin.stubAuthSuccess();
        Thread.sleep(500);
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN_B)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");

        // log out only token A
        doLogout(VALID_TOKEN);

        WireMockAdmin.reset();
        WireMockAdmin.stubDoActionSuccess();
        Thread.sleep(500);

        // token B session must still be active
        ApiResponse response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", "qazWSXedc")
                .formParam("token", VALID_TOKEN_B)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);

        assertEquals("OK", response.getResult());
        doLogout(VALID_TOKEN_B);
    }
}
