
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
                  <el-button type="primary" @click="getcode" :disabled="gettingCode || countdown > 0">
                      {{ gettingCode ? '发送中...' : (countdown > 0 ? `${countdown}秒后重新获取` : '获取验证码') }}
                  </el-button>
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
import { 
    SendVerifyCode, 
    Register 
    } from '@/api/index.js'

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
                    {min:3,max:10,message:'用户名长度必须在3-10之间',trigger:'blur'}
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
            loading: false,
            gettingCode: false,
            countdown: 0,
            timer: null
        }
    },
    methods: {
        //验证码获取
        getcode() {
            // 验证邮箱格式
            if (!this.user.email || !this.user.email.includes('@')) {
                this.$message({
                    message: '请输入正确的邮箱格式',
                    type: 'warning'
                });
                return;
            }
            
            // 防止重复点击
            this.gettingCode = true;
            console.log('开始发送验证码，邮箱:', this.user.email);
            
            SendVerifyCode({
                email: this.user.email
            })
            .then(response => {
                console.log('验证码发送响应:', response);
                console.log('响应状态:', response.status);
                console.log('响应数据:', response.data);
                
                // 检查响应格式
                if (response.data) {
                    if (response.data.success || response.data.code == 200) {
                        this.$message({
                            message: response.data.message || '验证码已发送，请查收',
                            type: 'success'
                        });
                        // 60秒后才能再次获取验证码
                        this.countdown = 60;
                        this.timer = setInterval(() => {
                            this.countdown--;
                            if (this.countdown <= 0) {
                                clearInterval(this.timer);
                                this.gettingCode = false;
                            }
                        }, 1000);
                    } else {
                        this.$message({
                            message: response.data.message || '验证码发送失败',
                            type: 'error'
                        });
                        this.gettingCode = false;
                    }
                } else {
                    // 处理没有data字段的情况
                    console.log('响应中没有data字段');
                    this.$message({
                        message: '验证码发送成功',
                        type: 'success'
                    });
                    // 60秒后才能再次获取验证码
                    this.countdown = 60;
                    this.timer = setInterval(() => {
                        this.countdown--;
                        if (this.countdown <= 0) {
                            clearInterval(this.timer);
                            this.gettingCode = false;
                        }
                    }, 1000);
                }
            })
            .catch(error => {
                console.error('验证码发送错误:', error);
                console.error('错误详情:', {
                    message: error.message,
                    response: error.response,
                    status: error.response?.status,
                    data: error.response?.data
                });
                let errorMessage = '验证码发送失败，请稍后重试';
                if (error.response && error.response.data && error.response.data.message) {
                    errorMessage = error.response.data.message;
                } else if (error.message) {
                    errorMessage = error.message;
                }
                this.$message({
                    message: errorMessage,
                    type: 'error'
                });
                this.gettingCode = false;
            })
        },

        beforeDestroy() {
            // 组件销毁时清除定时器
            if (this.timer) {
                clearInterval(this.timer);
            }
        },
        //注册
        register() {
            // 表单验证
            this.$refs.loginform.validate((valid) => {
                if (valid) {
                    this.loading = true;
                    Register({
                        name: this.user.name,
                        password: this.user.password,
                        email: this.user.email,
                        code: this.user.code
                    }, {
                        headers: {
                            'Content-Type': 'application/json'
                        }
                    })
                    .then(response => {
                        console.log('注册响应:', response);
                        console.log('响应状态:', response.status);
                        console.log('响应数据:', response.data);
                        this.loading = false;
                        
                        // 检查响应格式
                        if (response.data) {
                            if (response.data.success || response.data.code == 200) {
                                this.$message({
                                    message: '注册成功，即将跳转到登录页面',
                                    type: 'success'
                                });
                                // 3秒后跳转到登录页面
                                setTimeout(() => {
                                    this.$router.push('/login');
                                }, 3000);
                            } else {
                                this.$message({
                                    message: response.data.message || '注册失败',
                                    type: 'error'
                                });
                            }
                        } else {
                            // 处理没有data字段的情况
                            console.log('响应中没有data字段');
                            this.$message({
                                message: '注册成功，即将跳转到登录页面',
                                type: 'success'
                            });
                            // 3秒后跳转到登录页面
                            setTimeout(() => {
                                this.$router.push('/login');
                            }, 3000);
                        }
                    })
                    .catch(error => {
                        console.error('注册错误:', error);
                        console.error('错误详情:', {
                            message: error.message,
                            response: error.response,
                            status: error.response?.status,
                            data: error.response?.data
                        });
                        this.loading = false;
                        let errorMessage = '注册失败，请稍后重试';
                        if (error.response && error.response.data && error.response.data.message) {
                            errorMessage = error.response.data.message;
                        } else if (error.message) {
                            errorMessage = error.message;
                        }
                        this.$message({
                            message: errorMessage,
                            type: 'error'
                        });
                    });
                } else {
                    this.$message({
                        message: '请检查表单填写是否正确',
                        type: 'warning'
                    });
                }
            });
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