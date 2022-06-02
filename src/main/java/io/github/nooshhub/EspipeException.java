package io.github.nooshhub;

/**
 * @author neals
 * @since 6/1/2022
 */
public class EspipeException extends RuntimeException{

    public EspipeException(String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
