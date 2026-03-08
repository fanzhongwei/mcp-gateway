package com.mmyf.commons.util.http;

import com.mmyf.commons.exception.ResourceReadFailedException;
import com.mmyf.commons.util.SysUtils;
import com.mmyf.commons.util.uuid.UUIDHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

/**
 * description: 文件下载工具类 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 20-12-22
 */
@UtilityClass
public class FileDownLoadUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownLoadUtils.class);

    private static final int DOWN_IN_MEM_SIZE = 4 * 1024 * 1024;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * 下载文件
     *
     * @param filename 下载的文件名，带后缀的哈
     * @param inputStream 下载的文件流
     * @param response HttpServletResponse
     */
    public static void downloadFile(String filename, InputStream inputStream, HttpServletResponse response) {
        try (TempInputStream tempInputStream = getTempInputStream(inputStream);
             OutputStream outputStream = response.getOutputStream()) {
            // 设置下载文件响应头
            response.setContentLength(tempInputStream.getLength());
            response.setContentType("application/octet-stream");
            String name = new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            // 设置文件头
            response.setHeader("Content-Disposition", "attachment;fileName=" + name);

            IOUtils.copy(tempInputStream, outputStream);
            outputStream.flush();
        } catch (Exception e) {
            LOGGER.error("下载文件【{}】失败", filename, e);
            throw new ResourceReadFailedException(SysUtils.stringFormat("下载文件【{}】失败，失败原因为：{}", filename, e.getMessage()));
        }
    }

    /**
     * 根据原始流获取下载的流，转换规则如下：</br>
     * 1、如果流中的内容 <= 4M(4 * 1024 * 1024)，直接放内存中，100个线程同时下载，内存占用也就400M左右，应该还能扛得住</br>
     * 2、如果流中的内容 > 4M(4 * 1024 * 1024)，先存成本地文件，然后再返回FileInputStream
     *
     * @param inputStream InputStream
     * @return TempDownloadInputStream 必须手动关闭
     */
    public static TempInputStream getTempInputStream(InputStream inputStream) {
        if (null == inputStream) {
            return null;
        }
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try {
            long count = 0;
            int n = 0;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (-1 != (n = inputStream.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
                // 大于DOWN_IN_MEM_SIZE，存临时文件，然后再下载
                if (count > DOWN_IN_MEM_SIZE) {
                    File tempFile = writeToTempFile(output.toByteArray(), inputStream);
                    return new TempInputStream(tempFile);
                }
            }
            // 小于等于DOWN_IN_MEM_SIZE，直接内存中下载即可
            return new TempInputStream(output.toByteArray());
        } catch (IOException e) {
            LOGGER.error("获取原始流失败，请检查", e);
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static File writeToTempFile(byte[] bytes, InputStream inputStream) {
        FileOutputStream outputStream = null;
        try {
            File tempFile = File.createTempFile(UUIDHelper.getUuid(), null);
            outputStream = new FileOutputStream(tempFile);
            IOUtils.write(bytes, outputStream);
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
            return tempFile;
        } catch (IOException e) {
            LOGGER.error("创建临时文件失败", e);
            return null;
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

    }

    /**
     * 临时文件流，用完close的时候自动删除
     */
    public static class TempInputStream extends FilterInputStream {

        private String path;
        private int length;

        public String getTempPath() {
            return path;
        }

        public int getLength() {
            return length;
        }

        private TempInputStream(File tempFile) throws FileNotFoundException {
            super(new FileInputStream(tempFile));
            path = tempFile.getAbsolutePath();
            length = (int)tempFile.length();
        }

        private TempInputStream(byte[] bytes) {
            super(new ByteArrayInputStream(bytes));
            length = bytes.length;
        }

        /**
         * 安静地关闭，并且删除临时文件
         */
        @Override
        public void close() {
            IOUtils.closeQuietly(in);
            in = new ClosedInputStream();
            if (StringUtils.isNotBlank(path)) {
                FileUtils.deleteQuietly(new File(this.path));
            }
        }
    }
}
