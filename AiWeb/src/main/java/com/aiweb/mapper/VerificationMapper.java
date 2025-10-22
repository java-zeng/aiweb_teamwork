package com.aiweb.mapper;

import com.aiweb.entity.Verification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VerificationMapper extends BaseMapper<Verification> {
    /**
     * 删除所有已过期且未验证成功的记录
     * (result is null or result = false)
     */
    @Delete("DELETE FROM verifications WHERE expire_time < NOW() AND (result IS NULL OR result = false)")
    void deleteExpiredAndUnverified();
}
