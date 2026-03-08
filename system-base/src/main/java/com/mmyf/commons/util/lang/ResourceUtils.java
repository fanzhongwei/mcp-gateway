package com.mmyf.commons.util.lang;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * package com.mmyf.commons.util <br/>
 * description: TODO <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/5/30
 */
public class ResourceUtils {

    /**
     * Gets resource name.
     *
     * @param <T>   the type parameter
     * @param clazz the clazz
     * @return the resource name
     */
    public static  <T> String getResourceName(Class<T> clazz) {
        return clazz.getDeclaredAnnotation(Schema.class).name();
    }

}
