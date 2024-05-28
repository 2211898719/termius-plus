package com.codeages.termiusplus.biz.storage.mapper;

import com.codeages.termiusplus.biz.storage.dto.FileCreationDTO;
import com.codeages.termiusplus.biz.storage.entity.File;
import com.codeages.termiusplus.biz.storage.vo.FileDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileDto toVO(File file);

    File toEntity(FileCreationDTO dto);
}
