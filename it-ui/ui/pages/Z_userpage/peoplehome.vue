<template>
    <div class="container">
      <header>
        <HeaderGreeting />
      </header>
      <div class="main-grid">
        <section class="left-panel">
          <div class="block"><el-avatar :size="250" :src="'/pic/choubi.jpg'"></el-avatar></div>
          <!-- 个人介绍开始 -->
          <!-- 带框的 -->
          <el-descriptions class="margin-top" title="个人介绍" :column="4" direction="vertical">
            <el-descriptions-item label="用户名">kooriookami</el-descriptions-item>
            <el-descriptions-item label="手机号">18100000000</el-descriptions-item>
            <el-descriptions-item label="居住地">苏州市</el-descriptions-item>
            <el-descriptions-item label="联系地址">江苏省苏州市吴中区吴中大道 1188 号</el-descriptions-item>
          </el-descriptions>
          <!-- 不带的 -->
          <h2>个人简介</h2><br>
          <p>昵称：  {{username}}</p><br>
          <p>生日：  {{formatDate(userbrithday)}}</p><br>
          <p>邮箱：  {{useremail}}</p><br>
          <p>联系地址：  {{useraddress}}</p><br>
          <p>性别：  {{usersex}}</p><br>
          <p>标签：  {{userbog.join('、')}}</p><br>
          <p>签名：  {{usersign}}</p><br>
          <!-- 个人介绍结束 -->
          <el-button type="text" @click="dialogFormVisible = true">修改个人信息</el-button>
          <el-dialog title="个人信息" :visible.sync="dialogFormVisible">
            <el-form ref="form" :model="user" label-width="80px">
              <el-form-item label="昵称">
                  <el-input v-model="username" placeholder="请输入昵称" prefix-icon="el-icon-user" clearable></el-input>
              </el-form-item>
              <el-form-item label="邮箱">
                  <el-input v-model="useremail" placeholder="请输入邮箱" prefix-icon="el-icon-email" clearable></el-input>
              </el-form-item>
              <el-form-item label="生日">
                  <el-date-picker type="date" placeholder="请选择生日" v-model="userbrithday" style="width: 100%;"></el-date-picker>
              </el-form-item>
              <el-form-item label="性别">
                  <el-radio-group v-model="usersex">
                      <el-radio v-model="usersex" label="男">男</el-radio>
                      <el-radio v-model="usersex" label="女">女</el-radio>
                  </el-radio-group>
              </el-form-item>
              <el-form-item label="签名">
                  <el-input v-model="usersign" placeholder="为大家留下签名吧" prefix-icon="el-icon-edit" clearable></el-input>
              </el-form-item>
              <el-form-item label="标签">
                <el-checkbox-group v-model="userbog">
                  <el-checkbox label="java" name="type"></el-checkbox>
                  <el-checkbox label="python" name="type"></el-checkbox>
                  <el-checkbox label="c++" name="type"></el-checkbox>
                  <el-checkbox label="前端" name="type"></el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="省市县">
                  <el-cascader v-model="usercity" :options="cityList" :props="{ expandTrigger: 'hover',value:'cityValue',label:'cityName',children:'children' }" @change="handleChange">

                  </el-cascader>
              </el-form-item>
              <el-form-item>
                  <el-button type="primary" @click="dialogFormVisible = false">提交</el-button>
                  <el-button type="text" @click="dialogFormVisible = false">取消</el-button>
              </el-form-item>
            </el-form>
          </el-dialog>
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
    layout: 'default',
    data() {
      return {
        imageUrl: '' ,// 添加 imageUrl 数据属性
        value1: 12322,
        value2: 1222,
        title: '',
        like: false,
        dialogFormVisible: false,
        username:'',          //昵称
        userbrithday: '',     //生日
        useremail: '',        //邮箱
        useraddress: '',      //联系地址
        usersex: '',          //性别
        userbog:[],           //标签
        usersign: '',         //签名
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
        this.$router.push('/collection');
      },
      formatDate(date) {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}年${month}月${day}日`;
      },
      onSubmit() {
        // 提交表单数据
        this.$refs.form.validate((valid) => {
          if (valid) {
            // 提交成功
            this.$message({
              message: '提交成功',
              type: 'success'
            });
            // 关闭弹窗
            this.dialogFormVisible = false;
          } else {
            // 提交失败
            this.$message({
              message: '请填写完整信息',
              type: 'error'
            });
          }
        });
      },
    }
  }
  </script>
  
<style scoped>
  /* 全局样式重置 */
  :global(html), :global(body) {
    margin: 0;
    padding: 0;
    min-height: 100vh;
    background-color: #000000;
  }

  .container {
    max-width: 100%;
    width: 100%;
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    color: #ffffff;
    background-color: #000000;
    min-height: 100vh;
    overflow-y: auto;
  }
  
  .main-grid {
    display: grid;
    grid-template-columns: 2fr 3fr 2fr;
    gap: 20px;
    margin: 20px;
    padding: 0;
  }
  
  @media (max-width: 768px) {
    .main-grid {
      grid-template-columns: 1fr;
      margin: 10px;
    }
  }
  
  footer {
    margin: 0;
    padding: 20px 0 0 0;
    border-top: 1px solid #444;
    background-color: #000000;
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
  
  /* 点赞和收藏统计样式 */
  .like-statistic {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-align: center;
  }
  .collect-statistic {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-align: center;
  }
  
  /* 热力图样式 */
  .block {
    margin-top: 20px;
    text-align: center;
  }
  .demonstration {
    display: block;
    text-align: center;
    margin-bottom: 20px;
  }
  .el-image {
    width: 100%;
    max-width: 500px;
    height: auto;
  }
  
  /* 响应式设计 */
  @media (max-width: 768px) {
    .container {
      padding: 0;
    }
    .main-grid {
      gap: 15px;
      margin: 10px;
    }
    .el-statistic {
      margin-bottom: 10px;
    }
  }
  
  /* 按钮样式 */
  .el-button--info {
    background-color: #909399;
    border-color: #909399;
  }
  .el-button--info:hover {
    background-color: #7c7e83;
    border-color: #7c7e83;
  }
  
  /* 描述列表样式 */
  .el-descriptions {
    margin-top: 20px;
  }
</style>