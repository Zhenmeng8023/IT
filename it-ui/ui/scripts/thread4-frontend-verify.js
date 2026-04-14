const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')
const babelParser = require('@babel/parser')

const root = path.resolve(__dirname, '..')
const files = [
  'pages/f_project/projectmanage/components/ProjectAuditCenter.vue',
  'pages/f_project/projectmanage/components/ProjectRepoWorkbench.vue',
  'pages/f_project/projectmanage/components/ConflictBadge.vue',
  'pages/f_project/projectmanage/components/ConflictListPanel.vue',
  'pages/f_project/projectmanage/components/ConflictResolutionActionBar.vue',
  'pages/f_project/projectmanage/components/ConflictDetailDrawer.vue',
  'api/projectMergeRequest.js'
]

function parseScript(code, file) {
  babelParser.parse(code, {
    sourceType: 'module',
    plugins: [
      'dynamicImport',
      'optionalChaining',
      'nullishCoalescingOperator',
      'objectRestSpread'
    ]
  })
  console.log(`[ok] script ${file}`)
}

function verifyVueFile(relativeFile) {
  const absoluteFile = path.join(root, relativeFile)
  const source = fs.readFileSync(absoluteFile, 'utf8')
  const parsed = compiler.parseComponent(source)

  if (!parsed.template || !parsed.template.content) {
    throw new Error(`Missing template in ${relativeFile}`)
  }

  const compiled = compiler.compile(parsed.template.content)
  if (compiled.errors && compiled.errors.length) {
    throw new Error(`Template compile failed in ${relativeFile}: ${compiled.errors.join('; ')}`)
  }

  if (parsed.script && parsed.script.content) {
    parseScript(parsed.script.content, relativeFile)
  }

  console.log(`[ok] template ${relativeFile}`)
}

function verifyJsFile(relativeFile) {
  const absoluteFile = path.join(root, relativeFile)
  parseScript(fs.readFileSync(absoluteFile, 'utf8'), relativeFile)
}

for (const file of files) {
  if (file.endsWith('.vue')) {
    verifyVueFile(file)
  } else {
    verifyJsFile(file)
  }
}

console.log('thread4 frontend verification passed')
