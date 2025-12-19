import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { AppInitializer } from '@/utils/appInitializer'
import '@/styles/index.scss'

/**
 * 应用启动函数
 */
async function bootstrap() {
  try {
    // 创建Vue应用实例
    const app = createApp(App)

    // 注册Element Plus图标
    for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
      app.component(key, component)
    }

    // 注册插件
    app.use(createPinia())
    app.use(router)
    app.use(ElementPlus)

    // 初始化应用系统
    const appInitializer = new AppInitializer()
    await appInitializer.initialize({
      enablePerformanceMonitoring: import.meta.env.DEV
    })

    // 挂载应用
    app.mount('#app')

    // 在应用卸载时清理资源
    app.config.globalProperties.$destroy = () => {
      appInitializer.destroy()
    }

    console.log('🚀 应用启动成功')
  } catch (error) {
    console.error('❌ 应用启动失败:', error)
    
    // 显示错误页面或重试机制
    document.body.innerHTML = `
      <div style="
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100vh;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        background: #f5f5f5;
        color: #333;
      ">
        <h1 style="color: #e74c3c; margin-bottom: 16px;">应用启动失败</h1>
        <p style="margin-bottom: 24px; text-align: center; max-width: 400px;">
          很抱歉，应用在启动过程中遇到了问题。请刷新页面重试。
        </p>
        <button 
          onclick="window.location.reload()" 
          style="
            padding: 12px 24px;
            background: #3498db;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
          "
        >
          刷新页面
        </button>
      </div>
    `
  }
}

// 启动应用
bootstrap()