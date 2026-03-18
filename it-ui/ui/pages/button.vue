<template>
<div>
<!-- Z_registerpage/registepage中22行 -->
    <el-button v-permission="'auth:register:send-code'" type="primary" @click="getcode" :disabled="gettingCode || countdown > 0">
        {{ gettingCode ? '发送中...' : (countdown > 0 ? `${countdown}秒后重新获取` : '获取验证码') }}
    </el-button>
<!-- Z_registerpage/registepage中31行 -->
    <el-button v-permission="'auth:register'" type="success" :loading="loading" @click="register">注册</el-button>
<!-- Z_login/logpage中17行 -->
    <el-button v-permission="'auth:login'" type="primary" @click="login" :loading="loading">登录</el-button>
<!-- Z_userpage/peoplehome的287行 -->
     <el-button v-permission="'blog:delete'"
                    type="danger" 
                    icon="el-icon-delete" 
                    size="mini" 
                    circle
                    @click.stop="handleDeleteBlog(blog)"
                    title="删除博客"
                  ></el-button>
<!-- Z_userpage/peoplehome的316行 -->
        <el-button v-permission="'circle:comment:delete'"
                            type="danger" 
                            icon="el-icon-delete" 
                            size="mini" 
                            circle
                            @click.stop="handleDeletePost(post)"
                            title="删除帖子"
                        ></el-button>
<!-- Z_circledetail/circledetail的39行 -->
        <el-button v-permission="'circle:comment:create'" type="primary" @click="submitTopLevelComment" :disabled="!newComment.trim()" :loading="submitting">
            发表评论
        </el-button>
<!-- Z_circledetail/circledetail的79行 -->
         <el-button v-permission="'circle:comment:create'" type="primary" size="small" @click="submitReply(topComment)" :loading="replySubmitting">
            提交回复
        </el-button>
<!-- Z_circledetail/circledetail的113行 -->
        <el-button v-permission="'circle:comment:create'" type="primary" size="small" @click="submitReply(topComment, reply)" :loading="replySubmitting">
            提交回复
        </el-button>
<!-- Z_circledetail/circledetail的147行 -->
        <el-button v-permission="'circle:comment:create'" type="primary" size="small" @click="submitReply(topComment, nestedReply)" :loading="replySubmitting">
            提交回复
        </el-button>
<!-- Z_circledetail/circledetail的181行 -->
        <el-button v-permission="'circle:comment:create'" type="primary" size="small" @click="submitReply(topComment, deepReply)" :loading="replySubmitting">
            提交回复
        </el-button>
<!-- Z_biogdetail/biogdetail的20行 -->
        <el-button v-permission="'like:delete'"
            :type="blog.isLiked ? 'primary' : 'default'"
            size="small"
            :icon="blog.isLiked ? 'el-icon-sunny' : 'el-icon-sunrise'"
            @click="handleLike"
            circle
            :class="{ 'liked-button': blog.isLiked }"
            :loading="likeLoading"
          ></el-button>
<!-- Z_biogdetail/biogdetail的32行 -->
          <el-button v-permission="'collect:delete'"
            :type="blog.isCollected ? 'warning' : 'default'"
            size="small"
            :icon="blog.isCollected ? 'el-icon-star-on' : 'el-icon-star-off'"
            @click="handleCollect"
            circle
            :loading="collectLoading"
          ></el-button>
<!-- Z_biogdetail/biogdetail的90行 -->
          <el-button v-permission="'comment:create'" type="primary" size="small" @click="submitReply(comment)" :loading="replySubmitting">
            提交回复
          </el-button>
<!-- Z_biogdetail/biogdetail的126行 -->
           <el-button v-permission="'comment:create'" type="primary" size="small" @click="submitReply(comment, reply)" :loading="replySubmitting">
            提交回复
          </el-button>
<!-- Z_biogdetail/biogdetail的151行 -->
          <el-button v-permission="'comment:create'" type="primary" @click="submitTopLevelComment" :disabled="!newComment.trim()" :loading="submitting">
            发表评论
          </el-button>
<!-- f_systemmanage/usermanage/count的10行 -->
           <el-button v-permission="'user:create'" type="primary" icon="el-icon-plus" @click="handleAddUser">
            新增用户
           </el-button>    
<!-- f_systemmanage/usermanage/count的58行 -->
           <el-button v-permission="'user:list'" type="primary" @click="handleSearch">查询</el-button>
<!-- f_systemmanage/usermanage/count的124行 -->
           <el-button v-permission="'user:update'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
            </el-button>
<!-- f_systemmanage/usermanage/count的247行 -->
            <el-button v-permission="'user:create'" type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
<!-- f_systemmanage/usermanage/info的40行 -->
            <el-button v-permission="'admin:user:list-page'" type="primary" @click="handleSearch">查询</el-button>
<!-- f_systemmanage/usermanage/info的41行 -->
            <el-button v-permission="'admin:user:list-page'" type="primary" @click="handleReset">重置</el-button>
<!-- f_systemmanage/usermanage/info的122行 -->
            <el-button v-permission="'user:read2'"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)">
              查看
            </el-button>
<!-- f_systemmanage/usermanage/info的130行 -->
            <el-button v-permission="'admin:user:list-page'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
            </el-button>
<!-- f_systemmanage/usermanage/info的264行 -->
            <el-button v-permission="'admin:user:list-page'" type="primary" @click="handleEditSubmit" :loading="submitLoading">确定</el-button>
<!-- f_systemmanage/label/label的13行 -->
            <el-button v-permission="'admin:tag:list-page'" type="primary" icon="el-icon-plus" @click="handleAddLabel">
             新增标签
            </el-button>
<!-- f_systemmanage/label/label的16行 -->
            <el-button v-permission="'admin:tag:create'" type="success" icon="el-icon-folder-add" @click="handleAddCategory">
             新增分类
            </el-button>  
<!-- f_systemmanage/label/label的27行 -->
            <el-button v-permission="'admin:tag:list-page'" slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
<!-- f_systemmanage/label/label的39行 -->
            <el-button v-permission="'admin:tag:list-page'" type="text" icon="el-icon-more" @click="handleCategoryMenu"></el-button>
<!-- f_systemmanage/label/label的113行 -->
            <el-button v-permission="'admin:tag:update'"
             size="mini"
             type="text"
             icon="el-icon-edit"
             @click="handleEdit(scope.row)">
             编辑
            </el-button>
<!-- f_systemmanage/label/label的122行 -->
            <el-button v-permission="'admin:tag:delete'"
             size="mini"
             type="text"
             icon="el-icon-delete"
             @click="handleDelete(scope.row)"
             style="color: #F56C6C;">
             删除
            </el-button>
<!-- f_systemmanage/label/label的197行 -->
            <el-button v-permission="'admin:tag:create'" type="primary" @click="handleLabelSubmit" :loading="submitLoading">确定</el-button>
<!-- f_systemmanage/label/label的224行 -->
            <el-button v-permission="'admin:tag:create'" type="primary" @click="handleCategorySubmit" :loading="submitLoading">确定</el-button>
<!-- f_systemmanage/role/role的12行 -->
            <el-button v-permission="'role:create'" type="primary" icon="el-icon-plus" @click="handleCreateRole">
              新增角色
            </el-button>
<!-- f_systemmanage/role/role的65行 -->
            <el-button v-permission="'role:update'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditRole(scope.row)">
              编辑
            </el-button>
<!-- f_systemmanage/role/role的72行 -->
            <el-button v-permission="'role:assign-menu'"
              size="mini"
              type="text"
              icon="el-icon-setting"
              @click="handlePermissionConfig(scope.row)">
              权限配置
            </el-button>
<!-- f_systemmanage/role/role的72行 -->
            <el-button v-permission="'role:delete'"
              size="mini"
              type="text"
              icon="el-icon-delete"
              style="color: #f56c6c"
              @click="handleDeleteRole(scope.row)"
              :disabled="scope.row.id === 1">
              删除
            </el-button>
<!-- f_systemmanage/menu/menu的12行 -->
            <el-button v-permission="'menu:create'" type="primary" icon="el-icon-plus" @click="handleAddMenu">
              新增菜单
            </el-button>
<!-- f_systemmanage/menu/menu的112行 -->
            <el-button v-permission="'menu:update'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
            </el-button>            
<!-- f_systemmanage/menu/menu的120行 -->
            <el-button v-permission="'menu:create'"
              size="mini"
              type="text"
              icon="el-icon-plus"
              @click="handleAddSubMenu(scope.row)"
              v-if="scope.row.type === 'menu'">
              添加子菜单
            </el-button>
<!-- f_systemmanage/menu/menu的129行 -->
            <el-button v-permission="'menu:delete'"
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              style="color: #F56C6C;">
              删除
            </el-button>
<!-- f_systemmanage/menu/menu的249行 -->
            <el-button v-permission="'menu:create'" type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
<!-- f_systemmanage/permission/permission的18行 -->
            <el-button v-permission="'permission:create'" type="primary" icon="el-icon-plus" @click="handleAddPermission">
              新增权限
            </el-button>
<!-- f_systemmanage/permission/permission的63行 -->
            <el-button v-permission="'permission:update'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditPermission(scope.row)">
              编辑
            </el-button>
<!-- f_systemmanage/permission/permission的70行 -->
            <el-button v-permission="'permission:delete'"
              size="mini"
              type="text"
              icon="el-icon-delete"
              style="color: #f56c6c;"
              @click="handleDeletePermission(scope.row)">
              删除
            </el-button>
<!-- f_systemmanage/audit/audit的131行 -->
            <el-button v-permission="'blog:list-unpublished'"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)">
              查看
            </el-button>
<!-- f_systemmanage/audit/audit的139行 -->
            <el-button v-permission="'admin:blog:approve'"
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-check"
              @click="handleApprove(scope.row)"
              style="color: #67C23A;">
              通过
            </el-button>
<!-- f_systemmanage/audit/audit的149行 -->
            <el-button v-permission="'admin:blog:approve'"
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-close"
              @click="handleReject(scope.row)"
              style="color: #F56C6C;">
              拒绝
            </el-button>        
<!-- f_systemmanage/audit/audit的169行 -->
             <el-button v-permission="'blog:delete'"
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              style="color: #F56C6C;">
              删除
            </el-button>
<!-- f_systemmanage/audit/audit的223行 -->
            <el-button v-permission="'blog:read'" type="success" icon="el-icon-check" @click="handleApprove(currentBlog)">
              通过审核
            </el-button>
            <el-button v-permission="'blog:read'" type="danger" icon="el-icon-close" @click="handleReject(currentBlog)">
              拒绝审核
            </el-button>





</template>

<script>
export default {

}
</script>

<style>

</style>