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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api-admin/user")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
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
