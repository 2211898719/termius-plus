package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.server.dto.LsFileDto;
import com.codeages.javaskeletonserver.biz.server.dto.SFTPParams;
import com.codeages.javaskeletonserver.biz.server.service.SFTPService;
import com.codeages.javaskeletonserver.biz.storage.utils.FileUtil;
import com.codeages.javaskeletonserver.common.IdPayload;
import lombok.SneakyThrows;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api-admin/sftp")
public class SFTPController {

    private final SFTPService sftpService;

    public SFTPController(SFTPService sftpService) {
        this.sftpService = sftpService;
    }

    @PostMapping("/init")
    public String init(@RequestBody IdPayload idPayload) {
        return sftpService.init(idPayload.getId());
    }

    @GetMapping("/{id}/pwd")
    public String pwd(@PathVariable String id) {
        return sftpService.pwd(id);
    }

    @GetMapping("/{id}/ls")
    public List<RemoteResourceInfo> ls(@PathVariable String id, SFTPParams params) {
        return sftpService.ls(id, params.getRemotePath());
    }

    @PostMapping("/{id}/mkdir")
    void mkdir(@PathVariable String id, @RequestBody SFTPParams params) {
        sftpService.mkdir(id, params.getRemotePath());
    }

    @PostMapping("/{id}/rm")
    public void rm(@PathVariable String id, @RequestBody SFTPParams params) {
        sftpService.rm(id, params.getRemotePath());
    }

    @PostMapping("/{id}/rmDir")
    public void rmDir(@PathVariable String id, @RequestBody SFTPParams params) {
        sftpService.rmDir(id, params.getRemotePath());
    }

    @PostMapping("/{id}/rename")
    public void rename(@PathVariable String id, @RequestBody SFTPParams params) {
        sftpService.rename(id, params.getRemotePath(), params.getNewRemotePath());
    }

    @PostMapping("/{id}/upload")
    public void upload(@PathVariable String id,
                       @RequestParam("file") MultipartFile file,
                       @RequestParam("remotePath") String remotePath) {
        sftpService.upload(id, file, remotePath);
    }

    @SneakyThrows
    @GetMapping("/{id}/download")
    public void download(@PathVariable String id, SFTPParams params, HttpServletResponse response) {
        String filename = params.getRemotePath().substring(params.getRemotePath().lastIndexOf("/") + 1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        sftpService.download(id, params.getRemotePath(), outputStream);
        FileUtil.writeByteToResponse(outputStream.toByteArray(), filename, response);
    }

    @PostMapping("/{id}/close")
    public void close(@PathVariable String id) {
        sftpService.close(id);
    }
}


