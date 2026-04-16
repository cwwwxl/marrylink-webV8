# 登录后空白页面问题修复

## 问题描述
登录成功后，访问 `http://localhost:3000/#/dashboard` 显示空白页面，没有任何内容展示。

## 根本原因
后端 [`MenuController`](marrylink-admin/backend/src/main/java/com/marrylink/controller/MenuController.java) 在判断用户角色时使用了错误的角色名称格式。

### 详细分析

1. **JWT Token 中的角色格式**：
   - 在 [`JwtAuthenticationFilter`](marrylink-admin/backend/src/main/java/com/marrylink/security/JwtAuthenticationFilter.java:75) 第75行，从 Token 中提取的 `roles` 列表存储到 request 属性中
   - 这些角色值是**不带前缀**的，例如：`["ADMIN"]` 或 `["HOST"]`

2. **Spring Security 权限格式**：
   - 在第59行构建 Spring Security 权限时，会添加 `ROLE_` 前缀
   - 例如：`ADMIN` → `ROLE_ADMIN`

3. **MenuController 的错误判断**：
   - 原代码在第61行和第66行检查 `roles.contains("ROLE_ADMIN")`
   - 但 request 属性中的 roles 是不带前缀的，所以永远匹配不到
   - 导致返回的路由列表为空或只有基础路由

## 修复方案

### 1. 修复后端 MenuController（已完成）

**文件**：[`marrylink-admin/backend/src/main/java/com/marrylink/controller/MenuController.java`](marrylink-admin/backend/src/main/java/com/marrylink/controller/MenuController.java:61)

**修改前**：
```java
// 管理员专属菜单
if (roles != null && roles.contains("ROLE_ADMIN")) {
    children.add(createRoute("user", "marrylink/user/index", "UserManage", "用户管理", "user"));
}

// 管理员和主持人共享菜单
if (roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_HOST"))) {
    children.add(createRoute("host", "marrylink/host/index", "HostManage", "主持人管理", "user"));
    children.add(createRoute("order", "marrylink/order/index", "OrderManage", "订单管理", "document"));
    children.add(createRoute("questionnaire", "marrylink/questionnaire/index", "QuestionnaireManage", "问卷管理", "document"));
}
```

**修改后**：
```java
// 管理员专属菜单
if (roles != null && roles.contains("ADMIN")) {
    children.add(createRoute("user", "marrylink/user/index", "UserManage", "用户管理", "user"));
}

// 管理员和主持人共享菜单
if (roles != null && (roles.contains("ADMIN") || roles.contains("HOST"))) {
    children.add(createRoute("host", "marrylink/host/index", "HostManage", "主持人管理", "user"));
    children.add(createRoute("order", "marrylink/order/index", "OrderManage", "订单管理", "document"));
    children.add(createRoute("questionnaire", "marrylink/questionnaire/index", "QuestionnaireManage", "问卷管理", "document"));
}
```

### 2. 添加前端调试日志（已完成）

**文件**：[`marrylink-web/src/plugins/permission.ts`](marrylink-web/src/plugins/permission.ts:46)

在权限守卫中添加了调试日志，方便排查问题：
```typescript
console.log("Fetching user info...");
await userStore.getUserInfo();
console.log("User info fetched:", userStore.user);

console.log("Generating routes for roles:", userStore.user.roles);
const dynamicRoutes = await permissionStore.generateRoutes();
console.log("Dynamic routes generated:", dynamicRoutes);
```

## 测试步骤

1. **重启后端服务**：
   ```bash
   cd marrylink-admin/backend
   mvn clean package -DskipTests
   java -jar target/marrylink-admin-1.0.0.jar
   ```

2. **启动前端服务**（如果未运行）：
   ```bash
   cd marrylink-web
   npm run dev
   ```

3. **测试登录**：
   - 访问 `http://localhost:3000`
   - 使用管理员账号登录（例如：admin/admin123）
   - 登录成功后应该能看到完整的菜单和控制台页面

4. **检查浏览器控制台**：
   - 打开浏览器开发者工具（F12）
   - 查看 Console 标签页
   - 应该能看到类似以下的日志：
     ```
     Generating routes for roles: ["ADMIN"]
     Dynamic routes generated: [...]
     ```

## 预期结果

登录成功后：
- 左侧应显示完整的菜单结构
- 主内容区域应显示控制台页面
- 管理员用户应该能看到：
  - 婚礼智配
    - 控制台
    - 用户管理
    - 主持人管理
    - 订单管理
    - 问卷管理

## 注意事项

1. **角色命名规范**：
   - JWT Token 中存储的角色：`ADMIN`, `HOST`, `COUPLE`（不带前缀）
   - Spring