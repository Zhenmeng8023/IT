<template>
  <div class="blog-detail-container">
    <!-- ========== AI助手 ========== -->
    <!-- <AIAssistant/> -->
    <!-- ========== 文章头部卡片 ========== -->
    <el-card class="blog-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="detailLoading">
      <!-- 博客标题 -->
      <div class="blog-title-wrapper">
        <h1 class="blog-title">{{ blog.title }}</h1>
        <!-- 价格标签 -->
        <el-tag 
          v-if="blog.price !== undefined && blog.price !== null" 
          :type="getPriceTagType(blog.price)" 
          size="small"
          class="price-tag"
          v-text="getPriceTagText(blog.price)"
        ></el-tag>
      </div>

      <!-- 作者信息及互动按钮区 -->
      <div class="blog-meta">
        <!-- 作者头像 -->
        <el-avatar :size="50" :src="blog.avatar" @click="goToAuthor" style="cursor: pointer;"></el-avatar>
        <!-- 作者名称 -->
        <span class="author-name" @click="goToAuthor" style="cursor: pointer;">{{ blog.author }}</span>
        <!-- 发布日期 -->
        <span class="publish-date">发布于 {{ blog.publishDate }}</span>

        <!-- 点赞 & 收藏按钮组 -->
        <div class="action-buttons">
          <!-- 点赞按钮 -->
          <el-button
            :type="blog.isLiked ? 'primary' : 'default'"
            size="small"
            :icon="blog.isLiked ? 'el-icon-sunny' : 'el-icon-sunrise'"
            @click="handleLike"
            circle
            :class="{ 'liked-button': blog.isLiked }"
            :loading="likeLoading"
          ></el-button>
          <span class="like-count">{{ blog.likeCount }}</span>

          <!-- 收藏按钮 -->
          <el-button
            :type="blog.isCollected ? 'warning' : 'default'"
            size="small"
            :icon="blog.isCollected ? 'el-icon-star-on' : 'el-icon-star-off'"
            @click="handleCollect"
            circle
            :loading="collectLoading"
          ></el-button>
          <span class="collect-count">{{ blog.collectCount }}</span> 
        </div>
      </div>
    </el-card>

    <!-- ========== 文章正文卡片 ========== -->
    <el-card class="blog-content" :body-style="{ padding: '30px' }" shadow="never">
      <!-- 付费博客处理（price > 0 时） -->
      <div v-if="blog.price > 0 && !hasPurchased" class="paid-content-wrapper">
        <!-- 内容预览 -->
        <div class="content-preview" v-html="blog.content"></div>
        <!-- 模糊遮罩 -->
        <div class="content-blur"></div>
        <!-- 付费提示 -->
        <div class="paid-prompt">
          <i class="el-icon-lock"></i>
          <h3>此内容为付费文章</h3>
          <p class="price-tag">
            <span class="price-label">价格：</span>
            <span class="price-value">¥{{ blog.price }}</span>
          </p>
          <p class="purchase-benefit">购买后可永久查看该内容</p>
          <div class="purchase-actions">
            <el-button type="primary" @click="purchaseBlog">立即购买</el-button>
            <el-button @click="$router.back()">返回上一页</el-button>
          </div>
        </div>
      </div>
      
      <!-- VIP 内容处理（price === -1 时为 VIP 专属） -->
      <div v-else-if="blog.price === -1 && !isVipUser && !contentExpanded" class="vip-content-wrapper">
        <!-- 内容预览（只显示前几行） -->
        <div class="content-preview" v-html="blog.content"></div>
        <!-- 模糊遮罩 -->
        <div class="content-blur"></div>
        <!-- VIP 提示 -->
        <div class="vip-prompt">
          <i class="el-icon-star-on"></i>
          <h3>此内容为 VIP 专属</h3>
          <p>开通 VIP 会员，畅享全部优质内容</p>
          <div class="vip-actions">
            <el-button type="primary" @click="vipDialogVisible = true">立即开通 VIP</el-button>
            <el-button @click="contentExpanded = true" v-if="isVipUser">展开全部内容</el-button>
          </div>
        </div>
      </div>
      <!-- 完整内容 -->
      <div v-else class="content-body" v-html="blog.content"></div>
    </el-card>

    <!-- ========== VIP开通引导弹窗 ========== -->
    <el-dialog
      title="VIP 专属内容"
      :visible.sync="vipDialogVisible"
      width="400px"
      :close-on-click-modal="false"
    >
      <div style="text-align: center; padding: 20px;">
        <i class="el-icon-star-on" style="font-size: 48px; color: #f5a623;"></i>
        <h3>此内容仅限 VIP 会员阅读</h3>
        <p>开通 VIP 会员，畅享全部优质内容</p>
        <el-button type="primary" @click="goToVipPage" style="margin-top: 20px;">立即开通 VIP</el-button>
      </div>
    </el-dialog>
    
    <!-- ========== 付费博客购买确认弹窗 ========== -->
    <el-dialog
      title="确认购买"
      :visible.sync="showPurchaseDialog"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="purchase-confirm-content">
        <p><strong>文章标题：</strong>{{ blog.title }}</p>
        <p class="amount-info">
          <span>支付金额：</span>
          <span class="amount">¥{{ purchaseAmount }}</span>
        </p>
        <p class="balance-info">使用余额支付</p>
      </div>
      <span slot="footer">
        <el-button @click="showPurchaseDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmPayment" :loading="purchaseSubmitting">确认支付</el-button>
      </span>
    </el-dialog>

    <!-- ========== 评论区卡片 ========== -->
    <el-card class="comment-section" shadow="hover" v-loading="commentLoading">
      <!-- 卡片头部：显示评论总数 -->
      <div slot="header" class="comment-header">
        <span>评论（{{ totalComments }}）</span>
      </div>

      <!-- 评论列表（支持多级回复） -->
      <div class="comment-list">
        <!-- 遍历顶级评论 -->
        <div v-for="comment in topLevelComments" :key="comment.id" class="comment-thread">
          <!-- 顶级评论 -->
          <div class="comment-item">
            <el-avatar :size="40" :src="comment.avatar"></el-avatar>
            <div class="comment-content">
              <div class="comment-meta">
                <span class="comment-author">{{ comment.nickname }}</span>
                <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                <span v-if="comment.isAuthor" class="author-badge">作者</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
              <div class="comment-actions">
                <el-button type="text" size="small" @click="showReplyInput(comment)">
                  <i class="el-icon-chat-line-round"></i> 回复
                </el-button>
              </div>
            </div>
          </div>

          <!-- 回复输入框（针对顶级评论） -->
          <div v-if="replyTarget && replyTarget.id === comment.id && !replyTarget.parentId" class="reply-input-wrapper">
            <el-input
              type="textarea"
              :rows="2"
              :placeholder="'回复 @' + (replyTarget.nickname || '用户')"
              v-model="replyContent"
              resize="none"
            ></el-input>
            <div class="reply-actions">
              <el-button size="small" @click="cancelReply">取消</el-button>
              <el-button type="primary" size="small" @click="submitReply(comment)" :loading="replySubmitting">
                提交回复
              </el-button>
            </div>
          </div>

          <!-- 该顶级评论下的回复列表 -->
          <div v-if="comment.replies && comment.replies.length" class="replies">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <el-avatar :size="30" :src="reply.avatar"></el-avatar>
              <div class="reply-content">
                <div class="comment-meta">
                  <span class="comment-author">{{ reply.nickname }}</span>
                  <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                  <span v-if="reply.isAuthor" class="author-badge">作者</span>
                  <span v-if="reply.replyTo" class="reply-to">回复 @{{ reply.replyTo }}</span>
                </div>
                <div class="comment-text">{{ reply.content }}</div>
                <div class="comment-actions">
                  <el-button type="text" size="small" @click="showReplyInput(reply, comment)">
                    <i class="el-icon-chat-line-round"></i> 回复
                  </el-button>
                </div>
              </div>

              <!-- 针对回复的回复输入框 -->
              <div v-if="replyTarget && replyTarget.id === reply.id" class="reply-input-wrapper nested">
                <el-input
                  type="textarea"
                  :rows="2"
                  :placeholder="'回复 @' + (replyTarget.nickname || '用户')"
                  v-model="replyContent"
                  resize="none"
                ></el-input>
                <div class="reply-actions">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button type="primary" size="small" @click="submitReply(comment, reply)" :loading="replySubmitting">
                    提交回复
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 没有评论时显示 -->
        <div v-if="!topLevelComments.length" class="no-comment">
          暂无评论，快来抢沙发吧～
        </div>
      </div>

      <!-- 发表评论输入区（顶级评论） -->
      <div class="comment-input-area">
        <el-input
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          v-model="newComment"
          resize="none"
        ></el-input>
        <div class="comment-submit">
          <el-button type="primary" @click="submitTopLevelComment" :disabled="!newComment.trim()" :loading="submitting">
            发表评论
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- ========== 回到顶部按钮 ========== -->
    <el-backtop target=".blog-detail-container" :bottom="100" :right="40">
      <div class="backtop-inner">
        <i class="el-icon-arrow-up"></i>
        <span>顶部</span>
      </div>
    </el-backtop>
  </div>
</template>

<script>
import { GetCurrentUser, GetBlogById, CheckUserLiked, DeleteLike, AddLike, CollectBlog, CancelCollectBlog, AddComment, ReplyComment, GetCommentsByPost, IsCollected } from '@/api/index'

export default {
  name: 'BlogDetail',
  data() {
    return {
      // ---------- 博客详情数据 ----------
      blog: {
        id: '',
        like_id: '',
        title: '',
        author: '',
        authorId: '',
        avatar: '',
        publishDate: '',
        likeCount: 0,
        collectCount: 0,
        isLiked: false,
        isCollected: false,
        content: null, // 内容省略
        isVipOnly: false, // 是否为 VIP 专属内容
        price: 0 // 博客价格：0 免费，-1VIP，其他为付费价格
      },

      // ---------- 评论列表数据（扁平结构）----------
      comments: [],
      // 新评论（顶级）
      newComment: '',
      submitting: false,
      // 回复相关
      replyTarget: null, // 当前正在回复的评论对象
      replyContent: '',
      replySubmitting: false,
      // 加载状态
      detailLoading: false,
      commentLoading: false,
      likeLoading: false,
      collectLoading: false,
      // 当前正在处理的博客ID，用于处理竞态条件
      currentProcessingBlogId: null,
      // 用户信息
      currentUser: null,
      userId: null,
      username: '',
      userAvatar: '',
      // VIP 相关
      isVipUser: false, // 当前用户是否为 VIP
      contentExpanded: false, // 内容是否展开
      vipDialogVisible: false, // VIP 开通引导弹窗
      // 付费博客相关
      hasPurchased: false, // 是否已购买付费博客
      showPurchaseDialog: false, // 显示购买对话框
      purchaseAmount: 0, // 购买金额
      purchaseOrderNo: '', // 订单号
      purchaseSubmitting: false // 购买提交中
    };
  },
  computed: {
 // 计算评论树结构（只展示二级评论）
topLevelComments() {
  // 创建评论映射表，便于快速查找
  const commentMap = new Map();
  const topComments = [];
  
  // 首先将所有评论加入映射表
  this.comments.forEach(comment => {
    commentMap.set(comment.id, { ...comment, replies: [] });
  });
  
  // 遍历所有评论，建立二级父子关系
this.comments.forEach(comment => {
  const commentWithReplies = commentMap.get(comment.id);
  
  if (comment.parentId === null) {
    // 顶级评论的parentId为null
    topComments.push(commentWithReplies);
  } else {
    // 找到父评论
    let parentComment = commentMap.get(comment.parentId);
    if (parentComment) {
      // 找到顶级评论
      let topLevelComment = parentComment;
      while (topLevelComment.parentId) {
        topLevelComment = commentMap.get(topLevelComment.parentId);
      }
      
      // 将评论添加到顶级评论的回复中
      topLevelComment.replies.push(commentWithReplies);
    }
  }
});
  
  // 排序评论（按创建时间正序）
  const sortComments = (comments) => {
    comments.sort((a, b) => new Date(a.createTime) - new Date(b.createTime));
    // 只对二级评论排序，不递归处理更深层次的评论
    comments.forEach(comment => {
      if (comment.replies && comment.replies.length) {
        comment.replies.sort((a, b) => new Date(a.createTime) - new Date(b.createTime));
      }
    });
  };
  
  sortComments(topComments);
  return topComments;
},
    // 评论总数
    totalComments() {
      return this.comments.length;
    }
  },
  created() {
    // 从路由参数获取博客id
    const blogId = this.$route.params.id;
    if (blogId) {
      this.blog.id = blogId;
      // 先获取博客详情，再获取用户信息
      this.getBlogDetail(blogId).then(() => {
        // 博客详情获取完成后，再获取用户信息
        this.getCurrentUser();
      });
    }
  },
  methods: {
    /**
     * 格式化时间
     * @param {string} time - 时间字符串
     * @returns {string} 格式化后的时间
     */
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },

    /**
     * 根据价格获取标签类型
     * @param {number} price - 博客价格
     * @returns {string} - 标签类型：success(免费), warning(VIP), primary(付费)
     */
    getPriceTagType(price) {
      if (price === 0) {
        return 'success'; // 免费博客 - 绿色
      } else if (price === -1) {
        return 'warning'; // VIP 专属 - 橙色
      } else {
        return 'primary'; // 付费博客 - 蓝色
      }
    },

    /**
     * 根据价格获取标签文本
     * @param {number} price - 博客价格
     * @returns {string} - 标签文本
     */
    getPriceTagText(price) {
      if (price === 0) {
        return '免费';
      } else if (price === -1) {
        return 'VIP';
      } else {
        return `¥${price}`;
      }
    },

    /**
     * 获取当前用户信息
     */
    async getCurrentUser() {
      try {
        // 使用实际 API 获取用户信息
        const res = await GetCurrentUser();
        const userData = res.data;
        if (userData) {
          this.currentUser = userData;
          this.userId = userData.id;
          this.username = userData.username || userData.nickname;
          this.userAvatar = userData.avatar || userData.avatarUrl;
          // 获取用户 VIP 状态（优先使用后端返回的字段）
          this.isVipUser = userData.isVip === true || userData.vipStatus === 'active' || false;
          console.log('用户 VIP 状态:', this.isVipUser, '用户数据:', userData);
          // 获取用户点赞状态
          this.checkLikeStatus();
          // 检查收藏状态
          this.checkCollectStatus();
        }
      } catch (error) {
        console.error('获取当前用户信息失败', error);
        // 404 表示用户未登录，不是错误
        if (error.response && error.response.status === 404) {
          console.log('用户未登录');
        }
      }
    },

    /**
     * 获取博客详情
     * @param {string} blogId - 博客id
     * @returns {Promise} - 返回Promise
     */
    async getBlogDetail(blogId) {
      // 设置当前正在处理的博客ID，用于处理竞态条件
      this.currentProcessingBlogId = blogId;
      this.detailLoading = true;
      try {
        // 使用实际API获取博客详情
        const res = await GetBlogById(blogId);
        
        // 检查是否是当前正在处理的博客，避免竞态条件
        if (this.currentProcessingBlogId !== blogId) {
          console.log('忽略过期的博客详情响应');
          return;
        }
        
        let blogData = res.data;
        console.log('获取到的博客数据:', blogData);
        
        if (blogData) {
          // 处理作者信息（嵌套对象）
          let authorName = '未知作者';
          let authorId = '';
          let authorAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
          
          if (blogData.author) {
            if (typeof blogData.author === 'object') {
              // 作者是对象格式
              authorName = blogData.author.displayName || blogData.author.nickname || blogData.author.username || '未知作者';
              authorId = blogData.author.id || '';
              authorAvatar = blogData.author.avatar || authorAvatar;
            } else {
              // 作者是字符串格式
              authorName = blogData.author;
            }
          }
          
          // 处理发布时间
          let publishDate = '';
          if (blogData.publishTime) {
            publishDate = new Date(blogData.publishTime).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            });
          } else if (blogData.createdAt) {
            publishDate = new Date(blogData.createdAt).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            });
          }
          
          // 确保所有必要字段都有值
        const blogInfo = {
          id: blogId,
          like_id: '',
          title: blogData.title || '无标题',
          author: authorName, // 确保是字符串
          authorId: authorId,
          avatar: authorAvatar,
          publishDate: publishDate,
          likeCount: blogData.likeCount || 0,
          collectCount: blogData.collectCount || 0,
          isLiked: false,
          isCollected: false,
          content: blogData.content || '',
          isVipOnly: blogData.isVipOnly || false, // 确保有 VIP 标识字段
          price: blogData.price !== undefined ? blogData.price : 0 // 添加价格字段
        };
          console.log('处理后的博客数据:', blogInfo);
          // 确保 blog 对象被正确更新
          this.$set(this, 'blog', blogInfo);
                    
          // 检查付费博客购买状态
          if (this.blog.price > 0 && this.userId) {
            await this.checkPurchaseStatus(blogId);
          }
                    
          // 获取评论列表
          await this.getComments();
        }
      } catch (error) {
        console.error('获取博客详情失败', error);
        this.$message.error('获取博客详情失败');
      } finally {
        this.detailLoading = false;
      }
    },

    // 获取评论列表
    async getComments() {
      if (!this.blog.id) return;
      
      // 保存当前博客ID，用于后续检查
      const currentBlogId = this.blog.id;
      
      this.commentLoading = true;
      try {
        // 使用实际API获取评论列表
        const res = await GetCommentsByPost(currentBlogId);
        
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的评论列表响应');
          return;
        }
        
        let commentsData = res.data;
        console.log('获取到的评论数据:', commentsData);
        
        // 确保commentsData是数组
        if (!Array.isArray(commentsData)) {
          commentsData = [];
        }
        
        console.log('解析出的评论数据:', commentsData);
        
        // 打印第一条评论的详细结构，以便调试
        if (commentsData.length > 0) {
          console.log('第一条评论的详细结构:', commentsData[0]);
        }
        
        // 转换评论数据格式，确保字段名一致
        const convertedComments = commentsData.map(comment => {
          // 打印每条评论的结构，以便调试
          console.log('原始评论数据:', comment);
          
          // 确定评论人的昵称和头像
          let nickname = '匿名用户';
          let avatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
          
          // 如果是博客作者的评论，使用博客作者的信息
          if (comment.authorId === this.blog.authorId) {
            nickname = this.blog.author;
            avatar = this.blog.avatar;
          } else if (comment.nickname) {
            // 如果评论数据中有昵称，使用评论数据中的昵称
            nickname = comment.nickname;
            avatar = comment.avatar || avatar;
          } else if (comment.author) {
            // 如果评论数据中有作者信息，使用作者信息
            if (typeof comment.author === 'object') {
              nickname = comment.author.displayName || comment.author.nickname || comment.author.username || '匿名用户';
              avatar = comment.author.avatar || avatar;
            } else {
              nickname = comment.author;
            }
          } else if (comment.username) {
            // 如果评论数据中有用户名，使用用户名
            nickname = comment.username;
          }
          
          const convertedComment = {
            id: comment.id || comment.commentId,
            parentId: comment.parentCommentId || comment.parent_id || null,
            postId: comment.postId || comment.post_id || currentBlogId,
            content: comment.content || comment.body || '',
            nickname: nickname,
            avatar: avatar,
            createTime: comment.createTime || comment.createdAt || comment.createDate || new Date().toISOString(),
            likeCount: comment.likes || 0,
            status: comment.status || 'normal',
            authorId: comment.authorId || comment.author?.id || null,
            replyTo: '', // 初始化回复对象为空白
            isAuthor: comment.authorId === this.blog.authorId // 添加是否为作者的标识
          };
          
          // 打印转换后的评论数据，以便调试
          console.log('转换后的评论数据:', convertedComment);
          
          return convertedComment;
        });
        
        // 第二次遍历，设置回复对象的昵称
        convertedComments.forEach(comment => {
          if (comment.parentId) {
            const parentComment = convertedComments.find(c => c.id === comment.parentId);
            if (parentComment) {
              comment.replyTo = parentComment.nickname || '匿名用户';
            }
          }
        });
        
        console.log('转换后的评论数据:', convertedComments);
        this.comments = convertedComments;
      } catch (error) {
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的评论列表错误');
          return;
        }
        
        console.error('获取评论列表失败', error);
        this.$message.error('获取评论列表失败：' + (error.message || '网络错误'));
      } finally {
        this.commentLoading = false;
      }
    },

    /**
     * 转换单个评论数据格式
     * @param {Object} comment - 原始评论数据
     * @param {Array} allComments - 所有评论数据，用于查找回复对象
     * @returns {Object} 转换后的评论数据
     */
     convertCommentData(comment, allComments = null) {
      // 确定评论人的昵称和头像
      let nickname = '匿名用户';
      let avatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
      const authorId = comment.authorId || comment.author?.id || this.userId;
      
      // 如果是博客作者的评论，使用博客作者的信息
      if (authorId === this.blog.authorId) {
        nickname = this.blog.author;
        avatar = this.blog.avatar;
      } else if (comment.nickname) {
        // 如果评论数据中有昵称，使用评论数据中的昵称
        nickname = comment.nickname;
        avatar = comment.avatar || avatar;
      } else if (comment.author) {
        // 如果评论数据中有作者信息，使用作者信息
        if (typeof comment.author === 'object') {
          nickname = comment.author.displayName || comment.author.nickname || comment.author.username || '匿名用户';
          avatar = comment.author.avatar || avatar;
        } else {
          nickname = comment.author;
        }
      } else if (comment.username) {
        // 如果评论数据中有用户名，使用用户名
        nickname = comment.username;
      } else if (this.username) {
        // 如果当前用户已登录，使用当前用户的信息
        nickname = this.username;
        avatar = this.userAvatar || avatar;
      }
      
      const convertedComment = {
        id: comment.id || comment.commentId,
        parentId: comment.parentCommentId || comment.parent_id || null,
        postId: comment.postId || comment.post_id || this.blog.id,
        content: comment.content || comment.body || '',
        nickname: nickname,
        avatar: avatar,
        createTime: comment.createTime || comment.createdAt || comment.createDate || new Date().toISOString(),
        likeCount: comment.likes || 0,
        status: comment.status || 'normal',
        authorId: authorId,
        replyTo: '', // 初始化回复对象为空白
        isAuthor: authorId === this.blog.authorId // 添加是否为作者的标识
      };
      
      // 设置回复对象的昵称
      if (convertedComment.parentId) {
        let parentComment = null;
        if (allComments) {
          parentComment = allComments.find(c => c.id === convertedComment.parentId);
        } else {
          parentComment = this.comments.find(c => c.id === convertedComment.parentId);
        }
        
        if (parentComment) {
          convertedComment.replyTo = parentComment.nickname || '匿名用户';
        }
      }
      
      // 打印转换后的评论数据，以便调试
      console.log('convertCommentData 转换后的评论数据:', convertedComment);
      
      return convertedComment;
    },


    /**
     * 检查收藏状态
     */
    async checkCollectStatus() {
      if (!this.userId || !this.blog.id) return;
      
      // 保存当前博客ID，用于后续检查
      const currentBlogId = this.blog.id;
      
      try {
        // 使用实际API检查收藏状态
        const res = await IsCollected(this.userId, 'blog', this.blog.id);
        
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的收藏状态响应');
          return;
        }
        
        // 处理响应数据
        const isCollected = res.data || false;
        this.blog.isCollected = isCollected;
        this.blog.collect_id = isCollected ? res.data.id : '';
        console.log('用户收藏状态:', isCollected);
      } catch (error) {
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的收藏状态错误');
          return;
        }
        
        // 未收藏状态
        this.blog.isCollected = false;
        this.blog.collect_id = '';
      }
    },

    /**
     * 检查用户点赞状态
     */
    async checkLikeStatus() {
      if (!this.userId || !this.blog.id) return;
      
      // 保存当前博客ID，用于后续检查
      const currentBlogId = this.blog.id;
      
      try {
        // 使用实际API检查点赞状态
        const res = await CheckUserLiked(this.userId, 'blog', this.blog.id);
        
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的点赞状态响应');
          return;
        }
        
        // 处理响应数据
        const isLiked = res.data || false;
        this.blog.isLiked = isLiked;
        this.blog.like_id = isLiked ? res.data.id : '';
        console.log('用户点赞状态:', isLiked);
      } catch (error) {
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的点赞状态错误');
          return;
        }
        
        // 未点赞状态
        this.blog.isLiked = false;
        this.blog.like_id = '';
      }
    },

    /**
     * 处理点赞/取消点赞
     */
    async handleLike() {
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.likeLoading = true;
      try {
        if (this.blog.isLiked) {
          // 取消点赞
          if (this.blog.like_id) {
            await DeleteLike(this.blog.like_id);
            this.blog.isLiked = false;
            this.blog.likeCount--;
            this.blog.like_id = '';
            this.$message.success('取消点赞成功');
          }
        } else {
          // 点赞
          const likeData = {
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          };
          console.log('发送点赞请求:', likeData);
          const res = await AddLike(likeData);
          console.log('点赞响应:', res);
          // 后端直接返回LikeRecord对象
          if (res.data && res.data.id) {
            this.blog.isLiked = true;
            this.blog.likeCount++;
            this.blog.like_id = res.data.id;
            console.log('点赞成功，点赞记录ID:', res.data.id);
            this.$message.success('点赞成功');
          }
        }
      } catch (error) {
        console.error('处理点赞失败', error);
        this.$message.error('操作失败，请稍后重试');
      } finally {
        this.likeLoading = false;
      }
    },

    /**
     * 处理收藏/取消收藏
     */
    async handleCollect() {
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.collectLoading = true;
      try {
        if (this.blog.isCollected) {
          // 取消收藏
          if (this.blog.collect_id) {
            await CancelCollectBlog(this.blog.collect_id);
            this.blog.isCollected = false;
            this.blog.collectCount--;
            this.blog.collect_id = '';
            this.$message.success('取消收藏成功');
          }
        } else {
          // 收藏
          const collectData = {
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          };
          console.log('发送收藏请求:', collectData);
          const res = await CollectBlog(collectData);
          console.log('收藏响应:', res);
          // 后端返回收藏记录对象
          if (res.data && res.data.id) {
            this.blog.isCollected = true;
            this.blog.collectCount++;
            this.blog.collect_id = res.data.id; // 保存收藏记录ID，用于取消收藏
            console.log('收藏成功，收藏记录ID:', res.data.id);
            this.$message.success('收藏成功');
          }
        }
      } catch (error) {
        console.error('处理收藏失败', error);
        this.$message.error('操作失败，请稍后重试');
      } finally {
        this.collectLoading = false;
      }
    },

    /**
     * 跳转到作者详情页
     */
    goToAuthor() {
      if (this.blog.authorId) {
        this.$router.push(`/user/${this.blog.authorId}`);
      }
    },

    /**
     * 跳转到 VIP开通页面（充值页面）
     */
    goToVipPage() {
      this.vipDialogVisible = false;
      // 跳转到充值/VIP开通页面（假设已有 /wallet 路由）
      this.$router.push('/wallet');
    },
    
    /**
     * 检查付费博客购买状态
     */
    async checkPurchaseStatus(blogId) {
      if (!this.userId) return;
          
      try {
        const res = await this.$axios.get(`/api/content-purchase/check/${blogId}`, {
          headers: { 'X-User-Id': this.userId }
        });
        this.hasPurchased = res.data;
        console.log('购买状态:', this.hasPurchased);
      } catch (error) {
        console.error('检查购买状态失败', error);
        // 默认设置为未购买
        this.hasPurchased = false;
      }
    },
    
    /**
     * 购买付费博客
     */
    async purchaseBlog() {
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
          
      this.purchaseSubmitting = true;
      try {
        // 创建订单
        const res = await this.$axios.post('/api/content-purchase/create-order', {
          blogId: this.blog.id,
          paymentMethod: 'balance'  // 默认使用余额支付
        }, {
          headers: { 'X-User-Id': this.userId }
        });
            
        if (res.data.success) {
          if (res.data.alreadyPurchased) {
            this.hasPurchased = true;
            this.$message.success('您已购买过该博客');
          } else {
            // 显示支付确认对话框
            this.purchaseAmount = res.data.amount;
            this.purchaseOrderNo = res.data.orderNo;
            this.showPurchaseDialog = true;
          }
        } else {
          this.$message.error(res.data.message || '创建订单失败');
        }
      } catch (error) {
        console.error('创建订单失败', error);
        this.$message.error('创建订单失败：' + (error.response?.data?.message || error.message || '网络错误'));
      } finally {
        this.purchaseSubmitting = false;
      }
    },
    
    /**
     * 确认支付
     */
    async confirmPayment() {
      if (!this.purchaseOrderNo) return;
          
      this.purchaseSubmitting = true;
      try {
        await this.$axios.post('/api/content-purchase/complete', null, {
          params: {
            blogId: this.blog.id,
            orderNo: this.purchaseOrderNo
          },
          headers: { 'X-User-Id': this.userId }
        });
            
        this.hasPurchased = true;
        this.showPurchaseDialog = false;
        this.$message.success('购买成功');
      } catch (error) {
        console.error('支付失败', error);
        this.$message.error('支付失败：' + (error.response?.data?.message || error.message || '网络错误'));
      } finally {
        this.purchaseSubmitting = false;
      }
    },

    /**
     * 显示回复输入框
     * @param {Object} comment - 要回复的评论对象
     * @param {Object} parentComment - 父评论（可选）
     */
    showReplyInput(comment, parentComment = null) {
      this.replyTarget = comment;
      this.replyContent = '';
    },

    /**
     * 取消回复
     */
    cancelReply() {
      this.replyTarget = null;
      this.replyContent = '';
    },

    /**
     * 提交顶级评论
     */
     async submitTopLevelComment() {
      if (!this.newComment.trim()) return;
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.submitting = true;
      try {
        const res = await AddComment({
          content: this.newComment,
          parentCommentId: null,
          postId: this.blog.id,
          authorId: this.userId
        });
        console.log('发表评论响应:', res);
        // 后端直接返回Comment对象
        if (res.data && res.data.id) {
          // 转换新评论的数据格式，确保与获取的评论格式一致
          const newCommentData = this.convertCommentData(res.data);
          this.comments.push(newCommentData);
          this.newComment = '';
          this.$message.success('评论发表成功');
        }
      } catch (error) {
        console.error('发表评论出错', error);
        this.$message.error('网络错误，请稍后重试');
      } finally {
        this.submitting = false;
      }
    },

/**
     * 提交回复
     * @param {Object} parentComment - 父评论（顶级评论）
     * @param {Object} replyToComment - 被回复的评论（可选，用于嵌套回复）
     */
     async submitReply(parentComment, replyToComment = null) {
      if (!this.replyContent.trim()) return;
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.replySubmitting = true;
      try {
        // 由于后端没有专门的回复接口，我们直接使用AddComment接口
        const res = await AddComment({
          content: this.replyContent,
          parentCommentId: replyToComment ? replyToComment.id : parentComment.id,
          postId: this.blog.id,
          authorId: this.userId
        });
        console.log('发表回复响应:', res);
        // 后端直接返回Comment对象
        if (res.data && res.data.id) {
          // 转换新回复的数据格式，确保与获取的评论格式一致
          const newReplyData = this.convertCommentData(res.data);
          this.comments.push(newReplyData);
          this.replyTarget = null;
          this.replyContent = '';
          this.$message.success('回复发表成功');
        }
      } catch (error) {
        console.error('发表回复出错', error);
        this.$message.error('网络错误，请稍后重试');
      } finally {
        this.replySubmitting = false;
      }
    },
  },
};
</script>

<style scoped>
/* ========== 全局容器 ========== */
.blog-detail-container {
  max-width: 900px;
  margin: 30px auto;
  padding: 0 20px;
  min-height: 100vh;
  position: relative;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* ========== 头部卡片 ========== */
.blog-header {
  margin-bottom: 25px;
  border-radius: 20px !important;
  border: 1px solid rgba(0, 0, 0, 0.03);
  overflow: hidden;
  background: white !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.blog-header:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.05) !important;
}

.blog-title {
  margin: 0 0 16px 0;
  font-size: 2.2rem;
  color: #1e293b;
  line-height: 1.3;
  font-weight: 600;
  background: linear-gradient(135deg, #1e293b, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 标题包装器 */
.blog-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

/* 价格标签样式 */
.price-tag {
  border: none;
  font-weight: 600;
  border-radius: 12px;
  padding: 4px 12px;
  font-size: 13px;
  white-space: nowrap;
}

.blog-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.author-name {
  font-size: 1.2rem;
  font-weight: 500;
  color: #3b82f6;
  cursor: pointer;
  transition: color 0.2s;
}

.author-name:hover {
  color: #2563eb;
}

.publish-date {
  color: #64748b;
  font-size: 0.95rem;
}

/* 互动按钮组 */
.action-buttons {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 5px;
}

.like-count,
.collect-count {
  margin-right: 15px;
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

/* 点赞/收藏按钮美化 */
.action-buttons .el-button {
  border: none;
  background: #f1f5f9;
  color: #475569;
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: scale(1.1);
  background: #e2e8f0;
}

.action-buttons .el-button.el-button--primary {
  background: #3b82f6;
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

.action-buttons .el-button.el-button--warning {
  background: #f59e0b;
  color: white;
  box-shadow: 0 4px 10px rgba(245, 158, 11, 0.2);
}

/* 点赞按钮高亮动画 */
.liked-button {
  animation: pulse 0.5s ease-in-out;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

/* ========== 正文卡片 ========== */
.blog-content {
  border-radius: 20px !important;
  background-color: white !important;
  margin-bottom: 25px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.content-body {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #334155;
}

/* ========== VIP 内容处理 ========== */
.vip-content-wrapper {
  position: relative;
  overflow: hidden;
}

.content-preview {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #334155;
  /* 限制预览高度，超出部分模糊处理 */
  max-height: 300px;
  overflow: hidden;
  position: relative;
}

.content-blur {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 150px;
  background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.98));
  backdrop-filter: blur(8px);
  z-index: 1;
}

.vip-prompt {
  position: relative;
  margin-top: 20px;
  padding: 30px 20px;
  text-align: center;
  background: linear-gradient(135deg, #fef3c7 0%, #fef9e7 100%);
  border-radius: 16px;
  border: 2px solid #fcd34d;
  z-index: 2;
}

.vip-prompt i {
  font-size: 42px;
  color: #f59e0b;
  margin-bottom: 15px;
  display: block;
}

.vip-prompt h3 {
  margin: 0 0 8px;
  color: #92400e;
  font-size: 18px;
  font-weight: 700;
}

.vip-prompt p {
  margin: 0 0 20px;
  color: #78350f;
  font-size: 14px;
  line-height: 1.6;
}

.vip-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.vip-actions .el-button {
  border-radius: 25px;
  padding: 10px 30px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
}

.vip-actions .el-button--primary {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: none;
  box-shadow: 0 4px 15px rgba(245, 158, 11, 0.3);
}

.vip-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245, 158, 11, 0.4);
}

.content-body h2 {
  margin: 28px 0 16px;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
  color: #1e293b;
}

.content-body p {
  margin: 16px 0;
}

.content-body code {
  background-color: #f1f5f9;
  padding: 2px 6px;
  border-radius: 6px;
  font-family: 'Fira Code', 'Courier New', monospace;
  color: #e11d48;
}

.content-body pre {
  background-color: #0f172a;
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
  border: none;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.content-body pre code {
  background-color: transparent;
  padding: 0;
  color: #e2e8f0;
  font-family: 'Fira Code', 'Courier New', monospace;
  font-size: 0.95rem;
}

/* 正文中的图片 */
.content-body img {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin: 20px 0;
}

/* ========== 评论区卡片 ========== */
.comment-section {
  border-radius: 20px !important;
  background-color: white !important;
  border: 1px solid rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.comment-header {
  font-weight: 600;
  font-size: 1.1rem;
  color: #1e293b;
  padding: 15px 20px;
  border-bottom: 1px solid #e2e8f0;
}

/* 评论列表 */
.comment-list {
  margin-bottom: 20px;
  padding: 0 20px;
}

/* 评论线程 */
.comment-thread {
  margin-bottom: 25px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 25px;
}

.comment-thread:last-child {
  border-bottom: none;
}

/* 单条评论 */
.comment-item {
  display: flex;
  gap: 15px;
  padding: 10px 0;
}

.comment-item .el-avatar {
  border: 2px solid #e2e8f0;
  transition: border-color 0.2s;
}

.comment-item:hover .el-avatar {
  border-color: #3b82f6;
}

.comment-content {
  flex: 1;
}

.comment-meta {
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.comment-author {
  font-weight: 600;
  color: #3b82f6;
  margin-right: 10px;
}

.comment-time {
  color: #94a3b8;
  font-size: 0.85rem;
}

.comment-text {
  color: #334155;
  line-height: 1.6;
}

/* 作者徽章 */
.author-badge {
  font-size: 12px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  border-radius: 20px;
  font-weight: normal;
  display: inline-block;
}

/* 回复列表 */
.replies {
  margin-left: 55px;
  margin-top: 15px;
}

.reply-item {
  display: flex;
  gap: 12px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #e2e8f0;
}

.reply-item:first-child {
  border-top: none;
  padding-top: 0;
}

.reply-content {
  flex: 1;
}

/* 回复对象提示 */
.reply-to {
  font-size: 12px;
  color: #94a3b8;
  background-color: #f1f5f9;
  padding: 2px 8px;
  border-radius: 20px;
  margin-left: 5px;
}

.reply-to::before {
  content: '@';
  margin-right: 2px;
}

/* 评论操作按钮 */
.comment-actions {
  margin-top: 8px;
}

.comment-actions .el-button {
  padding: 0;
  margin-right: 15px;
  font-size: 13px;
  color: #64748b;
  transition: color 0.2s;
}

.comment-actions .el-button:hover {
  color: #3b82f6;
  background: transparent !important;
}

.comment-actions .el-button i {
  margin-right: 3px;
  font-size: 14px;
}

/* 回复输入框 */
.reply-input-wrapper {
  margin-top: 15px;
  margin-left: 55px;
  padding: 15px;
  background-color: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.reply-input-wrapper.nested {
  margin-left: 0;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

/* 发表评论区域 */
.comment-input-area {
  margin-top: 20px;
  padding: 0 20px 20px;
}

.comment-input-area .el-textarea__inner {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.comment-input-area .el-textarea__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.comment-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-submit .el-button {
  border-radius: 30px;
  padding: 10px 25px;
  font-weight: 500;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  color: white;
  transition: transform 0.2s, box-shadow 0.2s;
}

.comment-submit .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.2);
}

.comment-submit .el-button:disabled {
  background: #cbd5e1;
  opacity: 0.6;
}

/* 无评论提示 */
.no-comment {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
  border-radius: 12px;
  margin: 20px;
}

/* ========== 回到顶部按钮 ========== */
.backtop-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  border-radius: 50%;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.backtop-inner:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.4);
}

.backtop-inner i {
  font-size: 20px;
  margin-bottom: 2px;
}

/* ========== 付费内容处理 ========== */
.paid-content-wrapper {
  position: relative;
  overflow: hidden;
}

.content-preview {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #334155;
  /* 限制预览高度，超出部分模糊处理 */
  max-height: 300px;
  overflow: hidden;
  position: relative;
}

.content-blur {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 150px;
  background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.98));
  backdrop-filter: blur(8px);
  z-index: 1;
}

.paid-prompt {
  position: relative;
  margin-top: 20px;
  padding: 40px 20px;
  text-align: center;
  background: linear-gradient(135deg, #e0e7ff 0%, #f0f4ff 100%);
  border-radius: 16px;
  border: 2px solid #6366f1;
  z-index: 2;
}

.paid-prompt i {
  font-size: 48px;
  color: #6366f1;
  margin-bottom: 15px;
  display: block;
}

.paid-prompt h3 {
  margin: 0 0 15px;
  color: #3730a3;
  font-size: 20px;
  font-weight: 700;
}

.price-tag {
  margin: 20px 0;
  padding: 15px;
  background: white;
  border-radius: 8px;
  display: inline-block;
}

.price-label {
  font-size: 14px;
  color: #6b7280;
  margin-right: 10px;
}

.price-value {
  font-size: 32px;
  font-weight: bold;
  color: #ef4444;
}

.purchase-benefit {
  margin: 15px 0 25px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
}

.purchase-actions .el-button--primary {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
}

.purchase-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

/* 购买确认对话框样式 */
.purchase-confirm-content p {
  margin: 10px 0;
  font-size: 15px;
  color: #374151;
}

.amount-info .amount {
  font-size: 28px;
  font-weight: bold;
  color: #ef4444;
  display: block;
  margin-top: 10px;
}

.balance-info {
  color: #6b7280;
  font-size: 13px;
  margin-top: 15px;
}

/* ========== 响应式设计 ========== */
@media screen and (max-width: 768px) {
  .blog-detail-container {
    padding: 0 15px;
    margin: 15px auto;
  }

  .blog-title {
    font-size: 1.8rem;
  }

  .blog-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .action-buttons {
    margin-left: 0;
  }

  .replies {
    margin-left: 30px;
  }

  .reply-input-wrapper {
    margin-left: 30px;
  }
}

@media screen and (max-width: 480px) {
  .blog-title {
    font-size: 1.5rem;
  }

  .comment-item {
    flex-direction: column;
    gap: 10px;
  }

  .comment-item .el-avatar {
    align-self: flex-start;
  }

  .replies {
    margin-left: 20px;
  }
}
</style>