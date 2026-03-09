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
              <el-button type="primary" @click="login">登录</el-button>
              <el-button type="primary" @click="login">注册</el-button>
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
              }
          }
      },
      methods:{
          login(){
              this.$refs.loginform.validate((valid)=>{               //校验表单
                  if(valid){
                      //校验通过
                      //alert('登录成功');
                      this.$router.push('/'); // 跳转到网页主页
                  }else{
                      //校验没通过
                      console.log('校验失败');
                      alert('登录失败');
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