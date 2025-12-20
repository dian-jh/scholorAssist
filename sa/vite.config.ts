import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量，process.cwd() 为项目根目录
  // 第三个参数 '' 表示加载所有环境变量，不管是否有 VITE_ 前缀（尽管 vite 默认只暴露 VITE_ 开头的给客户端，但在 config 中我们可以读取所有）
  const env = loadEnv(mode, process.cwd(), '')

  return {
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
        // 所有 API 请求（包括文件流）统一走网关
        '/api': {
          target: 'http://localhost:10100',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api/, '/api')
        },
        // 静态文件请求（/files/** 等）也统一走网关，由网关路由到文件服务
        '/files': {
          target: `${env.VITE_GATEWAY_PROTOCOL || 'http'}://${env.VITE_GATEWAY_HOST || 'localhost'}:${env.VITE_GATEWAY_PORT || '10100'}`,
          changeOrigin: true,
          secure: false
        },
        // 兼容历史静态前缀，同样指向网关
        '/uploads': {
          target: `${env.VITE_GATEWAY_PROTOCOL || 'http'}://${env.VITE_GATEWAY_HOST || 'localhost'}:${env.VITE_GATEWAY_PORT || '10100'}`,
          changeOrigin: true,
          secure: false
        },
        '/pdfs': {
          target: `${env.VITE_GATEWAY_PROTOCOL || 'http'}://${env.VITE_GATEWAY_HOST || 'localhost'}:${env.VITE_GATEWAY_PORT || '10100'}`,
          changeOrigin: true,
          secure: false
        },
        // 其他静态映射
        '/static': {
          target: `${env.VITE_GATEWAY_PROTOCOL || 'http'}://${env.VITE_GATEWAY_HOST || 'localhost'}:${env.VITE_GATEWAY_PORT || '10100'}`,
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
  }
})
