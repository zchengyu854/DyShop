import { ref } from 'vue'

// 最近浏览：localStorage 持久化（key 去重，最多 30 条，最新在前）
const KEY = 'dyshop:recent-viewed'
const MAX = 30

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || []
  } catch {
    return []
  }
}

function save(next) {
  localStorage.setItem(KEY, JSON.stringify(next))
}

export function useRecentViewed() {
  const list = ref(load())

  function set(next) {
    list.value = next
    save(next)
  }

  function record(item) {
    set([
      { id: item.id, name: item.name, mainImage: item.mainImage, price: item.price },
      ...list.value.filter((i) => i.id !== item.id),
    ].slice(0, MAX))
  }

  function remove(id) {
    set(list.value.filter((i) => i.id !== id))
  }

  function clear() {
    set([])
  }

  return { list, record, remove, clear }
}
