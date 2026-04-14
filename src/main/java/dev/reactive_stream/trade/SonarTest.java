package dev.reactive_stream.trade;

public class SonarTest {

    public static void main(String[] args) {
        String password = getPasswordFromEnv();

        if (password == null || password.isBlank()) {
//            System.out.println("비밀번호가 설정되지 않았습니다.");
            return;
        }

        printMessage("중복 코드 테스트", 3);
    }

    // 환경 변수에서 비밀번호 가져오기 (보안 개선)
    private static String getPasswordFromEnv() {
        return System.getenv("APP_PASSWORD");
    }

    // 중복 제거
    private static void printMessage(String message, int count) {
        for (int i = 0; i < count; i++) {

        }
    }
}