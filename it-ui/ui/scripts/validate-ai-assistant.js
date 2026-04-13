const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const runtime = require(path.join(root, 'utils', 'aiRuntime.js'))

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8')
}

function testErrorClassification() {
  assert.strictEqual(runtime.classifyAiError({ code: 'ECONNABORTED', message: 'timeout' }).type, 'timeout')
  assert.strictEqual(runtime.classifyAiError({ response: { status: 401, data: { message: 'unauthorized' } } }).type, 'unauthorized')
  assert.strictEqual(runtime.classifyAiError({ response: { status: 403, data: { message: 'forbidden' } } }).type, 'forbidden')
  assert.strictEqual(runtime.classifyAiError({ response: { status: 500, data: { message: 'server' } } }).type, 'server')
  assert.strictEqual(runtime.classifyAiError({ name: 'AbortError', message: 'aborted' }).type, 'canceled')
  assert.strictEqual(runtime.classifyAiError(new TypeError('Failed to fetch')).type, 'network')
}

function testSseParsing() {
  const events = []
  let buffer = ''
  buffer += 'data: {"delta":"你"}\n\n'
  buffer = runtime.consumeSseBuffer(buffer, event => events.push(runtime.parseStreamPayload(event.data)))
  buffer += 'event: message\n'
  buffer += 'data: {"delta":"好","sessionId":12}\n\n'
  buffer = runtime.consumeSseBuffer(buffer, event => events.push(runtime.parseStreamPayload(event.data)))

  assert.strictEqual(buffer, '')
  assert.deepStrictEqual(events.map(item => item.delta), ['你', '好'])
  assert.strictEqual(events[1].sessionId, 12)
}

function testSourceNormalization() {
  const sources = runtime.extractAiSources({
    citations: [{
      knowledgeBaseId: 7,
      knowledgeBaseName: '项目知识库',
      documentId: 8,
      documentTitle: 'README',
      chunkId: 9,
      content: '命中内容'
    }]
  })
  assert.strictEqual(sources[0].knowledgeBaseId, 7)
  assert.strictEqual(sources[0].knowledgeBaseName, '项目知识库')
  assert.strictEqual(sources[0].documentId, 8)
  assert.strictEqual(sources[0].documentTitle, 'README')
  assert.strictEqual(sources[0].chunkId, 9)
}

function testStaticInteractionContracts() {
  const assistant = read('components/AIAssistant.vue')
  const dock = read('components/SceneAiDock.vue')
  const api = read('api/aiAssistant.js')
  const request = read('utils/request.js')

  assert(assistant.includes("window.addEventListener('ai-assistant-open'"), 'assistant listens for dock open event')
  assert(assistant.includes('activeStream'), 'assistant owns active stream lifecycle')
  assert(assistant.includes('stopStream(false'), 'assistant exposes cancel flow')
  assert(assistant.includes('retryLast'), 'assistant exposes retry flow')
  assert(assistant.includes('selectSession'), 'assistant supports session switching')
  assert(assistant.includes('handleKnowledgeBaseChange'), 'assistant supports knowledge-base switching')
  assert(assistant.includes('developerMode'), 'assistant gates debug UI behind developer mode')
  assert(dock.includes("new CustomEvent('ai-assistant-open'"), 'dock opens the main assistant')
  assert(!dock.includes('aiChatTurn'), 'dock does not call AI APIs directly')
  assert(!dock.includes('modelId: 1'), 'dock does not hard-code a model id')
  assert(!dock.includes('<el-dialog'), 'dock does not own result dialogs')
  assert(api.includes("credentials: 'include'"), 'stream fetch includes cookies')
  assert(api.includes('headers: buildAuthHeaders(headers)'), 'stream fetch uses shared auth headers')
  assert(request.includes('buildAuthHeaders'), 'axios request injects shared auth headers')
}

testErrorClassification()
testSseParsing()
testSourceNormalization()
testStaticInteractionContracts()

console.log('AI assistant validation passed')
