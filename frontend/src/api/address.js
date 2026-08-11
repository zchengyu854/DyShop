import { request } from '@/utils/request'

// 地址列表（默认在前）
export function fetchAddresses() {
  return request.get('/addresses')
}

// 新增地址
export function createAddress(data) {
  return request.post('/addresses', data)
}

// 编辑地址
export function updateAddress(id, data) {
  return request.put(`/addresses/${id}`, data)
}

// 删除地址（逻辑删除）
export function deleteAddress(id) {
  return request.delete(`/addresses/${id}`)
}

// 设为默认地址
export function setDefaultAddress(id) {
  return request.put(`/addresses/${id}/default`)
}
