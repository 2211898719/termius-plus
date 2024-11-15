package com.codeages.termiusplus.biz.sql.mapper;

import com.codeages.termiusplus.biz.sql.dto.DbConnCreateParams;
import com.codeages.termiusplus.biz.sql.dto.DbConnDto;
import com.codeages.termiusplus.biz.sql.dto.DbConnUpdateParams;
import com.codeages.termiusplus.biz.sql.entity.DbConn;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DbConnMapper {
    DbConnDto toDto(DbConn dbConn);

    DbConn toCreateEntity(DbConnCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DbConn toUpdateEntity(@MappingTarget DbConn dbConn, DbConnUpdateParams updateParams);
}
