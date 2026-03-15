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

@Epic("Authentication")
@Feature("LOGIN")
public class LoginTest extends TestSuite {

    @BeforeEach
    void reset() {
        RestAssured.filters(new AllureRestAssured());
        WireMockAdmin.reset();
        // clean up token state before each test
        RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", API_KEY)
                .formParam("token", HEX_TOKEN)
                .formParam("action", "LOGOUT")
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

    @Step("Send LOGIN request with token {token} - expect rejection")
    Response performLoginRaw(String token) {
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", API_KEY)
                .formParam("token", token)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint");
    }

    @Step("Send LOGIN request without action parameter")
    Response performLoginWithoutAction() {
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", API_KEY)
                .formParam("token", HEX_TOKEN)
                .post(Config.appUrl() + "/endpoint");
    }

    @Test
    @Story("Successful login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("LOGIN returns OK when external service confirms authentication")
    @Description("Stub external auth service to return 200. Send LOGIN request. Response must be OK.")
    void shouldReturnOkWhenExternalServiceConfirms() {
        WireMockAdmin.stubAuthSuccess();

        ApiResponse response = performLogin(HEX_TOKEN);

        Allure.step("Check that LOGIN result is OK", () -> assertEquals("OK", response.getResult()));
    }

    @Test
    @Story("External service unavailable")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("LOGIN returns ERROR when external service responds with an error")
    @Description("Stub external auth service to return 500. Send LOGIN request. Response must be ERROR.")
    void shouldReturnErrorWhenExternalServiceFails() {
        WireMockAdmin.stubAuthFailure();

        ApiResponse response = performLogin(HEX_TOKEN);

        Allure.step("Check that LOGIN result is ERROR when external service fails",
                () -> assertEquals("ERROR", response.getResult()));
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGIN rejects a token shorter than 32 characters")
    @Description("Token 'ABC123' is only 6 characters. Must be rejected with 400 or ERROR.")
    void shouldRejectShortToken() {
        Response response = performLoginRaw("ABC123");
        int code = response.statusCode();

        Allure.step("Check that short token is rejected with 400 or ERROR",
                () -> assertTrue(code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGIN rejects a token longer than 32 characters")
    @Description("Token with 33 characters exceeds the required length of 32. Must be rejected.")
    void shouldRejectLongToken() {
        Response response = performLoginRaw("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"); // 33 characters
        int code = response.statusCode();

        Allure.step("Check that long token is rejected with 400 or ERROR",
                () -> assertTrue(code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("LOGIN rejects a token containing invalid characters")
    @Description("Token with lowercase letters does not match the required pattern. Must be rejected.")
    void shouldRejectTokenWithInvalidChars() {
        Response response = performLoginRaw("abcdefghijklmnopqrstuvwxyz123456");
        int code = response.statusCode();

        Allure.step("Check that token with invalid characters is rejected with 400 or ERROR",
                () -> assertTrue(code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
    }

    @Test
    @Story("Invalid token")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("LOGIN rejects an empty token")
    @Description("Send LOGIN with an empty token value. Must be rejected with 400 or ERROR.")
    void shouldRejectEmptyToken() {
        Response response = performLoginRaw("");
        int code = response.statusCode();

        Allure.step("Check that empty token is rejected with 400 or ERROR",
                () -> assertTrue(code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
    }

    @Test
    @Story("Missing parameters")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Request without action parameter returns an error")
    @Description("Send a request with token but without the action parameter. Must be rejected.")
    void shouldRejectRequestWithoutAction() {
        Response response = performLoginWithoutAction();
        int code = response.statusCode();

        Allure.step("Check that request without action is rejected with 400 or ERROR",
                () -> assertTrue(code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
    }
}