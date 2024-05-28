package com.codeages.termiusplus.biz.storage.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.storage.dto.FileCreationDTO;
import com.codeages.termiusplus.biz.storage.entity.File;
import com.codeages.termiusplus.biz.storage.enums.FileTargetTypeEnum;
import com.codeages.termiusplus.biz.storage.mapper.FileMapper;
import com.codeages.termiusplus.biz.storage.repository.FileRepository;
import com.codeages.termiusplus.biz.storage.service.StorageService;
import com.codeages.termiusplus.biz.storage.vo.FileDto;
import com.codeages.termiusplus.exception.AppException;
import com.codeages.termiusplus.security.SecurityContext;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageServiceImpl implements StorageService {
    @Value("${file.dir}")
    private String fileDir;

    private final FileMapper fileMapper;

    private final FileRepository fileRepository;

    private final SecurityContext securityContext;

    public StorageServiceImpl(FileMapper fileMapper, FileRepository fileRepository, SecurityContext securityContext) {
        this.fileMapper = fileMapper;
        this.fileRepository = fileRepository;
        this.securityContext = securityContext;
    }


    @Override
    public FileDto store(MultipartFile upload, FileTargetTypeEnum targetType) {
        String uuid = IdUtil.simpleUUID();

        FileCreationDTO dto = new FileCreationDTO(
                upload.getOriginalFilename(),
                FileUtil.extName(upload.getOriginalFilename()),
                upload.getSize(),
                securityContext.getUser().getId(),
                transfer(upload, uuid),
                targetType,
                uuid,
                DateUtil.date()
        );

        File saveFile = fileRepository.save(fileMapper.toEntity(dto));

        return fileMapper.toVO(saveFile);
    }

    @Override
    public FileDto store(java.io.File file, FileTargetTypeEnum targetType) {
        String uuid = IdUtil.simpleUUID();

        FileCreationDTO dto = new FileCreationDTO(
                file.getName(),
                FileUtil.extName(file.getName()),
                FileUtil.size(file),
                securityContext.getUser().getId(),
                transfer(file, uuid),
                targetType,
                uuid,
                DateUtil.date()
        );

        File saveFile = fileRepository.save(fileMapper.toEntity(dto));


        return fileMapper.toVO(saveFile);
    }

    @Override
    public File getFileByUUID(String uuid) {
        if (StrUtil.isBlank(uuid)) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT);
        }

        return fileRepository.getByUuid(uuid)
                             .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "文件不存在"));
    }

    @Override
    public FileDto getFileInfo(String uuid) {
        return fileMapper.toVO(getFileByUUID(uuid));
    }

    @Override
    public File getPublicFileByUUID(String uuid) {
        if (StrUtil.isBlank(uuid)) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT);
        }

        return fileRepository.getByUuid(uuid)
                             .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "文件不存在"));
    }

    @SneakyThrows(IOException.class)
    private String transfer(MultipartFile upload, String uuid) {
        String fileName = uuid + '.' + FileUtil.extName(upload.getOriginalFilename());
        String fileTargetPath = getUploadPath() + FileUtil.FILE_SEPARATOR + fileName;

        FileUtil.writeBytes(upload.getBytes(), fileTargetPath);

        return fileTargetPath;
    }


    private String transfer(java.io.File file, String uuid) {
        String fileName = uuid + '.' + FileUtil.extName(file.getName());
        String fileTargetPath = getUploadPath() + FileUtil.FILE_SEPARATOR + fileName;

        FileUtil.move(file, new java.io.File(fileTargetPath), true);

        return fileTargetPath;
    }

    private String getUploadPath() {
        String storageDir = fileDir + FileUtil.FILE_SEPARATOR + DateUtil.format(DateUtil.date(), "yyyyMM");
        if (!FileUtil.exist(storageDir)) {
            FileUtil.mkdir(storageDir);
        }

        return storageDir;
    }

    @Override
    public List<FileDto> findFileInfo(List<String> uuids) {
        return fileRepository.findAllByUuidIn(uuids).stream().map(fileMapper::toVO).collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String uuid) {
        File file = getFileByUUID(uuid);
        FileUtil.del(file.getUri());
        fileRepository.delete(file);
    }

    @Override
    @SneakyThrows(IOException.class)
    public FileDto store(ByteArrayOutputStream outputStream, String fileName, FileTargetTypeEnum targetType) {
        byte[] content = outputStream.toByteArray();
        outputStream.close();

        return store(content, fileName, targetType);
    }

    public FileDto store(byte[] content, String fileName, FileTargetTypeEnum targetType) {
        String uuid = IdUtil.simpleUUID();

        FileCreationDTO dto = new FileCreationDTO(
                fileName,
                FileUtil.extName(fileName),
                (long) content.length,
                securityContext.getUser().getId(),
                transfer(content, fileName, uuid),
                targetType,
                uuid,
                DateUtil.date()
        );


        return fileMapper.toVO(fileRepository.save(fileMapper.toEntity(dto)));
    }

    private String transfer(byte[] content, String name, String uuid) {
        String fileName = uuid + '.' + FileUtil.extName(name);
        String fileTargetPath = getUploadPath() + FileUtil.FILE_SEPARATOR + fileName;

        FileUtil.writeBytes(content, fileTargetPath);

        return fileTargetPath;
    }

}
