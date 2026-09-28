<template>
  <div class="page">
    <div class="header">
      <h2>💼 岗位管理</h2>
      <button @click="showDialog = true" class="btn-add">+ 新增岗位</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="jobs.length === 0" class="empty">
      <div class="empty-icon">📋</div>
      <p>暂无岗位</p>
      <span>点击右上角"新增岗位"开始添加</span>
    </div>

    <div v-else class="grid">
      <div v-for="job in jobs" :key="job.id" class="job-card">
        <div class="job-header">
          <h3>{{ job.title }}</h3>
          <button @click="handleDelete(job.id)" class="btn-del">🗑</button>
        </div>
        <p class="job-desc">{{ job.description || '暂无描述' }}</p>
        <div class="job-meta">
          <span v-if="job.department" class="meta-tag">🏢 {{ job.department }}</span>
          <span v-if="job.location" class="meta-tag">📍 {{ job.location }}</span>
        </div>
      </div>
    </div>

    <!-- 新增岗位弹窗 -->
    <div v-if="showDialog" class="dialog-overlay" @click.self="showDialog = false">
      <div class="dialog">
        <h3>新增岗位</h3>
        <div class="form-group">
          <label>岗位名称 <span class="required">*</span></label>
          <input v-model="newJob.title" placeholder="例如：Java后端开发工程师" />
        </div>
        <div class="form-group">
          <label>岗位描述</label>
          <textarea v-model="newJob.description" rows="4" placeholder="岗位职责、任职要求..."></textarea>
        </div>
        <div class="form-group">
          <label>部门</label>
          <input v-model="newJob.department" placeholder="例如：研发部" />
        </div>
        <div class="form-group">
          <label>工作地点</label>
          <input v-model="newJob.location" placeholder="例如：北京" />
        </div>
        <div class="dialog-btns">
          <button @click="showDialog = false" class="btn-cancel">取消</button>
          <button @click="handleCreate" :disabled="creating" class="btn-confirm">
            {{ creating ? '创建中...' : '确认创建' }}
          </button>
        </div>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import apiClient from '@/utils/request'

interface Job {
  id: number
  title: string
  description?: string
  department?: string
  location?: string
}

const jobs = ref<Job[]>([])
const loading = ref(false)
const creating = ref(false)
const errorMsg = ref('')
const showDialog = ref(false)

const newJob = reactive({ title: '', description: '', department: '', location: '' })

onMounted(() => fetchJobs())

async function fetchJobs() {
  loading.value = true
  try {
    const res = await apiClient.get('/api/job')
    if (res.data.code === 200) jobs.value = res.data.data || []
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '加载失败'
  } finally { loading.value = false }
}

async function handleCreate() {
  if (!newJob.title.trim()) { errorMsg.value = '岗位名称不能为空'; return }
  creating.value = true
  errorMsg.value = ''
  try {
    const res = await apiClient.post('/api/job', newJob)
    if (res.data.code === 200) {
      showDialog.value = false
      newJob.title = ''; newJob.description = ''; newJob.department = ''; newJob.location = ''
      await fetchJobs()
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '创建失败'
  } finally { creating.value = false }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除该岗位？')) return
  try {
    await apiClient.delete(`/api/job/${id}`)
    await fetchJobs()
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '删除失败'
  }
}
</script>

<style scoped>
.page { padding: 40px; max-width: 960px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header h2 { margin: 0; font-size: 22px; color: var(--text); font-weight: 700; }
.btn-add {
  padding: 10px 22px; background: var(--primary); color: #fff;
  border: none; border-radius: var(--radius-sm); font-size: 14px;
  cursor: pointer; transition: background 0.2s; font-weight: 500;
}
.btn-add:hover { background: var(--primary-hover); }
.loading, .empty { text-align: center; color: var(--text-light); padding: 80px 0; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty p { font-size: 16px; margin-bottom: 4px; }
.empty span { font-size: 13px; color: var(--text-lighter); }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.job-card {
  background: #fff; border-radius: var(--radius-md); padding: 20px;
  box-shadow: var(--shadow-sm); border: 1px solid var(--border);
  transition: all 0.2s;
}
.job-card:hover { box-shadow: var(--shadow-md); }
.job-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.job-header h3 { margin: 0; font-size: 16px; color: var(--text); font-weight: 600; }
.btn-del { background: none; border: none; font-size: 16px; cursor: pointer; opacity: 0.4; padding: 4px; transition: opacity 0.2s; }
.btn-del:hover { opacity: 1; }
.job-desc { font-size: 13px; color: var(--text-light); line-height: 1.6; margin: 0 0 14px; }
.job-meta { display: flex; gap: 10px; flex-wrap: wrap; }
.meta-tag {
  font-size: 12px; color: var(--text-light);
  background: var(--primary-light); padding: 3px 10px;
  border-radius: 12px;
}
.error { color: var(--danger); font-size: 13px; margin-top: 8px; }

/* 弹窗 */
.dialog-overlay {
  position: fixed; inset: 0; background: rgba(74, 74, 90, 0.3);
  display: flex; align-items: center; justify-content: center; z-index: 100;
  backdrop-filter: blur(2px);
}
.dialog {
  background: #fff; border-radius: var(--radius-lg); padding: 28px;
  width: 440px; max-height: 80vh; overflow-y: auto;
  box-shadow: var(--shadow-lg);
}
.dialog h3 { margin: 0 0 20px; font-size: 18px; color: var(--text); }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; color: var(--text-light); margin-bottom: 4px; }
.form-group .required { color: var(--accent-pink); }
.form-group input, .form-group textarea {
  width: 100%; padding: 10px 12px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); font-size: 14px; outline: none;
  box-sizing: border-box; transition: border-color 0.2s;
}
.form-group input:focus, .form-group textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(155, 126, 196, 0.1);
}
.dialog-btns { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
.btn-cancel {
  padding: 9px 20px; background: #f5f5f5; color: var(--text-light);
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  cursor: pointer; font-size: 14px; transition: all 0.2s;
}
.btn-cancel:hover { background: #eee; }
.btn-confirm {
  padding: 9px 20px; background: var(--primary); color: #fff;
  border: none; border-radius: var(--radius-sm); cursor: pointer;
  font-size: 14px; font-weight: 500; transition: background 0.2s;
}
.btn-confirm:hover { background: var(--primary-hover); }
.btn-confirm:disabled { background: var(--text-lighter); cursor: not-allowed; }
@media (max-width: 640px) { .grid { grid-template-columns: 1fr; } .page { padding: 20px; } }
</style>
