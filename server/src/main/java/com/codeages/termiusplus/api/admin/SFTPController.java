package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.server.dto.SFTPInitParams;
import com.codeages.termiusplus.biz.server.dto.SFTPParams;
import com.codeages.termiusplus.biz.server.dto.SFTPServerUploadServerParams;
import com.codeages.termiusplus.biz.server.service.SFTPService;
import com.cxytiandi.encrypt.springboot.annotation.DecryptIgnore;
import com.cxytiandi.encrypt.springboot.annotation.EncryptIgnore;
import lombok.SneakyThrows;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

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
    @DecryptIgnore
    public void upload(@PathVariable String id,
                       @RequestParam("file") MultipartFile file,
                       @RequestParam("remotePath") String remotePath) {
        sftpService.upload(id, file, remotePath);
    }

    @PostMapping("/{id}/uploadFile")
    @DecryptIgnore
    public void uploadFile(@PathVariable String id,
                           @RequestParam("file") MultipartFile file,
                           @RequestParam("remotePath") String remotePath) {
        sftpService.uploadFile(id, file, remotePath);
    }

    @PostMapping("/serverUploadServer")
    public String serverUploadServer(@RequestBody SFTPServerUploadServerParams params) {
        return sftpService.asyncServerUploadServer(params);
    }

    @PostMapping("/cancelUploadTask")
    public boolean cancelUploadTask(@RequestBody SFTPServerUploadServerParams params) {
        return sftpService.cancelUploadTask(params.getTaskId());
    }

    @SneakyThrows
    @GetMapping("/{id}/download")
    @EncryptIgnore
    @DecryptIgnore
    public void download(HttpServletResponse response, @PathVariable String id, SFTPParams params) {
        sftpService.download(response, id, params.getRemotePath());
    }

    @PostMapping("/{id}/close")
    public void close(@PathVariable String id) {
        sftpService.close(id);
    }

    //以文本形式读取文件内容
    @GetMapping("/{id}/readFile")
    public ResponseEntity<String> readFile(@PathVariable String id, SFTPParams params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN); // 设置为纯文本
        return new ResponseEntity<>(
                new String(sftpService.readFile(id, params.getRemotePath())),
                headers,
                HttpStatus.OK
        );
    }

    //写入文件内容
    @PostMapping("/{id}/writeFile")
    public void writeFile(@PathVariable String id, @RequestBody SFTPParams params) {
        sftpService.writeFile(id, params.getRemotePath(), params.getContent());
    }
}


