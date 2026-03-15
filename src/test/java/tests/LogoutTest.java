package tests;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.ApiResponse;
import org.junit.jupiter.api.*;
import util.Config;
import util.TestSuite;
import util.WireMockAdmin;

import static util.TestConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Session Management")
@Feature("LOGOUT")
public class LogoutTest extends TestSuite {

        @BeforeEach
        void reset() {
                RestAssured.filters(new AllureRestAssured());
                WireMockAdmin.reset();
        }

        @Step("Login with token {token}")
        void doLogin(String token) {
                WireMockAdmin.stubAuthSuccess();
                RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", token)
                                .formParam("action", "LOGIN")
                                .post(Config.appUrl() + "/endpoint");
                WireMockAdmin.reset();
        }

        @Step("Logout with token {token}")
        void doLogout(String token) {
                RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", token)
                                .formParam("action", "LOGOUT")
                                .post(Config.appUrl() + "/endpoint");
        }

        @Step("Send LOGOUT request with token {token}")
        ApiResponse performLogout(String token) {
                return RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", token)
                                .formParam("action", "LOGOUT")
                                .post(Config.appUrl() + "/endpoint")
                                .then()
                                .extract().as(ApiResponse.class);
        }

        @Step("Send ACTION request with token {token}")
        Response performAction(String token) {
                return RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", token)
                                .formParam("action", "ACTION")
                                .post(Config.appUrl() + "/endpoint");
        }

        @Step("Send LOGIN request with token {token}")
        ApiResponse performLogin(String token) {
                return RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", token)
                                .formParam("action", "LOGIN")
                                .post(Config.appUrl() + "/endpoint")
                                .then()
                                .extract().as(ApiResponse.class);
        }

        @Test
        @Story("Successful logout")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("LOGOUT successfully terminates an active session")
        @Description("Login with a valid token, then perform LOGOUT. Response must be OK.")
        void test1() {
                doLogin(HEX_TOKEN);

                ApiResponse response = performLogout(HEX_TOKEN);

                Allure.step("Check that LOGOUT result is OK", () -> assertEquals("OK", response.getResult()));
        }

        @Test
        @Story("Logout without login")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("LOGOUT for a token that never logged in does not crash the application")
        @Description("Attempt LOGOUT without prior LOGIN. Application must not crash — 200 or 403 are acceptable.")
        void test2() {
                doLogout(HEX_TOKEN); // ensure logged out first

                Response response = RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", HEX_TOKEN)
                                .formParam("action", "LOGOUT")
                                .post(Config.appUrl() + "/endpoint");

                int code = response.statusCode();
                Allure.step("Check that application does not crash — response is 200 or 403",
                                () -> assertTrue(code == 200 || code == 403));
        }

        @Test
        @Story("Session state after logout")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("After LOGOUT the token is removed — ACTION is no longer available")
        @Description("Login, logout, then attempt ACTION. The session must be invalidated and ACTION must be rejected.")
        void test3() {
                doLogin(HEX_TOKEN);
                doLogout(HEX_TOKEN);

                Response actionResponse = performAction(HEX_TOKEN);
                int code = actionResponse.statusCode();

                Allure.step("Check that ACTION is rejected after LOGOUT with 403 or ERROR",
                                () -> assertTrue(code == 403
                                                || actionResponse.as(ApiResponse.class).getResult().equals("ERROR")));
        }

        @Test
        @Story("Re-login after logout")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("A token can log in again after it has logged out")
        @Description("Login, logout, then login again. The second LOGIN must succeed.")
        void test4() {
                doLogin(HEX_TOKEN);
                doLogout(HEX_TOKEN);

                WireMockAdmin.stubAuthSuccess();
                ApiResponse response = performLogin(HEX_TOKEN);

                Allure.step("Check that second LOGIN result is OK", () -> assertEquals("OK", response.getResult()));

                doLogout(HEX_TOKEN);
        }

        @Test
        @Story("Double logout")
        @Severity(SeverityLevel.MINOR)
        @DisplayName("A second LOGOUT on the same token does not crash the application")
        @Description("Login, logout, then logout again. Application must handle repeated LOGOUT gracefully.")
        void test5() {
                doLogin(HEX_TOKEN);
                doLogout(HEX_TOKEN);

                Response second = RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", HEX_TOKEN)
                                .formParam("action", "LOGOUT")
                                .post(Config.appUrl() + "/endpoint");

                int code = second.statusCode();
                Allure.step("Check that second LOGOUT does not crash — response is 200 or 403",
                                () -> assertTrue(code == 200 || code == 403));
        }

        @Test
        @Story("Session isolation")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Logging out one token does not terminate another token's session")
        @Description("Login with token A and token B. Logout token A. Token B must still be able to perform ACTION.")
        void test6() {
                // log in both tokens
                WireMockAdmin.stubAuthSuccess();

                Allure.step("Login with token A", () -> RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", HEX_TOKEN)
                                .formParam("action", "LOGIN")
                                .post(Config.appUrl() + "/endpoint"));

                Allure.step("Login with token B", () -> RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", HEX_TOKEN_B)
                                .formParam("action", "LOGIN")
                                .post(Config.appUrl() + "/endpoint"));

                doLogout(HEX_TOKEN);

                WireMockAdmin.reset();
                WireMockAdmin.stubDoActionSuccess();

                ApiResponse response = RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", HEX_TOKEN_B)
                                .formParam("action", "ACTION")
                                .post(Config.appUrl() + "/endpoint")
                                .then()
                                .extract().as(ApiResponse.class);

                Allure.step("Check that token B session is still active after token A logout",
                                () -> assertEquals("OK", response.getResult()));

                doLogout(HEX_TOKEN_B);
        }
}