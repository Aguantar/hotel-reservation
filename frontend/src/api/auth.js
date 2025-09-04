import axios from 'axios'
import router from '@/router'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  withCredentials: true
})

// ── 요청 인터셉터: 토큰 첨부
api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// ── 응답 인터셉터: 401 → refresh → 재시도
let isRefreshing = false
let subscribers = []
const onRefreshed = (token) => { subscribers.forEach(cb => cb(token)); subscribers = [] }
const addSubscriber = (cb) => subscribers.push(cb)

api.interceptors.response.use(
  res => res,
  async err => {
    const { config, response } = err
    if (!response) return Promise.reject(err)
    const isAuthPath = config.url?.includes('/auth/')
    if (response.status === 401 && !config._retry && !isAuthPath) {
      config._retry = true
      if (isRefreshing) {
        return new Promise(resolve => {
          addSubscriber((newToken) => {
            config.headers.Authorization = 'Bearer ' + newToken
            resolve(api(config))
          })
        })
      }
      isRefreshing = true
      try {
        const { data } = await api.post('/auth/refresh')
        localStorage.setItem('token', data.token)
        onRefreshed(data.token)
        config.headers.Authorization = 'Bearer ' + data.token
        return api(config)
      } catch (e) {
        localStorage.removeItem('token')
        router.push('/login')
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(err)
  }
)

// === 공개 API들 ===
export const checkUsername = (loginId) =>
  api.get('/auth/check-username', { params: { loginId } }).then(r => r.data)

export const checkEmail = (email) =>
  api.get('/auth/check-email', { params: { email } }).then(r => r.data)

export const signup = (payload, verificationCode) =>
  api.post(`/auth/signup?verificationCode=${encodeURIComponent(String(verificationCode ?? ''))}`, payload)
     .then(r => r.data)


export const login = (payload) =>
  api.post('/auth/login', payload).then(r => r.data)

export const logout = () =>
  api.post('/auth/logout').then(r => r.data)

export const getMe = () =>
  api.get('/me').then(r => r.data)

// ✅ 여기 수정: request → api
export const sendEmailCode = (email) =>
  api.post('/auth/email/send', { email }).then(r => r.data)

export const verifyEmailCode = (email, code) =>
  api.post('/auth/email/verify', { email, code }).then(r => r.data)

export default api
