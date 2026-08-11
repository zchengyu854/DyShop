import { onBeforeUnmount, onMounted } from 'vue'

/**
 * 滚动渐入：扫描页面内 [data-reveal] 元素，进入视口时添加 .revealed。
 * 纯 opacity/transform 过渡（配合 main.css 全局规则），不触发重排。
 * 首屏 Hero 不标记 data-reveal，避免 LCP 受影响。
 */
export function useReveal() {
  let observer = null
  const els = []

  onMounted(() => {
    if (!('IntersectionObserver' in window)) {
      document.querySelectorAll('[data-reveal]').forEach((el) => el.classList.add('revealed'))
      return
    }
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('revealed')
            observer.unobserve(entry.target)
          }
        })
      },
      { threshold: 0.15, rootMargin: '0px 0px -10% 0px' }
    )
    document.querySelectorAll('[data-reveal]').forEach((el) => {
      els.push(el)
      observer.observe(el)
    })
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    els.length = 0
  })
}
