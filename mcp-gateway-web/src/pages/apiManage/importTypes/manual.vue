<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/importTypes/manual.vue
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: 手动导入接口页面
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-23
-->
<script setup lang="ts">
import { message } from 'ant-design-vue'
import axios from 'axios'
import { fetchProxyRequest } from '~/fetch/http'
import {
	generateId,
	saveApi,
	type ApiData,
	convertJsonParamsToJsonString,
} from '~/utils/apiSaveUtils'
import JsonBodyEditor from './JsonBodyEditor.vue'
import type { JsonParamNode } from '~/utils/jsonParamTypes'
import {
	useTableScroll,
	type TableScrollConfig,
} from '~/utils/tableScrollUtils'

// 接收父组件传递的业务系统信息和环境信息
const props = defineProps<{
	systemId?: string
	systemName?: string
	selectedEnv?: any | null
	editingApiData?: any | null
}>()

// 定义事件
const emit = defineEmits<{
	back: []
	success: [] // 保留用于向后兼容，但不再使用
	saved: [] // 保存成功事件（编辑模式和新增模式），不关闭页面
}>()

// HTTP 方法选项
const httpMethods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']

// API 定义表单
const apiForm = reactive({
	method: 'GET',
	path: '',
	name: '',
	description: '',
})

// 当前激活的标签页
const activeTab = ref('params')

// 标签页配置
const tabs = reactive([
	{ key: 'params', label: 'Params', badge: null as number | null },
	{ key: 'body', label: 'Body', badge: null as number | null },
	{ key: 'headers', label: 'Headers', badge: null as number | null },
	{ key: 'cookies', label: 'Cookies', badge: null as number | null },
])

// Query 参数列表
const queryParams = ref<any[]>([])

// Headers 列表
const headers = ref<any[]>([])

// Body form-data 列表
const formDataParams = ref<any[]>([])

// Body url-encoded 列表
const urlEncodedParams = ref<any[]>([])

// Body 配置
const bodyConfig = reactive({
	type: 'json', // json, form-data, x-www-form-urlencoded, raw
	jsonParams: [] as JsonParamNode[], // JSON参数树形结构
	raw: '',
})

// Cookies 列表
const cookies = ref<any[]>([])

// 响应配置
const responseConfig = reactive({
	validateResponse: false,
	successStatus: '200',
})

// 响应数据
const responseData = ref<any>(null)

// 响应状态
const responseStatus = ref<number | null>(null)
const responseHeaders = ref<Record<string, string>>({})
const responseTime = ref<number>(0)

// 响应内容展开/收起状态（编辑时默认收起，有响应数据时默认展开）
const responseExpanded = ref(false)

// JsonBodyEditor 组件引用
const jsonBodyEditorRef = ref<InstanceType<typeof JsonBodyEditor> | null>(null)

// 切换响应内容展开/收起
const toggleResponseExpanded = () => {
	responseExpanded.value = !responseExpanded.value
}

// 所有表格的配置
const paramsTableContainerRef = ref<HTMLElement | null>(null)
const paramsTableScrollY = ref<number | undefined>(undefined)

const formDataTableContainerRef = ref<HTMLElement | null>(null)
const formDataTableScrollY = ref<number | undefined>(undefined)

const urlEncodedTableContainerRef = ref<HTMLElement | null>(null)
const urlEncodedTableScrollY = ref<number | undefined>(undefined)

const headersTableContainerRef = ref<HTMLElement | null>(null)
const headersTableScrollY = ref<number | undefined>(undefined)

const cookiesTableContainerRef = ref<HTMLElement | null>(null)
const cookiesTableScrollY = ref<number | undefined>(undefined)

// 所有表格配置数组
const tableScrollConfigs: TableScrollConfig[] = [
	{ containerRef: paramsTableContainerRef, scrollY: paramsTableScrollY },
	{
		containerRef: formDataTableContainerRef,
		scrollY: formDataTableScrollY,
		heightAdjustment: 20,
	},
	{
		containerRef: urlEncodedTableContainerRef,
		scrollY: urlEncodedTableScrollY,
		heightAdjustment: 20,
	},
	{ containerRef: headersTableContainerRef, scrollY: headersTableScrollY },
	{ containerRef: cookiesTableContainerRef, scrollY: cookiesTableScrollY },
]

// 使用表格滚动工具
const {
	calculate: calculateAllTablesScrollY,
	initResizeObserver,
	cleanup: cleanupTableScroll,
	initWindowResize,
} = useTableScroll(tableScrollConfigs)

// 监听响应区域展开状态变化，重新计算所有表格高度
watch(responseExpanded, () => {
	// 延迟计算，等待响应区域展开/收起动画完成（如果有）
	// 使用多次计算确保准确
	setTimeout(() => {
		calculateAllTablesScrollY()
	}, 100)
	setTimeout(() => {
		calculateAllTablesScrollY()
	}, 350) // 略大于 CSS transition 时间 0.3s
})

// 监听标签页切换，重新计算所有表格高度
watch(activeTab, () => {
	// 延迟计算，确保 DOM 更新完成
	setTimeout(() => {
		calculateAllTablesScrollY()
	}, 50)
})

// 监听 body 类型切换（form-data、x-www-form-urlencoded 等），重新计算表格高度
watch(
	() => bodyConfig.type,
	() => {
		setTimeout(() => {
			calculateAllTablesScrollY()
		}, 50)
	},
)

// 监听窗口大小变化和容器大小变化
onMounted(() => {
	// 延迟计算，确保 DOM 完全渲染
	setTimeout(() => {
		calculateAllTablesScrollY()
	}, 100)

	// 初始化窗口大小监听
	initWindowResize()

	// 初始化 ResizeObserver，监听所有表格容器和父容器的大小变化
	nextTick(() => {
		const additionalContainers: HTMLElement[] = []
		// 获取父容器（request-tabs-container）
		if (paramsTableContainerRef.value) {
			const tabsContainer = paramsTableContainerRef.value.closest(
				'.request-tabs-container',
			)
			if (tabsContainer) {
				additionalContainers.push(tabsContainer as HTMLElement)
			}
		}
		initResizeObserver(additionalContainers)
	})
})

onUnmounted(() => {
	cleanupTableScroll()
})

// 保存最后一次请求的参数，用于重新下载
const lastRequestParams = ref<{
	url: string
	method: string
	headers: Record<string, string>
	body: any
	crossOrigin: boolean
} | null>(null)

// 判断是否为文件下载响应
const isFileDownload = computed(() => {
	const contentDisposition =
		responseHeaders.value['content-disposition'] ||
		responseHeaders.value['Content-Disposition'] ||
		''
	return (
		contentDisposition.toLowerCase().includes('attachment') ||
		contentDisposition.toLowerCase().includes('filename')
	)
})

// 从响应头中提取文件名
const getDownloadFileName = computed(() => {
	const contentDisposition =
		responseHeaders.value['content-disposition'] ||
		responseHeaders.value['Content-Disposition'] ||
		''

	if (!contentDisposition) {
		return 'download'
	}

	// 优先提取 filename*=UTF-8''xxx 格式（RFC 5987）
	const filenameStarMatch = contentDisposition.match(/filename\*=([^;]+)/i)
	if (filenameStarMatch && filenameStarMatch[1]) {
		let filename = filenameStarMatch[1].trim()
		// 处理 UTF-8 编码的文件名 (filename*=UTF-8''xxx)
		if (filename.startsWith("UTF-8''")) {
			try {
				filename = decodeURIComponent(filename.substring(7))
				if (filename) {
					return filename
				}
			} catch (e) {
				// 解码失败，继续尝试其他方式
			}
		} else {
			// 尝试直接解码
			try {
				filename = decodeURIComponent(filename)
				if (filename) {
					return filename
				}
			} catch (e) {
				// 解码失败，继续尝试其他方式
			}
		}
	}

	// 其次提取 filename="xxx" 或 filename=xxx 格式
	const filenameMatch = contentDisposition.match(
		/filename[^=]*=((['"]).*?\2|[^;\n]+)/i,
	)
	if (filenameMatch && filenameMatch[1]) {
		const filename = filenameMatch[1].replace(/['"]/g, '').trim()
		if (filename) {
			return filename
		}
	}

	return 'download'
})

// 请求加载状态
const requestLoading = ref(false)

// 判断是否跨域
const isCrossOrigin = (url: string): boolean => {
	try {
		const targetUrl = new URL(url)
		const currentOrigin = window.location.origin
		return targetUrl.origin !== currentOrigin
	} catch (error) {
		// URL 格式错误，默认认为跨域
		return true
	}
}

// 构建请求URL
const buildRequestUrl = (baseUrl: string, path: string): string => {
	// 确保baseUrl不以/结尾，path以/开头
	const cleanBaseUrl = baseUrl.replace(/\/$/, '')
	const cleanPath = path.startsWith('/') ? path : `/${path}`

	// 构建查询参数
	const queryString = queryParams.value
		.filter(param => param.key && param.key.trim())
		.map(param => {
			const key = encodeURIComponent(param.key)
			const value = encodeURIComponent(param.value || '')
			return `${key}=${value}`
		})
		.join('&')

	const url = `${cleanBaseUrl}${cleanPath}`
	return queryString ? `${url}?${queryString}` : url
}

// 构建请求Headers
const buildRequestHeaders = (): Record<string, string> => {
	const requestHeaders: Record<string, string> = {}

	// 添加自定义Headers
	headers.value.forEach(header => {
		if (header.key && header.key.trim()) {
			requestHeaders[header.key] = header.value || ''
		}
	})

	// 根据Body类型设置Content-Type
	if (['POST', 'PUT', 'PATCH'].includes(apiForm.method)) {
		if (!requestHeaders['Content-Type'] && !requestHeaders['content-type']) {
			if (bodyConfig.type === 'json') {
				requestHeaders['Content-Type'] = 'application/json'
			} else if (bodyConfig.type === 'x-www-form-urlencoded') {
				requestHeaders['Content-Type'] = 'application/x-www-form-urlencoded'
			} else if (bodyConfig.type === 'form-data') {
				// form-data 不设置Content-Type，让浏览器自动设置boundary
				delete requestHeaders['Content-Type']
			} else if (bodyConfig.type === 'raw') {
				requestHeaders['Content-Type'] = 'text/plain'
			}
		}
	}

	// 添加Cookies
	const cookieString = cookies.value
		.filter(cookie => cookie.key && cookie.key.trim())
		.map(cookie => `${cookie.key}=${cookie.value || ''}`)
		.join('; ')

	if (cookieString) {
		requestHeaders.Cookie = cookieString
	}

	return requestHeaders
}

// 构建请求Body
const buildRequestBody = (): any => {
	if (!['POST', 'PUT', 'PATCH'].includes(apiForm.method)) {
		return undefined
	}

	switch (bodyConfig.type) {
		case 'json': {
			// 优先使用 JsonBodyEditor 中手动输入的 JSON 字符串
			if (jsonBodyEditorRef.value) {
				const jsonString = jsonBodyEditorRef.value.getJsonString()
				if (jsonString && jsonString.trim()) {
					try {
						// 解析 JSON 字符串为对象
						return JSON.parse(jsonString)
					} catch (error) {
						// 如果解析失败，返回原始字符串（虽然不应该发生）
						console.error('JSON 解析失败:', error)
						return jsonString
					}
				}
			}
			// 如果没有手动输入过，使用工具方法将 jsonParams 转换成 JSON
			if (bodyConfig.jsonParams && bodyConfig.jsonParams.length > 0) {
				const jsonString = convertJsonParamsToJsonString(bodyConfig.jsonParams)
				try {
					return JSON.parse(jsonString)
				} catch (error) {
					console.error('JSON 解析失败:', error)
					return {}
				}
			}
			return undefined
		}

		case 'form-data': {
			const formData = new FormData()
			formDataParams.value.forEach(param => {
				if (param.key && param.key.trim()) {
					if (param.type === 'file' && param.value) {
						// 文件上传需要特殊处理，这里暂时只处理文本
						formData.append(param.key, param.value)
					} else {
						formData.append(param.key, param.value || '')
					}
				}
			})
			return formData
		}

		case 'x-www-form-urlencoded': {
			const params = new URLSearchParams()
			urlEncodedParams.value.forEach(param => {
				if (param.key && param.key.trim()) {
					params.append(param.key, param.value || '')
				}
			})
			return params.toString()
		}

		case 'raw':
			return bodyConfig.raw

		default:
			return undefined
	}
}

// 发送请求
const handleSend = async () => {
	if (!apiForm.path) {
		message.warning('请输入接口路径')
		return
	}

	if (!props.selectedEnv || !props.selectedEnv.baseUrl) {
		message.warning('请选择环境')
		return
	}

	requestLoading.value = true
	responseData.value = null
	responseStatus.value = null
	responseHeaders.value = {}
	responseTime.value = 0
	// 发送请求时默认展开响应内容
	responseExpanded.value = true

	const startTime = Date.now()

	try {
		const url = buildRequestUrl(props.selectedEnv.baseUrl, apiForm.path)
		const headers = buildRequestHeaders()
		const data = buildRequestBody()

		// 判断是否跨域
		const crossOrigin = isCrossOrigin(url)

		// 保存请求参数，用于后续下载
		lastRequestParams.value = {
			url,
			method: apiForm.method,
			headers: { ...headers },
			body: data,
			crossOrigin,
		}

		let response: any

		if (crossOrigin) {
			// 跨域请求，使用后端代理接口
			// 处理特殊类型的 body（FormData、URLSearchParams 等）
			let proxyBody: any = data
			if (data instanceof FormData) {
				// FormData 转换为对象（注意：文件上传需要特殊处理）
				const formDataObj: Record<string, any> = {}
				data.forEach((value, key) => {
					formDataObj[key] = value
				})
				proxyBody = formDataObj
				// 移除 Content-Type，让后端自动设置
				if (headers['Content-Type']) {
					delete headers['Content-Type']
				}
			} else if (data instanceof URLSearchParams) {
				// URLSearchParams 转换为字符串
				proxyBody = data.toString()
			}

			const proxyResponse = await fetchProxyRequest({
				url,
				method: apiForm.method,
				headers: headers as Record<string, any>,
				body: proxyBody,
			})

			// 后端直接返回原始响应，已经是 Blob 格式（因为设置了 responseType: 'blob'）
			response = {
				status: proxyResponse.status,
				statusText: proxyResponse.statusText,
				headers: proxyResponse.headers as Record<string, string>,
				data: proxyResponse.data, // 已经是 Blob 格式
			}
		} else {
			// 同源请求，直接发送
			const config: any = {
				method: apiForm.method.toLowerCase(),
				url,
				headers,
				timeout: 30000,
				validateStatus: () => true, // 不抛出错误，接收所有状态码
				responseType: 'blob', // 使用 blob 接收响应，支持文件下载
			}

			// 只有POST/PUT/PATCH等需要body的方法才添加data
			if (data !== undefined) {
				config.data = data
			}

			response = await axios(config)
		}

		responseTime.value = Date.now() - startTime
		responseStatus.value = response.status
		responseHeaders.value = response.headers as Record<string, string>

		// 检查是否为文件下载响应
		const contentDisposition =
			responseHeaders.value['content-disposition'] ||
			responseHeaders.value['Content-Disposition'] ||
			''
		const contentType =
			responseHeaders.value['content-type'] ||
			responseHeaders.value['Content-Type'] ||
			''
		const isAttachment =
			contentDisposition.toLowerCase().includes('attachment') ||
			contentDisposition.toLowerCase().includes('filename')

		// 判断是否为文件下载：有 Content-Disposition 头，或者响应是 Blob 且 Content-Type 不是 JSON/XML/HTML/Text
		if (
			isAttachment ||
			(response.data instanceof Blob &&
				!contentType.includes('application/json') &&
				!contentType.includes('application/xml') &&
				!contentType.includes('text/html') &&
				!contentType.includes('text/plain') &&
				!contentType.includes('application/x-www-form-urlencoded'))
		) {
			// 文件下载响应，不存储内容，只显示下载提示
			responseData.value = null
		} else {
			// 普通响应，尝试将 Blob 转换为文本
			let rawData = response.data

			// 如果响应是 Blob，尝试转换为文本（用于 JSON 等文本格式）
			if (rawData instanceof Blob) {
				// 异步读取 Blob 内容
				rawData
					.text()
					.then((text: string) => {
						if (text.trim().startsWith('{') || text.trim().startsWith('[')) {
							try {
								responseData.value = JSON.parse(text)
							} catch (e) {
								responseData.value = text
							}
						} else {
							responseData.value = text
						}
					})
					.catch(() => {
						// 如果读取失败，可能是二进制数据，不显示内容
						responseData.value = null
					})
			} else if (typeof rawData === 'string' && rawData.trim()) {
				// 尝试解析 JSON 字符串
				if (rawData.trim().startsWith('{') || rawData.trim().startsWith('[')) {
					try {
						rawData = JSON.parse(rawData)
					} catch (e) {
						// 解析失败，保持原始字符串
					}
				}
				responseData.value = rawData
			} else {
				responseData.value = rawData
			}
		}

		// 根据配置验证响应
		if (responseConfig.validateResponse) {
			const expectedStatus = parseInt(responseConfig.successStatus)
			if (response.status !== expectedStatus) {
				message.warning(
					`响应状态码 ${response.status} 与预期 ${expectedStatus} 不符`,
				)
			} else {
				message.success('请求成功')
			}
		} else if (response.status >= 200 && response.status < 300) {
			message.success('请求成功')
		} else {
			message.warning(`请求完成，状态码: ${response.status}`)
		}
	} catch (error: any) {
		responseTime.value = Date.now() - startTime

		if (error.response) {
			// 服务器返回了错误响应，直接展示原始错误数据
			responseStatus.value = error.response.status
			responseHeaders.value = error.response.headers as Record<string, string>
			// 直接使用原始错误响应数据，不添加额外包装
			let errorData = error.response.data
			if (typeof errorData === 'string' && errorData.trim()) {
				// 尝试解析 JSON 字符串
				if (
					errorData.trim().startsWith('{') ||
					errorData.trim().startsWith('[')
				) {
					try {
						errorData = JSON.parse(errorData)
					} catch (e) {
						// 解析失败，保持原始字符串
					}
				}
			}
			responseData.value = errorData
			message.error(
				`请求失败: ${error.response.status} ${error.response.statusText}`,
			)
		} else if (error.request) {
			// 请求已发出但没有收到响应
			responseData.value = '网络错误：未收到服务器响应'
			message.error('网络错误：请检查网络连接或目标服务器是否可访问')
		} else {
			// 请求配置错误
			responseData.value = error.message || '请求配置错误'
			message.error(error.message || '请求失败')
		}
	} finally {
		requestLoading.value = false
	}
}

// 下载文件 - 使用浏览器原生下载功能
const handleDownloadFile = async () => {
	if (!lastRequestParams.value) {
		message.error('请求参数不存在，无法下载')
		return
	}

	const params = lastRequestParams.value
	const downloadLoading = message.loading('正在准备下载...', 0)

	try {
		// 准备请求参数
		let proxyBody: any = params.body
		if (params.body instanceof FormData) {
			// FormData 转换为对象
			const formDataObj: Record<string, any> = {}
			params.body.forEach((value, key) => {
				formDataObj[key] = value
			})
			proxyBody = formDataObj
		} else if (params.body instanceof URLSearchParams) {
			proxyBody = params.body.toString()
		}

		// 获取 token（如果需要）
		const payToken = useCookie('pay-token')
		const token = payToken.value || ''

		// 使用 fetch 发送请求，获取 blob 响应
		const response = await fetch('/web/api/v1/proxy/request', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				Authorization: token,
			},
			body: JSON.stringify({
				url: params.url,
				method: params.method,
				headers: params.headers || {},
				body: proxyBody,
			}),
		})

		// 获取响应内容类型
		const contentType = response.headers.get('content-type') || ''

		// 如果响应不成功，尝试读取错误信息
		if (!response.ok) {
			let errorMessage = `请求失败: ${response.status} ${response.statusText}`

			// 尝试读取错误响应体
			try {
				if (contentType.includes('application/json')) {
					const errorData = await response.json()
					if (
						errorData &&
						(errorData.message || errorData.error || errorData.msg)
					) {
						errorMessage += `\n${errorData.message || errorData.error || errorData.msg}`
					} else if (typeof errorData === 'string') {
						errorMessage += `\n${errorData}`
					} else {
						errorMessage += `\n${JSON.stringify(errorData)}`
					}
				} else if (contentType.includes('text/')) {
					const errorText = await response.text()
					if (errorText) {
						errorMessage += `\n${errorText}`
					}
				} else {
					// 对于其他类型，尝试读取为文本
					const errorBlob = await response.blob()
					const errorText = await errorBlob.text()
					if (errorText && errorText.trim()) {
						// 如果是 HTML，尝试提取文本内容
						if (contentType.includes('html')) {
							const parser = new DOMParser()
							const doc = parser.parseFromString(errorText, 'text/html')
							const bodyText =
								doc.body?.textContent || doc.body?.innerText || errorText
							errorMessage += `\n${bodyText.substring(0, 200)}` // 限制长度
						} else {
							errorMessage += `\n${errorText.substring(0, 200)}` // 限制长度
						}
					}
				}
			} catch (e) {
				// 读取错误信息失败，使用默认错误信息
			}

			throw new Error(errorMessage)
		}

		// 获取 blob 数据
		const blob = await response.blob()

		// 从响应头中获取文件名
		const contentDisposition =
			response.headers.get('content-disposition') ||
			response.headers.get('Content-Disposition') ||
			''
		let filename = getDownloadFileName.value

		// 如果响应头中有新的文件名，使用响应头中的
		if (contentDisposition) {
			const filenameStarMatch = contentDisposition.match(/filename\*=([^;]+)/i)
			if (filenameStarMatch && filenameStarMatch[1]) {
				let decodedFilename = filenameStarMatch[1].trim()
				if (decodedFilename.startsWith("UTF-8''")) {
					try {
						decodedFilename = decodeURIComponent(decodedFilename.substring(7))
						if (decodedFilename) {
							filename = decodedFilename
						}
					} catch (e) {
						// 解码失败，使用之前解析的文件名
					}
				}
			} else {
				const filenameMatch = contentDisposition.match(
					/filename[^=]*=((['"]).*?\2|[^;\n]+)/i,
				)
				if (filenameMatch && filenameMatch[1]) {
					const extractedFilename = filenameMatch[1].replace(/['"]/g, '').trim()
					if (extractedFilename) {
						filename = extractedFilename
					}
				}
			}
		}

		// 创建 blob URL 并触发下载
		const url = window.URL.createObjectURL(blob)
		const link = document.createElement('a')
		link.href = url
		link.download = filename
		link.style.display = 'none'
		document.body.appendChild(link)
		link.click()
		document.body.removeChild(link)

		// 延迟释放 URL，确保下载开始
		setTimeout(() => {
			window.URL.revokeObjectURL(url)
		}, 100)

		downloadLoading()
		message.success('文件下载已开始')
	} catch (error: any) {
		downloadLoading()
		message.error('文件下载失败：' + (error.message || '未知错误'))
	}
}

// 加载编辑数据
const loadEditingData = () => {
	if (!props.editingApiData) {
		return
	}

	// 编辑时默认收起响应内容
	responseExpanded.value = false

	const data = props.editingApiData

	// 加载基本信息
	Object.assign(apiForm, data.apiForm)

	// 加载参数列表
	queryParams.value = (data.queryParams || []).map((param: any) => ({
		...param,
		required: param.required !== undefined ? param.required : false,
	}))
	headers.value = (data.headers || []).map((header: any) => ({
		...header,
		required: header.required !== undefined ? header.required : false,
	}))
	formDataParams.value = (data.formDataParams || []).map((param: any) => ({
		...param,
		type: param.type || 'string', // 默认类型为 string
		required: param.required !== undefined ? param.required : false,
	}))
	urlEncodedParams.value = (data.urlEncodedParams || []).map((param: any) => ({
		...param,
		type: param.type || 'string', // 默认类型为 string
		required: param.required !== undefined ? param.required : false,
	}))
	cookies.value = (data.cookies || []).map((cookie: any) => ({
		...cookie,
		required: cookie.required !== undefined ? cookie.required : false,
	}))

	// 加载Body配置
	const savedBodyConfig = data.bodyConfig || {
		type: 'json',
		jsonParams: [],
		raw: '',
	}

	// 调试日志
	console.log('loadEditingData - savedBodyConfig:', savedBodyConfig)
	console.log('loadEditingData - jsonParams:', savedBodyConfig.jsonParams)

	bodyConfig.type = savedBodyConfig.type || 'json'
	bodyConfig.raw = savedBodyConfig.raw || ''

	// 如果存在jsonParams，使用jsonParams；否则尝试从json字符串解析
	if (
		savedBodyConfig.jsonParams &&
		Array.isArray(savedBodyConfig.jsonParams) &&
		savedBodyConfig.jsonParams.length > 0
	) {
		// 确保每个参数都有所有必需的字段
		const normalizedParams = savedBodyConfig.jsonParams.map((param: any) => ({
			id: param.id || generateId(),
			name: param.name || '',
			required: param.required !== undefined ? param.required : false,
			type: param.type || 'string',
			example: param.example || '',
			description: param.description || '',
			children: param.children || undefined,
		}))
		// 使用 Object.assign 更新 reactive 对象，确保响应式更新
		Object.assign(bodyConfig, {
			jsonParams: normalizedParams,
		})
		console.log('loadEditingData - 设置 jsonParams 后:', bodyConfig.jsonParams)
		console.log(
			'loadEditingData - jsonParams 长度:',
			bodyConfig.jsonParams.length,
		)
	} else {
		Object.assign(bodyConfig, {
			jsonParams: [],
		})
	}

	// 加载响应配置
	Object.assign(
		responseConfig,
		data.responseConfig || { validateResponse: false, successStatus: '200' },
	)
}

// 保存接口
const handleSave = async () => {
	if (!apiForm.name || !apiForm.name.trim()) {
		message.warning('没有接口名称，请补充')
		return
	}
	if (!apiForm.path) {
		message.warning('请输入接口路径')
		return
	}

	const isEditing = !!props.editingApiData
	const apiId = isEditing ? props.editingApiData.id : generateId()

	// 构建要保存的数据
	const apiData: ApiData = {
		id: apiId,
		apiForm: { ...apiForm },
		queryParams: [...queryParams.value],
		headers: [...headers.value],
		bodyConfig: { ...bodyConfig },
		formDataParams: [...formDataParams.value],
		urlEncodedParams: [...urlEncodedParams.value],
		cookies: [...cookies.value],
		responseConfig: { ...responseConfig },
		systemId: props.systemId,
		systemName: props.systemName,
		selectedEnv: props.selectedEnv,
		saveTime: isEditing
			? props.editingApiData.saveTime
			: new Date().toISOString(),
		updateTime: new Date().toISOString(),
	}

	// 使用工具函数保存
	const success = await saveApi(apiData, isEditing)
	if (success) {
		// 无论是编辑模式还是新增模式，保存成功后都触发saved事件，不关闭页面
		emit('saved')
	}
}

// 监听编辑数据变化，加载数据
watch(
	() => props.editingApiData,
	() => {
		loadEditingData()
	},
	{ immediate: true },
)

// 添加 Query 参数
const handleAddQueryParam = () => {
	queryParams.value.push({
		key: '',
		value: '',
		type: 'string',
		description: '',
		required: false,
	})
}

// 删除 Query 参数
const handleDeleteQueryParam = (index: number) => {
	queryParams.value.splice(index, 1)
}

// 添加 Header
const handleAddHeader = () => {
	headers.value.push({
		key: '',
		value: '',
		description: '',
		required: false,
	})
}

// 删除 Header
const handleDeleteHeader = (index: number) => {
	headers.value.splice(index, 1)
}

// 添加 Body form-data 参数
const handleAddFormDataParam = () => {
	formDataParams.value.push({
		key: '',
		value: '',
		type: 'string', // string, integer, boolean, number, array, file
		description: '',
		required: false,
	})
}

// 删除 Body form-data 参数
const handleDeleteFormDataParam = (index: number) => {
	formDataParams.value.splice(index, 1)
}

// 添加 Body url-encoded 参数
const handleAddUrlEncodedParam = () => {
	urlEncodedParams.value.push({
		key: '',
		value: '',
		type: 'string', // string, integer, boolean, number, array (不支持 file)
		description: '',
		required: false,
	})
}

// 删除 Body url-encoded 参数
const handleDeleteUrlEncodedParam = (index: number) => {
	urlEncodedParams.value.splice(index, 1)
}

// 添加 Cookie
const handleAddCookie = () => {
	cookies.value.push({
		key: '',
		value: '',
		description: '',
		required: false,
	})
}

// 删除 Cookie
const handleDeleteCookie = (index: number) => {
	cookies.value.splice(index, 1)
}

// 更新标签页徽章
const updateTabBadge = () => {
	tabs.forEach(tab => {
		if (tab.key === 'headers') {
			tab.badge = headers.value.length > 0 ? headers.value.length : null
		} else if (tab.key === 'body') {
			let count = 0
			if (bodyConfig.type === 'form-data') {
				count = formDataParams.value.length
			} else if (bodyConfig.type === 'x-www-form-urlencoded') {
				count = urlEncodedParams.value.length
			} else if (bodyConfig.type === 'json') {
				// 统计jsonParams的数量
				const countParams = (params: JsonParamNode[]): number => {
					let count = params.length
					for (const param of params) {
						if (param.children) {
							count += countParams(param.children)
						}
					}
					return count
				}
				count =
					bodyConfig.jsonParams && bodyConfig.jsonParams.length > 0
						? countParams(bodyConfig.jsonParams)
						: 0
			} else if (bodyConfig.type === 'raw' && bodyConfig.raw.trim()) {
				count = 1
			}
			tab.badge = count > 0 ? count : null
		} else if (tab.key === 'cookies') {
			tab.badge = cookies.value.length > 0 ? cookies.value.length : null
		}
	})
}

watch(
	[
		headers,
		cookies,
		() => bodyConfig.type,
		() => bodyConfig.jsonParams,
		() => bodyConfig.raw,
		formDataParams,
		urlEncodedParams,
	],
	() => {
		updateTabBadge()
	},
	{ deep: true },
)

// 监听 urlEncodedParams 的类型变化，如果选择了 file 类型，自动改回 string 并提示
watch(
	urlEncodedParams,
	() => {
		if (bodyConfig.type === 'x-www-form-urlencoded') {
			urlEncodedParams.value.forEach((param: any) => {
				if (param.type === 'file') {
					param.type = 'string'
					message.warning(
						'Body 为 x-www-form-urlencoded 时不支持 file 类型，请改用 form-data',
					)
				}
			})
		}
	},
	{ deep: true },
)

// 监听 bodyConfig.type 变化，如果切换到 x-www-form-urlencoded，检查是否有 file 类型
watch(
	() => bodyConfig.type,
	newType => {
		if (newType === 'x-www-form-urlencoded') {
			urlEncodedParams.value.forEach((param: any) => {
				if (param.type === 'file') {
					param.type = 'string'
					message.warning(
						'Body 为 x-www-form-urlencoded 时不支持 file 类型，请改用 form-data',
					)
				}
			})
		}
	},
)
</script>

<template>
	<div class="manual-import-container">
		<!-- 返回按钮和接口名称 -->
		<div class="import-back-header">
			<a-button type="text" @click="emit('back')">
				<i class="ri-arrow-left-line mr-1"></i>
				返回
			</a-button>
			<div class="api-meta-inputs">
				<div class="api-name-input">
					<a-input
						v-model:value="apiForm.name"
						placeholder="请输入接口名称"
						size="large"
						class="name-input"
					/>
				</div>
				<div class="api-desc-input">
					<a-input
						v-model:value="apiForm.description"
						placeholder="请输入接口说明"
						size="large"
						class="desc-input"
					/>
				</div>
			</div>
		</div>
		<!-- 顶部：API 请求定义 -->
		<div class="api-request-bar">
			<div class="request-method">
				<a-select
					v-model:value="apiForm.method"
					class="method-select"
					size="large"
				>
					<a-select-option
						v-for="method in httpMethods"
						:key="method"
						:value="method"
					>
						{{ method }}
					</a-select-option>
				</a-select>
			</div>
			<div class="request-path">
				<a-input
					v-model:value="apiForm.path"
					placeholder='接口路径（"/"起始）'
					size="large"
					class="path-input"
				/>
			</div>
			<div class="request-actions">
				<a-dropdown>
					<a-button
						type="primary"
						size="large"
						:loading="requestLoading"
						@click="handleSend"
					>
						发送
						<template #icon>
							<i class="ri-arrow-down-s-line"></i>
						</template>
					</a-button>
				</a-dropdown>
				<a-button type="primary" size="large" @click="handleSave">
					保存
				</a-button>
			</div>
		</div>

		<!-- 中间：请求组件标签页 -->
		<div class="request-tabs-container">
			<a-tabs v-model:activeKey="activeTab" class="request-tabs">
				<a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label">
					<template #tab>
						<span class="tab-label">
							{{ tab.label }}
							<a-badge
								v-if="tab.badge !== null"
								:count="tab.badge"
								:number-style="{ backgroundColor: '#722ed1' }"
							/>
						</span>
					</template>
				</a-tab-pane>
			</a-tabs>

			<!-- Params 标签页内容 -->
			<div
				v-if="activeTab === 'params'"
				ref="paramsTableContainerRef"
				class="tab-content"
			>
				<div class="tab-content-header">
					<h4 class="tab-content-title">Query 参数</h4>
					<a-button type="link" @click="handleAddQueryParam">
						<i class="ri-add-line mr-1"></i>
						添加参数
					</a-button>
				</div>
				<a-table
					:data-source="queryParams"
					:row-key="(_record: any, index: number) => index"
					:columns="[
						{ title: '参数名', dataIndex: 'key', key: 'key', width: '20%' },
						{
							title: '必填',
							dataIndex: 'required',
							key: 'required',
							width: '8%',
						},
						{ title: '参数值', dataIndex: 'value', key: 'value', width: '20%' },
						{ title: '类型', dataIndex: 'type', key: 'type', width: '15%' },
						{
							title: '说明',
							dataIndex: 'description',
							key: 'description',
							width: '27%',
						},
						{ title: '操作', key: 'action', width: '10%' },
					]"
					:pagination="false"
					size="small"
					bordered
					:scroll="paramsTableScrollY ? { y: paramsTableScrollY } : undefined"
				>
					<template #bodyCell="{ column, record, index }">
						<template v-if="column.key === 'key'">
							<a-input
								v-model:value="record.key"
								placeholder="参数名"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'required'">
							<a-checkbox
								v-model:checked="record.required"
								class="table-cell-checkbox"
							/>
						</template>
						<template v-else-if="column.key === 'value'">
							<a-input
								v-model:value="record.value"
								placeholder="参数值"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'type'">
							<a-select
								v-model:value="record.type"
								size="small"
								class="table-cell-select"
							>
								<a-select-option value="string">string</a-select-option>
								<a-select-option value="number">number</a-select-option>
								<a-select-option value="array">array</a-select-option>
								<a-select-option value="object">object</a-select-option>
								<a-select-option value="boolean">boolean</a-select-option>
							</a-select>
						</template>
						<template v-else-if="column.key === 'description'">
							<a-input
								v-model:value="record.description"
								placeholder="说明"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'action'">
							<a-button
								type="link"
								danger
								size="small"
								@click="handleDeleteQueryParam(index)"
							>
								删除
							</a-button>
						</template>
					</template>
				</a-table>
			</div>

			<!-- Body 标签页内容 -->
			<div v-if="activeTab === 'body'" class="tab-content">
				<div class="body-type-selector">
					<a-radio-group v-model:value="bodyConfig.type" button-style="solid">
						<a-radio-button value="json">JSON</a-radio-button>
						<a-radio-button value="form-data">form-data</a-radio-button>
						<a-radio-button value="x-www-form-urlencoded">
							x-www-form-urlencoded
						</a-radio-button>
						<a-radio-button value="raw">raw</a-radio-button>
					</a-radio-group>
				</div>

				<!-- JSON 格式 -->
				<div v-if="bodyConfig.type === 'json'" class="body-content">
					<JsonBodyEditor
						ref="jsonBodyEditorRef"
						v-model="bodyConfig.jsonParams"
					/>
				</div>

				<!-- form-data 格式 -->
				<div
					v-if="bodyConfig.type === 'form-data'"
					ref="formDataTableContainerRef"
					class="body-content"
				>
					<div class="tab-content-header">
						<h4 class="tab-content-title">表单数据</h4>
						<a-button type="link" @click="handleAddFormDataParam">
							<i class="ri-add-line mr-1"></i>
							添加参数
						</a-button>
					</div>
					<a-table
						:data-source="formDataParams"
						:row-key="(_record: any, index: number) => index"
						:columns="[
							{ title: '参数名', dataIndex: 'key', key: 'key', width: '20%' },
							{
								title: '必填',
								dataIndex: 'required',
								key: 'required',
								width: '8%',
							},
							{
								title: '参数值',
								dataIndex: 'value',
								key: 'value',
								width: '20%',
							},
							{ title: '类型', dataIndex: 'type', key: 'type', width: '15%' },
							{
								title: '说明',
								dataIndex: 'description',
								key: 'description',
								width: '27%',
							},
							{ title: '操作', key: 'action', width: '10%' },
						]"
						:pagination="false"
						size="small"
						bordered
						:scroll="
							formDataTableScrollY ? { y: formDataTableScrollY } : undefined
						"
					>
						<template #bodyCell="{ column, record, index }">
							<template v-if="column.key === 'key'">
								<a-input
									v-model:value="record.key"
									placeholder="参数名"
									size="small"
									class="table-cell-input"
								/>
							</template>
							<template v-else-if="column.key === 'required'">
								<a-checkbox
									v-model:checked="record.required"
									class="table-cell-checkbox"
								/>
							</template>
							<template v-else-if="column.key === 'value'">
								<a-input
									v-if="record.type !== 'file'"
									v-model:value="record.value"
									:placeholder="
										record.type === 'array' ? '例如: [1,2,3]' : '参数值'
									"
									size="small"
									class="table-cell-input"
								/>
								<a-upload
									v-else
									:show-upload-list="false"
									size="small"
									class="table-cell-upload"
								>
									<a-button size="small">选择文件</a-button>
								</a-upload>
							</template>
							<template v-else-if="column.key === 'type'">
								<a-select
									v-model:value="record.type"
									size="small"
									class="table-cell-select"
								>
									<a-select-option value="string">string</a-select-option>
									<a-select-option value="integer">integer</a-select-option>
									<a-select-option value="boolean">boolean</a-select-option>
									<a-select-option value="number">number</a-select-option>
									<a-select-option value="array">array</a-select-option>
									<a-select-option value="file">file</a-select-option>
								</a-select>
							</template>
							<template v-else-if="column.key === 'description'">
								<a-input
									v-model:value="record.description"
									placeholder="说明"
									size="small"
									class="table-cell-input"
								/>
							</template>
							<template v-else-if="column.key === 'action'">
								<a-button
									type="link"
									danger
									size="small"
									@click="handleDeleteFormDataParam(index)"
								>
									删除
								</a-button>
							</template>
						</template>
					</a-table>
				</div>

				<!-- x-www-form-urlencoded 格式 -->
				<div
					v-if="bodyConfig.type === 'x-www-form-urlencoded'"
					ref="urlEncodedTableContainerRef"
					class="body-content"
				>
					<div class="tab-content-header">
						<h4 class="tab-content-title">URL 编码表单</h4>
						<a-button type="link" @click="handleAddUrlEncodedParam">
							<i class="ri-add-line mr-1"></i>
							添加参数
						</a-button>
					</div>
					<a-table
						:data-source="urlEncodedParams"
						:row-key="(_record: any, index: number) => index"
						:columns="[
							{ title: '参数名', dataIndex: 'key', key: 'key', width: '20%' },
							{
								title: '必填',
								dataIndex: 'required',
								key: 'required',
								width: '8%',
							},
							{
								title: '参数值',
								dataIndex: 'value',
								key: 'value',
								width: '20%',
							},
							{ title: '类型', dataIndex: 'type', key: 'type', width: '15%' },
							{
								title: '说明',
								dataIndex: 'description',
								key: 'description',
								width: '27%',
							},
							{ title: '操作', key: 'action', width: '10%' },
						]"
						:pagination="false"
						size="small"
						bordered
						:scroll="
							urlEncodedTableScrollY ? { y: urlEncodedTableScrollY } : undefined
						"
					>
						<template #bodyCell="{ column, record, index }">
							<template v-if="column.key === 'key'">
								<a-input
									v-model:value="record.key"
									placeholder="参数名"
									size="small"
									class="table-cell-input"
								/>
							</template>
							<template v-else-if="column.key === 'required'">
								<a-checkbox
									v-model:checked="record.required"
									class="table-cell-checkbox"
								/>
							</template>
							<template v-else-if="column.key === 'value'">
								<a-input
									v-model:value="record.value"
									:placeholder="
										record.type === 'array' ? '例如: [1,2,3]' : '参数值'
									"
									size="small"
									class="table-cell-input"
								/>
							</template>
							<template v-else-if="column.key === 'type'">
								<a-select
									v-model:value="record.type"
									size="small"
									class="table-cell-select"
								>
									<a-select-option value="string">string</a-select-option>
									<a-select-option value="integer">integer</a-select-option>
									<a-select-option value="boolean">boolean</a-select-option>
									<a-select-option value="number">number</a-select-option>
									<a-select-option value="array">array</a-select-option>
									<a-select-option value="file" disabled>
										<a-tooltip
											title="Body 为 x-www-form-urlencoded 时不支持 file 类型，请改用 form-data"
											placement="top"
										>
											<span>file</span>
										</a-tooltip>
									</a-select-option>
								</a-select>
							</template>
							<template v-else-if="column.key === 'description'">
								<a-input
									v-model:value="record.description"
									placeholder="说明"
									size="small"
									class="table-cell-input"
								/>
							</template>
							<template v-else-if="column.key === 'action'">
								<a-button
									type="link"
									danger
									size="small"
									@click="handleDeleteUrlEncodedParam(index)"
								>
									删除
								</a-button>
							</template>
						</template>
					</a-table>
				</div>

				<!-- raw 格式 -->
				<div v-if="bodyConfig.type === 'raw'" class="body-content">
					<div class="tab-content-header">
						<h4 class="tab-content-title">原始数据</h4>
					</div>
					<a-textarea
						v-model:value="bodyConfig.raw"
						placeholder="请输入原始数据"
						:rows="9"
						class="body-textarea"
					/>
				</div>
			</div>

			<!-- Headers 标签页内容 -->
			<div
				v-if="activeTab === 'headers'"
				ref="headersTableContainerRef"
				class="tab-content"
			>
				<div class="tab-content-header">
					<h4 class="tab-content-title">请求头</h4>
					<a-button type="link" @click="handleAddHeader">
						<i class="ri-add-line mr-1"></i>
						添加请求头
					</a-button>
				</div>
				<a-table
					:data-source="headers"
					:row-key="(_record: any, index: number) => index"
					:columns="[
						{ title: '参数名', dataIndex: 'key', key: 'key', width: '25%' },
						{
							title: '必填',
							dataIndex: 'required',
							key: 'required',
							width: '8%',
						},
						{ title: '参数值', dataIndex: 'value', key: 'value', width: '42%' },
						{
							title: '说明',
							dataIndex: 'description',
							key: 'description',
							width: '15%',
						},
						{ title: '操作', key: 'action', width: '10%' },
					]"
					:pagination="false"
					size="small"
					bordered
					:scroll="headersTableScrollY ? { y: headersTableScrollY } : undefined"
				>
					<template #bodyCell="{ column, record, index }">
						<template v-if="column.key === 'key'">
							<a-input
								v-model:value="record.key"
								placeholder="参数名"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'required'">
							<a-checkbox
								v-model:checked="record.required"
								class="table-cell-checkbox"
							/>
						</template>
						<template v-else-if="column.key === 'value'">
							<a-input
								v-model:value="record.value"
								placeholder="参数值"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'description'">
							<a-input
								v-model:value="record.description"
								placeholder="说明"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'action'">
							<a-button
								type="link"
								danger
								size="small"
								@click="handleDeleteHeader(index)"
							>
								删除
							</a-button>
						</template>
					</template>
				</a-table>
			</div>

			<!-- Cookies 标签页内容 -->
			<div
				v-if="activeTab === 'cookies'"
				ref="cookiesTableContainerRef"
				class="tab-content"
			>
				<div class="tab-content-header">
					<h4 class="tab-content-title">Cookies</h4>
					<a-button type="link" @click="handleAddCookie">
						<i class="ri-add-line mr-1"></i>
						添加 Cookie
					</a-button>
				</div>
				<a-table
					:data-source="cookies"
					:row-key="(_record: any, index: number) => index"
					:columns="[
						{ title: '参数名', dataIndex: 'key', key: 'key', width: '25%' },
						{
							title: '必填',
							dataIndex: 'required',
							key: 'required',
							width: '8%',
						},
						{ title: '参数值', dataIndex: 'value', key: 'value', width: '42%' },
						{
							title: '说明',
							dataIndex: 'description',
							key: 'description',
							width: '15%',
						},
						{ title: '操作', key: 'action', width: '10%' },
					]"
					:pagination="false"
					size="small"
					bordered
					:scroll="cookiesTableScrollY ? { y: cookiesTableScrollY } : undefined"
				>
					<template #bodyCell="{ column, record, index }">
						<template v-if="column.key === 'key'">
							<a-input
								v-model:value="record.key"
								placeholder="参数名"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'required'">
							<a-checkbox
								v-model:checked="record.required"
								class="table-cell-checkbox"
							/>
						</template>
						<template v-else-if="column.key === 'value'">
							<a-input
								v-model:value="record.value"
								placeholder="参数值"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'description'">
							<a-input
								v-model:value="record.description"
								placeholder="说明"
								size="small"
								class="table-cell-input"
							/>
						</template>
						<template v-else-if="column.key === 'action'">
							<a-button
								type="link"
								danger
								size="small"
								@click="handleDeleteCookie(index)"
							>
								删除
							</a-button>
						</template>
					</template>
				</a-table>
			</div>
		</div>

		<!-- 底部：返回响应 -->
		<div class="response-container">
			<div class="response-header">
				<div class="response-title-section">
					<a-button
						type="text"
						size="small"
						class="response-expand-btn"
						@click="toggleResponseExpanded"
					>
						<i
							:class="
								responseExpanded
									? 'ri-arrow-down-s-line'
									: 'ri-arrow-right-s-line'
							"
						></i>
					</a-button>
					<span class="response-title">返回响应</span>
					<span v-if="responseStatus !== null" class="response-status">
						<span
							class="status-code"
							:class="{
								'status-success': responseStatus >= 200 && responseStatus < 300,
								'status-error': responseStatus >= 400,
								'status-warning': responseStatus >= 300 && responseStatus < 400,
							}"
						>
							{{ responseStatus }}
						</span>
					</span>
					<span v-if="responseTime > 0" class="response-time">
						耗时: {{ responseTime }}ms
					</span>
				</div>
				<div class="response-controls">
					<span class="control-item">
						校验响应
						<a-switch
							v-model:checked="responseConfig.validateResponse"
							size="small"
						/>
					</span>
					<a-select
						v-model:value="responseConfig.successStatus"
						size="small"
						style="width: 120px"
					>
						<a-select-option value="200">成功(200)</a-select-option>
						<a-select-option value="201">创建(201)</a-select-option>
						<a-select-option value="204">无内容(204)</a-select-option>
					</a-select>
				</div>
			</div>
			<div
				v-show="responseExpanded"
				class="response-content"
				style="flex: 0 0 300px; min-height: 300px"
			>
				<div
					v-if="!responseData && !requestLoading && !isFileDownload"
					class="response-empty"
				>
					<i class="ri-rocket-line" style="font-size: 64px; color: #d9d9d9"></i>
					<p class="empty-text">点击"发送"按钮获取返回结果</p>
				</div>
				<div v-else-if="requestLoading" class="response-loading">
					<a-spin size="large" />
					<p class="loading-text">请求发送中...</p>
				</div>
				<div v-else-if="isFileDownload" class="response-download">
					<div class="download-container">
						<i class="ri-download-cloud-2-line download-icon"></i>
						<p class="download-filename">{{ getDownloadFileName }}</p>
						<a-button
							type="primary"
							size="large"
							class="download-button"
							@click="handleDownloadFile"
						>
							<i class="ri-download-line mr-1"></i>
							下载文件
						</a-button>
					</div>
				</div>
				<div v-else class="response-data">
					<pre>{{ JSON.stringify(responseData, null, 2) }}</pre>
				</div>
			</div>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.manual-import-container {
	display: flex;
	flex-direction: column;
	height: 100%;
	width: 100%;
	min-width: 0;
	overflow: hidden;
	background-color: #fff;
	.dark & {
		background-color: #1f1f1f;
	}

	.import-back-header {
		display: flex;
		align-items: center;
		gap: 16px;
		padding: 12px 16px;
		border-bottom: 1px solid #f0f0f0;
		.dark & {
			border-bottom-color: #303030;
		}

		.api-meta-inputs {
			flex: 1;
			padding-left: 18px;
			display: flex;
			gap: 12px;
			min-width: 0;
		}

		.api-name-input,
		.api-desc-input {
			flex: 1;
			min-width: 0;
		}

		.api-name-input {
			.name-input {
				width: 100%;
			}
		}

		.api-desc-input {
			.desc-input {
				width: 100%;
			}
		}
	}

	// 顶部：API 请求定义
	.api-request-bar {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 16px;
		border-bottom: 1px solid #f0f0f0;
		.dark & {
			border-bottom-color: #303030;
		}

		.request-env {
			min-width: 200px;
		}

		.request-method {
			.method-select {
				width: 100px;
			}
		}

		.request-path {
			flex: 1;
			.path-input {
				width: 100%;
			}
		}

		.request-actions {
			display: flex;
			gap: 8px;
		}
	}

	// 中间：请求组件标签页
	.request-tabs-container {
		flex: 1;
		display: flex;
		flex-direction: column;
		overflow: hidden;

		.request-tabs {
			flex-shrink: 0;
			:deep(.ant-tabs-nav) {
				margin: 0;
				padding: 0 16px;
			}

			.tab-label {
				display: inline-flex;
				align-items: center;
				gap: 4px;
			}
		}

		.tab-content {
			padding: 16px;
			display: flex;
			flex-direction: column;
			flex: 1;
			min-height: 0;
			overflow: hidden;

			.tab-content-header {
				display: flex;
				justify-content: space-between;
				align-items: center;
				margin-bottom: 12px;
				flex-shrink: 0;
			}

			.tab-content-title {
				margin: 0;
				font-size: 14px;
				font-weight: 500;
				color: rgba(0, 0, 0, 0.85);
				.dark & {
					color: rgba(255, 255, 255, 0.85);
				}
			}

			// Body 类型切换区域，正常跟随页面滚动
			.body-type-selector {
				margin-bottom: 16px;
			}

			// 仅表格内容区域出现滚动条
			.param-table-wrapper {
				max-height: 260px;
				overflow-y: auto;
			}

			.body-content {
				display: flex;
				flex-direction: column;
				flex: 1;
				min-height: 0;
				overflow: hidden;

				.body-textarea {
					font-family: 'Courier New', monospace;
					font-size: 12px;
				}
			}

			// 表格单元格内的输入框和下拉框样式
			:deep(.ant-table-tbody) {
				.ant-table-cell {
					height: 36px;
					padding: 0;
				}
				.table-cell-input {
					width: 100%;
					height: 100%;
				}

				.table-cell-select {
					width: 100%;
					height: 100%;
				}

				.table-cell-checkbox {
					display: flex;
					align-items: center;
					justify-content: center;
					width: 100%;
					height: 100%;
				}

				.ant-select-selector {
					height: 100%;
					.ant-select-selection-item {
						line-height: 36px;
					}
				}

				.table-cell-upload {
					width: 100%;
					height: 100%;

					.ant-upload {
						width: 100%;
						height: 100%;
					}

					.ant-btn {
						width: 100%;
						height: 100%;
					}
				}
			}
		}
	}

	// 底部：返回响应
	.response-container {
		display: flex;
		flex-direction: column;
		flex-shrink: 0;
		height: auto;
		min-height: 0;
		border-top: 1px solid #f0f0f0;
		min-width: 0;
		overflow: hidden;
		transition: height 0.3s ease;
		.dark & {
			border-top-color: #303030;
		}

		.response-header {
			display: flex;
			justify-content: space-between;
			align-items: center;
			padding: 12px 16px;
			flex-shrink: 0;
			border-bottom: 1px solid #f0f0f0;
			.dark & {
				border-bottom-color: #303030;
			}

			.response-title-section {
				display: flex;
				align-items: center;
				gap: 8px;

				.response-expand-btn {
					padding: 0;
					width: 20px;
					height: 20px;
					display: flex;
					align-items: center;
					justify-content: center;
					color: rgba(0, 0, 0, 0.45);
					transition: color 0.2s;
					.dark & {
						color: rgba(255, 255, 255, 0.45);
					}

					&:hover {
						color: rgba(0, 0, 0, 0.85);
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					i {
						font-size: 16px;
					}
				}
			}

			.response-title-section {
				display: flex;
				align-items: center;
				gap: 12px;
			}

			.response-title {
				font-size: 14px;
				font-weight: 500;
				color: rgba(0, 0, 0, 0.85);
				.dark & {
					color: rgba(255, 255, 255, 0.85);
				}
			}

			.response-status {
				.status-code {
					padding: 2px 8px;
					border-radius: 4px;
					font-size: 12px;
					font-weight: 500;
					background-color: #f0f0f0;
					color: rgba(0, 0, 0, 0.65);
					.dark & {
						background-color: #303030;
						color: rgba(255, 255, 255, 0.65);
					}

					&.status-success {
						background-color: #f6ffed;
						color: #52c41a;
						.dark & {
							background-color: #162312;
							color: #73d13d;
						}
					}

					&.status-warning {
						background-color: #fffbe6;
						color: #faad14;
						.dark & {
							background-color: #2b2111;
							color: #ffc53d;
						}
					}

					&.status-error {
						background-color: #fff2f0;
						color: #ff4d4f;
						.dark & {
							background-color: #2a1215;
							color: #ff7875;
						}
					}
				}
			}

			.response-time {
				font-size: 12px;
				color: rgba(0, 0, 0, 0.45);
				.dark & {
					color: rgba(255, 255, 255, 0.45);
				}
			}

			.response-controls {
				display: flex;
				align-items: center;
				gap: 16px;

				.control-item {
					display: flex;
					align-items: center;
					gap: 8px;
					font-size: 14px;
					color: rgba(0, 0, 0, 0.65);
					.dark & {
						color: rgba(255, 255, 255, 0.65);
					}
				}
			}
		}

		.response-content {
			flex: 0 0 300px;
			min-height: 300px;
			padding: 16px;
			overflow-y: auto;
			overflow-x: hidden;
			min-width: 0;

			.response-empty {
				display: flex;
				flex-direction: column;
				align-items: center;
				justify-content: center;
				height: 100%;

				.empty-text {
					margin-top: 16px;
					font-size: 14px;
					color: rgba(0, 0, 0, 0.45);
					.dark & {
						color: rgba(255, 255, 255, 0.45);
					}
				}
			}

			.response-loading {
				display: flex;
				flex-direction: column;
				align-items: center;
				justify-content: center;
				height: 100%;

				.loading-text {
					margin-top: 16px;
					font-size: 14px;
					color: rgba(0, 0, 0, 0.45);
					.dark & {
						color: rgba(255, 255, 255, 0.45);
					}
				}
			}

			.response-download {
				display: flex;
				align-items: center;
				justify-content: center;
				height: 100%;
				width: 100%;
				overflow: hidden;

				.download-container {
					display: flex;
					flex-direction: column;
					align-items: center;
					justify-content: center;
					gap: 24px;
					padding: 32px;
					width: 100%;
					max-width: 100%;
					box-sizing: border-box;
					background-color: #fafafa;
					border-radius: 8px;
					border: 1px dashed #d9d9d9;
					.dark & {
						background-color: #1f1f1f;
						border-color: #434343;
					}

					.download-icon {
						font-size: 48px;
						color: #1890ff;
						flex-shrink: 0;
						.dark & {
							color: #69c0ff;
						}
					}

					.download-filename {
						margin: 0;
						font-size: 16px;
						font-weight: 500;
						color: rgba(0, 0, 0, 0.85);
						word-break: break-all;
						text-align: center;
						width: 100%;
						max-width: 100%;
						overflow-wrap: break-word;
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.download-button {
						min-width: 160px;
						flex-shrink: 0;
					}
				}
			}

			.response-data {
				width: 100%;
				min-width: 0;
				overflow: hidden;

				pre {
					margin: 0;
					padding: 12px;
					background-color: #f5f5f5;
					border-radius: 4px;
					font-size: 12px;
					overflow-x: auto;
					overflow-y: auto;
					max-width: 100%;
					word-wrap: break-word;
					white-space: pre-wrap;
					word-break: break-all;
					.dark & {
						background-color: #262626;
						color: rgba(255, 255, 255, 0.85);
					}
				}
			}
		}
	}
}
</style>
