package com.aiweb.utils;

import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class SmsUtil {
    /**
     * 对短信猫中我们登陆的密码进行指定的md5加密
     * @param plainText
     * @return
     */
    public static String md5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 使用 UTF-8 编码进行哈希
            md.update(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] b = md.digest();

            // 将字节数组转换为 32 位十六进制字符串
            StringBuilder buf = new StringBuilder("");
            for (int offset = 0; offset < b.length; offset++) {
                int i = b[offset];
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (Exception e) {
            // 如果 MD5 算法不可用，抛出运行时异常
            throw new RuntimeException("MD5 加密失败", e);
        }
    }

    /**
     * 对 URL 参数进行 UTF-8 编码
     * 用于编码短信内容中的中文。
     */
    public static String encodeUrlString(String str) {
        if (str == null) return null;
        try {
            // 直接使用 Java 标准库的 URLEncoder
            return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            throw new RuntimeException("URL 编码失败", e);
        }
    }

    //短信宝的错误提示信息
    public static String getErrorMessage(String code) {
        switch (code) {
            case "30": return "密码错误";
            case "40": return "账号或手机号为空";
            case "41": return "余额不足";
            case "42": return "内容为空";
            case "43": return "包含敏感词";
            case "50": return "手机号码错误";
            case "51": return "签名错误";
            default: return "未知错误";
        }
    }

}
