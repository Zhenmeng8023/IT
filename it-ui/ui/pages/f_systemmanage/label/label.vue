<template>
  <div class="label-management">
    <div class="page-header">
      <h1>标签管理</h1>
      <p>分类由标签自身的分类字段自动聚合，新增标签时可以直接输入新分类。</p>
    </div>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div>
          <el-button v-permission="'btn:tag:create'" type="primary" icon="el-icon-plus" @click="handleAddLabel">
            新增标签
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索标签"
            style="width: 220px"
            clearable
            @keyup.enter.native="handleSearch"
            @clear="handleSearch"
          >
            <el-button v-permission="'btn:tag:search'" slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
          </el-input>
        </div>
      </div>
    </el-card>

    <div class="content-container">
      <el-card class="category-card" shadow="never">
        <div slot="header" class="card-header">
          <span>标签分类</span>
        </div>

        <el-tree
          :data="categoryList"
          :props="treeProps"
          node-key="id"
          :default-expand-all="true"
          :highlight-current="true"
          :current-node-key="currentCategory"
          @node-click="handleCategoryClick"
        >
          <span class="custom-tree-node" slot-scope="{ node, data }">
            <i :class="data.icon" style="margin-right: 8px;"></i>
            <span>{{ node.label }}</span>
            <span class="tag-count">({{ getCategoryTagCount(data.id) }})</span>
          </span>
        </el-tree>
      </el-card>

      <el-card class="list-card" shadow="never">
        <div slot="header" class="card-header">
          <span>标签列表</span>
          <span class="sub-title">当前分类：{{ currentCategoryName }}</span>
        </div>

        <el-table :data="labelList" v-loading="loading" stripe style="width: 100%">
          <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>

          <el-table-column prop="name" label="标签名称" min-width="160">
            <template slot-scope="scope">
              <el-tag type="info" size="small">{{ scope.row.name }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="parentName" label="父标签" width="140" align="center">
            <template slot-scope="scope">
              {{ scope.row.parentName || '根标签' }}
            </template>
          </el-table-column>

          <el-table-column prop="category" label="分类" width="140" align="center">
            <template slot-scope="scope">
              <el-tag size="small">{{ scope.row.category || '未分类' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="useCount" label="引用次数" width="100" align="center"></el-table-column>

          <el-table-column prop="description" label="描述" min-width="220">
            <template slot-scope="scope">
              {{ scope.row.description || '-' }}
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
            <template slot-scope="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button
                v-permission="'btn:tag:edit'"
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleEdit(scope.row)"
              >
                编辑
              </el-button>

              <el-button
                v-permission="'btn:tag:delete'"
                size="mini"
                type="text"
                icon="el-icon-delete"
                style="color: #f56c6c"
                @click="handleDelete(scope.row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            :current-page="pagination.currentPage"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          ></el-pagination>
        </div>
      </el-card>
    </div>

    <el-dialog
      :title="labelDialogTitle"
      :visible.sync="labelDialogVisible"
      width="520px"
      @close="handleLabelDialogClose"
    >
      <el-form ref="labelForm" :model="labelForm" :rules="labelRules" label-width="88px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="labelForm.name" maxlength="50" placeholder="请输入标签名称"></el-input>
        </el-form-item>

        <el-form-item label="父标签" prop="parentId">
          <el-select v-model="labelForm.parentId" clearable filterable placeholder="请选择父标签">
            <el-option :value="null" label="无"></el-option>
            <el-option
              v-for="tag in parentTagOptions"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-select
            v-model="labelForm.category"
            clearable
            filterable
            allow-create
            default-first-option
            placeholder="可选择已有分类或直接输入新分类"
          >
            <el-option
              v-for="category in categoryOptions"
              :key="category"
              :label="category"
              :value="category"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="labelForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入标签描述"
          ></el-input>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="labelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleLabelSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { CreateTag, UpdateTag, DeleteTag, GetAllTags, GetTagCategories } from '@/api/index.js'

export default {
  name: 'Label',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchKeyword: '',
      currentCategory: 'all',
      currentCategoryName: '全部标签',
      categoryList: [],
      allLabelList: [],
      labelList: [],
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      treeProps: {
        label: 'name',
        children: 'children'
      },
      labelDialogVisible: false,
      labelDialogType: 'add',
      labelForm: {
        id: null,
        name: '',
        parentId: null,
        category: '',
        description: ''
      },
      labelRules: {
        name: [
          { required: true, message: '请输入标签名称', trigger: 'blur' },
          { min: 1, max: 50, message: '标签名称长度在 1 到 50 个字符', trigger: 'blur' }
        ],
        category: [
          { required: true, message: '请输入或选择分类', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    labelDialogTitle() {
      return this.labelDialogType === 'add' ? '新增标签' : '编辑标签'
    },
    categoryOptions() {
      return this.categoryList
        .filter(item => item.id !== 'all')
        .map(item => item.name)
    },
    parentTagOptions() {
      return this.allLabelList.filter(tag => tag.id !== this.labelForm.id)
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    extractList(response) {
      if (Array.isArray(response)) return response
      if (Array.isArray(response?.data)) return response.data
      if (Array.isArray(response?.data?.data)) return response.data.data
      return []
    },
    resetLabelForm() {
      this.labelForm = {
        id: null,
        name: '',
        parentId: null,
        category: this.currentCategory !== 'all' ? this.currentCategoryName : '',
        description: ''
      }
    },
    async fetchCategories() {
      try {
        const categories = this.extractList(await GetTagCategories())
        this.categoryList = [
          {
            id: 'all',
            name: '全部标签',
            type: 'root',
            icon: 'el-icon-collection',
            tagCount: this.allLabelList.length
          },
          ...categories.map(category => ({
            id: category.id || category.name,
            name: category.name,
            type: 'category',
            icon: 'el-icon-folder',
            tagCount: category.tagCount || 0
          }))
        ]
      } catch (error) {
        this.fallbackFetchCategories()
      }
    },
    fallbackFetchCategories() {
      const categoryMap = this.allLabelList.reduce((result, tag) => {
        if (!tag.category) return result
        result[tag.category] = (result[tag.category] || 0) + 1
        return result
      }, {})

      this.categoryList = [
        {
          id: 'all',
          name: '全部标签',
          type: 'root',
          icon: 'el-icon-collection',
          tagCount: this.allLabelList.length
        },
        ...Object.keys(categoryMap).sort().map(name => ({
          id: name,
          name,
          type: 'category',
          icon: 'el-icon-folder',
          tagCount: categoryMap[name]
        }))
      ]
    },
    async fetchLabels() {
      const allTags = this.extractList(await GetAllTags())
      this.allLabelList = Array.isArray(allTags) ? allTags : []
      this.applyFilters()
      await this.fetchCategories()
    },
    applyFilters() {
      let filteredTags = this.allLabelList.slice()

      if (this.currentCategory !== 'all') {
        filteredTags = filteredTags.filter(tag => tag.category === this.currentCategory)
      }

      if (this.searchKeyword) {
        const keyword = this.searchKeyword.trim().toLowerCase()
        filteredTags = filteredTags.filter(tag =>
          String(tag.name || '').toLowerCase().includes(keyword)
        )
      }

      this.pagination.total = filteredTags.length
      const totalPages = Math.max(1, Math.ceil(this.pagination.total / this.pagination.pageSize))
      if (this.pagination.currentPage > totalPages) {
        this.pagination.currentPage = totalPages
      }
      const startIndex = (this.pagination.currentPage - 1) * this.pagination.pageSize
      const endIndex = startIndex + this.pagination.pageSize
      this.labelList = filteredTags.slice(startIndex, endIndex)
    },
    async fetchData() {
      this.loading = true
      try {
        await this.fetchLabels()
      } catch (error) {
        console.error('获取标签数据失败:', error)
        this.$message.error(error?.response?.data?.message || error?.message || '获取标签数据失败')
      } finally {
        this.loading = false
      }
    },
    getCategoryTagCount(categoryId) {
      if (categoryId === 'all') return this.allLabelList.length
      const category = this.categoryList.find(item => item.id === categoryId)
      return category ? (category.tagCount || 0) : 0
    },
    handleCategoryClick(data) {
      this.currentCategory = data.id
      this.currentCategoryName = data.name
      this.pagination.currentPage = 1
      this.applyFilters()
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.applyFilters()
    },
    handleAddLabel() {
      this.labelDialogType = 'add'
      this.resetLabelForm()
      this.labelDialogVisible = true
    },
    handleEdit(label) {
      this.labelDialogType = 'edit'
      this.labelForm = {
        id: label.id,
        name: label.name || '',
        parentId: label.parentId || null,
        category: label.category || '',
        description: label.description || ''
      }
      this.labelDialogVisible = true
    },
    handleDelete(label) {
      this.$confirm(`确定要删除标签 "${label.name}" 吗？删除后博客中的该标签也会同步移除。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await DeleteTag(label.id)
          this.$message.success('标签删除成功')
          await this.fetchData()
        } catch (error) {
          console.error('删除标签失败:', error)
          this.$message.error(error?.response?.data?.message || error?.message || '删除标签失败')
        }
      }).catch(() => {})
    },
    handleLabelDialogClose() {
      if (this.$refs.labelForm) {
        this.$refs.labelForm.clearValidate()
      }
    },
    handleLabelSubmit() {
      this.$refs.labelForm.validate(async valid => {
        if (!valid) return

        this.submitLoading = true
        try {
          const payload = {
            name: String(this.labelForm.name || '').trim(),
            parentId: this.labelForm.parentId || null,
            category: String(this.labelForm.category || '').trim(),
            description: String(this.labelForm.description || '').trim()
          }

          if (this.labelDialogType === 'add') {
            await CreateTag(payload)
            this.$message.success('标签新增成功')
          } else {
            await UpdateTag(this.labelForm.id, payload)
            this.$message.success('标签更新成功')
          }

          this.labelDialogVisible = false
          await this.fetchData()
        } catch (error) {
          console.error('标签保存失败:', error)
          this.$message.error(error?.response?.data?.message || error?.message || '标签保存失败')
        } finally {
          this.submitLoading = false
        }
      })
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.applyFilters()
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.applyFilters()
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
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .content-container {
    flex-direction: column;
  }

  .category-card {
    width: 100%;
  }
}
</style>
