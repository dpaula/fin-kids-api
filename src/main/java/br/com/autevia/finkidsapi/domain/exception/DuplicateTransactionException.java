package br.com.autevia.finkidsapi.domain.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }
}
