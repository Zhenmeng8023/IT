function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function decodeHtmlEntities(text) {
  return String(text || '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
}

function normalizeLineBreaks(text) {
  return String(text || '').replace(/\r\n?/g, '\n')
}

function collapseBlankLines(text) {
  return String(text || '')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function stripTags(text) {
  return String(text || '').replace(/<[^>]+>/g, ' ')
}

function sanitizeHtml(html) {
  return String(html || '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/\son[a-z]+\s*=\s*(".*?"|'.*?'|[^\s>]+)/gi, '')
    .replace(/\s(href|src)\s*=\s*(["'])\s*javascript:[^"']*\2/gi, ' $1="#"')
    .replace(/\s(href|src)\s*=\s*javascript:[^\s>]+/gi, ' $1="#"')
}

function toTextValue(input) {
  if (input === null || input === undefined) return ''

  if (typeof input === 'string') {
    return input
  }

  if (Array.isArray(input)) {
    return input.map(item => toTextValue(item)).filter(Boolean).join(' ')
  }

  if (typeof input === 'object') {
    const keys = ['content', 'previewContent', 'html', 'value', 'text', 'body', 'summary', 'raw']
    for (const key of keys) {
      if (typeof input[key] === 'string' && input[key].trim()) {
        return input[key]
      }
    }

    if (Array.isArray(input.ops)) {
      return deltaToText(input)
    }

    try {
      return JSON.stringify(input)
    } catch (e) {
      return String(input)
    }
  }

  return String(input)
}

function normalizeRichSource(input) {
  const raw = normalizeLineBreaks(toTextValue(input)).trim()
  if (!raw) return ''

  if (raw.startsWith('{') || raw.startsWith('[')) {
    try {
      const parsed = JSON.parse(raw)
      if (parsed && typeof parsed === 'object') {
        return normalizeLineBreaks(toTextValue(parsed)).trim()
      }
    } catch (e) {
      // Not JSON, keep raw text.
    }
  }

  return raw
}

function deltaToText(delta) {
  if (!delta || !Array.isArray(delta.ops)) return ''
  let result = ''

  delta.ops.forEach(op => {
    if (!op) return

    if (typeof op.insert === 'string') {
      result += op.insert
      return
    }

    if (op.insert && typeof op.insert === 'object') {
      if (op.insert.image) {
        result += `\n![image](${op.insert.image})\n`
        return
      }
      if (op.insert.video) {
        result += `\n[video](${op.insert.video})\n`
        return
      }
    }

    result += '\n'
  })

  return result
}

function inlineMarkdown(text) {
  const escaped = escapeHtml(decodeHtmlEntities(String(text || '')))
  return escaped
    .replace(/!\[([^\]]*)\]\((https?:\/\/[^\s)]+)\)/g, '<img alt="$1" src="$2">')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/~~(.+?)~~/g, '<s>$1</s>')
    .replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
    .replace(/(^|[^_])_([^_\n]+)_(?!_)/g, '$1<em>$2</em>')
}

function markdownToHtml(source, emptyText = '暂无内容') {
  const raw = collapseBlankLines(normalizeLineBreaks(source).trim())
  if (!raw) {
    return `<div class="empty-rich-content">${escapeHtml(emptyText)}</div>`
  }

  const lines = raw.split('\n')
  const blocks = []
  let i = 0

  while (i < lines.length) {
    const line = String(lines[i] || '')
    const trimmed = line.trim()

    if (!trimmed) {
      i += 1
      continue
    }

    if (/^```/.test(trimmed)) {
      const codeLines = []
      i += 1
      while (i < lines.length && !/^```/.test(String(lines[i] || '').trim())) {
        codeLines.push(lines[i])
        i += 1
      }
      if (i < lines.length && /^```/.test(String(lines[i] || '').trim())) {
        i += 1
      }
      blocks.push(`<pre><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`)
      continue
    }

    if (/^#{1,6}\s+/.test(trimmed)) {
      const level = trimmed.match(/^#+/)[0].length
      const text = trimmed.replace(/^#{1,6}\s+/, '')
      blocks.push(`<h${level}>${inlineMarkdown(text)}</h${level}>`)
      i += 1
      continue
    }

    if (/^([-*_])\1{2,}$/.test(trimmed)) {
      blocks.push('<hr>')
      i += 1
      continue
    }

    if (/^\s*>\s+/.test(trimmed)) {
      const quoteLines = []
      while (i < lines.length && /^\s*>\s+/.test(String(lines[i] || '').trim())) {
        quoteLines.push(String(lines[i] || '').trim().replace(/^\s*>\s+/, ''))
        i += 1
      }
      blocks.push(`<blockquote>${quoteLines.map(part => inlineMarkdown(part)).join('<br>')}</blockquote>`)
      continue
    }

    if (/^\s*[-*+]\s+/.test(trimmed)) {
      const items = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(String(lines[i] || '').trim())) {
        items.push(String(lines[i] || '').trim().replace(/^\s*[-*+]\s+/, ''))
        i += 1
      }
      blocks.push(`<ul>${items.map(item => `<li>${inlineMarkdown(item)}</li>`).join('')}</ul>`)
      continue
    }

    if (/^\s*\d+\.\s+/.test(trimmed)) {
      const items = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(String(lines[i] || '').trim())) {
        items.push(String(lines[i] || '').trim().replace(/^\s*\d+\.\s+/, ''))
        i += 1
      }
      blocks.push(`<ol>${items.map(item => `<li>${inlineMarkdown(item)}</li>`).join('')}</ol>`)
      continue
    }

    if (looksLikeMarkdownTableRow(trimmed) && isMarkdownTableSeparator(lines[i + 1])) {
      const headers = parseMarkdownTableCells(trimmed)
      i += 2
      const rows = []
      while (i < lines.length) {
        const rowLine = String(lines[i] || '').trim()
        if (!rowLine || !looksLikeMarkdownTableRow(rowLine)) break
        if (isMarkdownTableSeparator(rowLine)) {
          i += 1
          continue
        }
        rows.push(parseMarkdownTableCells(rowLine))
        i += 1
      }

      const headHtml = `<thead><tr>${headers.map(cell => `<th>${inlineMarkdown(cell)}</th>`).join('')}</tr></thead>`
      const bodyHtml = rows.length
        ? `<tbody>${rows.map(row => `<tr>${headers.map((_, idx) => `<td>${inlineMarkdown(row[idx] || '')}</td>`).join('')}</tr>`).join('')}</tbody>`
        : ''
      blocks.push(`<div class="rich-table-wrap"><table>${headHtml}${bodyHtml}</table></div>`)
      continue
    }

    const paragraphLines = [trimmed]
    i += 1
    while (i < lines.length) {
      const next = String(lines[i] || '').trim()
      if (!next) {
        i += 1
        break
      }
      if (isSpecialMarkdownLine(lines[i], lines[i + 1])) {
        break
      }
      paragraphLines.push(next)
      i += 1
    }

    blocks.push(`<p>${paragraphLines.map(part => inlineMarkdown(part)).join('<br>')}</p>`)
  }

  return blocks.join('')
}

function htmlToMarkdownSource(html) {
  let value = collapseBlankLines(normalizeLineBreaks(sanitizeHtml(html)))
  if (!value) return ''

  value = value
    .replace(/<\s*br\s*\/?\s*>/gi, '\n')
    .replace(/<\s*\/\s*(p|div|section|article|header|footer|aside|main|blockquote|tr)\s*>/gi, '\n\n')
    .replace(/<\s*(p|div|section|article|header|footer|aside|main|blockquote|tr)[^>]*>/gi, '')
    .replace(/<\s*\/\s*(ul|ol|table|thead|tbody)\s*>/gi, '\n')
    .replace(/<\s*(ul|ol|table|thead|tbody)[^>]*>/gi, '')
    .replace(/<\s*li[^>]*>/gi, '\n- ')
    .replace(/<\s*\/\s*li\s*>/gi, '\n')
    .replace(/<\s*h([1-6])[^>]*>([\s\S]*?)<\s*\/\s*h\1\s*>/gi, (_, level, inner) => {
      return `\n\n${'#'.repeat(Number(level))} ${stripTags(inner)}\n\n`
    })
    .replace(/<\s*blockquote[^>]*>/gi, '\n> ')
    .replace(/<\s*\/\s*blockquote\s*>/gi, '\n\n')
    .replace(/<\s*strong[^>]*>|<\s*b[^>]*>/gi, '**')
    .replace(/<\s*\/\s*strong\s*>|<\s*\/\s*b\s*>/gi, '**')
    .replace(/<\s*em[^>]*>|<\s*i[^>]*>/gi, '*')
    .replace(/<\s*\/\s*em\s*>|<\s*\/\s*i\s*>/gi, '*')
    .replace(/<\s*code[^>]*>/gi, '`')
    .replace(/<\s*\/\s*code\s*>/gi, '`')
    .replace(/<\s*pre[^>]*>/gi, '\n```\n')
    .replace(/<\s*\/\s*pre\s*>/gi, '\n```\n')
    .replace(/<\s*img\b[^>]*>/gi, tag => {
      const srcMatch = tag.match(/\bsrc\s*=\s*["']([^"']+)["']/i)
      const altMatch = tag.match(/\balt\s*=\s*["']([^"']*)["']/i)
      const src = srcMatch ? srcMatch[1] : ''
      const alt = altMatch ? altMatch[1] : 'image'
      return src ? `\n![${alt}](${src})\n` : ''
    })
    .replace(/<\s*a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>([\s\S]*?)<\s*\/\s*a\s*>/gi, (_, href, inner) => {
      const text = stripTags(inner).trim() || href
      return `[${text}](${href})`
    })
    .replace(/<[^>]+>/g, ' ')

  value = decodeHtmlEntities(value)
  value = value
    .replace(/\u00a0/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()

  return value
}

function looksLikeHtml(text) {
  return /<\/?[a-z][\s\S]*>/i.test(String(text || ''))
}

function looksLikeEscapedHtml(text) {
  return /&lt;\/?[a-z][\s\S]*?&gt;/i.test(String(text || ''))
}

function isMarkdownTableSeparator(line) {
  const cells = parseMarkdownTableCells(line)
  return cells.length > 0 && cells.every(cell => /^:?-{3,}:?$/.test(cell.replace(/\s+/g, '')))
}

function parseMarkdownTableCells(line) {
  return String(line || '')
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map(cell => cell.trim())
}

function looksLikeMarkdownTableRow(line) {
  const text = String(line || '').trim()
  return text.includes('|') && parseMarkdownTableCells(text).length >= 2
}

function isSpecialMarkdownLine(line, nextLine) {
  const text = String(line || '').trim()
  if (!text) return true
  if (/^```/.test(text)) return true
  if (/^([-*_])\1{2,}$/.test(text)) return true
  if (/^#{1,6}\s+/.test(text)) return true
  if (/^\s*>\s+/.test(text)) return true
  if (/^\s*[-*+]\s+/.test(text)) return true
  if (/^\s*\d+\.\s+/.test(text)) return true
  if (looksLikeMarkdownTableRow(text) && isMarkdownTableSeparator(nextLine)) return true
  return false
}

export function renderRichContent(source, options = {}) {
  const emptyText = options.emptyText || '暂无内容'
  const raw = normalizeRichSource(source)
  if (!raw) {
    return `<div class="empty-rich-content">${escapeHtml(emptyText)}</div>`
  }

  if (looksLikeEscapedHtml(raw)) {
    return markdownToHtml(htmlToMarkdownSource(decodeHtmlEntities(raw)), emptyText)
  }

  if (looksLikeHtml(raw)) {
    return markdownToHtml(htmlToMarkdownSource(raw), emptyText)
  }

  return markdownToHtml(raw, emptyText)
}

export function richContentToPlainText(source) {
  const raw = normalizeRichSource(source)
  if (!raw) return ''

  const text = looksLikeHtml(raw) || looksLikeEscapedHtml(raw)
    ? htmlToMarkdownSource(looksLikeEscapedHtml(raw) ? decodeHtmlEntities(raw) : raw)
    : raw

  return decodeHtmlEntities(text)
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '$1')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/__(.+?)__/g, '$1')
    .replace(/~~(.+?)~~/g, '$1')
    .replace(/(^|[\n\r])\s*#{1,6}\s+/g, '$1')
    .replace(/(^|[\n\r])\s*[-*+]\s+/g, '$1')
    .replace(/(^|[\n\r])\s*\d+\.\s+/g, '$1')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function normalizeRichContent(source) {
  return normalizeRichSource(source)
}
