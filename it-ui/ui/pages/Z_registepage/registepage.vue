
<template>
  <div class="register-page">
    <section class="register-shell">
      <div class="register-aside">
        <span class="brand-badge">Join ITSpace</span>
        <h1 class="brand-title">创建账号，加入技术分享与协作社区</h1>
        <p class="brand-desc">
          注册后即可发布博客、参与圈子讨论、体验项目协作与会员服务，把灵感和作品沉淀在同一个空间。
        </p>

        <div class="feature-list">
          <div class="feature-item">
            <i class="el-icon-edit-outline"></i>
            <div>
              <strong>开始创作</strong>
              <span>发布技术博客、记录项目经验与实践总结。</span>
            </div>
          </div>
          <div class="feature-item">
            <i class="el-icon-chat-line-round"></i>
            <div>
              <strong>加入交流</strong>
              <span>在技术圈中提问、讨论并建立持续连接。</span>
            </div>
          </div>
          <div class="feature-item">
            <i class="el-icon-folder-opened"></i>
            <div>
              <strong>进入协作</strong>
              <span>管理项目、任务与知识文档，保持工作流一致。</span>
            </div>
          </div>
        </div>
      </div>

      <el-card class="register-card" shadow="hover">
        <div class="card-head">
          <h2>创建新账号</h2>
          <p>完成基础信息后，即可开始你的社区旅程</p>
        </div>

        <el-form ref="loginform" :model="user" label-position="top" :rules="loginrules" class="register-form">
          <el-form-item label="用户名" prop="name">
            <el-input v-model="user.name" placeholder="请输入用户名" prefix-icon="el-icon-user" clearable></el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="user.password" placeholder="请输入密码" prefix-icon="el-icon-lock" show-password clearable></el-input>
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <div class="email-row">
              <el-input v-model="user.email" placeholder="请输入邮箱" prefix-icon="el-icon-message" clearable></el-input>
              <el-button type="primary" class="code-btn" @click="getcode" :disabled="gettingCode || countdown > 0">
                {{ gettingCode ? '发送中...' : (countdown > 0 ? `${countdown}秒后重试` : '获取验证码') }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="验证码" prop="code">
            <el-input v-model="user.code" placeholder="请输入验证码" prefix-icon="el-icon-key" clearable></el-input>
          </el-form-item>

          <el-form-item class="action-row">
            <el-button type="success" class="primary-action" :loading="loading" @click="register">立即注册</el-button>
            <el-button class="secondary-action" :disabled="loading" @click="$router.push('/login')">返回登录</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>
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

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.22), transparent 30%),
    radial-gradient(circle at bottom right, rgba(16, 185, 129, 0.18), transparent 28%),
    linear-gradient(135deg, #08101f 0%, #11213f 45%, #1d4c76 100%);
}

.register-shell {
  width: min(1140px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 440px);
  gap: 24px;
  align-items: stretch;
}

.register-aside,
.register-card {
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 30px 60px rgba(8, 16, 31, 0.24);
  backdrop-filter: blur(16px);
}

.register-aside {
  position: relative;
  overflow: hidden;
  padding: 36px;
  color: #0f172a;
}

.register-aside::after {
  content: '';
  position: absolute;
  right: -80px;
  bottom: -80px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.22), transparent 70%);
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 18px;
}

.brand-title {
  margin: 0 0 14px;
  font-size: clamp(2rem, 5vw, 3.25rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.brand-desc {
  margin: 0;
  max-width: 540px;
  color: #475569;
  font-size: 15px;
  line-height: 1.85;
}

.feature-list {
  display: grid;
  gap: 16px;
  margin-top: 34px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 18px 18px 16px;
  border-radius: 22px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}

.feature-item i {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: #dcfce7;
  color: #059669;
  font-size: 18px;
}

.feature-item strong {
  display: block;
  margin-bottom: 6px;
  color: #0f172a;
  font-size: 15px;
}

.feature-item span {
  display: block;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.register-card {
  padding: 10px;
}

.card-head {
  margin-bottom: 18px;
}

.card-head h2 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #0f172a;
}

.card-head p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.register-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #334155;
  font-weight: 600;
}

.register-form :deep(.el-input__inner) {
  height: 46px;
  border-radius: 14px;
  border-color: #dbeafe;
  background: #f8fbff;
}

.register-form :deep(.el-input__inner:focus) {
  border-color: #22c55e;
}

.email-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 12px;
}

.code-btn {
  width: 100%;
  border-radius: 14px;
  padding: 0 14px;
}

.action-row {
  margin-bottom: 0;
}

.primary-action,
.secondary-action {
  width: 100%;
  border-radius: 14px;
  padding: 12px 18px;
  font-weight: 700;
}

.primary-action {
  margin-bottom: 12px;
  border: none;
  background: linear-gradient(135deg, #16a34a, #22c55e);
  box-shadow: 0 14px 24px rgba(34, 197, 94, 0.2);
}

.secondary-action {
  margin-left: 0;
}

@media (max-width: 900px) {
  .register-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .register-page {
    padding: 18px 12px;
  }

  .register-aside,
  .register-card {
    border-radius: 24px;
  }

  .register-aside {
    padding: 24px 20px;
  }

  .email-row {
    grid-template-columns: 1fr;
  }
}
</style>
