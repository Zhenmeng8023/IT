import Vue from 'vue'
import Router from 'vue-router'
import { normalizeURL, decode } from 'ufo'
import { interopDefault } from './utils'
import scrollBehavior from './router.scrollBehavior.js'

const _492fad9d = () => interopDefault(import('..\\pages\\company\\about.vue' /* webpackChunkName: "pages/company/about" */))
const _05e2d396 = () => interopDefault(import('..\\pages\\userpage\\peoplehome.vue' /* webpackChunkName: "pages/userpage/peoplehome" */))
const _67bc5d4d = () => interopDefault(import('..\\pages\\webhome\\webhome.vue' /* webpackChunkName: "pages/webhome/webhome" */))
const _486b520a = () => interopDefault(import('..\\pages\\userpage\\components\\Calendar.vue' /* webpackChunkName: "pages/userpage/components/Calendar" */))
const _d1ff45ae = () => interopDefault(import('..\\pages\\userpage\\components\\ContentSection.vue' /* webpackChunkName: "pages/userpage/components/ContentSection" */))
const _41535e99 = () => interopDefault(import('..\\pages\\userpage\\components\\FooterPlayer.vue' /* webpackChunkName: "pages/userpage/components/FooterPlayer" */))
const _79e57c3a = () => interopDefault(import('..\\pages\\userpage\\components\\HeaderGreeting.vue' /* webpackChunkName: "pages/userpage/components/HeaderGreeting" */))
const _25339dae = () => interopDefault(import('..\\pages\\userpage\\components\\HeatmapTracker.vue' /* webpackChunkName: "pages/userpage/components/HeatmapTracker" */))
const _2853a8da = () => interopDefault(import('..\\pages\\userpage\\components\\RecommendLinks.vue' /* webpackChunkName: "pages/userpage/components/RecommendLinks" */))
const _aed0ee96 = () => interopDefault(import('..\\pages\\index.vue' /* webpackChunkName: "pages/index" */))

const emptyFn = () => {}

Vue.use(Router)

export const routerOptions = {
  mode: 'history',
  base: '/',
  linkActiveClass: 'nuxt-link-active',
  linkExactActiveClass: 'nuxt-link-exact-active',
  scrollBehavior,

  routes: [{
    path: "/company/about",
    component: _492fad9d,
    name: "company-about"
  }, {
    path: "/userpage/peoplehome",
    component: _05e2d396,
    name: "userpage-peoplehome"
  }, {
    path: "/webhome/webhome",
    component: _67bc5d4d,
    name: "webhome-webhome"
  }, {
    path: "/userpage/components/Calendar",
    component: _486b520a,
    name: "userpage-components-Calendar"
  }, {
    path: "/userpage/components/ContentSection",
    component: _d1ff45ae,
    name: "userpage-components-ContentSection"
  }, {
    path: "/userpage/components/FooterPlayer",
    component: _41535e99,
    name: "userpage-components-FooterPlayer"
  }, {
    path: "/userpage/components/HeaderGreeting",
    component: _79e57c3a,
    name: "userpage-components-HeaderGreeting"
  }, {
    path: "/userpage/components/HeatmapTracker",
    component: _25339dae,
    name: "userpage-components-HeatmapTracker"
  }, {
    path: "/userpage/components/RecommendLinks",
    component: _2853a8da,
    name: "userpage-components-RecommendLinks"
  }, {
    path: "/",
    component: _aed0ee96,
    name: "index"
  }],

  fallback: false
}

export function createRouter (ssrContext, config) {
  const base = (config._app && config._app.basePath) || routerOptions.base
  const router = new Router({ ...routerOptions, base  })

  // TODO: remove in Nuxt 3
  const originalPush = router.push
  router.push = function push (location, onComplete = emptyFn, onAbort) {
    return originalPush.call(this, location, onComplete, onAbort)
  }

  const resolve = router.resolve.bind(router)
  router.resolve = (to, current, append) => {
    if (typeof to === 'string') {
      to = normalizeURL(to)
    }
    return resolve(to, current, append)
  }

  return router
}
