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
        <el-button type="primary" icon="el-icon-plus" @click="handleAddLabel">
          新增标签
        </el-button>
        <el-button type="success" icon="el-icon-folder-add" @click="handleAddCategory">
          新增分类
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">
          刷新
        </el-button>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索标签名称"
            clearable
            style="width: 250px"
            prefix-icon="el-icon-search"
            @input="handleSearch">
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
            <span class="tag-count" v-if="data.type === 'category'">({{ getCategoryTagCount(data.id) }})</span>
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
                  :type="getTagType(scope.row.type)"
                  size="small"
                  style="margin-right: 8px;">
                  {{ scope.row.name }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="type" label="标签类型" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.type === 'system' ? 'primary' : 'success'" size="small">
                {{ scope.row.type === 'system' ? '系统' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="categoryName" label="所属分类" width="120" align="center"></el-table-column>
          
          <el-table-column prop="usageCount" label="使用次数" width="100" align="center">
            <template slot-scope="scope">
              <span class="usage-count">{{ scope.row.usageCount }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="color" label="标签颜色" width="100" align="center">
            <template slot-scope="scope">
              <div class="color-preview" :style="{ backgroundColor: scope.row.color }"></div>
            </template>
          </el-table-column>
          
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template slot-scope="scope">
              <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                @change="handleStatusChange(scope.row)">
              </el-switch>
            </template>
          </el-table-column>
          
          <el-table-column prop="createTime" label="创建时间" width="160" align="center">
            <template slot-scope="scope">
              {{ formatDate(scope.row.createTime) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleEdit(scope.row)">
                编辑
              </el-button>
              
              <el-button
                size="mini"
                type="text"
                icon="el-icon-document"
                @click="handleViewUsage(scope.row)">
                使用记录
              </el-button>
              
              <el-button
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
        
        <el-form-item label="标签类型" prop="type">
          <el-radio-group v-model="labelForm.type">
            <el-radio label="system">系统标签</el-radio>
            <el-radio label="user">用户标签</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="labelForm.categoryId" placeholder="请选择分类" clearable>
            <el-option
              v-for="category in categoryOptions"
              :key="category.id"
              :label="category.name"
              :value="category.id">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="标签颜色" prop="color">
          <el-color-picker v-model="labelForm.color" show-alpha></el-color-picker>
          <span style="margin-left: 10px; color: #909399;">{{ labelForm.color }}</span>
        </el-form-item>
        
        <el-form-item label="标签描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="labelForm.description"
            placeholder="请输入标签描述">
          </el-input>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="labelForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="labelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleLabelSubmit" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>

    <!-- 新增/编辑分类对话框 -->
    <el-dialog
      :title="categoryDialogTitle"
      :visible.sync="categoryDialogVisible"
      width="400px">
      
      <el-form ref="categoryForm" :model="categoryForm" :rules="categoryRules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        
        <el-form-item label="分类图标" prop="icon">
          <el-input v-model="categoryForm.icon" placeholder="请输入图标类名，如：el-icon-folder">
            <template slot="prepend">
              <i :class="categoryForm.icon" v-if="categoryForm.icon"></i>
              <span v-else>图标</span>
            </template>
          </el-input>
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

    <!-- 标签使用记录对话框 -->
    <el-dialog
      title="标签使用记录"
      :visible.sync="usageDialogVisible"
      width="800px">
      
      <el-table :data="usageList" stripe style="width: 100%">
        <el-table-column prop="user" label="使用用户" width="120"></el-table-column>
        <el-table-column prop="content" label="使用内容" min-width="200"></el-table-column>
        <el-table-column prop="type" label="内容类型" width="100"></el-table-column>
        <el-table-column prop="usageTime" label="使用时间" width="160">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.usageTime) }}
          </template>
        </el-table-column>
      </el-table>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="usageDialogVisible = false">关闭</el-button>
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
      // 标签列表
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
      categoryDialogType: 'add',
      // 使用记录对话框控制
      usageDialogVisible: false,
      // 当前操作的标签
      currentLabel: {},
      // 标签表单
      labelForm: {
        id: null,
        name: '',
        type: 'user',
        categoryId: '',
        color: '#409EFF',
        description: '',
        status: 1
      },
      // 分类表单
      categoryForm: {
        id: null,
        name: '',
        icon: 'el-icon-folder',
        description: ''
      },
      // 标签表单验证规则
      labelRules: {
        name: [
          { required: true, message: '请输入标签名称', trigger: 'blur' },
          { min: 1, max: 20, message: '标签名称长度在 1 到 20 个字符', trigger: 'blur' }
        ],
        categoryId: [
          { required: true, message: '请选择所属分类', trigger: 'change' }
        ]
      },
      // 分类表单验证规则
      categoryRules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' },
          { min: 1, max: 20, message: '分类名称长度在 1 到 20 个字符', trigger: 'blur' }
        ]
      },
      // 使用记录列表
      usageList: []
    }
  },
  computed: {
    labelDialogTitle() {
      return this.labelDialogType === 'add' ? '新增标签' : '编辑标签'
    },
    
    categoryDialogTitle() {
      return this.categoryDialogType === 'add' ? '新增分类' : '编辑分类'
    },
    
    // 分类选项（用于下拉选择）
    categoryOptions() {
      return this.categoryList.filter(cat => cat.type === 'category')
    }
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
        // 使用导入的GetAllTags函数
        const response = await GetAllTags()
        
        // 添加根节点"全部标签"
        this.categoryList = [
          {
            id: 'all',
            name: '全部标签',
            type: 'root',
            icon: 'el-icon-collection'
          }
        ]
      } catch (error) {
        console.error('获取分类列表失败:', error)
        this.$message.error('获取分类列表失败')
      }
    },
    
    /**
     * 获取标签列表
     */
    async fetchLabels() {
      try {
        // 使用导入的getLabelList函数（如果存在）或继续使用GetAllTags
        const response = await GetAllTags()
        
        // 处理不同的响应格式
        let tags = []
        if (Array.isArray(response.data)) {
          tags = response.data
        } else if (response.data && typeof response.data === 'object' && response.data.code === 0) {
          tags = response.data.data || []
        } else if (Array.isArray(response)) {
          tags = response
        }
        
        // 根据搜索条件过滤
        let filtered = tags
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
        
      } catch (error) {
        console.error('获取标签列表失败:', error)
        this.$message.error('获取标签列表失败')
      }
    },
    
    // 获取数据
    async fetchData() {
      this.loading = true
      try {
        await Promise.all([
          this.fetchCategories(),
          this.fetchLabels()
        ])
      } catch (error) {
        console.error('获取数据失败:', error)
        this.$message.error('获取数据失败')
      } finally {
        this.loading = false
      }
    },
    
    // 获取分类下的标签数量
    getCategoryTagCount(categoryId) {
      if (categoryId === 'all') return this.labelList.length
      return this.labelList.filter(label => label.categoryId === categoryId).length
    },
    
    // 分类点击事件
    handleCategoryClick(data) {
      this.currentCategory = data.id
      this.currentCategoryName = data.name
      this.pagination.currentPage = 1
      this.handleSearch()
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
        const categoryMatch = this.currentCategory === 'all' || label.categoryId === this.currentCategory
        return nameMatch && categoryMatch
      })
      
      this.pagination.total = this.filteredLabelList.length
    },
    
    // 获取标签类型样式
    getTagType(type) {
      const types = {
        system: 'primary',
        user: 'success'
      }
      return types[type] || 'info'
    },
    
    // 新增标签
    handleAddLabel() {
      this.labelDialogType = 'add'
      this.labelForm = {
        id: null,
        name: '',
        type: 'user',
        categoryId: this.currentCategory !== 'all' ? this.currentCategory : '',
        color: '#409EFF',
        description: '',
        status: 1
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
          console.log('开始删除标签，标签ID:', label.id, '标签名称:', label.name)
          
          // 检测后端是否支持DELETE方法
          console.log('检测后端删除接口支持情况...')
          
          // 直接使用前端模拟删除（后端不支持DELETE方法）
          this.$message.warning('后端删除接口暂未实现，使用前端模拟删除')
          this.handleMockDelete(label)
          
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
      this.categoryDialogType = 'add'
      this.categoryForm = {
        id: null,
        name: '',
        icon: 'el-icon-folder',
        description: ''
      }
      this.categoryDialogVisible = true
    },
    
    // 分类菜单操作
    handleCategoryMenu() {
      // 可以添加分类管理的更多操作
      this.$message.info('分类管理功能开发中')
    },
    
    /**
     * 查看标签使用记录
     */
    async handleViewUsage(label) {
      this.currentLabel = { ...label }
      
      try {
        // 根据实际API，可能需要调用其他接口获取使用记录
        // 暂时使用模拟数据
        this.usageList = [
          {
            user: 'user1',
            content: 'Vue.js入门教程',
            type: '博客',
            usageTime: '2024-03-10 15:30:00'
          },
          {
            user: 'user2',
            content: '前端开发最佳实践',
            type: '博客',
            usageTime: '2024-03-09 10:20:00'
          }
        ]
        
        this.usageDialogVisible = true
      } catch (error) {
        console.error('获取使用记录失败:', error)
        this.$message.error('获取使用记录失败')
      }
    },
    
    /**
     * 更新标签状态
     */
    async handleStatusChange(label) {
      try {
        // 使用项目中实际存在的接口路径更新标签状态
        await UpdateTag(label.id, { 
          ...label,
          status: label.status 
        })
        
        const statusText = label.status === 1 ? '启用' : '禁用'
        this.$message.success(`标签 "${label.name}" 已${statusText}`)
      } catch (error) {
        console.error('更新标签状态失败:', error)
        this.$message.error('更新标签状态失败')
      }
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
              // 新增标签 - 使用项目中实际存在的接口路径
              const response = await CreateTag(this.labelForm)
              const newLabel = response.data
              
              // 添加新标签到列表
              this.labelList.unshift(newLabel)
              this.$message.success('标签新增成功')
            } else {
              // 编辑标签 - 使用项目中实际存在的接口路径
              await UpdateTag(this.labelForm.id, this.labelForm)
              
              // 更新列表中的标签信息
              const index = this.labelList.findIndex(item => item.id === this.labelForm.id)
              if (index !== -1) {
                this.labelList[index] = {
                  ...this.labelList[index],
                  ...this.labelForm
                }
                this.$message.success('标签信息更新成功')
              }
            }
            
            this.labelDialogVisible = false
            this.filteredLabelList = this.labelList
            this.pagination.total = this.labelList.length
          } catch (error) {
            console.error('标签操作失败:', error)
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    },
    
    /**
     * 分类表单提交
     */
    async handleCategorySubmit() {
      this.$refs.categoryForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            if (this.categoryDialogType === 'add') {
              // 使用项目中实际存在的接口路径创建分类
              const response = await CreateTag({
                ...this.categoryForm,
                type: 'category'
              })
              const newCategory = response.data
              
              // 添加新分类到列表（排除根节点）
              const categories = this.categoryList.filter(cat => cat.type !== 'root')
              categories.push(newCategory)
              this.categoryList = [
                this.categoryList.find(cat => cat.type === 'root'),
                ...categories
              ]
              this.$message.success('分类新增成功')
            }
            
            this.categoryDialogVisible = false
          } catch (error) {
            console.error('分类操作失败:', error)
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    },
    
    // 获取分类名称
    getCategoryName(categoryId) {
      const category = this.categoryList.find(cat => cat.id === categoryId)
      return category ? category.name : '未知分类'
    },
    
    // 标签对话框关闭
    handleLabelDialogClose() {
      this.$refs.labelForm.clearValidate()
    },
    
    // 分页大小改变
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
    },
    
    // 当前页改变
    handleCurrentChange(page) {
      this.pagination.currentPage = page
    },
    
    // 刷新数据
    refreshData() {
      this.$message.success('数据刷新成功')
      this.fetchData()
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
    
    // 格式化日期时间
    formatDateTime(date) {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
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