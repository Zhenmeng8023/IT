<template>
  <div class="post-detail-container">
    <!-- 帖子内容卡片 -->
    <el-card class="post-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="loading">
      <div class="post-meta">
        <el-avatar :size="50" :src="post.avatar"></el-avatar>
        <span class="author-name">{{ post.author }}</span>
        <span class="publish-date">发布于 {{ post.publishDate }}</span>
      </div>
      <div class="post-content" v-html="post.content"></div>
    </el-card>

    <!-- 评论区卡片 -->
    <el-card class="comment-section" shadow="hover">
      <div slot="header" class="comment-header">
        <span>评论（{{ totalComments }}）</span>
        <div class="comment-tools">
          <!-- 只看楼主开关 -->
          <el-switch
            v-model="onlyAuthor"
            active-text="只看楼主"
            inactive-text="全部评论"
            @change="handleFilterChange"
          >
          </el-switch>
        </div>
      </div>

      <!-- 全局评论输入框（发表顶级评论） -->
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

      <!-- 评论列表 -->
      <div class="comment-list" v-loading="commentLoading">
        <!-- 顶级评论循环 -->
        <div v-for="topComment in topLevelComments" :key="topComment.id" class="comment-thread">
          <!-- 顶级评论 -->
          <div class="comment-item">
            <el-avatar :size="40" :src="topComment.avatar"></el-avatar>
            <div class="comment-content">
              <div class="comment-meta">
                <span class="comment-author">{{ topComment.nickname }}</span>
                <span class="comment-time">{{ formatTime(topComment.createTime) }}</span>
              </div>
              <div class="comment-text">{{ topComment.content }}</div>
              <div class="comment-actions">
                <el-button type="text" size="small" @click="showReplyInput(topComment)">
                  回复
                </el-button>
                <!-- 可选点赞 -->
                <!-- <el-button type="text" size="small">点赞 {{ topComment.likeCount }}</el-button> -->
              </div>
            </div>
          </div>

          <!-- 回复输入框（针对顶级评论） -->
          <div v-if="replyTarget && replyTarget.id === topComment.id" class="reply-input-wrapper">
            <el-input
              type="textarea"
              :rows="2"
              :placeholder="'回复 @' + topComment.nickname"
              v-model="replyContent"
              resize="none"
            ></el-input>
            <div class="reply-actions">
              <el-button size="small" @click="cancelReply">取消</el-button>
              <el-button type="primary" size="small" @click="submitReply(topComment)" :loading="replySubmitting">
                提交回复
              </el-button>
            </div>
          </div>

          <!-- 递归显示所有层级的回复 -->
          <div v-if="topComment.children && topComment.children.length" class="replies">
            <div v-for="reply in topComment.children" :key="reply.id" class="reply-item">
              <el-avatar :size="30" :src="reply.avatar"></el-avatar>
              <div class="reply-content">
                <div class="comment-meta">
                  <span class="comment-author">{{ reply.nickname }}</span>
                  <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                </div>
                <div class="comment-text">{{ reply.content }}</div>
                <div class="comment-actions">
                  <el-button type="text" size="small" @click="showReplyInput(reply, topComment)">
                    回复
                  </el-button>
                </div>
              </div>

              <!-- 针对回复的回复输入框（仍作为该顶级评论的子级） -->
              <div v-if="replyTarget && replyTarget.id === reply.id" class="reply-input-wrapper nested">
                <el-input
                  type="textarea"
                  :rows="2"
                  :placeholder="'回复 @' + reply.nickname"
                  v-model="replyContent"
                  resize="none"
                ></el-input>
                <div class="reply-actions">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button type="primary" size="small" @click="submitReply(topComment, reply)" :loading="replySubmitting">
                    提交回复
                  </el-button>
                </div>
              </div>

              <!-- 递归显示嵌套回复 -->
              <div v-if="reply.children && reply.children.length" class="replies nested">
                <div v-for="nestedReply in reply.children" :key="nestedReply.id" class="reply-item">
                  <el-avatar :size="25" :src="nestedReply.avatar"></el-avatar>
                  <div class="reply-content">
                    <div class="comment-meta">
                      <span class="comment-author">{{ nestedReply.nickname }}</span>
                      <span class="comment-time">{{ formatTime(nestedReply.createTime) }}</span>
                    </div>
                    <div class="comment-text">{{ nestedReply.content }}</div>
                    <div class="comment-actions">
                      <el-button type="text" size="small" @click="showReplyInput(nestedReply, topComment)">
                        回复
                      </el-button>
                    </div>
                  </div>

                  <!-- 针对嵌套回复的回复输入框 -->
                  <div v-if="replyTarget && replyTarget.id === nestedReply.id" class="reply-input-wrapper nested">
                    <el-input
                      type="textarea"
                      :rows="2"
                      :placeholder="'回复 @' + nestedReply.nickname"
                      v-model="replyContent"
                      resize="none"
                    ></el-input>
                    <div class="reply-actions">
                      <el-button size="small" @click="cancelReply">取消</el-button>
                      <el-button type="primary" size="small" @click="submitReply(topComment, nestedReply)" :loading="replySubmitting">
                        提交回复
                      </el-button>
                    </div>
                  </div>

                  <!-- 递归显示更深层次的回复 -->
                  <div v-if="nestedReply.children && nestedReply.children.length" class="replies nested">
                    <div v-for="deepReply in nestedReply.children" :key="deepReply.id" class="reply-item">
                      <el-avatar :size="20" :src="deepReply.avatar"></el-avatar>
                      <div class="reply-content">
                        <div class="comment-meta">
                          <span class="comment-author">{{ deepReply.nickname }}</span>
                          <span class="comment-time">{{ formatTime(deepReply.createTime) }}</span>
                        </div>
                        <div class="comment-text">{{ deepReply.content }}</div>
                        <div class="comment-actions">
                          <el-button type="text" size="small" @click="showReplyInput(deepReply, topComment)">
                            回复
                          </el-button>
                        </div>
                      </div>

                      <!-- 针对深层回复的回复输入框 -->
                      <div v-if="replyTarget && replyTarget.id === deepReply.id" class="reply-input-wrapper nested">
                        <el-input
                          type="textarea"
                          :rows="2"
                          :placeholder="'回复 @' + deepReply.nickname"
                          v-model="replyContent"
                          resize="none"
                        ></el-input>
                        <div class="reply-actions">
                          <el-button size="small" @click="cancelReply">取消</el-button>
                          <el-button type="primary" size="small" @click="submitReply(topComment, deepReply)" :loading="replySubmitting">
                            提交回复
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
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
    </el-card>

    <!-- 回到顶部按钮 -->
    <el-backtop target=".post-detail-container" :bottom="100" :right="40">
      <div class="backtop-inner">
        <i class="el-icon-arrow-up"></i>
        <span>顶部</span>
      </div>
    </el-backtop>
  </div>
</template>

<script>
export default {
  layout: 'circle',
  data() {
    return {
      postId: this.$route.params.id, // 从路由获取帖子ID
      circleId: this.$route.params.circleId || 1,
      loading: false,
      currentUser: null,
      userId: null,
      username: '',
      userAvatar: '',
      rawComments: [],
      // 帖子详情数据实验开始
      post: {
        title: '【精华】Vue 3 组合式 API 最佳实践分享',
        author: '前端小王子',
        avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
        publishDate: '2024-03-15 10:30',
        content: `
          <h2>为什么要使用组合式 API？</h2>
          <p>随着项目规模的增长，Vue 2 的选项式 API 可能导致代码关注点分散，而组合式 API 允许我们将逻辑按功能组织，提升可维护性。</p>
          <p>在 Vue 3 中，组合式 API 已经成为官方推荐的方式。下面是一个简单的例子：</p>
          <pre><code>
import { ref, computed } from 'vue';

export default {
  setup() {
    const count = ref(0);
    const double = computed(() => count.value * 2);
    function increment() {
      count.value++;
    }
    return { count, double, increment };
  }
};
          </code></pre>
          <p>这样可以将响应式状态、计算属性和方法集中在一个 <code>setup</code> 函数中，让组件更加清晰。</p>
          <p>在实际项目中，我总结了几个最佳实践：</p>
          <ul>
            <li>使用 <code>ref</code> 处理基本类型，<code>reactive</code> 处理对象</li>
            <li>将相关逻辑封装到自定义组合函数中</li>
            <li>使用 <code>toRefs</code> 保持响应式解构</li>
            <li>合理使用 <code>watch</code> 和 <code>watchEffect</code></li>
          </ul>
          <p>大家有什么问题和经验，欢迎在评论区交流讨论！</p>
        `,
      },
     
      commentLoading: false,
      // 排序与过滤
      sortOrder: 'desc', // 'desc' 最新，'asc' 最早
      onlyAuthor: false,
      // 新评论（顶级）
      newComment: '',
      submitting: false,
      // 回复相关
      replyTarget: null, // 当前正在回复的评论对象（顶级或回复）
      replyContent: '',
      replySubmitting: false,
    };
  },

  watch: {
    rawComments: {
      handler(newVal) {
        console.log('rawComments 更新:', newVal);
      },
      deep: true
    },
    topLevelComments: {
      handler(newVal) {
        console.log('topLevelComments 更新:', newVal);
      },
      deep: true
    }
  },

  computed: {
    totalComments() {
      return this.rawComments.length;
    },
    // 根据只看楼主过滤后的评论
    filteredComments() {
      if (!this.onlyAuthor) return this.rawComments;
      
      // 获取帖子作者名称（处理可能的数据结构）
      let authorName = '';
      if (typeof this.post.author === 'string') {
        authorName = this.post.author;
      } else if (this.post.author && typeof this.post.author === 'object') {
        authorName = this.post.author.nickname || this.post.author.name || '';
      }
      
      console.log('作者名称:', authorName);
      console.log('评论作者列表:', this.rawComments.map(c => c.nickname));
      
      // 过滤出作者发表的评论
      return this.rawComments.filter(c => {
        // 去除可能的前后空格，并确保类型一致
        const commentAuthor = (c.nickname || '').toString().trim();
        const postAuthor = authorName.toString().trim();
        return commentAuthor.toLowerCase() === postAuthor.toLowerCase();
      });
    },
    // 按时间排序后的评论
    sortedComments() {
      const sorted = [...this.filteredComments];
      sorted.sort((a, b) => {
        const timeA = new Date(a.createTime).getTime();
        const timeB = new Date(b.createTime).getTime();
        return this.sortOrder === 'desc' ? timeB - timeA : timeA - timeB;
      });
      return sorted;
    },
// 构建顶级评论及其 children（扁平化结构，最多两级显示）
topLevelComments() {
      console.log('开始构建评论树，postId:', this.postId);
      console.log('原始评论数据:', this.rawComments);
      
      // 首先创建所有评论的映射
      const commentMap = new Map();
      const topList = [];
      const allComments = [...this.sortedComments];

      // 第一次遍历：找出顶级评论（直接回复主题帖的评论）
      allComments.forEach(comment => {
        // 创建评论副本并初始化children数组
        const commentWithChildren = { ...comment, children: [] };
        commentMap.set(comment.id, commentWithChildren);
        
        // 找出顶级评论（parentCommentId等于主题帖ID）
        if (comment.parentCommentId == this.postId) {
          topList.push(commentWithChildren);
        }
      });

      // 第二次遍历：将所有子评论（包括多层嵌套的）都添加到对应顶级评论的children中
      allComments.forEach(comment => {
        // 跳过主题帖本身和顶级评论
        if (comment.id == this.postId || comment.parentCommentId == this.postId) {
          return;
        }
        
        // 如果当前评论是回复某个评论的
        if (comment.parentCommentId && comment.parentCommentId != this.postId) {
          // 找到该评论所属的顶级评论
          let topLevelParent = null;
          let currentParentId = comment.parentCommentId;
          
          // 循环向上查找，直到找到顶级评论（其parentCommentId为postId）
          while (currentParentId) {
            const parentComment = commentMap.get(currentParentId) || 
                                allComments.find(c => c.id === currentParentId);
            
            if (!parentComment) {
              break; // 找不到父评论，跳出循环
            }
            
            if (parentComment.parentCommentId == this.postId) {
              // 找到了顶级评论
              topLevelParent = commentMap.get(parentComment.id);
              break;
            } else {
              // 继续向上查找
              currentParentId = parentComment.parentCommentId;
            }
          }
          
          // 如果找到了对应的顶级评论，则将当前评论添加到其children中
          if (topLevelParent) {
            // 避免重复添加
            if (!topLevelParent.children.some(child => child.id === comment.id)) {
              topLevelParent.children.push(commentMap.get(comment.id));
            }
          } else {
            // 如果找不到顶级父评论，可能是数据问题，尝试查找最近的顶级评论
            console.warn('未找到顶级父评论，尝试查找最接近的评论', comment);
          }
        }
      });

      console.log('构建完成的评论树:', topList);

      // 对所有层级的评论按时间排序
      const sortComments = (comments) => {
        if (comments && comments.length) {
          // 对当前层级的评论排序
          comments.sort((a, b) => {
            const timeA = new Date(a.createTime).getTime();
            const timeB = new Date(b.createTime).getTime();
            return this.sortOrder === 'desc' ? timeB - timeA : timeA - timeB;
          });
        }
      };

      // 对顶级评论排序
      sortComments(topList);
      // 对每个顶级评论下的子评论也进行排序
      topList.forEach(topComment => {
        sortComments(topComment.children);
      });

      return topList;
    },
  },
  created() {
    // 从路由获取帖子ID，始终尝试获取真实数据
    if (this.postId) {
      this.fetchPostDetail();
      this.fetchComments();
    } else {
      // 使用模拟数据
      this.loading = false;
      this.commentLoading = false;
    }
  },

  mounted() {
  // 其他初始化代码...
  this.getCurrentUser();
},

  methods: {
    /**
   * 获取当前用户信息
   */
   async getCurrentUser() {
    try {
      // 直接调用后端接口获取当前用户信息
      const res = await this.$axios.get('/api/users/current');
      
      // 处理不同的响应格式
      let userData = null;
      
      if (res.data && typeof res.data.code !== 'undefined') {
        // 标准格式 {code: 0, data: {...}}
        if (res.data.code === 0 && res.data.data) {
          userData = res.data.data;
        }
      } else if (res.data && res.data.id) {
        // 直接返回用户对象
        userData = res.data;
      } else if (res.id) {
        // 直接返回用户对象（无data字段）
        userData = res;
      }
      
      if (userData) {
        this.currentUser = userData;
        this.userId = userData.id;
        this.username = userData.username || userData.nickname;
        this.userAvatar = userData.avatar || userData.avatarUrl;
      }
    } catch (error) {
      console.error('获取当前用户信息失败', error);
      // 404 表示用户未登录，不是错误
      if (error.response && error.response.status === 404) {
        console.log('用户未登录');
      }
    }
  },
// 获取帖子详情
async fetchPostDetail() {
  this.loading = true;
  try {
    console.log('开始请求帖子详情，postId:', this.postId);
    
    // 由于axios配置问题，response直接是数据对象，不是标准响应
    const postData = await this.$axios.get(`/api/circle/comments/${this.postId}`);
    console.log('帖子详情API响应:', postData);
    
    if (postData && postData.id) {
      // 转换数据格式以匹配前端期望
      this.post = {
        id: postData.id || this.postId,
        title: postData.title || `帖子 #${postData.id || this.postId}`,
        author: this.parseAuthorInfo(postData.author || { 
          username: postData.username || '匿名用户',
          nickname: postData.nickname || '匿名用户',
          avatarUrl: postData.avatarUrl || null
        }),
        avatar: postData.avatar || 
                (postData.author && postData.author.avatarUrl) || 
                'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
        publishDate: postData.createdAt ? 
                     this.formatTime(postData.createdAt) : 
                     this.formatTime(new Date().toISOString()),
        content: postData.content || '暂无内容',
        likes: postData.likes || 0,
        replyCount: postData.replyCount || 0,
        circleId: postData.circleId || null
      };
      
      console.log('设置到组件中的帖子数据:', this.post);
    } else {
      console.error('获取帖子失败：数据格式错误', postData);
      this.$message.error('获取帖子失败：数据格式错误');
    }
  } catch (error) {
    console.error('获取帖子详情出错:', error);
    this.$message.error('网络错误，无法获取帖子详情');
  } finally {
    this.loading = false;
  }
},   

// 获取评论列表
async fetchComments() {
  this.commentLoading = true;
  try {
    console.log('开始请求评论列表，postId:', this.postId);
    
    // 使用正确的API接口来获取主题帖的所有评论
    const commentsResponse = await this.$axios.get(`/api/circle/posts/${this.postId}/comments/all`);
    console.log('评论API原始响应:', commentsResponse);
    
    // 检查响应数据
    let commentsData = [];
    
    // 处理不同响应结构
    if (commentsResponse) {
      // 情况1: response本身就是数组
      if (Array.isArray(commentsResponse)) {
        commentsData = commentsResponse;
      } 
      // 情况2: response是对象，包含data数组
      else if (commentsResponse.data) {
        // 标准axios响应格式
        if (commentsResponse.data.code === 0 && Array.isArray(commentsResponse.data.data)) {
          commentsData = commentsResponse.data.data;
        } 
        // 其他可能的格式
        else if (Array.isArray(commentsResponse.data)) {
          commentsData = commentsResponse.data;
        } 
        // 尝试从其他常见字段获取数组
        else if (commentsResponse.data.list && Array.isArray(commentsResponse.data.list)) {
          commentsData = commentsResponse.data.list;
        } 
        // 如果data是对象，尝试提取数组
        else if (typeof commentsResponse.data === 'object') {
          // 查找对象中的数组字段
          for (const key in commentsResponse.data) {
            if (Array.isArray(commentsResponse.data[key])) {
              commentsData = commentsResponse.data[key];
              console.log(`从字段 ${key} 中获取评论数据`);
              break;
            }
          }
        }
      }
      // 情况3: response是对象，但没有data字段，尝试从其他字段获取数组
      else if (typeof commentsResponse === 'object' && !Array.isArray(commentsResponse)) {
        // 尝试查找对象中的数组字段
        for (const key in commentsResponse) {
          if (Array.isArray(commentsResponse[key])) {
            commentsData = commentsResponse[key];
            console.log(`从字段 ${key} 中获取评论数据`);
            break;
          }
        }
      }
    }
    
    console.log('解析后的评论数据:', commentsData);
    
    // 转换评论数据格式 - 保留所有字段用于调试
    this.rawComments = commentsData.map(comment => {
      return {
        id: comment.id || comment.commentId,
        parentCommentId: comment.parentCommentId || comment.parent_id || comment.parent_comment_id || null,
        postId: comment.postId || comment.post_id || this.postId, // 确保postId正确
        // 解析作者信息
        author: this.parseAuthorInfo(comment.author || comment.user || comment.creator || {}),
        nickname: comment.author?.nickname || comment.author?.username || 
                comment.user?.nickname || comment.user?.username ||
                comment.creator?.nickname || comment.creator?.username || '匿名用户',
        avatar: comment.avatar || comment.userAvatar || 
              (comment.user && comment.user.avatar) || 
              (comment.author && comment.author.avatarUrl) || 
              'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
        createTime: comment.createTime || comment.createdAt || comment.create_date || new Date().toISOString(),
        publishDate: comment.createTime || comment.createdAt || comment.create_date || new Date().toISOString(),
        content: comment.content || comment.body || '',
        likeCount: comment.likeCount || comment.likes || 0
      };
    });
    
    console.log('转换后的评论列表:', this.rawComments);
    
  } catch (error) {
    console.error('获取评论列表出错', error);
    this.$message.error('网络错误，无法获取评论');
  } finally {
    this.commentLoading = false;
  }
},
    // 格式化时间
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    },
    // 排序变化
    handleSortChange() {
      // 计算属性自动更新
    },
    // 只看楼主变化
    handleFilterChange() {
      // 计算属性自动更新
    },
     // 提交顶级评论
    async submitTopLevelComment() {
      if (!this.newComment.trim()) return;
      this.submitting = true;
      try {
        console.log('提交评论，内容:', this.newComment);
        
        // 发送评论请求
        const response = await this.$axios.post('/api/circle/comments', {
          circleId: this.post.circleId || 1, // 从帖子信息中获取圈子ID，默认为1
          parentCommentId: this.postId, // 顶级评论，父评论ID为主题帖ID
          content: this.newComment,
          authorId: this.userId
        });
        
        console.log('提交评论API响应:', response);
        
        // 处理响应数据
        let responseData = response;
        let success = false;
        let message = '';
        let commentId = null;
        
        // 检查响应结构
        if (response) {
          // 情况1: response是标准API响应格式 {code: 0, data: {...}, message: "成功"}
          if (response.code === 0) {
            success = true;
            message = response.message || '评论发表成功';
            if (response.data) {
              responseData = response.data;
              commentId = response.data.id;
            }
          }
          // 情况2: response直接是数据对象
          else if (response.id) {
            success = true;
            message = '评论发表成功';
            responseData = response;
            commentId = response.id;
          }
          // 情况3: response是对象，包含data字段
          else if (response.data) {
            if (response.data.code === 0) {
              success = true;
              message = response.data.message || '评论发表成功';
              if (response.data.data) {
                responseData = response.data.data;
                commentId = response.data.data.id;
              } else {
                responseData = response.data;
                commentId = response.data.id;
              }
            } else if (response.data.id) {
              success = true;
              message = '评论发表成功';
              responseData = response.data;
              commentId = response.data.id;
            }
          }
        }
        
        if (success) {
          // 成功提交后添加到本地列表
          const newComment = {
            id: commentId || Date.now(),
            parentCommentId: this.postId, // 顶级评论，父评论ID为主题帖ID
            postId: this.postId, // 设置postId为主题帖ID
            author: this.parseAuthorInfo(responseData.author || { 
              username: this.username || '当前用户',
              nickname: this.nickname || '当前用户'
            }),
            nickname: this.nickname || '当前用户',
            avatar: this.userAvatar||'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            createTime: responseData.createdAt || new Date().toISOString(),
            publishDate: responseData.createdAt ? 
                        this.formatTime(responseData.createdAt) : 
                        this.formatTime(new Date().toISOString()),
            content: responseData.content || this.newComment,
            likeCount: responseData.likes || 0,
          };
          
          this.rawComments.unshift(newComment); // 添加到列表开头
          this.newComment = '';
          this.$message.success(message);
          
          // 重新获取评论列表以确保数据完整
          await this.fetchComments();
        } else {
          // 提取错误信息
          let errorMsg = '评论发表失败';
          if (response && response.message) {
            errorMsg = response.message;
          } else if (response && response.msg) {
            errorMsg = response.msg;
          } else if (response && response.data && response.data.message) {
            errorMsg = response.data.message;
          } else if (response && response.data && response.data.msg) {
            errorMsg = response.data.msg;
          }
          this.$message.error(errorMsg);
        }
      } catch (error) {
        console.error('提交评论出错', error);
        
        // 提取错误信息
        let errorMsg = '评论发表失败: 网络错误';
        if (error.response) {
          console.error('错误响应:', error.response);
          if (error.response.data) {
            if (error.response.data.message) {
              errorMsg = `评论发表失败: ${error.response.data.message}`;
            } else if (error.response.data.msg) {
              errorMsg = `评论发表失败: ${error.response.data.msg}`;
            } else if (typeof error.response.data === 'string') {
              errorMsg = `评论发表失败: ${error.response.data}`;
            }
          } else {
            errorMsg = `评论发表失败: ${error.response.status} ${error.response.statusText}`;
          }
        } else if (error.message) {
          errorMsg = `评论发表失败: ${error.message}`;
        }
        
        this.$message.error(errorMsg);
      } finally {
        this.submitting = false;
      }
    },
    // 显示回复输入框
    showReplyInput(comment) {
      this.replyTarget = comment;
      this.replyContent = '';
    },
    // 取消回复
    cancelReply() {
      this.replyTarget = null;
      this.replyContent = '';
    },
    // 解析作者信息
    parseAuthorInfo(authorInfo) {
      if (!authorInfo) return '匿名用户';
      
      // 如果是字符串，尝试解析为JSON
      if (typeof authorInfo === 'string') {
        try {
          const parsed = JSON.parse(authorInfo);
          return parsed.nickname || parsed.username || parsed.name || '匿名用户';
        } catch (e) {
          // 如果不是JSON字符串，直接返回
          return authorInfo;
        }
      }
      
      // 如果是对象，直接提取用户名
      if (typeof authorInfo === 'object') {
        return authorInfo.nickname || authorInfo.username || authorInfo.name || '匿名用户';
      }
      
      return authorInfo;
    },

    // 提交回复
    async submitReply(topComment, replyTo = null) {
      if (!this.replyContent.trim()) return;
      this.replySubmitting = true;
      try {
        // 发送回复请求到正确的API
        const replyData = {
          circleId: this.post.circleId, // 从帖子信息中获取圈子ID
          authorId: this.userId, // 应该从用户登录信息中获取
          parentCommentId: replyTo ? replyTo.id : topComment.id, // 正确设置父评论ID
          content: this.replyContent
        };
        
        const response = await this.$axios.post('/api/circle/comments', replyData);
    
        
        // 响应没有特殊的data格式，直接处理响应对象
        if (response && response.id) {
          // 成功提交后添加到本地列表
          const newReply = {
            id: response.id,
            parentCommentId: replyTo ? replyTo.id : topComment.id,
            postId: this.postId, // 设置postId为主题帖ID
            nickname: '当前用户', // 实际应用中应从用户信息中获取
            avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            createTime: response.createdAt || new Date().toISOString(),
            content: response.content || this.replyContent,
            likeCount: response.likes || 0,
          };
          this.rawComments.push(newReply);
          this.replyContent = '';
          this.replyTarget = null;
          this.$message.success('回复成功');
          
          // 重新获取评论列表以确保顺序正确
          await this.fetchComments();
        } else {
          // 提取错误信息
          let errorMsg = '回复失败';
          if (response && response.message) {
            errorMsg = response.message;
          } else if (response && response.msg) {
            errorMsg = response.msg;
          }
          this.$message.error(errorMsg);
        }
      } catch (error) {
        console.error('提交回复出错', error);
        this.$message.error('回复失败: ' + (error.message || '网络错误'));
      } finally {
        this.replySubmitting = false;
      }
    },
  }
};
</script>

<style scoped>
.post-detail-container {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
  min-height: 120vh;
  position: relative;
}

/* 帖子卡片 */
.post-header {
  margin-bottom: 20px;
  border-radius: 8px;
}
.post-title {
  margin: 0 0 16px 0;
  font-size: 2.2rem;
  color: #303133;
  line-height: 1.3;
}
.post-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.author-name {
  font-size: 1.2rem;
  font-weight: 500;
  color: #409EFF;
}
.publish-date {
  color: #909399;
  font-size: 0.95rem;
}
.post-content {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #2c3e50;
}
.post-content pre {
  background-color: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}
.post-content code {
  background-color: #f6f8fa;
  padding: 2px 4px;
  border-radius: 4px;
  color: #d14;
}
.post-content ul {
  padding-left: 20px;
}

/* 评论区卡片 */
.comment-section {
  border-radius: 8px;
}
.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}
.comment-tools {
  display: flex;
  align-items: center;
  gap: 15px;
}
.comment-input-area {
  margin: 20px 0;
}
.comment-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

/* 评论列表 */
.comment-list {
  margin-top: 20px;
}
.comment-thread {
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;
  padding-bottom: 20px;
}
.comment-thread:last-child {
  border-bottom: none;
}
.comment-item {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
}
.reply-item {
  display: flex;
  gap: 12px;
  margin-left: 55px;
  margin-top: 15px;
}
.reply-item .el-avatar {
  flex-shrink: 0;
}
.comment-content,
.reply-content {
  flex: 1;
}
.comment-meta {
  margin-bottom: 5px;
}
.comment-author {
  font-weight: 500;
  color: #409EFF;
  margin-right: 10px;
}
.comment-time {
  color: #909399;
  font-size: 0.85rem;
}
.comment-text {
  color: #2c3e50;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.comment-actions {
  margin-top: 8px;
}
.comment-actions .el-button {
  padding: 0;
  margin-right: 15px;
}

/* 回复输入框 */
.reply-input-wrapper {
  margin-top: 15px;
  margin-left: 55px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 6px;
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

.no-comment {
  text-align: center;
  padding: 30px;
  color: #909399;
  font-size: 14px;
}

/* 回到顶部按钮 */
.backtop-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background-color: #409EFF;
  color: white;
  border-radius: 50%;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: background-color 0.3s;
}
.backtop-inner:hover {
  background-color: #66b1ff;
}
.backtop-inner i {
  font-size: 20px;
  margin-bottom: 2px;
}
</style>