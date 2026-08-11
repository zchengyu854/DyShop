import { request } from '@/utils/request'

// 购物车列表
export function fetchCart() {
  return request.get('/cart')
}

// 加入购物车（同商品同 SKU 已存在则累加；skuId=0 表示无规格商品）
export function addCartItem(productId, quantity = 1, skuId = 0) {
  return request.post('/cart/items', { productId, skuId, quantity })
}

// 更新数量（按购物车行 id）
export function updateCartItem(cartItemId, quantity) {
  return request.put(`/cart/items/${cartItemId}`, { quantity })
}

// 更新勾选状态（按购物车行 id）
export function updateCartChecked(cartItemId, checked) {
  return request.put(`/cart/items/${cartItemId}/checked`, { checked: checked ? 1 : 0 })
}

// 移除单个条目（按购物车行 id）
export function removeCartItem(cartItemId) {
  return request.delete(`/cart/items/${cartItemId}`)
}

// 清空购物车
export function clearCart() {
  return request.delete('/cart')
}
