<template>
  <div class="page">
    <div class="welcome">
      <h1>👋 欢迎回来，{{ username }}</h1>
      <p>选择以下功能开始使用 AI 智能面试系统</p>
    </div>

    <div class="cards">
      <div class="feature-card card-purple" @click="$router.push('/interview')">
        <div class="card-icon">🎯</div>
        <h3>开始面试</h3>
        <p>启动 AI 模拟面试，实时对话评估你的技术能力</p>
        <span class="card-link">进入 →</span>
      </div>

      <div class="feature-card card-blue" @click="$router.push('/history')">
        <div class="card-icon">📜</div>
        <h3>面试历史</h3>
        <p>查看过往面试记录、得分和评估报告</p>
        <span class="card-link">查看 →</span>
      </div>

      <div class="feature-card card-mint" @click="$router.push('/jobs')">
        <div class="card-icon">💼</div>
        <h3>岗位管理</h3>
        <p>管理招聘岗位信息，设置面试评估标准</p>
        <span class="card-link">管理 →</span>
      </div>

      <div class="feature-card card-pink" @click="$router.push('/resumes')">
        <div class="card-icon">📄</div>
        <h3>简历管理</h3>
        <p>上传和管理候选人简历，AI 自动解析匹配</p>
        <span class="card-link">管理 →</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const username = ref('')

onMounted(() => {
  username.value = localStorage.getItem('username') || '用户'
})
</script>

<style scoped>
.page { padding: 40px; max-width: 960px; margin: 0 auto; }
.welcome { margin-bottom: 36px; }
.welcome h1 { font-size: 26px; color: var(--text); margin: 0 0 8px; font-weight: 700; }
.welcome p { color: var(--text-light); font-size: 15px; margin: 0; }

.cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.feature-card {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 28px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
  border: 1px solid var(--border);
  position: relative;
  overflow: hidden;
}
.feature-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 4px;
  transition: height 0.3s ease;
}
.card-purple::before { background: var(--primary); }
.card-blue::before { background: var(--accent-blue); }
.card-mint::before { background: var(--accent-mint); }
.card-pink::before { background: var(--accent-pink); }

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}
.card-icon { font-size: 42px; margin-bottom: 14px; }
.feature-card h3 { margin: 0 0 8px; font-size: 18px; color: var(--text); font-weight: 600; }
.feature-card p { margin: 0 0 16px; font-size: 14px; color: var(--text-light); line-height: 1.6; }
.card-link { color: var(--primary); font-size: 14px; font-weight: 600; }

@media (max-width: 640px) {
  .cards { grid-template-columns: 1fr; }
  .page { padding: 20px; }
}
</style>
