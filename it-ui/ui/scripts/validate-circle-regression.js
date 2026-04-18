const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')

const circleHomePath = path.join(root, 'pages', 'Z_circlepage', 'circlehome.vue')
const circleDetailPath = path.join(root, 'pages', 'Z_circledetail', 'circledetail.vue')
const circleManagePath = path.join(root, 'pages', 'f_circlemanage', 'circlemanage', 'circlemanage.vue')
const circleAuditPath = path.join(root, 'pages', 'f_circlemanage', 'circleaudit', 'circleaudit.vue')
const blogPagePath = path.join(root, 'pages', 'Z_blogpage', 'blogpage.vue')

const mojibakeTokens = [
  '\uFFFD',
  '\u951f',
  '\u9225'
]

function read(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function assertNoMojibake(source, label) {
  mojibakeTokens.forEach((token) => {
    const decoded = JSON.parse(`"${token}"`)
    assert(!source.includes(decoded), `${label} contains mojibake token: ${token}`)
  })
}

function assertContains(source, needle, message) {
  assert(source.includes(needle), message)
}

function assertContainsAny(source, needles, message) {
  assert(needles.some((needle) => source.includes(needle)), message)
}

function testPublicCircleFlowContracts() {
  const home = read(circleHomePath)
  const detail = read(circleDetailPath)

  assertNoMojibake(home, 'circlehome.vue')
  assertNoMojibake(detail, 'circledetail.vue')

  assertContains(home, '@click.native="goToPostDetail(post)"', 'public list should navigate with full post payload')
  assertContains(home, 'path: `/circle/${post.id}`', 'public list should route to circle detail page')
  assertContains(home, 'query: post.circleId ? { circleId: post.circleId } : {}', 'public list should carry circleId in route query')
  assertContains(home, 'const payload = await GetAllCirclePosts()', 'public list should fetch posts from backend API')
  assertContains(home, 'this.allPosts = await this.enrichAuthors(normalizedPosts)', 'public list should enrich author info before rendering')
  assertContains(home, '\u5f00\u53d1\u8005\u5708\u5b50', 'public page title should render expected Chinese copy')
  assertContains(home, '\u8fdb\u5165\u8ba8\u8bba', 'public card action copy should render expected Chinese text')
  assertContains(home, '\u6682\u65e0\u5e16\u5b50\uff0c\u5feb\u53bb\u53d1\u5e16\u5427\uff01', 'public empty-state copy should render expected Chinese text')
  assert(!home.includes('useMockData'), 'public list should not fallback to mock data')

  assertContains(detail, 'const postData = await getCirclePostDetail(this.postId)', 'public detail should request post detail from backend')
  assertContains(detail, 'const commentsData = await getCirclePostComments(this.postId)', 'public detail should request comments from backend')
  assertContains(detail, '\u8bf7\u5148\u767b\u5f55\u540e\u518d\u53d1\u8868\u8bc4\u8bba', 'public detail should block unauthenticated top-level comments')
  assertContains(detail, '\u8bf7\u5148\u767b\u5f55\u540e\u518d\u56de\u590d', 'public detail should block unauthenticated replies')
  assertContains(detail, '!newComment.trim() || !canSubmitComment || !userId', 'public detail should disable top-level submit without login')
  assertContains(detail, '!replyContent.trim() || !canSubmitComment || !userId', 'public detail should disable reply submit without login')
  assertContains(detail, 'parentCommentId: this.postId', 'public detail should submit top-level comments against current post')
}

function assertManageFlowContract(source, label) {
  assertNoMojibake(source, label)

  assertContains(source, "detailTab = 'posts'", `${label} should open posts tab when entering post management`)
  assertContains(source, 'this.loadPostList(circle.id)', `${label} should load posts for selected circle`)
  assertContains(source, '@click="handleViewPost(scope.row)"', `${label} should support viewing post details`)
  assertContains(source, '@click="handleApprovePost(scope.row)"', `${label} should expose approve action for pending posts`)
  assertContains(source, 'await approveCirclePost(post.id)', `${label} should call approve post API`)
  assertContains(source, 'this.loadPostList(this.currentCircle.id)', `${label} should refresh post list after approval`)

  assertContains(source, 'page: this.pagination.currentPage', `${label} list params should use one-based page`)
  assertContains(source, 'pageSize: this.pagination.pageSize', `${label} list params should carry pageSize`)
  assertContainsAny(
    source,
    ['status: this.filterForm.status', 'params.status = this.filterForm.status'],
    `${label} list params should include status filter`
  )
  assertContainsAny(
    source,
    ['privacy: this.filterForm.privacy', 'params.privacy = this.filterForm.privacy'],
    `${label} list params should include privacy filter`
  )

  assert(!source.includes('toggleCircleRecommend'), `${label} should not expose deprecated recommend API`)
  assert(!source.includes('handleToggleRecommend'), `${label} should not expose deprecated recommend action`)
}

function testManageCircleFlowContracts() {
  const manage = read(circleManagePath)
  const audit = read(circleAuditPath)

  assertManageFlowContract(manage, 'circlemanage.vue')
  assertManageFlowContract(audit, 'circleaudit.vue')
}

function testBlogMainlineContract() {
  const blog = read(blogPagePath)

  assertNoMojibake(blog, 'blogpage.vue')
  assertContains(blog, 'const result = await fetchBlogFeed({', 'blog list should load from unified blog feed API')
  assertContains(blog, 'this.$router.push(`/blog/${post.id}`)', 'blog page should route to blog detail')
  assertContains(blog, 'this.$router.push(`/other/${author.id}`)', 'blog page should route to author profile')
}

testPublicCircleFlowContracts()
testManageCircleFlowContracts()
testBlogMainlineContract()

console.log('Circle/Blog regression validation passed')