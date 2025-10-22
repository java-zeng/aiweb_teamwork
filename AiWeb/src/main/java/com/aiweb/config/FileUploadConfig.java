package com.aiweb.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;
import java.nio.charset.StandardCharsets;

/**
 * 文件上传配置类
 * 确保正确处理中文字符编码
 */
@Configuration
public class FileUploadConfig {

    /**
     * 配置文件上传解析器
     * 确保正确处理中文字符
     */
    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

    /**
     * 配置文件上传参数
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置文件大小限制
        factory.setMaxFileSize(DataSize.ofMegabytes(50));
        factory.setMaxRequestSize(DataSize.ofMegabytes(50));
        
        // 设置临时文件存储位置
        factory.setLocation(System.getProperty("java.io.tmpdir"));
        
        // 设置文件大小阈值（0表示所有文件都写入磁盘）
        factory.setFileSizeThreshold(DataSize.ofBytes(0));
        
        return factory.createMultipartConfig();
    }
}
