<template>
  <div class="landing-page">
    <div class="page-ambient ambient-left" aria-hidden="true"></div>
    <div class="page-ambient ambient-right" aria-hidden="true"></div>
    <div class="page-grid" aria-hidden="true"></div>

    <header class="header-shell" :class="{ scrolled: isScrolled }">
      <nav class="top-nav" aria-label="首页导航">
        <div class="nav-logo">
          <span class="logo-mark">◍</span>
        </div>
      </nav>
      <div class="nav-actions">
        <button data-testid="landing-login" class="btn-ghost" @click="router.push('/login')">登录</button>
        <button data-testid="landing-register" class="btn-solid" @click="router.push('/register')">加入我们</button>
      </div>
    </header>

    <main class="landing-main">
      <section class="hero-section" data-testid="landing-hero">
        <div class="hero-paper" aria-hidden="true"></div>
        <div class="hero-orbit orbit-one" aria-hidden="true"></div>
        <div class="hero-orbit orbit-two" aria-hidden="true"></div>
        <div class="hero-glow" aria-hidden="true"></div>
        <div class="hero-noise" aria-hidden="true"></div>
        <div class="floating-bg" aria-hidden="true">
          <div
            v-for="(img, i) in floatingImages"
            :key="i"
            :class="['float-img']"
            :style="{
              top: img.top,
              left: img.left,
              animationDelay: img.delay,
              zIndex: img.zIndex,
              '--float-width': img.width,
              '--float-rotate': img.rotate
            }"
          >
            <div class="float-card">
              <img :src="img.url" :alt="img.label" />
            </div>
          </div>
        </div>
        <div class="hero-center">
          <div class="hero-copy">
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
              <button data-testid="landing-hero-register" class="btn-main" @click="router.push('/register')">立即注册</button>
            </div>
          </div>
        </div>
      </section>

      <section class="scroll-box scroll-box-1" ref="scrollRevealRef">
        <div class="sticky-container">
          <div class="editorial-frame">
            <aside class="section-aside" aria-hidden="true">
              <span class="section-label">{{ sectionMeta[0].label }}</span>
            </aside>
            <div class="reveal-block" :style="revealTopStyle">
              <p class="section-note">{{ sectionMeta[0].note }}</p>
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
        </div>
      </section>

      <section class="scroll-box scroll-box-2" ref="scrollRevealRef2">
        <div class="sticky-container">
          <div class="editorial-frame editorial-frame-alt">
            <aside class="section-aside" aria-hidden="true">
              <span class="section-label">{{ sectionMeta[1].label }}</span>
            </aside>
            <div class="reveal-block" :style="revealBottomStyle">
              <p class="section-note">{{ sectionMeta[1].note }}</p>
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
        </div>
      </section>

      <section class="inspiration-wall" ref="galleryRef" data-testid="landing-gallery">
        <div class="section-shell">
          <div class="editorial-rail" aria-hidden="true">
            <span>Discover</span>
          </div>
          <div class="wall-header" :class="{ show: galleryShow }">
            <h2>社团灵感墙</h2>
            <p>从音乐节到黑客松，校园里的每一种热爱都能找到同伴与舞台。</p>
          </div>
          <div class="cosmos-grid" :class="{ show: galleryShow }">
            <article v-for="(img, i) in galleryImages" :key="i" class="grid-card" :class="img.size">
              <div class="img-wrapper">
                <img :src="img.url" :alt="img.label" loading="lazy" />
              </div>
              <div class="card-info">
                <span class="card-tag">#{{ img.category }}</span>
                <span class="card-label">{{ img.label }}</span>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="stats-section" ref="highlightRef" data-testid="landing-stats">
        <div class="section-shell stats-shell">
          <div class="editorial-rail" aria-hidden="true">
            <span>Impact</span>
          </div>
          <div class="stats-header" :class="{ show: highlightShow }">
            <h2>把参与、连接与成长，转化成看得见的校园轨迹</h2>
          </div>
          <div class="stats-grid" :class="{ show: highlightShow }">
            <article v-for="item in heroMetrics" :key="item.label" class="stat-item">
              <span class="stat-value">{{ item.value }}</span>
              <span class="stat-desc">{{ item.label }}</span>
              <p class="stat-note">{{ item.note }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="footer-cta" ref="ctaRef" data-testid="landing-cta">
        <div class="cta-spotlight" aria-hidden="true"></div>
        <div class="cta-content" :class="{ show: ctaShow }">
          <span class="tag tag-inverse">BEGIN ORBIT</span>
          <h2>下一段校园经历，就从今天开始</h2>
          <p>登录后进入社团广场，查看公告、活动与社团动态。</p>
          <button data-testid="landing-enter" class="btn-outline" @click="handleEnterHome">进入广场</button>
        </div>
      </section>
    </main>

    <footer class="main-footer">
      <div class="footer-bottom">
        <div class="footer-brand">
          <span class="logo-mark footer-logo">◍</span>
          <span>© 2026 校园社团管理系统</span>
        </div>
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

const heroMetrics = [
  { value: '50+', label: '活跃社团', note: '覆盖艺术、科技、公益与运动等多元方向。' },
  { value: '3K+', label: '注册成员', note: '持续连接新生、骨干与跨社团协作伙伴。' },
  { value: '200+', label: '年度活动', note: '把灵感转化为真实参与和长期成长记录。' }
]

const sectionMeta = [
  { label: 'Connect', note: '从兴趣出发，让热爱先被看见，再被连接。' },
  { label: 'Become', note: '在活动、协作与创作里，逐步成为更好的自己。' }
]

const handleEnterHome = () => {
  if (authStore.token) {
    router.push('/home')
  } else {
    router.push('/login')
  }
}

const floatingImages = [
  { url: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=360&q=80', top: '11%', left: '4%', width: '172px', delay: '0s', zIndex: 1, label: '草坪音乐节', tier: 'mid', rotate: '-7deg' },
  { url: 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=360&q=80', top: '11%', left: '78%', width: '214px', delay: '0.15s', zIndex: 3, label: '代码马拉松', tier: 'front', rotate: '7deg' },
  { url: 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=360&q=80', top: '72%', left: '8%', width: '186px', delay: '0.3s', zIndex: 2, label: '新生篮球赛', tier: 'front', rotate: '-6deg' },
  { url: 'https://images.unsplash.com/photo-1452780212940-6f5c0d14d848?w=360&q=80', top: '75%', left: '80%', width: '178px', delay: '0.45s', zIndex: 2, label: '摄影外拍', tier: 'mid', rotate: '6deg' },
  { url: 'https://images.unsplash.com/photo-1593113598332-cd288d649433?w=360&q=80', top: '38%', left: '87%', width: '162px', delay: '0.6s', zIndex: 1, label: '青年志愿行', tier: 'far', rotate: '4deg' },
  { url: 'https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?w=360&q=80', top: '4%', left: '45%', width: '144px', delay: '0.75s', zIndex: 1, label: '汉服雅集', tier: 'far', rotate: '-4deg' },
  { url: 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=360&q=80', top: '81%', left: '43%', width: '208px', delay: '0.9s', zIndex: 1, label: '电竞联赛', tier: 'mid', rotate: '2deg' },
  { url: 'https://images.unsplash.com/photo-1535525153412-5a42439a210d?w=360&q=80', top: '46%', left: '2%', width: '164px', delay: '1.05s', zIndex: 2, label: '街舞联排', tier: 'front', rotate: '-5deg' },
  { url: 'https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=360&q=80', top: '22%', left: '1%', width: '136px', delay: '1.2s', zIndex: 1, label: '实验室开放日', tier: 'far', rotate: '-3deg' },
  { url: 'https://images.unsplash.com/photo-1556075798-4825dfaaf498?w=360&q=80', top: '20%', left: '88%', width: '130px', delay: '1.35s', zIndex: 1, label: '算法竞赛宣讲会', tier: 'far', rotate: '5deg' },
  { url: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=360&q=80', top: '31%', left: '12%', width: '122px', delay: '1.5s', zIndex: 1, label: '简历门诊', tier: 'far', rotate: '-2deg' },
  { url: 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=360&q=80', top: '29%', left: '75%', width: '140px', delay: '1.65s', zIndex: 1, label: '急救技能培训', tier: 'mid', rotate: '5deg' },
  { url: 'https://images.unsplash.com/photo-1571008887538-b36bb32f4571?w=360&q=80', top: '59%', left: '86%', width: '136px', delay: '1.8s', zIndex: 1, label: '校园迷你马拉松', tier: 'far', rotate: '3deg' },
  { url: 'https://images.unsplash.com/photo-1547447134-cd3f5c716030?w=360&q=80', top: '60%', left: '3%', width: '126px', delay: '1.95s', zIndex: 1, label: '滑板刷街活动', tier: 'far', rotate: '-4deg' },
  { url: 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=360&q=80', top: '12%', left: '20%', width: '120px', delay: '2.1s', zIndex: 1, label: '迎新晚会', tier: 'far', rotate: '-6deg' },
  { url: 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=360&q=80', top: '64%', left: '73%', width: '132px', delay: '2.25s', zIndex: 1, label: '校园十佳歌手', tier: 'mid', rotate: '4deg' }
]

const galleryImages = [
  { url: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=900&q=80', label: '草坪音乐节', category: '艺术', size: 'tall' },
  { url: 'https://images.unsplash.com/photo-1556075798-4825dfaaf498?w=900&q=80', label: '算法竞赛宣讲会', category: '科技', size: '' },
  { url: 'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=900&q=80', label: '羽毛球联谊赛', category: '运动', size: 'wide' },
  { url: 'https://images.unsplash.com/photo-1452780212940-6f5c0d14d848?w=900&q=80', label: '摄影外拍活动', category: '文化', size: '' },
  { url: 'https://images.unsplash.com/photo-1593113598332-cd288d649433?w=900&q=80', label: '青年志愿行', category: '公益', size: 'tall' },
  { url: 'https://images.unsplash.com/photo-1535525153412-5a42439a210d?w=900&q=80', label: '街舞联排', category: '艺术', size: '' },
  { url: 'https://images.unsplash.com/photo-1559136555-9303baea8ebd?w=900&q=80', label: '创业计划路演', category: '创新', size: 'wide' },
  { url: 'https://images.unsplash.com/photo-1529107386315-e1a2ed48a620?w=900&q=80', label: '模拟联合国会议', category: '实践', size: '' }
]

const topLineText = '在这里，热爱会被看见，也会被连接'
const bottomLineText = '探索无限可能，遇见更好的自己'

const topLineChars = computed(() => Array.from(topLineText))
const bottomLineChars = computed(() => Array.from(bottomLineText))

const clamp01 = (value) => Math.min(1, Math.max(0, value))
const smoothstep = (value) => value * value * (3 - 2 * value)

const charStyle = (index, len, progress) => {
  const p = clamp01(progress)
  const usable = 0.82
  const start = (index / Math.max(1, len - 1)) * usable
  const span = Math.max(0.055, Math.min(0.09, 0.14 * (18 / Math.max(18, len))))
  const t = smoothstep(clamp01((p - start) / span))
  const alpha = 0.22 + t * 0.78

  return {
    color: `rgba(14, 27, 42, ${alpha})`,
    filter: `blur(${(1 - t) * 6}px)`,
    transform: `translateY(${(1 - t) * 0.14}em)`
  }
}

const revealTopStyle = computed(() => ({
  opacity: 0.72 + revealProgress.value * 0.28,
  transform: `translateY(${(1 - revealProgress.value) * 12}px)`
}))

const revealBottomStyle = computed(() => ({
  opacity: 0.72 + revealProgress2.value * 0.28,
  transform: `translateY(${(1 - revealProgress2.value) * 12}px)`
}))

const handleScroll = () => {
  isScrolled.value = window.scrollY > 24

  if (scrollRevealRef.value) {
    const rect = scrollRevealRef.value.getBoundingClientRect()
    revealProgress.value = clamp01((window.innerHeight - rect.top) / (window.innerHeight * 0.9))
  }

  if (scrollRevealRef2.value) {
    const rect = scrollRevealRef2.value.getBoundingClientRect()
    revealProgress2.value = clamp01((window.innerHeight - rect.top) / (window.innerHeight * 0.9))
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

  window.addEventListener('scroll', handleScroll, { passive: true })
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
  --ink: #102033;
  --headline: #17304a;
  --muted: #627285;
  --paper: #f4eee3;
  --surface: rgba(255, 255, 255, 0.76);
  --surface-strong: rgba(255, 255, 255, 0.88);
  --border: rgba(16, 32, 51, 0.12);
  --teal: #0f766e;
  --teal-deep: #115e59;
  --orange: #c2410c;
  --hero-shadow: 0 28px 64px rgba(16, 32, 51, 0.18);
  --shadow-soft: 0 12px 24px rgba(16, 32, 51, 0.08);
  --shadow-lg: 0 30px 70px rgba(16, 32, 51, 0.16);

  position: relative;
  min-height: 100vh;
  overflow-x: clip;
  font-family: 'Outfit', sans-serif;
  color: var(--ink);
  background:
    radial-gradient(circle at 10% 12%, rgba(15, 118, 110, 0.12), transparent 34%),
    radial-gradient(circle at 92% 14%, rgba(194, 65, 12, 0.14), transparent 38%),
    linear-gradient(180deg, #faf5ea 0%, var(--paper) 48%, #ece5d9 100%);
}

.landing-page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    linear-gradient(rgba(16, 32, 51, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16, 32, 51, 0.03) 1px, transparent 1px);
  background-size: 34px 34px;
  mask-image: radial-gradient(circle at 50% 15%, #000 28%, transparent 74%);
}

.page-ambient {
  position: fixed;
  z-index: 0;
  border-radius: 999px;
  filter: blur(68px);
  pointer-events: none;
}

.ambient-left {
  top: -100px;
  left: -40px;
  width: 320px;
  height: 320px;
  background: rgba(15, 118, 110, 0.16);
}

.ambient-right {
  right: -60px;
  bottom: 8%;
  width: 300px;
  height: 300px;
  background: rgba(194, 65, 12, 0.15);
}

.page-grid {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.45;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.34), transparent 24%),
    radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.22), transparent 28%);
}

.landing-main,
.main-footer {
  position: relative;
  z-index: 2;
}

.header-shell {
  position: fixed;
  top: 14px;
  left: 50%;
  z-index: 5;
  transform: translateX(-50%);
  width: min(1180px, calc(100% - 24px));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 14px;
  border: 1px solid transparent;
  border-radius: 999px;
  transition: all 0.34s cubic-bezier(0.16, 1, 0.3, 1);
}

.header-shell.scrolled {
  border-color: var(--border);
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(18px);
  box-shadow: 0 12px 26px rgba(16, 32, 51, 0.1);
}

.top-nav,
.nav-actions {
  display: flex;
  align-items: center;
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
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(145deg, var(--teal), var(--teal-deep));
  box-shadow: 0 8px 16px rgba(15, 118, 110, 0.32);
}

.nav-actions {
  gap: 10px;
}

.nav-actions button,
.btn-main,
.btn-outline {
  border-radius: 999px;
  padding: 12px 20px;
  font-family: inherit;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, background 0.24s ease, border-color 0.24s ease, color 0.24s ease;
}

.nav-actions button {
  border: 1px solid transparent;
}

.btn-ghost {
  color: var(--ink);
  background: rgba(255, 255, 255, 0.72);
  border-color: var(--border);
}

.btn-ghost:hover {
  color: var(--teal);
  border-color: rgba(15, 118, 110, 0.36);
  transform: translateY(-1px);
}

.btn-solid {
  color: #fff;
  border-color: var(--teal-deep);
  background: linear-gradient(135deg, var(--teal) 0%, var(--teal-deep) 100%);
  box-shadow: 0 12px 20px rgba(15, 118, 110, 0.22);
}

.btn-solid:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 24px rgba(15, 118, 110, 0.28);
}

.hero-section {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 126px 24px 94px;
  overflow: hidden;
}

.hero-paper {
  position: absolute;
  inset: 34px 22px 20px;
  z-index: -2;
  margin: auto;
  width: min(1240px, 100%);
  max-width: calc(100% - 44px);
  border: 0;
  border-radius: 0;
  background: none;
  box-shadow: none;
  pointer-events: none;
}

.hero-paper::before,
.hero-paper::after {
  content: '';
  position: absolute;
  inset: 18px;
  border-radius: 24px;
  pointer-events: none;
}

.hero-paper::before {
  display: none;
}

.hero-paper::after {
  display: none;
}

.hero-orbit,
.hero-glow,
.hero-noise {
  position: absolute;
  pointer-events: none;
}

.hero-orbit {
  inset: 50%;
  z-index: -1;
  border-radius: 50%;
  border: 1px solid rgba(16, 32, 51, 0.08);
  transform: translate(-50%, -50%);
}

.orbit-one {
  width: min(1100px, 86vw);
  height: min(720px, 64vw);
}

.orbit-two {
  width: min(920px, 72vw);
  height: min(560px, 50vw);
  transform: translate(-50%, -50%) rotate(-12deg);
}

.hero-glow {
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.48) 0%, transparent 42%),
    radial-gradient(circle at 50% 54%, rgba(15, 118, 110, 0.07) 0%, transparent 40%),
    radial-gradient(circle at 50% 40%, rgba(194, 65, 12, 0.08) 0%, transparent 32%);
}

.hero-noise {
  inset: 0;
  opacity: 0.55;
  background:
    radial-gradient(circle at 50% 42%, rgba(255, 255, 255, 0.44) 0%, transparent 52%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), transparent 42%);
}

.floating-bg {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}

.float-img {
  position: absolute;
  --float-opacity: 0.9;
  width: var(--float-width);
  opacity: 0;
  transform: translateY(34px) rotate(var(--float-rotate)) scale(0.92);
  animation:
    floatIn 0.95s cubic-bezier(0.16, 1, 0.3, 1) forwards,
    drift 10s ease-in-out infinite;
}

.float-card {
  position: relative;
  overflow: hidden;
  border-radius: 0;
  border: 0;
  box-shadow: none;
  background: transparent;
}

.float-card::after {
  display: none;
}

.float-img img {
  width: 100%;
  display: block;
  aspect-ratio: 4 / 5;
  object-fit: cover;
  filter: saturate(0.88) contrast(1.04);
}

.float-label {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 12px;
  z-index: 1;
  padding: 0;
  border-radius: 0;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: rgba(250, 248, 241, 0.95);
  background: transparent;
  backdrop-filter: none;
  text-shadow: 0 1px 2px rgba(16, 32, 51, 0.65);
  text-align: center;
}

.tier-far {
  --float-opacity: 0.56;
}

.tier-far .float-card {
  box-shadow: none;
}

.tier-far img {
  filter: saturate(0.72) contrast(1.01);
}

.tier-mid {
  --float-opacity: 0.76;
}

.tier-front {
  --float-opacity: 0.9;
}

@keyframes floatIn {
  to {
    opacity: var(--float-opacity);
    transform: translateY(0) rotate(var(--float-rotate)) scale(1);
  }
}

@keyframes drift {
  0%,
  100% {
    transform: translateY(0) rotate(var(--float-rotate));
  }
  50% {
    transform: translateY(-10px) rotate(var(--float-rotate));
  }
}

.hero-center {
  width: min(1160px, 100%);
  display: flex;
  justify-content: center;
  padding: 0 16px;
}

.hero-copy {
  width: min(760px, 100%);
  padding: 24px clamp(8px, 2vw, 18px) 10px;
  border: 0;
  border-radius: 0;
  background: transparent;
  backdrop-filter: none;
  text-align: center;
  box-shadow: none;
}

.hero-meta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-meta span + span::before {
  content: '/';
  margin-right: 10px;
}

.hero-tag {
  margin-bottom: 18px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-weight: 600;
  color: var(--muted);
  font-size: 0.8rem;
}

.hero-copy h1,
.reveal-big-text,
.wall-header h2,
.stats-header h2,
.cta-content h2 {
  font-family: 'Noto Serif SC', serif;
}

.hero-copy h1 {
  font-size: clamp(2.5rem, 6.4vw, 5rem);
  line-height: 1.1;
  font-weight: 700;
  color: var(--headline);
  text-wrap: balance;
}

.hero-sub {
  margin: 24px auto 34px;
  max-width: 34ch;
  font-size: clamp(1rem, 2vw, 1.18rem);
  color: var(--muted);
}

.hero-actions {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.btn-main,
.btn-outline {
  border: 0;
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

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 22px;
}

.metric-pill {
  display: grid;
  gap: 4px;
  padding: 6px 4px;
  border-radius: 0;
  border: 0;
  background: transparent;
  backdrop-filter: none;
  box-shadow: none;
}

.metric-value {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.34rem;
  font-weight: 700;
  color: var(--headline);
}

.metric-label {
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
}

.scroll-hint {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--muted);
  font-size: 0.82rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.scroll-line {
  width: 78px;
  height: 1px;
  background: linear-gradient(90deg, rgba(16, 32, 51, 0.16), rgba(16, 32, 51, 0.52), rgba(16, 32, 51, 0.16));
}

.reveal-container {
  overflow: hidden;
}

.reveal-text {
  opacity: 0;
  transform: translateY(112%);
  animation: revealUp 0.9s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes revealUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.delay-1 {
  animation-delay: 0.12s;
}

.delay-2 {
  animation-delay: 0.24s;
}

.delay-3 {
  animation-delay: 0.38s;
}

.delay-4 {
  animation-delay: 0.52s;
}

.scroll-box {
  position: relative;
  min-height: 95vh;
  padding: 12px 0;
}

.scroll-box::before,
.scroll-box::after {
  content: '';
  position: absolute;
  left: 50%;
  width: min(1180px, calc(100% - 48px));
  height: 1px;
  transform: translateX(-50%);
  background: linear-gradient(90deg, transparent, rgba(16, 32, 51, 0.12), transparent);
}

.scroll-box::before {
  top: 0;
}

.scroll-box::after {
  bottom: 0;
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
  padding: 88px 24px 52px;
}

.editorial-frame {
  width: min(1180px, 100%);
  min-height: min(70vh, 640px);
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  align-items: stretch;
  gap: 24px;
  padding: clamp(16px, 3vw, 28px);
  border-radius: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.editorial-frame-alt {
  background: transparent;
}

.section-aside {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-right: 24px;
  border-right: 1px solid rgba(16, 32, 51, 0.12);
  color: var(--muted);
}

.section-index {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1;
  color: rgba(16, 32, 51, 0.55);
}

.section-label {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.reveal-block {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 20px;
  text-align: center;
}

.reveal-big-text {
  font-size: clamp(1.8rem, 5vw, 3.7rem);
  line-height: 1.28;
  font-weight: 700;
}

.reveal-by-char {
  display: inline-block;
}

.section-note {
  max-width: 28ch;
  color: var(--muted);
  font-size: 0.96rem;
  line-height: 1.65;
}

.reveal-char {
  display: inline-block;
  color: rgba(14, 27, 42, 0.2);
  transform: translateY(0.14em);
  filter: blur(6px);
  transition: color 10ms linear, filter 10ms linear, transform 10ms linear;
  will-change: transform, filter, color;
}

.inspiration-wall {
  padding: 110px 24px;
  background: linear-gradient(180deg, #f6efe2 0%, #f1e7d7 100%);
}

.stats-section {
  padding: 110px 24px;
  background: linear-gradient(180deg, #ece3d5 0%, #dde8e5 100%);
}

.section-shell {
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: clamp(14px, 3vw, 24px);
  border-radius: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.stats-shell {
  background: transparent;
}

.editorial-rail {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.editorial-rail span:first-child {
  display: inline;
  width: auto;
  height: auto;
  border-radius: 0;
  border: 0;
}

.wall-header,
.stats-header {
  display: grid;
  gap: 12px;
  max-width: 700px;
  margin-bottom: 34px;
  opacity: 0;
  transform: translateY(24px);
  transition: all 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.wall-header.show,
.stats-header.show {
  opacity: 1;
  transform: translateY(0);
}

.tag {
  display: inline-block;
  width: fit-content;
  padding: 0;
  border-radius: 0;
  border: 0;
  background: transparent;
  color: var(--muted);
  letter-spacing: 0.18em;
  font-size: 0.72rem;
  font-weight: 700;
}

.wall-header h2,
.stats-header h2 {
  font-size: clamp(2rem, 4vw, 3rem);
  color: var(--headline);
}

.wall-header p {
  max-width: 38ch;
  line-height: 1.7;
  color: var(--muted);
}

.cosmos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  grid-auto-rows: 208px;
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
  border-radius: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.grid-card::after {
  display: none;
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

.img-wrapper {
  position: relative;
}

.img-wrapper::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(16, 32, 51, 0.02), rgba(16, 32, 51, 0.38));
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
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 20px 20px 18px;
  color: #f9f8f4;
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

.stats-grid {
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
  position: relative;
  display: grid;
  gap: 8px;
  min-height: 210px;
  padding: 12px 8px 8px;
  border-radius: 0;
  border: 0;
  background: transparent;
  backdrop-filter: none;
  box-shadow: none;
}

.stat-item::before {
  display: none;
}

.stat-kicker {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--muted);
}

.stat-value {
  display: block;
  margin-top: auto;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.5rem, 4vw, 3.4rem);
  color: var(--teal-deep);
}

.stat-desc {
  font-size: 1rem;
  font-weight: 700;
  color: var(--headline);
}

.stat-note {
  max-width: 24ch;
  line-height: 1.65;
  color: var(--muted);
}

.footer-cta {
  position: relative;
  padding: 130px 24px 94px;
  background: linear-gradient(165deg, #102230 0%, #132a2e 60%, #143733 100%);
  text-align: center;
  overflow: hidden;
}

.cta-spotlight {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 24%, rgba(255, 255, 255, 0.14) 0%, transparent 38%),
    radial-gradient(circle at 50% 70%, rgba(15, 118, 110, 0.18) 0%, transparent 42%);
  pointer-events: none;
}

.cta-content {
  position: relative;
  width: min(820px, 100%);
  margin: 0 auto;
  padding: 22px 0 0;
  border-radius: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
  opacity: 0;
  transform: translateY(24px);
  transition: all 0.9s cubic-bezier(0.16, 1, 0.3, 1);
}

.cta-content::before {
  display: none;
}

.cta-content.show {
  opacity: 1;
  transform: translateY(0);
}

.tag-inverse {
  margin-bottom: 18px;
  border-color: transparent;
  background: transparent;
  color: rgba(246, 248, 245, 0.82);
}

.cta-content h2 {
  color: #f6f8f5;
  font-size: clamp(1.9rem, 4vw, 3rem);
  margin-bottom: 16px;
}

.cta-content p {
  max-width: 34ch;
  margin: 0 auto 28px;
  line-height: 1.7;
  color: rgba(246, 248, 245, 0.78);
}

.btn-outline {
  color: #f6f8f5;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: transparent;
}

.btn-outline:hover {
  color: #112433;
  background: #f6f8f5;
  transform: translateY(-2px);
}

.main-footer {
  padding: 24px 24px 36px;
  background: #102230;
}

.footer-bottom {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  color: rgba(246, 248, 245, 0.75);
  font-size: 0.82rem;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.footer-logo {
  width: 30px;
  height: 30px;
  font-size: 0.9rem;
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

@media (max-width: 1200px) {
  .float-img {
    width: min(var(--float-width), 15vw);
  }
}

@media (max-width: 992px) {
  .header-shell {
    width: min(960px, calc(100% - 20px));
    padding: 8px 12px;
  }

  .hero-section {
    padding-top: 112px;
  }

  .hero-paper {
    inset: 30px 14px 16px;
  }

  .float-img.tier-far {
    display: none;
  }

  .hero-metrics,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .editorial-frame,
  .editorial-frame-alt {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .section-aside {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding-right: 0;
    padding-bottom: 16px;
    border-right: 0;
    border-bottom: 1px solid rgba(16, 32, 51, 0.12);
  }

  .stat-item {
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .header-shell {
    top: 10px;
    width: calc(100% - 16px);
    gap: 10px;
    padding: 8px 10px;
  }

  .logo-text {
    font-size: 0.78rem;
    letter-spacing: 0.05em;
  }

  .nav-actions {
    gap: 8px;
  }

  .nav-actions button {
    padding: 10px 14px;
    font-size: 0.82rem;
  }

  .hero-section {
    padding: 104px 16px 76px;
    min-height: auto;
  }

  .hero-paper::after,
  .float-label {
    display: none;
  }

  .float-img {
    width: clamp(92px, 24vw, 128px);
  }

  .float-img.tier-mid,
  .float-img.tier-far {
    display: none;
  }

  .float-img.tier-front {
    --float-opacity: 0.44;
  }

  .hero-copy {
    padding: 28px 18px 24px;
  }

  .hero-meta {
    flex-wrap: wrap;
    justify-content: center;
  }

  .hero-actions,
  .hero-metrics {
    width: 100%;
  }

  .btn-main,
  .btn-outline {
    width: min(280px, 100%);
  }

  .scroll-hint {
    flex-direction: column;
  }

  .scroll-line {
    width: 1px;
    height: 42px;
  }

  .scroll-box {
    min-height: 82vh;
  }

  .sticky-container {
    padding: 76px 16px 40px;
  }

  .editorial-frame,
  .editorial-frame-alt,
  .section-shell {
    padding: 12px 0;
    border-radius: 0;
  }

  .inspiration-wall,
  .stats-section,
  .footer-cta {
    padding-left: 16px;
    padding-right: 16px;
  }

  .cosmos-grid {
    grid-template-columns: 1fr 1fr;
    grid-auto-rows: 168px;
  }

  .grid-card.wide,
  .grid-card.tall {
    grid-column: span 1;
    grid-row: span 1;
  }

  .footer-bottom {
    flex-direction: column;
    justify-content: center;
    text-align: center;
  }
}

@media (max-width: 560px) {
  .header-shell {
    flex-wrap: wrap;
    justify-content: center;
  }

  .top-nav,
  .nav-actions {
    width: 100%;
    justify-content: center;
  }

  .hero-copy h1 {
    font-size: clamp(2.05rem, 10vw, 2.8rem);
  }

  .hero-sub,
  .section-note,
  .wall-header p,
  .cta-content p {
    font-size: 0.94rem;
  }

  .cosmos-grid {
    grid-template-columns: 1fr;
    grid-auto-rows: 220px;
  }

  .metric-pill,
  .stat-item {
    padding-left: 14px;
    padding-right: 14px;
  }

  .footer-links {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .float-img,
  .reveal-text {
    animation: none !important;
    transform: none !important;
  }

  .float-img {
    opacity: var(--float-opacity) !important;
  }

  .reveal-char {
    transition: none !important;
    transform: none !important;
    filter: none !important;
    color: rgba(14, 27, 42, 1) !important;
  }

  .wall-header,
  .stats-header,
  .cosmos-grid,
  .stats-grid,
  .cta-content {
    transition: none !important;
    transform: none !important;
    opacity: 1 !important;
  }
}
</style>
