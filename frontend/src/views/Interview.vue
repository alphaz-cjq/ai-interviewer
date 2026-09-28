<template>
  <div class="interview-page">
    <!-- ===== 顶部导航栏 ===== -->
    <div class="header-bar">
      <div class="header-left">
        <span class="header-title">🤖 AI 智能面试官</span>
        <span class="header-job">{{ jobTitle }}</span>
      </div>
      <div class="header-right">
        <span v-if="interviewStarted && !interviewEnded" class="question-badge">
          第 {{ questionNum }} 题
        </span>
        <button v-if="interviewStarted && !interviewEnded" @click="endInterview" class="btn-end">
          ⏹ 结束面试
        </button>
        <button v-if="interviewEnded" @click="resetInterview" class="btn-reset">
          🔄 重新面试
        </button>
      </div>
    </div>

    <!-- ===== 对话消息区域 ===== -->
    <div class="chat-area" ref="chatContainer">
      <!-- 面试未开始 — 选择简历和岗位 -->
      <div v-if="!interviewStarted" class="welcome-screen">
        <div class="welcome-icon">🤖</div>
        <h2>AI 智能面试官</h2>
        <p class="welcome-desc">选择您的简历和目标岗位，AI 将结合两者进行针对性提问</p>

        <div class="setup-form">
          <!-- 选择简历 -->
          <div class="setup-group">
            <label>📄 选择简历</label>
            <select v-model="selectedResumeId" class="setup-select">
              <option :value="null" disabled>-- 请选择简历 --</option>
              <option v-for="r in resumeList" :key="r.id" :value="r.id">
                {{ r.name || '未命名简历' }}
              </option>
            </select>
            <span v-if="resumeList.length === 0" class="setup-hint">
              暂无简历，请先到
              <router-link to="/resumes">简历管理</router-link>
              上传
            </span>
          </div>

          <!-- 选择岗位 -->
          <div class="setup-group">
            <label>💼 选择岗位</label>
            <select v-model="selectedJobId" class="setup-select">
              <option :value="null" disabled>-- 请选择岗位 --</option>
              <option v-for="j in jobList" :key="j.id" :value="j.id">
                {{ j.title }}
              </option>
            </select>
            <span v-if="jobList.length === 0" class="setup-hint">
              暂无岗位，请先到
              <router-link to="/jobs">岗位管理</router-link>
              添加
            </span>
          </div>
        </div>

        <button
          @click="startInterview"
          :disabled="loading || !selectedResumeId || !selectedJobId"
          class="btn-start"
        >
          {{ loading ? '⏳ 准备中...' : '🚀 开始面试' }}
        </button>
        <p v-if="!selectedResumeId || !selectedJobId" class="setup-warn">
          ⚠️ 请先选择简历和岗位后再开始面试
        </p>
      </div>

      <!-- 消息列表 -->
      <div v-else class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-row', `message-${msg.role}`]"
        >
          <!-- 系统消息 -->
          <div v-if="msg.role === 'system'" class="system-bubble">
            <span class="system-text">{{ msg.text }}</span>
          </div>

          <!-- AI 消息（左侧） -->
          <div v-else-if="msg.role === 'ai'" class="ai-row">
            <div class="avatar ai-avatar">🤖</div>
            <div class="bubble-wrapper">
              <div class="bubble ai-bubble">
                <span class="bubble-text">{{ getDisplayText(msg) }}</span>
                <span v-if="msg.isStreaming" class="typing-cursor">|</span>
              </div>
              <div v-if="msg.questionNum" class="bubble-label">
                第 {{ msg.questionNum }} 题
              </div>
            </div>
          </div>

          <!-- 用户消息（右侧） -->
          <div v-else-if="msg.role === 'user'" class="user-row">
            <div class="bubble-wrapper user-wrapper">
              <div class="bubble user-bubble">
                <span class="bubble-text">{{ msg.text }}</span>
              </div>
              <div v-if="msg.score !== undefined" class="score-tag">
                ⭐ {{ msg.score }} 分
                <span v-if="msg.comment" class="score-comment">{{ msg.comment }}</span>
              </div>
            </div>
            <div class="avatar user-avatar">👤</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 底部输入区域 ===== -->
    <div v-if="interviewStarted" class="input-area">
      <!-- 面试结束面板 -->
      <div v-if="interviewEnded" class="end-panel">
        <div class="end-summary">
          <span class="end-icon">🎉</span>
          <span class="end-title">面试结束！</span>
          <span class="end-score">总分：<strong>{{ totalScore }}</strong></span>
          <span class="end-comment">{{ endComment }}</span>
        </div>
        <!-- 评估报告（可拖拽调整高度） -->
        <div v-if="showReport && reportData" class="report-panel">
          <!-- 拖拽手柄 -->
          <div class="report-drag-handle" @mousedown="onReportDragStart">
            <span class="drag-dots">⋮⋮⋮</span>
            <span class="drag-label">拖拽调整高度</span>
          </div>
          <!-- 可滚动报告内容 -->
          <div class="report-scroll-area" :style="{ height: reportPanelHeight + 'px' }">
            <h4>📊 面试评估报告</h4>
            <div class="report-dimensions">
              <div v-for="(value, key) in reportData.dimensions" :key="key" class="report-dim-row">
                <span class="dim-name">{{ key }}</span>
                <div class="dim-bar-bg">
                  <div class="dim-bar-fill" :style="{ width: (value / 10 * 100) + '%' }"></div>
                </div>
                <span class="dim-value">{{ value }}/10</span>
              </div>
            </div>
            <div class="report-item"><strong>✅ 优势：</strong>{{ reportData.strengths }}</div>
            <div class="report-item"><strong>⚠️ 不足：</strong>{{ reportData.weaknesses }}</div>
            <div class="report-item"><strong>💡 改进建议：</strong>{{ reportData.suggestion }}</div>
          </div>
        </div>
      </div>

      <!-- 输入框 -->
      <div v-if="!interviewEnded" class="input-row">
        <textarea
          v-model="answerText"
          placeholder="输入你的回答... (Enter 发送，Shift+Enter 换行)"
          rows="3"
          class="input-textarea"
          :disabled="submitting || isAiTyping"
          @keydown="handleKeydown"
        ></textarea>
        <button
          @click="submitAnswer"
          :disabled="submitting || isAiTyping || !answerText.trim()"
          class="btn-send"
        >
          {{ submitting ? '⏳' : '📤' }}
        </button>
      </div>

      <!-- 调试信息 -->
      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
      <div v-if="debugInfo" class="debug-msg">{{ debugInfo }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, nextTick, onActivated } from 'vue'
import apiClient from '@/utils/request'

defineOptions({ name: 'Interview' })

// ============ 类型定义 ============
interface Message {
  id: number
  role: 'ai' | 'user' | 'system'
  text: string
  displayedText?: string    // 逐字显示（打字机效果），流式完成前使用，完成後清除
  isStreaming?: boolean
  score?: number
  comment?: string
  questionNum?: number
}

// ============ 请求拦截器 ============
apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('authToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ============ 状态变量 ============
const loading = ref(false)
const submitting = ref(false)
const interviewStarted = ref(false)
const interviewEnded = ref(false)

const jobTitle = ref('')
const questionNum = ref(1)

// ============ 简历 + 岗位选择 ============
interface ResumeOption { id: number; name: string }
interface JobOption { id: number; title: string }
const resumeList = ref<ResumeOption[]>([])
const jobList = ref<JobOption[]>([])
const selectedResumeId = ref<number | null>(null)
const selectedJobId = ref<number | null>(null)
const totalScore = ref(0)
const endComment = ref('')
const answerText = ref('')
const errorMsg = ref('')
const debugInfo = ref('')
const isAiTyping = ref(false)

const reportData = ref<any>(null)
const showReport = ref(false)

const interviewId = ref<number | null>(null)
const currentQuestionId = ref<number | null>(null)

// 幂等 token：同一道题的提交复用同一个 token，换题后重置，防止重复提交
const idempotencyToken = ref('')

// ============ 对话消息 ============
const messages = ref<Message[]>([])
let msgIdCounter = 0
const chatContainer = ref<HTMLElement | null>(null)

// 当前正在流式输出的 AI 消息 ID（null 表示没有在流式输出）
const streamingMsgId = ref<number | null>(null)

// ============ 报告面板可拖拽调整高度 ============
const reportPanelHeight = ref(280)       // 报告面板默认高度(px)
const isDragging = ref(false)
const dragStartY = ref(0)
const dragStartHeight = ref(0)

function onReportDragStart(e: MouseEvent) {
  isDragging.value = true
  dragStartY.value = e.clientY
  dragStartHeight.value = reportPanelHeight.value
  document.addEventListener('mousemove', onReportDragMove)
  document.addEventListener('mouseup', onReportDragEnd)
  document.body.style.cursor = 'ns-resize'
  document.body.style.userSelect = 'none'
}

function onReportDragMove(e: MouseEvent) {
  if (!isDragging.value) return
  const delta = dragStartY.value - e.clientY  // 向上拖增大高度
  const newHeight = Math.max(100, Math.min(600, dragStartHeight.value + delta))
  reportPanelHeight.value = newHeight
}

function onReportDragEnd() {
  isDragging.value = false
  document.removeEventListener('mousemove', onReportDragMove)
  document.removeEventListener('mouseup', onReportDragEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

// ============ 辅助函数 ============
function genId(): number {
  return ++msgIdCounter
}

function generateIdempotencyToken(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

// SSE 接口异常时，后端会返回 HTTP 200 + JSON 错误体（BusinessException 走 GlobalExceptionHandler）。
// 用 response.ok 判断会误当成功，然后拿 JSON 去解析 SSE 导致卡死。
// 所以这里按 Content-Type 区分：text/event-stream 才是真流，否则读 JSON 拿 message 抛错。
async function ensureSSEResponse(response: Response): Promise<void> {
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('text/event-stream')) return
  const body = await response.json().catch(() => null)
  throw new Error(body?.message || `请求失败（HTTP ${response.status}）`)
}

function getUserId(): string {
  return localStorage.getItem('userId') || '1'
}

function pushMessage(msg: Omit<Message, 'id'> & { id?: number }): Message {
  const newMsg: Message = { id: msg.id ?? genId(), ...msg }
  if (msg.isStreaming) {
    newMsg.displayedText = ''
  }
  messages.value.push(newMsg)
  // ✅ 从 reactive 数组中取出 Proxy 包装后的对象，保证后续所有赋值都经过 Vue 的 Proxy 拦截
  //    直接返回 newMsg 的话是 plain object，赋值不会触发视图更新！
  const proxyMsg = messages.value[messages.value.length - 1]
  return proxyMsg as Message
}

/** 通过 ID 从 reactive 数组中查找消息（拿到的是 Proxy，保证响应式） */
function getMsgById(id: number): Message | undefined {
  return messages.value.find(m => m.id === id)
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// ============ 打字机效果 ============
const typewriterTimers = new Map<number, ReturnType<typeof setInterval>>()

function runTypewriter(msgId: number) {
  if (typewriterTimers.has(msgId)) return

  const msg = getMsgById(msgId)
  if (!msg) return
  if (msg.displayedText === undefined) {
    msg.displayedText = ''
  }

  const timer = setInterval(() => {
    // ✅ 每 tick 都从 reactive 数组取，保证读到最新数据 + 写入经过 Proxy
    const m = getMsgById(msgId)
    if (!m) {
      clearInterval(timer)
      typewriterTimers.delete(msgId)
      return
    }

    if (!m.isStreaming) {
      // 流式结束：一次性补全剩余文本
      if ((m.displayedText ?? '').length < m.text.length) {
        m.displayedText = m.text
        scrollToBottom()
      }
      clearInterval(timer)
      typewriterTimers.delete(msgId)
      return
    }

    // 流式中：逐字追加
    const target = m.text
    const current = m.displayedText ?? ''
    if (current.length < target.length) {
      const chunk = Math.min(1 + Math.floor(Math.random() * 3), target.length - current.length)
      m.displayedText = target.substring(0, current.length + chunk)
      scrollToBottom()
    }
  }, 30)

  typewriterTimers.set(msgId, timer)
}

function stopTypewriter(msgId: number) {
  const timer = typewriterTimers.get(msgId)
  if (timer) {
    clearInterval(timer)
    typewriterTimers.delete(msgId)
  }
  const msg = getMsgById(msgId)
  if (msg) {
    msg.displayedText = msg.text
    msg.isStreaming = false
  }
}

function getDisplayText(msg: Message): string {
  // 流式消息：用打字机文本；非流式：直接用完整文本
  if (msg.isStreaming || msg.displayedText !== undefined) {
    return msg.displayedText ?? ''
  }
  return msg.text
}

// 监听消息变化自动滚动
watch(
  () => [messages.value.length, messages.value.map(m => m.displayedText ?? m.text).join('')],
  () => nextTick(() => scrollToBottom()),
  { deep: false }
)

// ============ 加载简历和岗位列表 ============
async function loadResumeAndJobLists() {
  try {
    const [resumeRes, jobRes] = await Promise.all([
      apiClient.get('/api/resume/list'),  // 后端从 SecurityContext 获取 userId
      apiClient.get('/api/job')
    ])
    if (resumeRes.data.code === 200) resumeList.value = resumeRes.data.data || []
    if (jobRes.data.code === 200) jobList.value = jobRes.data.data || []
  } catch (e: any) {
    console.error('加载简历/岗位列表失败', e)
  }
}

// 页面加载时获取列表
loadResumeAndJobLists()

// keep-alive 激活时（从其他页面切回来）刷新列表并滚动到底部
onActivated(() => {
  loadResumeAndJobLists()
  nextTick(() => scrollToBottom())
})

// ============ 面试控制 ============
const startInterview = async () => {
  if (!selectedResumeId.value || !selectedJobId.value) {
    errorMsg.value = '请先选择简历和岗位'
    return
  }

  // 设置岗位标题
  const selectedJob = jobList.value.find(j => j.id === selectedJobId.value)
  jobTitle.value = selectedJob?.title || '未知岗位'
  loading.value = true
  errorMsg.value = ''
  interviewStarted.value = true

  pushMessage({ role: 'system', text: '🎯 面试开始，AI 面试官正在准备题目...' })

  await fetchStreamQuestion()
  loading.value = false
}

const resetInterview = () => {
  // 清除所有打字机定时器
  typewriterTimers.forEach(t => clearInterval(t))
  typewriterTimers.clear()

  interviewStarted.value = false
  interviewEnded.value = false
  questionNum.value = 1
  totalScore.value = 0
  endComment.value = ''
  answerText.value = ''
  interviewId.value = null
  currentQuestionId.value = null
  idempotencyToken.value = ''
  errorMsg.value = ''
  debugInfo.value = ''
  isAiTyping.value = false
  streamingMsgId.value = null
  reportData.value = null
  showReport.value = false
  messages.value = []
  msgIdCounter = 0
}

const endInterview = async () => {
  // 清除打字机定时器
  typewriterTimers.forEach(t => clearInterval(t))
  typewriterTimers.clear()

  interviewEnded.value = true
  isAiTyping.value = false
  streamingMsgId.value = null
  pushMessage({ role: 'system', text: '⏹ 您已主动结束面试。' })

  // 通知后端将面试状态改为 TERMINATED
  if (!interviewId.value) {
    debugInfo.value = '⚠ 未获取到面试ID，无法更新状态'
    return
  }

  try {
    const token = localStorage.getItem('authToken')
    const resp = await fetch(`/api/interview/${interviewId.value}/terminate`, {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${token}` }
    })
    if (!resp.ok) {
      const errText = await resp.text()
      debugInfo.value = `⚠ 终止状态更新失败：HTTP ${resp.status} - ${errText}`
    } else {
      debugInfo.value = '✅ 面试已终止，状态已同步到后端'
    }
  } catch (e: any) {
    debugInfo.value = `⚠ 终止状态更新网络错误：${e.message}`
  }
}

// ============ 键盘事件 ============
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    submitAnswer()
  }
}

// ============ SSE 流式接收题目 ============
const fetchStreamQuestion = async () => {
  isAiTyping.value = true
  debugInfo.value = 'AI 正在出题...'

  // 创建一条新的 AI 消息用于流式输出（pushMessage 返回 reactive Proxy）
  const aiMsg = pushMessage({
    role: 'ai',
    text: '',
    isStreaming: true,
  })
  const aiMsgId = aiMsg.id
  streamingMsgId.value = aiMsgId
  runTypewriter(aiMsgId)   // 启动打字机效果

  try {
    const token = localStorage.getItem('authToken')
    const response = await fetch(
      `/api/interview/start/stream`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ jobId: selectedJobId.value, resumeId: selectedResumeId.value })
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    await ensureSSEResponse(response)

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) continue
        if (!line.startsWith('data:')) continue

        const data = line.substring(5).trim()
        if (data === '') continue

        try {
          const parsed = JSON.parse(data)

          if (parsed && typeof parsed === 'object') {
            // 面试 ID
            if (parsed.interviewId) {
              interviewId.value = parsed.interviewId
            }
            // 题目 ID
            if (parsed.questionId) {
              currentQuestionId.value = parsed.questionId
              idempotencyToken.value = ''   // 新题目 → 换新幂等 token
            }
            // 题目编号 — 流式结束
            if (parsed.questionNum) {
              questionNum.value = parsed.questionNum
              // ✅ 通过 reactive 数组修改（确保触发渲染）
              const m = getMsgById(aiMsgId)
              if (m) {
                m.isStreaming = false
                m.questionNum = parsed.questionNum
              }
              streamingMsgId.value = null
              isAiTyping.value = false
              debugInfo.value = '题目已生成完成'
            }
          }
        } catch {
          // 非 JSON → 纯文本 token，追加到当前 AI 消息
          aiMsg.text += data
        }
      }
    }
  } catch (error: any) {
    console.error('SSE 连接失败：', error)
    errorMsg.value = '流式连接失败：' + error.message
    stopTypewriter(aiMsgId)
    streamingMsgId.value = null
    isAiTyping.value = false
  }
}

// ============ 提交答案 ============
const submitAnswer = async () => {
  if (!answerText.value.trim()) return
  if (!interviewId.value || !currentQuestionId.value) {
    errorMsg.value = '面试ID或题目ID丢失，请重新开始面试'
    return
  }

  submitting.value = true
  errorMsg.value = ''
  debugInfo.value = '提交中...'
  isAiTyping.value = true

  // 幂等 token：同一道题复用同一个 token，只有换题后才重置
  if (!idempotencyToken.value) {
    idempotencyToken.value = generateIdempotencyToken()
  }

  // 1. 立即显示用户消息
  const userAnswer = answerText.value.trim()
  pushMessage({
    role: 'user',
    text: userAnswer,
  })
  answerText.value = ''

  // 2. 创建新的 AI 消息用于接收回复
  const aiMsg = pushMessage({
    role: 'ai',
    text: '',
    isStreaming: true,
  })
  const aiMsgId = aiMsg.id
  streamingMsgId.value = aiMsgId
  runTypewriter(aiMsgId)   // 启动打字机效果

  try {
    const token = localStorage.getItem('authToken')
    const response = await fetch(
      `/api/interview/submit/stream`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          interviewId: interviewId.value,
          questionId: currentQuestionId.value,
          answerText: userAnswer,
          idempotencyToken: idempotencyToken.value
        })
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    await ensureSSEResponse(response)

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) continue
        if (!line.startsWith('data:')) continue

        const data = line.substring(5).trim()
        if (data === '') continue

        try {
          const parsed = JSON.parse(data)

          if (parsed.score !== undefined) {
            // 评分事件 — 更新上一条用户消息的评分
            const lastUserMsg = findLastUserMessage()
            if (lastUserMsg) {
              lastUserMsg.score = parsed.score
              lastUserMsg.comment = parsed.comment
            }
            debugInfo.value = `得分：${parsed.score}，评价：${parsed.comment}`
          } else if (parsed.type === 'INTERVIEW_END') {
            // 面试结束
            interviewEnded.value = true
            totalScore.value = parsed.totalScore || 0
            endComment.value = parsed.comment || '面试结束'
            stopTypewriter(aiMsgId)
            // ✅ 通过 reactive 数组更新文本
            const endAIMsg = getMsgById(aiMsgId)
            if (endAIMsg) {
              endAIMsg.text = parsed.comment || '面试结束，感谢您的参与！'
              endAIMsg.isStreaming = false
            }
            streamingMsgId.value = null
            isAiTyping.value = false
            pushMessage({ role: 'system', text: `🎉 面试结束！总分：${parsed.totalScore || 0}` })
            fetchReport()
            debugInfo.value = '面试结束'
          } else if (parsed.questionNum) {
            // 下一题
            if (parsed.questionId) {
              currentQuestionId.value = parsed.questionId
              idempotencyToken.value = ''   // 新题目 → 换新幂等 token
            }
            if (parsed.interviewId) {
              interviewId.value = parsed.interviewId
            }
            questionNum.value = parsed.questionNum
            const nextAIMsg = getMsgById(aiMsgId)
            if (nextAIMsg) {
              nextAIMsg.questionNum = parsed.questionNum
              nextAIMsg.isStreaming = false
            }
            streamingMsgId.value = null
            isAiTyping.value = false
            debugInfo.value = `第 ${questionNum.value} 题`
          }
        } catch {
          // 非 JSON → 文本 token，追加到当前 AI 消息
          aiMsg.text += data
        }
      }
    }
  } catch (error: any) {
    console.error('提交答案失败：', error)
    errorMsg.value = '提交失败：' + error.message
    stopTypewriter(aiMsgId)
    streamingMsgId.value = null
    isAiTyping.value = false
  } finally {
    submitting.value = false
  }
}

// ============ 查找最后一条用户消息（用于回填评分）============
function findLastUserMessage(): Message | undefined {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const msg = messages.value[i]
    if (msg && msg.role === 'user') {
      return msg
    }
  }
  return undefined
}

// ============ 获取评估报告 ============
const fetchReport = async (retryCount = 0) => {
  try {
    const token = localStorage.getItem('authToken')
    const res = await fetch(`/api/report/${interviewId.value}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await res.json()

    if (data.code === 200) {
      reportData.value = data.data
      showReport.value = true
      debugInfo.value = '报告已生成'
      await nextTick(() => scrollToBottom())
      return
    }

    if (data.message?.includes('生成中') && retryCount < 5) {
      debugInfo.value = `报告生成中，3秒后重试...（${retryCount + 1}/5）`
      setTimeout(() => fetchReport(retryCount + 1), 3000)
    } else {
      debugInfo.value = '获取报告失败：' + data.message
    }
  } catch (e) {
    console.error('获取报告异常：', e)
  }
}
</script>

<style scoped>
/* ===== 整体布局 ===== */
.interview-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
}

/* ===== 顶部导航栏 ===== */
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  z-index: 10;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}
.header-job {
  font-size: 13px;
  color: var(--primary);
  background: var(--primary-light);
  padding: 4px 10px;
  border-radius: 12px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.question-badge {
  font-size: 13px;
  color: var(--accent-blue);
  background: var(--bg-mid);
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}
.btn-end {
  padding: 6px 14px;
  background: transparent;
  color: var(--danger);
  border: 1px solid var(--danger);
  border-radius: var(--radius-sm);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-end:hover {
  background: var(--danger);
  color: #fff;
}
.btn-reset {
  padding: 6px 14px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-reset:hover { background: var(--primary-hover); }

/* ===== 对话区域 ===== */
.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
}

/* ===== 欢迎页 ===== */
.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.welcome-icon { font-size: 64px; margin-bottom: 16px; }
.welcome-screen h2 { font-size: 24px; color: var(--text); margin-bottom: 8px; }
.welcome-screen p { color: var(--text-light); margin-bottom: 8px; }
.welcome-desc {
  max-width: 400px;
  color: var(--text-lighter) !important;
  font-size: 14px;
  line-height: 1.6;
}
.setup-form {
  width: 100%; max-width: 380px;
  display: flex; flex-direction: column; gap: 16px;
  margin: 8px 0 4px;
}
.setup-group { display: flex; flex-direction: column; gap: 6px; }
.setup-group label {
  font-size: 14px; font-weight: 600;
  color: var(--text); text-align: left;
}
.setup-select {
  width: 100%; padding: 10px 14px;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  font-size: 14px; background: #fff; outline: none; cursor: pointer;
}
.setup-select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(155, 126, 196, 0.1);
}
.setup-hint { font-size: 12px; color: var(--text-lighter); text-align: left; }
.setup-hint a { color: var(--primary); text-decoration: underline; }
.setup-warn { color: var(--warning); font-size: 13px; margin-top: 8px; }
.btn-start {
  margin-top: 24px; padding: 14px 40px;
  background: linear-gradient(135deg, var(--primary), var(--primary-hover));
  color: #fff; border: none; border-radius: 28px;
  font-size: 17px; cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 14px rgba(155, 126, 196, 0.35);
}
.btn-start:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(155, 126, 196, 0.5);
}
.btn-start:disabled {
  background: var(--text-lighter);
  box-shadow: none; cursor: not-allowed;
}

/* ===== 消息列表 ===== */
.message-list {
  display: flex; flex-direction: column; gap: 16px;
  max-width: 800px; width: 100%; margin: 0 auto;
}

/* ===== 系统消息 ===== */
.system-bubble { display: flex; justify-content: center; padding: 4px 0; }
.system-text {
  font-size: 13px; color: var(--text-lighter);
  background: #F4F0F8; padding: 6px 16px; border-radius: 12px;
}

/* ===== AI 消息行 ===== */
.ai-row { display: flex; align-items: flex-start; gap: 10px; }
.avatar {
  width: 38px; height: 38px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.ai-avatar { background: var(--bg-start); }
.user-avatar { background: var(--bg-mid); }
.bubble-wrapper { max-width: 70%; display: flex; flex-direction: column; gap: 4px; }
.bubble { padding: 12px 16px; border-radius: 18px; font-size: 15px; line-height: 1.7; word-break: break-word; }
.ai-bubble {
  background: #fff; color: var(--text);
  border-top-left-radius: 4px; box-shadow: var(--shadow-sm);
}
.bubble-label { font-size: 12px; color: var(--text-lighter); padding-left: 4px; }

/* ===== 用户消息行 ===== */
.user-row { display: flex; align-items: flex-start; justify-content: flex-end; gap: 10px; }
.user-wrapper { align-items: flex-end; max-width: 70%; }
.user-bubble {
  background: linear-gradient(135deg, var(--accent-blue), #8EBCE0);
  color: #fff; border-top-right-radius: 4px;
  box-shadow: 0 1px 3px rgba(126, 184, 224, 0.25);
}
.score-tag {
  font-size: 12px; color: var(--warning);
  background: #FDF6E8; padding: 3px 10px; border-radius: 10px;
  text-align: right; align-self: flex-end;
  display: flex; align-items: center; gap: 6px;
}
.score-comment { color: var(--text-light); font-size: 11px; }

/* ===== 打字光标 ===== */
.typing-cursor {
  display: inline-block; width: 2px; height: 1em;
  background: var(--primary); animation: blink 0.8s infinite;
  vertical-align: text-bottom; margin-left: 2px;
}
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

/* ===== 底部输入区域 ===== */
.input-area {
  flex-shrink: 0; background: #fff;
  border-top: 1px solid var(--border); padding: 12px 20px;
  display: flex; flex-direction: column; gap: 8px;
}

/* ===== 结束面板 ===== */
.end-panel { display: flex; flex-direction: column; gap: 12px; }
.end-summary {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 10px 16px; background: #E8F8F0; border-radius: 10px;
}
.end-icon { font-size: 24px; }
.end-title { font-size: 17px; font-weight: 700; color: var(--success); }
.end-score { font-size: 15px; color: var(--text); }
.end-score strong { color: var(--warning); font-size: 18px; }
.end-comment { font-size: 14px; color: var(--text-light); }

/* ===== 评估报告 ===== */
.report-panel {
  background: #FAF7FC; border-radius: 10px;
  border: 1px solid var(--border); overflow: hidden;
  display: flex; flex-direction: column;
}
.report-drag-handle {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 6px 16px; background: #F4F0F8; cursor: ns-resize;
  border-bottom: 1px solid var(--border); user-select: none;
  transition: background 0.15s;
}
.report-drag-handle:hover { background: #EDE8F2; }
.drag-dots { font-size: 14px; color: var(--text-lighter); letter-spacing: 2px; }
.drag-label { font-size: 11px; color: var(--text-lighter); }
.report-scroll-area { overflow-y: auto; padding: 16px; }
.report-scroll-area h4 { margin: 0 0 12px 0; font-size: 15px; color: var(--text); }
.report-dimensions { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.report-dim-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.dim-name { width: 80px; color: var(--text-light); }
.dim-bar-bg { flex: 1; height: 8px; background: var(--border); border-radius: 4px; overflow: hidden; }
.dim-bar-fill {
  height: 100%; background: linear-gradient(90deg, var(--primary), var(--accent-blue));
  border-radius: 4px; transition: width 0.6s ease;
}
.dim-value { width: 40px; color: var(--text); font-weight: 600; text-align: right; }
.report-item {
  font-size: 13px; color: var(--text-light); margin-bottom: 6px; line-height: 1.6;
}

/* ===== 输入行 ===== */
.input-row {
  display: flex; gap: 10px; align-items: flex-end;
  max-width: 800px; width: 100%; margin: 0 auto;
}
.input-textarea {
  flex: 1; padding: 10px 14px; border: 1px solid var(--border);
  border-radius: var(--radius-md); font-size: 14px; line-height: 1.6;
  resize: none; outline: none; transition: border-color 0.2s;
}
.input-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(155, 126, 196, 0.1);
}
.input-textarea:disabled { background: #F8F6FA; color: var(--text-lighter); }
.btn-send {
  width: 48px; height: 48px; padding: 0;
  background: var(--primary); color: #fff; border: none;
  border-radius: 50%; font-size: 20px; cursor: pointer; flex-shrink: 0;
  transition: transform 0.15s, background 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.btn-send:hover:not(:disabled) {
  background: var(--primary-hover); transform: scale(1.05);
}
.btn-send:disabled { background: var(--text-lighter); cursor: not-allowed; }

/* ===== 调试 / 错误 ===== */
.error-msg { color: var(--danger); font-size: 12px; text-align: center; }
.debug-msg { color: var(--text-lighter); font-size: 12px; text-align: center; }
</style>
