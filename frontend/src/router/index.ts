import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import ForgotPassword from '../views/ForgotPassword.vue'
import Layout from '../views/Layout.vue'
import Home from '../views/Home.vue'
import Interview from '../views/Interview.vue'
import History from '../views/History.vue'
import Jobs from '../views/Jobs.vue'
import Resumes from '../views/Resumes.vue'

const routes = [
  { path: '/', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/forgot-password', name: 'ForgotPassword', component: ForgotPassword },
  {
    path: '/',
    component: Layout,
    children: [
      { path: 'home', name: 'Home', component: Home },
      { path: 'interview', name: 'Interview', component: Interview },
      { path: 'history', name: 'History', component: History },
      { path: 'jobs', name: 'Jobs', component: Jobs },
      { path: 'resumes', name: 'Resumes', component: Resumes },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录重定向到登录页
router.beforeEach((to, from, next) => {
  const publicPages = ['/', '/register', '/forgot-password']
  const token = localStorage.getItem('authToken')
  if (!token && !publicPages.includes(to.path)) {
    next('/')
  } else {
    next()
  }
})

export default router
