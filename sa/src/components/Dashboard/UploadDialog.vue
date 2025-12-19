<template>
  <el-dialog
    v-model="visible"
    title="上传PDF文献"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="upload-container">
      <el-upload
        ref="uploadRef"
        class="upload-dragger"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :accept="'.pdf'"
        :limit="1"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将PDF文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传PDF文件，且不超过50MB
          </div>
        </template>
      </el-upload>
      
      <div v-if="selectedFile" class="file-info">
        <h4>文件信息</h4>
        <el-form :model="uploadForm" label-width="80px">
          <el-form-item label="文件名">
            <span>{{ selectedFile.name }}</span>
          </el-form-item>
          <el-form-item label="文件大小">
            <span>{{ formatFileSize(selectedFile.size) }}</span>
          </el-form-item>
          <el-form-item label="文档标题">
            <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select 
              v-model="uploadForm.categoryId" 
              placeholder="选择分类"
              :loading="categoriesLoading"
              clearable
              filterable
            >
              <el-option
                v-for="category in flatCategories"
                :key="category.id"
                :label="formatCategoryLabel(category)"
                :value="category.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      
      <div v-if="uploading" class="upload-progress">
        <el-progress :percentage="uploadProgress" />
        <p v-if="retryCount === 0">正在上传文档...</p>
        <p v-else>正在重试上传（第 {{ retryCount + 1 }} 次尝试）...</p>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button 
          type="primary" 
          :disabled="!selectedFile || uploading"
          :loading="uploading"
          @click="handleUpload"
        >
          开始上传
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { useDocumentStore, useCategoryStore } from '@/store'
import type { MockCategory } from '@/api/mockManager'

interface Props {
  visible: boolean
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success', document: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const documentStore = useDocumentStore()
const categoryStore = useCategoryStore()

const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const categoriesLoading = ref(false)
const retryCount = ref(0)
const maxRetries = 3

const uploadForm = reactive({
  title: '',
  categoryId: ''
})

// 获取扁平化的分类列表用于选择器
const flatCategories = computed(() => {
  return categoryStore.getFlatCategories
})

// 格式化分类选项显示文本（显示层级缩进）
const formatCategoryLabel = (category: MockCategory & { level?: number }) => {
  const indent = '　'.repeat(category.level || 0) // 使用全角空格缩进
  return `${indent}${category.name}`
}

// 监听对话框显示状态，自动加载分类数据
watch(visible, async (newVisible) => {
  if (newVisible && flatCategories.value.length === 0) {
    await loadCategories()
  }
})

const loadCategories = async () => {
  try {
    categoriesLoading.value = true
    await categoryStore.fetchCategories()
    
    // 检查是否成功加载分类数据
    if (flatCategories.value.length === 0) {
      ElMessage.warning('暂无可用分类，请先创建分类')
    }
  } catch (error) {
    console.error('加载分类数据失败:', error)
    
    // 根据错误类型提供不同的提示
    let errorMessage = '加载分类数据失败'
    if (error instanceof Error) {
      if (error.message.includes('网络')) {
        errorMessage = '网络连接失败，请检查网络连接后重试'
      } else if (error.message.includes('超时')) {
        errorMessage = '请求超时，请稍后重试'
      } else if (error.message.includes('权限')) {
        errorMessage = '没有权限访问分类数据，请联系管理员'
      } else {
        errorMessage = `加载分类数据失败：${error.message}`
      }
    }
    
    ElMessage.error(errorMessage)
  } finally {
    categoriesLoading.value = false
  }
}

// 重试上传函数
const retryUpload = async (): Promise<any> => {
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      retryCount.value = attempt
      
      const result = await documentStore.uploadDocumentFile(
        selectedFile.value!,
        uploadForm.title.trim(),
        uploadForm.categoryId
      )
      
      return result
    } catch (error) {
      console.error(`上传尝试 ${attempt + 1} 失败:`, error)
      
      // 如果是最后一次尝试，抛出错误
      if (attempt === maxRetries) {
        throw error
      }
      
      // 等待一段时间后重试（指数退避）
      const delay = Math.pow(2, attempt) * 1000 // 1s, 2s, 4s
      await new Promise(resolve => setTimeout(resolve, delay))
      
      ElMessage.info(`上传失败，正在进行第 ${attempt + 2} 次尝试...`)
    }
  }
}

const handleFileChange = (file: UploadFile) => {
  if (file.raw) {
    // UploadRawFile 兼容 File；显式缩小类型并防止 undefined
    selectedFile.value = file.raw as File
    uploadForm.title = file.name.replace(/\.pdf$/i, '')
  } else {
    selectedFile.value = null
    uploadForm.title = ''
  }
}

const formatFileSize = (size: number) => {
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const handleUpload = async () => {
  // 验证文件
  if (!selectedFile.value) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  
  // 验证文件类型
  if (!selectedFile.value.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.error('只支持上传PDF格式的文件')
    return
  }
  
  // 验证文件大小（50MB限制）
  const maxSize = 50 * 1024 * 1024 // 50MB
  if (selectedFile.value.size > maxSize) {
    ElMessage.error('文件大小不能超过50MB')
    return
  }
  
  // 验证分类选择
  if (!uploadForm.categoryId) {
    ElMessage.warning('请选择文档分类')
    return
  }
  
  // 验证标题
  if (!uploadForm.title.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }
  
  try {
    uploading.value = true
    uploadProgress.value = 0
    retryCount.value = 0
    
    const result = await retryUpload()
    
    ElMessage.success('文档上传成功')
    emit('success', result.data)
    visible.value = false
    
    // 重置表单
    resetForm()
  } catch (error) {
    console.error('上传失败:', error)
    
    // 根据错误类型提供不同的提示
    let errorMessage = '上传失败'
    if (error instanceof Error) {
      if (error.message.includes('网络')) {
        errorMessage = '网络连接失败，请检查网络连接后重试'
      } else if (error.message.includes('超时')) {
        errorMessage = '上传超时，请检查网络连接或稍后重试'
      } else if (error.message.includes('文件大小')) {
        errorMessage = '文件过大，请选择小于50MB的文件'
      } else if (error.message.includes('格式')) {
        errorMessage = '文件格式不支持，请上传PDF文件'
      } else if (error.message.includes('权限')) {
        errorMessage = '没有上传权限，请联系管理员'
      } else if (error.message.includes('存储空间')) {
        errorMessage = '存储空间不足，请联系管理员'
      } else {
        errorMessage = `上传失败：${error.message}`
      }
    }
    
    ElMessage.error(errorMessage)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

const resetForm = () => {
  selectedFile.value = null
  uploadForm.title = ''
  uploadForm.categoryId = ''
  uploadProgress.value = 0
  retryCount.value = 0
}

// 监听对话框关闭，重置表单
watch(visible, (newVisible) => {
  if (!newVisible) {
    resetForm()
  }
})

// 监听上传进度
const unwatchProgress = documentStore.$subscribe((mutation, state) => {
  uploadProgress.value = state.uploadProgress
})

onMounted(() => {
  // 组件挂载时预加载分类数据
  if (flatCategories.value.length === 0) {
    loadCategories()
  }
})
</script>

<style lang="scss" scoped>
.upload-container {
  .upload-dragger {
    width: 100%;
  }
}

.file-info {
  margin-top: 20px;
  padding: 16px;
  background-color: var(--el-fill-color-light);
  border-radius: 6px;
  
  h4 {
    margin: 0 0 16px 0;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}

.upload-progress {
  margin-top: 20px;
  text-align: center;
  
  p {
    margin-top: 8px;
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}
</style>