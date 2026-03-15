package tests;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import util.Config;
import util.TestSuite;
import util.WireMockAdmin;

import static util.TestConfig.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Security")
@Feature("API Key Validation")
public class ApiKeyTest extends TestSuite {

    @BeforeEach
    void reset() {
        RestAssured.filters(new AllureRestAssured());
        WireMockAdmin.reset();
    }

    @Step("Send LOGIN request without API key")
    int sendRequestWithoutApiKey() {
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .formParam("token", HEX_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().statusCode();
    }

    @Step("Send LOGIN request with API key '{apiKey}'")
    int sendRequestWithApiKey(String apiKey) {
        return RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", apiKey)
                .formParam("token", HEX_TOKEN)
                .formParam("action", "LOGIN")
                .post(Config.appUrl() + "/endpoint")
                .then()
                .extract().statusCode();
    }

    @Test
    @Story("Missing key")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Request without X-Api-Key header is rejected with 401 or 403")
    @Description("Send a request with no X-Api-Key header at all. Application must reject it before processing.")
    void test1() {
        int code = sendRequestWithoutApiKey();

        Allure.step("Check that response code is 401 or 403", () -> assertTrue(code == 401 || code == 403));
    }

    @Test
    @Story("Wrong key")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Request with an incorrect API key is rejected")
    @Description("Send a request with a completely wrong API key. Application must reject it.")
    void test2() {
        int code = sendRequestWithApiKey("wrongkey");

        Allure.step("Check that response code is 401 or 403", () -> assertTrue(code == 401 || code == 403));
    }

    @Test
    @Story("Empty key")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Request with an empty API key is rejected")
    @Description("Send a request with X-Api-Key header present but empty. Application must reject it.")
    void test3() {
        int code = sendRequestWithApiKey("");

        Allure.step("Check that response code is 401 or 403", () -> assertTrue(code == 401 || code == 403));
    }

    @Test
    @Story("Correct key")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Request with the correct API key is accepted and processed")
    @Description("Send a request with the correct API key. Application must not reject it with 401 or 403.")
    void test4() {
        WireMockAdmin.stubAuthSuccess();

        int code = sendRequestWithApiKey(API_KEY);

        Allure.step("Check that valid API key is not rejected with 401 or 403",
                () -> assertTrue(code != 401 && code != 403,
                        "Valid API key should not be rejected, got: " + code));

        Allure.step("Cleanup - logout token after test", () -> RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .header("X-Api-Key", API_KEY)
                .formParam("token", HEX_TOKEN)
                .formParam("action", "LOGOUT")
                .post(Config.appUrl() + "/endpoint"));
    }

    @Test
    @Story("Case sensitivity")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("API key check is case-sensitive - wrong casing is rejected")
    @Description("Correct key is 'qazWSXedc'. Send the same key in all uppercase - must be rejected.")
    void test5() {
        int code = sendRequestWithApiKey("QAZWSXEDC");

        Allure.step("Check that uppercase API key is rejected with 401 or 403",
                () -> assertTrue(code == 401 || code == 403));
    }
}