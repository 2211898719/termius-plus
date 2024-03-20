package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.server.dto.SFTPInitParams;
import com.codeages.javaskeletonserver.biz.server.dto.SFTPParams;
import com.codeages.javaskeletonserver.biz.server.dto.SFTPServerUploadServerParams;
import com.codeages.javaskeletonserver.biz.server.service.SFTPService;
import lombok.SneakyThrows;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api-admin/sftp")
public class SFTPController {

    private final SFTPService sftpService;

    public SFTPController(SFTPService sftpService) {
        this.sftpService = sftpService;
    }

    @PostMapping("/init")
    public String init(@RequestBody SFTPInitParams sftpInitParams) {
        return sftpService.init(sftpInitParams.getSessionId(), sftpInitParams.getServerId());
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

    @PostMapping("/serverUploadServer")
    public void serverUploadServer(@RequestBody SFTPServerUploadServerParams params) {
        sftpService.serverUploadServer(params);
    }

    @SneakyThrows
    @GetMapping("/{id}/download")
    public void download(@PathVariable String id, SFTPParams params, HttpServletResponse response) {
        sftpService.download(id, params.getRemotePath(), response);
    }

    @PostMapping("/{id}/close")
    public void close(@PathVariable String id) {
        sftpService.close(id);
    }
}


