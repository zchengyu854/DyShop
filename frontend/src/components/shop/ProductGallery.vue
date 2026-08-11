<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
  mainImage: { type: String, default: '' },
  name: { type: String, default: '' },
  // 规格 SKU 图（颜色维度等）：有值覆盖大图，清空回退商品图
  skuImage: { type: String, default: '' },
})

const gallery = computed(() => {
  const list = (props.images?.length ? props.images : [props.mainImage]).filter(Boolean)
  return list.length ? list : ['']
})

// SKU 图优先于相册缩略图
const displayImage = computed(() => props.skuImage || gallery.value[current.value] || '')

const current = ref(0)

function select(index) {
  current.value = index
}
</script>

<template>
  <div class="gallery">
    <div class="main-wrap">
      <img v-if="displayImage" class="main-img" :src="displayImage" :alt="name" />
      <div v-else class="main-img main-empty" />
    </div>

    <div v-if="gallery.length > 1" class="thumbs">
      <button
        v-for="(img, i) in gallery"
        :key="img"
        type="button"
        class="thumb"
        :class="{ active: i === current }"
        @click="select(i)"
      >
        <img :src="img" :alt="`${name} 图${i + 1}`" loading="lazy" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.main-wrap {
}
.main-img {
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--radius-lg);
  object-fit: cover;
  display: block;
  background: var(--bg-gray);
}
.empty {
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--radius-lg);
  background: var(--bg-gray);
}
.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}
.thumb {
  width: 64px;
  height: 64px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  background: var(--bg-gray);
  transition: border-color 0.2s;
}
.thumb.active {
  border-color: var(--blue);
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>