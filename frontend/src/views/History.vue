<template>
  <div class="page">
    <h2>📜 面试历史</h2>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="list.length === 0" class="empty">
      <div class="empty-icon">📭</div>
      <p>暂无面试记录</p>
      <button @click="$router.push('/interview')" class="btn-start">🎯 开始第一次面试</button>
    </div>

    <div v-else class="list">
      <div v-for="item in list" :key="item.id" class="history-card" @click="toggleExpand(item.id)">
        <div class="card-top">
          <div class="card-left">
            <h3>{{ item.jobTitle || '未知岗位' }}</h3>
            <span :class="['status', statusClass(item.status)]">
              {{ statusLabel(item.status) }}
            </span>
          </div>
          <div class="card-right">
            <span class="score">{{ item.totalScore ?? '--' }} 分</span>
            <span class="date">{{ item.startTime?.substring(0, 10) || '' }}</span>
            <span class="arrow">{{ expandedId === item.id ? '▲' : '▼' }}</span>
          </div>
        </div>

        <div v-if="expandedId === item.id" class="card-detail">
          <div class="detail-row">
            <span>📝 题目数量：</span><strong>{{ item.questionCount || 0 }} 题</strong>
          </div>
          <div class="detail-row">
            <span>🕐 时间：</span><strong>{{ item.startTime || '-' }}</strong>
          </div>
          <button @click.stop="viewReport(item.id)" class="btn-report">📊 查看评估报告</button>
        </div>
      </div>
    </div>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <!-- 评估报告弹窗 -->
    <div v-if="reportVisible" class="report-modal-overlay" @click="closeReport">
      <div class="report-modal" @click.stop>
        <div class="modal-header">
          <h3>📊 面试评估报告</h3>
          <button @click="closeReport" class="btn-close">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="reportLoading" class="report-loading">
            <div class="spinner"></div>
            <p>{{ reportLoadingText }}</p>
          </div>
          <div v-else-if="reportError" class="report-error">
            <p>❌ {{ reportError }}</p>
          </div>
          <div v-else-if="reportData" class="report-content">
            <div v-if="reportData.dimensions" class="report-section">
              <h4>📈 维度评分</h4>
              <div class="report-dimensions">
                <div v-for="(value, key) in reportData.dimensions" :key="key" class="report-dim-row">
                  <span class="dim-name">{{ key }}</span>
                  <div class="dim-bar-bg">
                    <div class="dim-bar-fill" :style="{ width: (value / 10 * 100) + '%' }"></div>
                  </div>
                  <span class="dim-value">{{ value }}/10</span>
                </div>
              </div>
            </div>
            <div v-if="reportData.strengths" class="report-section">
              <h4>✅ 优势</h4>
              <p class="report-text">{{ reportData.strengths }}</p>
            </div>
            <div v-if="reportData.weaknesses" class="report-section">
              <h4>⚠️ 不足</h4>
              <p class="report-text">{{ reportData.weaknesses }}</p>
            </div>
            <div v-if="reportData.suggestion" class="report-section">
              <h4>💡 改进建议</h4>
              <p class="report-text">{{ reportData.suggestion }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import apiClient from '@/utils/request'

interface HistoryItem {
  id: number
  jobTitle: string
  status: string
  totalScore: number | null
  questionCount: number
  startTime: string
}

const list = ref<HistoryItem[]>([])
const loading = ref(false)
const errorMsg = ref('')
const expandedId = ref<number | null>(null)

const reportVisible = ref(false)
const reportLoading = ref(false)
const reportLoadingText = ref('正在加载报告...')
const reportError = ref('')
const reportData = ref<any>(null)

onMounted(() => fetchHistory())

async function fetchHistory() {
  loading.value = true
  errorMsg.value = ''
  const userId = localStorage.getItem('userId') || '1'
  try {
    const res = await apiClient.get('/api/interview/history', { params: { userId } })
    if (res.data.code === 200) {
      list.value = res.data.data || []
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

function statusLabel(status: string): string {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'TERMINATED': return '已终止'
    default: return '进行中'
  }
}

function statusClass(status: string): string {
  switch (status) {
    case 'COMPLETED': return 'done'
    case 'TERMINATED': return 'terminated'
    default: return 'progress'
  }
}

function viewReport(interviewId: number) {
  reportVisible.value = true
  reportLoading.value = true
  reportLoadingText.value = '正在加载报告...'
  reportError.value = ''
  reportData.value = null
  fetchReport(interviewId, 0)
}

function closeReport() {
  reportVisible.value = false
  reportData.value = null
  reportError.value = ''
  reportLoading.value = false
}

async function fetchReport(interviewId: number, retryCount: number) {
  try {
    const token = localStorage.getItem('authToken')
    const res = await fetch(`/api/report/${interviewId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await res.json()
    if (data.code === 200) {
      reportData.value = data.data
      reportLoading.value = false
      return
    }
    if (data.message?.includes('生成中') && retryCount < 5) {
      reportLoadingText.value = `报告生成中，3秒后重试...（${retryCount + 1}/5）`
      setTimeout(() => fetchReport(interviewId, retryCount + 1), 3000)
    } else {
      reportLoading.value = false
      reportError.value = data.message || '获取报告失败'
    }
  } catch (e: any) {
    reportLoading.value = false
    reportError.value = e.message || '获取报告异常'
  }
}
</script>

<style scoped>
.page { padding: 40px; max-width: 800px; margin: 0 auto; }
.page h2 { margin: 0 0 24px; font-size: 22px; color: var(--text); font-weight: 700; }
.loading { text-align: center; color: var(--text-light); padding: 80px 0; }
.empty { text-align: center; padding: 80px 0; color: var(--text-light); }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty p { font-size: 16px; margin-bottom: 16px; }
.btn-start {
  padding: 10px 24px; background: var(--primary); color: #fff;
  border: none; border-radius: var(--radius-sm); font-size: 15px;
  cursor: pointer; font-weight: 500; transition: background 0.2s;
}
.btn-start:hover { background: var(--primary-hover); }
.list { display: flex; flex-direction: column; gap: 12px; }
.history-card {
  background: #fff; border-radius: var(--radius-md); padding: 18px 20px;
  cursor: pointer; box-shadow: var(--shadow-sm); border: 1px solid var(--border);
  transition: all 0.2s;
}
.history-card:hover { box-shadow: var(--shadow-md); }
.card-top { display: flex; justify-content: space-between; align-items: center; }
.card-left { display: flex; align-items: center; gap: 12px; }
.card-left h3 { margin: 0; font-size: 16px; color: var(--text); font-weight: 600; }
.status { font-size: 12px; padding: 3px 10px; border-radius: 10px; font-weight: 500; }
.status.done { background: #E8F8F0; color: var(--success); }
.status.terminated { background: #F4F0F8; color: var(--primary); }
.status.progress { background: #FDF3E8; color: var(--warning); }
.card-right { display: flex; align-items: center; gap: 16px; }
.score { font-size: 15px; font-weight: 600; color: var(--warning); }
.date { font-size: 13px; color: var(--text-lighter); }
.arrow { font-size: 12px; color: var(--text-lighter); }
.card-detail {
  margin-top: 16px; padding-top: 16px;
  border-top: 1px solid var(--border);
  display: flex; flex-direction: column; gap: 8px;
}
.detail-row { font-size: 14px; color: var(--text-light); }
.detail-row strong { color: var(--text); }
.btn-report {
  align-self: flex-start; padding: 8px 16px;
  background: var(--primary); color: #fff; border: none;
  border-radius: var(--radius-sm); font-size: 13px; cursor: pointer;
  margin-top: 4px; transition: background 0.2s;
}
.btn-report:hover { background: var(--primary-hover); }
.error { color: var(--danger); text-align: center; margin-top: 16px; }

/* 弹窗 */
.report-modal-overlay {
  position: fixed; inset: 0; background: rgba(74, 74, 90, 0.35);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; backdrop-filter: blur(2px);
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.report-modal {
  background: #fff; border-radius: var(--radius-lg); width: 90%;
  max-width: 560px; max-height: 80vh; display: flex; flex-direction: column;
  box-shadow: var(--shadow-lg);
  animation: slideUp 0.3s ease;
}
@keyframes slideUp { from { transform: translateY(30px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 24px; border-bottom: 1px solid var(--border); flex-shrink: 0;
}
.modal-header h3 { margin: 0; font-size: 18px; color: var(--text); }
.btn-close {
  width: 32px; height: 32px; padding: 0; background: #f5f5f5;
  color: var(--text-light); border: none; border-radius: 50%;
  font-size: 16px; cursor: pointer; display: flex; align-items: center;
  justify-content: center; transition: all 0.2s;
}
.btn-close:hover { background: var(--danger); color: #fff; }
.modal-body { padding: 24px; overflow-y: auto; flex: 1; }
.report-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: var(--text-light); }
.report-loading p { margin-top: 16px; font-size: 14px; }
.spinner { width: 36px; height: 36px; border: 3px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.report-error { text-align: center; padding: 40px 0; color: var(--danger); font-size: 14px; }
.report-content { display: flex; flex-direction: column; gap: 20px; }
.report-section h4 { margin: 0 0 10px; font-size: 15px; color: var(--text); }
.report-text {
  margin: 0; font-size: 14px; color: var(--text-light); line-height: 1.7;
  background: #FAF7FC; padding: 12px 16px; border-radius: var(--radius-sm);
  border-left: 3px solid var(--primary);
}
.report-dimensions { display: flex; flex-direction: column; gap: 10px; }
.report-dim-row { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.dim-name { width: 90px; color: var(--text-light); flex-shrink: 0; }
.dim-bar-bg { flex: 1; height: 10px; background: var(--border); border-radius: 5px; overflow: hidden; }
.dim-bar-fill {
  height: 100%; background: linear-gradient(90deg, var(--primary), var(--accent-blue));
  border-radius: 5px; transition: width 0.6s ease;
}
.dim-value { width: 45px; color: var(--text); font-weight: 600; text-align: right; flex-shrink: 0; }
</style>
