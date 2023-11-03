import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import(/* webpackChunkName: "about" */ '../views/Index.vue')
  },
  {
    path: '/DS',
    name: 'DS',
    component: () => import(/* webpackChunkName: "about" */ '../views/DSdetail.vue')
  },
  {
    path: '/resource',
    name: 'resource',
    component: () => import(/* webpackChunkName: "about" */ '../views/resource.vue')
  },
  {
    path: '/resourceDetail',
    name: 'resourceDetail',
    component: () => import(/* webpackChunkName: "about" */ '../views/resourceDetail.vue')
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

//要添加的代码
router.beforeEach((to, from, next) => {
  // chrome
  document.body.scrollTop = 0
  // firefox
  document.documentElement.scrollTop = 0
  // safari
  window.pageYOffset = 0
  next()
})

export default router
