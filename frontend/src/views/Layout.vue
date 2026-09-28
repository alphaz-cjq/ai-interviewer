<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-icon">✨</span>
          <span class="logo-text">AI 面试</span>
        </div>
        <div class="user-info">
          <div class="avatar-circle">{{ username.charAt(0) }}</div>
          <span class="username">{{ username }}</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/home" class="nav-item" :class="{ active: $route.path === '/home' }">
          <span class="nav-icon">📋</span> 首页
        </router-link>
        <router-link to="/interview" class="nav-item" :class="{ active: $route.path === '/interview' }">
          <span class="nav-icon">🎯</span> 开始面试
        </router-link>
        <router-link to="/history" class="nav-item" :class="{ active: $route.path === '/history' }">
          <span class="nav-icon">📜</span> 面试历史
        </router-link>
        <router-link to="/jobs" class="nav-item" :class="{ active: $route.path === '/jobs' }">
          <span class="nav-icon">💼</span> 岗位管理
        </router-link>
        <router-link to="/resumes" class="nav-item" :class="{ active: $route.path === '/resumes' }">
          <span class="nav-icon">📄</span> 简历管理
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button @click="logout" class="btn-logout">🚪 退出登录</button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="main-content">
      <router-view v-slot="{ Component }">
        <keep-alive :include="['Interview']">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')

onMounted(() => {
  username.value = localStorage.getItem('username') || '用户'
})

function logout() {
  localStorage.removeItem('authToken')
  localStorage.removeItem('username')
  localStorage.removeItem('userId')
  router.push('/')
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 220px;
  min-width: 220px;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 16px rgba(155, 126, 196, 0.08);
  z-index: 10;
  border-right: 1px solid var(--border);
}
.sidebar-header {
  padding: 24px 20px 20px;
  border-bottom: 1px solid var(--border);
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}
.logo-icon {
  font-size: 22px;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  letter-spacing: 0.5px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--bg-start), var(--primary-light));
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}
.username {
  font-size: 14px;
  color: var(--text);
  font-weight: 500;
}

/* ===== 导航 ===== */
.sidebar-nav {
  flex: 1;
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  margin: 0 10px;
  border-radius: var(--radius-sm);
  color: var(--text-light);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}
.nav-item:hover {
  background: #F8F4FC;
  color: var(--primary);
}
.nav-item.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}
.nav-icon {
  font-size: 16px;
  width: 22px;
  text-align: center;
}

/* ===== 底部 ===== */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border);
}
.btn-logout {
  width: 100%;
  padding: 10px;
  background: transparent;
  color: var(--text-light);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-logout:hover {
  background: #FEF0F0;
  color: var(--danger);
  border-color: #F5D0D0;
}

/* ===== 主内容区 ===== */
.main-content {
  flex: 1;
  overflow-y: auto;
}
</style>
