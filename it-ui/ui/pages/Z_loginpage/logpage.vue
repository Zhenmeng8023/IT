<template>
  <div class="login-page">
    <section class="login-shell">
      <div class="login-aside">
        <span class="brand-badge">IT Forum</span>
        <h1 class="brand-title">登录你的账号，继续协作与创作</h1>
        <p class="brand-desc">
          进入博客、圈子和项目空间，延续你的写作、讨论与协作进度。
        </p>

        <div class="feature-list">
          <div class="feature-item">
            <i class="el-icon-document"></i>
            <div>
              <strong>博客创作</strong>
              <span>继续编辑、发布和管理你的技术内容。</span>
            </div>
          </div>
          <div class="feature-item">
            <i class="el-icon-chat-dot-round"></i>
            <div>
              <strong>圈子交流</strong>
              <span>回到关注的讨论圈，跟进新的互动消息。</span>
            </div>
          </div>
          <div class="feature-item">
            <i class="el-icon-folder-opened"></i>
            <div>
              <strong>项目协作</strong>
              <span>快速进入项目面板，查看任务与知识库内容。</span>
            </div>
          </div>
        </div>
      </div>

      <el-card class="login-card" shadow="hover">
        <div class="card-head">
          <h2>欢迎回来</h2>
          <p>请输入账号信息完成登录</p>
        </div>

        <el-form ref="loginform" :model="user" label-position="top" :rules="loginrules" class="login-form">
          <el-form-item label="用户名" prop="name">
            <el-input
              v-model.trim="user.name"
              placeholder="请输入用户名"
              prefix-icon="el-icon-user"
              clearable
              autocomplete="username"
            ></el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="user.password"
              placeholder="请输入密码"
              prefix-icon="el-icon-lock"
              @keyup.enter.native="login"
              show-password
              clearable
              autocomplete="current-password"
            ></el-input>
          </el-form-item>

          <div class="form-tools">
            <el-button type="text" @click="showForgotPasswordDialog">忘记密码</el-button>
            <span class="redirect-hint" v-if="redirectPath">登录后将返回之前页面</span>
          </div>

          <el-form-item class="action-row">
            <el-button type="primary" class="primary-action" @click="login" :loading="loading">
              立即登录
            </el-button>
            <el-button class="secondary-action" @click="register" :disabled="loading">
              去注册
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>

    <el-dialog
      title="忘记密码"
      :visible.sync="forgotPasswordDialogVisible"
      width="420px"
      @close="closeForgotPasswordDialog"
    >
      <el-form
        ref="forgotPasswordForm"
        :model="forgotPasswordForm"
        :rules="forgotPasswordRules"
        label-width="80px"
      >
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="forgotPasswordForm.email"
            placeholder="请输入注册邮箱"
            prefix-icon="el-icon-message"
            clearable
          ></el-input>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <el-row :gutter="10">
            <el-col :span="15">
              <el-input
                v-model="forgotPasswordForm.code"
                placeholder="请输入6位验证码"
                prefix-icon="el-icon-mobile-phone"
                clearable
                maxlength="6"
              ></el-input>
            </el-col>
            <el-col :span="9">
              <el-button
                type="primary"
                class="code-btn"
                @click="sendVerificationCode"
                :disabled="gettingCode || countdown > 0"
              >
                {{ gettingCode ? '发送中...' : (countdown > 0 ? `${countdown}秒后重试` : '获取验证码') }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="forgotPasswordForm.newPassword"
            placeholder="请输入新密码（8-20位）"
            prefix-icon="el-icon-lock"
            show-password
            clearable
          ></el-input>
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="forgotPasswordForm.confirmPassword"
            placeholder="请再次输入新密码"
            prefix-icon="el-icon-lock"
            show-password
            clearable
          ></el-input>
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
import { useUserStore } from '@/store/user'
import { SendPasswordResetVerifyCode, ResetPassword } from '@/api/index.js'

export default {
  layout: 'login',
  data() {
    return {
      user: {
        name: '',
        password: ''
      },
      loginrules: {
        name: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 1, max: 20, message: '用户名长度必须在1-20之间', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 8, max: 20, message: '密码长度必须在8-20之间', trigger: 'blur' }
        ]
      },
      loading: false,
      forgotPasswordDialogVisible: false,
      forgotPasswordForm: {
        email: '',
        code: '',
        newPassword: '',
        confirmPassword: ''
      },
      forgotPasswordRules: {
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        code: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { len: 6, message: '验证码长度必须为6位', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 8, max: 20, message: '密码长度必须在8-20之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== this.forgotPasswordForm.newPassword) {
                callback(new Error('两次输入密码不一致'))
              } else {
                callback()
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
  computed: {
    redirectPath() {
      const redirect = this.$route.query.redirect
      return typeof redirect === 'string' && redirect.trim() ? redirect : ''
    }
  },
  created() {
    this.redirectIfLoggedIn()
  },
  beforeDestroy() {
    this.clearTimer()
  },
  methods: {
    getUserStore() {
      return useUserStore()
    },
    clearTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
      this.gettingCode = false
    },
    async redirectIfLoggedIn() {
      if (!process.client) {
        return
      }

      const userStore = this.getUserStore()
      userStore.restorePermissions()

      if (!userStore.userInfo && !userStore.token) {
        return
      }

      try {
        await userStore.syncSessionFromServer({
          forceReloadPermissions: !userStore.permissions?.length
        })
        this.navigateAfterLogin()
      } catch (error) {
        userStore.clearLocalState()
      }
    },
    resolveDefaultRoute(roleId) {
      return roleId === 4 ? '/' : '/homepage'
    },
    navigateAfterLogin() {
      const userStore = this.getUserStore()
      const fallbackPath = this.resolveDefaultRoute(userStore.userInfo?.roleId)
      const targetPath = this.redirectPath || fallbackPath

      if (this.$route.fullPath !== targetPath) {
        this.$router.replace(targetPath)
      }
    },
    async login() {
      const valid = await new Promise(resolve => {
        this.$refs.loginform.validate(result => resolve(result))
      })

      if (!valid) {
        this.$message.warning('请检查输入信息')
        return
      }

      this.loading = true

      try {
        const userStore = this.getUserStore()
        await userStore.login({
          username: this.user.name,
          password: this.user.password
        })

        this.$message.success('登录成功')
        this.navigateAfterLogin()
      } catch (error) {
        let errorMessage = '登录失败，请稍后重试'
        if (error.response?.data?.message) {
          errorMessage = error.response.data.message
        } else if (error.message) {
          errorMessage = error.message
        }
        this.$message.error(errorMessage)
      } finally {
        this.loading = false
      }
    },
    register() {
      this.$router.push({
        path: '/registe',
        query: this.redirectPath ? { redirect: this.redirectPath } : {}
      })
    },
    showForgotPasswordDialog() {
      this.forgotPasswordDialogVisible = true
    },
    closeForgotPasswordDialog() {
      this.forgotPasswordDialogVisible = false
      this.resetForgotPasswordForm()
    },
    resetForgotPasswordForm() {
      this.forgotPasswordForm = {
        email: '',
        code: '',
        newPassword: '',
        confirmPassword: ''
      }
      this.countdown = 0
      this.clearTimer()
    },
    async sendVerificationCode() {
      let isValid = true

      await this.$refs.forgotPasswordForm.validateField('email', error => {
        if (error) {
          isValid = false
        }
      })

      if (!isValid) {
        return
      }

      this.gettingCode = true

      try {
        const response = await SendPasswordResetVerifyCode({
          email: this.forgotPasswordForm.email
        })

        if (response.data && response.data.success === false) {
          this.$message.error(response.data.message || '验证码发送失败')
          this.gettingCode = false
          return
        }

        this.$message.success(response.data?.message || '验证码已发送，请查收')
        this.countdown = 60
        this.timer = setInterval(() => {
          this.countdown -= 1
          if (this.countdown <= 0) {
            this.clearTimer()
          }
        }, 1000)
      } catch (error) {
        const errorMessage =
          error.response?.data?.message ||
          error.message ||
          '验证码发送失败，请稍后重试'
        this.$message.error(errorMessage)
        this.gettingCode = false
      }
    },
    async submitForgotPassword() {
      const valid = await new Promise(resolve => {
        this.$refs.forgotPasswordForm.validate(result => resolve(result))
      })

      if (!valid) {
        this.$message.warning('请检查输入信息')
        return
      }

      this.loading = true

      try {
        const params = new URLSearchParams()
        params.append('email', this.forgotPasswordForm.email)
        params.append('token', this.forgotPasswordForm.code)
        params.append('newPassword', this.forgotPasswordForm.newPassword)

        const response = await ResetPassword(params)
        if (response.data && response.data.success === false) {
          this.$message.error(response.data.message || '密码重置失败')
          return
        }

        this.$message.success(response.data?.message || '密码重置成功，请使用新密码登录')
        this.closeForgotPasswordDialog()
      } catch (error) {
        const errorMessage =
          error.response?.data?.message ||
          error.message ||
          '密码重置失败，请稍后重试'
        this.$message.error(errorMessage)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.22), transparent 30%),
    radial-gradient(circle at bottom right, rgba(14, 165, 233, 0.2), transparent 28%),
    linear-gradient(135deg, #0f172a 0%, #172554 45%, #1e3a8a 100%);
}

.login-shell {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 430px);
  gap: 24px;
  align-items: stretch;
}

.login-aside,
.login-card {
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.22);
  backdrop-filter: blur(16px);
}

.login-aside {
  padding: 36px;
  color: #0f172a;
  position: relative;
  overflow: hidden;
}

.login-aside::after {
  content: '';
  position: absolute;
  right: -60px;
  bottom: -60px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.22), transparent 70%);
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 18px;
}

.brand-title {
  margin: 0 0 14px;
  font-size: clamp(2rem, 5vw, 3.3rem);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.brand-desc {
  margin: 0;
  max-width: 520px;
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
  background: #e0ecff;
  color: #1d4ed8;
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

.login-card {
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

.login-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #334155;
  font-weight: 600;
}

.login-form :deep(.el-input__inner) {
  height: 46px;
  border-radius: 14px;
  border-color: #dbeafe;
  background: #f8fbff;
}

.login-form :deep(.el-input__inner:focus) {
  border-color: #3b82f6;
}

.form-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 12px;
}

.redirect-hint {
  color: #94a3b8;
  font-size: 12px;
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
  box-shadow: 0 14px 24px rgba(37, 99, 235, 0.2);
}

.secondary-action {
  margin-left: 0;
}

.code-btn {
  width: 100%;
  border-radius: 12px;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 18px 12px;
  }

  .login-aside,
  .login-card {
    border-radius: 24px;
  }

  .login-aside {
    padding: 24px 20px;
  }

  .form-tools {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
