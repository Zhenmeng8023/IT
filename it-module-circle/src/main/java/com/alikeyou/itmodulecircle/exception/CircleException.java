package com.alikeyou.itmodulecircle.exception;

public class CircleException extends RuntimeException {

    public CircleException(String message) {
        super(message);
    }

    public CircleException(String message, Throwable cause) {
        super(message, cause);
    }
}
