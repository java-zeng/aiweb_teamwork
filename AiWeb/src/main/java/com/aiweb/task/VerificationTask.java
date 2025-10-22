package com.aiweb.task;


import com.aiweb.mapper.VerificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VerificationTask {
    @Autowired
    private VerificationMapper verificationMapper;

    /**
     * 定时清理未注册成功的验证信息
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanupExpiredVerifications() {
        log.info("开始执行定时任务：清理过期的验证记录...");
        try {
            verificationMapper.deleteExpiredAndUnverified();
            log.info("过期的验证记录清理完成。");
        } catch (Exception e) {
            log.error("清理过期验证记录时发生错误", e);
        }
    }
}
