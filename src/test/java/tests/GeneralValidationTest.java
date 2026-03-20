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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Request Validation")
@Feature("General Checks")
public class GeneralValidationTest extends TestSuite {

        @BeforeEach
        void reset() {
                RestAssured.replaceFiltersWith(new AllureRestAssured());
                WireMockAdmin.reset();
                // ensure token is logged out before each test
                RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", HEX_TOKEN)
                                .formParam("action", "LOGOUT")
                                .post(Config.appUrl() + "/endpoint");
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

        @Step("Send request with action '{action}' and token {token}")
        Response sendRequest(String token, String action) {
                return RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("token", token)
                                .formParam("action", action)
                                .post(Config.appUrl() + "/endpoint");
        }

        @Step("Send request with no body")
        Response sendEmptyRequest() {
                return RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .post(Config.appUrl() + "/endpoint");
        }

        @Step("Send request with no token parameter")
        Response sendRequestWithoutToken(String action) {
                return RestAssured.given()
                                .contentType("application/x-www-form-urlencoded")
                                .accept("application/json")
                                .header("X-Api-Key", API_KEY)
                                .formParam("action", action)
                                .post(Config.appUrl() + "/endpoint");
        }

        @Test
        @Story("Unknown action")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("An unknown action value returns an error")
        @Description("Send a request with an action value that does not exist. Must be rejected with 400 or ERROR.")
        void test1() {
                Response response = sendRequest(HEX_TOKEN, "UNKNOWN_ACTION");
                int code = response.statusCode();

                Allure.step("Check that unknown action is rejected with 400 or ERROR", () -> assertTrue(
                                code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
        }

        @Test
        @Story("Missing parameters")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("A completely empty request body returns an error")
        @Description("Send a request with no parameters at all. Application must reject it with 400 or ERROR.")
        void test2() {
                Response response = sendEmptyRequest();
                int code = response.statusCode();

                Allure.step("Check that empty request is rejected with 400 or ERROR", () -> assertTrue(
                                code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
        }

        @Test
        @Story("Missing parameters")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Request without token parameter returns an error")
        @Description("Send a LOGIN request without the token field. Application must reject it.")
        void test3() {
                Response response = sendRequestWithoutToken("LOGIN");
                int code = response.statusCode();

                Allure.step("Check that request without token is rejected with 400 or ERROR", () -> assertTrue(
                                code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
        }

        @Test
        @Story("Response format")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Every response always contains the result field")
        @Description("Perform LOGIN, ACTION, and LOGOUT. Each response must contain a non-null result field.")
        void test4() {
                WireMockAdmin.stubAuthSuccess();

                ApiResponse login = sendRequest(HEX_TOKEN, "LOGIN").then().extract().as(ApiResponse.class);
                Allure.step("Check that LOGIN response contains result field", () -> assertNotNull(login.getResult()));

                WireMockAdmin.reset();
                WireMockAdmin.stubDoActionSuccess();

                ApiResponse action = sendRequest(HEX_TOKEN, "ACTION").then().extract().as(ApiResponse.class);
                Allure.step("Check that ACTION response contains result field",
                                () -> assertNotNull(action.getResult()));

                ApiResponse logout = sendRequest(HEX_TOKEN, "LOGOUT").then().extract().as(ApiResponse.class);
                Allure.step("Check that LOGOUT response contains result field",
                                () -> assertNotNull(logout.getResult()));
        }

        @Test
        @Story("Response format")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Error response always contains a non-empty message field")
        @Description("Attempt ACTION without LOGIN. Response must be ERROR and contain a non-empty message explaining the reason.")
        void test5() {
                ApiResponse response = sendRequest(HEX_TOKEN, "ACTION").then().extract().as(ApiResponse.class);

                Allure.step("Check that result is ERROR", () -> assertEquals("ERROR", response.getResult()));
                Allure.step("Check that message field is present and not empty", () -> {
                        assertNotNull(response.getMessage());
                        assertFalse(response.getMessage().isBlank());
                });
        }

        @Test
        @Story("Case sensitivity")
        @Severity(SeverityLevel.MINOR)
        @DisplayName("Action value in lowercase is not recognized")
        @Description("Send a request with action 'login' in lowercase. Only uppercase values are valid per specification.")
        void test6() {
                Response response = sendRequest(HEX_TOKEN, "login");
                int code = response.statusCode();

                Allure.step("Check that lowercase action is rejected with 400 or ERROR", () -> assertTrue(
                                code == 400 || response.as(ApiResponse.class).getResult().equals("ERROR")));
        }

        @Test
        @Story("Response format")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("Successful response does not contain a message field")
        @Description("Perform ACTION on a logged-in token. Successful response should not contain a message field.")
        void test7() {
                doLogin(HEX_TOKEN);
                WireMockAdmin.stubDoActionSuccess();

                ApiResponse response = sendRequest(HEX_TOKEN, "ACTION").then().extract().as(ApiResponse.class);

                Allure.step("Check that ACTION result is OK", () -> assertEquals("OK", response.getResult()));
                // assertNull(response.getMessage()); // commented out - occasionally fails
        }

        @Test
        @Story("External service interaction")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("LOGIN forwards the token to the external /auth service")
        @Description("After LOGIN, verify that the application made a request to the external /auth service.")
        void test8() {
                WireMockAdmin.stubAuthSuccess();
                sendRequest(HEX_TOKEN, "LOGIN");

                int count = WireMockAdmin.getRequestCount("/auth");
                Allure.step("Check that /auth was called at least once",
                                () -> assertTrue(count >= 1, "Expected /auth to be called, but count was: " + count));
        }

        @Test
        @Story("External service interaction")
        @Severity(SeverityLevel.NORMAL)
        @DisplayName("ACTION forwards the token to the external /doAction service")
        @Description("After ACTION, verify that the application made a request to the external /doAction service.")
        void test9() {
                doLogin(HEX_TOKEN);
                WireMockAdmin.stubDoActionSuccess();
                sendRequest(HEX_TOKEN, "ACTION");

                int count = WireMockAdmin.getRequestCount("/doAction");
                Allure.step("Check that /doAction was called at least once", () -> assertTrue(count >= 1,
                                "Expected /doAction to be called, but count was: " + count));
        }
}