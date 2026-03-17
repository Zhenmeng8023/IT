<template>
  <div class="post-detail-container">
    <!-- 帖子内容卡片 -->
    <el-card class="post-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="loading">
      <h1 class="post-title">{{ post.title }}</h1>
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
          <!-- 排序切换 -->
          <el-radio-group v-model="sortOrder" size="small" @change="handleSortChange">
            <el-radio-button label="desc">最新</el-radio-button>
            <el-radio-button label="asc">最早</el-radio-button>
          </el-radio-group>
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

          <!-- 该顶级评论下的回复列表（二级评论） -->
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
      loading: false,
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
      // 原始评论列表（扁平）- 添加丰富的实验数据
      rawComments: [
        // 顶级评论1 - 用户B
        {
          id: 101,
          parentId: null,
          nickname: '代码猎人',
          avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
          createTime: '2024-03-15T11:20:00Z',
          content: '写得真好！组合式 API 确实让逻辑复用更方便了。我最近在项目中也在尝试使用，感觉代码组织清晰了很多。',
          likeCount: 12,
        },
        // 顶级评论2 - 用户C
        {
          id: 102,
          parentId: null,
          nickname: '前端小迷妹',
          avatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
          createTime: '2024-03-15T12:05:00Z',
          content: '有个问题想请教：在 setup 中如何使用 provide/inject？我试了几次都不太成功。',
          likeCount: 5,
        },
        // 顶级评论3 - 用户D
        {
          id: 103,
          parentId: null,
          nickname: 'Vue爱好者',
          avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
          createTime: '2024-03-15T13:30:00Z',
          content: '补充一点：使用 reactive 时要注意解构会丢失响应式，可以用 toRefs 来解决。',
          likeCount: 8,
        },
        // 顶级评论4 - 用户E
        {
          id: 104,
          parentId: null,
          nickname: 'TypeScript高手',
          avatar: 'https://cube.elemecdn.com/2/8e/4a7b8c9d0e1f2a3b4c5d6e7f8g9h0i.png',
          createTime: '2024-03-15T14:15:00Z',
          content: '配合 TypeScript 使用体验极佳，类型推导非常强大。',
          likeCount: 15,
        },
        
        // 回复 - 针对评论101（代码猎人）
        {
          id: 201,
          parentId: 101,
          nickname: '前端小王子', // 楼主回复
          avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          createTime: '2024-03-15T11:35:00Z',
          content: '感谢支持！建议你多看看官方文档，里面有很多实用的例子。',
          likeCount: 3,
        },
        {
          id: 202,
          parentId: 101,
          nickname: '代码猎人',
          avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
          createTime: '2024-03-15T11:50:00Z',
          content: '@前端小王子 好的，谢谢建议！',
          likeCount: 1,
        },
        {
          id: 203,
          parentId: 101,
          nickname: '路人甲',
          avatar: 'https://cube.elemecdn.com/4/5d/6e7f8g9h0i1j2k3l4m5n6o7p8q9r0s.png',
          createTime: '2024-03-15T15:20:00Z',
          content: '同感！组合式 API 让代码复用变得特别简单。',
          likeCount: 2,
        },
        
        // 回复 - 针对评论102（前端小迷妹）
        {
          id: 204,
          parentId: 102,
          nickname: 'Vue大神',
          avatar: 'https://cube.elemecdn.com/5/6f/7g8h9i0j1k2l3m4n5o6p7q8r9s0t.png',
          createTime: '2024-03-15T12:30:00Z',
          content: 'p)',
          likeCount: 7,
        },
        {
          id: 205,
          parentId: 102,
          nickname: '前端小迷妹',
          avatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
          createTime: '2024-03-15T12:45:00Z',
          content: '@Vue大神 感谢！原来这么简单，我试试。',
          likeCount: 0,
        },
        {
          id: 206,
          parentId: 102,
          nickname: '前端小王子', // 楼主回复
          avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          createTime: '2024-03-15T13:00:00Z',
          content: '补充一下，如果需要响应式，可以用 ref 包裹。',
          likeCount: 4,
        },
        
        // 回复 - 针对评论103（Vue爱好者）
        {
          id: 207,
          parentId: 103,
          nickname: '前端小王子', // 楼主回复
          avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          createTime: '2024-03-15T13:50:00Z',
          content: '没错，这点很重要！很多人容易踩坑。',
          likeCount: 3,
        },
        {
          id: 208,
          parentId: 103,
          nickname: 'Vue初学者',
          avatar: 'https://cube.elemecdn.com/6/7g/8h9i0j1k2l3m4n5o6p7q8r9s0t1u.png',
          createTime: '2024-03-15T16:30:00Z',
          content: '原来如此，我之前就遇到这个问题，谢谢提醒！',
          likeCount: 2,
        },
        
        // 回复 - 针对评论104（TypeScript高手）
        {
          id: 209,
          parentId: 104,
          nickname: '前端小王子', // 楼主回复
          avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          createTime: '2024-03-15T14:30:00Z',
          content: '是的，特别是 defineComponent 的泛型支持，让类型推导更强大。',
          likeCount: 5,
        },
        {
          id: 210,
          parentId: 104,
          nickname: 'TypeScript高手',
          avatar: 'https://cube.elemecdn.com/2/8e/4a7b8c9d0e1f2a3b4c5d6e7f8g9h0i.png',
          createTime: '2024-03-15T14:50:00Z',
          content: '@前端小王子 对，现在写 Vue 3 + TS 真的很舒服。',
          likeCount: 2,
        },
        
        // 更深层嵌套 - 针对回复的回复
        {
          id: 301,
          parentId: 206, // 针对前端小王子的回复（id=206）
          nickname: '前端小迷妹',
          avatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
          createTime: '2024-03-15T13:15:00Z',
          content: '@前端小王子 明白了，用 ref 包裹后就能保持响应式了，谢谢！',
          likeCount: 1,
        },
        {
          id: 302,
          parentId: 207, // 针对前端小王子的回复（id=207）
          nickname: 'Vue爱好者',
          avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
          createTime: '2024-03-15T14:00:00Z',
          content: '@前端小王子 大佬能不能写个完整的例子？',
          likeCount: 2,
        },
        {
          id: 303,
          parentId: 302, // 针对Vue爱好者的回复（id=302）
          nickname: '前端小王子', // 楼主回复
          avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          createTime: '2024-03-15T14:20:00Z',
          content: '@Vue爱好者 好的，我晚点整理一下发个 demo。',
          likeCount: 3,
        },
        //帖子详情数据实验结束
      ],
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
    // 构建顶级评论及其 children
    topLevelComments() {
      const topMap = new Map();
      const topList = [];

      // 先找出所有顶级评论（parentId 为 null 或 0）
      this.sortedComments.forEach(comment => {
        if (!comment.parentId) {
          // 创建副本避免修改原始数据
          const topComment = { ...comment, children: [] };
          topList.push(topComment);
          topMap.set(comment.id, topComment);
        }
      });

      // 将非顶级评论（回复）分配到对应的顶级评论下
      this.sortedComments.forEach(comment => {
        if (comment.parentId) {
          const parentComment = topMap.get(comment.parentId);
          if (parentComment && parentComment.children) {
            parentComment.children.push({ ...comment });
          }
        }
      });

      // 对每个顶级评论下的 children 也按时间排序
      topList.forEach(top => {
        if (top.children && top.children.length) {
          top.children.sort((a, b) => {
            const timeA = new Date(a.createTime).getTime();
            const timeB = new Date(b.createTime).getTime();
            return this.sortOrder === 'desc' ? timeB - timeA : timeA - timeB;
          });
        }
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
  methods: {
    // 获取帖子详情
    async fetchPostDetail() {
      this.loading = true;
      try {
        // 尝试使用不同的API路径
        const response = await this.$axios.get(`/api/circle/comments/${this.postId}`);
        console.log('帖子详情API响应:', response.data);
        
        // 检查响应数据
        let postData = null;
        if (response) {
          // 检查response是否直接是数据对象（没有data字段）
          if (typeof response === 'object' && !response.data) {
            postData = response;
          } else if (response.data) {
            // 标准axios响应格式
            if (response.data.code === 0 && response.data.data) {
              postData = response.data.data;
            } else if (typeof response.data === 'object') {
              postData = response.data;
            }
          } else if (Array.isArray(response) && response.length > 0) {
            postData = response[0];
          }
        }
        
        if (postData) {
          // 转换数据格式以匹配前端期望
          this.post = {
            title: postData.title || postData.subject || '无标题',
            author: this.parseAuthorInfo(postData.author || postData.user || postData.creator),
            avatar: postData.avatar || postData.userAvatar || postData.user?.avatar || postData.author?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            publishDate: this.formatTime(postData.createTime || postData.createdAt || postData.publishDate),
            content: postData.content || postData.body || ''
          };
        } else {
          this.$message.error('获取帖子失败：数据格式错误');
        }
      } catch (error) {
        console.error('获取帖子详情出错', error);
        this.$message.error('网络错误');
      } finally {
        this.loading = false;
      }
    },
    // 获取评论列表
    async fetchComments() {
      this.commentLoading = true;
      try {
        // 尝试使用不同的API路径
        const response = await this.$axios.get(`/api/circle/posts/${this.postId}/replies`);
        console.log('评论API响应:', response.data);
        
        // 检查响应数据
        let commentsData = [];
        if (response) {
          // 检查response是否直接是数据数组（没有data字段）
          if (Array.isArray(response)) {
            commentsData = response;
          } else if (response.data) {
            // 标准axios响应格式
            if (response.data.code === 0 && Array.isArray(response.data.data)) {
              commentsData = response.data.data;
            } else if (Array.isArray(response.data)) {
              commentsData = response.data;
            } else if (response.data.data && Array.isArray(response.data.data)) {
              commentsData = response.data.data;
            } else if (response.data.list && Array.isArray(response.data.list)) {
              commentsData = response.data.list;
            }
          }
        }
        
        // 转换评论数据格式
        this.rawComments = commentsData.map(comment => {
          return {
            id: comment.id || comment.commentId,
            parentId: comment.parentId || comment.parent_id || null,
            nickname: this.parseAuthorInfo(comment.author || comment.user || comment.creator),
            avatar: comment.avatar || comment.userAvatar || comment.user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            createTime: comment.createTime || comment.createdAt || comment.create_date,
            content: comment.content || comment.body || '',
            likeCount: comment.likeCount || comment.likes || 0
          };
        });
      } catch (error) {
        console.error('获取评论列表出错', error);
        this.$message.error('网络错误');
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
        // 发送评论请求到正确的API
        const response = await this.$axios.post('/api/circle/comments', {
          circleId: 1, // 假设圈子ID为1
          authorId: 1, // 假设作者ID为1
          postId: this.postId, // 被评论的圈子动态ID
          parentCommentId: null, // 顶级评论，没有父评论ID
          content: this.newComment
        });
        
        if (response.data && response.data.code === 0) {
          // 成功提交后添加到本地列表
          const newComment = {
            id: response.data.data?.id || Date.now(),
            parentId: null,
            nickname: '当前用户', // 实际应用中应从用户信息中获取
            avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            createTime: new Date().toISOString(),
            content: this.newComment,
            likeCount: 0,
          };
          this.rawComments.push(newComment);
          this.newComment = '';
          this.$message.success('评论发表成功');
          
          // 重新获取评论列表以确保顺序正确
          await this.fetchComments();
        } else {
          this.$message.error(response.data.msg || '评论发表失败');
        }
      } catch (error) {
        console.error('提交评论出错', error);
        this.$message.error('评论发表失败: ' + (error.message || '网络错误'));
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
          circleId: 1, // 假设圈子ID为1
          authorId: 1, // 假设作者ID为1
          postId: this.postId, // 与父评论的postId相同
          parentCommentId: replyTo ? replyTo.id : topComment.id, // 如果是回复某人的回复，则使用被回复的ID；否则使用顶级评论ID
          content: this.replyContent
        };
        
        const response = await this.$axios.post('/api/circle/comments', replyData);
        
        if (response.data && response.data.code === 0) {
          // 成功提交后添加到本地列表
          const newReply = {
            id: response.data.data?.id || Date.now(),
            parentId: replyTo ? replyTo.id : topComment.id,
            nickname: '当前用户', // 实际应用中应从用户信息中获取
            avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            createTime: new Date().toISOString(),
            content: this.replyContent,
            likeCount: 0,
          };
          this.rawComments.push(newReply);
          this.replyContent = '';
          this.replyTarget = null;
          this.$message.success('回复成功');
          
          // 重新获取评论列表以确保顺序正确
          await this.fetchComments();
        } else {
          this.$message.error(response.data.msg || '回复失败');
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