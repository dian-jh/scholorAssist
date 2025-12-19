系统角色：你是资深移动 UI 设计师 + 前端原型工程师（熟悉移动 H5、iOS 交互、Tailwind CSS、Vant 风格设计）。请完整读取并理解项目根目录下的 supplier.md（这是需求文档）。基于 supplier.md，为“文献辅助阅读系统”的 H5 移动端输出**高保真原型**（专注移动交互，模拟 iPhone 15 Pro），以便用于原型评审与开发。严格按下列要求执行，不要提问，直接开始输出。

一、总体目标（必须）

- 输出一套移动端高保真静态原型（每个页面独立 HTML 文件，Tailwind CSS 或 Vant 风格，iframe 在 index.html 中平铺展示），模拟 iPhone 15 Pro 视窗（约 393 x 852 CSS px），容器带圆角和顶部 iOS 状态栏。
- 图片必须使用真实 Unsplash/Pexels 链接并在 README 中列出关键词。

二、设计/技术规范（必须）

- 目标设备：iPhone 15 Pro 尺寸模拟（393×852 CSS px），容器圆角 28px。
- 视觉风格：学术/清新/浅色主题，主色 #1763EA，字体使用系统字体栈。
- CSS：Tailwind（CDN 可接受），图标 FontAwesome 或 Heroicons。
- JS：最小 Vanilla JS 或 Alpine.js 用于：底部 Tab 切换、上传进度模拟、选中文本弹出操作、AI 提问弹窗、笔记保存至 localStorage。
- 图片须真实，不使用占位符。

三、必须包含页面（最低）

- m_login.html（移动登录）
- m_home.html（类目树 + 文献列表，卡片式）
- m_reader.html（移动阅读器：文本/页式预览 + 选中操作）
- m_ai_chat.html（聊天式对话界面）
- m_notes.html（笔记列表 / 编辑）
- m_profile.html（用户中心 / 同步设置）

每页顶部写 2–3 行移动端的功能要点（注释）。

四、文件结构
/prototype-h5/
  /assets/
    /images/ (以 URL 引用)
  /pages/
    m_login.html
    m_home.html
    m_reader.html
    m_ai_chat.html
    m_notes.html
    m_profile.html
  index.html
  /data/
    mock_categories.json
    mock_documents.json
    mock_notes.json
  README.md

五、交互细节（必须实现）

- 顶部 iOS 状态栏模拟（时间/信号/电池）；
- 底部 Tab Bar（4个 Tab：Home / Notes / Chat / Profile）；
- 阅读器：支持文本选中（长按或双击模拟），弹出“做笔记 / 问 AI / 分享”浮层（在移动上要适配手势）；
- AI 提问：弹窗式输入，返回 mock 答案（显示消息气泡、可保存为笔记）；
- 同步提示：profile 页面提供“同步到云”按钮（模拟同步进度）。

六、交付物（必须）

- 独立 HTML 文件集，每页含注释，index.html 以 iframe 平铺；
- mock JSON 数据，README（如何打开、替换 API、图片来源关键词）；
- 开发交接说明（说明哪些组件建议做成 React/Vue 组件、后端接口格式示例）。

七、验收清单（必须在最后输出）

- [ ] index.html 能以 iPhone 15 Pro 视窗展示并交互；
- [ ] 选中文本弹出移动适配的操作浮层、AI 问答并保存笔记可演示；
- [ ] 图片使用真实 Unsplash/Pexels 链接并在 README 注明来源；
- [ ] 输出完整文件树、README 与开发交接说明。

八、输出顺序（严格）

1. 输出 120–160 字移动端用户体验分析；
2. 列出关键页面与每页功能要点；
3. 列出项目文件结构与 mock 数据样例（JSON）；
4. 逐页输出 HTML 文件代码（从 m_login.html 开始）；
5. 输出 index.html（iframe 平铺）；
6. 输出 README.md 与验收清单。

现在开始第1步：输出（A）移动端用户体验分析（120–160 字），（B）页面清单与功能要点，（C）文件结构草案，随后继续生成页面代码（不要提问）。
