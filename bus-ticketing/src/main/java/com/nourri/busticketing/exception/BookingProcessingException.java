package com.nourri.busticketing.exception;

public class BookingProcessingException extends RuntimeException {
    public BookingProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
