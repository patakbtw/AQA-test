package util;

// Reads env, if it can't find the variable, it uses the default value
public class Config {

    public static String appUrl() {
        return System.getenv().getOrDefault("APP_URL", "http://localhost:8080");
    }

    public static String wireMockUrl() {
        return System.getenv().getOrDefault("WIREMOCK_URL", "http://localhost:8888");
    }
}
