<template>
  <div class="landing-page">
    <nav class="top-nav" :class="{ scrolled: isScrolled }">
      <div class="nav-logo">
        <span class="logo-mark">◍</span>
        <span class="logo-text">Campus Orbit</span>
      </div>
    </nav>
    <div class="nav-actions" :class="{ scrolled: isScrolled }">
      <button class="btn-ghost" @click="router.push('/login')">登录</button>
      <button class="btn-solid" @click="router.push('/register')">免费加入</button>
    </div>

    <section class="hero-section">
      <div class="hero-noise"></div>
      <div class="floating-bg">
        <div
          v-for="(img, i) in floatingImages"
          :key="i"
          class="float-img"
          :style="{
            top: img.top,
            left: img.left,
            width: img.width,
            animationDelay: img.delay,
            zIndex: img.zIndex
          }"
        >
          <img :src="img.url" :alt="img.label" />
        </div>
      </div>
      <div class="hero-center">
        <p class="hero-tag reveal-text delay-1">新生 / 社团 / 活动 / 创作</p>
        <div class="reveal-container">
          <h1 class="reveal-text delay-2">你的校园宇宙</h1>
        </div>
        <div class="reveal-container">
          <h1 class="reveal-text delay-3">从一个社团开始发光</h1>
        </div>
        <div class="reveal-container">
          <p class="hero-sub reveal-text delay-4">
            浏览社团，加入活动，记录每一次协作和成长。
          </p>
        </div>
        <div class="hero-actions reveal-text delay-4">
          <button class="btn-main" @click="router.push('/register')">立即注册</button>
        </div>
      </div>
    </section>

    <section class="scroll-box scroll-box-1" ref="scrollRevealRef">
      <div class="sticky-container">
        <div class="reveal-block" :style="revealTopStyle">
          <h2 class="reveal-big-text reveal-by-char" :aria-label="topLineText">
            <span
              v-for="(ch, i) in topLineChars"
              :key="`top-${i}`"
              class="reveal-char"
              :style="charStyle(i, topLineChars.length, revealProgress)"
            >
              {{ ch === ' ' ? '\u00A0' : ch }}
            </span>
          </h2>
        </div>
      </div>
    </section>

    <section class="scroll-box scroll-box-2" ref="scrollRevealRef2">
      <div class="sticky-container">
        <div class="reveal-block" :style="revealBottomStyle">
          <h2 class="reveal-big-text reveal-by-char" :aria-label="bottomLineText">
            <span
              v-for="(ch, i) in bottomLineChars"
              :key="`bottom-${i}`"
              class="reveal-char"
              :style="charStyle(i, bottomLineChars.length, revealProgress2)"
            >
              {{ ch === ' ' ? '\u00A0' : ch }}
            </span>
          </h2>
        </div>
      </div>
    </section>

    <section class="inspiration-wall" ref="galleryRef">
      <div class="wall-header" :class="{ show: galleryShow }">
        <span class="tag">DISCOVER</span>
        <h2>社团灵感墙</h2>
      </div>
      <div class="cosmos-grid" :class="{ show: galleryShow }">
        <div v-for="(img, i) in galleryImages" :key="i" class="grid-card" :class="img.size">
          <div class="img-wrapper">
            <img :src="img.url" :alt="img.label" loading="lazy" />
          </div>
          <div class="card-info">
            <span class="card-tag">#{{ img.category }}</span>
            <span class="card-label">{{ img.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="stats-section" ref="highlightRef">
      <div class="stats-grid" :class="{ show: highlightShow }">
        <div class="stat-item">
          <span class="stat-value">50+</span>
          <span class="stat-desc">活跃社团</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">3K+</span>
          <span class="stat-desc">注册成员</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">200+</span>
          <span class="stat-desc">年度活动</span>
        </div>
      </div>
    </section>

    <section class="footer-cta" ref="ctaRef">
      <div class="cta-content" :class="{ show: ctaShow }">
        <h2>下一段校园经历，就从今天开始</h2>
        <button class="btn-outline" @click="handleEnterHome">进入广场</button>
      </div>
    </section>

    <footer class="main-footer">
      <div class="footer-bottom">
        <span>© 2026 校园社团管理系统</span>
        <div class="footer-links">
          <a>关于我们</a>
          <a>服务协议</a>
          <a>隐私政策</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const ctaRef = ref(null)
const highlightRef = ref(null)
const galleryRef = ref(null)
const scrollRevealRef = ref(null)
const scrollRevealRef2 = ref(null)

const ctaShow = ref(false)
const highlightShow = ref(false)
const galleryShow = ref(false)
const isScrolled = ref(false)
const revealProgress = ref(0)
const revealProgress2 = ref(0)

let obs = null

const handleEnterHome = () => {
  if (authStore.token) {
    router.push('/home')
  } else {
    router.push('/login')
  }
}

const floatingImages = [
  { url: 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=360&q=80', top: '10%', left: '3%', width: '170px', delay: '0s', zIndex: 1, label: '社团演出' },
  { url: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=360&q=80', top: '11%', left: '79%', width: '210px', delay: '0.15s', zIndex: 2, label: '技术协作' },
  { url: 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=360&q=80', top: '71%', left: '7%', width: '184px', delay: '0.3s', zIndex: 1, label: '社团运动' },
  { url: 'https://images.unsplash.com/photo-1506466010722-395aa2bef877?w=360&q=80', top: '74%', left: '82%', width: '176px', delay: '0.45s', zIndex: 2, label: '阅读交流' },
  { url: 'https://images.unsplash.com/photo-1559027615-cd4628902d4a?w=360&q=80', top: '39%', left: '88%', width: '160px', delay: '0.6s', zIndex: 1, label: '志愿服务' },
  { url: 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?w=360&q=80', top: '4%', left: '44%', width: '142px', delay: '0.75s', zIndex: 1, label: '校园乐队' },
  { url: 'https://images.unsplash.com/photo-1551818255-e6e10975bc17?w=360&q=80', top: '80%', left: '43%', width: '205px', delay: '0.9s', zIndex: 1, label: '辩论赛' },
  { url: 'https://images.unsplash.com/photo-1571260899304-425eee4c7efc?w=360&q=80', top: '46%', left: '2%', width: '162px', delay: '1.05s', zIndex: 2, label: '街舞排练' },
  { url: 'https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=360&q=80', top: '22%', left: '1%', width: '136px', delay: '1.2s', zIndex: 1, label: '课堂互动' },
  { url: 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=360&q=80', top: '19%', left: '88%', width: '128px', delay: '1.35s', zIndex: 1, label: '小组协作' },
  { url: 'https://images.unsplash.com/photo-1491438590914-bc09fcaaf77a?w=360&q=80', top: '31%', left: '11%', width: '122px', delay: '1.5s', zIndex: 1, label: '活动记录' },
  { url: 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=360&q=80', top: '29%', left: '76%', width: '138px', delay: '1.65s', zIndex: 1, label: '阅读沙龙' },
  { url: 'https://images.unsplash.com/photo-1460518451285-97b6aa326961?w=360&q=80', top: '58%', left: '86%', width: '134px', delay: '1.8s', zIndex: 1, label: '夜间活动' },
  { url: 'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?w=360&q=80', top: '60%', left: '3%', width: '124px', delay: '1.95s', zIndex: 1, label: '讨论会' },
  { url: 'https://images.unsplash.com/photo-1489515217757-5fd1be406fef?w=360&q=80', top: '12%', left: '20%', width: '118px', delay: '2.1s', zIndex: 1, label: '创意工坊' },
  { url: 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=360&q=80', top: '64%', left: '74%', width: '130px', delay: '2.25s', zIndex: 1, label: '社团合影' }
]

const galleryImages = [
  { url: 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=900&q=80', label: '草坪音乐节', category: '艺术', size: 'tall' },
  { url: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=900&q=80', label: '代码马拉松', category: '科技', size: '' },
  { url: 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=900&q=80', label: '瑜伽社团日', category: '运动', size: 'wide' },
  { url: 'https://images.unsplash.com/photo-1506466010722-395aa2bef877?w=900&q=80', label: '读书分享夜', category: '文化', size: '' },
  { url: 'https://images.unsplash.com/photo-1559027615-cd4628902d4a?w=900&q=80', label: '青年志愿行', category: '公益', size: 'tall' },
  { url: 'https://images.unsplash.com/photo-1571260899304-425eee4c7efc?w=900&q=80', label: '街舞联排', category: '艺术', size: '' },
  { url: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=900&q=80', label: '团队共创坊', category: '创新', size: 'wide' },
  { url: 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?w=900&q=80', label: '校园乐团', category: '艺术', size: '' }
]

const topLineText = '在这里，热爱会被看见，也会被连接'
const bottomLineText = '探索无限可能，遇见更好的自己'

const topLineChars = computed(() => Array.from(topLineText))
const bottomLineChars = computed(() => Array.from(bottomLineText))

const clamp01 = (v) => Math.min(1, Math.max(0, v))

const charStyle = (index, len, progress) => {
  const p = clamp01(progress)
  const usable = 0.88
  const start = (index / Math.max(1, len - 1)) * usable
  const span = Math.max(0.03, Math.min(0.06, 0.095 * (18 / Math.max(18, len))))
  const t = clamp01((p - start) / span)

  return {
    color: `rgba(14, 27, 42, ${t})`,
    filter: `blur(${(1 - t) * 12}px)`,
    transform: `translateY(${(1 - t) * 0.24}em)`
  }
}

const revealTopStyle = computed(() => ({
  opacity: 1,
  transform: `translateY(${(1 - revealProgress.value) * 16}px)`
}))

const revealBottomStyle = computed(() => ({
  opacity: 1,
  transform: `translateY(${(1 - revealProgress2.value) * 16}px)`
}))

const handleScroll = () => {
  isScrolled.value = window.scrollY > 40

  if (scrollRevealRef.value) {
    const rect = scrollRevealRef.value.getBoundingClientRect()
    revealProgress.value = clamp01((window.innerHeight - rect.top) / window.innerHeight)
  }

  if (scrollRevealRef2.value) {
    const rect = scrollRevealRef2.value.getBoundingClientRect()
    revealProgress2.value = clamp01((window.innerHeight - rect.top) / window.innerHeight)
  }
}

onMounted(() => {
  obs = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.target === ctaRef.value && entry.isIntersecting) ctaShow.value = true
      if (entry.target === highlightRef.value && entry.isIntersecting) highlightShow.value = true
      if (entry.target === galleryRef.value && entry.isIntersecting) galleryShow.value = true
    })
  }, { threshold: 0.12 })

  if (ctaRef.value) obs.observe(ctaRef.value)
  if (highlightRef.value) obs.observe(highlightRef.value)
  if (galleryRef.value) obs.observe(galleryRef.value)

  window.addEventListener('scroll', handleScroll)
  handleScroll()
})

onUnmounted(() => {
  obs?.disconnect()
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;700&display=swap');

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.landing-page {
  --ink: #0e1b2a;
  --muted: #4e6073;
  --bg: #f3efe6;
  --surface: rgba(255, 255, 255, 0.76);
  --border: rgba(14, 27, 42, 0.12);
  --teal: #0f766e;
  --orange: #c2410c;
  --hero-shadow: 0 28px 64px rgba(14, 27, 42, 0.16);

  position: relative;
  min-height: 100vh;
  overflow-x: clip;
  font-family: 'Outfit', sans-serif;
  color: var(--ink);
  background: radial-gradient(circle at 10% 8%, rgba(15, 118, 110, 0.14), transparent 35%),
    radial-gradient(circle at 90% 10%, rgba(194, 65, 12, 0.15), transparent 42%),
    linear-gradient(180deg, #f9f5ec 0%, var(--bg) 52%, #ede6d9 100%);
}

.landing-page::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(rgba(14, 27, 42, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(14, 27, 42, 0.03) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: radial-gradient(circle at 50% 18%, #000 28%, transparent 74%);
  z-index: 0;
}

.top-nav {
  position: fixed;
  top: 14px;
  left: 16px;
  z-index: 1000;
  display: inline-flex;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: 999px;
  transition: all 0.34s cubic-bezier(0.16, 1, 0.3, 1);
}

.top-nav.scrolled {
  border-color: var(--border);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px);
  box-shadow: 0 10px 22px rgba(14, 27, 42, 0.09);
}

.nav-actions {
  position: fixed;
  top: 14px;
  right: 16px;
  z-index: 1000;
  display: inline-flex;
  gap: 10px;
  padding: 6px;
  border: 1px solid transparent;
  border-radius: 999px;
  transition: all 0.34s cubic-bezier(0.16, 1, 0.3, 1);
}

.nav-actions.scrolled {
  border-color: var(--border);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px);
  box-shadow: 0 10px 22px rgba(14, 27, 42, 0.09);
}

.nav-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 0.98rem;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  font-weight: 700;
}

.logo-mark {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(145deg, var(--teal), #115e59);
  box-shadow: 0 8px 16px rgba(15, 118, 110, 0.35);
}

.nav-actions button {
  border-radius: 999px;
  padding: 10px 18px;
  border: 1px solid transparent;
  font-family: inherit;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.24s ease;
}

.btn-ghost {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.72);
  border-color: var(--border);
}

.btn-ghost:hover {
  border-color: rgba(15, 118, 110, 0.4);
  color: var(--teal);
}

.btn-solid {
  color: #fff;
  border-color: #0f5f59;
  background: linear-gradient(135deg, #0f766e 0%, #115e59 100%);
}

.btn-solid:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 20px rgba(15, 118, 110, 0.3);
}

.hero-section {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 150px 24px 110px;
  overflow: hidden;
}

.hero-noise {
  position: absolute;
  inset: 0;
  z-index: -1;
  opacity: 0.55;
  background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.36) 0%, transparent 70%);
}

.floating-bg {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}

.float-img {
  position: absolute;
  opacity: 0;
  transform: translateY(30px) scale(0.9);
  animation: floatIn 1s cubic-bezier(0.16, 1, 0.3, 1) forwards, drift 9s ease-in-out infinite;
}

.float-img img {
  width: 100%;
  display: block;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: var(--hero-shadow);
  filter: saturate(0.86) contrast(1.04);
}

@keyframes floatIn {
  to {
    opacity: 0.9;
    transform: translateY(0) scale(1);
  }
}

@keyframes drift {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.hero-center {
  width: min(920px, 100%);
  text-align: center;
  position: relative;
  z-index: 2;
}

.hero-tag {
  margin-bottom: 18px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-weight: 600;
  color: var(--muted);
  font-size: 0.8rem;
}

.hero-center h1 {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.5rem, 6.4vw, 5rem);
  line-height: 1.13;
  font-weight: 700;
}

.hero-sub {
  margin: 24px auto 34px;
  max-width: 560px;
  font-size: clamp(1rem, 2vw, 1.18rem);
  color: var(--muted);
}

.hero-actions {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 12px;
}

.btn-main,
.btn-outline {
  border: 0;
  border-radius: 999px;
  padding: 13px 28px;
  font-family: inherit;
  font-size: 0.96rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s ease;
}

.btn-main {
  color: #fff;
  background: linear-gradient(135deg, var(--orange) 0%, #9a3412 100%);
  box-shadow: 0 12px 24px rgba(194, 65, 12, 0.3);
}

.btn-main:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 28px rgba(194, 65, 12, 0.36);
}

.reveal-container {
  overflow: hidden;
}

.reveal-text {
  display: block;
  transform: translateY(110%);
  animation: revealUp 0.9s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes revealUp {
  to {
    transform: translateY(0);
  }
}

.delay-1 {
  animation-delay: 0.12s;
}

.delay-2 {
  animation-delay: 0.22s;
}

.delay-3 {
  animation-delay: 0.36s;
}

.delay-4 {
  animation-delay: 0.48s;
}

.scroll-box {
  position: relative;
  height: 95vh;
}

.scroll-box-1 {
  background: linear-gradient(180deg, #dce9ef 0%, #e8ece2 100%);
}

.scroll-box-2 {
  background: linear-gradient(180deg, #efe3d6 0%, #e4ebea 100%);
}

.sticky-container {
  position: sticky;
  top: 0;
  height: 100vh;
  display: grid;
  place-items: center;
  padding: 84px 28px;
}

.reveal-block {
  width: min(980px, 100%);
  text-align: center;
}

.reveal-big-text {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.8rem, 5vw, 3.7rem);
  line-height: 1.3;
  font-weight: 700;
}

.reveal-by-char {
  display: inline-block;
}

.reveal-char {
  display: inline-block;
  color: rgba(14, 27, 42, 0.2);
  transform: translateY(0.24em);
  filter: blur(12px);
  transition: color 10ms linear, filter 10ms linear, transform 10ms linear;
  will-change: transform, filter, color;
}

.inspiration-wall {
  padding: 112px 28px;
  background: linear-gradient(180deg, #f6efe2 0%, #f1e7d7 100%);
}

.wall-header {
  width: min(1100px, 100%);
  margin: 0 auto 42px;
  opacity: 0;
  transform: translateY(24px);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.wall-header.show {
  opacity: 1;
  transform: translateY(0);
}

.tag {
  display: inline-block;
  margin-bottom: 12px;
  color: var(--muted);
  letter-spacing: 0.18em;
  font-size: 0.75rem;
  font-weight: 700;
}

.wall-header h2 {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 4vw, 3rem);
}

.cosmos-grid {
  width: min(1100px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  grid-auto-rows: 200px;
  gap: 18px;
  opacity: 0;
  transform: translateY(34px);
  transition: all 0.9s ease;
}

.cosmos-grid.show {
  opacity: 1;
  transform: translateY(0);
}

.grid-card {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 12px 24px rgba(14, 27, 42, 0.12);
}

.grid-card.tall {
  grid-row: span 2;
}

.grid-card.wide {
  grid-column: span 2;
}

.img-wrapper,
.img-wrapper img {
  width: 100%;
  height: 100%;
}

.img-wrapper img {
  object-fit: cover;
  transition: transform 0.45s ease, filter 0.45s ease;
  filter: saturate(0.88) contrast(1.02);
}

.grid-card:hover .img-wrapper img {
  transform: scale(1.07);
  filter: saturate(1.03) contrast(1.03);
}

.card-info {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 18px 18px 16px;
  background: linear-gradient(180deg, transparent 0%, rgba(14, 27, 42, 0.72) 78%);
  color: #f9f8f4;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.28s ease;
}

.grid-card:hover .card-info {
  opacity: 1;
  transform: translateY(0);
}

.card-tag {
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  opacity: 0.86;
}

.card-label {
  font-size: 1rem;
  font-weight: 600;
}

.stats-section {
  padding: 110px 28px;
  background: linear-gradient(180deg, #ece3d5 0%, #dde8e5 100%);
}

.stats-grid {
  width: min(1100px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.8s ease;
}

.stats-grid.show {
  opacity: 1;
  transform: translateY(0);
}

.stat-item {
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background: var(--surface);
  backdrop-filter: blur(8px);
  text-align: center;
  padding: 34px 20px;
}

.stat-value {
  display: block;
  margin-bottom: 8px;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.4rem, 4vw, 3.2rem);
  color: #124f4b;
}

.stat-desc {
  color: var(--muted);
  letter-spacing: 0.08em;
  font-size: 0.82rem;
  text-transform: uppercase;
}

.footer-cta {
  padding: 130px 28px;
  text-align: center;
  background: linear-gradient(165deg, #112433 0%, #132a2e 60%, #143733 100%);
}

.cta-content {
  width: min(760px, 100%);
  margin: 0 auto;
  padding: 44px 30px;
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 30px 48px rgba(0, 0, 0, 0.28);
  opacity: 0;
  transform: translateY(24px);
  transition: all 0.9s cubic-bezier(0.16, 1, 0.3, 1);
}

.cta-content.show {
  opacity: 1;
  transform: translateY(0);
}

.cta-content h2 {
  color: #f6f8f5;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.8rem, 4vw, 2.8rem);
  margin-bottom: 24px;
}

.btn-outline {
  color: #f6f8f5;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: transparent;
}

.btn-outline:hover {
  color: #112433;
  background: #f6f8f5;
}

.main-footer {
  padding: 26px 28px 36px;
  background: #112433;
}

.footer-bottom {
  width: min(1100px, 100%);
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: rgba(246, 248, 245, 0.75);
  font-size: 0.82rem;
}

.footer-links {
  display: inline-flex;
  gap: 20px;
}

.footer-links a {
  cursor: pointer;
  transition: color 0.2s ease;
}

.footer-links a:hover {
  color: #fff;
}

@media (max-width: 992px) {
  .float-img {
    opacity: 0.5 !important;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .top-nav {
    top: 10px;
    left: 10px;
    padding: 10px;
  }

  .nav-actions {
    top: 10px;
    right: 10px;
    padding: 4px;
  }

  .logo-text {
    font-size: 0.78rem;
    letter-spacing: 0.05em;
  }

  .nav-actions button {
    padding: 9px 14px;
    font-size: 0.82rem;
  }

  .hero-section {
    padding: 126px 18px 88px;
  }

  .float-img {
    display: none;
  }

  .hero-actions {
    width: 100%;
    justify-content: center;
  }

  .btn-main,
  .btn-outline {
    width: min(280px, 100%);
  }

  .scroll-box {
    height: 82vh;
  }

  .inspiration-wall,
  .stats-section,
  .footer-cta {
    padding-left: 18px;
    padding-right: 18px;
  }

  .cosmos-grid {
    grid-template-columns: 1fr 1fr;
    grid-auto-rows: 168px;
  }

  .grid-card.wide {
    grid-column: span 1;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .float-img,
  .reveal-text {
    animation: none !important;
    transform: none !important;
  }

  .reveal-char {
    transition: none !important;
    transform: none !important;
    filter: none !important;
    color: rgba(14, 27, 42, 1) !important;
  }
}
</style>
