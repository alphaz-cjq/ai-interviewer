<template>
  <div class="page">
    <div class="header">
      <h2>📄 简历管理</h2>
      <label class="btn-upload">
        + 上传简历
        <input type="file" accept=".pdf,.doc,.docx,.txt" @change="handleUpload" hidden />
      </label>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="resumes.length === 0" class="empty">
      <div class="empty-icon">📂</div>
      <p>暂无简历</p>
      <span>点击右上角"上传简历"开始添加</span>
    </div>

    <div v-else class="list">
      <div v-for="item in resumes" :key="item.id" class="resume-card">
        <div class="card-left">
          <span class="file-icon">📄</span>
          <div class="card-info">
            <!-- 编辑模式 -->
            <div v-if="editingId === item.id" class="edit-row">
              <input
                v-model="editName"
                class="edit-input"
                placeholder="请输入简历名称"
                @keydown.enter="saveRename(item.id)"
                @keydown.escape="cancelRename"
                ref="editInputRef"
              />
              <button @click="saveRename(item.id)" class="btn-save">✓</button>
              <button @click="cancelRename" class="btn-cancel">✕</button>
            </div>
            <!-- 展示模式 -->
            <template v-else>
              <h4>{{ item.name || '未命名简历' }}</h4>
              <span class="time">{{ item.uploadTime || '' }}</span>
            </template>
          </div>
        </div>
        <div class="card-actions" v-if="editingId !== item.id">
          <button @click="startRename(item)" class="btn-rename">✏️ 重命名</button>
          <button @click="handleDelete(item.id)" class="btn-del">🗑 删除</button>
        </div>
      </div>
    </div>

    <p v-if="uploading" class="upload-status">⏳ 正在上传解析...</p>
    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <p v-if="successMsg" class="success">{{ successMsg }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import apiClient from '@/utils/request'

interface ResumeItem {
  id: number
  name: string
  uploadTime?: string
  userId?: number
}

const resumes = ref<ResumeItem[]>([])
const loading = ref(false)
const uploading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

const editingId = ref<number | null>(null)
const editName = ref('')
const editInputRef = ref<HTMLInputElement | null>(null)

onMounted(() => fetchResumes())

async function fetchResumes() {
  loading.value = true
  const currentUserId = Number(localStorage.getItem('userId') || '1')
  try {
    const res = await apiClient.get('/api/resume/list', { params: { userId: currentUserId } })
    if (res.data.code === 200) {
      const data: ResumeItem[] = res.data.data || []
      if (data.length > 0 && data[0]?.userId !== undefined) {
        resumes.value = data.filter(item => item.userId === currentUserId)
      } else {
        resumes.value = data
      }
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '加载失败'
  } finally { loading.value = false }
}

async function handleUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  uploading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  const formData = new FormData()
  formData.append('file', file)
  formData.append('userId', localStorage.getItem('userId') || '1')
  try {
    const res = await apiClient.post('/api/resume/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.code === 200) {
      successMsg.value = '上传成功！'
      await fetchResumes()
      setTimeout(() => successMsg.value = '', 3000)
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '上传失败'
  } finally { uploading.value = false }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除该简历？')) return
  const userId = localStorage.getItem('userId') || '1'
  try {
    await apiClient.delete(`/api/resume/${id}`, { params: { userId } })
    await fetchResumes()
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '删除失败'
  }
}

function startRename(item: ResumeItem) {
  editingId.value = item.id
  editName.value = item.name || ''
  errorMsg.value = ''
  successMsg.value = ''
  nextTick(() => {
    editInputRef.value?.focus()
    editInputRef.value?.select()
  })
}

function cancelRename() {
  editingId.value = null
  editName.value = ''
}

async function saveRename(id: number) {
  const newName = editName.value.trim()
  if (!newName) {
    errorMsg.value = '简历名称不能为空'
    return
  }
  const userId = localStorage.getItem('userId') || '1'
  try {
    const res = await apiClient.put(`/api/resume/${id}/rename`, null, {
      params: { userId, fileName: newName }
    })
    if (res.data.code === 200) {
      const item = resumes.value.find(r => r.id === id)
      if (item) item.name = newName
      successMsg.value = '重命名成功！'
      setTimeout(() => successMsg.value = '', 3000)
    } else {
      errorMsg.value = res.data.message || '重命名失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '重命名失败'
  } finally {
    editingId.value = null
    editName.value = ''
  }
}
</script>

<style scoped>
.page { padding: 40px; max-width: 800px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header h2 { margin: 0; font-size: 22px; color: var(--text); font-weight: 700; }
.btn-upload {
  padding: 10px 22px; background: var(--primary); color: #fff;
  border: none; border-radius: var(--radius-sm); font-size: 14px;
  cursor: pointer; font-weight: 500; transition: background 0.2s;
}
.btn-upload:hover { background: var(--primary-hover); }
.loading, .empty { text-align: center; color: var(--text-light); padding: 80px 0; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty p { font-size: 16px; margin-bottom: 4px; }
.empty span { font-size: 13px; color: var(--text-lighter); }
.list { display: flex; flex-direction: column; gap: 10px; }
.resume-card {
  background: #fff; border-radius: var(--radius-md); padding: 16px 20px;
  display: flex; justify-content: space-between; align-items: center;
  box-shadow: var(--shadow-sm); border: 1px solid var(--border);
  transition: box-shadow 0.2s;
}
.resume-card:hover { box-shadow: var(--shadow-md); }
.card-left { display: flex; align-items: center; gap: 14px; }
.card-info { display: flex; flex-direction: column; gap: 4px; }
.file-icon { font-size: 28px; }
.card-info h4 { margin: 0; font-size: 15px; color: var(--text); font-weight: 500; }
.time { font-size: 12px; color: var(--text-lighter); }
.card-actions { display: flex; gap: 8px; align-items: center; }
.btn-rename {
  padding: 6px 14px; background: transparent; color: var(--primary);
  border: 1px solid var(--primary); border-radius: var(--radius-sm);
  font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.btn-rename:hover { background: var(--primary); color: #fff; }
.btn-del {
  padding: 6px 14px; background: transparent; color: var(--danger);
  border: 1px solid var(--danger); border-radius: var(--radius-sm);
  font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.btn-del:hover { background: var(--danger); color: #fff; }
.edit-row { display: flex; align-items: center; gap: 6px; }
.edit-input {
  padding: 6px 10px; border: 1px solid var(--primary); border-radius: var(--radius-sm);
  font-size: 14px; outline: none; width: 220px; transition: box-shadow 0.2s;
}
.edit-input:focus { box-shadow: 0 0 0 3px rgba(155, 126, 196, 0.15); }
.btn-save {
  width: 30px; height: 30px; padding: 0; background: var(--success); color: #fff;
  border: none; border-radius: 6px; font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.btn-save:hover { opacity: 0.9; }
.btn-cancel {
  width: 30px; height: 30px; padding: 0; background: var(--danger); color: #fff;
  border: none; border-radius: 6px; font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.btn-cancel:hover { opacity: 0.9; }
.upload-status { text-align: center; color: var(--warning); font-size: 14px; margin-top: 12px; }
.error { color: var(--danger); text-align: center; margin-top: 12px; font-size: 13px; }
.success { color: var(--success); text-align: center; margin-top: 12px; font-size: 13px; }
</style>
