/**
 * @projectName dbd-online-server
 * @package com.mmyf.commons.util
 * @className com.mmyf.commons.util.freemarkerUtils
 * @copyright Copyright 2020 Teddy, Inc All rights reserved.
 */
package com.mmyf.commons.util.freemarker;

import com.mmyf.commons.exception.ResourceCreatedFailedException;
import com.mmyf.commons.exception.ResourceWriteFailedException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * FreemarkerUtils
 *
 * @author Teddy
 * @version 1.0
 * @description Freemarker工具类
 * @date 2021/4/7 14:24
 */
@UtilityClass
@Slf4j
public class FreeMarkerUtils {

    /**
     * description: 写入数据
     * date: 2021/4/7 14:53
     * author: Teddy
     *
     * @param template 模板
     * @param data     数据
     * @return byte[] 字节数组
     */
    public static byte[] write(Template template, Object data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); Writer out = new OutputStreamWriter(baos)) {
            template.process(data, out);
            return baos.toByteArray();
        } catch (TemplateException | IOException e) {
            log.error("处理freemarker模版失败！模板--【{}】，处理数据--【{}】", template, data, e);
            throw new ResourceWriteFailedException("处理freemarker模版失败！", e);
        }
    }

    /**
     * description: 写入数据
     * date: 2021/4/7 14:52
     * author: Teddy
     *
     * @param templateDirPath 模板路径
     * @param templateName 模板名称
     * @param dataMap 数据
     * @return byte[] 字节数组
     */
    public static byte[] write(String templateDirPath, String templateName, Map<String, Object> dataMap) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream)) {
            final Template template = getTemplate(templateDirPath, templateName);
            template.process(dataMap, outputStreamWriter);
            return byteArrayOutputStream.toByteArray();
        } catch (TemplateException | IOException e) {
            log.error("处理freemarker模版失败！ 模版路径:{} 模版名称:{} 数据：{}", templateDirPath, templateName, dataMap.toString(), e);
            throw new ResourceWriteFailedException("处理freemarker模版失败！", e);
        }
    }

    /**
     * description: 根据路径获取模板 
     * date: 2021/4/7 14:43 
     * author: Teddy 
     *
     * @param templateDirPath 路径
     * @param templateName 模板名
     * @return freemarker.template.Template 模板
     */
    public static Template getTemplate(String templateDirPath, String templateName) {
        try {
            Configuration configuration = new Configuration(Configuration.getVersion());
            configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
            configuration.setClassLoaderForTemplateLoading(FreeMarkerUtils.class.getClassLoader(), templateDirPath);
            return configuration.getTemplate(templateName);
        } catch (IOException e) {
            log.error("获取freemarker模版失败！ 模版路径:{} 模版名称:{}", templateDirPath, templateName, e);
            throw new ResourceCreatedFailedException("获取freemarker模版失败！", e);
        }
    }
}
