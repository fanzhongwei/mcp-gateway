package com.mmyf.commons.exception;

import com.mmyf.commons.util.lang.ResourceUtils;
import org.slf4j.helpers.MessageFormatter;

/**
 * 资源读取失败
 *
 */
public class ResourceReadFailedException extends RuntimeException {

    private static final String MESSAGE = "资源 [%s] 读取失败!";
    private static final long serialVersionUID = -5157112925985329282L;

    /**
     * Instantiates a new Resource read failed exception.
     *
     * @param resourceType the resource type
     */
    public <T> ResourceReadFailedException(Class<T> resourceType) {
        super(String.format(MESSAGE, ResourceUtils.getResourceName(resourceType)));
    }

    /**
     * Constructs a new runtime exceptions with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     *
     * @param message the message
     */
    public ResourceReadFailedException(String message) {
        super(message);
    }

    /**
     * Instantiates a new custom exception.
     *
     * @param message the message
     * @param args    the args
     */
    public ResourceReadFailedException(String message, Object... args) {
        super(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    /**
     * Instantiates a new custom exception.
     *
     * @param e       the e
     * @param message the message
     * @param args    the args
     */
    public ResourceReadFailedException(Throwable e, String message, Object... args) {
        super(MessageFormatter.arrayFormat(message, args).getMessage(), e);
    }
}
