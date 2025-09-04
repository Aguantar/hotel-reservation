<script setup>
import { ref, onMounted } from 'vue'
import { getMe, logout as apiLogout } from '@/api/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const me = ref(null)
const msg = ref('')

async function fetchMe () {
  msg.value = ''
  try {
    me.value = await getMe()
  } catch (e) {
    msg.value = `토큰 확인 실패: ${e?.response?.status || ''}`
  }
}

onMounted(async () => {
  // 1) 소셜 로그인 성공 시 /main#token=... 으로 오므로, 해시에서 토큰 추출
  const m = location.hash.match(/token=([^&]+)/)
  if (m) {
    const token = decodeURIComponent(m[1])
    localStorage.setItem('token', token)
    history.replaceState({}, '', location.pathname) // 주소창에서 해시 제거
  }

  // 2) 저장된 토큰이 전혀 없으면 로그인 페이지로
  if (!localStorage.getItem('token')) {
    router.push('/login')
    return
  }

  // 3) 내 정보 로딩
  await fetchMe()
})

async function checkToken () { await fetchMe() }

async function logout () {
  try { await apiLogout() } catch (_) {}
  localStorage.removeItem('token')
  me.value = null
  router.push('/login')
}
</script>

<template>
  <div class="page">
    <div class="card">
      <div class="head">
        <h1 class="title">메인 페이지</h1>
        <p class="greet" v-if="me">환영합니다, <b>{{ me.name }}</b>님</p>
      </div>

      <div class="row gap">
        <button class="btn primary" @click="checkToken">토큰 확인(/api/me)</button>
        <button class="btn" @click="logout">로그아웃</button>
      </div>

      <p class="hint" v-if="msg">{{ msg }}</p>
      <pre v-if="me" class="mt" style="white-space:pre-wrap">{{ JSON.stringify(me, null, 2) }}</pre>
    </div>
  </div>
</template>

<style scoped>
.page{min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0b0b0c}
.card{width:100%;max-width:720px;background:#111318;border:1px solid #24262b;border-radius:16px;padding:28px}
.head{display:flex;align-items:baseline;justify-content:space-between;margin-bottom:10px}
.title{font-size:22px;font-weight:800;color:#e5e7eb}
.greet{color:#cbd5e1}
.row{display:flex;align-items:center}.gap{gap:10px}
.btn{padding:.65rem 1rem;border:1px solid #30343a;border-radius:10px;background:#171a1f;color:#e5e7eb;cursor:pointer}
.btn.primary{background:#6b46c1;border-color:#6b46c1}
.hint{color:#cbd5e1}.mt{margin-top:12px}
</style>
