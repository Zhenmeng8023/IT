import http from './shared/http'

const QR_IMAGE_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg']

export function isPaidStatus(status) {
  return ['paid', 'success', 'completed'].includes(String(status || '').toLowerCase())
}

export function isFailedStatus(status) {
  return ['failed', 'fail', 'cancelled', 'canceled', 'closed', 'timeout'].includes(String(status || '').toLowerCase())
}

export function isImageQrCode(codeUrl) {
  const normalized = String(codeUrl || '').toLowerCase()
  return QR_IMAGE_EXTENSIONS.some(extension => normalized.includes(extension))
}

export function buildQrImageUrl(codeUrl, size = 220) {
  if (!codeUrl) {
    return ''
  }

  if (isImageQrCode(codeUrl)) {
    return codeUrl
  }

  return `https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&data=${encodeURIComponent(codeUrl)}`
}

function parsePaymentUrl(paymentUrl) {
  if (!paymentUrl) {
    return {}
  }

  try {
    const baseUrl = typeof window !== 'undefined' && window.location
      ? window.location.origin
      : 'http://localhost:3000'
    const url = new URL(paymentUrl, baseUrl)

    return {
      orderNo: url.searchParams.get('orderNo') || '',
      amount: url.searchParams.get('amount') || '',
      paymentType: url.searchParams.get('type') || '',
      codeUrl: url.searchParams.get('codeUrl') || ''
    }
  } catch (error) {
    return {}
  }
}

export async function getOrderByNo(orderNo) {
  if (!orderNo) {
    throw new Error('缺少订单号')
  }

  const response = await http.get(`/api/orders/order-no/${encodeURIComponent(orderNo)}`)
  return response.data
}

export async function pollOrderStatus(orderNo) {
  const order = await getOrderByNo(orderNo)
  const status = order && order.status

  return {
    order,
    status,
    paid: isPaidStatus(status),
    failed: isFailedStatus(status)
  }
}

export async function payTestOrder(orderNo) {
  if (process.env.NODE_ENV === 'production') {
    throw new Error('测试支付接口仅允许开发环境调用')
  }

  if (!orderNo) {
    throw new Error('缺少订单号')
  }

  const response = await http.post('/api/orders/pay-test', null, {
    params: { orderNo }
  })
  return response.data
}

export async function getPaymentQrCode(orderNo, paymentMethod = 'wechat') {
  const order = await getOrderByNo(orderNo)

  if (!order || !order.id) {
    return {
      order,
      codeUrl: '',
      qrCodeUrl: ''
    }
  }

  const response = await http.post(`/api/orders/${order.id}/payment-url`, null, {
    params: { paymentMethod }
  })
  const payload = response.data || {}
  const parsed = parsePaymentUrl(payload.paymentUrl || payload.url || '')
  const codeUrl = payload.codeUrl || parsed.codeUrl || payload.paymentUrl || ''

  return {
    ...payload,
    ...parsed,
    order,
    codeUrl,
    qrCodeUrl: buildQrImageUrl(codeUrl)
  }
}
