package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.user.dto.UserCreateParams;
import com.codeages.termiusplus.biz.user.dto.UserDto;
import com.codeages.termiusplus.biz.user.dto.UserSearchParams;
import com.codeages.termiusplus.biz.user.dto.UserUpdateParams;
import com.codeages.termiusplus.biz.user.service.UserService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.common.PagerResponse;
import com.codeages.termiusplus.exception.AppException;
import com.codeages.termiusplus.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api-admin/user")
public class AdminUserController {

    private final UserService userService;
    private final SecurityContext securityContext;

    public AdminUserController(UserService userService, SecurityContext securityContext) {
        this.userService = userService;
        this.securityContext = securityContext;
    }


    @PostMapping("/create")
    public UserDto create(@RequestBody UserCreateParams params) {
        return userService.create(params);
    }

    @PostMapping("/update")
    public UserDto update(@RequestBody UserUpdateParams params) {
        return userService.update(params);
    }

    @PostMapping("/lock")
    public OkResponse lock(@RequestBody IdPayload id) {
        userService.lock(id.getId());
        return OkResponse.TRUE;
    }

    @PostMapping("/unlock")
    public OkResponse unlock(@RequestBody IdPayload id) {
        userService.unlock(id.getId());
        return OkResponse.TRUE;
    }

    @PostMapping("/resetPassword")
    public Map<String, String> resetPassword(@RequestBody IdPayload id) {
        return Map.of("password", userService.resetPassword(id.getId()));
    }

    public static void main(String[] args) {
        String proxyHost = "localhost"; // 代理主机
        int proxyPort = 7890; // 代理端口
        String targetHost = "36.136.32.72"; // 目标主机
        int targetPort = 2221; // telnet 默认端口
        int timeout = 5000; // 超时设置

        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));

        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket(proxy)) {
            socket.connect(new InetSocketAddress(targetHost, targetPort), timeout);
            System.out.println("通过代理连接成功！" + (System.currentTimeMillis() - startTime) + "ms");
        } catch (IOException e) {
            System.out.println("通过代理连接失败：" + e.getMessage());
        }
    }

    @GetMapping("/get")
    public UserDto get(@RequestParam Long id) {
        return userService.get(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    @GetMapping("/getByUsername")
    public UserDto getByUsername(@RequestParam String username) {
        return userService.getByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    @GetMapping("/search")
    public PagerResponse<UserDto> search(UserSearchParams params, @PageableDefault(size = 20, sort = { "createdAt" }, direction = Sort.Direction.DESC) Pageable pager) {
        var users = userService.search(params, pager);
        return new PagerResponse<>(users, pager);
    }

    @GetMapping("/list")
    public List<UserDto> list(UserSearchParams params) {
        return userService.search(params, Pageable.unpaged()).getContent();
    }

}
