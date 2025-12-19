import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: [
        'vue',
        'vue-router',
        'pinia'
      ],
      dts: true
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: true
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`
      }
    }
  },
  server: {
    port: 3000,
    open: true,
    cors: true,
    proxy: {
      // 优先匹配文件字节流/下载接口，直接代理到文献服务，避免网关或Nacos路由未生效导致404
      '/api/files': {
        target: `${process.env.VITE_UPLOADS_BASE_URL || `${process.env.VITE_LITERATURE_PROTOCOL || 'http'}://${process.env.VITE_LITERATURE_HOST || 'localhost'}:${process.env.VITE_LITERATURE_PORT || '10140'}`}`,
        changeOrigin: true,
        secure: false
      },
      // API 请求统一走网关
      '/api': {
        target: `${process.env.VITE_GATEWAY_PROTOCOL || 'http'}://${process.env.VITE_GATEWAY_HOST || 'localhost'}:${process.env.VITE_GATEWAY_PORT || '10100'}`,
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      },
      // 静态文件请求（/files/** 等）直接代理到文献服务，避免网关未配置静态路由导致 404
      // 可通过 VITE_UPLOADS_BASE_URL 指定完整地址；否则默认使用文献服务端口 10140
      '/files': {
        target: `${process.env.VITE_UPLOADS_BASE_URL || `${process.env.VITE_LITERATURE_PROTOCOL || 'http'}://${process.env.VITE_LITERATURE_HOST || 'localhost'}:${process.env.VITE_LITERATURE_PORT || '10140'}`}`,
        changeOrigin: true,
        secure: false
      },
      // 兼容历史静态前缀，同样指向文献服务
      '/uploads': {
        target: `${process.env.VITE_UPLOADS_BASE_URL || `${process.env.VITE_LITERATURE_PROTOCOL || 'http'}://${process.env.VITE_LITERATURE_HOST || 'localhost'}:${process.env.VITE_LITERATURE_PORT || '10140'}`}`,
        changeOrigin: true,
        secure: false
      },
      '/pdfs': {
        target: `${process.env.VITE_UPLOADS_BASE_URL || `${process.env.VITE_LITERATURE_PROTOCOL || 'http'}://${process.env.VITE_LITERATURE_HOST || 'localhost'}:${process.env.VITE_LITERATURE_PORT || '10140'}`}`,
        changeOrigin: true,
        secure: false
      },
      // 其他静态映射
      '/static': {
        target: `${process.env.VITE_UPLOADS_BASE_URL || `${process.env.VITE_LITERATURE_PROTOCOL || 'http'}://${process.env.VITE_LITERATURE_HOST || 'localhost'}:${process.env.VITE_LITERATURE_PORT || '10140'}`}`,
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    target: 'es2015',
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    rollupOptions: {
      output: {
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: '[ext]/[name]-[hash].[ext]'
      }
    }
  }
})