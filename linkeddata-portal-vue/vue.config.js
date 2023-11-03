const { defineConfig } = require('@vue/cli-service');

// production 生产环境   development 本地环境
let isProduction = process.env.NODE_ENV == 'production',  //判断当前的环境
    baseUrl; //定义baseUrl
if (isProduction) {
    // 生产环境
    baseUrl = process.env.VUE_APP_API  //当前就是生产环境baseUrl的地址
} else {
    baseUrl = process.env.VUE_APP_API //当前就是开发环境baseUrl的地址
}

module.exports = defineConfig({
  lintOnSave: false ,
  transpileDependencies: true,
  publicPath: "/",
	assetsDir: "static",
	outputDir: process.env.outputDir,
	// 取消map文件
	productionSourceMap: false,
  //开发服务,build后的生产模式还需nginx代理
	devServer: {
		open: false, //运行后自动打开浏览器
		port: 8880, //挂载端口
		proxy: {
			'/api': {
				target: baseUrl,
				changeOrigin: true,
				pathRewrite: {
					'^/api': ''
				}
			}
		}
	},
	chainWebpack: config => {
		config.plugin('html').tap( args => {
			args[0].title = 'SemWeb Cloud'
			return args
		} )
	}
})
