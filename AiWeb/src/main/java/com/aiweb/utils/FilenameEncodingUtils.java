package com.aiweb.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 文件名编码处理工具类
 * 专门解决中文字符在文件上传时的编码问题
 */
@Slf4j
public class FilenameEncodingUtils {
    
    /**
     * 获取正确的文件名，解决中文编码问题
     * 保持原始文件名不变，只修复编码问题
     */
    public static String getCorrectFilename(MultipartFile file) {
        if (file == null) {
            return "unknown_file";
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "uploaded_file_" + System.currentTimeMillis();
        }
        
        log.info("原始文件名: {}", originalFilename);
        
        // 检查是否包含乱码字符
        if (containsGarbledCharacters(originalFilename)) {
            log.warn("检测到文件名包含乱码，尝试修复编码但保持原始文件名结构");
            return fixFilenameEncodingPreserveStructure(originalFilename);
        }
        
        // 如果没有乱码，直接返回原始文件名
        log.info("文件名正常，保持原始文件名: {}", originalFilename);
        return originalFilename;
    }
    
    /**
     * 检查文件名是否包含乱码字符
     */
    private static boolean containsGarbledCharacters(String filename) {
        if (filename == null) return true;
        
        // 检查常见的乱码模式
        return filename.contains("?") || 
               filename.contains("") || 
               filename.contains("æ") || 
               filename.contains("å") || 
               filename.contains("è") ||
               filename.contains("é") ||
               filename.contains("®") ||
               filename.contains("¡") ||
               filename.contains("½");
    }
    
    /**
     * 修复文件名编码，保持原始文件名结构
     * 使用更强大的编码检测和修复算法
     */
    private static String fixFilenameEncodingPreserveStructure(String originalFilename) {
        String fixedFilename = originalFilename;
        
        log.info("开始修复文件名编码: {}", originalFilename);
        
        try {
            // 方法1: GBK -> GB2312 (测试结果显示这是最有效的方法)
            if (containsGarbledCharacters(originalFilename)) {
                try {
                    byte[] bytes = originalFilename.getBytes("GBK");
                    fixedFilename = new String(bytes, "GB2312");
                    log.info("GBK转GB2312: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("GBK转GB2312成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("GBK转GB2312失败: {}", e.getMessage());
                }
            }
            
            // 方法2: GB2312 -> GBK (测试结果显示这也是有效的方法)
            if (containsGarbledCharacters(fixedFilename)) {
                try {
                    byte[] bytes = originalFilename.getBytes("GB2312");
                    fixedFilename = new String(bytes, "GBK");
                    log.info("GB2312转GBK: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("GB2312转GBK成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("GB2312转GBK失败: {}", e.getMessage());
                }
            }
            
            // 方法3: 直接尝试UTF-8解码（处理双重编码问题）
            if (containsGarbledCharacters(fixedFilename)) {
                try {
                    // 先尝试将字符串按ISO-8859-1编码获取字节，然后按UTF-8解码
                    byte[] bytes = originalFilename.getBytes(StandardCharsets.ISO_8859_1);
                    fixedFilename = new String(bytes, StandardCharsets.UTF_8);
                    log.info("UTF-8解码尝试: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("UTF-8解码成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("UTF-8解码失败: {}", e.getMessage());
                }
            }
            
            // 方法4: 尝试GBK编码转换
            if (containsGarbledCharacters(fixedFilename)) {
                try {
                    byte[] bytes = originalFilename.getBytes("GBK");
                    fixedFilename = new String(bytes, StandardCharsets.UTF_8);
                    log.info("GBK转UTF-8: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("GBK转换成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("GBK转换失败: {}", e.getMessage());
                }
            }
            
            // 方法5: 尝试GB2312编码转换
            if (containsGarbledCharacters(fixedFilename)) {
                try {
                    byte[] bytes = originalFilename.getBytes("GB2312");
                    fixedFilename = new String(bytes, StandardCharsets.UTF_8);
                    log.info("GB2312转UTF-8: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("GB2312转换成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("GB2312转换失败: {}", e.getMessage());
                }
            }
            
            // 方法4: 尝试Windows-1252编码转换
            if (containsGarbledCharacters(fixedFilename)) {
                try {
                    byte[] bytes = originalFilename.getBytes("Windows-1252");
                    fixedFilename = new String(bytes, StandardCharsets.UTF_8);
                    log.info("Windows-1252转UTF-8: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("Windows-1252转换成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("Windows-1252转换失败: {}", e.getMessage());
                }
            }
            
            // 方法5: 尝试CP1252编码转换
            if (containsGarbledCharacters(fixedFilename)) {
                try {
                    byte[] bytes = originalFilename.getBytes("CP1252");
                    fixedFilename = new String(bytes, StandardCharsets.UTF_8);
                    log.info("CP1252转UTF-8: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("CP1252转换成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("CP1252转换失败: {}", e.getMessage());
                }
            }
            
            // 方法6: 尝试Latin1编码转换
            if (containsGarbledCharacters(fixedFilename)) {
                try {
                    byte[] bytes = originalFilename.getBytes("Latin1");
                    fixedFilename = new String(bytes, StandardCharsets.UTF_8);
                    log.info("Latin1转UTF-8: {} -> {}", originalFilename, fixedFilename);
                    
                    if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                        log.info("Latin1转换成功，包含中文字符");
                        return fixedFilename;
                    }
                } catch (Exception e) {
                    log.warn("Latin1转换失败: {}", e.getMessage());
                }
            }
            
            // 方法7: 高级编码检测和修复
            if (containsGarbledCharacters(fixedFilename)) {
                fixedFilename = advancedEncodingFix(originalFilename);
                if (!containsGarbledCharacters(fixedFilename) && containsChineseCharacters(fixedFilename)) {
                    log.info("高级编码修复成功: {} -> {}", originalFilename, fixedFilename);
                    return fixedFilename;
                }
            }
            
        } catch (Exception e) {
            log.error("文件名编码修复过程中发生异常: {}", e.getMessage(), e);
        }
        
        // 如果所有方法都失败，返回原始文件名
        if (containsGarbledCharacters(fixedFilename)) {
            log.warn("所有编码修复方法都失败，保持原始文件名: {}", originalFilename);
            return originalFilename;
        }
        
        log.info("文件名编码修复成功: {} -> {}", originalFilename, fixedFilename);
        return fixedFilename;
    }
    
    /**
     * 高级编码修复算法
     * 针对复杂的编码问题进行深度修复
     * 优先使用测试成功的编码转换方式
     */
    private static String advancedEncodingFix(String originalFilename) {
        try {
            log.info("开始高级编码修复: {}", originalFilename);
            
            // 优先尝试测试成功的编码转换方式
            String[][] priorityEncodings = {
                {"GBK", "GB2312"},      // 测试成功的方法1
                {"GB2312", "GBK"},     // 测试成功的方法2
                {"UTF-8", "GBK"},       // 其他可能有效的方法
                {"UTF-8", "GB2312"},
                {"GBK", "UTF-8"},
                {"GB2312", "UTF-8"}
            };
            
            // 先尝试优先的编码转换
            for (String[] encoding : priorityEncodings) {
                try {
                    byte[] bytes = originalFilename.getBytes(encoding[0]);
                    String result = new String(bytes, encoding[1]);
                    
                    // 检查结果是否包含中文字符且没有乱码
                    if (containsChineseCharacters(result) && !containsGarbledCharacters(result)) {
                        log.info("优先编码修复成功: {} -> {} ({} -> {})", originalFilename, result, encoding[0], encoding[1]);
                        return result;
                    }
                } catch (Exception e) {
                    // 忽略转换失败的情况
                }
            }
            
            // 如果优先方法都失败，尝试所有编码组合
            String[] allEncodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1", "Windows-1252", "CP1252", "Latin1"};
            
            for (String sourceEncoding : allEncodings) {
                for (String targetEncoding : allEncodings) {
                    if (sourceEncoding.equals(targetEncoding)) continue;
                    
                    try {
                        byte[] bytes = originalFilename.getBytes(sourceEncoding);
                        String result = new String(bytes, targetEncoding);
                        
                        // 检查结果是否包含中文字符且没有乱码
                        if (containsChineseCharacters(result) && !containsGarbledCharacters(result)) {
                            log.info("高级编码修复成功: {} -> {} ({} -> {})", originalFilename, result, sourceEncoding, targetEncoding);
                            return result;
                        }
                    } catch (Exception e) {
                        // 忽略转换失败的情况
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("高级编码修复失败: {}", e.getMessage(), e);
        }
        
        return originalFilename;
    }
    
    
    
    
    /**
     * 验证文件名是否包含中文字符
     */
    public static boolean containsChineseCharacters(String filename) {
        if (filename == null) return false;
        
        for (char c : filename.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) { // 中文字符范围
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理重复文件名，添加数字后缀
     * @param originalFilename 原始文件名
     * @param existingFilenames 已存在的文件名列表
     * @return 处理后的唯一文件名
     */
    public static String handleDuplicateFilename(String originalFilename, java.util.List<String> existingFilenames) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "document_" + System.currentTimeMillis();
        }
        
        if (existingFilenames == null || existingFilenames.isEmpty()) {
            return originalFilename;
        }
        
        // 检查是否已存在
        if (!existingFilenames.contains(originalFilename)) {
            return originalFilename;
        }
        
        // 分离文件名和扩展名
        String nameWithoutExt;
        String extension;
        int lastDotIndex = originalFilename.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            nameWithoutExt = originalFilename.substring(0, lastDotIndex);
            extension = originalFilename.substring(lastDotIndex);
        } else {
            nameWithoutExt = originalFilename;
            extension = "";
        }
        
        // 查找可用的数字后缀
        int counter = 1;
        String newFilename;
        do {
            newFilename = nameWithoutExt + "_" + counter + extension;
            counter++;
        } while (existingFilenames.contains(newFilename) && counter <= 999);
        
        log.info("处理重复文件名: {} -> {}", originalFilename, newFilename);
        return newFilename;
    }
    
    /**
     * 生成唯一的文件名（用于数据库存储）
     * 格式：原始文件名_时间戳
     */
    public static String generateUniqueFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "document_" + System.currentTimeMillis();
        }
        
        String nameWithoutExt;
        String extension;
        int lastDotIndex = originalFilename.lastIndexOf('.');
        
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            nameWithoutExt = originalFilename.substring(0, lastDotIndex);
            extension = originalFilename.substring(lastDotIndex);
        } else {
            nameWithoutExt = originalFilename;
            extension = "";
        }
        
        return nameWithoutExt + "_" + System.currentTimeMillis() + extension;
    }
}
