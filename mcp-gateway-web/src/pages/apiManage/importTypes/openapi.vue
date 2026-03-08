<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/importTypes/openapi.vue
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: OpenAPI/Swagger 导入页面
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-23
-->
<script setup lang="tsx">
import { message } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import { fetchProxyRequest } from '~/fetch/http'
import {
	generateId,
	saveApis,
	type ApiData,
	parseJsonToParams,
} from '~/utils/apiSaveUtils'

// 接收父组件传递的业务系统信息和环境信息
const props = defineProps<{
	systemId?: string
	systemName?: string
	selectedEnv?: any | null
}>()

// 定义事件
const emit = defineEmits<{
	back: []
	success: []
}>()

// 当前激活的标签页
const activeTab = ref('url')

// 文件上传相关状态
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)

// URL 导入相关状态
const urlForm = reactive({
	url: '',
	loading: false,
})

// 树形选择器相关状态
const treeData = ref<any[]>([])
const originalTreeData = ref<any[]>([]) // 原始树数据，用于搜索过滤
const checkedKeys = ref<{ checked: string[]; halfChecked: string[] }>({
	checked: [],
	halfChecked: [],
})
const expandedKeys = ref<string[]>([])
const showTreeSelect = ref(false)
const apiSpecData = ref<any>(null) // 存储原始 API 规范数据
const searchValue = ref('') // 搜索关键词

// 文件上传前的验证
const beforeUpload: UploadProps['beforeUpload'] = file => {
	const isJson = file.type === 'application/json' || file.name.endsWith('.json')
	const isYaml =
		file.type === 'text/yaml' ||
		file.type === 'application/x-yaml' ||
		file.name.endsWith('.yaml') ||
		file.name.endsWith('.yml')

	if (!isJson && !isYaml) {
		message.error('只支持上传 JSON 或 YAML 格式的文件！')
		return false
	}

	const isLt10M = file.size / 1024 / 1024 < 10
	if (!isLt10M) {
		message.error('文件大小不能超过 10MB！')
		return false
	}

	return true
}

// 处理文件上传
const handleUpload = async (info: any) => {
	const { file, fileList: newFileList } = info

	// 如果是上传中状态，更新文件列表
	if (file.status === 'uploading') {
		fileList.value = newFileList
		uploading.value = true
		return
	}

	// 如果是完成状态
	if (file.status === 'done') {
		uploading.value = false
		message.success('文件上传成功！')
		// TODO: 处理文件内容，解析 OpenAPI/Swagger 规范
		await processFileContent(file)
	} else if (file.status === 'error') {
		uploading.value = false
		message.error('文件上传失败！')
	}
}

// 处理文件内容
const processFileContent = async (file: UploadFile) => {
	try {
		const fileObj = file.originFileObj || file
		if (!fileObj) {
			message.error('文件对象不存在')
			return
		}

		// 将文件对象转换为 File 类型并读取内容
		const fileInstance = fileObj instanceof File ? fileObj : (fileObj as any)
		const text = await fileInstance.text()
		let apiSpec: any = null

		// 判断是 JSON 还是 YAML
		if (file.name.endsWith('.json')) {
			apiSpec = JSON.parse(text)
		} else {
			// YAML 解析需要引入 yaml 库，这里先做简单处理
			message.warning('YAML 格式解析功能待实现')
			return
		}

		// 验证 OpenAPI/Swagger 格式
		if (!validateApiSpec(apiSpec)) {
			message.error(
				'文件格式不正确，请确保是 OpenAPI 3.0、3.1 或 Swagger 2.0 格式',
			)
			return
		}

		// 解析并显示树形选择器
		handleParsedApiSpec(apiSpec)
	} catch (error: any) {
		message.error('文件解析失败：' + (error.message || '未知错误'))
	}
}

// 验证 API 规范格式
const validateApiSpec = (spec: any): boolean => {
	if (!spec || typeof spec !== 'object') {
		return false
	}

	// 检查 OpenAPI 3.0/3.1
	if (
		spec.openapi &&
		(spec.openapi.startsWith('3.0') || spec.openapi.startsWith('3.1'))
	) {
		return true
	}

	// 检查 Swagger 2.0
	if (spec.swagger && spec.swagger.startsWith('2.0')) {
		return true
	}

	return false
}

// 解析 OpenAPI/Swagger 规范为树形结构（按 tags 分组，类似 Swagger UI）
const parseApiSpecToTree = (spec: any): any[] => {
	const tree: any[] = []

	if (!spec || !spec.paths) {
		return tree
	}

	// 判断是 OpenAPI 3.x 还是 Swagger 2.0
	const isOpenAPI3 =
		spec.openapi &&
		(spec.openapi.startsWith('3.0') || spec.openapi.startsWith('3.1'))
	const isSwagger2 = spec.swagger && spec.swagger.startsWith('2.0')

	if (!isOpenAPI3 && !isSwagger2) {
		return tree
	}

	// HTTP 方法列表
	const httpMethods = [
		'get',
		'post',
		'put',
		'delete',
		'patch',
		'head',
		'options',
		'trace',
	]

	// 按 tags 分组存储 API
	const tagMap = new Map<string, any[]>()
	const untaggedApis: any[] = [] // 没有 tag 的 API

	// 遍历 paths
	Object.keys(spec.paths).forEach(path => {
		const pathItem = spec.paths[path]

		// 遍历每个路径下的 HTTP 方法
		httpMethods.forEach(method => {
			if (pathItem[method]) {
				const operation = pathItem[method]
				const operationId =
					operation.operationId || `${method.toUpperCase()} ${path}`
				const summary = operation.summary || operationId
				const description = operation.description || ''

				// 获取 tags（OpenAPI 3.x 和 Swagger 2.0 都支持）
				const tags = operation.tags || []

				// 构建唯一标识：path + method
				const key = `${path}::${method.toUpperCase()}`

				const apiNode = {
					title: `${method.toUpperCase()} ${path}`,
					key,
					isLeaf: true,
					path,
					method: method.toUpperCase(),
					operation,
					summary,
					description,
				}

				// 如果有 tags，按 tag 分组
				if (tags && tags.length > 0) {
					tags.forEach((tag: string) => {
						if (!tagMap.has(tag)) {
							tagMap.set(tag, [])
						}
						tagMap.get(tag)!.push(apiNode)
					})
				} else {
					// 没有 tag 的 API
					untaggedApis.push(apiNode)
				}
			}
		})
	})

	// 构建树形结构：tag -> API
	tagMap.forEach((apis, tag) => {
		// 获取 tag 的描述信息（从 spec.tags 中查找）
		let tagDescription = ''
		if (spec.tags && Array.isArray(spec.tags)) {
			const tagInfo = spec.tags.find((t: any) => t.name === tag)
			if (tagInfo && tagInfo.description) {
				tagDescription = tagInfo.description
			}
		}

		tree.push({
			title: tag,
			key: `tag::${tag}`,
			children: apis,
			description: tagDescription,
		})
	})

	// 如果有未分组的 API，添加到"未分组"分类
	if (untaggedApis.length > 0) {
		tree.push({
			title: '未分组',
			key: 'tag::untagged',
			children: untaggedApis,
		})
	}

	// 如果没有 tag，按路径分组（兼容没有 tags 的情况）
	if (tree.length === 0) {
		const pathMap = new Map<string, any[]>()

		Object.keys(spec.paths).forEach(path => {
			const pathItem = spec.paths[path]
			const pathChildren: any[] = []

			httpMethods.forEach(method => {
				if (pathItem[method]) {
					const operation = pathItem[method]
					const operationId =
						operation.operationId || `${method.toUpperCase()} ${path}`
					const summary = operation.summary || operationId
					const description = operation.description || ''

					const key = `${path}::${method.toUpperCase()}`

					pathChildren.push({
						title: `${method.toUpperCase()} ${path}`,
						key,
						isLeaf: true,
						path,
						method: method.toUpperCase(),
						operation,
						summary,
						description,
					})
				}
			})

			if (pathChildren.length > 0) {
				pathMap.set(path, pathChildren)
			}
		})

		pathMap.forEach((apis, path) => {
			tree.push({
				title: path,
				key: `path::${path}`,
				children: apis,
			})
		})
	}

	return tree
}

// 处理解析后的 API 规范
const handleParsedApiSpec = (spec: any) => {
	apiSpecData.value = spec
	const tree = parseApiSpecToTree(spec)
	originalTreeData.value = tree
	treeData.value = tree
	checkedKeys.value = { checked: [], halfChecked: [] }
	searchValue.value = ''
	// 默认展开所有节点
	expandedKeys.value = tree.map(node => node.key)
	showTreeSelect.value = true
	message.success('解析成功，请选择要导入的 API')
}

// 计算选中的 API 数量（只统计叶子节点）
const selectedApiCount = computed(() => {
	const checked = checkedKeys.value.checked || []
	let count = 0

	const traverseTree = (nodes: any[]) => {
		nodes.forEach(node => {
			if (checked.includes(node.key) && node.isLeaf) {
				count++
			}
			if (node.children) {
				traverseTree(node.children)
			}
		})
	}

	traverseTree(originalTreeData.value)
	return count
})

// 处理树节点展开/收起
const handleExpand = (keys: string[]) => {
	expandedKeys.value = keys
}

// 处理选中的 API
const handleCheck = (checked: any) => {
	// a-tree 的 check 事件返回 { checked: string[], halfChecked: string[] }
	if (typeof checked === 'object' && checked.checked) {
		checkedKeys.value = checked
	} else if (Array.isArray(checked)) {
		checkedKeys.value = { checked, halfChecked: [] }
	}
}

// 获取选中的 API 数据
const getSelectedApiData = () => {
	const selectedApis: any[] = []
	const checked = checkedKeys.value.checked || []

	const traverseTree = (nodes: any[]) => {
		nodes.forEach(node => {
			if (checked.includes(node.key)) {
				if (node.isLeaf) {
					// 叶子节点，是具体的 API
					selectedApis.push({
						path: node.path,
						method: node.method,
						operation: node.operation,
						summary: node.summary,
						description: node.description,
					})
				}
			}
			if (node.children) {
				traverseTree(node.children)
			}
		})
	}

	traverseTree(originalTreeData.value)
	return selectedApis
}

// 根据 OpenAPI schema 生成 JSON 示例
const generateJsonExample = (schema: any, spec: any = null): any => {
	if (!schema) {
		return {}
	}

	// 处理 $ref 引用
	if (schema.$ref) {
		// OpenAPI 3.x: #/components/schemas/xxx
		// Swagger 2.0: #/definitions/xxx
		let refPath = ''
		let refSchema: any = null

		if (schema.$ref.startsWith('#/components/schemas/')) {
			// OpenAPI 3.x
			refPath = schema.$ref.replace('#/components/schemas/', '')
			if (spec && spec.components && spec.components.schemas) {
				refSchema = spec.components.schemas[refPath]
			}
		} else if (schema.$ref.startsWith('#/definitions/')) {
			// Swagger 2.0
			refPath = schema.$ref.replace('#/definitions/', '')
			if (spec && spec.definitions) {
				refSchema = spec.definitions[refPath]
			}
		}

		if (refSchema) {
			return generateJsonExample(refSchema, spec)
		}
		return {}
	}

	// 处理 allOf, anyOf, oneOf
	if (schema.allOf) {
		const result: any = {}
		schema.allOf.forEach((item: any) => {
			Object.assign(result, generateJsonExample(item, spec))
		})
		return result
	}

	if (schema.anyOf || schema.oneOf) {
		const firstItem = (schema.anyOf || schema.oneOf)[0]
		return generateJsonExample(firstItem, spec)
	}

	// 处理不同的类型
	const type = schema.type || 'object'

	switch (type) {
		case 'object': {
			const obj: any = {}
			if (schema.properties) {
				Object.keys(schema.properties).forEach(key => {
					const prop = schema.properties[key]
					// 包含所有字段，用于生成完整的 jsonParams 结构
					obj[key] = generateJsonExample(prop, spec)
				})
			}
			return obj
		}

		case 'array':
			if (schema.items) {
				return [generateJsonExample(schema.items, spec)]
			}
			return []

		case 'string':
			if (schema.enum && schema.enum.length > 0) {
				return schema.enum[0]
			}
			if (schema.format === 'date') {
				return '2024-01-01'
			}
			if (schema.format === 'date-time') {
				return '2024-01-01T00:00:00Z'
			}
			if (schema.format === 'email') {
				return 'example@example.com'
			}
			if (schema.format === 'uri') {
				return 'https://example.com'
			}
			return schema.example || schema.default || ''

		case 'number':
		case 'integer':
			return schema.example !== undefined
				? schema.example
				: schema.default !== undefined
					? schema.default
					: 0

		case 'boolean':
			return schema.example !== undefined
				? schema.example
				: schema.default !== undefined
					? schema.default
					: false

		case 'null':
			return null

		default:
			return schema.example || schema.default || null
	}
}

// 解析 $ref 引用（支持 components/parameters 引用）
const resolveParameterRef = (param: any, spec: any): any => {
	if (!param.$ref || !spec) {
		return param
	}

	// 解析 $ref 路径，例如: "#/components/parameters/Authorization"
	const refPath = param.$ref.replace('#/', '').split('/')
	if (
		refPath.length >= 3 &&
		refPath[0] === 'components' &&
		refPath[1] === 'parameters'
	) {
		const paramName = refPath[2]
		if (
			spec.components &&
			spec.components.parameters &&
			spec.components.parameters[paramName]
		) {
			return spec.components.parameters[paramName]
		}
	}

	return param
}

// 获取参数的默认值或示例值
const getParameterValue = (param: any): string => {
	// 优先使用 example
	if (param.example !== undefined && param.example !== null) {
		return String(param.example)
	}

	// 其次使用 schema.example
	if (
		param.schema &&
		param.schema.example !== undefined &&
		param.schema.example !== null
	) {
		return String(param.schema.example)
	}

	// 再次使用 default
	if (param.default !== undefined && param.default !== null) {
		return String(param.default)
	}

	// 最后使用 schema.default
	if (
		param.schema &&
		param.schema.default !== undefined &&
		param.schema.default !== null
	) {
		return String(param.schema.default)
	}

	return ''
}

// 提取 security 定义的 headers（如 Authorization）
const extractSecurityHeaders = (operation: any, spec: any): any[] => {
	const securityHeaders: any[] = []

	if (!operation.security && (!spec.security || spec.security.length === 0)) {
		return securityHeaders
	}

	// 使用 operation 级别的 security，如果没有则使用全局的
	const security = operation.security || spec.security || []

	security.forEach((sec: any) => {
		Object.keys(sec).forEach((schemeName: string) => {
			const scheme = spec.components?.securitySchemes?.[schemeName]
			if (!scheme) {
				return
			}

			// 处理不同类型的认证方案
			if (scheme.type === 'http') {
				if (scheme.scheme === 'bearer') {
					// Bearer Token
					securityHeaders.push({
						key: 'Authorization',
						value: 'Bearer ',
						description:
							scheme.description ||
							`Bearer token authentication (${schemeName})`,
					})
				} else if (scheme.scheme === 'basic') {
					// Basic Auth
					securityHeaders.push({
						key: 'Authorization',
						value: 'Basic ',
						description:
							scheme.description || `Basic authentication (${schemeName})`,
					})
				}
			} else if (scheme.type === 'apiKey' && scheme.in === 'header') {
				// API Key in header
				securityHeaders.push({
					key: scheme.name || 'X-API-Key',
					value: '',
					description:
						scheme.description || `API Key authentication (${schemeName})`,
				})
			} else if (scheme.type === 'oauth2') {
				// OAuth2
				securityHeaders.push({
					key: 'Authorization',
					value: 'Bearer ',
					description:
						scheme.description || `OAuth2 authentication (${schemeName})`,
				})
			}
		})
	})

	return securityHeaders
}

// 将 OpenAPI operation 转换为保存的数据结构
const convertOperationToApiData = (apiInfo: any): ApiData => {
	const { path, method, operation, summary, description } = apiInfo
	const spec = apiSpecData.value

	// 提取参数（包括 path-level 和 operation-level）
	// OpenAPI 3.x: pathItem.parameters 和 operation.parameters
	// Swagger 2.0: pathItem.parameters 和 operation.parameters
	const pathItem = spec.paths?.[path] || {}
	const pathParameters = pathItem.parameters || []
	const operationParameters = operation.parameters || []

	// 合并参数，operation 级别的参数优先级更高
	const allParameters = [...pathParameters, ...operationParameters]

	const queryParams: any[] = []
	const headers: any[] = []
	const cookies: any[] = []

	// Swagger 2.0 的 body 参数（在 parameters 中，in: 'body'）
	let swagger2BodyParam: any = null

	allParameters.forEach((param: any) => {
		// 解析 $ref 引用
		const resolvedParam = resolveParameterRef(param, spec)

		// Swagger 2.0: body 参数在 parameters 中，in: 'body'
		if (resolvedParam.in === 'body') {
			swagger2BodyParam = resolvedParam
			return // 跳过 body 参数，后面单独处理
		}

		const paramData = {
			key: resolvedParam.name || '',
			value: getParameterValue(resolvedParam),
			type: resolvedParam.schema?.type || 'string',
			description: resolvedParam.description || '',
			required: resolvedParam.required || false,
		}

		if (resolvedParam.in === 'query') {
			queryParams.push(paramData)
		} else if (resolvedParam.in === 'header') {
			headers.push({
				key: resolvedParam.name || '',
				value: getParameterValue(resolvedParam),
				description: resolvedParam.description || '',
			})
		} else if (resolvedParam.in === 'cookie') {
			cookies.push({
				key: resolvedParam.name || '',
				value: getParameterValue(resolvedParam),
				description: resolvedParam.description || '',
			})
		}
	})

	// 提取 security 定义的 headers（如 Authorization）
	const securityHeaders = extractSecurityHeaders(operation, spec)

	// 合并 security headers，避免重复
	securityHeaders.forEach((secHeader: any) => {
		const existingIndex = headers.findIndex(
			h => h.key.toLowerCase() === secHeader.key.toLowerCase(),
		)
		if (existingIndex === -1) {
			headers.push(secHeader)
		} else {
			// 如果已存在，合并描述信息
			headers[existingIndex].description =
				headers[existingIndex].description || secHeader.description
		}
	})

	// 提取 requestBody
	// OpenAPI 3.x: operation.requestBody
	// Swagger 2.0: parameters 中 in: 'body' 的参数
	const requestBody = operation.requestBody
	const bodyConfig = {
		type: 'json',
		jsonParams: [] as any[],
		raw: '',
	}
	const formDataParams: any[] = []
	const urlEncodedParams: any[] = []

	// 处理 OpenAPI 3.x 的 requestBody
	if (requestBody) {
		const content = requestBody.content || {}

		if (content['application/json']) {
			bodyConfig.type = 'json'
			const schema = content['application/json'].schema
			if (schema) {
				// 根据 schema 生成 JSON 示例
				const jsonExample = generateJsonExample(schema, apiSpecData.value)
				// 将 JSON 示例解析为 jsonParams 树形结构
				bodyConfig.jsonParams = parseJsonToParams(jsonExample)
			} else {
				bodyConfig.jsonParams = []
			}
		} else if (content['multipart/form-data']) {
			bodyConfig.type = 'form-data'
			const schema = content['multipart/form-data'].schema
			if (schema && schema.properties) {
				Object.keys(schema.properties).forEach(key => {
					const prop = schema.properties[key]
					formDataParams.push({
						key,
						value: '',
						type:
							prop.type === 'string' && prop.format === 'binary'
								? 'file'
								: 'text',
						description: prop.description || '',
					})
				})
			}
		} else if (content['application/x-www-form-urlencoded']) {
			bodyConfig.type = 'x-www-form-urlencoded'
			const schema = content['application/x-www-form-urlencoded'].schema
			if (schema && schema.properties) {
				Object.keys(schema.properties).forEach(key => {
					const prop = schema.properties[key]
					urlEncodedParams.push({
						key,
						value: '',
						description: prop.description || '',
					})
				})
			}
		}
	} else if (swagger2BodyParam) {
		// 处理 Swagger 2.0 的 body 参数
		bodyConfig.type = 'json'
		const schema = swagger2BodyParam.schema
		if (schema) {
			// 根据 schema 生成 JSON 示例
			const jsonExample = generateJsonExample(schema, apiSpecData.value)
			// 将 JSON 示例解析为 jsonParams 树形结构
			bodyConfig.jsonParams = parseJsonToParams(jsonExample)
		} else {
			bodyConfig.jsonParams = []
		}
	}

	// 构建 API 数据
	const apiData: ApiData = {
		id: generateId(),
		apiForm: {
			method,
			path,
			name: summary || `${method} ${path}`,
			description: description || '',
		},
		queryParams,
		headers,
		bodyConfig,
		formDataParams,
		urlEncodedParams,
		cookies,
		responseConfig: {
			validateResponse: false,
			successStatus: '200',
		},
		systemId: props.systemId,
		systemName: props.systemName,
		selectedEnv: props.selectedEnv,
		saveTime: new Date().toISOString(),
		updateTime: new Date().toISOString(),
	}

	return apiData
}

// 执行导入
const handleImport = async () => {
	const selectedApis = getSelectedApiData()
	if (selectedApis.length === 0) {
		message.warning('请至少选择一个 API 进行导入')
		return
	}

	// 转换为保存的数据结构
	const apiDataList: ApiData[] = selectedApis.map(api =>
		convertOperationToApiData(api),
	)

	// 批量保存
	const result = await saveApis(apiDataList)

	if (result.success > 0) {
		// 保存成功后，返回到接口列表页面
		setTimeout(() => {
			emit('success')
		}, 1500)
	}
}

// 过滤树节点
const filterTreeData = (nodes: any[], searchText: string): any[] => {
	if (!searchText || !searchText.trim()) {
		return nodes
	}

	const keyword = searchText.toLowerCase().trim()
	const filtered: any[] = []

	nodes.forEach(node => {
		const nodeTitle = (node.title || '').toLowerCase()
		const nodeDescription = (node.description || '').toLowerCase()
		const nodeSummary = (node.summary || '').toLowerCase()
		const nodePath = (node.path || '').toLowerCase()
		const nodeMethod = (node.method || '').toLowerCase()

		// 检查节点本身是否匹配
		const nodeMatches =
			nodeTitle.includes(keyword) ||
			nodeDescription.includes(keyword) ||
			nodeSummary.includes(keyword) ||
			nodePath.includes(keyword) ||
			nodeMethod.includes(keyword)

		// 递归过滤子节点
		let filteredChildren: any[] = []
		if (node.children && node.children.length > 0) {
			filteredChildren = filterTreeData(node.children, searchText)
		}

		// 如果节点匹配或有匹配的子节点，则保留该节点
		if (nodeMatches || filteredChildren.length > 0) {
			filtered.push({
				...node,
				children:
					filteredChildren.length > 0 ? filteredChildren : node.children,
			})
		}
	})

	return filtered
}

// 处理搜索
const handleSearch = () => {
	const value = searchValue.value
	if (!value || !value.trim()) {
		treeData.value = originalTreeData.value
		// 恢复展开状态
		expandedKeys.value = originalTreeData.value.map(node => node.key)
	} else {
		treeData.value = filterTreeData(originalTreeData.value, value)
		// 展开所有过滤后的节点
		const getAllKeys = (nodes: any[]): string[] => {
			let keys: string[] = []
			nodes.forEach(node => {
				keys.push(node.key)
				if (node.children && node.children.length > 0) {
					keys = keys.concat(getAllKeys(node.children))
				}
			})
			return keys
		}
		expandedKeys.value = getAllKeys(treeData.value)
	}
}

// 监听搜索值变化
watch(searchValue, () => {
	handleSearch()
})

// 处理 URL 导入
const handleUrlImport = async () => {
	if (!urlForm.url || !urlForm.url.trim()) {
		message.warning('请输入 URL 地址')
		return
	}

	// 验证 URL 格式
	try {
		// eslint-disable-next-line no-new
		new URL(urlForm.url)
	} catch {
		message.error('URL 格式不正确')
		return
	}

	urlForm.loading = true
	try {
		// 通过后端接口转发请求，避免跨域问题
		const response = await fetchProxyRequest({
			url: urlForm.url,
			method: 'GET',
			headers: {
				Accept: 'application/json, application/yaml, text/yaml, */*',
			},
		})

		// 代理接口现在直接返回原始响应，不再包装
		// 检查响应状态码
		if (response.status < 200 || response.status >= 300) {
			throw new Error(`请求失败，状态码: ${response.status}`)
		}

		// 获取响应数据，可能是字符串或对象
		let text: string = ''
		const responseData = response.data

		// 如果响应是字符串，直接使用
		if (typeof responseData === 'string') {
			text = responseData
		} else if (responseData !== null && responseData !== undefined) {
			// 如果是对象，转换为字符串
			text = JSON.stringify(responseData)
		}

		// 获取 Content-Type 判断文件类型
		const contentType =
			response.headers?.['content-type'] ||
			response.headers?.['Content-Type'] ||
			''

		let apiSpec: any = null

		// 判断是 JSON 还是 YAML
		if (contentType.includes('json') || urlForm.url.endsWith('.json')) {
			// JSON 格式
			try {
				// 如果响应数据已经是对象，直接使用；否则解析字符串
				if (typeof responseData === 'object') {
					apiSpec = responseData
				} else {
					apiSpec = JSON.parse(text)
				}
			} catch (e) {
				message.error(
					'JSON 解析失败：' + (e instanceof Error ? e.message : '未知错误'),
				)
				urlForm.loading = false
				return
			}
		} else if (
			contentType.includes('yaml') ||
			contentType.includes('yml') ||
			urlForm.url.endsWith('.yaml') ||
			urlForm.url.endsWith('.yml')
		) {
			// YAML 解析需要引入 yaml 库，这里先做简单处理
			message.warning('YAML 格式解析功能待实现')
			urlForm.loading = false
			return
		} else {
			// 尝试作为 JSON 解析
			try {
				if (typeof responseData === 'object') {
					apiSpec = responseData
				} else {
					apiSpec = JSON.parse(text)
				}
			} catch {
				message.error('无法识别文件格式，请确保 URL 指向的是 JSON 或 YAML 文件')
				urlForm.loading = false
				return
			}
		}

		// 验证 OpenAPI/Swagger 格式
		if (!validateApiSpec(apiSpec)) {
			message.error(
				'文件格式不正确，请确保是 OpenAPI 3.0、3.1 或 Swagger 2.0 格式',
			)
			urlForm.loading = false
			return
		}

		// 解析并显示树形选择器
		handleParsedApiSpec(apiSpec)
	} catch (error: any) {
		// 处理错误响应
		let errorMessage = '未知错误'
		if (error.response) {
			// 有响应但状态码不是 2xx
			errorMessage = `请求失败，状态码: ${error.response.status}`
			if (error.response.data) {
				const errorData =
					typeof error.response.data === 'string'
						? error.response.data
						: JSON.stringify(error.response.data)
				errorMessage += ` - ${errorData}`
			}
		} else if (error.message) {
			errorMessage = error.message
		}
		message.error('URL 导入失败：' + errorMessage)
	} finally {
		urlForm.loading = false
	}
}

// 自定义上传（不实际上传到服务器，只读取文件内容）
const customRequest: UploadProps['customRequest'] = async options => {
	const { file, onSuccess, onError } = options

	try {
		// 模拟上传成功，实际是读取文件内容
		await processFileContent(file as UploadFile)
		onSuccess?.(file)
	} catch (error: any) {
		onError?.(error)
	}
}
</script>

<template>
	<div class="openapi-import-container">
		<div class="import-header">
			<a-button type="text" class="back-button" @click="emit('back')">
				<i class="ri-arrow-left-line mr-1"></i>
				返回
			</a-button>
		</div>

		<div class="import-content" :class="{ 'has-tree': showTreeSelect }">
			<!-- 说明文字 -->
			<div v-if="!showTreeSelect" class="import-description">
				支持导入 OpenAPI 3.0、3.1 或 Swagger 2.0 数据格式的 JSON 或 YAML 文件
			</div>

			<!-- 标签页 -->
			<a-tabs
				v-if="!showTreeSelect"
				v-model:activeKey="activeTab"
				class="import-tabs"
			>
				<a-tab-pane key="url" tab="URL 导入">
					<div class="url-content">
						<a-form :model="urlForm" layout="vertical" class="url-form">
							<a-form-item label="URL 地址" required>
								<a-input
									v-model:value="urlForm.url"
									placeholder="请输入 OpenAPI/Swagger 文件的 URL 地址"
									size="large"
									:disabled="urlForm.loading"
									@press-enter="handleUrlImport"
								>
									<template #prefix>
										<i class="ri-link"></i>
									</template>
								</a-input>
							</a-form-item>
							<a-form-item>
								<a-button
									type="primary"
									size="large"
									:loading="urlForm.loading"
									block
									@click="handleUrlImport"
								>
									<i class="ri-download-line mr-1"></i>
									导入
								</a-button>
							</a-form-item>
						</a-form>
					</div>
				</a-tab-pane>

				<a-tab-pane key="upload" tab="上传文件">
					<div class="upload-content">
						<a-upload-dragger
							v-model:fileList="fileList"
							name="file"
							:multiple="false"
							:beforeUpload="beforeUpload"
							:customRequest="customRequest"
							accept=".json,.yaml,.yml"
							class="upload-dragger"
							@change="handleUpload"
						>
							<p class="ant-upload-drag-icon">
								<i class="ri-cloud-line"></i>
								<i class="ri-arrow-up-line upload-arrow"></i>
							</p>
							<p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
							<p class="ant-upload-hint">
								支持单个文件上传，仅支持 JSON 或 YAML 格式
							</p>
						</a-upload-dragger>
					</div>
				</a-tab-pane>
			</a-tabs>

			<!-- API 选择树 -->
			<div v-if="showTreeSelect" class="api-select-section">
				<div class="api-select-header">
					<div class="header-title">
						<h3>选择要导入的 API</h3>
						<span class="selected-count">
							已选择 {{ selectedApiCount }} 个 API
						</span>
					</div>
					<a-button type="text" @click="showTreeSelect = false">
						<i class="ri-close-line"></i>
					</a-button>
				</div>
				<div class="api-search-box">
					<a-input
						v-model:value="searchValue"
						placeholder="搜索 API（支持路径、方法、描述等）"
						allow-clear
						size="large"
					>
						<template #prefix>
							<i class="ri-search-line"></i>
						</template>
					</a-input>
				</div>
				<div class="api-tree-container">
					<a-tree
						:tree-data="treeData"
						:checked-keys="checkedKeys"
						:expanded-keys="expandedKeys"
						checkable
						:check-strictly="false"
						class="api-tree"
						@check="handleCheck"
						@expand="handleExpand"
					>
						<template #title="{ title, dataRef }">
							<div class="tree-node-title">
								<span class="node-title-text">{{ title }}</span>
								<span
									v-if="dataRef?.isLeaf && dataRef?.method"
									class="method-badge"
									:class="`method-${dataRef.method.toLowerCase()}`"
								>
									{{ dataRef.method }}
								</span>
								<span
									v-if="dataRef?.summary && dataRef?.isLeaf"
									class="node-summary"
								>
									{{ dataRef.summary }}
								</span>
							</div>
							<div
								v-if="dataRef?.description && !dataRef?.isLeaf"
								class="tree-node-description"
							>
								{{ dataRef.description }}
							</div>
						</template>
					</a-tree>
				</div>
				<div class="api-select-footer">
					<a-space>
						<a-button
							type="primary"
							size="large"
							:disabled="selectedApiCount === 0"
							@click="handleImport"
						>
							<i class="ri-check-line mr-1"></i>
							确认导入 ({{ selectedApiCount }})
						</a-button>
						<a-button size="large" @click="showTreeSelect = false">
							取消
						</a-button>
					</a-space>
				</div>
			</div>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.openapi-import-container {
	display: flex;
	flex-direction: column;
	height: 100%;
	overflow: hidden;

	.import-header {
		padding: 12px 16px;
		border-bottom: 1px solid #f0f0f0;
		.dark & {
			border-bottom-color: #303030;
		}

		.back-button {
			padding: 0;
		}
	}

	.import-content {
		flex: 1;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		padding: 24px;
		max-width: 800px;
		margin: 0 auto;
		width: 100%;

		&.has-tree {
			max-width: 100%;
			padding: 0;
		}

		.import-description {
			margin-bottom: 24px;
			padding: 12px 16px;
			background-color: #f5f5f5;
			border-radius: 6px;
			font-size: 14px;
			color: rgba(0, 0, 0, 0.65);
			line-height: 1.5;
			.dark & {
				background-color: #1f1f1f;
				color: rgba(255, 255, 255, 0.65);
			}
		}

		.import-tabs {
			:deep(.ant-tabs-content-holder) {
				padding-top: 24px;
			}

			.upload-content {
				.upload-dragger {
					:deep(.ant-upload-drag) {
						background-color: #fafafa;
						border: 2px dashed #d9d9d9;
						border-radius: 8px;
						padding: 64px 24px;
						transition: all 0.3s;
						min-height: 300px;
						display: flex;
						flex-direction: column;
						align-items: center;
						justify-content: center;
						.dark & {
							background-color: #1f1f1f;
							border-color: #434343;
						}

						&:hover {
							border-color: #1890ff;
							background-color: #f0f7ff;
							.dark & {
								border-color: #1890ff;
								background-color: rgba(24, 144, 255, 0.05);
							}
						}
					}

					.ant-upload-drag-icon {
						margin-bottom: 16px;
						position: relative;
						display: inline-block;

						i {
							font-size: 64px;
							color: #1890ff;
							display: block;
						}

						.upload-arrow {
							position: absolute;
							top: 50%;
							left: 50%;
							transform: translate(-50%, -50%);
							font-size: 32px;
							margin-top: -8px;
							color: #1890ff;
						}
					}

					.ant-upload-text {
						font-size: 16px;
						color: rgba(0, 0, 0, 0.85);
						margin-bottom: 8px;
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.ant-upload-hint {
						font-size: 14px;
						color: rgba(0, 0, 0, 0.45);
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}
				}
			}

			.url-content {
				.url-form {
					max-width: 600px;
					margin: 0 auto;
				}
			}
		}

		.api-select-section {
			flex: 1;
			display: flex;
			flex-direction: column;
			overflow: hidden;
			background-color: #fff;
			.dark & {
				background-color: #141414;
			}

			.api-select-header {
				display: flex;
				align-items: center;
				justify-content: space-between;
				padding: 16px 24px;
				border-bottom: 1px solid #f0f0f0;
				.dark & {
					border-bottom-color: #303030;
				}

				.header-title {
					display: flex;
					align-items: center;
					gap: 16px;

					h3 {
						margin: 0;
						font-size: 16px;
						font-weight: 500;
						color: rgba(0, 0, 0, 0.85);
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.selected-count {
						font-size: 14px;
						color: rgba(0, 0, 0, 0.45);
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}
				}
			}

			.api-search-box {
				padding: 16px 24px;
				border-bottom: 1px solid #f0f0f0;
				.dark & {
					border-bottom-color: #303030;
				}
			}

			.api-tree-container {
				flex: 1;
				overflow-y: auto;
				padding: 16px 24px;

				.api-tree {
					:deep(.ant-tree-node-content-wrapper) {
						padding: 4px 8px;
						border-radius: 4px;

						&:hover {
							background-color: #f5f5f5;
							.dark & {
								background-color: #1f1f1f;
							}
						}
					}

					.tree-node-title {
						display: flex;
						align-items: center;
						gap: 8px;
						flex-wrap: wrap;

						.node-title-text {
							font-size: 14px;
							color: rgba(0, 0, 0, 0.85);
							.dark & {
								color: rgba(255, 255, 255, 0.85);
							}
						}

						.method-badge {
							display: inline-block;
							padding: 2px 8px;
							border-radius: 4px;
							font-size: 12px;
							font-weight: 500;
							line-height: 1.5;

							&.method-get {
								background-color: #e6f7ff;
								color: #1890ff;
								.dark & {
									background-color: rgba(24, 144, 255, 0.2);
									color: #69c0ff;
								}
							}

							&.method-post {
								background-color: #f6ffed;
								color: #52c41a;
								.dark & {
									background-color: rgba(82, 196, 26, 0.2);
									color: #95de64;
								}
							}

							&.method-put {
								background-color: #fff7e6;
								color: #fa8c16;
								.dark & {
									background-color: rgba(250, 140, 22, 0.2);
									color: #ffc069;
								}
							}

							&.method-delete {
								background-color: #fff1f0;
								color: #f5222d;
								.dark & {
									background-color: rgba(245, 34, 45, 0.2);
									color: #ff7875;
								}
							}

							&.method-patch {
								background-color: #f9f0ff;
								color: #722ed1;
								.dark & {
									background-color: rgba(114, 46, 209, 0.2);
									color: #b37feb;
								}
							}
						}

						.node-summary {
							font-size: 12px;
							color: rgba(0, 0, 0, 0.45);
							.dark & {
								color: rgba(255, 255, 255, 0.45);
							}
						}
					}

					.tree-node-description {
						margin-top: 4px;
						font-size: 12px;
						color: rgba(0, 0, 0, 0.45);
						line-height: 1.5;
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}
				}
			}

			.api-select-footer {
				padding: 16px 24px;
				border-top: 1px solid #f0f0f0;
				.dark & {
					border-top-color: #303030;
				}
			}
		}
	}
}
</style>
