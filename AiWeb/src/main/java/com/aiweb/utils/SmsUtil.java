package com.aiweb.utils;

import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

@Component
public class SmsUtil {


    public String getCode(){
        Random random=new Random();
        String randomCode = String.valueOf(random.nextInt(90000) + 10000);
        return randomCode;
    }



}
