package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;

public class TestSuite {
    @BeforeAll
    static void writeEnvironment() throws Exception {
        Properties props = new Properties();
        props.setProperty("App.URL", Config.appUrl());
        props.setProperty("WireMock.URL", Config.wireMockUrl());
        props.setProperty("App.Version", "0.0.1-SNAPSHOT");
        Path path = Paths.get("target/allure-results/environment.properties");
        Files.createDirectories(path.getParent());
        props.store(Files.newOutputStream(path), null);
    }

}
