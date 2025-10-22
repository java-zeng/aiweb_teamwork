package com.aiweb.mapper;

import com.aiweb.entity.UserDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户文档数据访问层
 */
@Mapper
public interface UserDocumentMapper extends BaseMapper<UserDocument> {
}
