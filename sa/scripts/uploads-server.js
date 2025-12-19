// 简单的静态文件网关（Windows原生，不使用容器）
// 将 /uploads/** 映射到本机绝对路径 D:\project\scholorAssist\scholorAssist\uploads
// 端口：10180

import express from 'express'
import compression from 'compression'
import cors from 'cors'
import path from 'path'

const app = express()
const PORT = 10180
// 你的真实上传根目录（保持与后端返回路径一致）
const UPLOADS_ROOT = path.resolve('D:/project/scholorAssist/scholorAssist/uploads')

// 基础中间件
app.use(compression())
app.use(cors({ origin: '*', methods: ['GET', 'HEAD', 'OPTIONS'] }))

// 健康检查
app.get('/health', (req, res) => {
  res.type('text/plain').send('gateway healthy\n')
})

// 静态文件映射：/uploads/** -> UPLOADS_ROOT/**
app.use('/uploads', express.static(UPLOADS_ROOT, {
  immutable: true,
  maxAge: '30d',
  fallthrough: true,
  // 强制PDF以内嵌方式呈现，避免浏览器触发下载
  setHeaders: (res, filePath) => {
    const lower = filePath.toLowerCase()
    if (lower.endsWith('.pdf')) {
      res.setHeader('Content-Type', 'application/pdf')
      res.setHeader('Content-Disposition', `inline; filename="${path.basename(filePath)}"`)
      res.setHeader('Accept-Ranges', 'bytes')
    }
  }
}))

// 404处理（只针对静态资源未命中）
app.use((req, res) => {
  res.status(404).type('text/plain').send('Not Found')
})

app.listen(PORT, () => {
  console.log(`Static uploads server running at http://localhost:${PORT}/`)
  console.log(`Mapping: /uploads -> ${UPLOADS_ROOT}`)
})