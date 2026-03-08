package com.mmyf.commons.translate.deserialzer;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.mmyf.commons.constant.AbstractEnum;
import java.lang.reflect.Type;
import org.apache.commons.lang3.StringUtils;

/**
 * package com.mmyf.commons.translate.deserialzer <br/>
 * description: TODO <br/>
 *
 * @author Teddy
 * @date 2022/5/23
 */
public class FastJsonEnumDeserializer implements ObjectDeserializer {

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object o) {
        final JSONLexer lexer = parser.lexer;
        final int token = lexer.token();

        Class cls = (Class) type;
        Enum[] enumConstants = (Enum[]) cls.getEnumConstants();

        if (AbstractEnum.class.isAssignableFrom(cls) && token == JSONToken.LITERAL_STRING) {
            String value = lexer.stringVal();
            for (Enum e : enumConstants) {
                if (StringUtils.equals(((AbstractEnum) e).getCode(), value)) {
                    return (T) e;
                }
            }
        } else {
            //没实现EnumValue接口的 默认的按名字或者按ordinal
            if (token == JSONToken.LITERAL_INT) {
                int intValue = lexer.intValue();
                lexer.nextToken(JSONToken.COMMA);
                if (intValue < 0 || intValue > enumConstants.length) {
                    throw new JSONException(String.format("parse enum %s error, value : %s", cls.getName(), intValue));
                }
                return (T) enumConstants[intValue];
            } else if (token == JSONToken.LITERAL_STRING) {
                return (T) Enum.valueOf(cls, lexer.stringVal());
            }
        }
        return null;

    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }

}
