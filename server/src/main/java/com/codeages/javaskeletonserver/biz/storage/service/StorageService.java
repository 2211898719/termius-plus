package com.codeages.javaskeletonserver.biz.storage.service;


import com.codeages.javaskeletonserver.biz.storage.entity.File;
import com.codeages.javaskeletonserver.biz.storage.enums.FileTargetTypeEnum;
import com.codeages.javaskeletonserver.biz.storage.vo.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface StorageService {
    FileDto store(MultipartFile file, FileTargetTypeEnum targetType);

    FileDto store(java.io.File file, FileTargetTypeEnum targetType);

    File getFileByUUID(String uuid);

    FileDto getFileInfo(String uuid);

    File getPublicFileByUUID(String uuid);

    List<FileDto> findFileInfo(List<String> uuids);

    void deleteFile(String uuid);

    FileDto store(ByteArrayOutputStream outputStream, String fileName, FileTargetTypeEnum targetType);

}
