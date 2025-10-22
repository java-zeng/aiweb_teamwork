package com.aiweb.mapper;

import com.aiweb.entity.UserRoleKnowledgeBase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色知识库绑定关系Mapper接口
 */
@Mapper
public interface UserRoleKnowledgeBaseMapper extends BaseMapper<UserRoleKnowledgeBase> {
    
    /**
     * 根据用户ID查询所有角色知识库
     */
    List<UserRoleKnowledgeBase> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和角色名称查询知识库
     */
    UserRoleKnowledgeBase selectByUserIdAndRoleName(@Param("userId") Long userId, @Param("roleName") String roleName);
    
    /**
     * 根据知识库ID查询知识库信息
     */
    UserRoleKnowledgeBase selectByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);
}
