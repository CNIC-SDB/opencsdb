import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import * as ElIcons from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

import './util/rem'
import tool from './tool'

const app = createApp(App)
Object.keys(ElIcons).forEach(key => {
  app.component(key, ElIcons[key])
})


app.use(router)
app.use(ElementPlus)
app.use(tool)
app.mount('#app')