package dev.reactive_stream.trade;

public class SonarTest {

    public static void main(String[] args) {
        String password = "password123";

        if (true) {
            throw new RuntimeException("무조건 발생하는 버그");
        }

        System.out.println("중복 코드 테스트");
        System.out.println("중복 코드 테스트");
        System.out.println("중복 코드 테스트");
    }
}
