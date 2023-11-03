import { request, post } from './util/axios'
import time from './util/time'
import TheHeader from './components/TheHeader.vue'
import TheFooter from './components/TheFooter.vue'
import tool from './util/tool'

export default {
  install(app) {
    //挂载全局对象
    app.config.globalProperties.$http = request
    app.config.globalProperties.$post = post
    app.config.globalProperties.$TOOL = tool;

    //注册全局组件
		app.component('TheHeader', TheHeader);
		app.component('TheFooter', TheFooter);
    app.directive('time', time)
  }
}