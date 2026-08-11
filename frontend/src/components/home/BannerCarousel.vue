<script setup>
import { computed, onScopeDispose, shallowRef } from 'vue'

const banners = [
  'https://picsum.photos/seed/hero1/1920/800',
  'https://picsum.photos/seed/hero2/1920/800',
  'https://picsum.photos/seed/hero3/1920/800',
]

const index = shallowRef(0)
let timer = null

const count = computed(() => banners.length)

function clamp(i) {
  return ((i % count.value) + count.value) % count.value
}

function goTo(i) {
  if (count.value === 0) return
  index.value = clamp(i)
}

function next() {
  goTo(index.value + 1)
}

function start() {
  if (timer || count.value < 2) return
  timer = setInterval(next, 4000)
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

start()
onScopeDispose(stop)
</script>

<template>
  <section class="hero" @mouseenter="stop" @mouseleave="start">
    <Transition name="fade" mode="out-in">
      <img :key="index" :src="banners[index]" alt="" class="hero-img" />
    </Transition>
    <div class="hero-overlay"></div>
    <div class="hero-content">
      <h1 class="hero-title">新一代科技好物</h1>
      <p class="hero-sub">智能、影音、配件，一站配齐</p>
      <p class="hero-links">
        <a class="hero-link" href="#products">立即选购 ></a>
        <router-link class="hero-link" to="/products">了解更多 ></router-link>
      </p>
    </div>
    <div class="hero-dots">
      <span
        v-for="i in count"
        :key="i"
        class="dot"
        :class="{ active: i - 1 === index }"
        @click="goTo(i - 1)"
      ></span>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  height: clamp(380px, 62vh, 540px);
  overflow: hidden;
  background: #000;
}
.hero-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.45) 0%, rgba(0, 0, 0, 0.15) 45%, rgba(0, 0, 0, 0.35) 100%);
}
.hero-content {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 0 20px;
}
.hero-title {
  margin: 0;
  font-size: clamp(34px, 5vw, 52px);
  font-weight: 600;
  letter-spacing: -0.015em;
  color: #fff;
}
.hero-sub {
  margin: 10px 0 0;
  font-size: clamp(16px, 2vw, 21px);
  color: rgba(255, 255, 255, 0.88);
}
.hero-links {
  margin: 22px 0 0;
  display: flex;
  gap: 28px;
}
.hero-link {
  font-size: 17px;
  color: var(--link-bright);
}
.hero-link:hover {
  text-decoration: underline;
}
.hero-dots {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 8px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: width 0.25s, background 0.25s;
}
.dot.active {
  width: 20px;
  background: #fff;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
