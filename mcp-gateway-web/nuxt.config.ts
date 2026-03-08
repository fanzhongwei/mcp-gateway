/*
 * @FilePath: /mcp-gateway-web/nuxt.config.ts
 * @Author: teddy
 * @Date: 2024-02-01 01:36:34
 * @Description:  nuxt 框架配置
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-09 11:01:53
 */

export default defineNuxtConfig({
	// 禁用遥测数据收集
	telemetry: false,
	typescript: {
		// 禁用生成 *.vue 文件的 shim，禁用 TypeScript 的转换功能
		// 项目中的部分代码需要保持原生 JavaScript 格式，而不希望被 TypeScript 转换
		shim: false,
	},
	ssr: false,
	srcDir: 'src/',
	// 实验性功能
	// inlineSSRStyles 在渲染 HTML 时内联样式。目前仅在使用 Vite 时可用。 您还可以传递一个接收 Vue 组件路径并返回一个布尔值以指示是否内联该组件样式的函数。
	// inlineSSRStyles: false 提取为独立的 .css 文件
	// experimental: { inlineSSRStyles: false },
	// 页面设置
	app: {
		buildAssetsDir: '/admin/',
		rootId: 'admin',
		rootTag: 'main',
		baseURL: '/mcp-gateway',
		head: {
			title: 'MCP Gateway',
			htmlAttrs: {
				lang: 'zh-CN',
			},
			meta: [
				{
					'http-equiv': 'Pragma',
					content: 'no-cache',
				},
				{
					'http-equiv': 'Cache-Control',
					content: 'no-cache',
				},
				{
					'http-equiv': 'Expires',
					content: '0',
				},
				{
					name: 'viewport',
					content:
						'width=device-width, initial-scale=1.0,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no',
				},
				{
					hid: 'description',
					name: 'description',
					content: '',
				},
				{
					'http-equiv': 'X-UA-Compatible',
					content: 'IE=edge,chrome=1',
				},
				{
					charset: 'UTF-8',
				},
			],
			link: [
				// {
				// 	rel: 'stylesheet',
				// 	href: '',
				// },
			],
			script: [
				// {
				//  src: '',
				// },
			],
			noscript: [],
		},
	},
	// 全局 css
	css: [
		'@/assets/style/base.scss',
		'ant-design-vue/dist/reset.css',
		'remixicon/fonts/remixicon.css',
	],
	// 模块
	modules: ['@nuxtjs/tailwindcss'],
	// 路由配置：使用 hash 模式，避免后端需要配置 SPA 路由回退
	router: {
		options: {
			hashMode: true,
		},
	},
	// vue: {
	// 	compilerOptions: {
	// 		isCustomElement: tag => {
	// 			return tag === 'a-extract-style'
	// 		},
	// 	},
	// },
	// 环境变量
	// 使用方法：const runtimeConfig = useRuntimeConfig()
	runtimeConfig: {
		public: {
			// 自定义版本号
			version: '2026.1.30',
		},
	},
	// vite: {
	// 	css: {
	// 		preprocessorOptions: {
	// 			less: {
	// 				javascriptEnabled: true,
	// 			},
	// 		},
	// 	},
	// },
	nitro: {
		// 开发服务器代理：仅代理 /mcp-gateway/api，跳过静态资源（如 /mcp-gateway/admin/）
		devProxy: {
			'/mcp-gateway/api': {
				target: 'http://localhost:8888/mcp-gateway/api',
				changeOrigin: true,
				// 如果要代理 websockets，ws: true/false
				// 如果要验证 SSL 证书，secure: true/false
			},
			'/aliyunOSS': {
				// 要使用 URL 模块解析的 URL 字符串，target
				target: 'https://rysc.oss-cn-beijing.aliyuncs.com',
				// 将主机标头的来源更改为目标 URL， (虚拟主机站点)changeOrigin，Default: false
				changeOrigin: true,
				// 如果要代理 websockets，ws: true/false
				// 如果要验证 SSL 证书，secure: true/false
			},
		},
	},
})
