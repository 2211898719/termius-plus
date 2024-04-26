package com.codeages.javaskeletonserver.biz.application.mapper;

import com.codeages.javaskeletonserver.biz.application.dto.ApplicationServerCreateParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationServerDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationServerUpdateParams;
import com.codeages.javaskeletonserver.biz.application.entity.ApplicationServer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ApplicationServerMapper {
    ApplicationServerDto toDto(ApplicationServer applicationServer);

    ApplicationServer toCreateEntity(ApplicationServerCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ApplicationServer toUpdateEntity(@MappingTarget ApplicationServer applicationServer,
                                     ApplicationServerUpdateParams updateParams);
}
