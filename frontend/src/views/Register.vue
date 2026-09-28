<template>
  <div class="page">
    <div class="card">
      <h2>📝 用户注册</h2>

      <div class="form-group">
        <label>用户名</label>
        <input v-model="form.username" placeholder="请输入用户名" />
      </div>

      <div class="form-group">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </div>

      <div class="form-group">
        <label>确认密码</label>
        <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" />
      </div>

      <div class="form-group">
        <label>邮箱</label>
        <input v-model="form.email" placeholder="请输入邮箱" />
      </div>

      <div class="form-group">
        <label>手机号</label>
        <input v-model="form.phone" placeholder="请输入手机号（选填）" />
      </div>

      <div class="form-group">
        <label>验证码</label>
        <div class="code-row">
          <input v-model="form.code" placeholder="请输入邮箱验证码" style="flex: 1;" />
          <button
            @click="sendCode"
            :disabled="sendingCode || countdown > 0"
            class="btn-code"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </button>
        </div>
      </div>

      <button @click="handleRegister" :disabled="loading" class="btn-primary">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="success">{{ successMsg }}</p>

      <p class="link-text">
        已有账号？
        <router-link to="/">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/utils/request'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  code: ''
})

const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const errorMsg = ref('')
const successMsg = ref('')

async function sendCode() {
  if (!form.email) { errorMsg.value = '请先输入邮箱'; return }
  sendingCode.value = true
  errorMsg.value = ''
  try {
    await apiClient.post('/api/auth/send-code', null, { params: { email: form.email } })
    successMsg.value = '验证码已发送（模拟），请查看控制台日志'
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '发送失败'
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!form.username || !form.password || !form.email || !form.code) {
    errorMsg.value = '请填写所有必填项'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMsg.value = '两次密码不一致'
    return
  }
  if (form.password.length < 6) {
    errorMsg.value = '密码至少6位'
    return
  }

  loading.value = true
  try {
    const res = await apiClient.post('/api/auth/register', {
      username: form.username,
      password: form.password,
      email: form.email,
      phone: form.phone || '',
      code: form.code
    })
    if (res.data.code === 200) {
      successMsg.value = '注册成功！即将跳转到登录页...'
      setTimeout(() => router.push('/'), 1500)
    } else {
      errorMsg.value = res.data.message || '注册失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.card {
  width: 100%;
  max-width: 440px;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 36px 32px;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border);
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
