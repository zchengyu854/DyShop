import { reactive, computed } from 'vue'
import { cancelOrder, confirmOrder, deleteOrder, payOrder } from '@/api/order'
import { toast } from '@/utils/toast'

// ============================================================================
// useOrderAction —— 订单操作命令模式组合式函数
// ----------------------------------------------------------------------------
// 设计目标（P0 体验缺陷修复：点击无即时反馈 / 重复提交 / 状态错乱）：
//   1) 点击即响应：调用 run() 同步进入 pending（按钮 <50ms 内显示 loading + 禁用），
//      乐观更新本地状态（不可变：通过 patchOrder 替换对象，不 mutate 原对象）。
//   2) 状态锁：同一订单 pending 期间重复 run() 直接忽略（含连点/键盘重复触发）。
//   3) 回滚：接口失败/超时（8s）自动恢复乐观快照 + toast 具体错误原因。
//   4) 幂等契约：后端动作接口对「已处于目标状态」重复调用返回成功（no-op），
//      弱网超时后的重试不会因重复取消/确认/删除而报错。
//   5) 批量：runBatch 同步锁定所有选中订单，逐单回滚失败项，汇总提示。
// ----------------------------------------------------------------------------

/** 动作命令注册表：每类操作 = 接口 + 乐观目标状态 + 文案 + 二次确认元数据 */
export const ORDER_ACTIONS = {
  pay: {
    run: payOrder,
    targetStatus: 1, // 待支付 → 待发货
    needsConfirm: false,
    loadingText: '支付中…',
    successText: '支付成功',
    failText: '支付发起失败',
  },
  cancel: {
    run: cancelOrder,
    targetStatus: 4, // 待支付 → 已取消
    needsConfirm: true,
    loadingText: '取消中…',
    successText: '订单已取消',
    failText: '取消失败',
  },
  confirm: {
    run: confirmOrder,
    targetStatus: 3, // 待收货 → 已完成
    needsConfirm: true,
    loadingText: '确认中…',
    successText: '已确认收货',
    failText: '确认失败',
  },
  delete: {
    run: deleteOrder,
    targetStatus: null, // 删除为终态：成功后再移除行（不做乐观状态回填）
    needsConfirm: true,
    loadingText: '删除中…',
    successText: '订单已删除',
    failText: '删除失败',
  },
}

/** 本地状态文案（乐观更新瞬时回填，服务端校正后以接口为准） */
export const STATUS_TEXT = ['待支付', '待发货', '待收货', '已完成', '已取消']

/** 超时错误判定：axios 超时 code=ECONNABORTED */
function isTimeoutError(err) {
  return err?.code === 'ECONNABORTED' || /timeout/i.test(err?.message || '')
}

/** 统一失败文案：超时 → 明确引导；其余透出后端 message */
function failMessage(action, err) {
  if (isTimeoutError(err)) return '网络较慢，请稍后重试'
  return `${action.failText}${err?.message ? `: ${err.message}` : ''}`
}

/**
 * @param {object} opts
 * @param {(orderId: number, patch: object) => void} opts.patchOrder
 *   不可变更新回调：返回替换后的订单对象数组（组件负责实现 map 替换，禁止 mutate）
 * @param {(orderId: number) => void} [opts.removeOrder] 删除成功后的行移除回调
 * @param {(order: object) => void} [opts.onSuccess] 每单成功回调（toast 之后触发）
 */
export function useOrderAction({ patchOrder, removeOrder, onSuccess }) {
  /** 进行中的订单 id → action 名（同一订单互斥，锁内忽略重复触发） */
  const pending = reactive(new Map())

  const isPending = (orderId) => pending.has(orderId)
  const anyPending = computed(() => pending.size > 0)

  /** 乐观状态快照（仅取会被乐观更新的字段，回滚时精确恢复） */
  function snapshot(order) {
    return {
      status: order.status,
      statusText: order.statusText,
      payDeadline: order.payDeadline,
    }
  }

  /**
   * 单订单动作：点击 → 锁定 + 乐观更新 → 接口 → 成功保持/失败回滚。
   * @returns {Promise<boolean>} true=成功 false=失败或已被锁忽略
   */
  async function run(actionName, order) {
    const action = ORDER_ACTIONS[actionName]
    if (!action || pending.has(order.id)) return false

    pending.set(order.id, actionName)
    const prev = snapshot(order)
    // 乐观更新（不可变）：非删除操作先回填目标状态，删除等终态
    if (action.targetStatus !== null) {
      patchOrder(order.id, {
        status: action.targetStatus,
        statusText: STATUS_TEXT[action.targetStatus] || order.statusText,
        payDeadline: null,
      })
    }
    try {
      await action.run(order.id)
      toast.success(action.successText)
      if (actionName === 'delete') {
        removeOrder?.(order.id)
      }
      onSuccess?.(order)
      return true
    } catch (err) {
      // 失败回滚：恢复乐观快照（删除动作无乐观状态，无需回滚）
      if (action.targetStatus !== null) {
        patchOrder(order.id, prev)
      }
      toast.error(failMessage(action, err))
      return false
    } finally {
      pending.delete(order.id)
    }
  }

  /**
   * 批量动作：所有选中订单同步进入 pending（按钮整体 loading + 禁用），
   * 逐单成功/失败独立处理；返回 { ok, succeeded, failed } 汇总供页面提示。
   */
  async function runBatch(actionName, orders) {
    const targets = orders.filter((o) => !pending.has(o.id))
    if (targets.length === 0) return { ok: false, succeeded: 0, failed: 0 }

    const results = await Promise.all(targets.map((o) => run(actionName, o)))
    const succeeded = results.filter(Boolean).length
    const failed = targets.length - succeeded
    return { ok: failed === 0, succeeded, failed }
  }

  return { isPending, anyPending, run, runBatch }
}
