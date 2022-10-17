# 数据校验

Service 层，每个涉及到业务数据变更的接口，都应对入参做数据校验。

## 基本使用

以 `UserService.create` 接口为例。

### STEP 1: 对参数对象添加校验注解

```java {8-9,12,13}
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import javax.validation.constraints.NotEmpty;

@Data
public class UserCreateParams {

    @NotEmpty
    @Length(min = 2, max = 48)
    private String username;

    @NotEmpty
    @Length(min = 5, max = 16)
    private String password;
}
```

注解：
* `@NotEmpty` ： 字段不能为空。
* `@Length` ： 字段的字符数范围。


### STEP 2: 将 `javax.validation.Validator` 对象，注入 Service

```java {6-8}
@Service
public class UserServiceImpl implements UserService {

    final javax.validation.Validator validator;

    public UserServiceImpl(javax.validation.Validator validator) {
        this.validator = validator;
    }
    
    // ...
}
```

### STEP 3: 调用 `validate` 方法，校验数据

```java {6-10}
@Service
public class UserServiceImpl implements UserService {
    // ...
    @Override
    public UserDTO create(UserCreateParams params) {
        // violations 为不符合校验规则的错误信息
        var violations = validator.validate(params);
        if (!violations.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, violations);
        }
        // ...
    }
}
```

### STEP 4: 数据的业务逻辑校验

```java {12-15}
@Service
public class UserServiceImpl implements UserService {
    // ...
    @Override
    public UserDTO create(UserCreateParams params) {
        // violations 为不符合校验规则的错误信息
        var violations = validator.validate(params);
        if (!violations.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, violations);
        }

        var exist = repository.findByUsername(params.getUsername());
        if (exist.isPresent()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "用户名已存在");
        }
    }
}
```

## 校验单个参数字段

以 `UserService.changePassword(long id, String password)` 为例，接口参数 `password` 不是参数对象，是单个参数字段。

### 方式一：调用 `javax.validation.Validator.validateValue`

调用 `javax.validation.Validator.validateValue` 校验单个参数，我们可以复用参数对象中的校验规则。

```java {5-8}
@Service
public class UserServiceImpl implements UserService {
    @Override
    public void changePassword(long id, String password) {
        var violations = validator.validateValue(UserCreateParams.class, "password", password);
        if (!violations.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, violations);
        }

        // ...
    }
}
```

### 方式二：调用 `cn.hutool.core.lang.Validator` 的静态校验方法

如不能使用方式一校验数据，优先使用 hutool 类库的 `Validator` 类。如果 hutool 的 `Validator` 类，不能满足，则可以自己写专门的校验方法。

```java {5-7}
@Service
public class UserServiceImpl implements UserService {
    @Override
    public void changePassword(long id, String password) {
        if (Validator.isEmpty(password)) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "密码不能为空");
        }

        // ...
    }
}
```

## 常用校验注解

| 注解 | 描述 |
|------|--------|
| @Email | 校验是否符合Email格式 |
| @Max | 最大值 |
| @Min | 最小值 |
| @NotBlank | 不为null并且包含至少一个非空白字符 |
| @NotEmpty | 不为null并且不为空 |
| @Size | 校验容器的元素个数 |

## 为什么不推荐 Service 的方法参数上加 `@Valid` 注解，来校验数据？

* 使用 `@Valid` 要求在 接口的方法上，以及实现类的方法上加一样的注释，重复加，麻烦。
* 要求在 Service 类上，加 `@Validated` 注解，否则方法上的校验注解不生效。这个很容易忘了加，导致出现安全问题。
* 单元测试的时候，如果需要验证使用 `@Valid` 注解的数据，则需启动 Spring 容器，导致单元测试执行慢。

## 参考

* [Complete Guide to Validation With Spring Boot](https://reflectoring.io/bean-validation-with-spring-boot/)