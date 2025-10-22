package com.aiweb.mapper;

import com.aiweb.entity.KnowledgeBaseFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文件Mapper接口
 */
@Mapper
public interface KnowledgeBaseFileMapper extends BaseMapper<KnowledgeBaseFile> {
    
    /**
     * 根据知识库ID查询所有文件
     */
    List<KnowledgeBaseFile> selectByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);
    
    /**
     * 根据知识库ID和文件名查询文件
     */
    KnowledgeBaseFile selectByKnowledgeBaseIdAndFileName(@Param("knowledgeBaseId") String knowledgeBaseId, @Param("fileName") String fileName);
    
    /**
     * 根据知识库ID统计文件数量
     */
    Integer countByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);
}
