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
                v-if="note.tags && note.tags.length > 0"
                :type="getTagColor(note.tags[0])" 
                size="small"
              >
                {{ note.tags[0] }}
              </el-tag>
              <span class="note-page" v-if="getPageNumber(note.coord)">第{{ getPageNumber(note.coord) }}页</span>
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
            <p class="note-text" v-if="note.content">{{ note.content }}</p>
            <div class="note-highlight" v-if="note.referenceText">
              <el-icon><EditPen /></el-icon>
              <span>"{{ note.referenceText }}"</span>
            </div>
          </div>
          
          <div class="note-footer">
            <span class="note-time">{{ formatTime(note.createTime) }}</span>
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
        
        <el-form-item label="引用文本" v-if="newNote.referenceText">
          <el-input
            v-model="newNote.referenceText"
            type="textarea"
            :rows="2"
            disabled
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

        <el-form-item label="颜色">
           <el-color-picker v-model="newNote.color" />
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
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  Plus, 
  EditPen, 
  MoreFilled 
} from '@element-plus/icons-vue'
import { 
  searchNotes, 
  createNote as createNoteApi, 
  updateNote as updateNoteApi, 
  deleteNote as deleteNoteApi,
} from '@/api/NotesApi'
import type { NoteDTO, NoteCreateRequest } from '@/types/note'

// Props
interface Props {
  documentId?: string
  notes?: NoteDTO[] // 从父组件传入的笔记列表
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  noteSelect: [noteId: string]
  noteCreate: [content: string]
  noteUpdate: [noteId: string]
  noteDelete: [noteId: string]
  'refresh-notes': []
}>()

// 响应式数据
const searchQuery = ref('')
const activeCategory = ref('all')
const selectedNoteId = ref('')
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const loading = ref(false)
// const notes = ref<NoteDTO[]>([]) // 移除内部 notes 状态，使用 props.notes
const editingNote = ref<NoteDTO | null>(null)

const newNote = ref({
  content: '',
  referenceText: '',
  coord: '{}',
  tags: [] as string[],
  color: '#FFCC00'
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

// 分类配置 (映射为标签筛选)
const categories = [
  { key: 'all', label: '全部' },
  { key: '重点', label: '重点' },
  { key: '疑问', label: '疑问' },
  { key: '总结', label: '总结' }
]

// 计算属性
const filteredNotes = computed(() => {
  let filtered = props.notes || []

  // 按分类筛选 (标签)
  if (activeCategory.value !== 'all') {
    filtered = filtered.filter(note => note.tags && note.tags.includes(activeCategory.value))
  }

  // 按搜索关键词筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(note => 
      (note.content && note.content.toLowerCase().includes(query)) ||
      (note.referenceText && note.referenceText.toLowerCase().includes(query))
    )
  }

  // 按创建时间倒序排列 (假设 createTime 是字符串)
  return filtered.slice().sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
})

// 方法

const getPageNumber = (coordJson: string): number | null => {
  try {
    const coord = JSON.parse(coordJson)
    return coord.page || null
  } catch (e) {
    return null
  }
}

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

const setActiveCategory = (category: string) => {
  activeCategory.value = category
}

const selectNote = (note: NoteDTO) => {
  selectedNoteId.value = note.id
  emit('noteSelect', note.id)
  
  // 尝试解析坐标并跳转 (通常由父组件处理，这里只需emit)
}

const createNote = () => {
  showCreateDialog.value = true
  editingNote.value = null
  // 重置表单
  newNote.value = {
    content: '',
    referenceText: '',
    coord: JSON.stringify({ page: 1 }), // 默认第一页，实际应由外部传入或获取当前页
    tags: [],
    color: '#FFCC00'
  }
}

const editNote = (note: NoteDTO) => {
  showEditDialog.value = true
  editingNote.value = note
  // 填充表单
  newNote.value = {
    content: note.content || '',
    referenceText: note.referenceText || '',
    coord: note.coord,
    tags: [...(note.tags || [])],
    color: note.color || '#FFCC00'
  }
}

const saveNote = async () => {
  if (!props.documentId) {
    ElMessage.error('文档ID不能为空')
    return
  }

  loading.value = true
  try {
    if (editingNote.value) {
      // 更新笔记
      const updateData = {
        id: editingNote.value.id,
        content: newNote.value.content,
        color: newNote.value.color,
        tags: newNote.value.tags
      }
      const response = await updateNoteApi(updateData)
      if (response.code === 200) {
        ElMessage.success('笔记更新成功')
        emit('noteUpdate', editingNote.value.id)
        emit('refresh-notes')
      }
    } else {
      // 创建新笔记
      const createData: NoteCreateRequest = {
        documentId: props.documentId,
        content: newNote.value.content,
        referenceText: newNote.value.referenceText,
        coord: newNote.value.coord,
        color: newNote.value.color,
        tags: newNote.value.tags
      }
      
      const response = await createNoteApi(createData)
      if (response.code === 200) {
        ElMessage.success('笔记创建成功')
        emit('noteCreate', newNote.value.content)
        emit('refresh-notes')
      }
    }
    
    showCreateDialog.value = false
    showEditDialog.value = false
  } catch (error) {
    console.error('保存笔记失败:', error)
    ElMessage.error('保存笔记失败')
  } finally {
    loading.value = false
  }
}

const handleDialogClose = () => {
  showCreateDialog.value = false
  showEditDialog.value = false
  editingNote.value = null
}

const handleNoteAction = async (command: string) => {
  const [action, noteId] = command.split('-')
  
  if (action === 'edit') {
    const note = props.notes?.find(n => n.id === noteId)
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
          emit('refresh-notes')
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

const getTagColor = (tag: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  if (tag === '重点') return 'warning'
  if (tag === '疑问') return 'danger'
  if (tag === '总结') return 'success'
  return 'primary'
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
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
      box-shadow: 0 4px 12px rgba(16, 163, 127, 0.3);
      
      &::before {
        left: 100%;
      }
    }
    
    &:active {
      transform: translateY(0);
    }
  }
}
</style>
