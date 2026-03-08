package com.mmyf.commons.constant;

/**
 * package com.mmyf.commons.constant <br/>
 * description: 抽象的枚举接口，用于翻译和存储数据库 <br/>
 *
 * @author Teddy
 * @date 2022/5/18
 */
public interface AbstractEnum {

    /**
     * 代码值，存储到数据库
     *
     * @return
     */
    String getCode();

    /**
     * 代码名称，用于翻译
     *
     * @return
     */
    String getDesc();

    /**
     * 判断码值是否相等
     * @param code 代码值
     * @return 是否相等
     */
    default boolean codeEquals(String code) {
        return getCode().equals(code);
    }
}
