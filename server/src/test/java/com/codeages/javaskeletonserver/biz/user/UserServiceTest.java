package com.codeages.javaskeletonserver.biz.user;

import com.codeages.javaskeletonserver.UnitTestConfig;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.objectlog.service.ObjectLogService;
import com.codeages.javaskeletonserver.biz.user.dto.UserAuthedDto;
import com.codeages.javaskeletonserver.biz.user.dto.UserCreateParams;
import com.codeages.javaskeletonserver.biz.user.entity.User;
import com.codeages.javaskeletonserver.biz.user.manager.UserCacheManager;
import com.codeages.javaskeletonserver.biz.user.mapper.UserMapper;
import com.codeages.javaskeletonserver.biz.user.mapper.UserMapperImpl;
import com.codeages.javaskeletonserver.biz.user.repository.UserRepository;
import com.codeages.javaskeletonserver.biz.user.service.UserService;
import com.codeages.javaskeletonserver.biz.user.service.impl.UserServiceImpl;
import com.codeages.javaskeletonserver.exception.AppException;
import com.codeages.javaskeletonserver.security.AuthUser;
import com.codeages.javaskeletonserver.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.validation.Validation;
import javax.validation.Validator;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {UnitTestConfig.class})
public class UserServiceTest {

    private UserService userService;

    private UserRepository userRepo;

    private UserCacheManager cacheManager;

    private ObjectLogService logService;

    private SecurityContext context;

    private final UserMapper mapper;

    private final Validator validator;

    private final PasswordEncoder passwordEncoder;

    public UserServiceTest() {
        var vf = Validation.buildDefaultValidatorFactory();
        validator = vf.getValidator();
        mapper = new UserMapperImpl();
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @BeforeEach
    void beforeEach() {
        userRepo = mock(UserRepository.class);
        cacheManager = mock(UserCacheManager.class);
        logService = mock(ObjectLogService.class);
        context = mock(SecurityContext.class);

        userService = new UserServiceImpl(userRepo, mapper, cacheManager, validator, passwordEncoder, context, logService);
    }

    @Test
    void createWhenFirstTimeThanCreatedSuccess() {
        var authUserDto = new UserAuthedDto();
        authUserDto.setId(1L);
        authUserDto.setUsername("testAdmin");
        authUserDto.setRoles(List.of("ROLE_ADMIN"));

        var authUser = new AuthUser(authUserDto);
        authUser.setIp("127.0.0.1");

        // mock context.getUser()，返回了一个上述构建的 authUser
        when(context.getUser()).thenReturn(authUser);

        // mock userRepo.save ，并修改了入参
        when(userRepo.save(any())).thenAnswer((invocation -> {
            var user = (User) invocation.getArgument(0);
            user.setId(1L);
            return null;
        }));

        var params = new UserCreateParams();
        params.setUsername("test001");
        params.setPassword("testPassword");
        params.setEmail("test001@test.com");

        var user = userService.create(params);

        // 验证返回值
        assertEquals(1L, user.getId());
        assertEquals("test001", user.getUsername());

        // 验证内部关键逻辑：调用了缓存清除方法，并验证入参
        var removeCacheUserCaptor =  ArgumentCaptor.forClass(User.class);
        verify(cacheManager).removeCache(removeCacheUserCaptor.capture());
        var removeCacheUser = removeCacheUserCaptor.getAllValues().get(0);
        assertEquals(1L, removeCacheUser.getId());
        assertEquals("test001", removeCacheUser.getUsername());

        // 验证内部关键逻辑：调用了保存方法
        verify(userRepo).save(any());

        // 验证内部关键逻辑：记录了日志
        verify(logService).info(eq("User"), eq(1L), eq("create"), anyString());
    }

    @Test
    void createWhenUsernameExistThanThrowException() {
        when(userRepo.getByUsername(anyString())).thenReturn(Optional.of(new User()));

        var exception = assertThrows(AppException.class, () -> {
            var params = new UserCreateParams();
            params.setUsername("test001");
            params.setPassword("testPassword");
            params.setEmail("test001@test.com");
            var user = userService.create(params);
        });

        assertEquals(ErrorCode.INVALID_ARGUMENT.getCode(), exception.getCode());
        assertEquals("用户名已存在", exception.getMessage());
    }
}