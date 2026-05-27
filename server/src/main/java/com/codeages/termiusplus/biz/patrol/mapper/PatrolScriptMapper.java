package com.codeages.termiusplus.biz.patrol.mapper;

import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptCreateParams;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptDto;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptUpdateParams;
import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface PatrolScriptMapper {
    PatrolScriptDto toDto(PatrolScript entity);
    PatrolScript toCreateEntity(PatrolScriptCreateParams params);
    PatrolScript toUpdateEntity(@MappingTarget PatrolScript entity, PatrolScriptUpdateParams params);
}
