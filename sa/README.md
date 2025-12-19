# 文献辅助阅读系统

智能PDF阅读与AI问答平台

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **开发语言**: TypeScript
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **构建工具**: Vite
- **样式**: SCSS + CSS Variables
- **图标库**: Element Plus Icons

## 功能特性

- 📚 PDF文档上传与管理
- 🗂️ 多级分类管理
- 📝 智能笔记系统
- 🤖 AI问答助手
- 📊 阅读统计分析
- 🎨 响应式设计
- 🌙 深色模式支持

## 开发环境

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview

# 运行测试
npm run test

# 代码检查
npm run lint

# 类型检查
npm run type-check
```

## 项目结构

```
src/
├── api/                 # API接口
│   ├── mockManager.ts   # Mock数据管理
│   ├── DocumentApi.ts   # 文档相关API
│   └── CategoryApi.ts   # 分类相关API
├── components/          # 通用组件
│   ├── Layout/          # 布局组件
│   ├── Common/          # 公共组件
│   └── Dashboard/       # 仪表盘组件
├── views/               # 页面组件
├── router/              # 路由配置
├── store/               # 状态管理
│   └── modules/         # 状态模块
├── styles/              # 全局样式
└── assets/              # 静态资源
```

## 开发规范

- 使用 TypeScript 进行类型安全开发
- 遵循 Vue 3 Composition API 最佳实践
- 使用 Pinia 进行状态管理
- 所有API接口提供Mock实现
- 组件采用单文件组件(SFC)格式
- 样式使用SCSS预处理器

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 许可证

MIT License