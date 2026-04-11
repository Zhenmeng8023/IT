import axios from 'axios'
import { getToken } from '@/utils/auth'

const listeners = new Map()
let source = null
let reconnectTimer = null

const defaultEvents = [
  'connected',
  'notification-created',
  'notification-read',
  'notification-read-all',
  'notification-deleted',
  'notification-cleared',
  'message-created',
  'conversation-updated',
  'conversation-read'
]

function getListenerSet(eventName) {
  if (!listeners.has(eventName)) {
    listeners.set(eventName, new Set())
  }
  return listeners.get(eventName)
}

function totalListenerCount() {
  let total = 0
  listeners.forEach(set => {
    total += set.size
  })
  return total
}

function getClientId() {
  if (!process.client) return 'server'
  const storageKey = 'it-realtime-client-id'
  let clientId = window.sessionStorage.getItem(storageKey)
  if (!clientId) {
    clientId = `client-${Date.now()}-${Math.random().toString(16).slice(2)}`
    window.sessionStorage.setItem(storageKey, clientId)
  }
  return clientId
}

function getBaseUrl() {
  const configured = axios.defaults.baseURL || ''
  if (configured) {
    return configured.replace(/\/+$/, '')
  }
  if (process.client) {
    return window.location.origin.replace(/\/+$/, '')
  }
  return ''
}

function buildStreamUrl() {
  const token = getToken()
  const params = new URLSearchParams()
  if (token) {
    params.set('token', token)
  }
  params.set('clientId', getClientId())
  return `${getBaseUrl()}/api/realtime/stream?${params.toString()}`
}

function dispatch(eventName, payload) {
  const exact = listeners.get(eventName) || new Set()
  exact.forEach(handler => handler(payload))

  const wildcard = listeners.get('*') || new Set()
  wildcard.forEach(handler => handler({ eventName, payload }))
}

function parsePayload(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return raw
  }
}

function bindEventSource(es) {
  defaultEvents.forEach(eventName => {
    es.addEventListener(eventName, event => {
      dispatch(eventName, parsePayload(event.data))
    })
  })
  es.onmessage = event => {
    dispatch('message', parsePayload(event.data))
  }
  es.onerror = () => {
    closeRealtime(false)
    scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (!process.client || reconnectTimer || !getToken() || totalListenerCount() === 0) {
    return
  }
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connectRealtime()
  }, 2000)
}

export function connectRealtime() {
  if (!process.client || source || !getToken() || totalListenerCount() === 0) {
    return source
  }

  source = new EventSource(buildStreamUrl())
  bindEventSource(source)
  return source
}

export function closeRealtime(clearTimer = true) {
  if (source) {
    source.close()
    source = null
  }
  if (clearTimer && reconnectTimer) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

export function reconnectRealtime() {
  closeRealtime()
  connectRealtime()
}

export function subscribeRealtime(eventName, handler) {
  const name = eventName || '*'
  const set = getListenerSet(name)
  set.add(handler)
  connectRealtime()

  return () => {
    set.delete(handler)
    if (set.size === 0) {
      listeners.delete(name)
    }
    if (totalListenerCount() === 0) {
      closeRealtime()
    }
  }
}
