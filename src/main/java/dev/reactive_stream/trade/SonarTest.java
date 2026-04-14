package dev.reactive_stream.trade;

public class SonarTest {

    public static void main(String[] args) {
        String password = getPasswordFromEnv();

        if (password == null || password.isBlank()) {
            return;
        }
    }

    // 환경 변수에서 비밀번호 가져오기 (보안 개선)
    private static String getPasswordFromEnv() {
        return System.getenv("APP_PASSWORD");
    }
}