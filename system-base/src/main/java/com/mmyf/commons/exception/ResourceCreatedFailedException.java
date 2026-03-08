package com.mmyf.commons.exception;

import com.mmyf.commons.util.lang.ResourceUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ResourceCreatedFailedException extends RuntimeException {
    private static final String MESSAGE = "资源 [%s] 创建失败!";
    private static final long serialVersionUID = 9080559515965157906L;

    /**
     * Constructs a new runtime exceptions with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     *
     * @param resourceType the resource type
     */
    public <T> ResourceCreatedFailedException(Class<T> resourceType) {
        super(String.format(MESSAGE, ResourceUtils.getResourceName(resourceType)));
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for                later retrieval by the {@link #getMessage()} method.
     */
    public ResourceCreatedFailedException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Resource created failed exception.
     *
     * @param message the message
     * @param e       the e
     */
    public ResourceCreatedFailedException(String message, Exception e) {
        super(message, e);
    }

    /**
     * Instantiates a new custom exception.
     *
     * @param message the message
     * @param args    the args
     */
    public ResourceCreatedFailedException(String message, Object... args) {
        super(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    /**
     * Instantiates a new custom exception.
     *
     * @param e       the e
     * @param message the message
     * @param args    the args
     */
    public ResourceCreatedFailedException(Throwable e, String message, Object... args) {
        super(MessageFormatter.arrayFormat(message, args).getMessage(), e);
    }
}
