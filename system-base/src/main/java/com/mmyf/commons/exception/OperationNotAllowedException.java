package com.mmyf.commons.exception;

import org.slf4j.helpers.MessageFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * operation not allowed
 *
 * @author Teddy
 * @version 4.0
 */
@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class OperationNotAllowedException extends RuntimeException {
    private static final String MESSAGE = "不允许操作";
    private static final long serialVersionUID = 6173503547711529565L;

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public OperationNotAllowedException() {
        super(MESSAGE);
    }

    /**
     * Instantiates a new operation not allowed exceptions.
     *
     * @param s the detail message.
     */
    public OperationNotAllowedException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Operation not allowed exception.
     *
     * @param s the s
     * @param e the e
     */
    public OperationNotAllowedException(String s, Throwable e){
        super(s,e);
    }

    /**
     * Instantiates a new custom exception.
     *
     * @param message the message
     * @param args    the args
     */
    public OperationNotAllowedException(String message, Object... args) {
        super(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    /**
     * Instantiates a new custom exception.
     *
     * @param e       the e
     * @param message the message
     * @param args    the args
     */
    public OperationNotAllowedException(Throwable e, String message, Object... args) {
        super(MessageFormatter.arrayFormat(message, args).getMessage(), e);
    }
}
