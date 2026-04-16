# MarryLink App - 婚礼管理系统移动端

## 项目简介

MarryLink App 是一个基于 uni-app 框架开发的婚礼管理系统移动端应用，为新人提供便捷的婚礼主持人预约、问卷填写等服务。

## 技术栈

- **框架**: uni-app (Vue 2)
- **状态管理**: Vuex
- **UI 风格**: 蓝白主题 (#1d4ed8)
- **构建工具**: HBuilderX / Vue CLI

## 项目结构

```
marrylink-app/
├── pages/                    # 页面目录
│   ├── index/               # 首页
│   │   └── index.vue
│   ├── host/                # 主持人模块
│   │   ├── index.vue        # 主持人列表
│   │   └── detail.vue       # 主持人详情
│   ├── questionnaire/       # 问卷模块
│   │   ├── index.vue        # 问卷列表
│   │   └── fill.vue         # 填写问卷
│   ├── mine/                # 我的模块
│   │   └── index.vue
│   ├── login/               # 登录
│   │   └── index.vue
│   └── register/            # 注册
│       └── index.vue
├── api/                     # API 接口
│   ├── user.js             # 用户相关
│   ├── host.js             # 主持人相关
│   └── questionnaire.js    # 问卷相关
├── store/                   # Vuex 状态管理
│   ├── index.js
│   └── modules/
│       ├── user.js         # 用户状态
│       └── app.js          # 应用状态
├── utils/                   # 工具函数
│   └── request.js          # 请求封装
├── styles/                  # 全局样式
│   └── common.scss
├── static/                  # 静态资源
│   └── tabbar/             # 底部导航图标
├── App.vue                  # 应用入口
├── main.js                  # 主入口文件
├── manifest.json            # 应用配置
├── pages.json              # 页面路由配置
└── package.json            # 依赖配置
```

## 核心功能

### 1. 用户认证
- ✅ 用户注册
- ✅ 用户登录
- ✅ 自动登录（Token 持久化）
- ✅ 退出登录

### 2. 首页
- ✅ 轮播图展示
- ✅ 快捷入口（找主持人、填写问卷、我的预约、客服咨询）
- ✅ 推荐主持人列表
- ✅ 精彩案例展示

### 3. 主持人模块
- ✅ 主持人列表（支持搜索、标签筛选、价格筛选）
- ✅ 主持人详情（个人简介、服务特色、案例展示、用户评价）
- ✅ 立即预约功能
- ✅ 咨询功能

### 4. 问卷模块
- ✅ 问卷列表展示
- ✅ 问卷填写（支持单选、多选、文本、日期、时间等题型）
- ✅ 我的提交记录
- ✅ 表单验证

### 5. 我的模块
- ✅ 个人信息展示
- ✅ 预约订单管理（待确认、已确认、已完成、已取消）
- ✅ 我的问卷
- ✅ 我的收藏
- ✅ 设置
- ✅ 帮助与反馈

## 底部导航

应用包含 4 个主要 Tab 页面：

1. **首页** - 展示推荐内容和快捷入口
2. **主持人** - 浏览和搜索主持人
3. **问卷** - 查看和填写问卷
4. **我的** - 个人中心和订单管理

## API 配置

后端 API 地址配置在 [`utils/request.js`](utils/request.js:3) 中：

```javascript
const BASE_URL = 'http://localhost:8080/api'
```

请根据实际后端地址进行修改。

## 主题配置

应用采用蓝白主题，主色调为 `#1d4ed8`（Wedding Blue）。

主题变量定义在 [`styles/common.scss`](styles/common.scss:3) 中：

```scss
$primary-color: #1d4ed8;
$bg-color: #ffffff;
```

## 开发指南

### 环境要求

- Node.js 12+
- HBuilderX 或 Vue CLI

### 安装依赖

```bash
npm install
```

### 运行项目

#### H5 端
```bash
npm run dev:h5
```

#### 微信小程序
```bash
npm run dev:mp-weixin
```

#### App 端
```bash
npm run dev:app-plus
```

### 构建项目

#### H5 端
```bash
npm run build:h5
```

#### 微信小程序
```bash
npm run build:mp-weixin
```

#### App 端
```bash
npm run build:app-plus
```

## 注意事项

### 1. 图标资源

底部导航栏需要准备以下图标（放置在 `static/tabbar/` 目录）：

- `home.png` / `home-active.png` - 首页图标
- `host.png` / `host-active.png` - 主持人图标
- `questionnaire.png` / `questionnaire-active.png` - 问卷图标
- `mine.png` / `mine-active.png` - 我的图标

建议尺寸：81px × 81px（@3x）

### 2. 其他静态资源

需要准备的其他图片资源：

- `logo.png` - 应用 Logo
- `default-avatar.png` - 默认头像
- `banner1.jpg`, `banner2.jpg`, `banner3.jpg` - 首页轮播图
- `case1.jpg` ~ `case4.jpg` - 案例展示图片

### 3. 后端接口对接

确保后端 API 接口已经实现并可访问。主要接口包括：

- 用户认证：`/user/login`, `/user/register`, `/user/info`
- 主持人：`/host/list`, `/host/detail/{id}`, `/host/search`
- 问卷：`/questionnaire/list`, `/questionnaire/detail/{id}`, `/questionnaire/submit`
- 订单：`/order/create`, `/order/list`

### 4. 权限配置

微信小程序需要在 `manifest.json` 中配置相应的权限和域名白名单。

## 后续优化

- [ ] 添加图片上传功能
- [ ] 实现订单详情页面
- [ ] 添加支付功能
- [ ] 实现消息通知
- [ ] 优化加载性能
- [ ] 添加骨架屏
- [ ] 实现下拉刷新和上拉加载
- [ ] 添加分享功能

## 相关项目

- [marrylink-admin](../marrylink-admin) - 后端管理系统
- [marrylink-web](../marrylink-web) - Web 管理端

## 联系方式

如有问题，请联系开发团队。

## 许可证

Copyright © 2024 MarryLink Team