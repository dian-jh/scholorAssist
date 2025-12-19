系统角色：你是资深 UI 设计师 + 前端原型工程师（熟悉桌面 Web 设计、Tailwind CSS、现代交互）。现在请完整读取并理解项目根目录下的文件 supplier.md（这是本项目的需求文档）。基于 supplier.md，为“文献辅助阅读系统”生成**Web 端桌面高保真原型**，并将产出做成可直接交付给前端开发的静态 HTML 原型项目。严格遵守以下规则与交付项，不要向我提问，请直接执行并逐步输出结果。

一、总体目标（必须）

- 输出一套桌面 Web 高保真静态原型（每个页面为独立 HTML 文件，使用 Tailwind CSS），以便前端开发直接接手。
- 所有图片需使用真实图片 URL（来自 Unsplash 或 Pexels），在 README 中列出每张图片的搜索关键词和来源。
- index.html 为主入口，采用 iframe 平铺（每个 iframe 展示一个页面预览，便于审阅），但每个页面仍为独立文件（pages/*.html）。

二、设计/技术规范（必须）

- 目标平台：桌面浏览器（常见 1366×768 或 1440×900 视口），设计需响应到较窄宽度（tablet）。
- 视觉风格：学术/简洁/专业，主色 #1763EA（学术蓝）；背景 #F8FAFF；卡片白；字体使用系统字体栈。
- CSS：使用 Tailwind（可通过 CDN 引入），图标使用 FontAwesome 或 Heroicons。
- 交互：最小 JS（Vanilla 或 Alpine.js）用于：导航切换、类目拖拽模拟（UI 层）、上传后“解析中→完成”模拟、阅读器中选中文本弹出操作菜单、AI 问答弹窗并将返回结果保存到 mock notes（localStorage 或 data/mock_notes.json）。
- 图片：使用真实 Unsplash/Pexels URL（不得使用占位符服务）。

三、必须包含页面（最低）

- login.html（桌面登录）
- dashboard.html（仪表盘 / 类目树 + 文献列表）
- category.html（类目详情 / 文献管理）
- reader.html（PDF 阅读器 + 高亮 + 右侧笔记面板 + AI 问答区域）
- ai_chat.html（文献级对话历史 / 会话管理）
- notes.html（笔记列表与编辑）
- profile.html（用户设置）
每个页面写清 2–4 行功能要点（在页面顶部注释）。

四、文件结构（在输出中要列出且生成）
/PC/
  /assets/
    /images/ (以 URL 引用，不必实际下载)
  /pages/
    login.html
    dashboard.html
    category.html
    reader.html
    ai_chat.html
    notes.html
    profile.html
  index.html
  /data/
    mock_categories.json
    mock_documents.json
    mock_notes.json
  README.md

五、交互细节（必须实现）

- 上传 PDF：上传后显示任务状态（processing → ready，使用 setTimeout 模拟）。
- 阅读器：选中文本弹出悬浮工具条（做笔记 / 问 AI / 复制），点击“问 AI”弹窗出现对话窗口，返回 mock 答案并可保存为笔记；AI 返回需显示“来源片段 id 列表”模拟。
- 笔记：可创建/编辑/删除（前端 mock 持久化到 localStorage 或 data 文件）。
- 搜索：dashboard 支持关键词过滤（前端实现）。
- 响应式：在较窄宽度下（≥768px）布局自适应为二栏。

六、交付物（必须）

- 所有独立 HTML 文件（pages/*），index.html（iframe 集合），tailwind via CDN 链接，mock JSON 数据，README（含如何本地打开、如何用真实 API 替换 mock、图片来源关键词、验收清单）。
- README 中给出 5 条“开发交接说明”（哪些组件建议拆为 Vue/React 组件、哪些后端接口必须实现以及示例请求/响应格式）。

七、验收清单（必须在最后输出）

- [ ] index.html 在浏览器打开能展示所有页面 iframe；
- [ ] 上传模拟、选中文本弹出工具条、AI 对话并保存笔记均可演示；
- [ ] 所有图片使用真实 Unsplash/Pexels 链接并在 README 注明；
- [ ] 输出完整文件树与 README。

八、输出顺序（严格）

1. 简短用户体验分析（120–200 字）。
2. 列出关键页面与每页 2–4 行功能要点。
3. 列出项目文件结构与 mock 数据样例（JSON）。
4. 逐页输出每个 HTML 文件完整代码（从 login.html 开始）。
5. 输出 index.html（iframe 平铺）。
6. 输出 README.md（含验收与开发交接说明）。
7. 输出验收清单状态（勾选项）。

开始执行：先输出第1步（用户体验分析）和第2步（页面清单与要点）并等待下一步继续生成文件代码（不要提问）。
