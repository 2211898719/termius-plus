# API 风格

## URI 命名

接口 URI 由 3 部分组成：`/{prefix}/{service}/{method}` 。

* `/{prefix}`: API 的前缀。一种前缀，代表面向一种场景的 API。 比如一款 2B 的产品，平台开发方管理后台的 API 可以命名为 `/api/console`，企业操作界面的 API 可以命名为： `/api/corp`。如果整个应用只有一种场景，那么可以命名为 `/api`。
* `/{service}`: 接口服务名。
* `/{method}`: 接口方法名。

例如：
* `/api/console/log/search`：平台管理后台，日志查询接口
* `/api/console/corp/create`: 平台管理后台，企业创建接口
* `/api/corp/corp/create`：面向企业的自助注册接口

::: tip 为什么不采用 REST 风格的接口命名？
通常业务层接口按 `某某业务Service.某某业务操作`，这样的命名方式。而 REST 风格的接口命名，要求抽象为资源。导致业务层到接口层，需要进行抽象概念上的转换。
简单业务操作相对比较好转换，遇到复杂业务逻辑，往往 REST 风格的命名，比较难抽象。所以为了降低 API 接口命名的心智负担，API 层的接口命名与业务层的方法命名，保持同样的风格。
:::

## HTTP 动词 

仅使用`GET`、`POST`这两个 HTTP 动词。

* `GET`： 用在查询类接口，不对数据变更；
* `POST`: 用在数据更新类接口。

## 接口鉴权

使用 JWT 作为鉴权令牌。放入 HTTP 的 `Authorization` 头部。 例如：

```
Authorization: Bearer {{JWT}}
```

## 成功响应

单个对象，直接返回对象本身：

```json
{
    "key1": "value1",
    "key2": "key2"
}
```

含分页的查询接口，按以下格式返回：

```json
{
    "data": [
        {"key1": "value1", "key2": "value2"},
        {"key1": "value1", "key2": "value2"},
        {"key1": "value1", "key2": "value2"}
    ],
    "total": 100,
    "page": 2,
    "size": 20
}
```

* `total`: 表示该查询条件下共有 `total` 个对象
* `page`: 表示当前页码
* `size`: 表示每页的对象个数

不含分页的查询接口，按以下格式返回：

```json
[
    {"key1": "value1", "key2": "value2"},
    {"key1": "value1", "key2": "value2"},
    {"key1": "value1", "key2": "value2"}
]
```

## 失败响应

客户端引起的错误 HTTP 状态码设置为 4xx，服务端引起的错误，HTTP 状态码设置为 5xx，并返回以下结构体：

```json
{
    "timestamp": 1618847669123,
    "status": 400,
    "code": "INVALID_ARGUMENT",
    "message": "用户名已存在",
    "path": "/api/request/path",
    "traceId": "this-is-a-trace-id",
    "details": [
        "error detail 1",
        "error detail 2"
    ]
}
```

* `timestamp`: API 请求的时间戳
* `status`: HTTP 状态码
* `code`: 业务错误码
* `message`: 错误原因
* `path`: 错误的请求路径
* `traceId`: 请求的追踪ID
* `details`: 错误的堆栈信息，只在 DEBUG 模式下输出，便于开发调试接口，生产环境请勿输出。

## 业务错误码

使用英文单词来表示错误码。例如：

* `INVALID_ARGUMENT`: 参数不正确
* `ACCESS_DENIED`: 无权访问
* `NOT_FOUND`: 对象不存在

::: tip 为什么使用英文单词而不是使用数字来表示业务错误码？
英文单词比数字具有较高的识别度，不用再去对照错误码表了。
:::
