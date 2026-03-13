package util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * Manages WireMock stubs.
 * - In Docker: talks to the standalone WireMock container via Admin API
 * - Locally: starts an embedded WireMock server automatically
 */
public class WireMockAdmin {

    private static final String WIREMOCK_URL = Config.wireMockUrl();
    private static WireMockServer embeddedServer;
    private static boolean useEmbedded = false;

    static {
        // check if standalone WireMock is reachable; if not, start embedded
        try {
            Response response = RestAssured.given()
                    .get(WIREMOCK_URL + "/__admin/health");
            if (response.statusCode() != 200) {
                startEmbedded();
            }
        } catch (Exception e) {
            startEmbedded();
        }
    }

    private static void startEmbedded() {
        useEmbedded = true;
        embeddedServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8888));
        embeddedServer.start();
        System.out.println("[WireMockAdmin] Standalone WireMock not found — started embedded on port 8888");

        // shut down cleanly when JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (embeddedServer != null && embeddedServer.isRunning()) {
                embeddedServer.stop();
            }
        }));
    }

    public static void reset() {
        if (useEmbedded) {
            embeddedServer.resetAll();
        } else {
            RestAssured.given().post(WIREMOCK_URL + "/__admin/reset");
        }
    }

    public static void stubAuthSuccess() {
        if (useEmbedded) {
            embeddedServer.stubFor(
                    com.github.tomakehurst.wiremock.client.WireMock
                            .post(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/auth"))
                            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                    .withStatus(200).withBody("ok")));
        } else {
            RestAssured.given()
                    .contentType("application/json")
                    .body("{\"request\":{\"method\":\"POST\",\"url\":\"/auth\"}," +
                            "\"response\":{\"status\":200,\"body\":\"ok\"}}")
                    .post(WIREMOCK_URL + "/__admin/mappings");
        }
    }

    public static void stubAuthFailure() {
        if (useEmbedded) {
            embeddedServer.stubFor(
                    com.github.tomakehurst.wiremock.client.WireMock
                            .post(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/auth"))
                            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                    .withStatus(500).withBody("error")));
        } else {
            RestAssured.given()
                    .contentType("application/json")
                    .body("{\"request\":{\"method\":\"POST\",\"url\":\"/auth\"}," +
                            "\"response\":{\"status\":500,\"body\":\"error\"}}")
                    .post(WIREMOCK_URL + "/__admin/mappings");
        }
    }

    public static void stubDoActionSuccess() {
        if (useEmbedded) {
            embeddedServer.stubFor(
                    com.github.tomakehurst.wiremock.client.WireMock
                            .post(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/doAction"))
                            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                    .withStatus(200).withBody("ok")));
        } else {
            RestAssured.given()
                    .contentType("application/json")
                    .body("{\"request\":{\"method\":\"POST\",\"url\":\"/doAction\"}," +
                            "\"response\":{\"status\":200,\"body\":\"ok\"}}")
                    .post(WIREMOCK_URL + "/__admin/mappings");
        }
    }

    public static void stubDoActionFailure() {
        if (useEmbedded) {
            embeddedServer.stubFor(
                    com.github.tomakehurst.wiremock.client.WireMock
                            .post(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/doAction"))
                            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                    .withStatus(500).withBody("error")));
        } else {
            RestAssured.given()
                    .contentType("application/json")
                    .body("{\"request\":{\"method\":\"POST\",\"url\":\"/doAction\"}," +
                            "\"response\":{\"status\":500,\"body\":\"error\"}}")
                    .post(WIREMOCK_URL + "/__admin/mappings");
        }
    }

    public static int getRequestCount(String url) {
        if (useEmbedded) {
            return embeddedServer.findAll(
                    com.github.tomakehurst.wiremock.client.WireMock
                            .postRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo(url)))
                    .size();
        } else {
            String body = "{\"method\":\"POST\",\"url\":\"" + url + "\"}";
            return RestAssured.given()
                    .contentType("application/json")
                    .body(body)
                    .post(WIREMOCK_URL + "/__admin/requests/count")
                    .jsonPath()
                    .getInt("count");
        }
    }
}