package com.mmyf.commons.exception;

import com.mmyf.commons.util.lang.ResourceUtils;
import org.slf4j.helpers.MessageFormatter;

/**
 * 资源读取失败
 *
 */
public class ResourceWriteFailedException extends RuntimeException {

    private static final String MESSAGE = "资源 [%s] 写入失败!";
    private static final long serialVersionUID = 2220906099590361722L;

    /**
     * Instantiates a new Resource read failed exception.
     *
     * @param resourceType the resource type
     */
    public <T> ResourceWriteFailedException(Class<T> resourceType) {
        super(String.format(MESSAGE, ResourceUtils.getResourceName(resourceType)));
    }

    /**
     * Constructs a new runtime exceptions with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     *
     * @param message the message
     * @param e the Exception
     */
    public ResourceWriteFailedException(String message, Exception e) {
        super(message, e);
    }

    /**
     * Instantiates a new Resource update failed exception.
     *
     * @param throwable        the Throwable
     * @param message          the message
     * @param args             the args
     */
    public ResourceWriteFailedException(Throwable throwable, String message, Object... args) {
        super(MessageFormatter.arrayFormat(message, args)
                .getMessage(), throwable);
    }
}
