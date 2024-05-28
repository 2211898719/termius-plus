package com.codeages.termiusplus.api.admin;

import cn.hutool.core.collection.CollectionUtil;
import com.codeages.termiusplus.biz.storage.dto.FileFindInfoParams;
import com.codeages.termiusplus.biz.storage.enums.FileTargetTypeEnum;
import com.codeages.termiusplus.biz.storage.service.StorageService;
import com.codeages.termiusplus.biz.storage.utils.FileUtil;
import com.codeages.termiusplus.biz.storage.vo.FileDto;
import com.codeages.termiusplus.biz.user.dto.UserDto;
import com.codeages.termiusplus.biz.user.service.UserService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api-admin/file")
@Slf4j
public class FileController {

    private final StorageService storageService;

    private final UserService userService;

    public FileController(StorageService storageService, UserService userService) {
        this.storageService = storageService;
        this.userService = userService;
    }

    @PostMapping("/upload")
    public FileDto upload(@RequestParam("file") MultipartFile file,
                          @RequestParam(value = "targetType", defaultValue = "BASE") FileTargetTypeEnum targetType) {
        return storageService.store(file, targetType);
    }

    @GetMapping("/get/{uuid}")
    public void get(@PathVariable("uuid") String uuid, HttpServletResponse response) {
        FileUtil.writeFileToResponse(storageService.getFileByUUID(uuid), response);
    }

    @GetMapping("/getFileInfo/{uuid}")
    public FileDto getFileInfo(@PathVariable("uuid") String uuid) {
        return storageService.getFileInfo(uuid);
    }

    @GetMapping("/findFileInfoList")
    public Map<String, FileDto> findFileInfo(FileFindInfoParams fileFindInfoParams) {
        List<FileDto> fileDtoList = storageService.findFileInfo(fileFindInfoParams.getUuids());
        QueryUtils.batchQueryOneToOne(
                fileDtoList,
                FileDto::getUserId,
                userService::findAllByIdIn,
                UserDto::getId,
                (dto, userDto) -> dto.setUsername(userDto.getUsername())
        );

        return fileDtoList.stream()
                          .collect(Collectors.groupingBy(
                                  FileDto::getUuid,
                                  Collectors.collectingAndThen(Collectors.toList(), CollectionUtil::getFirst)
                          ));
    }

}
