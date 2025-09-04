<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { checkUsername, checkEmail, signup,
         sendEmailCode, verifyEmailCode } from '@/api/auth'   // ✅ 추가
import { openPostcode } from '@/lib/daumPostcode'

const form = ref({
  loginId:'', password:'', password2:'',
  name:'', email:'', phone:'',
  address1:'', address2:'', postcode:'',
  gender:'UNKNOWN', birthDate:''
})

const idCheck = ref(null)
const emailCheck = ref(null)
const loading = ref(false)
const msg = ref('')
const addr2Ref = ref(null)

// ✅ 이메일 인증 상태/메시지/쿨다운
const code = ref('')
const emailVerified = ref(false)
const codeMsg = ref('')
const cooldown = ref(0)
let cooldownTimer = null

async function onCheckId(){ idCheck.value = (await checkUsername(form.value.loginId)).available }
async function onCheckEmail(){ emailCheck.value = (await checkEmail(form.value.email)).available }

async function onSearchAddress () {
  await openPostcode((data) => {
    const addr = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress
    form.value.address1 = addr
    form.value.postcode = data.zonecode || ''
    nextTick(() => addr2Ref.value?.focus())
  })
}

// ✅ 인증코드 발송
async function onSendCode () {
  codeMsg.value = ''
  emailVerified.value = false
  if (!form.value.email) { codeMsg.value = '이메일을 먼저 입력하세요.'; return }
  // 중복 이메일이면 발송 막기
  const { available } = await checkEmail(form.value.email)
  if (!available) { codeMsg.value = '이미 사용 중인 이메일입니다.'; return }

  try {
    await sendEmailCode(form.value.email)      // POST /api/auth/email/send
    codeMsg.value = '인증 코드를 전송했습니다.'
    startCooldown(60)
  } catch (e) {
    codeMsg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '코드 전송 실패'
  }
}

function startCooldown (sec) {
  clearInterval(cooldownTimer)
  cooldown.value = sec
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}

// ✅ 인증코드 확인
async function onVerifyCode () {
  codeMsg.value = ''
  try {
   const { verified } = await verifyEmailCode(form.value.email, code.value) // POST /api/auth/email/verify
    if (verified) {
      emailVerified.value = true
      codeMsg.value = '이메일 인증 완료!'
      clearInterval(cooldownTimer)
      cooldown.value = 0
    } else {
      emailVerified.value = false
      codeMsg.value = '인증 코드가 일치하지 않습니다.'
    }
  } catch (e) {
    emailVerified.value = false
    codeMsg.value = e?.response?.data?.message || e?.response?.data?.error || e?.message || '인증 실패'
  }
}
onBeforeUnmount(() => clearInterval(cooldownTimer))

async function onSubmit () {
  msg.value = ''
  if (form.value.password !== form.value.password2) {
    msg.value = '비밀번호가 일치하지 않습니다.'; return
  }
  if (!emailVerified.value) {
    msg.value = '이메일 인증이 필요합니다.'; return
  }

  loading.value = true
  try {
    await signup({
      loginId: form.value.loginId,
      password: form.value.password,
      name: form.value.name,
      email: form.value.email,
      phone: form.value.phone,
      address1: form.value.address1,
      address2: form.value.address2,
      postcode: form.value.postcode,
      gender: form.value.gender,
      birthDate: form.value.birthDate || null
    }, code.value)
    msg.value = '회원가입 완료! 로그인 페이지로 이동하세요.'
  } catch (e) {
    msg.value = e?.response?.data?.error || '가입 실패'
  } finally { loading.value = false }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <h1 class="title">회원가입</h1>

      <div class="grid gap">
        <div class="row gap">
          <input class="input" v-model="form.loginId" placeholder="아이디(영문/숫자 6자+)" />
          <button class="btn" @click="onCheckId">중복확인</button>
          <span v-if="idCheck!==null" :class="idCheck?'ok':'bad'">{{ idCheck?'사용가능':'중복' }}</span>
        </div>

        <input class="input" type="password" v-model="form.password" placeholder="비밀번호" />
        <input class="input" type="password" v-model="form.password2" placeholder="비밀번호 확인" />
        <input class="input" v-model="form.name" placeholder="이름" />

        <!-- 이메일 + 중복확인 -->
        <div class="row gap">
          <input class="input flex1" type="email" v-model="form.email" placeholder="이메일" />
          <button class="btn" @click="onCheckEmail">중복확인</button>
          <span v-if="emailCheck!==null" :class="emailCheck?'ok':'bad'">{{ emailCheck?'사용가능':'중복' }}</span>
        </div>

        <!-- 이메일 인증: 코드 발송/확인 -->
        <div class="row gap">
          <input class="input" v-model="code" placeholder="인증 코드 입력" />
          <button class="btn" :disabled="cooldown>0" @click="onSendCode">
            {{ cooldown>0 ? `재발송(${cooldown}s)` : '인증코드 발송' }}
          </button>
          <button class="btn" @click="onVerifyCode">확인</button>
        </div>
        <p class="hint" :class="emailVerified?'ok':'bad'">{{ codeMsg }}</p>

        <input class="input" v-model="form.phone" placeholder="휴대폰" />

        <div class="row gap">
          <input class="input flex1" v-model="form.address1" placeholder="주소(도로명/지번)" />
          <button class="btn" type="button" @click="onSearchAddress">주소 검색</button>
        </div>
        <div class="row gap">
          <input class="input flex1" ref="addr2Ref" v-model="form.address2" placeholder="상세주소 (동/호수 등)" />
          <input class="input" style="max-width:120px" v-model="form.postcode" placeholder="우편번호" />
        </div>

        <div class="row gap">
          <label><input type="radio" value="MALE" v-model="form.gender" /> 남자</label>
          <label><input type="radio" value="FEMALE" v-model="form.gender" /> 여자</label>
          <label><input type="radio" value="UNKNOWN" v-model="form.gender" /> 선택안함</label>
        </div>

        <input class="input" v-model="form.birthDate" type="date" placeholder="YYYY-MM-DD" />

        <div class="row-between hint">
          <span>계정이 있으신가요?</span>
          <RouterLink class="link" to="/login">로그인</RouterLink>
        </div>

        <button class="btn primary w-full" :disabled="loading" @click="onSubmit">
          {{ loading ? '처리중...' : '가입하기' }}
        </button>

        <p class="text-sm center" v-if="msg">{{ msg }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 기존 스타일 유지 */
.page{min-height:100vh;display:flex;align-items:center;justify-content:center;background:#0b0b0c}
.card{width:100%;max-width:520px;background:#111318;border:1px solid #24262b;border-radius:16px;padding:28px;box-shadow:0 10px 30px rgba(0,0,0,.35)}
.title{font-size:22px;font-weight:800;margin-bottom:14px;color:#e5e7eb}
.grid.gap>*{margin-top:10px}
.row{display:flex;align-items:center}
.row-between{display:flex;align-items:center;justify-content:space-between}
.gap{gap:10px}
.flex1{flex:1}
.input{width:100%;padding:.75rem;border:1px solid #2a2e35;background:#0f1114;color:#e5e7eb;border-radius:10px;outline:none}
.input:focus{border-color:#6b46c1}
.btn{padding:.65rem 1rem;border:1px solid #30343a;border-radius:10px;background:#171a1f;color:#e5e7eb;cursor:pointer}
.btn.primary{background:#6b46c1;border-color:#6b46c1}
.w-full{width:100%}
.hint{font-size:.9rem;color:#cbd5e1}
.link{color:#8b5cf6;text-decoration:underline}
.ok{color:#22c55e}.bad{color:#ef4444}
.center{text-align:center}
</style>
