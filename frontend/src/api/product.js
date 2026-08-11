import { request } from '@/utils/request'

// 商品分页列表：{ page, size, categoryId, keyword }
export function fetchProducts(params) {
  return request.get('/products', { params })
}

// 商品详情
export function fetchProductDetail(id) {
  return request.get(`/products/${id}`)
}

// 分类列表
export function fetchCategories() {
  return request.get('/categories')
}
