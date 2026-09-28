<template>
  <div class="page">
    <div class="card">
      <h2>🔑 找回密码</h2>

      <div class="form-group">
        <label>邮箱</label>
        <input v-model="email" placeholder="请输入注册邮箱" />
      </div>

      <div class="form-group">
        <label>验证码</label>
        <div class="code-row">
          <input v-model="code" placeholder="请输入邮箱验证码" style="flex: 1;" />
          <button @click="sendCode" :disabled="sendingCode || countdown > 0" class="btn-code">
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </button>
        </div>
      </div>

      <div class="form-group">
        <label>新密码</label>
        <input v-model="newPassword" type="password" placeholder="请输入新密码（至少6位）" />
      </div>

      <div class="form-group">
        <label>确认新密码</label>
        <input v-model="confirmPassword" type="password" placeholder="请再次输入新密码" />
      </div>

      <button @click="handleReset" :disabled="loading" class="btn-primary">
        {{ loading ? '重置中...' : '重置密码' }}
      </button>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="success">{{ successMsg }}</p>

      <p class="link-text">
        <router-link to="/">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/utils/request'

const router = useRouter()

const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const errorMsg = ref('')
const successMsg = ref('')

async function sendCode() {
  if (!email.value) { errorMsg.value = '请先输入邮箱'; return }
  sendingCode.value = true
  errorMsg.value = ''
  try {
    await apiClient.post('/api/auth/send-code', null, { params: { email: email.value } })
    successMsg.value = '验证码已发送（模拟），请查看控制台日志'
    countdown.value = 60
    const timer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(timer) }, 1000)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '发送失败'
  } finally {
    sendingCode.value = false
  }
}

async function handleReset() {
  errorMsg.value = ''
  successMsg.value = ''
  if (!email.value || !code.value || !newPassword.value) {
    errorMsg.value = '请填写所有必填项'; return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = '两次密码不一致'; return
  }
  if (newPassword.value.length < 6) {
    errorMsg.value = '新密码至少6位'; return
  }

  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('email', email.value)
    params.append('code', code.value)
    params.append('newPassword', newPassword.value)
    const res = await apiClient.post('/api/auth/reset-password', params)
    if (res.data.code === 200) {
      successMsg.value = '密码已重置！即将跳转到登录页...'
      setTimeout(() => router.push('/'), 1500)
    } else {
      errorMsg.value = res.data.message || '重置失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '重置失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 40px 20px; }
.card {
  width: 100%; max-width: 440px; background: #fff;
  border-radius: var(--radius-lg); padding: 36px 32px;
  box-shadow: var(--shadow-lg); border: 1px solid var(--border);
}
.card h2 { margin: 0 0 24px; font-size: 22px; color: var(--text); text-align: center; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; color: var(--text-light); margin-bottom: 4px; }
.form-group input {
  width: 100%; padding: 10px 12px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); font-size: 14px; outline: none;
  box-sizing: border-box; transition: border-color 0.2s, box-shadow 0.2s;
}
.form-group input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(155, 126, 196, 0.1);
}
.code-row { display: flex; gap: 10px; }
.btn-code {
  white-space: nowrap; padding: 10px 16px; background: var(--accent-blue);
  color: #fff; border: none; border-radius: var(--radius-sm);
  font-size: 13px; cursor: pointer; transition: background 0.2s;
}
.btn-code:hover:not(:disabled) { background: #6EA8D0; }
.btn-code:disabled { background: var(--text-lighter); cursor: not-allowed; }
.btn-primary {
  width: 100%; padding: 12px; margin-top: 8px;
  background: var(--primary); color: #fff; border: none;
  border-radius: var(--radius-sm); font-size: 16px;
  cursor: pointer; font-weight: 500; transition: background 0.2s;
}
.btn-primary:hover:not(:disabled) { background: var(--primary-hover); }
.btn-primary:disabled { background: var(--text-lighter); cursor: not-allowed; }
.error { color: var(--danger); font-size: 13px; text-align: center; margin-top: 12px; }
.success { color: var(--success); font-size: 13px; text-align: center; margin-top: 12px; }
.link-text { text-align: center; font-size: 13px; color: var(--text-lighter); margin-top: 16px; }
.link-text a { color: var(--primary); }
</style>
