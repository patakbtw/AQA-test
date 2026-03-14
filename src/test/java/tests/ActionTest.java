package tests;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.ApiResponse;
import org.junit.jupiter.api.*;
import util.Config;
import util.WireMockAdmin;

import static util.TestConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("User Actions")
@Feature("ACTION")
public class ActionTest {

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

    @Step("Send ACTION request with token {token}")
    ApiResponse performAction(String token) {
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", API_KEY)
                .formParam("token", token)
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().as(ApiResponse.class);
    }

    @Test
    @Story("Successful action")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("ACTION succeeds for a logged-in token when external service responds with success")
    @Description("Login with a valid token, then perform ACTION. External service is stubbed to return 200.")
    void test1() {
        doLogin(HEX_TOKEN);
        WireMockAdmin.reset();
        WireMockAdmin.stubDoActionSuccess();

        ApiResponse response = performAction(HEX_TOKEN);

        Allure.step("Check that ACTION result is OK", () -> assertEquals("OK", response.getResult()));

        doLogout(HEX_TOKEN);
    }

    @Test
    @Story("Access without authentication")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ACTION is rejected for a token that has not logged in")
    @Description("Attempt ACTION without prior LOGIN. Application must reject the request.")
    void test2() {
        doLogout(HEX_TOKEN); // ensure clean state

        ApiResponse response = performAction(HEX_TOKEN);

        Allure.step("Check that ACTION result is ERROR", () -> assertEquals("ERROR", response.getResult()));
    }

    @Test
    @Story("Access after session ends")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ACTION is rejected after LOGOUT")
    @Description("Login, then logout, then attempt ACTION. Session must no longer be valid.")
    void test3() {
        doLogin(HEX_TOKEN);
        doLogout(HEX_TOKEN);

        ApiResponse response = performAction(HEX_TOKEN);

        Allure.step("Check that ACTION result is ERROR after logout",
                () -> assertEquals("ERROR", response.getResult()));
    }

    @Test
    @Story("External service unavailable")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ACTION returns ERROR when external service responds with an error")
    @Description("Login succeeds, but the external action service is stubbed to return 500.")
    void test4() {
        doLogin(HEX_TOKEN);
        WireMockAdmin.reset();
        WireMockAdmin.stubDoActionFailure();

        ApiResponse response = performAction(HEX_TOKEN);

        Allure.step("Check that ACTION result is ERROR when external service fails",
                () -> assertEquals("ERROR", response.getResult()));

        doLogout(HEX_TOKEN);
    }

    @Test
    @Story("Repeated actions")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("A logged-in token can perform ACTION multiple times in a row")
    @Description("Login once, then perform ACTION three times in a row. Each call must succeed.")
    void test5() {
        doLogin(HEX_TOKEN);
        WireMockAdmin.reset();
        WireMockAdmin.stubDoActionSuccess();

        for (int i = 0; i < 3; i++) {
            int attempt = i + 1;
            ApiResponse response = performAction(HEX_TOKEN);
            Allure.step("Check that ACTION attempt " + attempt + " result is OK",
                    () -> assertEquals("OK", response.getResult()));
        }

        doLogout(HEX_TOKEN);
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("ACTION rejects a malformed token")
    @Description("Send ACTION with a token that does not match the required format. Must be rejected with 400 or ERROR.")
    void test6() {
        Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", API_KEY)
                .formParam("token", "ABC123")
                .formParam("action", "ACTION")
                .post(Config.appUrl() + "/endpoint");

        int code = response.statusCode();
        Allure.step("Check that malformed token is rejected with 400 or ERROR",
                () -> assertEquals(true, code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
    }

    @Test
    @Story("Session isolation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Authenticating one token does not grant access to another")
    @Description("Login with token A, then attempt ACTION with token B. Token B must be rejected.")
    void test7() {
        doLogout(HEX_TOKEN_B); // ensure B is logged out
        doLogin(HEX_TOKEN);

        ApiResponse response = performAction(HEX_TOKEN_B);

        Allure.step("Check that token B is rejected when only token A is logged in",
                () -> assertEquals("ERROR", response.getResult()));

        doLogout(HEX_TOKEN);
    }
}