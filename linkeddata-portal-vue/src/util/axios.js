import axios from 'axios'
import qs from 'qs'
import { ElMessage } from "element-plus";

const handleReject = ({ statusCode, message, data }, showMessage = true) => {
	showMessage && ElMessage.error(message)
	const error = new Error(message)
	error.data = data
	error.statusCode = statusCode
	return Promise.reject(error)
  }
  const createInstance = (isForm, baseURL = process.env.VUE_APP_BASE_API) => {
	const instance = axios.create()
	instance.defaults.timeout = 10000
	instance.defaults.baseURL = baseURL
	if (isForm) {
    instance.defaults.transformRequest = [_ => qs.stringify(_)]
  }
	// 请求拦截
	instance.interceptors.request.use(
		config => {
			config.headers['X-Requested-With'] = 'XMLHttpRequest'
			return config
		},
		error => {
			return Promise.reject(error)
		}
	)
	// 响应拦截
	instance.interceptors.response.use(
		res => {
			const { data } = res
			return data;
		},
		e => {
			const res = e.response
			const { data, status } = res || {}
			let msg = ''
			if (status === 404 || status === undefined) {
				msg = res ? data.squeezeOut || e.message : '请求失败，请稍候重试'
			}
			return handleReject({ message: msg, statusCode: status, data })
		}
	)
  
	return instance
  }
  
  export { createInstance }
  
  export const request = createInstance()
  
  export const formRequest = createInstance(true)
  
  export const post = (...args) => formRequest.post(...args)