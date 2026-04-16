<template>
  <div class="circle-sort">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>圈子分类管理</h1>
      <p>管理圈子分类体系，支持分类的增删改查、排序和统计</p>
    </div>

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="handleAddCategory">添加分类</el-button>
        <el-button type="success" icon="el-icon-sort" @click="handleSortCategories">排序分类</el-button>
        <el-button type="warning" icon="el-icon-refresh" @click="refreshData">刷新数据</el-button>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索分类名称"
            clearable
            style="width: 200px"
            prefix-icon="el-icon-search"
            @input="handleSearch">
          </el-input>
        </div>
      </div>
    </el-card>

    <!-- 分类列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="categoryList"
        v-loading="loading"
        stripe
        style="width: 100%">
        
        <el-table-column type="index" label="序号" width="60" align="center">
          <template slot-scope="scope">
            <span>{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="icon" label="图标" width="80" align="center">
          <template slot-scope="scope">
            <i :class="scope.row.icon" style="font-size: 24px; color: #409EFF;"></i>
          </template>
        </el-table-column>
        
        <el-table-column prop="name" label="分类名称" min-width="150">
          <template slot-scope="scope">
            <div class="category-name">
              <span class="name-text">{{ scope.row.name }}</span>
              <el-tag v-if="scope.row.isHot" size="mini" type="danger" style="margin-left: 5px">热门</el-tag>
              <el-tag v-if="!scope.row.isActive" size="mini" type="info" style="margin-left: 5px">禁用</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="分类描述" min-width="200">
          <template slot-scope="scope">
            {{ scope.row.description || '暂无描述' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="circleCount" label="圈子数量" width="100" align="center">
          <template slot-scope="scope">
            <el-tag type="primary">{{ scope.row.circleCount }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="memberCount" label="成员总数" width="100" align="center">
          <template slot-scope="scope">
            <el-tag type="success">{{ scope.row.memberCount }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="postCount" label="帖子总数" width="100" align="center">
          <template slot-scope="scope">
            <el-tag type="warning">{{ scope.row.postCount }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="sortOrder" label="排序权重" width="100" align="center">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.sortOrder"
              size="mini"
              :min="1"
              :max="999"
              @change="handleSortChange(scope.row)"
              style="width: 80px">
            </el-input-number>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleViewCircles(scope.row)">
              查看圈子
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditCategory(scope.row)"
              style="color: #409EFF;">
              编辑
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              :icon="scope.row.isHot ? 'el-icon-star-off' : 'el-icon-star-on'"
              @click="handleToggleHot(scope.row)"
              :style="{color: scope.row.isHot ? '#E6A23C' : '#909399'}">
              {{ scope.row.isHot ? '取消热门' : '设为热门' }}
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              :icon="scope.row.isActive ? 'el-icon-close' : 'el-icon-check'"
              @click="handleToggleActive(scope.row)"
              :style="{color: scope.row.isActive ? '#F56C6C' : '#67C23A'}">
              {{ scope.row.isActive ? '禁用' : '启用' }}
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDeleteCategory(scope.row)"
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

    <!-- 添加/编辑分类对话框 -->
    <el-dialog
      :title="categoryDialogTitle"
      :visible.sync="categoryDialogVisible"
      width="500px">
      <el-form :model="categoryForm" :rules="categoryRules" ref="categoryForm" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        
        <el-form-item label="分类图标" prop="icon">
          <el-select v-model="categoryForm.icon" placeholder="请选择图标" style="width: 100%;">
            <el-option v-for="icon in iconOptions" :key="icon.value" :label="icon.label" :value="icon.value">
              <span style="margin-right: 10px;">
                <i :class="icon.value" style="color: #409EFF;"></i>
              </span>
              {{ icon.label }}
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="分类描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="categoryForm.description"
            placeholder="请输入分类描述（可选）">
          </el-input>
        </el-form-item>
        
        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number
            v-model="categoryForm.sortOrder"
            :min="1"
            :max="999"
            style="width: 100%;">
          </el-input-number>
          <span style="color: #909399; font-size: 12px;">数字越小，排序越靠前</span>
        </el-form-item>
        
        <el-form-item label="状态设置">
          <el-switch
            v-model="categoryForm.isActive"
            active-text="启用"
            inactive-text="禁用">
          </el-switch>
          <el-switch
            v-model="categoryForm.isHot"
            active-text="热门"
            inactive-text="普通"
            style="margin-left: 20px;">
          </el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCategory">确认</el-button>
      </span>
    </el-dialog>

    <!-- 查看圈子对话框 -->
    <el-dialog
      :title="currentCategory ? currentCategory.name + ' - 关联圈子' : '关联圈子'"
      :visible.sync="circlesDialogVisible"
      width="80%"
      :before-close="handleCloseCircles">
      <div v-if="currentCategory" class="circles-content">
        <el-card class="stat-card" shadow="never">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">圈子总数</div>
                <div class="stat-value">{{ currentCategory.circleCount }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">成员总数</div>
                <div class="stat-value">{{ currentCategory.memberCount }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">帖子总数</div>
                <div class="stat-value">{{ currentCategory.postCount }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-item">
                <div class="stat-label">今日活跃</div>
                <div class="stat-value">{{ currentCategory.todayActive }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
        
        <el-table :data="circleList" style="width: 100%; margin-top: 20px;" v-loading="circlesLoading">
          <el-table-column prop="name" label="圈子名称" min-width="200">
            <template slot-scope="scope">
              <div class="circle-info">
                <el-avatar :size="32" :src="scope.row.avatar" style="vertical-align: middle; margin-right: 8px;"></el-avatar>
                <span>{{ scope.row.name }}</span>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column prop="creator" label="创建人" width="120">
            <template slot-scope="scope">
              <el-avatar :size="24" :src="scope.row.creatorAvatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
              {{ scope.row.creator }}
            </template>
          </el-table-column>
          
          <el-table-column prop="memberCount" label="成员数" width="80" align="center">
            <template slot-scope="scope">
              {{ scope.row.memberCount }}
            </template>
          </el-table-column>
          
          <el-table-column prop="postCount" label="帖子数" width="80" align="center">
            <template slot-scope="scope">
              {{ scope.row.postCount }}
            </template>
          </el-table-column>
          
          <el-table-column prop="createTime" label="创建时间" width="160" align="center">
            <template slot-scope="scope">
              {{ formatDate(scope.row.createTime) }}
            </template>
          </el-table-column>
          
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'approved' ? 'success' : 'warning'" size="small">
                {{ scope.row.status === 'approved' ? '正常' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="150" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="text" @click="handleViewCircleDetail(scope.row)">查看详情</el-button>
              <el-button size="mini" type="text" style="color: #F56C6C;" @click="handleRemoveCircle(scope.row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="pagination-container" style="margin-top: 20px;">
          <el-pagination
            @size-change="handleCirclesSizeChange"
            @current-change="handleCirclesCurrentChange"
            :current-page="circlesPagination.currentPage"
            :page-sizes="[10, 20, 50]"
            :page-size="circlesPagination.pageSize"
            layout="total, sizes, prev, pager, next"
            :total="circlesPagination.total">
          </el-pagination>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="circlesDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleAddCircleToCategory">添加圈子</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getCircleCategories,
  createCircleCategory,
  updateCircleCategory,
  deleteCircleCategory,
  updateCircleCategorySort,
  toggleCircleCategoryHot,
  toggleCircleCategoryEnabled,
  getCirclesByCategory,
  removeCircleFromCategory
} from '@/api/circleCategory'
import { pickAvatarUrl } from '@/utils/avatar'

function unwrapResponse(payload) {
  if (!payload || typeof payload !== 'object') return payload
  if (!Object.prototype.hasOwnProperty.call(payload, 'data') || payload.data === payload) {
    return payload
  }
  return unwrapResponse(payload.data)
}

function normalizePage(payload) {
  const data = unwrapResponse(payload)
  if (Array.isArray(data)) {
    return { list: data, total: data.length }
  }
  if (!data || typeof data !== 'object') {
    return { list: [], total: 0 }
  }

  const list = data.list || data.records || data.content || data.items || data.rows || []
  const normalizedList = Array.isArray(list) ? list : []
  const total = Number(data.total != null ? data.total : data.totalElements)

  return {
    list: normalizedList,
    total: Number.isFinite(total) ? total : normalizedList.length
  }
}

export default {
  name: 'CircleSort',
  layout: 'manage',
  data() {
    return {
      searchKeyword: '',
      categoryList: [],
      loading: false,
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      categoryDialogVisible: false,
      circlesDialogVisible: false,
      categoryForm: {
        id: '',
        name: '',
        icon: '',
        description: '',
        sortOrder: 10,
        isActive: true,
        isHot: false
      },
      categoryRules: {
        name: [
          { required: true, message: '请输入分类名称', trigger: 'blur' },
          { min: 2, max: 20, message: '分类名称长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        icon: [
          { required: true, message: '请选择分类图标', trigger: 'change' }
        ]
      },
      iconOptions: [
        { value: 'el-icon-monitor', label: '技术' },
        { value: 'el-icon-reading', label: '学习' },
        { value: 'el-icon-camera', label: '摄影' },
        { value: 'el-icon-basketball', label: '运动' },
        { value: 'el-icon-music', label: '音乐' },
        { value: 'el-icon-film', label: '电影' },
        { value: 'el-icon-food', label: '美食' },
        { value: 'el-icon-shopping', label: '购物' },
        { value: 'el-icon-chat-line-round', label: '聊天' },
        { value: 'el-icon-present', label: '生活' }
      ],
      categoryDialogTitle: '添加分类',
      currentCategory: null,
      circleList: [],
      circlesLoading: false,
      circlesPagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      searchTimer: null
    }
  },
  mounted() {
    this.loadCategoryList()
  },
  beforeDestroy() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
      this.searchTimer = null
    }
  },
  methods: {
    mapCategory(raw) {
      return {
        id: raw.id || raw.categoryId,
        name: raw.name || raw.categoryName || '未命名分类',
        icon: raw.icon || raw.iconClass || 'el-icon-collection-tag',
        description: raw.description || '',
        circleCount: raw.circleCount || raw.circlesCount || 0,
        memberCount: raw.memberCount || 0,
        postCount: raw.postCount || 0,
        todayActive: raw.todayActive || raw.activeCount || 0,
        sortOrder: raw.sortOrder || raw.sort || 1,
        isActive: raw.isActive != null ? Boolean(raw.isActive) : !(raw.enabled === false),
        isHot: Boolean(raw.isHot || raw.hot),
        createTime: raw.createTime || raw.createdAt || null
      }
    },
    mapCircle(raw) {
      const creator = raw.creator || raw.owner || {}
      return {
        id: raw.id || raw.circleId,
        name: raw.name || raw.title || '未命名圈子',
        avatar: pickAvatarUrl(raw.avatarUrl, raw.avatar),
        creator: raw.creatorName || creator.nickname || creator.username || '未知用户',
        creatorAvatar: pickAvatarUrl(raw.creatorAvatarUrl, raw.creatorAvatar, creator.avatarUrl, creator.avatar),
        memberCount: raw.memberCount || 0,
        postCount: raw.postCount || 0,
        createTime: raw.createTime || raw.createdAt || null,
        status: raw.status || raw.type || 'approved'
      }
    },
    buildCategoryParams() {
      return {
        page: this.pagination.currentPage,
        size: this.pagination.pageSize,
        pageNum: this.pagination.currentPage,
        pageSize: this.pagination.pageSize,
        keyword: this.searchKeyword || undefined
      }
    },
    async validateCategoryForm() {
      return new Promise(resolve => {
        this.$refs.categoryForm.validate(valid => resolve(valid))
      })
    },
    async loadCategoryList() {
      this.loading = true
      try {
        const response = await getCircleCategories(this.buildCategoryParams())
        const pageData = normalizePage(response)
        this.categoryList = pageData.list.map(this.mapCategory)
        this.pagination.total = pageData.total
      } catch (error) {
        this.categoryList = []
        this.pagination.total = 0
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载分类列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      this.searchTimer = setTimeout(() => {
        this.pagination.currentPage = 1
        this.loadCategoryList()
      }, 300)
    },
    refreshData() {
      this.pagination.currentPage = 1
      this.loadCategoryList()
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.loadCategoryList()
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.loadCategoryList()
    },
    handleAddCategory() {
      this.categoryForm = {
        id: '',
        name: '',
        icon: '',
        description: '',
        sortOrder: 10,
        isActive: true,
        isHot: false
      }
      this.categoryDialogTitle = '添加分类'
      this.categoryDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.categoryForm) {
          this.$refs.categoryForm.clearValidate()
        }
      })
    },
    handleEditCategory(category) {
      this.categoryForm = {
        id: category.id,
        name: category.name,
        icon: category.icon,
        description: category.description,
        sortOrder: category.sortOrder,
        isActive: category.isActive,
        isHot: category.isHot
      }
      this.categoryDialogTitle = '编辑分类'
      this.categoryDialogVisible = true
    },
    async handleConfirmCategory() {
      const valid = await this.validateCategoryForm()
      if (!valid) return

      const payload = {
        name: this.categoryForm.name,
        icon: this.categoryForm.icon,
        description: this.categoryForm.description,
        sortOrder: this.categoryForm.sortOrder,
        isActive: this.categoryForm.isActive,
        isHot: this.categoryForm.isHot
      }

      try {
        if (this.categoryForm.id) {
          await updateCircleCategory(this.categoryForm.id, payload)
          this.$message.success('分类编辑成功')
        } else {
          await createCircleCategory(payload)
          this.$message.success('分类添加成功')
        }
        this.categoryDialogVisible = false
        this.refreshData()
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '分类操作失败')
      }
    },
    async handleSortChange(category) {
      try {
        await updateCircleCategorySort(category.id, { sortOrder: category.sortOrder })
        this.$message.success('排序更新成功')
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '更新排序失败')
        this.loadCategoryList()
      }
    },
    async handleToggleHot(category) {
      const nextValue = !category.isHot
      try {
        await toggleCircleCategoryHot(category.id, { isHot: nextValue })
        category.isHot = nextValue
        this.$message.success(nextValue ? '已设为热门分类' : '已取消热门分类')
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '切换热门状态失败')
      }
    },
    async handleToggleActive(category) {
      const nextValue = !category.isActive
      try {
        await toggleCircleCategoryEnabled(category.id, { isActive: nextValue })
        category.isActive = nextValue
        this.$message.success(nextValue ? '分类已启用' : '分类已禁用')
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '切换启用状态失败')
      }
    },
    async handleDeleteCategory(category) {
      try {
        await this.$confirm(`确定要删除分类 "${category.name}" 吗？此操作不可恢复！`, '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        await deleteCircleCategory(category.id)
        this.$message.success('分类删除成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '删除分类失败')
        }
      }
    },
    async handleViewCircles(category) {
      this.currentCategory = category
      this.circlesPagination.currentPage = 1
      await this.loadCircleList()
      this.circlesDialogVisible = true
    },
    async loadCircleList() {
      if (!this.currentCategory) {
        this.circleList = []
        this.circlesPagination.total = 0
        return
      }

      this.circlesLoading = true
      try {
        const response = await getCirclesByCategory(this.currentCategory.id, {
          page: this.circlesPagination.currentPage,
          size: this.circlesPagination.pageSize,
          pageNum: this.circlesPagination.currentPage,
          pageSize: this.circlesPagination.pageSize
        })
        const pageData = normalizePage(response)
        this.circleList = pageData.list.map(this.mapCircle)
        this.circlesPagination.total = pageData.total
      } catch (error) {
        this.circleList = []
        this.circlesPagination.total = 0
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载圈子列表失败')
      } finally {
        this.circlesLoading = false
      }
    },
    handleCloseCircles(done) {
      this.circlesDialogVisible = false
      this.currentCategory = null
      this.circleList = []
      this.circlesPagination.currentPage = 1
      this.circlesPagination.total = 0
      if (typeof done === 'function') {
        done()
      }
    },
    handleCirclesSizeChange(size) {
      this.circlesPagination.pageSize = size
      this.loadCircleList()
    },
    handleCirclesCurrentChange(page) {
      this.circlesPagination.currentPage = page
      this.loadCircleList()
    },
    handleViewCircleDetail(circle) {
      this.$message.info(`查看圈子 ${circle.name}（ID: ${circle.id}）`) 
    },
    async handleRemoveCircle(circle) {
      try {
        await this.$confirm(`确定要将圈子 "${circle.name}" 从分类中移除吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await removeCircleFromCategory(this.currentCategory.id, circle.id)
        this.$message.success('圈子移除成功')
        this.loadCircleList()
        this.loadCategoryList()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '移除圈子失败')
        }
      }
    },
    handleAddCircleToCategory() {
      this.$message.info('请在圈子管理页调整圈子分类归属')
    },
    async handleSortCategories() {
      if (!this.categoryList.length) return

      const sorted = [...this.categoryList].sort((a, b) => a.sortOrder - b.sortOrder)
      try {
        await Promise.all(
          sorted.map((item, index) => updateCircleCategorySort(item.id, { sortOrder: index + 1 }))
        )
        this.$message.success('分类排序已同步')
        this.loadCategoryList()
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '分类排序同步失败')
      }
    },
    formatDate(date) {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.circle-sort {
  padding: 20px;
}

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
  align-items: center;
}

.toolbar-right {
  margin-left: auto;
}

.table-card {
  margin-bottom: 20px;
}

.category-name {
  display: flex;
  align-items: center;
}

.name-text {
  font-weight: 500;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.circles-content {
  max-height: 600px;
  overflow-y: auto;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.circle-info {
  display: flex;
  align-items: center;
}
</style>
