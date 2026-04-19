function compactIsoTime(isoString) {
  return isoString.replace(/[-:TZ.]/g, '').slice(0, 14)
}

function normalizeToken(value) {
  return String(value || '')
    .trim()
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()
}

function buildDataName({ domain = 'it-ui', feature = 'smoke', role = 'guest', suffix = '' } = {}) {
  const runSeed = process.env.E2E_RUN_ID || compactIsoTime(new Date().toISOString())
  const parts = [domain, feature, role, runSeed, suffix].map(normalizeToken).filter(Boolean)
  return parts.join('__')
}

module.exports = {
  buildDataName
}
