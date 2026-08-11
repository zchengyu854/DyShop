// 金额引擎单元测试：全部整数分（cents）运算，用例与 docs/ch11/spec.md §6.4 一致
import { describe, it, expect } from 'vitest'
import {
  calculateOrderTotal,
  toCents,
  centsToYuan,
  formatMoney,
} from '../price.js'

const item = (base, qty, member) => ({
  basePriceCents: base,
  quantity: qty,
  ...(member != null && { memberPriceCents: member }),
})

describe('formatMoney / 分元转换', () => {
  it('toCents 正确处理浮点误差', () => {
    expect(toCents(0.1 + 0.2)).toBe(30)
    expect(toCents(19.99)).toBe(1999)
    expect(toCents('12.345')).toBe(1235)
    expect(toCents(null)).toBe(0)
    expect(toCents('abc')).toBe(0)
  })
  it('formatMoney 千分位 / 负数', () => {
    expect(formatMoney(1234567)).toBe('¥12,345.67')
    expect(formatMoney(50)).toBe('¥0.50')
    expect(formatMoney(-50)).toBe('-¥0.50')
  })
  it('centsToYuan', () => {
    expect(centsToYuan(1999)).toBe(19.99)
  })
})

describe('场景1：满减券（含门槛不足分支）', () => {
  it('满 ¥300 减 ¥30 → 券可用', () => {
    const r = calculateOrderTotal([item(20000, 2)], {
      type: 'REDUCE',
      minAmountCents: 30000,
      discountAmountCents: 3000,
    })
    expect(r.totalCents).toBe(40000)
    expect(r.couponApplied).toBe(true)
    expect(r.couponDiscountCents).toBe(3000)
    expect(r.payCents).toBe(37000)
    expect(r.memberBenefitCents).toBe(0)
  })
  it('订单 10000 < 门槛 30000 → 券不可用', () => {
    const r = calculateOrderTotal([item(10000, 1)], {
      type: 'REDUCE',
      minAmountCents: 30000,
      discountAmountCents: 3000,
    })
    expect(r.couponApplied).toBe(false)
    expect(r.couponDiscountCents).toBe(0)
    expect(r.payCents).toBe(10000)
    expect(r.invalid[0].reason).toContain('还差')
  })
})

describe('场景2：会员折扣（无券）', () => {
  it('会员价 90 折 → memberBenefit 生效', () => {
    const r = calculateOrderTotal([item(10000, 1, 9000)])
    expect(r.memberTotalCents).toBe(9000)
    expect(r.memberBenefitCents).toBe(1000)
    expect(r.payCents).toBe(9000)
    expect(r.couponApplied).toBe(false)
  })
})

describe('场景3：立减/无门槛券', () => {
  it('无门槛-¥1 → 实付 = 原价 − ¥1', () => {
    const r = calculateOrderTotal([item(500, 10)], {
      minAmountCents: 0,
      discountAmountCents: 100,
    })
    expect(r.payCents).toBe(4900)
    expect(r.couponDiscountCents).toBe(100)
  })
  it('券额 > 适用小计 → 不超适用小计，实付下限 0', () => {
    const r = calculateOrderTotal([item(500, 3)], {
      minAmountCents: 0,
      discountAmountCents: 1000,
    })
    // subtotal 1500；满分 1000 → 抵扣 min(1000,1500)=1000 → 实付 500
    expect(r.couponDiscountCents).toBe(1000)
    expect(r.payCents).toBe(500)
  })
  it('券额 ≥ 应付 → 实付 0，不返现', () => {
    const r = calculateOrderTotal([item(500, 3)], {
      minAmountCents: 0,
      discountAmountCents: 2000,
    })
    expect(r.couponDiscountCents).toBe(1500)
    expect(r.payCents).toBe(0)
  })
})

describe('场景4：两张券按 priority 叠加', () => {
  it('依次扣减，第二张门槛按剩余应付校验', () => {
    const r = calculateOrderTotal([item(10000, 1)], [
      { priority: 1, minAmountCents: 3000, discountAmountCents: 2000 },
      { priority: 2, minAmountCents: 8000, discountAmountCents: 1500 },
    ])
    // c1: 10000≥3000 → -2000，剩 8000；c2: 8000≥8000 → -1500 → 6500
    expect(r.couponApplied).toBe(true)
    expect(r.couponDiscountCents).toBe(3500)
    expect(r.payCents).toBe(6500)
    expect(r.usedCoupons).toHaveLength(2)
  })
  it('第二张门槛 > 剩余应付 → 不可用，仍用第一张', () => {
    const r = calculateOrderTotal([item(10000, 1)], [
      { priority: 1, minAmountCents: 3000, discountAmountCents: 2000 },
      { priority: 2, minAmountCents: 9000, discountAmountCents: 1000 },
    ])
    expect(r.couponApplied).toBe(true)
    expect(r.couponDiscountCents).toBe(2000)
    expect(r.payCents).toBe(8000)
    expect(r.usedCoupons).toHaveLength(1)
    expect(r.invalid[0].reason).toContain('还差')
  })
})

describe('场景5：二选一取优（会员更省 → 券不生效）', () => {
  it('券方案 10000 > 会员方案 8000 → 用会员价', () => {
    const r = calculateOrderTotal([item(10000, 1, 8000)], {
      minAmountCents: 0,
      discountAmountCents: 1000,
    })
    expect(r.couponApplied).toBe(false)
    expect(r.payCents).toBe(8000)
    expect(r.memberBenefitCents).toBe(2000)
    expect(r.couponDiscountCents).toBe(0)
    expect(r.usedCoupons).toHaveLength(0)
  })
})

describe('场景6：取消用券（无券 → 回退原价/会员价）', () => {
  it('无券 → 会员价', () => {
    const r = calculateOrderTotal([item(10000, 1, 9500)], null)
    expect(r.couponApplied).toBe(false)
    expect(r.payCents).toBe(9500)
    expect(r.memberBenefitCents).toBe(500)
  })
})

describe('附加：LIMITED 范围券', () => {
  const rows = [
    item(15000, 1),
    { ...item(8000, 1), productId: 2 },
  ]
  it('只按适用商品小计判定并抵扣', () => {
    const r = calculateOrderTotal(rows, {
      scope: 'LIMITED',
      productIds: [2],
      minAmountCents: 0,
      discountAmountCents: 5000,
    })
    expect(r.totalCents).toBe(23000)
    expect(r.couponApplied).toBe(true)
    expect(r.couponDiscountCents).toBe(5000)
    expect(r.payCents).toBe(18000)
  })
  it('适用小计不足门槛 → 不可用', () => {
    const r = calculateOrderTotal(rows, {
      scope: 'LIMITED',
      productIds: [2],
      minAmountCents: 10000,
      discountAmountCents: 5000,
    })
    expect(r.couponApplied).toBe(false)
    expect(r.invalid[0].reason).toContain('还差')
  })
})