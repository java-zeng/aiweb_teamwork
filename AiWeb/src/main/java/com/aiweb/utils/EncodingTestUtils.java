package com.aiweb.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 编码测试工具类
 * 用于测试和验证各种编码转换效果
 */
@Slf4j
public class EncodingTestUtils {
    
    /**
     * 测试所有可能的编码转换
     */
    public static String testAllEncodings(String garbledFilename) {
        if (garbledFilename == null || garbledFilename.isEmpty()) {
            return "输入为空";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("=== 编码转换测试 ===\n");
        result.append("原始乱码: ").append(garbledFilename).append("\n\n");
        
        String[] encodings = {
            "UTF-8", "GBK", "GB2312", "ISO-8859-1", 
            "Windows-1252", "CP1252", "Latin1", "ASCII"
        };
        
        for (String sourceEncoding : encodings) {
            for (String targetEncoding : encodings) {
                if (sourceEncoding.equals(targetEncoding)) continue;
                
                try {
                    byte[] bytes = garbledFilename.getBytes(sourceEncoding);
                    String converted = new String(bytes, targetEncoding);
                    
                    // 检查是否包含中文字符
                    boolean hasChinese = containsChineseCharacters(converted);
                    boolean hasGarbled = containsGarbledCharacters(converted);
                    
                    if (hasChinese && !hasGarbled) {
                        result.append("✅ 成功: ").append(sourceEncoding)
                              .append(" -> ").append(targetEncoding)
                              .append(" = ").append(converted).append("\n");
                    } else if (hasChinese) {
                        result.append("⚠️  部分成功: ").append(sourceEncoding)
                              .append(" -> ").append(targetEncoding)
                              .append(" = ").append(converted).append("\n");
                    }
                } catch (Exception e) {
                    // 忽略转换失败的情况
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 检查是否包含中文字符
     */
    private static boolean containsChineseCharacters(String text) {
        if (text == null) return false;
        
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) { // 中文字符范围
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否包含乱码字符
     */
    private static boolean containsGarbledCharacters(String text) {
        if (text == null) return true;
        
        return text.contains("?") || 
               text.contains("") || 
               text.contains("æ") || 
               text.contains("å") || 
               text.contains("è") ||
               text.contains("é") ||
               text.contains("®") ||
               text.contains("¡") ||
               text.contains("½");
    }
}
