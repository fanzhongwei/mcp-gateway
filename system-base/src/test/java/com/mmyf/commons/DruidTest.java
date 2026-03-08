package com.mmyf.commons;

import com.alibaba.druid.filter.config.ConfigTools;
import org.junit.jupiter.api.Test;

/**
 * package com.mmyf.commons
 * description: TODO
 * Copyright 2023 Teddy, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2023-12-31 14:07:06
 */
public class DruidTest {

    @Test
    public void decrypt() throws Exception {
        String publicKey = "xxx";
        String password = "xxx";

        System.out.println(ConfigTools.decrypt(publicKey, password));
        ConfigTools.main(new String[]{"xxx"});
    }
}
