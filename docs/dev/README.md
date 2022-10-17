# 开发指南

## 原则

### KISS 原则

KISS 全称是 Keep it Simple, Stupid，用最直白的话说，“简单就是美”。可以应用在很多场景中，它不仅经常用来指导软件开发，还经常用在架构设计、产品设计等。

在架构设计层面：KISS 的“简单”，强调的是易实施性。让模块容易实现，实现的时候心智负担低。

在代码实现层面，代码的可读性和可维护性是衡量代码质量非常重要的两个标准。而 KISS 原则就是保持代码可读和可维护的重要手段。代码足够简单，也就意味着很容易读懂，bug 比较难隐藏。即便出现 bug，修复起来也比较简单。

具体可以：
* **不要使用同事可能不懂的技术来实现代码。** 比如复杂的正则表达式，还有一些编程语言中过于高级的语法等。
* **不要重复造轮子，要善于使用已经有的工具类库。** 经验证明，自己去实现这些类库，出 bug 的概率会更高，维护的成本也比较高。
* **不要过度优化。** 不要过度使用一些奇技淫巧（比如，位运算代替算术运算、复杂的条件语句代替 if-else、使用一些过于底层的函数等）来优化代码，牺牲代码的可读性。

## 下载源码

```
git clone git@coding.codeages.work:techbase/java-skeleton-project.git
```

项目使用 Monorepo 策略管理源码，整个项目只有一个 Git 仓库。这种代码的管理方式的优势有：

* **代码重用将变得非常容易：** 由于所有的项目代码都集中于一个代码仓库，我们将很容易抽离出各个项目共用的业务组件或工具；
* **依赖管理将变得非常简单：** 同理，由于项目之间的引用路径内化在同一个仓库之中，我们很容易追踪当某个项目的代码修改后，会影响到其他哪些项目。通过使用一些工具，我们将很容易地做到版本依赖管理和版本号自动升级；
* **代码重构将变得非常便捷：** 想想究竟是什么在阻止您进行代码重构，很多时候，原因来自于「不确定性」，您不确定对某个项目的修改是否对于其他项目而言是「致命的」，出于对未知的恐惧，您会倾向于不重构代码，这将导致整个项目代码的腐烂度会以惊人的速度增长。而在 monorepo 策略的指导下，您能够明确知道您的代码的影响范围，并且能够对被影响的项目可以进行统一的测试，这会鼓励您不断优化代码；
* **它倡导了一种开放，透明，共享的组织文化，这有利于开发者成长，代码质量的提升：** 在 monorepo 策略下，每个开发者都被鼓励去查看，修改他人的代码（只要有必要），同时，也会激起开发者维护代码，和编写单元测试的责任心（毕竟朋友来访之前，我们从不介意自己的房子究竟有多乱），这将会形成一种良性的技术氛围，从而保障整个组织的代码质量。

引用自： https://segmentfault.com/a/1190000039157365

## 目录结构

```
|- server   # 后端 API 项目目录
|- web      # Web 前端项目目录
|- conf     # 项目开发、测试、生产环境配置
|- docs     # 项目文档
|- scripts  # 项目构建发布脚本
```

## 后端

### 快速入门

* Java
  * [Var](https://www.infoq.cn/article/java-10-var-type-inference)
  * [Optional](http://ckjava.com/2019/06/22/understand-Java-8-Optional/)
* [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/index.html) ： 快速浏览 Getting Started、Using Spring Boot、 Spring Boot Features 这三部分文档。
* [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/2.4.7/reference/html/) ：数据库的相关操作使用了 Spring Data JAP。快速浏览官方文档，了解基础用法。
* [Flyway](https://flywaydb.org/documentation/) ： 数据库版本控制。快速浏览官方文档，掌握基础用法。
* [MapStruct](https://mapstruct.org/documentation/stable/reference/html/) : 用于 VO / DTO / Entity 对象之间自动映射字段。 快速浏览官方文档的几个实例，了解基础用法。
* [Hutool](https://hutool.cn/) ：工具集。需了解类库有哪些能力，日常开发中使用该工具集能事半功倍。
* [请求参数注解 `@RequestParam`](https://www.baeldung.com/spring-request-param)
* [分页](https://www.baeldung.com/spring-data-web-support#the-pageablehandlermethodargumentresolver-class)

### 应用架构

采用经典三层应用架构。

![](../images/spring-web-app-architecture.png)

* **Web层**是Web应用程序的最上层。它负责处理用户的输入并将正确的响应返回给用户。Web层还必须处理其他层引发的异常。因为Web层是我们应用程序的入口点，所以它必须注意身份验证并充当防范未授权用户的第一道防线。
* **服务层**位于Web层下方。负责实现业务逻辑。
* **存储层**是Web应用程序的最低层。它负责与使用的数据存储进行通信。

我们真的需要 DTO 吗？为什么我们不能 Entity 返回到 Web 层？

* 域模型指定了我们应用程序的内部模型。如果我们将此模型暴露给外界，则客户将不得不知道如何使用它。换句话说，我们的应用程序的客户将不得不照顾不属于他们的东西。如果使用DTO，则可以向应用程序的客户端隐藏此模型，并提供更简单，更简洁的API。
* 如果我们将领域模型暴露给外部世界，那么我们在不破坏依赖于它的其他内容的情况下就无法对其进行更改。如果我们使用DTO，只要不对DTO进行任何更改，就可以更改域模型。

参考：[Understanding Spring Web Application Architecture: The Classic Way](https://www.petrikainulainen.net/software-development/design/understanding-spring-web-application-architecture-the-classic-way/)

### 集成框架类库

* [数据存储层：Spring Data Jpa、QueryDSL]()
* [登录验证：Spring Security]()
* [数据库 Migration: Flyway]()
* [数据校验]()
* [任务调度：Quartz]()
* [消息队列：RocketMQ]()
* [缓存：Redis]()
* [对象字段映射: MapStruct]()
* [通用工具类库: Hutool]()
* [第三方 REST API 请求的 Client 封装 (TODO)]()
* [日志格式输出：Elastic Common Schema (TODO)]()
* [单元测试]()
* [API 开发测试：IDEA HTTP Test]()
* [代码质量检测：Sonar - TODO]()
* [CI: Gitlab - TODO]()

### 开发 IDE

在 `IntelliJ IDEA` IDE 中，打开 `java-skeleton-project/server` 目录。

::: tip
建议使用 IntelliJ IDEA。VSCode 装上 Java 插件后能开发，但使用重构等功能时，有些不太好使。
:::

### API 风格

* 接口命名采用 HTTP RPC 的命名方式，而非 REST 风格。
* 只使用 GET / POST。
* 错误码使用有意义的字母，而非数字。例如：`NOT_FOUND`, `INVALID_ARGUMENT`。

### API 开发测试

见 `server/src/test/*.http` 文件。

## 前端

### 快速入门

* [ES6 教程](https://wangdoc.com/es6/)： 
  * [let 和 const 命令](https://wangdoc.com/es6/let.html)
  * [Promise 对象](https://wangdoc.com/es6/promise.html)
  * [async 函数](https://wangdoc.com/es6/async.html)
  * [Class 的基本语法](https://wangdoc.com/es6/class.html)
  * [Module 的语法](https://wangdoc.com/es6/module.html)
* [Vue3 官方教程](https://vue3js.cn/docs/zh/guide/introduction.html) ： 学习 基础、深入组件、高阶指南(响应性、组合式 API)。
* [Vue Router](https://router.vuejs.org/zh/) ： 学习 指南 这部分文档，快速过一遍就行。
* [axios](https://github.com/axios/axios) ： 前端的 HTTP Client。浏览下官方的 README 。
* [Lodash](https://www.lodashjs.com/) ： 前端的工具库。浏览下工具库提供了哪些能力，开发时用上这些工具库能事半功倍。
* [Day.js](https://day.js.org/zh-CN/) ： Day.js 是一个轻量的处理时间和日期的 JavaScript 库。浏览下官方文档。
* [tiny-emitter](https://github.com/scottcorgan/tiny-emitter) ：超简单的事件派发器。浏览下官方 README。
* [Ant Design Vue](https://2x.antdv.com/components/overview-cn/) ：Ant Design Vue 是面向桌面PC端的UI组件库。浏览下有哪些组件，体验下组件的功能。
* [Vant](https://vant-contrib.gitee.io/vant/v3/#/zh-CN) ： Vant 是面向移动端的 UI 组件库。浏览下有哪些组件，体验下组件的功能。
* [UI设计规范](https://ant.design/docs/spec/introduce-cn)

### 开发 IDE

在 WebStorm IDE 中，打开 `java-skeleton-project/web` 目录。

::: tip
建议使用 WebStorm。VSCode 能用，但使用重构、代码智能提示时，有些不太好使。
:::

### 目录结构

```
|- web
    |- admin        # 平台总管理后台
    |- pc           # 应用 PC 端
    |- h5           # 应用 H5 端
    |- shared       # 前端共享代码目录
```

### 开发 Step By Step

以开发 `admin` 为例。

#### 启动前端开发服务

```
cd web/admin
yarn serve
```

#### 启动后端服务

在 `IntelliJ IDEA` 中启动服务。 或者在前端项目模流下新增 `.env.local` 将后端 API 服务代理到测试环境：

```
VUE_APP_PROXY_FOR_API=http://skeleton-dev.edusoho.cn
```

#### 打开页面

```
http://localhost:8101/admin/
```

初始管理后台账号：`admin`，密码：`admin`。