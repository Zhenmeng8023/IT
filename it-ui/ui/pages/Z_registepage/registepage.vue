<template>
  <div class="login">
      <!-- 注册卡片 -->
      <el-card class="box-card">
          <div slot="header" class="clearfix">
              <el-button type="text" icon="el-icon-s-home">IT论坛登录</el-button>
          </div>
      <!-- 表单内容 -->
      <el-form ref="loginform" :model="user" label-width="150px" :rules="loginrules">
          <el-form-item label="用户名" prop ="name">
              <el-input v-model="user.name" placeholder="请输入用户名" prefix-icon="el-icon-user" clearable></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
              <el-input v-model="user.password" placeholder="请输入密码" prefix-icon="el-icon-lock" show-password clearable></el-input>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
              <el-col :span="15">
                  <el-input v-model="user.email" placeholder="请输入邮箱" prefix-icon="el-icon-email" clearable></el-input>
              </el-col>
              <el-col :span="5" :offset="2">
                  <el-button type="primary" @click="getcode">获取验证码</el-button>
              </el-col>
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input v-model="user.code" placeholder="请输入验证码" prefix-icon="el-icon-email" clearable></el-input>
          </el-form-item>
          <el-form-item>
              <!-- <el-button type="primary" @click="login" :loading="loading">登录</el-button> -->
              <el-button type="success" :loading="loading" @click="register">注册</el-button>
              <!-- <el-button >重置</el-button> -->
          </el-form-item>
      </el-form>
      <!-- 表单内容结束 -->
      </el-card>
      <!-- 注册结束 -->
    </div>
</template>

<script>
export default {
    layout: 'login',
    data() {
        return {
            user: {
                name: '',
                password: '',
                email:'',
                code:''
            },
            loginrules: {
                name: [
                    { required: true, message: '请输入用户名', trigger: 'blur' },
                    {min:1,max:10,message:'用户名长度必须在1-10之间',trigger:'blur'}
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    {min:8,max:20,message:'密码长度必须在8-20之间',trigger:'blur'}
                ],
                email: [
                    { required: true, message: '请输入邮箱', trigger: 'blur' },
                    {type:'email',message:'请输入正确的邮箱格式',trigger:'blur'}
                ],
                code: [
                    { required: true, message: '请输入验证码', trigger: 'blur' },
                    {min:6,max:6,message:'验证码长度必须为6位',trigger:'blur'}
                ]
            },
            loading: false
        }
    },
    methods: {
        //验证码获取
        getcode() {
            // this.$message({
            //     message: '验证码已发送',
            //     type: 'success'
            // })
            this.$axios.post('/send-verify-code',{
                email:this.user.email
            })
        },
        //注册
        register() {
            // this.$refs.loginform.validate((valid) => {
            //     if (valid) {
            //         this.$message({
            //             message: '注册成功',
            //             type: 'success'
            //         })
            //     } else {
            //         this.$message({
            //             message: '注册失败',
            //             type: 'error'
            //         })
            //     }
            // })
            this.$axios.post('/register',{
                name:this.user.name,
                password:this.user.password,
                email:this.user.email,
                code:this.user.code
            })
        }
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