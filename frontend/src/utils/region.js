import pca from '@/assets/data/pca.json'

// 省市区联动数据（pca.json：省名 -> { 市名 -> [区名] }，民政部行政区划）

export function getProvinces() {
  return Object.keys(pca)
}

export function getCities(province) {
  return province ? Object.keys(pca[province] || {}) : []
}

export function getDistricts(province, city) {
  if (!province || !city) return []
  return pca[province]?.[city] || []
}

export function isValidRegion(province, city, district) {
  if (!getCities(province).includes(city)) return false
  const districts = getDistricts(province, city)
  return districts.length === 0 || districts.includes(district)
}
