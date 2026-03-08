package com.mmyf.commons.code;

import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.google.common.collect.Maps;
import com.mmyf.commons.model.entity.code.Code;
import com.mmyf.commons.model.entity.code.CodeType;
import com.mmyf.commons.util.PinYinUtils;
import com.mmyf.commons.util.SysUtils;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码枚举生成
 *
 * @author fanzhongwei
 * @date 2024/03/06 12:28
 **/
public class CodeEnumGeneratorTest {

    private DataSourceConfig dataSourceConfig = new DataSourceConfig.Builder("jdbc:mysql://localhost:3306/system_base",
            "db_payment", "db_payment").build();


    private static final String CODE_TYPE_SQL = "select * from system_base.t_code_type where cast(c_id as SIGNED)> 100010 order by cast(c_id as SIGNED)";
    private static final String CODE_SQL = "select * from system_base.t_code";

    @Test
    public void enumGen() throws Exception {
        try (Connection conn = dataSourceConfig.getConn()) {
            Map<String, CodeType> codeTypeMap = Maps.newLinkedHashMap();
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(CODE_TYPE_SQL);
            ) {
                while (resultSet.next()) {
                    CodeType codeType = new CodeType();
                    codeType.setId(resultSet.getString("c_id"));
                    codeType.setName(resultSet.getString("c_name"));
                    codeTypeMap.put(codeType.getId(), codeType);
                }
            }
            Map<String, List<Code>> codeTypeCodeListMap = Maps.newHashMap();
            try (Statement statement = conn.createStatement();
                 ResultSet resultSet = statement.executeQuery(CODE_SQL);
            ) {
                while (resultSet.next()) {
                    Code code = new Code();
                    code.setPid(resultSet.getString("c_pid"));
                    code.setCode(resultSet.getString("c_code"));
                    code.setName(resultSet.getString("c_name"));
                    codeTypeCodeListMap.computeIfAbsent(code.getPid(), c -> new ArrayList<>()).add(code);
                }
            }

            codeTypeMap.forEach(((type, codeType) -> {
                if (!codeTypeCodeListMap.containsKey(type)) {
                    return;
                }
                String enumName = PinYinUtils.getFirstSpell(codeType.getName());
                String enumItems = codeTypeCodeListMap.get(type)
                        .stream()
                        .map(code ->
                                SysUtils.stringFormat(
                                        "        /** {} */\n" +
                                           "        {}_{}(\"{}\", \"{}\")",
                                        code.getName(),
                                        PinYinUtils.getFirstSpell(code.getName()),
                                        code.getCode(),
                                        code.getCode(),
                                        code.getName()
                                ))
                        .collect(Collectors.joining(",\n"));

                System.out.println(SysUtils.stringFormat(
                        "    /**\n" +
                                "     * {}，{}\n" +
                                "     **/\n" +
                                "    @Getter\n" +
                                "    @AllArgsConstructor\n" +
                                "    enum {} implements AbstractEnum {\n" +
                                "{};" +
                                "\n" +
                                "        private final String code;\n" +
                                "        private final String desc;\n" +
                                "    };",
                        codeType.getName(),
                        codeType.getId(),
                        enumName,
                        enumItems
                ));
            }));
        }
    }
}
