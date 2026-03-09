<template>
    <div class="container">
      <header>
        <HeaderGreeting />
      </header>
      <div class="main-grid">
        <section class="left-panel">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload">
            <img v-if="imageUrl" :src="imageUrl" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
          <!-- 个人介绍开始 -->
          <el-descriptions class="margin-top" title="个人介绍" :column="4" direction="vertical">
            <el-descriptions-item label="用户名">kooriookami</el-descriptions-item>
            <el-descriptions-item label="手机号">18100000000</el-descriptions-item>
            <el-descriptions-item label="居住地">苏州市</el-descriptions-item>
            <el-descriptions-item label="联系地址">江苏省苏州市吴中区吴中大道 1188 号</el-descriptions-item>
          </el-descriptions>
          <!-- 个人介绍结束 -->
        </section>

        <section class="middle-space">
          <!-- 点赞量开始 -->
          <div>
            <el-row :gutter="20">
              <el-col :span="6">
                <div>
                  <el-statistic :value="value1" title="点赞量" class="like-statistic">
                  </el-statistic>
                </div>
              </el-col>
              <el-col :span="6">
                <div>
                  <el-statistic :value="value2" title="收藏量" class="collect-statistic">
                  </el-statistic>
                </div>
              </el-col>
              <el-button type="info" icon="el-icon-time" @click="handleHistoryClick">历史记录</el-button>
              <el-button type="info" icon="el-icon-star-off" @click="handleCollectClick">我的收藏</el-button>
            </el-row>
          </div>
          <br><br><br><br>
          <!-- 点赞量结束 -->
          <!-- <HeatmapTracker /> -->
          <div class="block">
            <span class="demonstration">热力图</span>
            <el-image src="/pic/choubi.jpg"></el-image>
          </div>
          <!-- <span>-----------------------热力图-------------------------------------------------</span> -->
        </section>

        <section class="right-pane1">
          <ContentSection />
        </section>
      </div>
      
      <footer>
        <FooterPlayer />
      </footer>
    </div>
  </template>
  
  <script>
  import HeaderGreeting from '../Z_userpage/components/HeaderGreeting.vue'
  import Calendar from '../Z_userpage/components/Calendar.vue'
  // import RecommendLinks from '../userpage/components/RecommendLinks.vue'
  import ContentSection from '../Z_userpage/components/ContentSection.vue'
  import FooterPlayer from '../Z_userpage/components/FooterPlayer.vue'
  // import HeatmapTracker from './components/HeatmapTracker.vue'
  
  export default {
    name: 'App',
    data() {
      return {
        imageUrl: '' ,// 添加 imageUrl 数据属性
        value1: 12322,
        value2: 1222,
        title: '点赞量',
        like: false
      }
    },
    components: {
      HeaderGreeting,
      Calendar,
      // RecommendLinks,
      ContentSection,
      FooterPlayer
    },
    methods: {
      handleAvatarSuccess(res, file) {
        if (res && res.url) {
          this.imageUrl = res.url;
        } else {
          // 备用方案：使用本地临时URL
          this.imageUrl = URL.createObjectURL(file.raw);
        }
      },
      beforeAvatarUpload(file) {
        const isJPG = file.type === 'image/jpeg';
        const isLt2M = file.size / 1024 / 1024 < 2;

        if (!isJPG) {
          this.$message.error('上传头像图片只能是 JPG 格式!');
        }
        if (!isLt2M) {
          this.$message.error('上传头像图片大小不能超过 2MB!');
        }
        return isJPG && isLt2M;
      },
      handleHistoryClick() {
        this.$router.push('/history');
      },
      handleCollectClick() {
        
      }
    }
  }
  </script>
  
<style scoped>

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    color: #333;
    color: #fff;
    background-color: #000000;
    min-height: 100vh;
  }
  .main-grid {
    display: grid;
    grid-template-columns: 2fr 3fr 2fr;
    /* gap: 40px;
    margin-top: 30px; */
  }
  @media (max-width: 768px) {
    .main-grid {
      grid-template-columns: 1fr;
    }
  }
  footer {
    margin-top: 50px;
    padding-top: 20px;
    border-top: 1px solid #eee;
    border-top: 1px solid #444;
  }
  /* 头像上传器样式 */
  .avatar-uploader .el-upload {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
  }
  .avatar-uploader .el-upload:hover {
    border-color: #409EFF;
  }
  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 178px;
    height: 178px;
    line-height: 178px;
    text-align: center;
  }
  .avatar {
    width: 178px;
    height: 178px;
    display: block;
  }
  /* 头像上传器样式结束 */
  /* 个人信息样式 */
  /* 个人介绍卡片整体背景 */
  /* 穿透 scoped 限制，覆盖 Element UI 内部元素 */
  ::v-deep .el-descriptions {
    background-color: #000000 !important;
    border: 1px solid #333 !important;
    border-radius: 8px;
    padding: 20px;
  }

  ::v-deep .el-descriptions__title {
    color: #ffffff !important;
  }

  ::v-deep .el-descriptions__body {
    background-color: #000000 !important;
  }

  ::v-deep .el-descriptions-item__label {
    background-color: #000000 !important;
    color: #b0b0b0 !important; /* 标签文字用浅灰，也可用白色 */
  }

  ::v-deep .el-descriptions-item__content {
    background-color: #000000 !important;
    color: #ffffff !important;
  }

  ::v-deep .el-descriptions-item__cell {
    background-color: #000000 !important;
    border-color: #444 !important;
  }

  /* 如果希望鼠标悬停时稍微亮一点 */
  ::v-deep .el-descriptions-item:hover .el-descriptions-item__cell {
    background-color: #1a1a1a !important;
  }
  /* 点赞量样式 */
  .like-statistic ::v-deep .el-statistic__number {
    font-size: 24px;
    font-weight: bold;
    color: #ff0000; /* 点赞量数值颜色 */
  }
  
  .like-statistic ::v-deep .el-statistic__title {
    font-size: 14px;
    color: #ffffff; /* 白色标题 */
    margin-bottom: 8px;
  }

  /* 收藏量样式 */
  .collect-statistic ::v-deep .el-statistic__number {
    font-size: 24px;
    font-weight: bold;
    color: #ff0000; /* 收藏量数值颜色 */
  }
  
  .collect-statistic ::v-deep .el-statistic__title {
    font-size: 14px;
    color: #ffffff; /* 白色标题 */
    margin-bottom: 8px;
  }
</style>