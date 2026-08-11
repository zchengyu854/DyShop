# dyshop-backend

dyshop 购物程序后端（前后端分离架构中的后端工程，独立构建部署）。

## 技术栈

Java 17 · Spring Boot 3.3.x · Spring Security (JWT) · MyBatis-Plus · MySQL 8 · Maven（多模块）

## 模块结构

```
backend/
├── pom.xml                  # 父 POM：聚合 + 依赖版本管理
├── dyshop-common/           # 通用基础模块（库，不可独立启动）
│   └── src/main/java/com/dyshop/common/
│       ├── entity/          # 实体类（与数据库表对应）
│       ├── dto/             # 通用数据传输对象
│       ├── vo/              # 通用视图对象
│       ├── result/          # 统一返回体 Result / ResultCode
│       ├── exception/       # 统一异常体系 + 全局异常处理器
│       ├── constant/        # 常量（角色、订单状态等）
│       ├── util/            # 工具类（JwtUtil 等）
│       └── config/          # 通用配置（MyBatis-Plus 分页、Jackson 等）
├── dyshop-api/              # C 端用户接口模块（端口 8081，可独立启动）
│   └── src/main/java/com/dyshop/api/
│       ├── DyshopApiApplication.java
│       ├── controller/      # 用户端接口：auth / product / cart / order / address
│       ├── service/         # 业务逻辑层
│       ├── mapper/          # MyBatis-Plus Mapper
│       ├── dto/             # 请求体
│       ├── vo/              # 响应体
│       └── config/          # SecurityConfig(JWT) / WebConfig 等
└── sql/
    └── schema.sql           # 数据库建库建表脚本
```

> 说明：后台管理接口与 C 端同属 `dyshop-api` 模块（路径 `/api/admin/**`，端口 8081）；
> 历史曾规划独立 `dyshop-admin` 模块（8082），实为空壳无任何 controller，已删除。

## 功能模块规划

| 模块 | 归属 | 说明 |
|---|---|---|
| 用户注册/登录 | api | JWT 无状态认证 |
| 商品浏览 | api | 商品列表（分页/搜索）、详情 |
| 购物车 | api | 加入/修改/删除/结算 |
| 下单 | api | 下单 → 待支付（模拟支付接口）→ 待发货 |
| 订单管理 | api | 订单列表/详情/取消 |
| 收货地址 | api | 地址 CRUD |
| 商品管理 | admin | 商品 CRUD、上下架、库存 |
| 订单管理 | admin | 订单查询、发货、关闭 |

## 快速开始

```bash
# 首次：下载依赖并安装 common 到本地仓库
./mvnw clean install -DskipTests

# C 端接口 → http://localhost:8081
./mvnw -pl dyshop-api spring-boot:run
```

依赖 MySQL：先在本地 MySQL 8 执行 `sql/schema.sql` 建库建表，再执行 `sql/data.sql`
写入种子数据，最后按各模块 `application-dev.yml` 修改连接配置。

## 约定

- 包名根：`com.dyshop`
- 统一返回体：`Result<T>`，错误码见 `ResultCode`
- 认证：请求头 `Authorization: Bearer <token>`；admin 接口需要管理员角色
- 端口：8081（C 端 + 后台管理）
- 前端对接：见 `../frontend/README.md`（`/api` → 8081）

> 当前为架构骨架阶段：分层包已建好，业务代码后续填充。
