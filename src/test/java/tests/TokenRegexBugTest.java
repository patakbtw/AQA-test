package tests;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import model.ApiResponse;
import org.junit.jupiter.api.*;
import util.Config;
import util.TestSuite;
import util.WireMockAdmin;

import static util.TestConfig.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("Authentication")
@Feature("Token Validation")
public class TokenRegexBugTest extends TestSuite {

  @BeforeEach
  void reset() {
    RestAssured.reset();
    WireMockAdmin.reset();
    // no filter — cleanup only
    RestAssured.given()
        .contentType("application/x-www-form-urlencoded")
        .accept("application/json")
        .header("X-Api-Key", API_KEY)
        .formParam("token", SPEC_TOKEN)
        .formParam("action", "LOGOUT")
        .post(Config.appUrl() + "/endpoint");
  }

  @Test
  @Story("Token format per specification")
  @Severity(SeverityLevel.CRITICAL)
  @DisplayName("[BUG] Full LOGIN → ACTION → LOGOUT flow fails for token with chars G-Z")
  @Description("Per API specification, tokens may contain A-Z and 0-9 characters.\n\n" +
      "Bug discovered by decompiling UserRequest.class from the jar:\n" +
      "  Actual regex:   ^[0-9A-F]{32}$  (hex only)\n" +
      "  Expected regex: ^[A-Z0-9]{32}$  (per specification)\n\n" +
      "Example of rejected token: GHIJKLMNOPQRSTUVWXYZ012345678901\n" +
      "Example of accepted token: ABCDEF1234567890ABCDEF1234567890\n\n" +
      "This test will pass once the bug is fixed.")
  @Issue("1")
  @Link(name = "Bug Report", url = "https://github.com/patakbtw/AQA-test/issues/1")
  void testFullFlowWithSpecToken() {
    Allure.step("Stub external auth service to return success", WireMockAdmin::stubAuthSuccess);
    ApiResponse login = performLogin(SPEC_TOKEN);

    Allure.step("Reset stubs and stub external action service to return success", () -> {
      WireMockAdmin.reset();
      WireMockAdmin.stubDoActionSuccess();
    });
    ApiResponse action = performAction(SPEC_TOKEN);

    Allure.step("Reset stubs before LOGOUT", WireMockAdmin::reset);
    ApiResponse logout = performLogout(SPEC_TOKEN);

    // run all checks independently — all failures are reported even if LOGIN fails
    assertAll(
        () -> Allure.step("Check that LOGIN result is OK", () -> assertEquals("OK", login.getResult())),
        () -> Allure.step("Check that ACTION result is OK", () -> assertEquals("OK", action.getResult())),
        () -> Allure.step("Check that LOGOUT result is OK", () -> assertEquals("OK", logout.getResult())));
  }

  @Step("Send LOGIN request with token {token}")
  private ApiResponse performLogin(String token) {
    return RestAssured.given()
        .filter(new AllureRestAssured())
        .contentType("application/x-www-form-urlencoded")
        .accept("application/json")
        .header("X-Api-Key", API_KEY)
        .formParam("token", token)
        .formParam("action", "LOGIN")
        .post(Config.appUrl() + "/endpoint")
        .then()
        .extract().as(ApiResponse.class);
  }

  @Step("Send ACTION request with token {token}")
  private ApiResponse performAction(String token) {
    return RestAssured.given()
        .filter(new AllureRestAssured())
        .contentType("application/x-www-form-urlencoded")
        .accept("application/json")
        .header("X-Api-Key", API_KEY)
        .formParam("token", token)
        .formParam("action", "ACTION")
        .post(Config.appUrl() + "/endpoint")
        .then()
        .extract().as(ApiResponse.class);
  }

  @Step("Send LOGOUT request with token {token}")
  private ApiResponse performLogout(String token) {
    return RestAssured.given()
        .filter(new AllureRestAssured())
        .contentType("application/x-www-form-urlencoded")
        .accept("application/json")
        .header("X-Api-Key", API_KEY)
        .formParam("token", token)
        .formParam("action", "LOGOUT")
        .post(Config.appUrl() + "/endpoint")
        .then()
        .extract().as(ApiResponse.class);
  }
}