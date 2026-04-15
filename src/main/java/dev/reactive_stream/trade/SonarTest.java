package dev.reactive_stream.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SonarTest {

    private static final Logger log = LoggerFactory.getLogger(SonarTest.class);

    public static void main(String[] args) {
        String password = System.getenv("APP_PASSWORD");

        if (isInvalid(password)) {
            log.warn("APP_PASSWORD is not set");
            return;
        }

        log.info("Application started successfully 배포테스트");
    }

    private static boolean isInvalid(String value) {
        return value == null || value.isBlank();
    }
}