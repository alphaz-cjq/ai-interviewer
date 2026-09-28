<template>
  <div class="login-page">
    <div class="login-card">
      <h2>🤖 AI 智能面试官</h2>

      <div class="form-group">
        <label>用户名</label>
        <input
          v-model="username"
          placeholder="请输入用户名"
          @keyup.enter="handleLogin"
        />
      </div>

      <div class="form-group">
        <label>密码</label>
        <input
          v-model="password"
          type="password"
          placeholder="请输入密码"
          @keyup.enter="handleLogin"
        />
      </div>

      <button @click="handleLogin" :disabled="loading" class="btn-login">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="success">{{ successMsg }}</p>

      <div class="links">
        <router-link to="/register">还没有账号？立即注册</router-link>
        <router-link to="/forgot-password">忘记密码？</router-link>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import apiClient from '@/utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()

const username = ref('')
const password = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const loading = ref(false)

const handleLogin = async () => {
  errorMsg.value = ''
  successMsg.value = ''
  loading.value = true

  try {
    const response = await apiClient.post('/api/auth/login', {
      username: username.value,
      password: password.value
    })

    if (response.data.code === 200) {
      const loginToken = response.data.data
      successMsg.value = '登录成功！正在跳转...'
      localStorage.setItem('authToken', loginToken)
      localStorage.setItem('username', username.value)
      localStorage.setItem('userId', '1')
      setTimeout(() => router.push('/home'), 500)
    } else {
      errorMsg.value = response.data.message || '登录失败'
    }
  } catch (error: any) {
    if (error.response) {
      errorMsg.value = error.response.data?.message || `请求失败：${error.response.status}`
    } else if (error.request) {
      errorMsg.value = '网络异常，请确认后端服务是否启动'
    } else {
      errorMsg.value = error.message || '登录失败，请重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}
.login-card {
  width: 400px;
  padding: 36px 32px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border);
}
.login-card h2 {
  text-align: center;
  margin: 0 0 24px;
  font-size: 22px;
  color: var(--text);
}
.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-size: 13px;
  color: var(--text-light);
  margin-bottom: 4px;
}
.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.form-group input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(155, 126, 196, 0.1);
}
.btn-login {
  width: 100%;
  padding: 12px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 16px;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.2s;
}
.btn-login:hover:not(:disabled) {
  background: var(--primary-hover);
}
.btn-login:disabled {
  background: var(--text-lighter);
  cursor: not-allowed;
}
.error {
  color: var(--danger);
  font-size: 13px;
  text-align: center;
  margin-top: 12px;
}
.success {
  color: var(--success);
  font-size: 13px;
  text-align: center;
  margin-top: 12px;
}
.links {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 13px;
}
.links a {
  color: var(--primary);
  text-decoration: none;
}
.links a:hover {
  text-decoration: underline;
}
</style>
