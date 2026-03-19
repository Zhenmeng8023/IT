<template>
    <div class="login">
      <!-- 登录卡片 -->
      <el-card class="box-card">
          <div slot="header" class="clearfix">
              <el-button type="text" icon="el-icon-s-home">IT论坛登录</el-button>
          </div>
      <!-- 表单内容 -->
      <el-form ref="loginform" :model="user" label-width="80px" :rules="loginrules">
          <el-form-item label="用户名" prop ="name">
              <el-input v-model="user.name" placeholder="请输入用户名" prefix-icon="el-icon-user" clearable></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
              <el-input v-model="user.password" placeholder="请输入密码" prefix-icon="el-icon-lock" @keyup.enter="login" show-password clearable></el-input>
          </el-form-item>
          <el-form-item>
              <el-button type="primary" @click="login" :loading="loading">登录</el-button>
              <el-button type="success" @click="register" :loading="loading">注册</el-button>
              <!-- <el-button >重置</el-button> -->
          </el-form-item>
      </el-form>
      <!-- 表单内容结束 -->
      </el-card>
      <!-- 登录结束 -->
    </div>
  </template>
  
  <script>
  //引入 Pinia store
  import { useUserStore } from '@/store/user'

  export default {
      layout: 'login',                              //使用自定义布局
      data(){
          return{
              user:{
                  name:'',
                  password:''
              },
              loginrules:{
                  name:[
                      {required:true,message:'请输入用户名',trigger:'blur'},
                      {min:1,max:10,message:'用户名长度必须在1-10之间',trigger:'blur'}
                  ],
                  password:[
                      {required:true,message:'请输入密码',trigger:'blur'},
                      {min:8,max:20,message:'密码长度必须在8-20之间',trigger:'blur'}
                  ]
              },
              loading: false
          }
      },
      methods:{
          login() {
              this.$refs.loginform.validate((valid) => {
                  if (valid) {
                      // 显示加载状态
                      this.loading = true;
                      console.log('开始登录，用户名:', this.user.name);
                      
                      // 校验通过，使用 Pinia store 的登录方法
                      console.log('发送登录请求，参数:', {
                          username: this.user.name,
                          password: this.user.password
                      });
                      
                      // 使用 Pinia store 的 login 方法
                      const userStore = useUserStore()
                      userStore.login({
                          username: this.user.name,
                          password: this.user.password
                      })
                      .then(response => {
                          this.loading = false;
                          console.log('登录成功');
                          // 根据角色ID跳转到不同页面
                          const userStore = useUserStore();
                          const roleId = userStore.userInfo?.roleId;
                          console.log('用户角色ID:', roleId);
                          if (roleId === 4) {
                              // 角色ID为4，跳转到首页
                              this.$router.push('/');
                          } else {
                              // 其他角色ID，跳转到后台页面
                              this.$router.push('/menu');
                          }
                      })
                      .catch(error => {
                          this.loading = false;
                          // 网络错误或其他错误
                          console.error('登录请求失败:', error);
                          console.error('错误详情:', {
                              message: error.message,
                              response: error.response,
                              status: error.response?.status,
                              data: error.response?.data
                          });
                          
                          let errorMessage = '登录失败，请稍后重试';
                          if (error.message) {
                              errorMessage = error.message;
                          }
                          this.$message({
                              message: errorMessage,
                              type: 'error'
                          });
                      });
                  } else {
                      // 校验没通过
                      console.log('校验失败');
                      this.$message({
                          message: '请检查输入信息',
                          type: 'warning'
                      });
                      return false;
                  }
              });
          },
          register(){
                this.$router.push('/registe');
          },
      }
  
  }
  </script>
  <style scoped>
      .login {
          height: 100%;
          display: flex;
          justify-content: center;
          align-items: center;
      }
      .login-card {
          width: 500px;
      }
      .box-card{
          margin: 20px 20px;
      }
  </style>