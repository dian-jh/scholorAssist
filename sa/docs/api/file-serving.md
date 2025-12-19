# 文档文件服务实现指南

本文档说明如何在后端提供文档文件访问能力，满足前端读取与下载需求，并保证权限控制与性能。

## 统一接口

- 主接口：`GET /api/documents/{id}/file`
- 行为：返回 PDF 二进制流；支持范围（断点续传）；需要登录态与权限校验。
- 回退：网关静态映射 `/uploads/**` 可作为备用访问方式。

## 存储路径规范

- 推荐目录结构：`<storage_root>/uploads/documents/YYYY/MM/DD/<docId>.pdf`
- 文档表中存储相对路径（以 `/uploads/` 开头），避免硬编码磁盘绝对路径。
- 迁移脚本需统一清洗历史数据：将 `D:\...\uploads\...` 规范化为 `/uploads/...`。

## Spring Boot 示例

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentFileController {
    @Value("${app.storage.root:/data}")
    private String storageRoot;

    private final DocumentService documentService;

    public DocumentFileController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(
            @PathVariable String id,
            @RequestHeader(value = "Range", required = false) String range,
            Principal principal
    ) throws IOException {
        // 1) 权限校验：确认用户对该文档有访问权限
        if (!documentService.canAccess(id, principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        // 2) 查找文件相对路径（以 /uploads/ 开头）
        String relPath = documentService.getRelativePath(id); // e.g. /uploads/documents/2025/11/07/doc_xxx.pdf
        Path filePath = Paths.get(storageRoot, relPath.replaceFirst("^/", ""));
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

        // 3) 返回资源并支持范围
        Resource resource = new FileSystemResource(filePath);
        String contentType = "application/pdf";

        // 简化处理：Spring ContentRange 可使用自定义逻辑或第三方库
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }
}
```

静态资源映射（可选）：

```java
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Value("${app.storage.root:/data}")
    private String storageRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageRoot).toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
```

## Node/Express 示例

```js
import express from 'express'
import fs from 'fs'
import path from 'path'

const app = express()
const storageRoot = process.env.STORAGE_ROOT || '/data'

app.get('/api/documents/:id/file', async (req, res) => {
  const id = req.params.id
  // TODO: 权限校验
  const relPath = await getRelativePathById(id) // '/uploads/documents/2025/11/07/doc_xxx.pdf'
  const filePath = path.join(storageRoot, relPath.replace(/^\//, ''))
  if (!fs.existsSync(filePath)) {
    return res.status(404).json({ code: 404, msg: '文件不存在', data: null })
  }

  const stat = fs.statSync(filePath)
  const range = req.headers.range
  res.setHeader('Accept-Ranges', 'bytes')
  res.setHeader('Content-Type', 'application/pdf')

  if (range) {
    const [startStr, endStr] = range.replace(/bytes=/, '').split('-')
    const start = parseInt(startStr, 10)
    const end = endStr ? parseInt(endStr, 10) : stat.size - 1
    const chunkSize = end - start + 1
    res.writeHead(206, {
      'Content-Range': `bytes ${start}-${end}/${stat.size}`,
      'Content-Length': chunkSize,
      'Content-Type': 'application/pdf'
    })
    fs.createReadStream(filePath, { start, end }).pipe(res)
  } else {
    res.writeHead(200, {
      'Content-Length': stat.size,
      'Content-Type': 'application/pdf'
    })
    fs.createReadStream(filePath).pipe(res)
  }
})
```

## Nginx 网关映射（可选）

```nginx
location /uploads/ {
    alias /data/uploads/;
    expires 7d;
    add_header Cache-Control "public";
}

# 反向代理到后端API
location /api/ {
    proxy_pass http://backend:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 权限与安全

- 所有 `/api/documents/{id}/file` 请求必须校验登录态与文档归属。
- 返回头不应泄露服务器内部路径。
- 建议对下载频率进行限流与审计日志记录。

## 跨域与CORS

- 若前端与后端域不同，需允许 `GET` 与 `HEAD`，并允许 `Authorization` 头与 `Range` 头（如需）。
- 对静态 `/uploads/**` 建议通过同域网关提供，避免复杂的跨域配置。

## 错误码约定

- 404：文件不存在或记录缺失
- 403：无访问权限
- 500：服务器内部错误