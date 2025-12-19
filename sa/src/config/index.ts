/**
 * 系统配置管理
 * 集中管理所有配置项，包括mock开关、网关地址等
 */

// 配置接口定义
export interface AppConfig {
  // Mock数据开关
  useMock: boolean
  // 网关地址配置
  gateway: {
    host: string
    port: number
    protocol: string
    baseUrl: string
  }
  // 静态文件服务配置（用于PDF、图片等）
  staticFiles: {
    baseUrl: string
  }
  // API配置
  api: {
    baseUrl: string
    timeout: number
  }
  // 应用配置
  app: {
    title: string
    version: string
  }
}

/**
 * 获取环境变量值，支持默认值
 */
function getEnvValue(key: string, defaultValue: string = ''): string {
  return import.meta.env[key] || defaultValue
}

/**
 * 获取布尔类型环境变量
 */
function getEnvBoolean(key: string, defaultValue: boolean = false): boolean {
  const value = getEnvValue(key).toLowerCase()
  return value === 'true' || value === '1'
}

/**
 * 获取数字类型环境变量
 */
function getEnvNumber(key: string, defaultValue: number = 0): number {
  const value = getEnvValue(key)
  const parsed = parseInt(value, 10)
  return isNaN(parsed) ? defaultValue : parsed
}

/**
 * 构建网关基础URL
 */
function buildGatewayUrl(protocol: string, host: string, port: number): string {
  return `${protocol}://${host}:${port}`
}

/**
 * 系统配置对象
 * 所有配置项都从环境变量中读取，支持默认值
 */
export const config: AppConfig = {
  // Mock开关配置
  useMock: getEnvBoolean('VITE_USE_MOCK', false),
  
  // 网关配置
  gateway: {
    protocol: getEnvValue('VITE_GATEWAY_PROTOCOL', 'http'),
    host: getEnvValue('VITE_GATEWAY_HOST', 'localhost'),
    port: getEnvNumber('VITE_GATEWAY_PORT', 10100),
    get baseUrl() {
      return buildGatewayUrl(this.protocol, this.host, this.port)
    }
  },
  // 静态文件服务：默认与gateway一致，可通过 VITE_UPLOADS_BASE_URL 单独指定
  staticFiles: {
    get baseUrl() {
      const fallback = config.gateway.baseUrl
      const envUrl = getEnvValue('VITE_UPLOADS_BASE_URL', '')
      return envUrl || fallback
    }
  },
  
  // API配置
  api: {
    get baseUrl() {
      return `${config.gateway.baseUrl}/api`
    },
    timeout: getEnvNumber('VITE_API_TIMEOUT', 10000)
  },
  
  // 应用配置
  app: {
    title: getEnvValue('VITE_APP_TITLE', '文献辅助阅读系统'),
    version: getEnvValue('VITE_APP_VERSION', '1.0.0')
  }
}

/**
 * 配置工具函数
 */
export class ConfigManager {
  /**
   * 获取完整配置对象
   */
  static getConfig(): AppConfig {
    return config
  }
  
  /**
   * 检查是否使用Mock数据
   */
  static useMock(): boolean {
    return config.useMock
  }
  
  /**
   * 获取网关基础URL
   */
  static getGatewayUrl(): string {
    return config.gateway.baseUrl
  }

  /**
   * 获取静态文件基础URL（用于PDF/图片等）
   */
  static getUploadsBaseUrl(): string {
    return config.staticFiles.baseUrl
  }
  
  /**
   * 获取API基础URL
   */
  static getApiBaseUrl(): string {
    return config.api.baseUrl
  }
  
  /**
   * 获取API超时时间
   */
  static getApiTimeout(): number {
    return config.api.timeout
  }
  
  /**
   * 获取应用标题
   */
  static getAppTitle(): string {
    return config.app.title
  }
  
  /**
   * 打印当前配置（用于调试）
   */
  static printConfig(): void {
    console.group('🔧 系统配置信息')
    console.log('Mock模式:', config.useMock ? '启用' : '禁用')
    console.log('网关地址:', config.gateway.baseUrl)
    console.log('静态文件地址:', config.staticFiles.baseUrl)
    console.log('API地址:', config.api.baseUrl)
    console.log('应用标题:', config.app.title)
    console.log('API超时:', config.api.timeout + 'ms')
    console.groupEnd()
  }
}

// 开发环境下打印配置信息
if (import.meta.env.DEV) {
  ConfigManager.printConfig()
}

// 导出默认配置对象
export default config