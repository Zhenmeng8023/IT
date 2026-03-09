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
          login(){
              this.$refs.loginform.validate((valid)=>{               //校验表单
                  if(valid){
                      // 显示加载状态
                      this.loading = true;
                      
                      //校验通过，调用后端登录API
                      this.$axios.post('/login', {
                          username: this.user.name,
                          password: this.user.password
                      }, {
                          headers: {
                              'Content-Type': 'application/json'
                          }
                      })
                      .then(response => {
                          this.loading = false;
                          console.log('登录响应:', response);
                          
                          // 登录成功
                          if (response.success) {
                              console.log('登录成功');
                              // 存储token或用户信息
                              if (response.other && response.other.token) {
                                  localStorage.setItem('token', response.other.token);
                              }
                              // 跳转到网页主页
                              this.$router.push('/');
                          } else {
                              // 登录失败
                              console.log('登录失败:', response.message);
                              alert('登录失败：' + (response.message || '用户名或密码错误'));
                          }
                      })
                      .catch(error => {
                          this.loading = false;
                          // 网络错误或其他错误
                          console.error('登录请求失败:', error);
                          console.error('错误详情:', error.response || error.message);
                          
                          if (error.response) {
                              // 服务器返回错误
                              alert('登录失败：' + (error.response.data.message || '服务器错误'));
                          } else if (error.request) {
                              // 请求发送成功但没有收到响应
                              alert('登录失败：网络连接失败，请检查后端服务是否运行');
                          } else {
                              // 其他错误
                              alert('登录失败：' + error.message);
                          }
                      });
                  }else{
                      //校验没通过
                      console.log('校验失败');
                      alert('登录失败：请检查输入信息');
                      return false;
                  }
              })
          },
          register(){
              this.$refs.loginform.validate((valid)=>{               //校验表单
                  if(valid){
                      // 显示加载状态
                      this.loading = true;
                      
                      //校验通过，调用后端注册API
                      this.$axios.post('/register', {
                          username: this.user.name,
                          password: this.user.password
                      }, {
                          headers: {
                              'Content-Type': 'application/json'
                          }
                      })
                      .then(response => {
                          this.loading = false;
                          console.log('注册响应:', response);
                          
                          // 注册成功
                          if (response.success) {
                              // 提示注册成功
                              alert('注册成功，请登录');
                          } else {
                              // 注册失败
                              alert('注册失败：' + (response.message || '注册失败'));
                          }
                      })
                      .catch(error => {
                          this.loading = false;
                          // 网络错误或其他错误
                          console.error('注册请求失败:', error);
                          console.error('错误详情:', error.response || error.message);
                          
                          if (error.response) {
                              // 服务器返回错误
                              alert('注册失败：' + (error.response.data.message || '服务器错误'));
                          } else if (error.request) {
                              // 请求发送成功但没有收到响应
                              alert('注册失败：网络连接失败，请检查后端服务是否运行');
                          } else {
                              // 其他错误
                              alert('注册失败：' + error.message);
                          }
                      });
                  }else{
                      //校验没通过
                      console.log('校验失败');
                      alert('注册失败：请检查输入信息');
                      return false;
                  }
              })
          },
      }
  
  }
  </script>
  <style>
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