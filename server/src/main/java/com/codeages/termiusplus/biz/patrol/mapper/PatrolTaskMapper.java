package com.codeages.termiusplus.biz.patrol.mapper;

import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;
import com.codeages.termiusplus.biz.patrol.entity.PatrolTask;
import org.mapstruct.Mapper;

@Mapper
public interface PatrolTaskMapper {
    PatrolTaskDto toDto(PatrolTask entity);
}
