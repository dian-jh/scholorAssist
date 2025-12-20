实现 Zotero 风格的笔记（即：高亮 + 关联笔记 + 侧边栏索引）在前端实现是完全可行的，且不需要复杂的后端修改。后端目前的 Map<String, Object> positionInfo 和 selectedText 字段完全足够承载这些数据。

核心思路是：后端只负责存“位置数据块”，前端负责“计算坐标”和“渲染图层”。

以下是具体的前端优先修改方案，主要针对 src/components/Reader 目录下的文件。

1. 定义数据结构（前端约定）
既然后端是 Map，我们在前端定义一个清晰的接口，直接存入 positionInfo。

在 src/types/note.d.ts (或者新建一个 types 文件) 中添加：

TypeScript

// Zotero 风格的高亮数据结构
export interface HighlightPosition {
  pageNumber: number; // 关联的页码 (1-based)
  rects: Array<{      // 支持多行高亮，所以是数组
    x: number;        // PDF 坐标系下的 x (比率或点)
    y: number;        // PDF 坐标系下的 y
    width: number;
    height: number;
  }>;
  color?: string;     // 高亮颜色，如 "#ffeb3b" (黄)
}

// 扩展现有的 Note 类型（如果需要）
export interface NoteWithPosition extends Note {
  positionInfo: HighlightPosition;
}
2. 核心逻辑：获取选中文本的 PDF 坐标
这是最关键的一步。Zotero 的体验在于选中文本后，高亮位置是跟随 PDF 内容的，而不是屏幕像素。

你需要修改 ModernPdfReader.vue (或你实际使用的 PDF 阅读器组件)，监听文本层的鼠标抬起事件。

在 ModernPdfReader.vue 或 useDocumentData.ts 中添加此工具函数：

TypeScript

import * as pdfjsLib from 'pdfjs-dist';

/**
 * 将浏览器的 DOM 选区转换为 PDF 页面内的标准坐标
 * @param selection window.getSelection() 对象
 * @param pageElement PDF 页面的 DOM 容器 (.page)
 * @param viewport PDF.js 的 viewport 对象
 */
export const getSelectionCoords = (
  selection: Selection,
  pageElement: HTMLElement,
  viewport: pdfjsLib.PageViewport
): HighlightPosition['rects'] | null => {
  if (!selection.rangeCount) return null;

  const range = selection.getRangeAt(0);
  const clientRects = range.getClientRects(); // 获取所有选中行的屏幕坐标
  const pageRect = pageElement.getBoundingClientRect(); // 获取 PDF 页面的屏幕坐标

  const pdfRects = [];

  for (const rect of clientRects) {
    // 1. 计算相对于 PDF 页面左上角的像素坐标
    const x = rect.left - pageRect.left;
    const y = rect.top - pageRect.top;

    // 2. 使用 PDF.js 的 viewport 转换方法将像素转为 PDF 点坐标 (Points)
    // 注意：convertToPdfPoint 返回的是 [x, y]，且 PDF 坐标原点通常在左下角，
    // 但 PDF.js 的 convertToPdfPoint 会帮你处理好变换矩阵。
    // 我们需要分别转换左上角和右下角来确定矩形
    const [x1, y1] = viewport.convertToPdfPoint(x, y);
    const [x2, y2] = viewport.convertToPdfPoint(x + rect.width, y + rect.height);
    
    // 3. 规范化矩形数据 (PDF 坐标系)
    // 注意：视口不同，y1 和 y2 的大小关系可能不同，需取绝对值
    pdfRects.push({
      x: Math.min(x1, x2),
      y: Math.min(y1, y2), // PDF 坐标系下，通常 y 越小越靠下，但这里我们存标准数学坐标
      width: Math.abs(x1 - x2),
      height: Math.abs(y1 - y2)
    });
  }

  return pdfRects.length > 0 ? pdfRects : null;
};
3. 交互实现：弹出 Zotero 风格气泡
修改 src/components/Reader/ActionBubble.vue 或主阅读器页面，当监测到 mouseup 时：

TypeScript

// 在 ModernPdfReader.vue 的 script setup 中

const onTextLayerMouseUp = (event: MouseEvent, pageNumber: number) => {
  const selection = window.getSelection();
  if (!selection || selection.isCollapsed) {
    showActionBubble.value = false; // 隐藏气泡
    return;
  }

  // 1. 获取当前页面的 DOM 元素和 Viewport (假设你存了 viewport 引用)
  const pageContainer = document.querySelector(`.page[data-page-number="${pageNumber}"]`);
  const viewport = pageViewports.value[pageNumber]; 

  if (pageContainer && viewport) {
    // 2. 计算坐标
    const rects = getSelectionCoords(selection, pageContainer as HTMLElement, viewport);
    
    if (rects) {
      // 3. 暂存这个位置信息，准备创建笔记
      currentSelection.value = {
        text: selection.toString(),
        position: {
            pageNumber,
            rects
        }
      };

      // 4. 显示操作气泡 (Zotero 风格：位置跟随鼠标)
      bubblePosition.value = { x: event.clientX, y: event.clientY };
      showActionBubble.value = true;
    }
  }
};
4. 渲染层：在 PDF 上绘制高亮
Zotero 的高亮本质上是在 PDF Canvas 上面盖了一层透明的 div。你需要创建一个专门的“高亮层组件” HighlightLayer.vue，或者直接在 ModernPdfReader.vue 的 v-for 循环中渲染。

修改 ModernPdfReader.vue 的 Template：

HTML

<div class="pdf-page-container" v-for="page in pages" :key="page.pageNumber">
    
    <canvas ref="canvasRefs" ... />

    <div class="textLayer" @mouseup="(e) => onTextLayerMouseUp(e, page.pageNumber)">...</div>

    <div class="highlight-layer">
        <template v-for="note in getNotesForPage(page.pageNumber)" :key="note.id">
            <div 
                v-for="(rect, idx) in note.positionInfo.rects" 
                :key="idx"
                class="highlight-rect"
                :style="getHighlightStyle(rect, page.viewport, note.style.color)"
                @click="onHighlightClick(note)"
            ></div>
        </template>
    </div>

</div>
对应的样式计算函数 (getHighlightStyle)：

这步是将数据库里的 PDF 坐标 反向转换 为当前屏幕像素。

TypeScript

const getHighlightStyle = (pdfRect, viewport, color = '#ffeb3b') => {
  // 使用 PDF.js viewport 将 PDF 坐标转回屏幕像素
  const [x, y, w, h] = viewport.convertToViewportRectangle([
    pdfRect.x, 
    pdfRect.y, 
    pdfRect.x + pdfRect.width, 
    pdfRect.y + pdfRect.height
  ]);

  // 注意：convertToViewportRectangle 返回的是 [xMin, yMin, xMax, yMax]
  // 需要转换成 CSS 的 left/top/width/height
  
  return {
    position: 'absolute',
    left: `${Math.min(x, w)}px`,   // 并不是简单的 x, w，而是 rect 的边界
    top: `${Math.min(y, h)}px`,
    width: `${Math.abs(x - w)}px`,
    height: `${Math.abs(y - h)}px`,
    backgroundColor: color,
    opacity: 0.4, // 半透明效果
    cursor: 'pointer'
  };
};
5. 为什么这样最“接近 Zotero”？
数据分离：Zotero 的高亮和笔记是分离的。你可以在创建笔记时，如果没有内容 (content 为空)，就只渲染为黄色高亮；如果有内容，点击高亮时才弹出笔记卡片。

视觉锚点：通过 rects 数组，即使用户选中了跨栏、跨页（需拆分存储）的文字，前端也能完美渲染出断开的高亮块，而不是一个丑陋的大矩形。

简单后端：后端完全不需要懂 PDF 坐标，它只需要把这个 JSON 存进去，读出来即可。