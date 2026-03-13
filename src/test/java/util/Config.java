package util;

/**
 * Читает настройки из переменных окружения.
 * Если переменная не задана — используется дефолтное значение для локального запуска.
 */
public class Config {

    public static String appUrl() {
        return System.getenv().getOrDefault("APP_URL", "http://localhost:8080");
    }

    public static String wireMockUrl() {
        return System.getenv().getOrDefault("WIREMOCK_URL", "http://localhost:8888");
    }
}
