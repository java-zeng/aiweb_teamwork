package com.aiweb.utils;

import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class SmsUtil {
    public String getCode(){
        Random random=new Random();
        String randomCode = String.valueOf(random.nextInt(900000) + 100000);
        return randomCode;
    }
}
