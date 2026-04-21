function normalizeText(value) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function normalizeSceneCode(sceneCode = '') {
  const raw = normalizeText(sceneCode).toLowerCase()
  if (!raw) return 'global.assistant'
  if (raw === 'project-detail' || raw === 'projectdetail') return 'project.detail'
  if (raw === 'blog-write' || raw === 'blogwrite') return 'blog.write'
  if (raw === 'blog-detail' || raw === 'blogdetail') return 'blog.detail'
  if (raw === 'knowledge-base' || raw === 'knowledgebase') return 'knowledge.base'
  return raw
}

const AI_SUGGESTION_PRESETS = Object.freeze({
  'project.detail': Object.freeze([
    Object.freeze({
      label: '分析项目',
      actionCode: 'project.detail.summary',
      prompt: '请结合当前项目上下文，生成项目概览、核心功能、主要风险和下一步建议。',
      description: '梳理项目目标、现状和整体判断'
    }),
    Object.freeze({
      label: '拆解任务',
      actionCode: 'project.detail.tasks',
      prompt: '请把当前项目拆解为可执行任务，按阶段给出优先级、产出和注意事项。',
      description: '把目标拆成可执行阶段和任务草稿'
    }),
    Object.freeze({
      label: '识别风险',
      actionCode: 'project.detail.risks',
      prompt: '请识别当前项目的关键风险、影响范围和缓解建议。',
      description: '提前发现风险点和阻塞项'
    }),
    Object.freeze({
      label: '下一步建议',
      actionCode: 'project.detail.next-steps',
      prompt: '请给出当前项目接下来最值得推进的行动建议和优先顺序。',
      description: '给出短期可执行的下一步动作'
    })
  ]),
  'blog.write': Object.freeze([
    Object.freeze({
      label: '润色正文',
      actionCode: 'blog.polish',
      prompt: '请润色当前博客正文，保留原意，不编造事实，并指出需要作者确认的地方。',
      description: '优化正文表达、结构和可读性'
    }),
    Object.freeze({
      label: '生成摘要',
      actionCode: 'blog.summary',
      prompt: '请为当前博客生成 120 字以内摘要，并保留关键信息。',
      description: '提炼适合展示的摘要草稿'
    }),
    Object.freeze({
      label: '生成标签',
      actionCode: 'blog.summary',
      prompt: '请根据当前博客内容生成 3-5 个具体、可检索的标签建议。',
      description: '补充摘要和标签推荐'
    })
  ]),
  'blog.detail': Object.freeze([
    Object.freeze({
      label: '获取当前信息',
      actionCode: 'blog.detail.summary',
      prompt: '请基于当前博客详情提炼核心信息，并说明阅读重点。',
      description: '快速理解当前博客信息'
    }),
    Object.freeze({
      label: '解释当前博客',
      actionCode: 'blog.detail.explain',
      prompt: '请用更易懂的语言解释当前博客的主要观点和背景。',
      description: '将当前博客内容解释得更清晰'
    }),
    Object.freeze({
      label: '猜你可能疑惑',
      actionCode: 'blog.detail.possible-questions',
      prompt: '请猜测读者最可能疑惑的问题，并给出简洁回答。',
      description: '提前识别读者可能疑问'
    })
  ]),
  'knowledge.base': Object.freeze([
    Object.freeze({
      label: '知识库能回答什么',
      actionCode: 'knowledge.base.capability',
      prompt: '请说明当前知识库更适合回答哪些问题、有哪些边界，以及推荐怎么提问。',
      description: '判断当前知识库的适用范围'
    }),
    Object.freeze({
      label: '知识缺口',
      actionCode: 'knowledge.base.gaps',
      prompt: '请结合当前知识库说明可能缺少哪些信息，哪些问题回答风险较高。',
      description: '识别缺失资料和高风险问题'
    }),
    Object.freeze({
      label: '检索提示',
      actionCode: 'knowledge.base.search-hints',
      prompt: '请给我更好的关键词、提问方式和检索提示，帮助我更准确命中知识库内容。',
      description: '优化提问方式和检索命中率'
    })
  ]),
  'global.assistant': Object.freeze([
    Object.freeze({
      label: '当前页能做什么',
      actionCode: 'global.assistant.page-capability',
      prompt: '请说明我在当前页面可以完成哪些操作，并给出最推荐的下一步。',
      description: '快速了解当前页面能力'
    }),
    Object.freeze({
      label: '推荐下一步',
      actionCode: 'global.assistant.next-action',
      prompt: '请根据当前页面和上下文，给出我现在最值得执行的下一步操作。',
      description: '给出当前最值得做的事情'
    }),
    Object.freeze({
      label: '结合知识库回答',
      actionCode: 'global.assistant.kb-answer',
      prompt: '请优先结合当前绑定知识库来回答，并说明需要我补充哪些上下文。',
      description: '结合知识库做问答和追问'
    })
  ])
})

export function getAiSuggestionPreset(sceneCode = '') {
  const normalized = normalizeSceneCode(sceneCode)
  return AI_SUGGESTION_PRESETS[normalized] || AI_SUGGESTION_PRESETS['global.assistant']
}

export function toAiQuickPromptList(sceneCode = '') {
  return getAiSuggestionPreset(sceneCode).map(item => ({
    label: item.label,
    prompt: item.prompt,
    actionCode: item.actionCode,
    description: item.description
  }))
}
