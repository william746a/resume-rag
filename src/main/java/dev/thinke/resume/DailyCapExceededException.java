package dev.thinke.resume;

public class DailyCapExceededException extends RuntimeException {

    public DailyCapExceededException(String message) {
        super(message);
    }
}
