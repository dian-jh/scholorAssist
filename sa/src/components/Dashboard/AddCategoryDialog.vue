<template>
  <el-dialog
    v-model="visible"
    title="添加分类"
    width="400px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item label="父分类" prop="parentId">
        <el-select v-model="form.parentId" placeholder="选择父分类（可选）" clearable>
          <el-option label="无（根分类）" value="" />
          <el-option 
            v-for="category in flatCategories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useCategoryStore } from '@/store'

interface Props {
  visible: boolean
  parentId?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const categoryStore = useCategoryStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const flatCategories = computed(() => categoryStore.getFlatCategories)

const form = reactive({
  name: '',
  parentId: props.parentId || ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 20, message: '分类名称长度在1到20个字符', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true
    
    await categoryStore.createNewCategory(form.name, form.parentId || undefined)
    
    ElMessage.success('分类创建成功')
    emit('success')
    
    // 重置表单
    form.name = ''
    form.parentId = ''
  } catch (error) {
    console.error('创建分类失败:', error)
  } finally {
    loading.value = false
  }
}
</script>