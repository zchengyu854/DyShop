import { defineStore } from 'pinia'
import {
  addCartItem,
  clearCart as apiClearCart,
  fetchCart,
  removeCartItem,
  updateCartChecked,
  updateCartItem,
} from '@/api/cart'

// 购物车态：与服务端同步。购物车接口均需登录。
// 可购买：有库存且商品未下架（下架/售罄行不参与勾选与结算）
function isPurchasable(it) {
  return (it.stock ?? 0) > 0 && (it.productStatus ?? 1) === 1
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
  }),
  getters: {
    totalQuantity: (state) => state.items.reduce((sum, it) => sum + it.quantity, 0),
    totalPrice: (state) =>
      state.items.reduce((sum, it) => sum + Number(it.price) * it.quantity, 0),
    // 勾选维度（结算使用）；下架/售罄行（不可购买）不参与勾选/结算
    checkedItems: (state) => state.items.filter((it) => isChecked(it) && isPurchasable(it)),
    checkedTotalQuantity: (state) =>
      state.items.reduce(
        (sum, it) => sum + (isChecked(it) && isPurchasable(it) ? it.quantity : 0),
        0
      ),
    checkedTotalPrice: (state) =>
      state.items.reduce(
        (sum, it) =>
          sum + (isChecked(it) && isPurchasable(it) ? Number(it.price) * it.quantity : 0),
        0
      ),
    allChecked: (state) => {
      const available = state.items.filter((it) => isPurchasable(it))
      return available.length > 0 && available.every((it) => isChecked(it))
    },
  },
  actions: {
    async fetchCart() {
      this.items = await fetchCart()
    },
    async addToCart(productId, quantity = 1, skuId = 0) {
      await addCartItem(productId, quantity, skuId)
      await this.fetchCart()
    },
    async updateQuantity(cartItemId, quantity) {
      await updateCartItem(cartItemId, quantity)
      const item = this.items.find((it) => it.cartItemId === cartItemId)
      if (item) item.quantity = quantity
    },
    async toggleChecked(cartItemId, checked) {
      const item = this.items.find((it) => it.cartItemId === cartItemId)
      if (!item) return
      const prev = isChecked(item)
      item.checked = checked ? 1 : 0
      try {
        await updateCartChecked(cartItemId, checked)
      } catch (e) {
        item.checked = prev ? 1 : 0
        throw e
      }
    },
    async setAllChecked(checked) {
      // 全选/全不选不包含不可购买行（下架/售罄）
      const targets = this.items.filter(
        (it) => isPurchasable(it) && isChecked(it) !== checked
      )
      // 逐条切换，任一条失败则回滚并中断
      const snapshots = targets.map((it) => ({ item: it, prev: isChecked(it) }))
      targets.forEach((it) => {
        it.checked = checked ? 1 : 0
      })
      try {
        for (const { item } of snapshots) {
          await updateCartChecked(item.cartItemId, checked)
        }
      } catch (e) {
        snapshots.forEach(({ item, prev }) => {
          item.checked = prev ? 1 : 0
        })
        throw e
      }
    },
    async removeItem(cartItemId) {
      await removeCartItem(cartItemId)
      this.items = this.items.filter((it) => it.cartItemId !== cartItemId)
    },
    async clear() {
      await apiClearCart()
      this.items = []
    },
  },
})

// 后端 checked 为 0/1，缺省视为已勾选（兼容旧数据）
function isChecked(item) {
  return item.checked == null || item.checked === 1
}
