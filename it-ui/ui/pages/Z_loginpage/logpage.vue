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
          </el-form-item>
          <el-form-item>
              <el-button type="text" @click="showForgotPasswordDialog">忘记密码</el-button>
          </el-form-item>
      </el-form>
      </el-card>
      
      <!-- 忘记密码弹窗 -->
      <el-dialog
          title="忘记密码"
          :visible.sync="forgotPasswordDialogVisible"
          width="400px"
          @close="closeForgotPasswordDialog"
      >
          <el-form ref="forgotPasswordForm" :model="forgotPasswordForm" :rules="forgotPasswordRules" label-width="80px">
              <el-form-item label="邮箱" prop="email">
                  <el-input v-model="forgotPasswordForm.email" placeholder="请输入注册邮箱" prefix-icon="el-icon-message" clearable></el-input>
              </el-form-item>
              <el-form-item label="验证码" prop="code">
                  <el-row :gutter="10">
                      <el-col :span="16">
                          <el-input v-model="forgotPasswordForm.code" placeholder="请输入6位验证码" prefix-icon="el-icon-mobile-phone" clearable maxlength="6"></el-input>
                      </el-col>
                      <el-col :span="8">
                          <el-button 
                              type="primary" 
                              @click="sendVerificationCode" 
                              :disabled="gettingCode || countdown > 0"
                          >
                              {{ gettingCode ? '发送中...' : (countdown > 0 ? `${countdown}秒后重新发送` : '获取验证码') }}
                          </el-button>
                      </el-col>
                  </el-row>
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="forgotPasswordForm.newPassword" placeholder="请输入新密码（8-20位）" prefix-icon="el-icon-lock" show-password clearable></el-input>
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="forgotPasswordForm.confirmPassword" placeholder="请确认新密码" prefix-icon="el-icon-lock" show-password clearable></el-input>
              </el-form-item>
          </el-form>
          <div slot="footer" class="dialog-footer">
              <el-button @click="closeForgotPasswordDialog">取消</el-button>
              <el-button type="primary" @click="submitForgotPassword" :loading="loading">重置密码</el-button>
          </div>
      </el-dialog>
    </div>
</template>

<script>
//引入 Pinia store
import { useUserStore } from '@/store/user'
// 引入API
import { SendPasswordResetVerifyCode, ResetPassword } from '@/api/index.js'

export default {
    layout: 'login',
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
            loading: false,
            // 忘记密码相关
            forgotPasswordDialogVisible: false,
            forgotPasswordForm: {
                email: '',
                code: '',
                newPassword: '',
                confirmPassword: ''
            },
            forgotPasswordRules: {
                email: [
                    {required: true, message: '请输入邮箱', trigger: 'blur'},
                    {type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur'}
                ],
                code: [
                    {required: true, message: '请输入验证码', trigger: 'blur'},
                    {len: 6, message: '验证码长度必须为6位', trigger: 'blur'}
                ],
                newPassword: [
                    {required: true, message: '请输入新密码', trigger: 'blur'},
                    {min: 8, max: 20, message: '密码长度必须在8-20之间', trigger: 'blur'}
                ],
                confirmPassword: [
                    {required: true, message: '请确认密码', trigger: 'blur'},
                    {
                        validator: (rule, value, callback) => {
                            if (value !== this.forgotPasswordForm.newPassword) {
                                callback(new Error('两次输入密码不一致'));
                            } else {
                                callback();
                            }
                        },
                        trigger: 'blur'
                    }
                ]
            },
            gettingCode: false,
            countdown: 0,
            timer: null
        }
    },
    methods:{
        login() {
            this.$refs.loginform.validate((valid) => {
                if (valid) {
                    this.loading = true;
                    console.log('开始登录，用户名:', this.user.name);
                    
                    const userStore = useUserStore()
                    
                    userStore.login({
                        username: this.user.name,
                        password: this.user.password
                    })
                    .then(response => {
                        this.loading = false;
                        console.log('登录成功');
                        
                        const userStore = useUserStore();
                        console.log('Store中的token:', userStore.token);
                        
                        this.$nextTick(() => {
                            const roleId = userStore.userInfo?.roleId;
                            console.log('用户角色ID:', roleId);
                            
                            if (roleId === 4) {
                                this.$router.push('/');
                            } else {
                                this.$router.push('/homepage');
                            }
                        });
                    })
                    .catch(error => {
                        this.loading = false;
                        console.error('登录请求失败:', error);
                        
                        let errorMessage = '登录失败，请稍后重试';
                        if (error.response?.data?.message) {
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
        
        // 忘记密码相关方法
        showForgotPasswordDialog() {
            this.forgotPasswordDialogVisible = true;
        },
        
        closeForgotPasswordDialog() {
            this.forgotPasswordDialogVisible = false;
            this.resetForgotPasswordForm();
        },
        
        resetForgotPasswordForm() {
            this.forgotPasswordForm = {
                email: '',
                code: '',
                newPassword: '',
                confirmPassword: ''
            };
            this.countdown = 0;
            if (this.timer) {
                clearInterval(this.timer);
                this.gettingCode = false;
            }
        },
        
        /**
         * 发送验证码
         */
        async sendVerificationCode() {
            // 验证邮箱格式
            let isValid = true;
            
            await this.$refs.forgotPasswordForm.validateField('email', (error) => {
                if (error) isValid = false;
            });
            
            if (!isValid) return;
            
            this.gettingCode = true;
            console.log('发送验证码，邮箱:', this.forgotPasswordForm.email);
            
            try {
                // 调用发送验证码API
                const response = await SendPasswordResetVerifyCode({
                    email: this.forgotPasswordForm.email
                });
                
                console.log('验证码发送响应:', response);
                
                if (response.data) {
                    if (response.data.success) {
                        this.$message.success(response.data.message || '验证码已发送，请查收');
                        this.countdown = 60;
                        this.timer = setInterval(() => {
                            this.countdown--;
                            if (this.countdown <= 0) {
                                clearInterval(this.timer);
                                this.gettingCode = false;
                            }
                        }, 1000);
                    } else {
                        this.$message.error(response.data.message || '验证码发送失败');
                        this.gettingCode = false;
                    }
                } else {
                    this.$message.success('验证码已发送，请查收');
                    this.countdown = 60;
                    this.timer = setInterval(() => {
                        this.countdown--;
                        if (this.countdown <= 0) {
                            clearInterval(this.timer);
                            this.gettingCode = false;
                        }
                    }, 1000);
                }
                
            } catch (error) {
                console.error('验证码发送错误:', error);
                
                let errorMessage = '验证码发送失败，请稍后重试';
                if (error.response?.data?.message) {
                    errorMessage = error.response.data.message;
                } else if (error.message) {
                    errorMessage = error.message;
                }
                this.$message.error(errorMessage);
                this.gettingCode = false;
            }
        },
        
        /**
         * 提交重置密码
         * 使用 URLSearchParams 处理 @RequestParam 参数
         */
        async submitForgotPassword() {
            this.$refs.forgotPasswordForm.validate(async (valid) => {
                if (valid) {
                    this.loading = true;
                    
                    // 构建 URLSearchParams 对象
                    const params = new URLSearchParams();
                    params.append('email', this.forgotPasswordForm.email);
                    params.append('token', this.forgotPasswordForm.code);
                    params.append('newPassword', this.forgotPasswordForm.newPassword);
                    
                    console.log('重置密码请求参数:', {
                        email: this.forgotPasswordForm.email,
                        token: this.forgotPasswordForm.code,
                        newPassword: this.forgotPasswordForm.newPassword
                    });
                    
                    try {
                        // 调用重置密码API，传递 URLSearchParams
                        const response = await ResetPassword(params);
                        
                        console.log('重置密码响应:', response);
                        this.loading = false;
                        
                        if (response.data) {
                            if (response.data.success) {
                                this.$message.success(response.data.message || '密码重置成功，请使用新密码登录');
                                this.closeForgotPasswordDialog();
                            } else {
                                this.$message.error(response.data.message || '密码重置失败');
                            }
                        } else {
                            this.$message.success('密码重置成功，请使用新密码登录');
                            this.closeForgotPasswordDialog();
                        }
                    } catch (error) {
                        console.error('重置密码错误:', error);
                        this.loading = false;
                        
                        let errorMessage = '密码重置失败，请稍后重试';
                        if (error.response?.data?.message) {
                            errorMessage = error.response.data.message;
                        } else if (error.message) {
                            errorMessage = error.message;
                        }
                        this.$message.error(errorMessage);
                    }
                } else {
                    this.$message.warning('请检查输入信息');
                    return false;
                }
            });
        },
        
        beforeDestroy() {
            // 组件销毁时清除定时器
            if (this.timer) {
                clearInterval(this.timer);
            }
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