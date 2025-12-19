# 部署配置文档

## 环境要求

### 开发环境
- Node.js >= 18.0.0
- npm >= 8.0.0 或 yarn >= 1.22.0
- 现代浏览器（Chrome >= 87, Firefox >= 78, Safari >= 14, Edge >= 88）

### 生产环境
- Web服务器（Nginx, Apache, IIS等）
- HTTPS支持（推荐）
- CDN支持（可选）

## 构建部署

### 1. 安装依赖
```bash
npm install
```

### 2. 环境配置
创建环境变量文件：

**.env.production**
```
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_APP_TITLE=文献辅助阅读系统
```

### 3. 构建生产版本
```bash
npm run build
```

构建完成后，`dist` 目录包含所有静态文件。

### 4. 部署到服务器

#### Nginx 配置示例
```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    # 重定向到HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    # SSL证书配置
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    
    # 网站根目录
    root /var/www/scholar-assist/dist;
    index index.html;
    
    # 启用gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/javascript application/xml+rss application/json;
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # SPA路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API代理（如果需要）
    location /api/ {
        proxy_pass http://backend-server:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Apache 配置示例
```apache
<VirtualHost *:80>
    ServerName yourdomain.com
    Redirect permanent / https://yourdomain.com/
</VirtualHost>

<VirtualHost *:443>
    ServerName yourdomain.com
    DocumentRoot /var/www/scholar-assist/dist
    
    # SSL配置
    SSLEngine on
    SSLCertificateFile /path/to/certificate.crt
    SSLCertificateKeyFile /path/to/private.key
    
    # 启用压缩
    LoadModule deflate_module modules/mod_deflate.so
    <Location />
        SetOutputFilter DEFLATE
        SetEnvIfNoCase Request_URI \
            \.(?:gif|jpe?g|png)$ no-gzip dont-vary
        SetEnvIfNoCase Request_URI \
            \.(?:exe|t?gz|zip|bz2|sit|rar)$ no-gzip dont-vary
    </Location>
    
    # 静态资源缓存
    <FilesMatch "\.(css|js|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$">
        ExpiresActive On
        ExpiresDefault "access plus 1 year"
    </FilesMatch>
    
    # SPA路由支持
    <Directory "/var/www/scholar-assist/dist">
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
        
        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </Directory>
</VirtualHost>
```

## Docker 部署

### Dockerfile
```dockerfile
# 构建阶段
FROM node:18-alpine as build-stage

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine as production-stage

# 复制构建文件
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### docker-compose.yml
```yaml
version: '3.8'

services:
  frontend:
    build: .
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./ssl:/etc/nginx/ssl:ro
    environment:
      - NODE_ENV=production
    restart: unless-stopped
    
  # 如果有后端服务
  backend:
    image: your-backend-image
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=your-database-url
    restart: unless-stopped
```

### 构建和运行
```bash
# 构建镜像
docker build -t scholar-assist-frontend .

# 运行容器
docker run -d -p 80:80 --name scholar-assist scholar-assist-frontend

# 或使用docker-compose
docker-compose up -d
```

## CI/CD 配置

### GitHub Actions 示例
```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
    
    - name: Install dependencies
      run: npm ci
    
    - name: Run tests
      run: npm run test
    
    - name: Build application
      run: npm run build
      env:
        VITE_API_BASE_URL: ${{ secrets.API_BASE_URL }}
    
    - name: Deploy to server
      uses: appleboy/ssh-action@v0.1.5
      with:
        host: ${{ secrets.HOST }}
        username: ${{ secrets.USERNAME }}
        key: ${{ secrets.SSH_KEY }}
        script: |
          cd /var/www/scholar-assist
          git pull origin main
          npm ci
          npm run build
          sudo systemctl reload nginx
```

## 性能优化

### 1. 代码分割
项目已配置路由级别的代码分割，确保按需加载。

### 2. 资源优化
- 图片使用WebP格式
- 启用Gzip/Brotli压缩
- 配置适当的缓存策略

### 3. CDN配置
```javascript
// vite.config.ts 生产配置
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          elementPlus: ['element-plus']
        }
      }
    }
  },
  // CDN配置
  base: process.env.NODE_ENV === 'production' ? 'https://cdn.yourdomain.com/' : '/'
})
```

## 监控和日志

### 1. 错误监控
集成Sentry或其他错误监控服务：

```javascript
// main.ts
import * as Sentry from "@sentry/vue"

Sentry.init({
  app,
  dsn: "YOUR_SENTRY_DSN",
  environment: process.env.NODE_ENV
})
```

### 2. 性能监控
```javascript
// 性能监控
if ('performance' in window) {
  window.addEventListener('load', () => {
    const perfData = performance.getEntriesByType('navigation')[0]
    // 发送性能数据到监控服务
  })
}
```

## 安全配置

### 1. CSP头部
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:;";
```

### 2. 其他安全头部
```nginx
add_header X-Frame-Options "SAMEORIGIN";
add_header X-Content-Type-Options "nosniff";
add_header X-XSS-Protection "1; mode=block";
add_header Referrer-Policy "strict-origin-when-cross-origin";
```

## 故障排除

### 常见问题

1. **路由404错误**
   - 确保服务器配置了SPA路由回退
   - 检查base路径配置

2. **静态资源加载失败**
   - 检查资源路径配置
   - 确认CDN配置正确

3. **API请求失败**
   - 检查CORS配置
   - 确认API代理设置

4. **构建失败**
   - 检查Node.js版本
   - 清除node_modules重新安装

### 日志查看
```bash
# Nginx日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# Docker日志
docker logs scholar-assist

# 系统日志
journalctl -u nginx -f
```

## 备份和恢复

### 1. 代码备份
- 使用Git版本控制
- 定期推送到远程仓库

### 2. 配置备份
```bash
# 备份Nginx配置
cp /etc/nginx/sites-available/scholar-assist /backup/nginx/

# 备份SSL证书
cp -r /etc/ssl/certs/scholar-assist /backup/ssl/
```

### 3. 自动化备份脚本
```bash
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/scholar-assist_$DATE"

mkdir -p $BACKUP_DIR

# 备份应用文件
cp -r /var/www/scholar-assist $BACKUP_DIR/

# 备份配置文件
cp /etc/nginx/sites-available/scholar-assist $BACKUP_DIR/

# 压缩备份
tar -czf "$BACKUP_DIR.tar.gz" $BACKUP_DIR
rm -rf $BACKUP_DIR

echo "Backup completed: $BACKUP_DIR.tar.gz"
```