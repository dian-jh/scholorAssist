import { defineStore } from 'pinia'
import type { MockCategory } from '@/api/mockManager'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/CategoryApi'

// 扩展分类接口，包含层级信息
interface CategoryWithLevel extends MockCategory {
  level?: number
}

interface CategoryState {
  categories: MockCategory[]
  loading: boolean
  selectedCategoryId: string | null
}

export const useCategoryStore = defineStore('category', {
  state: (): CategoryState => ({
    categories: [],
    loading: false,
    selectedCategoryId: null
  }),

  getters: {
    getCategories: (state) => state.categories,
    getLoading: (state) => state.loading,
    getSelectedCategoryId: (state) => state.selectedCategoryId,
    
    getFlatCategories: (state) => {
      const flatCategories: CategoryWithLevel[] = []
      
      function flatten(categories: MockCategory[], level = 0) {
        categories.forEach(category => {
          flatCategories.push({ ...category, level })
          if (category.children && category.children.length > 0) {
            flatten(category.children, level + 1)
          }
        })
      }
      
      flatten(state.categories)
      return flatCategories
    },
    
    getCategoryById: (state) => (id: string) => {
      function findCategory(categories: MockCategory[]): MockCategory | null {
        for (const category of categories) {
          if (category.id === id) {
            return category
          }
          if (category.children) {
            const found = findCategory(category.children)
            if (found) return found
          }
        }
        return null
      }
      
      return findCategory(state.categories)
    }
  },

  actions: {
    async fetchCategories() {
      this.loading = true
      try {
        const response = await getCategoryList()
        this.categories = response.data
        return response
      } catch (error) {
        console.error('获取分类列表失败:', error)
        
        // 提供更详细的错误信息
        if (error instanceof Error) {
          if (error.message.includes('网络')) {
            throw new Error('网络连接失败，请检查网络连接')
          } else if (error.message.includes('超时')) {
            throw new Error('请求超时，请稍后重试')
          } else if (error.message.includes('401') || error.message.includes('权限')) {
            throw new Error('没有权限访问分类数据')
          } else if (error.message.includes('404')) {
            throw new Error('分类服务不可用')
          } else if (error.message.includes('500')) {
            throw new Error('服务器内部错误，请稍后重试')
          }
        }
        
        throw error
      } finally {
        this.loading = false
      }
    },

    async createNewCategory(name: string, parentId?: string) {
      try {
        const response = await createCategory({ name, parent_id: parentId })
        
        if (parentId) {
          // 添加到父分类的children中
          const parentCategory = this.getCategoryById(parentId)
          if (parentCategory) {
            if (!parentCategory.children) {
              parentCategory.children = []
            }
            parentCategory.children.push(response.data)
          }
        } else {
          // 添加到根级分类
          this.categories.push(response.data)
        }
        
        return response
      } catch (error) {
        console.error('创建分类失败:', error)
        throw error
      }
    },

    async updateCategoryInfo(id: string, name: string, parentId?: string) {
      try {
        const response = await updateCategory(id, { name, parent_id: parentId })
        
        // 更新本地数据
        const category = this.getCategoryById(id)
        if (category) {
          category.name = name
          category.parent_id = parentId || null
        }
        
        return response
      } catch (error) {
        console.error('更新分类失败:', error)
        throw error
      }
    },

    async deleteCategoryById(id: string) {
      try {
        const response = await deleteCategory(id)
        
        // 从本地数据中移除
        this.removeCategoryFromTree(id)
        
        return response
      } catch (error) {
        console.error('删除分类失败:', error)
        throw error
      }
    },

    removeCategoryFromTree(id: string) {
      function removeFromArray(categories: MockCategory[]): boolean {
        const index = categories.findIndex(cat => cat.id === id)
        if (index > -1) {
          categories.splice(index, 1)
          return true
        }
        
        for (const category of categories) {
          if (category.children && removeFromArray(category.children)) {
            return true
          }
        }
        
        return false
      }
      
      removeFromArray(this.categories)
    },

    setSelectedCategory(categoryId: string | null) {
      this.selectedCategoryId = categoryId
    },

    updateCategoryDocumentCount(categoryId: string, count: number) {
      const category = this.getCategoryById(categoryId)
      if (category) {
        category.document_count = count
      }
    }
  }
})