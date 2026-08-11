<script setup>
import { reactive, ref, watch } from 'vue'
import { createAddress, updateAddress } from '@/api/address'
import { getCities, getDistricts, getProvinces } from '@/utils/region'
import { toast } from '@/utils/toast'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 编辑对象；null 表示新增
  address: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])

const provinces = getProvinces()

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
})

const errors = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  detail: '',
})

const saving = ref(false)

// 当前地址已是默认时，禁止取消默认（保证始终有默认地址）
const isCurrentDefault = ref(false)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    const addr = props.address
    isCurrentDefault.value = !!addr?.isDefault
    form.receiverName = addr?.receiverName || ''
    form.receiverPhone = addr?.receiverPhone || ''
    form.province = addr?.province || ''
    form.city = addr?.city || ''
    form.district = addr?.district || ''
    form.detail = addr?.detail || ''
    form.isDefault = addr ? !!addr.isDefault : false
    resetErrors()
    document.body.style.overflow = 'hidden'
  }
)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) document.body.style.overflow = ''
  }
)

watch(
  () => form.province,
  () => {
    form.city = ''
    form.district = ''
    errors.city = ''
  }
)

watch(
  () => form.city,
  () => {
    form.district = ''
  }
)

const cities = () => getCities(form.province)
const districts = () => getDistricts(form.province, form.city)

function validate() {
  resetErrors()
  let ok = true
  if (!form.receiverName.trim() || form.receiverName.trim().length < 2) {
    errors.receiverName = '收货人姓名需 2~50 个字符'
    ok = false
  }
  if (!/^1[3-9]\d{9}$/.test(form.receiverPhone.trim())) {
    errors.receiverPhone = '请输入正确的 11 位手机号'
    ok = false
  }
  if (!form.province) {
    errors.province = '请选择省份'
    ok = false
  }
  if (!form.city) {
    errors.city = '请选择城市'
    ok = false
  }
  if (!form.detail.trim() || form.detail.trim().length < 5) {
    errors.detail = '详细地址需 5~200 个字符'
    ok = false
  }
  return ok
}

function resetErrors() {
  Object.keys(errors).forEach((k) => (errors[k] = ''))
}

function close() {
  emit('update:modelValue', false)
}

async function handleSave() {
  if (!validate()) return
  const payload = {
    receiverName: form.receiverName.trim(),
    receiverPhone: form.receiverPhone.trim(),
    province: form.province,
    city: form.city,
    district: form.district,
    detail: form.detail.trim(),
    isDefault: form.isDefault ? 1 : 0,
  }
  saving.value = true
  try {
    if (props.address) {
      await updateAddress(props.address.id, payload)
      toast.success('地址已更新')
    } else {
      await createAddress(payload)
      toast.success('地址已添加')
    }
    emit('saved')
    close()
  } catch (e) {
    toast.error(e.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="overlay" @click.self="close">
      <div class="modal" role="dialog" aria-modal="true" :aria-label="address ? '编辑收货地址' : '新增收货地址'">
        <h3 class="modal-title">{{ address ? '编辑收货地址' : '新增收货地址' }}</h3>

        <div class="field">
          <label class="field-label" for="addr-name">收货人</label>
          <input
            id="addr-name"
            v-model="form.receiverName"
            class="field-input"
            type="text"
            maxlength="50"
            placeholder="收货人姓名"
          />
          <p v-if="errors.receiverName" class="field-error">{{ errors.receiverName }}</p>
        </div>

        <div class="field">
          <label class="field-label" for="addr-phone">手机号</label>
          <input
            id="addr-phone"
            v-model="form.receiverPhone"
            class="field-input"
            type="tel"
            maxlength="11"
            placeholder="11 位手机号"
          />
          <p v-if="errors.receiverPhone" class="field-error">{{ errors.receiverPhone }}</p>
        </div>

        <div class="field">
          <label class="field-label" for="addr-province">所在地区</label>
          <div class="region-row">
            <select id="addr-province" v-model="form.province" class="region-select">
              <option value="" disabled>省份</option>
              <option v-for="p in provinces" :key="p" :value="p">{{ p }}</option>
            </select>
            <select v-model="form.city" class="region-select" :disabled="!form.province">
              <option value="" disabled>城市</option>
              <option v-for="c in cities()" :key="c" :value="c">{{ c }}</option>
            </select>
            <select
              v-model="form.district"
              class="region-select"
              :disabled="!form.city || districts().length === 0"
            >
              <option value="" disabled>区/县</option>
              <option v-for="d in districts()" :key="d" :value="d">{{ d }}</option>
            </select>
          </div>
          <p v-if="errors.province || errors.city" class="field-error">
            {{ errors.province || errors.city }}
          </p>
        </div>

        <div class="field">
          <label class="field-label" for="addr-detail">详细地址</label>
          <textarea
            id="addr-detail"
            v-model="form.detail"
            class="field-input field-textarea"
            rows="3"
            maxlength="200"
            placeholder="街道、楼牌号等"
          />
          <p v-if="errors.detail" class="field-error">{{ errors.detail }}</p>
        </div>

        <div class="field">
          <label class="switch-row" :class="{ 'switch-disabled': isCurrentDefault }">
            <input
              v-model="form.isDefault"
              type="checkbox"
              class="switch-input"
              :disabled="isCurrentDefault"
            />
            <span class="switch-track" aria-hidden="true"><span class="switch-thumb" /></span>
            <span class="switch-label">设为默认地址</span>
          </label>
          <p v-if="isCurrentDefault" class="field-hint">默认地址不可取消，请先设置其他地址为默认</p>
        </div>

        <div class="modal-actions">
          <button class="cancel-btn" @click="close">取消</button>
          <button class="save-btn" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.4);
}
.modal {
  width: 100%;
  max-width: 440px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  background: var(--bg);
  border-radius: 20px;
  padding: 28px 28px 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}
.modal-title {
  margin: 0 0 20px;
  font-size: 17px;
  font-weight: 600;
  color: var(--ink);
}
.field + .field {
  margin-top: 14px;
}
.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--ink-secondary);
}
.field-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg);
  color: var(--ink);
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.field-input:focus {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.15);
}
.field-textarea {
  height: auto;
  padding: 10px 14px;
  resize: vertical;
  line-height: 1.5;
}
.field-error {
  margin: 5px 0 0;
  font-size: 12px;
  color: #ff3b30;
}
.field-hint {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--ink-faint);
}
.region-row {
  display: flex;
  gap: 8px;
}
.region-select {
  flex: 1;
  min-width: 0;
  height: 44px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg);
  color: var(--ink);
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}
.region-select:focus {
  border-color: var(--blue);
}
.region-select:disabled {
  color: var(--ink-faint);
  background: var(--bg-gray);
  cursor: not-allowed;
}
.switch-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.switch-disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.switch-input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}
.switch-track {
  position: relative;
  width: 40px;
  height: 24px;
  border-radius: var(--radius-full);
  background: var(--border);
  transition: background 0.2s;
  flex: none;
}
.switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.2s;
}
.switch-input:checked + .switch-track {
  background: var(--blue);
}
.switch-input:checked + .switch-track .switch-thumb {
  transform: translateX(16px);
}
.switch-label {
  font-size: 14px;
  color: var(--ink);
}
.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}
.cancel-btn,
.save-btn {
  flex: 1;
  height: 44px;
  border-radius: var(--radius-full);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s, opacity 0.2s;
}
.cancel-btn {
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--ink);
}
.cancel-btn:hover {
  background: var(--bg-gray);
}
.save-btn {
  border: none;
  background: var(--blue);
  color: #fff;
  font-weight: 600;
}
.save-btn:hover {
  background: #0077ed;
}
.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
@media (max-width: 480px) {
  .region-row {
    flex-direction: column;
  }
}
</style>
