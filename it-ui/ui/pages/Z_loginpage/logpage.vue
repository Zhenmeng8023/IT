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
      
      <!-- 忘记密码弹窗 - 两步流程 -->
      <el-dialog
          title="忘记密码"
          :visible.sync="forgotPasswordDialogVisible"
          width="400px"
          @close="closeForgotPasswordDialog"
      >
          <!-- 第一步：邮箱和验证码 -->
          <div v-if="step === 1">
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
              </el-form>
              <div slot="footer" class="dialog-footer">
                  <el-button @click="closeForgotPasswordDialog">取消</el-button>
                  <el-button type="primary" @click="verifyCode" :loading="verifying">验证</el-button>
              </div>
          </div>

          <!-- 第二步：新密码设置 -->
          <div v-else-if="step === 2">
              <el-form ref="newPasswordForm" :model="newPasswordForm" :rules="newPasswordRules" label-width="80px">
                  <el-form-item label="新密码" prop="newPassword">
                      <el-input v-model="newPasswordForm.newPassword" placeholder="请输入新密码（8-20位）" prefix-icon="el-icon-lock" show-password clearable></el-input>
                  </el-form-item>
                  <el-form-item label="确认密码" prop="confirmPassword">
                      <el-input v-model="newPasswordForm.confirmPassword" placeholder="请确认新密码" prefix-icon="el-icon-lock" show-password clearable></el-input>
                  </el-form-item>
              </el-form>
              <div slot="footer" class="dialog-footer">
                  <el-button @click="goBackStep">返回</el-button>
                  <el-button type="primary" @click="submitResetPassword" :loading="resetting">重置密码</el-button>
              </div>
          </div>
      </el-dialog>
    </div>
</template>

<script>
// 引入 Pinia store
import { useUserStore } from '@/store/user'
// 引入API - 注意函数名称与 api/index.js 中的导出一致
import { SendPasswordResetVerifyCode, VerifyPasswordResetToken, ResetPassword } from '@/api/index.js'

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
                    { min: 1, max: 10, message: '用户名长度必须在1-10之间', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    { min: 8, max: 20, message: '密码长度必须在8-20之间', trigger: 'blur' }
                ]
            },
            loading: false,
            // 忘记密码相关
            forgotPasswordDialogVisible: false,
            step: 1,                      // 1: 验证码验证, 2: 新密码设置
            verifying: false,
            resetting: false,
            // 第一步表单
            forgotPasswordForm: {
                email: '',
                code: ''
            },
            forgotPasswordRules: {
                email: [
                    { required: true, message: '请输入邮箱', trigger: 'blur' },
                    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
                ],
                code: [
                    { required: true, message: '请输入验证码', trigger: 'blur' },
                    { len: 6, message: '验证码长度必须为6位', trigger: 'blur' }
                ]
            },
            // 第二步表单
            newPasswordForm: {
                newPassword: '',
                confirmPassword: ''
            },
            newPasswordRules: {
                newPassword: [
                    { required: true, message: '请输入新密码', trigger: 'blur' },
                    { min: 8, max: 20, message: '密码长度必须在8-20之间', trigger: 'blur' }
                ],
                confirmPassword: [
                    { required: true, message: '请确认密码', trigger: 'blur' },
                    {
                        validator: (rule, value, callback) => {
                            if (value !== this.newPasswordForm.newPassword) {
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
            timer: null,
            // 临时存储已验证的邮箱和验证码
            verifiedEmail: '',
            verifiedToken: ''
        };
    },
    methods: {
        login() {
            this.$refs.loginform.validate((valid) => {
                if (valid) {
                    this.loading = true;
                    console.log('开始登录，用户名:', this.user.name);

                    const userStore = useUserStore();

                    userStore
                        .login({
                            username: this.user.name,
                            password: this.user.password
                        })
                        .then((response) => {
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
                        .catch((error) => {
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
        register() {
            this.$router.push('/registe');
        },

        // ========== 忘记密码相关方法 ==========
        showForgotPasswordDialog() {
            // 重置所有状态
            this.step = 1;
            this.forgotPasswordForm = { email: '', code: '' };
            this.newPasswordForm = { newPassword: '', confirmPassword: '' };
            this.verifiedEmail = '';
            this.verifiedToken = '';
            this.countdown = 0;
            if (this.timer) {
                clearInterval(this.timer);
                this.gettingCode = false;
            }
            this.forgotPasswordDialogVisible = true;
            // 清除验证状态
            this.$nextTick(() => {
                this.$refs.forgotPasswordForm?.clearValidate();
            });
        },

        closeForgotPasswordDialog() {
            this.forgotPasswordDialogVisible = false;
            if (this.timer) {
                clearInterval(this.timer);
            }
        },

        /**
         * 发送验证码
         */
        async sendVerificationCode() {
            let isValid = true;
            await this.$refs.forgotPasswordForm.validateField('email', (error) => {
                if (error) isValid = false;
            });
            if (!isValid) return;

            this.gettingCode = true;
            console.log('发送验证码，邮箱:', this.forgotPasswordForm.email);

            try {
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
         * 验证验证码 - 使用 VerifyPasswordResetToken 接口
         */
        async verifyCode() {
            // 先验证表单字段
            let isValid = true;
            await this.$refs.forgotPasswordForm.validate((valid) => {
                if (!valid) isValid = false;
            });
            if (!isValid) return;

            this.verifying = true;
            try {
                // 调用验证令牌接口（GET 请求，参数为 email 和 token）
                const response = await VerifyPasswordResetToken({
                    email: this.forgotPasswordForm.email,
                    token: this.forgotPasswordForm.code
                });
                console.log('验证码验证响应:', response);

                if (response.data && response.data.success) {
                    // 验证通过，存储已验证的邮箱和验证码
                    this.verifiedEmail = this.forgotPasswordForm.email;
                    this.verifiedToken = this.forgotPasswordForm.code;
                    // 进入第二步
                    this.step = 2;
                    // 重置新密码表单
                    this.newPasswordForm = { newPassword: '', confirmPassword: '' };
                    this.$nextTick(() => {
                        this.$refs.newPasswordForm?.clearValidate();
                    });
                    this.$message.success('验证码验证通过，请设置新密码');
                } else {
                    this.$message.error(response.data?.message || '验证码错误或已过期');
                }
            } catch (error) {
                console.error('验证码验证错误:', error);
                let errorMessage = '验证码验证失败，请重试';
                if (error.response?.data?.message) {
                    errorMessage = error.response.data.message;
                } else if (error.message) {
                    errorMessage = error.message;
                }
                this.$message.error(errorMessage);
            } finally {
                this.verifying = false;
            }
        },

        /**
         * 返回第一步
         */
        goBackStep() {
            this.step = 1;
        },

        /**
         * 提交重置密码（第二步）
         */
        async submitResetPassword() {
            // 验证新密码表单
            let isValid = true;
            await this.$refs.newPasswordForm.validate((valid) => {
                if (!valid) isValid = false;
            });
            if (!isValid) return;

            this.resetting = true;
            // 使用第一步验证通过的邮箱和验证码，构建 URLSearchParams（后端使用 @RequestParam）
            const params = new URLSearchParams();
            params.append('email', this.verifiedEmail);
            params.append('token', this.verifiedToken);
            params.append('newPassword', this.newPasswordForm.newPassword);

            try {
                const response = await ResetPassword(params);
                console.log('重置密码响应:', response);
                this.resetting = false;

                if (response.data && response.data.success) {
                    this.$message.success(response.data.message || '密码重置成功，请使用新密码登录');
                    this.closeForgotPasswordDialog();
                } else {
                    this.$message.error(response.data?.message || '密码重置失败');
                }
            } catch (error) {
                console.error('重置密码错误:', error);
                let errorMessage = '密码重置失败，请稍后重试';
                if (error.response?.data?.message) {
                    errorMessage = error.response.data.message;
                } else if (error.message) {
                    errorMessage = error.message;
                }
                this.$message.error(errorMessage);
                this.resetting = false;
            }
        },

        beforeDestroy() {
            if (this.timer) {
                clearInterval(this.timer);
            }
        }
    }
};
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
    .box-card {
        margin: 20px 20px;
    }
</style>