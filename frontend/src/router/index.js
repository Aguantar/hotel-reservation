import { createRouter, createWebHistory } from 'vue-router'
import SignupView from '@/views/SignupView.vue'
import LoginView from '@/views/LoginView.vue'
import MainView from '@/views/MainView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/signup', component: SignupView },
    { path: '/login',  component: LoginView  },
    { path: '/main',   component: MainView, meta: { requiresAuth: true } },
    { path: '/', redirect: '/main' }
  ]
})

// ✅ 소셜 리다이렉트(#token 또는 ?token)를 먼저 흡수하고 URL 정리
router.beforeEach((to, from, next) => {
  // 1) hash(#token=...) 또는 query(?token=...)에서 토큰 추출
  const hash = to.hash || window.location.hash
  const m = hash && hash.match(/token=([^&]+)/)
  const tokenFromHash = m ? decodeURIComponent(m[1]) : null
  const tokenFromQuery = to.query?.token

  if (tokenFromHash || tokenFromQuery) {
    const token = tokenFromHash || tokenFromQuery
    localStorage.setItem('token', token)
    // URL에서 해시/쿼리 제거 (화면은 그대로)
    window.history.replaceState({}, '', to.path)
  }

  // 2) 이후 토큰 기준으로 접근 제어
  const token = localStorage.getItem('token')

  // 보호 라우트는 토큰 필수
  if (to.meta.requiresAuth && !token) return next('/login')

  // 로그인/회원가입 페이지에 토큰이 있으면 메인으로
  if ((to.path === '/login' || to.path === '/signup') && token) return next('/main')

  next()
})

export default router
