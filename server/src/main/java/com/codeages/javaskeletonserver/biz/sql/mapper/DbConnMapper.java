package com.codeages.javaskeletonserver.biz.sql.mapper;

import com.codeages.javaskeletonserver.biz.sql.entity.DbConn;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnDto;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnCreateParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnUpdateParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnSearchParams;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DbConnMapper {
    DbConnDto toDto(DbConn dbConn);

    DbConn toCreateEntity(DbConnCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DbConn toUpdateEntity(@MappingTarget DbConn dbConn, DbConnUpdateParams updateParams);
}
