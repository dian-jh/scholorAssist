# Nginx 静态资源映射配置（PDF可直接访问）

本项目建议由 Nginx 统一对外提供网关端口，并直接服务静态文件（如 PDF）以提升性能与可靠性。以下配置将 `/uploads/**` 映射到磁盘目录。

## 1. 关键配置

已在 `nginx.conf` 中新增一个监听 `10100` 的网关 `server`：

```
server {
  listen 10100;
  server_name localhost;

  # /uploads/** -> /data/uploads/**
  location /uploads/ {
    alias /data/uploads/;
    access_log off;
    expires 30d;
    add_header Cache-Control "public, max-age=2592000, immutable";
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, HEAD, OPTIONS" always;
  }

  location /health {
    access_log off;
    return 200 "gateway healthy\n";
    add_header Content-Type text/plain;
  }
}
```

说明：
- 使用 `alias` 而非 `root`，保证 `/uploads/xxx.pdf` 正确映射到实际磁盘路径 `/data/uploads/xxx.pdf`。
- 开启基础缓存与跨域响应头，便于前端在不同端口直接引用 PDF。

## 2. 容器部署（Linux/Windows）

1) 将宿主机的上传目录挂载到容器 `/data/uploads`（只读）：

Docker Compose（示例，需根据你的实际路径调整）：

```yaml
services:
  frontend:
    # ... 其他配置
    ports:
      - "80:80"        # 前端静态站点
      - "10100:10100"  # 网关（静态PDF）
    volumes:
      - D:/project/scholorAssist/scholorAssist/uploads:/data/uploads:ro  # Windows路径示例
```

注意：
- Windows 路径使用正斜杠：`D:/...`。
- 如果在 Linux，请改为 `/data/your_uploads_dir:/data/uploads:ro`。
- 容器需开放 `10100` 端口（`ports` 增加 `10100:10100`）。

2) 重启容器使配置生效：

```
docker compose up -d --build
```

## 3. Windows 原生 Nginx（不使用容器）

若你直接在 Windows 上运行 Nginx，已提供专用配置文件：`nginx.windows.conf`。

关键点：该配置监听 `10180` 端口，并将 `/uploads/**` 映射到本机绝对路径（默认 `D:/wwwroot/uploads/`）。如你的实际路径不同，请编辑该文件并将 `alias` 修改为你的真实目录（注意使用正斜杠并保留末尾斜杠）。

示例（默认）：

```nginx
server {
  listen 10180;
  server_name localhost;

  location /uploads/ {
    alias D:/wwwroot/uploads/;
    autoindex off;
    access_log off;
    expires 30d;
    add_header Cache-Control "public, max-age=2592000, immutable";
    add_header Access-Control-Allow-Origin "*" always;
    add_header Access-Control-Allow-Methods "GET, HEAD, OPTIONS" always;
    add_header Access-Control-Allow-Headers "*" always;
    try_files $uri $uri/ =404;
  }
}
```

启动方式（确保 `mime.types` 可用）：

```
# 方式一：将 nginx.windows.conf 复制到 Nginx 安装目录的 conf/ 后，直接启动
nginx.exe -c conf/nginx.windows.conf

# 方式二：保留配置在项目目录，指定前缀与配置路径
nginx.exe -p C:\nginx -c D:\project\scholorAssist\sa\nginx.windows.conf

# 停止/重载
nginx.exe -s reload
nginx.exe -s stop
```

## 4. 验证

使用浏览器或命令行验证：

```
curl -I http://localhost:10180/uploads/documents/2025/11/07/doc_1986804291322646528.pdf
```

预期：
- `HTTP/1.1 200 OK`
- `Content-Type: application/pdf`
- 无需后端应用参与（无 Spring Boot 404）

## 5. 前端对接

- 前端应使用 `http://localhost:10100/uploads/...` 作为 PDF 地址。
- 若后端返回的是绝对磁盘路径（如 `D:\...\uploads\2025\...\doc_xxx.pdf`），前端已修正逻辑，提取并保留 `/uploads/` 前缀后拼接到网关域名。

## 6. 常见问题

- 404：检查文件是否存在、路径大小写、`alias` 指向是否正确、是否挂载到 `/data/uploads`。
- 跨域：若前端端口不同，确保已有 `Access-Control-Allow-Origin: *`（或改为你的域名）。
- 大文件下载慢：考虑启用 `sendfile on; tcp_nopush on;`（已在全局开启），或采用 CDN/对象存储。

## 7. 安全提示

- 如需鉴权下载，建议将 `/uploads/` 改为受控下载接口，由 Nginx 验证签名或由网关后端发放一次性URL，再反代真实文件。