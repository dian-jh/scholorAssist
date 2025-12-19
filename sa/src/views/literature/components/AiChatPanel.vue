<template>
  <div class="ai-chat-panel">
    <div class="chat-header">
      <span class="title">AI 对话</span>
      <div>
        <el-button @click="showHistory = true">历史</el-button>
        <el-button @click="showSettings = true">设置</el-button>
      </div>
    </div>
    <div class="chat-messages">
      <!-- Chat messages will go here -->
    </div>
    <div class="chat-input">
      <el-input
        v-model="newMessage"
        placeholder="请输入您的问题..."
        @keyup.enter="sendMessage"
      />
      <el-button type="primary" @click="sendMessage">发送</el-button>
    </div>

    <el-drawer
      v-model="showHistory"
      title="对话历史"
      direction="rtl"
      size="300px"
    >
      <div class="history-content">
        <div
          v-for="conversation in conversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ active: conversation.id === currentConversationId }"
          @click="loadConversation(conversation.id)"
        >
          <div class="conversation-title">{{ conversation.title }}</div>
          <div class="conversation-time">{{ conversation.last_message_time }}</div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="showSettings" title="AI 设置" width="400px">
      <el-form label-width="80px">
        <el-form-item label="模型">
          <el-select v-model="aiModel">
            <el-option
              v-for="model in availableModels"
              :key="model.id"
              :label="model.name"
              :value="model.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="温度">
          <el-slider v-model="temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getConversationList, getAvailableModels } from '@/api/AiApi'

const newMessage = ref('')
const showHistory = ref(false)
const showSettings = ref(false)
const conversations = ref<any[]>([])
const currentConversationId = ref<string | null>(null)
const aiModel = ref('gpt-3.5-turbo')
const availableModels = ref<any[]>([])
const temperature = ref(0.7)

const sendMessage = () => {
  if (newMessage.value.trim() === '') return
  console.log('sending message:', newMessage.value)
  newMessage.value = ''
}

const loadConversation = (id: string) => {
  currentConversationId.value = id
  console.log('loading conversation:', id)
}

const saveSettings = () => {
  console.log('saving settings:', { model: aiModel.value, temperature: temperature.value })
  showSettings.value = false
}

onMounted(async () => {
  try {
    const historyResponse = await getConversationList()
    // 兼容后端字段：last_message_at -> last_message_time
    conversations.value = (historyResponse.data.conversations || []).map((c: any) => ({
      ...c,
      last_message_time: c.last_message_at ?? c.last_message_time
    }))

    const modelsResponse = await getAvailableModels()
    // API返回为字符串数组，映射为下拉所需的 {id, name}
    availableModels.value = (modelsResponse.data || []).map((m: string) => ({ id: m, name: m }))
  } catch (error) {
    console.error('Failed to load initial data:', error)
  }
})
</script>

<style scoped lang="scss">
.ai-chat-panel {
  position: fixed;
  right: 0;
  top: 60px; /* Assuming a header of 60px */
  width: 400px;
  height: calc(100vh - 60px);
  background-color: #fff;
  border-left: 1px solid #dcdfe6;
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .title {
    font-size: 18px;
    font-weight: bold;
  }
}

.chat-messages {
  flex-grow: 1;
  overflow-y: auto;
  margin-bottom: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
}

.chat-input {
  display: flex;
}

.history-content {
  padding: 0 10px;
}

.conversation-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;

  &:hover {
    background-color: #f5f7fa;
  }

  &.active {
    background-color: #ecf5ff;
  }

  .conversation-title {
    font-size: 14px;
    font-weight: 500;
  }

  .conversation-time {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>