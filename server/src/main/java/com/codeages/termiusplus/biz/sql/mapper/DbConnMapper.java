package com.codeages.termiusplus.biz.sql.mapper;

import com.codeages.termiusplus.biz.sql.entity.DbConn;
import com.codeages.termiusplus.biz.sql.dto.DbConnDto;
import com.codeages.termiusplus.biz.sql.dto.DbConnCreateParams;
import com.codeages.termiusplus.biz.sql.dto.DbConnUpdateParams;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DbConnMapper {
    DbConnDto toDto(DbConn dbConn);

    DbConn toCreateEntity(DbConnCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DbConn toUpdateEntity(@MappingTarget DbConn dbConn, DbConnUpdateParams updateParams);
}
