<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'

const router = useRouter()
const loginId = ref('')
const password = ref('')
const msg = ref('')
const loading = ref(false)

async function onLogin () {
  msg.value=''; loading.value=true
  try{
    const { token } = await login({ loginId: loginId.value, password: password.value })
    localStorage.setItem('token', token)
    router.push('/main')                  // ✅ 로그인 후 메인으로 이동
  }catch(e){ msg.value = e?.response?.data?.error || '로그인 실패' }
  finally{ loading.value=false }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <h1 class="title">로그인</h1>

      <div class="grid gap">
        <input class="input" v-model="loginId" placeholder="아이디" />
        <input class="input" v-model="password" type="password" placeholder="비밀번호" />

        <div class="row-between hint">
          <span>계정이 없으신가요?</span>
          <RouterLink class="link" to="/signup">회원가입</RouterLink>
        </div>

        <button class="btn primary w-full" :disabled="loading" @click="onLogin">
          {{ loading ? '진행중...' : '로그인' }}
        </button>

        <div class="row gap" style="margin-top:8px">
  <!-- LoginView.vue -->
<a class="btn w-full" href="http://localhost:8888/oauth2/authorization/naver">Naver로 로그인</a>
<a class="btn w-full" href="http://localhost:8888/oauth2/authorization/kakao">Kakao로 로그인</a>
<a class="btn w-full" href="http://localhost:8888/oauth2/authorization/google">Google로 로그인</a>

</div>


        <p class="text-sm center" v-if="msg">{{ msg }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page{min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0b0b0c}
.card{width:100%;max-width:420px;background:#111318;border:1px solid #24262b;border-radius:16px;padding:28px}
.title{font-size:22px;font-weight:800;margin-bottom:14px;color:#e5e7eb}
.grid.gap>*{margin-top:10px}
.row-between{display:flex;align-items:center;justify-content:space-between}
.input{width:100%;padding:.75rem;border:1px solid #2a2e35;background:#0f1114;color:#e5e7eb;border-radius:10px}
.btn{padding:.65rem 1rem;border:1px solid #30343a;border-radius:10px;background:#171a1f;color:#e5e7eb}
.btn.primary{background:#6b46c1;border-color:#6b46c1}
.w-full{width:100%}.hint{color:#cbd5e1}.link{color:#8b5cf6;text-decoration:underline}
.text-sm.center{text-align:center}
</style>
