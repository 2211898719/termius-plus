package com.codeages.javaskeletonserver.biz.storage.mapper;

import com.codeages.javaskeletonserver.biz.storage.dto.FileCreationDTO;
import com.codeages.javaskeletonserver.biz.storage.entity.File;
import com.codeages.javaskeletonserver.biz.storage.vo.FileDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileDto toVO(File file);

    File toEntity(FileCreationDTO dto);
}
