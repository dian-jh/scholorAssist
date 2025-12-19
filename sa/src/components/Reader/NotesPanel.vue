<template>
  <div class="notes-panel">
    <!-- 搜索和筛选 -->
    <div class="notes-header">
      <el-input
        v-model="searchQuery"
        placeholder="搜索笔记..."
        size="small"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      
      <div class="filter-tabs">
        <el-button-group size="small">
          <el-button 
            v-for="category in categories"
            :key="category.key"
            :type="activeCategory === category.key ? 'primary' : ''"
            @click="setActiveCategory(category.key)"
          >
            {{ category.label }}
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div class="notes-list">
      <div class="notes-section" v-if="filteredNotes.length > 0">
        <div 
          v-for="note in filteredNotes"
          :key="note.id"
          class="note-item"
          :class="{ 'active': selectedNoteId === note.id }"
          @click="selectNote(note)"
        >
          <div class="note-header">
            <div class="note-meta">
              <el-tag 
                :type="getNoteTypeColor(note.type)" 
                size="small"
              >
                {{ getNoteTypeLabel(note.type) }}
              </el-tag>
              <span class="note-page">第{{ note.page_number }}页</span>
            </div>
            <div class="note-actions">
              <el-dropdown @command="handleNoteAction">
                <el-button size="small" text>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="`edit-${note.id}`">编辑</el-dropdown-item>
                    <el-dropdown-item :command="`delete-${note.id}`" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          
          <div class="note-content">
            <p class="note-text">{{ note.content }}</p>
            <div class="note-highlight" v-if="note.highlight_data?.text">
              <el-icon><EditPen /></el-icon>
              <span>"{{ note.highlight_data?.text }}"</span>
            </div>
          </div>
          
          <div class="note-footer">
            <span class="note-time">{{ formatTime(note.createdAt) }}</span>
          </div>
        </div>
      </div>
      
      <!-- 空状态 -->
      <div class="empty-state" v-else>
        <el-empty 
          :image-size="80"
          description="暂无笔记"
        >
          <el-button type="primary" @click="createNote">
            <el-icon><Plus /></el-icon>
            创建第一条笔记
          </el-button>
        </el-empty>
      </div>
    </div>

    <!-- 创建笔记按钮 -->
    <div class="notes-footer" v-if="filteredNotes.length > 0">
      <el-button 
        type="primary" 
        size="small" 
        @click="createNote"
        style="width: 100%"
      >
        <el-icon><Plus /></el-icon>
        添加笔记
      </el-button>
    </div>

    <!-- 创建/编辑笔记对话框 -->
    <el-dialog
      :model-value="showCreateDialog || showEditDialog"
      :title="editingNote ? '编辑笔记' : '创建笔记'"
      width="500px"
      :before-close="handleDialogClose"
    >
      <el-form :model="newNote" label-width="80px">
        <el-form-item label="标题">
          <el-input 
            v-model="newNote.title" 
            placeholder="请输入笔记标题（可选）"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="笔记类型">
          <el-select v-model="newNote.type" placeholder="选择类型">
            <el-option label="重点标记" value="highlight" />
            <el-option label="个人想法" value="thought" />
            <el-option label="问题疑惑" value="question" />
            <el-option label="总结归纳" value="summary" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="页码">
          <el-input-number 
            v-model="newNote.page" 
            :min="1" 
            :max="999"
            controls-position="right"
          />
        </el-form-item>
        
        <el-form-item label="笔记内容">
          <el-input
            v-model="newNote.content"
            type="textarea"
            :rows="4"
            placeholder="输入笔记内容..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="引用文本" v-if="newNote.type === 'highlight'">
          <el-input
            v-model="newNote.highlightText"
            type="textarea"
            :rows="2"
            placeholder="引用的原文内容..."
          />
        </el-form-item>
        
        <el-form-item label="标签">
          <el-tag
            v-for="tag in newNote.tags"
            :key="tag"
            closable
            @close="removeTag(tag)"
            style="margin-right: 8px;"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="inputVisible"
            ref="inputRef"
            v-model="inputValue"
            size="small"
            style="width: 100px;"
            @keyup.enter="handleInputConfirm"
            @blur="handleInputConfirm"
          />
          <el-button v-else size="small" @click="showInput">
            + 添加标签
          </el-button>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="handleDialogClose">取消</el-button>
        <el-button type="primary" @click="saveNote" :loading="loading">
          {{ editingNote ? '更新' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  Plus, 
  EditPen, 
  MoreFilled 
} from '@element-plus/icons-vue'
import { 
  getNotesList, 
  createNote as createNoteApi, 
  updateNote as updateNoteApi, 
  deleteNote as deleteNoteApi,
  type Note as ApiNote
} from '@/api/NotesApi'
// 复合加载器：并发获取字节流与笔记，自动请求合并与缓存
import { fetchDocumentComposite } from './useDocumentData'

// Props
interface Props {
  documentId?: string
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  noteSelect: [noteId: string]
  noteCreate: [content: string]
  noteUpdate: [noteId: string]
  noteDelete: [noteId: string]
}>()

// 笔记类型定义（扩展API类型）
interface Note extends Omit<ApiNote, 'created_at' | 'updated_at'> {
  type: 'highlight' | 'thought' | 'question' | 'summary'
  createdAt: Date
  updatedAt: Date
}

// 响应式数据
const searchQuery = ref('')
const activeCategory = ref('all')
const selectedNoteId = ref('')
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const loading = ref(false)
const notes = ref<Note[]>([])
const editingNote = ref<Note | null>(null)

const newNote = ref({
  type: 'thought' as Note['type'],
  title: '',
  content: '',
  highlightText: '',
  page: 1,
  tags: [] as string[]
})

// 标签输入管理
const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref<any>(null)

const showInput = async () => {
  inputVisible.value = true
  await nextTick()
  inputRef.value?.focus()
}

const handleInputConfirm = () => {
  const value = inputValue.value.trim()
  if (value && !newNote.value.tags.includes(value)) {
    newNote.value.tags.push(value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

const removeTag = (tag: string) => {
  newNote.value.tags = newNote.value.tags.filter(t => t !== tag)
}

// 分类配置
const categories = [
  { key: 'all', label: '全部' },
  { key: 'highlight', label: '标记' },
  { key: 'thought', label: '想法' },
  { key: 'question', label: '问题' },
  { key: 'summary', label: '总结' }
]

// 计算属性
const filteredNotes = computed(() => {
  let filtered = notes.value

  // 按分类筛选
  if (activeCategory.value !== 'all') {
    filtered = filtered.filter(note => note.type === activeCategory.value)
  }

  // 按搜索关键词筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(note => 
      note.content.toLowerCase().includes(query) ||
      note.title.toLowerCase().includes(query) ||
      (note.highlight_data?.text && note.highlight_data.text.toLowerCase().includes(query))
    )
  }

  // 按创建时间倒序排列
  return filtered.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())
})

// 监听文档ID变化，重新加载笔记
watch(() => props.documentId, (newDocumentId) => {
  if (newDocumentId) {
    loadNotes()
  }
}, { immediate: true })

// 方法
async function loadNotes() {
  if (!props.documentId) return
  loading.value = true
  try {
    // 使用复合加载器以便与阅读器的加载去重合并
    const composite = await fetchDocumentComposite(props.documentId, { pageSize: 200 })
    const apiNotes: ApiNote[] = composite.notes || []
    notes.value = apiNotes.map((apiNote: ApiNote) => ({
      ...apiNote,
      type: getTypeFromTags(apiNote.tags) as Note['type'],
      createdAt: new Date(apiNote.created_at),
      updatedAt: new Date(apiNote.updated_at)
    }))
  } catch (error) {
    console.error('加载笔记失败:', error)
    ElMessage.error('加载笔记失败')
  } finally {
    loading.value = false
  }
}

// 根据标签推断笔记类型
const getTypeFromTags = (tags: string[]): string => {
  if (tags.includes('highlight') || tags.includes('标记')) return 'highlight'
  if (tags.includes('question') || tags.includes('问题')) return 'question'
  if (tags.includes('summary') || tags.includes('总结')) return 'summary'
  return 'thought'
}

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

const setActiveCategory = (category: string) => {
  activeCategory.value = category
}

const selectNote = (note: Note) => {
  selectedNoteId.value = note.id
  emit('noteSelect', note.id)
}

const createNote = () => {
  showCreateDialog.value = true
  editingNote.value = null
  // 重置表单
  newNote.value = {
    type: 'thought',
    title: '',
    content: '',
    highlightText: '',
    page: 1,
    tags: []
  }
}

const editNote = (note: Note) => {
  showEditDialog.value = true
  editingNote.value = note
  // 填充表单
  newNote.value = {
    type: note.type,
    title: note.title,
    content: note.content,
    highlightText: note.highlight_data?.text || '',
    page: note.page_number,
    tags: [...note.tags]
  }
}

const saveNote = async () => {
  if (!newNote.value.content.trim()) {
    ElMessage.warning('请输入笔记内容')
    return
  }

  if (!props.documentId) {
    ElMessage.error('文档ID不能为空')
    return
  }

  loading.value = true
  try {
    const noteData = {
      document_id: props.documentId,
      title: newNote.value.title || '无标题',
      content: newNote.value.content,
      page_number: newNote.value.page,
      tags: [...newNote.value.tags, newNote.value.type],
      highlight_data: newNote.value.highlightText ? {
        text: newNote.value.highlightText,
        start_offset: 0,
        end_offset: newNote.value.highlightText.length,
        color: getHighlightColor(newNote.value.type),
        position: { x: 0, y: 0, width: 0, height: 0 }
      } : undefined
    }

    if (editingNote.value) {
      // 更新笔记
      const response = await updateNoteApi(editingNote.value.id, noteData)
      if (response.code === 200) {
        ElMessage.success('笔记更新成功')
        emit('noteUpdate', editingNote.value.id)
      }
    } else {
      // 创建新笔记
      const response = await createNoteApi(noteData)
      if (response.code === 200) {
        ElMessage.success('笔记创建成功')
        emit('noteCreate', newNote.value.content)
      }
    }
    
    showCreateDialog.value = false
    showEditDialog.value = false
    await loadNotes()
  } catch (error) {
    console.error('保存笔记失败:', error)
    ElMessage.error('保存笔记失败')
  } finally {
    loading.value = false
  }
}

const getHighlightColor = (type: Note['type']): string => {
  const colors = {
    highlight: '#fbbf24',
    thought: '#3b82f6',
    question: '#ef4444',
    summary: '#10b981'
  }
  return colors[type]
}

const handleDialogClose = () => {
  showCreateDialog.value = false
  showEditDialog.value = false
  editingNote.value = null
}

const handleNoteAction = async (command: string) => {
  const [action, noteId] = command.split('-')
  
  if (action === 'edit') {
    const note = notes.value.find(n => n.id === noteId)
    if (note) {
      editNote(note)
    }
  } else if (action === 'delete') {
    ElMessageBox.confirm('确定要删除这条笔记吗？', '确认删除', {
      type: 'warning'
    }).then(async () => {
      loading.value = true
      try {
        const response = await deleteNoteApi(noteId)
        if (response.code === 200) {
          ElMessage.success('笔记已删除')
          emit('noteDelete', noteId)
          await loadNotes()
        }
      } catch (error) {
        console.error('删除笔记失败:', error)
        ElMessage.error('删除笔记失败')
      } finally {
        loading.value = false
      }
    }).catch(() => {
      // 用户取消删除
    })
  }
}

const getNoteTypeLabel = (type: Note['type']) => {
  const labels = {
    highlight: '标记',
    thought: '想法',
    question: '问题',
    summary: '总结'
  }
  return labels[type]
}

const getNoteTypeColor = (type: Note['type']): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  const colors: Record<Note['type'], 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    highlight: 'warning',
    thought: 'primary',
    question: 'danger',
    summary: 'success'
  }
  return colors[type]
}

const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString()
  }
}

// 生命周期
onMounted(() => {
  // 加载示例数据（类型安全，字段对齐API定义）
  notes.value = [
    {
      id: 'note_example_1',
      document_id: props.documentId || 'doc_example',
      title: '示例标记',
      content: '这个概念很重要，需要重点理解',
      page_number: 15,
      tags: ['highlight'],
      type: 'highlight',
      createdAt: new Date(Date.now() - 1000 * 60 * 30), // 30分钟前
      updatedAt: new Date(Date.now() - 1000 * 60 * 30),
      highlight_data: {
        text: '机器学习是人工智能的一个重要分支',
        start_offset: 0,
        end_offset: '机器学习是人工智能的一个重要分支'.length,
        color: getHighlightColor('highlight'),
        position: { x: 0, y: 0, width: 0, height: 0 }
      }
    },
    {
      id: 'note_example_2',
      document_id: props.documentId || 'doc_example',
      title: '示例问题',
      content: '这里的算法复杂度是如何计算的？需要进一步研究',
      page_number: 23,
      tags: ['question'],
      type: 'question',
      createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2), // 2小时前
      updatedAt: new Date(Date.now() - 1000 * 60 * 60 * 2)
    },
    {
      id: 'note_example_3',
      document_id: props.documentId || 'doc_example',
      title: '示例总结',
      content: '本章总结：深度学习的三个核心要素是数据、算法和计算力',
      page_number: 45,
      tags: ['summary'],
      type: 'summary',
      createdAt: new Date(Date.now() - 1000 * 60 * 60 * 24), // 1天前
      updatedAt: new Date(Date.now() - 1000 * 60 * 60 * 24)
    }
  ]
})
</script>

<style lang="scss" scoped>
.notes-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.notes-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  
  .filter-tabs {
    margin-top: 12px;
    
    :deep(.el-button-group) {
      width: 100%;
      
      .el-button {
        flex: 1;
        font-size: 12px;
        padding: 6px 8px;
        border-color: #e5e5e5;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        
        &.is-active {
          background: linear-gradient(135deg, #10a37f, #0d8f6b);
          border-color: #10a37f;
          color: #ffffff;
          box-shadow: 0 4px 12px rgba(16, 163, 127, 0.3);
        }
      }
    }
  }
}

.notes-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: linear-gradient(135deg, #c1c1c1, #a1a1a1);
    border-radius: 3px;
    
    &:hover {
      background: linear-gradient(135deg, #a1a1a1, #888);
    }
  }
}

.note-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
    transition: left 0.5s;
  }
  
  &:hover {
    background: #f8f9fa;
    transform: translateX(4px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    
    &::before {
      left: 100%;
    }
  }
  
  &.active {
    background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
    border-left: 3px solid #10a37f;
    box-shadow: 0 2px 8px rgba(16, 163, 127, 0.2);
  }
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  
  .note-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .note-page {
      font-size: 12px;
      color: #6b7280;
      background: #f3f4f6;
      padding: 2px 6px;
      border-radius: 8px;
      transition: all 0.2s ease;
    }
  }
  
  .note-actions {
    opacity: 0;
    transition: all 0.3s ease;
    transform: translateX(10px);
  }
}

.note-item:hover .note-actions {
  opacity: 1;
  transform: translateX(0);
}

.note-content {
  margin-bottom: 8px;
  
  .note-text {
    font-size: 14px;
    line-height: 1.5;
    color: #374151;
    margin: 0 0 8px 0;
    transition: color 0.2s ease;
  }
  
  .note-highlight {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    padding: 8px;
    background: linear-gradient(135deg, #fef3c7, #fde68a);
    border-radius: 8px;
    font-size: 13px;
    color: #92400e;
    transition: all 0.2s ease;
    
    .el-icon {
      margin-top: 2px;
      flex-shrink: 0;
    }
    
    &:hover {
      transform: scale(1.02);
      box-shadow: 0 2px 8px rgba(146, 64, 14, 0.2);
    }
  }
}

.note-footer {
  .note-time {
    font-size: 12px;
    color: #9ca3af;
    transition: color 0.2s ease;
  }
}

.notes-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #fafafa, #f5f5f5);
  
  .el-button {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    background: linear-gradient(135deg, #10a37f, #0d8f6b);
    border: none;
    border-radius: 12px;
    position: relative;
    overflow: hidden;
    
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
      transition: left 0.5s;
    }
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(16, 163, 127, 0.4);
      
      &::before {
        left: 100%;
      }
    }
    
    &:active {
      transform: translateY(-1px);
    }
  }
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  animation: fadeIn 0.5s ease;
  
  .el-empty {
    :deep(.el-empty__image) {
      transition: all 0.3s ease;
      
      &:hover {
        transform: scale(1.1);
      }
    }
  }
}

// 对话框样式优化
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  
  .el-dialog__header {
    background: linear-gradient(135deg, #fafafa, #f5f5f5);
    border-bottom: 1px solid #e5e5e5;
    padding: 20px 24px 16px;
  }
  
  .el-dialog__body {
    padding: 24px;
  }
  
  .el-form-item__label {
    color: #374151;
    font-weight: 500;
  }
  
  .el-input {
    .el-input__wrapper {
      border-radius: 8px;
      transition: all 0.3s ease;
      
      &:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
      
      &.is-focus {
        box-shadow: 0 4px 12px rgba(16, 163, 127, 0.2);
      }
    }
  }
  
  .el-textarea {
    .el-textarea__inner {
      border-radius: 8px;
      transition: all 0.3s ease;
      
      &:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
      
      &:focus {
        box-shadow: 0 4px 12px rgba(16, 163, 127, 0.2);
      }
    }
  }
  
  .el-select {
    .el-select__wrapper {
      border-radius: 8px;
      transition: all 0.3s ease;
      
      &:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
      
      &.is-focused {
        box-shadow: 0 4px 12px rgba(16, 163, 127, 0.2);
      }
    }
  }
  
  .el-dialog__footer {
    padding: 16px 24px 24px;
    
    .el-button {
      border-radius: 8px;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-1px);
      }
      
      &.el-button--primary {
        background: linear-gradient(135deg, #10a37f, #0d8f6b);
        border: none;
        
        &:hover {
          box-shadow: 0 4px 12px rgba(16, 163, 127, 0.4);
        }
      }
    }
  }
}

// 标签样式
:deep(.el-tag) {
  font-size: 11px;
  height: 20px;
  line-height: 18px;
  border-radius: 10px;
  transition: all 0.2s ease;
  
  &:hover {
    transform: scale(1.05);
  }
  
  &.el-tag--warning {
    background: linear-gradient(135deg, #fef3c7, #fde68a);
    border-color: #f59e0b;
  }
  
  &.el-tag--primary {
    background: linear-gradient(135deg, #dbeafe, #bfdbfe);
    border-color: #3b82f6;
  }
  
  &.el-tag--danger {
    background: linear-gradient(135deg, #fce7f3, #fbcfe8);
    border-color: #ec4899;
  }
  
  &.el-tag--success {
    background: linear-gradient(135deg, #d1fae5, #a7f3d0);
    border-color: #10b981;
  }
}

// 动画关键帧
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 移动端适配
@media (max-width: 768px) {
  .notes-header {
    padding: 12px;
    
    .filter-tabs {
      margin-top: 8px;
      
      :deep(.el-button-group) {
        .el-button {
          font-size: 11px;
          padding: 4px 6px;
        }
      }
    }
  }
  
  .note-item {
    padding: 10px 12px;
    
    &:hover {
      transform: translateX(2px);
    }
  }
  
  .note-content {
    .note-text {
      font-size: 13px;
    }
    
    .note-highlight {
      font-size: 12px;
      padding: 6px;
    }
  }
  
  .note-footer {
    .note-time {
      font-size: 11px;
    }
  }
  
  .notes-footer {
    padding: 12px;
  }
  
  .empty-state {
    padding: 30px 15px;
  }
  
  :deep(.el-dialog) {
    margin: 5vh auto;
    width: 90% !important;
    
    .el-dialog__header {
      padding: 16px 20px 12px;
    }
    
    .el-dialog__body {
      padding: 20px;
    }
    
    .el-dialog__footer {
      padding: 12px 20px 20px;
    }
  }
}

// 平板适配
@media (max-width: 1024px) and (min-width: 769px) {
  .notes-header {
    padding: 14px;
  }
  
  .note-item {
    padding: 11px 14px;
  }
  
  .notes-footer {
    padding: 14px;
  }
  
  :deep(.el-dialog) {
    width: 70% !important;
  }
}
</style>