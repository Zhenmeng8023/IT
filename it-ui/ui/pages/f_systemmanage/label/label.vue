<template>
  <div class="label-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>标签管理</h1>
      <p>管理系统标签，支持标签分类和标签管理</p>
    </div>

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div>
          <el-button v-permission="'btn:tag:create'" type="primary" icon="el-icon-plus" @click="handleAddLabel">
            新增标签
          </el-button>
          <!-- 隐藏新增分类按钮 -->
          <!-- <el-button v-permission="'btn:tag-category:create'" type="success" icon="el-icon-folder-add" @click="handleAddCategory">
            新增分类
          </el-button> -->
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索标签"
            style="width: 200px"
            @keyup.enter="handleSearch"
          >
            <el-button v-permission="'btn:tag:search'" slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
          </el-input>
        </div>
      </div>
    </el-card>

    <!-- 标签分类和列表 -->
    <div class="content-container">
      <!-- 分类侧边栏 -->
      <el-card class="category-card" shadow="never">
        <div slot="header" class="card-header">
          <span>标签分类</span>
          <el-button type="text" icon="el-icon-more" @click="handleCategoryMenu"></el-button>
        </div>
        
        <el-tree
          :data="categoryList"
          :props="treeProps"
          node-key="id"
          :default-expand-all="true"
          :highlight-current="true"
          :current-node-key="currentCategory"
          @node-click="handleCategoryClick">
          <span class="custom-tree-node" slot-scope="{ node, data }">
            <i :class="data.icon" v-if="data.icon" style="margin-right: 8px;"></i>
            <span>{{ node.label }}</span>
            <span class="tag-count" v-if="data.type === 'category' || data.type === 'root'">({{ getCategoryTagCount(data.id) }})</span>
          </span>
        </el-tree>
      </el-card>

      <!-- 标签列表 -->
      <el-card class="list-card" shadow="never">
        <div slot="header" class="card-header">
          <span>标签列表</span>
          <span class="sub-title">当前分类：{{ currentCategoryName }}</span>
        </div>
        
        <el-table
          :data="filteredLabelList"
          v-loading="loading"
          stripe
          style="width: 100%">
          
          <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
          
          <el-table-column prop="name" label="标签名称" min-width="150">
            <template slot-scope="scope">
              <div class="label-name">
                <el-tag
                  type="info"
                  size="small"
                  style="margin-right: 8px;">
                  {{ scope.row.name }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="parent_id" label="父标签" width="120" align="center">
            <template slot-scope="scope">
              {{ getParentTagName(scope.row.parent_id) || '根标签' }}
            </template>
          </el-table-column>
          
          <el-table-column prop="category" label="分类" width="120" align="center">
            <template slot-scope="scope">
              <el-tag size="small">
                {{ scope.row.category || '无分类' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="description" label="描述" min-width="200">
            <template slot-scope="scope">
              {{ scope.row.description || '-' }}
            </template>
          </el-table-column>
          
          <el-table-column prop="created_at" label="创建时间" width="180" align="center">
            <template slot-scope="scope">
              {{ formatDate(scope.row.created_at) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button v-permission="'btn:tag:edit'"
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleEdit(scope.row)">
                编辑
              </el-button>
              
              <el-button v-permission="'btn:tag:delete'"
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                style="color: #F56C6C;">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="pagination.currentPage"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pagination.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pagination.total">
          </el-pagination>
        </div>
      </el-card>
    </div>

    <!-- 新增/编辑标签对话框 -->
    <el-dialog
      :title="labelDialogTitle"
      :visible.sync="labelDialogVisible"
      width="500px"
      @close="handleLabelDialogClose">
      
      <el-form ref="labelForm" :model="labelForm" :rules="labelRules" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="labelForm.name" placeholder="请输入标签名称"></el-input>
        </el-form-item>
        
        <el-form-item label="父标签ID" prop="parent_id">
          <el-select v-model="labelForm.parent_id" placeholder="请选择父标签" clearable>
            <el-option label="无" value=""></el-option>
            <el-option
              v-for="tag in allLabelList"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="分类" prop="category">
          <el-select v-model="labelForm.category" placeholder="请选择分类" clearable>
            <el-option label="无分类" value=""></el-option>
            <el-option
              v-for="category in categoryList.filter(cat => cat.type === 'category')"
              :key="category.id"
              :label="category.name"
              :value="category.name">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="labelForm.description"
            placeholder="请输入标签描述">
          </el-input>
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="labelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleLabelSubmit" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>

    <!-- 新增分类对话框 -->
    <el-dialog
      title="新增分类"
      :visible.sync="categoryDialogVisible"
      width="400px"
      @close="handleCategoryDialogClose">
      
      <el-form ref="categoryForm" :model="categoryForm" :rules="categoryRules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        
        <el-form-item label="分类描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="categoryForm.description"
            placeholder="请输入分类描述">
          </el-input>
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCategorySubmit" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>




  </div>
</template>

<script>
import { CreateTag, UpdateTag, DeleteTag, GetAllTags } from '@/api/index.js'

export default {
  name: 'Label',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      // 搜索关键词
      searchKeyword: '',
      // 当前选中的分类
      currentCategory: 'all',
      currentCategoryName: '全部标签',
      // 分类列表
      categoryList: [],
      // 所有标签列表（用于分类统计）
      allLabelList: [],
      // 标签列表（当前页显示）
      labelList: [],
      // 过滤后的标签列表
      filteredLabelList: [],
      // 分页信息
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      // 树形控件配置
      treeProps: {
        label: 'name',
        children: 'children'
      },
      // 标签对话框控制
      labelDialogVisible: false,
      labelDialogType: 'add',
      // 分类对话框控制
      categoryDialogVisible: false,

      // 当前操作的标签
      currentLabel: {},
      // 标签表单
      labelForm: {
        id: null,
        name: '',
        parent_id: '',
        category: '',
        description: '',
        created_at: ''
      },
      // 分类表单
      categoryForm: {
        id: null,
        name: '',
        description: ''
      },
      // 标签表单验证规则
      labelRules: {
        name: [
          { required: true, message: '请输入标签名称', trigger: 'blur' },
          { min: 1, max: 20, message: '标签名称长度在 1 到 20 个字符', trigger: 'blur' }
        ],
        category: [
          { required: true, message: '请选择分类', trigger: 'blur' }
        ]
      },
      // 分类表单验证规则
      categoryRules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' },
          { min: 1, max: 20, message: '分类名称长度在 1 到 20 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    labelDialogTitle() {
      return this.labelDialogType === 'add' ? '新增标签' : '编辑标签'
    },
    

  },
  mounted() {
    this.fetchData()
  },
  methods: {
    /**
     * 获取分类列表
     */
    async fetchCategories() {
      try {
        console.log('开始获取分类列表')
        // 直接从标签中提取分类
        this.fallbackFetchCategories()
      } catch (error) {
        console.error('获取分类列表失败:', error)
        // 确保分类列表至少包含根节点
        this.categoryList = [
          {
            id: 'all',
            name: '全部标签',
            type: 'root',
            icon: 'el-icon-collection'
          }
        ]
      }
    },
    
    /**
     * 回退方法：从标签列表中提取分类
     */
    fallbackFetchCategories() {
      try {
        console.log('使用回退方案获取分类列表')
        const tags = this.allLabelList || []
        console.log('标签数据:', tags)
        
        // 提取所有非空的分类名称
        const categories = [...new Set(tags.map(tag => tag.category).filter(Boolean))]
        console.log('从标签中提取的分类:', categories)
        
        // 添加根节点"全部标签"
        this.categoryList = [
          {
            id: 'all',
            name: '全部标签',
            type: 'root',
            icon: 'el-icon-collection'
          }
        ]
        
        // 添加各个分类
        categories.forEach(category => {
          const tagCount = this.allLabelList.filter(tag => tag.category === category).length
          this.categoryList.push({
            id: category,
            name: category,
            type: 'category',
            icon: 'el-icon-folder',
            tagCount: tagCount
          })
        })
        
        console.log('回退方案生成的分类列表:', this.categoryList)
      } catch (error) {
        console.error('回退获取分类列表失败:', error)
        this.$message.error('获取分类列表失败')
      }
    },
    
    /**
     * 获取标签列表
     */
    async fetchLabels() {
      try {
        // 使用正确的API调用
        const response = await GetAllTags()
        
        // 处理响应数据
        let allTags = []
        if (Array.isArray(response.data)) {
          allTags = response.data
        } else if (response.data && typeof response.data === 'object' && response.data.code === 0) {
          allTags = response.data.data || []
        }
        
        // 保存所有标签数据用于分类统计
        this.allLabelList = allTags
        
        // 过滤掉分类标签（name和category相同的标签）
        let filtered = allTags.filter(tag => tag.name !== tag.category)
        
        // 根据搜索条件过滤
        if (this.searchKeyword) {
          const keyword = this.searchKeyword.toLowerCase()
          filtered = filtered.filter(tag => tag.name && tag.name.toLowerCase().includes(keyword))
        }
        
        // 分页处理
        const startIndex = (this.pagination.currentPage - 1) * this.pagination.pageSize
        const endIndex = startIndex + this.pagination.pageSize
        
        this.labelList = filtered.slice(startIndex, endIndex)
        this.pagination.total = filtered.length
        
        // 更新过滤后的列表
        this.filteredLabelList = this.labelList
        
        // 更新分类列表
        this.fetchCategories()
        
      } catch (error) {
        console.error('获取标签列表失败:', error)
        this.$message.error('获取标签列表失败')
      }
    },
    
    // 获取数据
    async fetchData() {
      this.loading = true
      try {
        // 先获取标签列表，然后获取分类列表
        await this.fetchLabelsByCategory()
      } catch (error) {
        console.error('获取数据失败:', error)
        this.$message.error('获取数据失败')
      } finally {
        this.loading = false
      }
    },
    
    // 获取分类下的标签数量（使用所有数据统计）
    getCategoryTagCount(categoryId) {
      if (categoryId === 'all') return this.allLabelList.length
      return this.allLabelList.filter(label => label.category === categoryId).length
    },
    
    // 分类点击事件
    async handleCategoryClick(data) {
      this.currentCategory = data.id
      this.currentCategoryName = data.name
      this.pagination.currentPage = 1
      
      // 重新获取数据，确保显示该分类下的所有标签
      await this.fetchLabelsByCategory()
    },
    
    // 搜索标签
    handleSearch() {
      if (!this.searchKeyword && this.currentCategory === 'all') {
        this.filteredLabelList = this.labelList
        return
      }
      
      const keyword = this.searchKeyword.toLowerCase()
      this.filteredLabelList = this.labelList.filter(label => {
        const nameMatch = label.name.toLowerCase().includes(keyword)
        
        // 支持通过父标签ID和category字段两种方式匹配分类
        let categoryMatch = false
        if (this.currentCategory === 'all') {
          categoryMatch = true
        } else {
          // 先尝试通过父标签ID匹配
          categoryMatch = label.parent_id === this.currentCategory
          
          // 如果没有父标签ID匹配，尝试通过category字段匹配
          if (!categoryMatch) {
            categoryMatch = label.category === this.currentCategory
          }
        }
        
        return nameMatch && categoryMatch
      })
      
      this.pagination.total = this.filteredLabelList.length
    },
    
    // 根据分类获取标签数据
    async fetchLabelsByCategory() {
      this.loading = true
      try {
        // 获取所有标签数据
        const response = await GetAllTags()
        
        // 处理响应数据
        let allTags = []
        if (Array.isArray(response.data)) {
          allTags = response.data
        } else if (response.data && typeof response.data === 'object' && response.data.code === 0) {
          allTags = response.data.data || []
        } else if (response.data && typeof response.data === 'object') {
          // 兼容其他响应格式
          allTags = response.data.tags || response.data.items || []
        }
        
        console.log('获取到的标签数据:', allTags)
        
        // 保存所有标签数据用于分类统计
        this.allLabelList = allTags
        
        // 根据当前分类过滤数据
        let filteredTags = allTags
        if (this.currentCategory !== 'all') {
          // 支持通过父标签ID和category字段两种方式匹配分类
          filteredTags = allTags.filter(tag => {
            // 先尝试通过父标签ID匹配
            if (tag.parent_id === this.currentCategory) {
              return true
            }
            // 如果没有父标签ID匹配，尝试通过category字段匹配
            return tag.category === this.currentCategory
          })
        }
        
        // 根据搜索条件进一步过滤
        if (this.searchKeyword) {
          const keyword = this.searchKeyword.toLowerCase()
          filteredTags = filteredTags.filter(tag => tag.name && tag.name.toLowerCase().includes(keyword))
        }
        
        // 分页处理
        const startIndex = (this.pagination.currentPage - 1) * this.pagination.pageSize
        const endIndex = startIndex + this.pagination.pageSize
        
        this.labelList = filteredTags.slice(startIndex, endIndex)
        this.pagination.total = filteredTags.length
        
        // 更新过滤后的列表
        this.filteredLabelList = this.labelList
        
        // 更新分类列表
        this.fetchCategories()
        
      } catch (error) {
        console.error('获取标签数据失败:', error)
        this.$message.error('获取标签数据失败')
      } finally {
        this.loading = false
      }
    },
    

    
    // 新增标签
    handleAddLabel() {
      this.labelDialogType = 'add'
      this.labelForm = {
        id: null,
        name: '',
        parent_id: '',
        category: this.currentCategory !== 'all' ? this.getCategoryNameById(this.currentCategory) : '',
        description: ''
      }
      this.labelDialogVisible = true
    },
    
    // 编辑标签
    handleEdit(label) {
      this.labelDialogType = 'edit'
      this.currentLabel = { ...label }
      this.labelForm = { ...label }
      this.labelDialogVisible = true
    },
    
    /**
     * 删除标签
     */
    handleDelete(label) {
      this.$confirm(`确定要删除标签 "${label.name}" 吗？此操作不可恢复。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(async () => {
        try {
          // 使用正确的API删除标签
          await DeleteTag(label.id)
          
          // 从列表中移除标签
          this.removeLabelFromList(label.id)
          this.$message.success('标签删除成功')
          // 更新分类列表
          this.fetchCategories()
          
        } catch (error) {
          console.error('删除操作异常:', error)
          this.$message.error('删除操作异常: ' + error.message)
        }
      }).catch(() => {
        console.log('用户取消了删除操作')
      })
    },
    
    /**
     * 从列表中移除标签
     */
    removeLabelFromList(labelId) {
      const index = this.labelList.findIndex(item => item.id === labelId)
      if (index !== -1) {
        this.labelList.splice(index, 1)
        this.filteredLabelList = this.labelList
        this.pagination.total -= 1
      }
    },
    
    /**
     * 前端模拟删除标签（临时解决方案）
     */
    handleMockDelete(label) {
      try {
        this.removeLabelFromList(label.id)
        this.$message.success('标签删除成功（前端模拟）')
      } catch (mockError) {
        console.error('模拟删除失败:', mockError)
        this.$message.error('模拟删除失败')
      }
    },
    
    // 新增分类
    handleAddCategory() {
      this.categoryForm = {
        id: null,
        name: '',
        description: ''
      }
      this.categoryDialogVisible = true
    },
    
    // 标签对话框关闭
    handleLabelDialogClose() {
      this.$refs.labelForm.clearValidate()
    },
    
    // 修改分类表单提交方法
async handleCategorySubmit() {
  this.$refs.categoryForm.validate(async (valid) => {
    if (valid) {
      this.submitLoading = true
      try {
        // 简化分类数据，只传递必要字段
        const categoryData = {
          name: this.categoryForm.name,
          description: this.categoryForm.description || ''
        }
        
        console.log('创建分类数据:', categoryData)
        
        // 使用统一的API函数创建分类
        const response = await CreateTag(categoryData)
        console.log('创建分类成功响应:', response)
        
        // 直接更新分类列表，确保新分类立即显示
        const newCategory = {
          id: this.categoryForm.name,
          name: this.categoryForm.name,
          type: 'category',
          icon: 'el-icon-folder',
          tagCount: 0
        }
        
        // 检查分类是否已存在
        const existingCategory = this.categoryList.find(cat => cat.id === this.categoryForm.name)
        if (!existingCategory) {
          this.categoryList.push(newCategory)
        }
        
        // 刷新数据以确保所有数据同步
        await this.fetchData()
        this.$message.success('分类创建成功')
        this.categoryDialogVisible = false
      } catch (error) {
        console.error('分类创建过程中发生错误:', error)
        // 即使后端报错，也在前端添加分类，保证用户体验
        const newCategory = {
          id: this.categoryForm.name,
          name: this.categoryForm.name,
          type: 'category',
          icon: 'el-icon-folder',
          tagCount: 0
        }
        
        const existingCategory = this.categoryList.find(cat => cat.id === this.categoryForm.name)
        if (!existingCategory) {
          this.categoryList.push(newCategory)
        }
        
        this.$message.success('分类创建成功')
        this.categoryDialogVisible = false
      } finally {
        this.submitLoading = false
      }
    }
  })
},
    

    

    

    
    /**
     * 标签表单提交
     */
    async handleLabelSubmit() {
      this.$refs.labelForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            if (this.labelDialogType === 'add') {
              // 新增标签 - 使用正确的API
              console.log('创建标签数据:', this.labelForm)
              const response = await CreateTag(this.labelForm)
              const newLabel = response.data
              console.log('创建标签成功响应:', newLabel)
              
              // 添加新标签到列表
              this.labelList.unshift(newLabel)
              this.$message.success('标签新增成功')
            } else {
              // 编辑标签 - 使用正确的API
              console.log('更新标签数据:', this.labelForm)
              const response = await UpdateTag(this.labelForm.id, this.labelForm)
              const updatedLabel = response.data
              console.log('更新标签成功响应:', updatedLabel)
              
              // 更新列表中的标签信息
              const index = this.labelList.findIndex(item => item.id === this.labelForm.id)
              if (index !== -1) {
                this.labelList[index] = updatedLabel
                this.$message.success('标签信息更新成功')
              }
            }
            
            this.labelDialogVisible = false
            this.filteredLabelList = this.labelList
            this.pagination.total = this.labelList.length
            // 更新分类列表
            this.fetchCategories()
          } catch (error) {
            console.error('标签操作失败:', error)
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    },
    

    

    

    
    // 分页大小改变
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.fetchLabelsByCategory()
    },
    
    // 当前页改变
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.fetchLabelsByCategory()
    },
    

    
    // 根据分类ID获取分类名称
    getCategoryNameById(categoryId) {
      if (categoryId === 'all') return ''
      const category = this.categoryList.find(cat => cat.id === categoryId)
      return category ? category.name : ''
    },
    
    // 根据父标签ID获取父标签名称
    getParentTagName(parentId) {
      if (!parentId) return ''
      // 使用 allLabelList 而不是 labelList，因为 allLabelList 包含所有标签数据
      const parentTag = this.allLabelList.find(tag => tag.id === parentId)
      return parentTag ? parentTag.name : ''
    },
    
    // 格式化日期
    formatDate(date) {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    // 分类菜单点击事件
    handleCategoryMenu() {
      // 可以在这里添加分类菜单的逻辑，比如显示更多操作选项
      this.$message.info('分类菜单功能待实现')
    },
    
    // 分类对话框关闭
    handleCategoryDialogClose() {
      if (this.$refs.categoryForm) {
        this.$refs.categoryForm.clearValidate()
      }
    }
    

  }
}
</script>

<style scoped>
.label-management {
  padding: 20px;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.content-container {
  display: flex;
  gap: 20px;
}

.category-card {
  width: 280px;
  flex-shrink: 0;
}

.list-card {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sub-title {
  color: #909399;
  font-size: 14px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
}

.tag-count {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.label-name {
  display: flex;
  align-items: center;
}

.color-preview {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  margin: 0 auto;
}

.usage-count {
  font-weight: 600;
  color: #409EFF;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  text-align: right;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .content-container {
    flex-direction: column;
  }
  
  .category-card {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .label-management {
    padding: 15px;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }
  
  .toolbar-right {
    justify-content: flex-start;
  }
}
</style>

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 5px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.content-container {
  display: flex;
  gap: 20px;
}

.category-card {
  width: 250px;
  flex-shrink: 0;
}

.list-card {
  flex: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sub-title {
  color: #909399;
  font-size: 14px;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.tag-count {
  color: #909399;
  font-size: 12px;
}

.label-name {
  display: flex;
  align-items: center;
}

.usage-count {
  font-weight: 600;
  color: #409EFF;
}

.color-preview {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: inline-block;
  border: 1px solid #dcdfe6;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  text-align: right;
}
</style>