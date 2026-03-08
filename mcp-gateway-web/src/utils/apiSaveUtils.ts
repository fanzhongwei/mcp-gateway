/*
 * @FilePath: /mcp-gateway-web/src/utils/apiSaveUtils.ts
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: API 保存工具函数
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-23
 */
import { message } from 'ant-design-vue'
import { _uuid } from '~/assets/js/util'
import {
	fetchApiAdd,
	fetchApiEdit,
	fetchApiDelete,
	fetchApiInfo,
	fetchApiList,
} from '~/fetch/http'
import type { JsonParamNode } from './jsonParamTypes'

// 从JSON对象解析为jsonParams树形结构
export const parseJsonToParams = (
	json: any,
	parentName = 'root',
): JsonParamNode[] => {
	if (json === null || json === undefined) {
		return []
	}

	if (Array.isArray(json)) {
		// 数组类型
		const firstItem = json.length > 0 ? json[0] : null
		let children: JsonParamNode[] = []

		if (firstItem !== null && typeof firstItem === 'object') {
			// 数组元素是对象类型，解析对象结构
			children = parseJsonToParams(firstItem, 'item')
		} else {
			// 数组元素是基础类型或数组为空，创建 items 节点
			let itemType: JsonParamNode['type'] = 'string'
			if (firstItem !== null) {
				if (typeof firstItem === 'number') {
					itemType = 'number'
				} else if (typeof firstItem === 'boolean') {
					itemType = 'boolean'
				}
			}
			children = [
				{
					id: generateId(),
					name: 'items',
					required: false,
					type: itemType,
					example: firstItem !== null ? String(firstItem) : '',
					description: '',
				},
			]
		}

		return [
			{
				id: generateId(),
				name: parentName,
				required: false,
				type: 'array',
				example: '',
				description: '',
				children,
			},
		]
	}

	if (typeof json === 'object') {
		// 对象类型
		const params: JsonParamNode[] = []
		for (const [key, value] of Object.entries(json)) {
			let type: JsonParamNode['type'] = 'string'
			let children: JsonParamNode[] | undefined

			if (value === null) {
				type = 'string' // null值处理为string类型
			} else if (Array.isArray(value)) {
				type = 'array'
				if (value.length > 0) {
					const firstItem = value[0]
					if (typeof firstItem === 'object' && firstItem !== null) {
						// 数组元素是对象类型，解析对象结构
						children = parseJsonToParams(firstItem, 'item')
					} else {
						// 数组元素是基础类型，创建 items 节点
						let itemType: JsonParamNode['type'] = 'string'
						if (typeof firstItem === 'number') {
							itemType = 'number'
						} else if (typeof firstItem === 'boolean') {
							itemType = 'boolean'
						}
						children = [
							{
								id: generateId(),
								name: 'items',
								required: false,
								type: itemType,
								example: String(firstItem),
								description: '',
							},
						]
					}
				} else {
					// 空数组，创建默认的 items 节点
					children = [
						{
							id: generateId(),
							name: 'items',
							required: false,
							type: 'string',
							example: '',
							description: '',
						},
					]
				}
			} else if (typeof value === 'object') {
				type = 'object'
				children = parseJsonToParams(value, key)
			} else if (typeof value === 'number') {
				type = 'number'
			} else if (typeof value === 'boolean') {
				type = 'boolean'
			}

			params.push({
				id: generateId(),
				name: key,
				required: false,
				type,
				example:
					value !== null && typeof value !== 'object' ? String(value) : '',
				description: '',
				children,
			})
		}
		return params
	}

	// 基本类型
	return [
		{
			id: generateId(),
			name: parentName,
			required: false,
			type:
				typeof json === 'number'
					? 'number'
					: typeof json === 'boolean'
						? 'boolean'
						: 'string',
			example: String(json),
			description: '',
		},
	]
}

// 生成唯一ID（使用共用的uuid方法）
export const generateId = (): string => {
	return _uuid()
}

// 将 JsonParamNode[] 转换为 JSON 对象
const convertParamsToJsonObject = (params: JsonParamNode[]): any => {
	if (params.length === 0) {
		return {}
	}

	// 如果只有一个根节点且名称为 'root'，则返回其值
	if (params.length === 1 && params[0].name === 'root') {
		return convertNodeToValue(params[0])
	}

	// 多个根节点，构建对象
	const result: any = {}
	for (const param of params) {
		if (param.name && param.name !== 'root') {
			result[param.name] = convertNodeToValue(param)
		}
	}
	return result
}

// 将单个 JsonParamNode 转换为对应的值
const convertNodeToValue = (node: JsonParamNode): any => {
	switch (node.type) {
		case 'object': {
			if (node.children && node.children.length > 0) {
				const obj: any = {}
				for (const child of node.children) {
					if (child.name && child.name !== 'items') {
						obj[child.name] = convertNodeToValue(child)
					}
				}
				return obj
			}
			return {}
		}
		case 'array': {
			if (node.children && node.children.length > 0) {
				// 数组类型，查找 items 子节点
				const itemsNode = node.children.find(child => child.name === 'items')
				if (itemsNode) {
					// 如果有示例值，使用示例值；否则使用默认值
					if (itemsNode.example) {
						const exampleValue = convertNodeToValue(itemsNode)
						return [exampleValue]
					}
					// 根据 items 类型返回默认值
					switch (itemsNode.type) {
						case 'number':
							return [0]
						case 'boolean':
							return [false]
						case 'object':
							return [{}]
						case 'array':
							return [[]]
						default:
							return ['']
					}
				}
				// 如果没有 items 节点，但有其他子节点，说明是对象数组
				const objArray: any[] = []
				if (node.children.length > 0) {
					const firstItem: any = {}
					for (const child of node.children) {
						if (child.name && child.name !== 'items') {
							firstItem[child.name] = convertNodeToValue(child)
						}
					}
					if (Object.keys(firstItem).length > 0) {
						objArray.push(firstItem)
					}
				}
				return objArray.length > 0 ? objArray : []
			}
			return []
		}
		case 'number': {
			if (node.example) {
				const num = Number(node.example)
				return isNaN(num) ? 0 : num
			}
			return 0
		}
		case 'boolean': {
			if (node.example) {
				return node.example.toLowerCase() === 'true'
			}
			return false
		}
		case 'string':
		default: {
			return node.example || ''
		}
	}
}

// 将 JsonParamNode[] 转换为格式化的 JSON 字符串
export const convertJsonParamsToJsonString = (
	params: JsonParamNode[],
	indent: number = 2,
): string => {
	try {
		const jsonObject = convertParamsToJsonObject(params)
		return JSON.stringify(jsonObject, null, indent)
	} catch (error) {
		console.error('转换 JSON 参数失败:', error)
		return '{}'
	}
}

// 前端 API 数据结构类型定义
export interface ApiData {
	id: string
	apiForm: {
		method: string
		path: string
		name: string
		description: string
	}
	queryParams: any[]
	headers: any[]
	bodyConfig: {
		type: string
		jsonParams: any[]
		raw: string
	}
	formDataParams: any[]
	urlEncodedParams: any[]
	cookies: any[]
	responseConfig: {
		validateResponse: boolean
		successStatus: string
	}
	systemId?: string
	systemName?: string
	selectedEnv?: any | null
	saveTime: string
	updateTime: string
}

// 后端 API 实体类型定义
interface BackendApi {
	id?: string
	systemId?: string
	method?: string
	path?: string
	name?: string
	description?: string
	queryParams?: any[] // 对象数组
	headers?: any[] // 对象数组
	bodyParam?: any // BodyParam 对象
	urlEncodedParams?: any[] // 对象数组
	cookies?: any[] // 对象数组
	responseConfig?: any // ResponseConfig 对象
	createTime?: string
	modifyTime?: string
}

// 将前端格式转换为后端格式
const convertToBackendFormat = (apiData: ApiData): BackendApi => {
	// 构建 bodyParam：将所有 body 相关参数组装成一个 BodyParam 对象
	const bodyParam: any = {
		type: apiData.bodyConfig?.type || 'json',
		jsonParams: apiData.bodyConfig?.jsonParams || [],
		raw: apiData.bodyConfig?.raw || '',
		formDataParams: apiData.formDataParams || [],
		urlEncodedParams: apiData.urlEncodedParams || [],
	}

	return {
		// 新增时使用前端生成的 uuid，编辑时使用数据原本的 uuid
		id: apiData.id || undefined,
		systemId: apiData.systemId || undefined,
		method: apiData.apiForm?.method || undefined,
		path: apiData.apiForm?.path || undefined,
		name: apiData.apiForm?.name || undefined,
		description: apiData.apiForm?.description || undefined,
		// 直接发送对象数组，而不是JSON字符串
		queryParams:
			apiData.queryParams && apiData.queryParams.length > 0
				? apiData.queryParams
				: undefined,
		headers:
			apiData.headers && apiData.headers.length > 0
				? apiData.headers
				: undefined,
		// 直接发送 BodyParam 对象，而不是JSON字符串
		bodyParam,
		// urlEncodedParams 作为独立字段（与 bodyParam 中的 urlEncodedParams 保持一致）
		urlEncodedParams:
			apiData.urlEncodedParams && apiData.urlEncodedParams.length > 0
				? apiData.urlEncodedParams
				: undefined,
		cookies:
			apiData.cookies && apiData.cookies.length > 0
				? apiData.cookies
				: undefined,
		// 直接发送 ResponseConfig 对象，而不是JSON字符串
		responseConfig: apiData.responseConfig || undefined,
	}
}

// 将后端格式转换为前端格式
const convertToFrontendFormat = (
	backendApi: BackendApi,
	systemName?: string,
	selectedEnv?: any,
): ApiData => {
	// 解析 bodyParam
	let bodyConfig = {
		type: 'json' as string,
		jsonParams: [] as any[],
		raw: '',
	}
	let formDataParams: any[] = []
	let urlEncodedParams: any[] = []

	if (backendApi.bodyParam) {
		const bodyParam = backendApi.bodyParam
		// BodyParam 对象格式
		if (
			bodyParam &&
			typeof bodyParam === 'object' &&
			!Array.isArray(bodyParam)
		) {
			bodyConfig = {
				type: bodyParam.type || 'json',
				jsonParams: Array.isArray(bodyParam.jsonParams)
					? bodyParam.jsonParams
					: [],
				raw: bodyParam.raw || '',
			}
			formDataParams = Array.isArray(bodyParam.formDataParams)
				? bodyParam.formDataParams
				: []
			urlEncodedParams = Array.isArray(bodyParam.urlEncodedParams)
				? bodyParam.urlEncodedParams
				: []
		}
	}

	// 如果 bodyParam 中没有 urlEncodedParams，使用独立的字段
	if (urlEncodedParams.length === 0 && backendApi.urlEncodedParams) {
		urlEncodedParams = Array.isArray(backendApi.urlEncodedParams)
			? backendApi.urlEncodedParams
			: []
	}

	return {
		id: backendApi.id || generateId(),
		apiForm: {
			method: backendApi.method || 'GET',
			path: backendApi.path || '',
			name: backendApi.name || '',
			description: backendApi.description || '',
		},
		queryParams: Array.isArray(backendApi.queryParams)
			? backendApi.queryParams
			: [],
		headers: Array.isArray(backendApi.headers) ? backendApi.headers : [],
		bodyConfig,
		formDataParams,
		urlEncodedParams,
		cookies: Array.isArray(backendApi.cookies) ? backendApi.cookies : [],
		responseConfig:
			backendApi.responseConfig && typeof backendApi.responseConfig === 'object'
				? backendApi.responseConfig
				: {
						validateResponse: false,
						successStatus: '200',
					},
		systemId: backendApi.systemId,
		systemName,
		selectedEnv: selectedEnv || null,
		saveTime: backendApi.createTime || new Date().toISOString(),
		updateTime: backendApi.modifyTime || new Date().toISOString(),
	}
}

// 保存单个 API
export const saveApi = async (
	apiData: ApiData,
	isEditing: boolean = false,
): Promise<boolean> => {
	try {
		// 新增时，确保前端已生成 uuid
		if (!isEditing && !apiData.id) {
			apiData.id = generateId()
		}

		// 编辑时，确保使用数据原本的 uuid
		if (isEditing && !apiData.id) {
			message.error('编辑时缺少接口ID')
			return false
		}

		const backendData = convertToBackendFormat(apiData)

		if (isEditing) {
			// 编辑模式：调用修改接口，使用数据原本的 uuid
			await fetchApiEdit(backendData)
			message.success('接口已更新')
		} else {
			// 新增模式：调用新增接口，使用前端生成的 uuid
			await fetchApiAdd(backendData)
			message.success('接口已保存')
		}

		return true
	} catch (error: any) {
		console.error('保存失败:', error)
		const errorMsg =
			error?.response?.data?.message || error?.message || '保存失败，请稍后重试'
		message.error(errorMsg)
		return false
	}
}

// 批量保存 API（支持合并重复接口）
export const saveApis = async (
	apis: ApiData[],
): Promise<{ success: number; failed: number; merged: number }> => {
	let success = 0
	let failed = 0
	let merged = 0

	try {
		// 先查询现有接口列表，用于检查重复
		// 获取所有要导入接口的 systemId（可能不同）
		const systemIds = [
			...new Set(apis.map(api => api.systemId).filter(Boolean)),
		]
		const existingApis: BackendApi[] = []

		// 查询每个 systemId 的接口列表
		for (const systemId of systemIds) {
			try {
				const response = await fetchApiList({
					page: {
						current: 1,
						size: 9999, // 获取所有数据
					},
					entityParam: { systemId },
				})
				const records = response.data?.records || []
				existingApis.push(...records)
			} catch (error) {
				console.error(`查询 systemId ${systemId} 的接口失败:`, error)
			}
		}

		// 处理每个要导入的接口
		for (const newApi of apis) {
			try {
				// 检查是否存在重复的接口（通过 path + method + systemId）
				const existingApi = existingApis.find((api: BackendApi) => {
					const path1 = (api.path || '').trim()
					const path2 = (newApi.apiForm?.path || '').trim()
					const method1 = (api.method || '').trim().toUpperCase()
					const method2 = (newApi.apiForm?.method || '').trim().toUpperCase()
					const systemId1 = api.systemId || ''
					const systemId2 = newApi.systemId || ''

					return (
						path1 === path2 && method1 === method2 && systemId1 === systemId2
					)
				})

				if (existingApi?.id) {
					// 存在重复，更新现有接口，使用数据原本的 uuid
					newApi.id = existingApi.id
					const backendData = convertToBackendFormat(newApi)
					await fetchApiEdit(backendData)
					merged++
					success++
				} else {
					// 不存在重复，新增接口，确保前端已生成 uuid
					if (!newApi.id) {
						newApi.id = generateId()
					}
					const backendData = convertToBackendFormat(newApi)
					await fetchApiAdd(backendData)
					success++
				}
			} catch (error) {
				console.error('处理接口失败:', error)
				failed++
			}
		}

		// 显示提示信息
		if (success > 0) {
			const mergedText = merged > 0 ? `，合并 ${merged} 个重复接口` : ''
			const failedText = failed > 0 ? `，${failed} 个失败` : ''
			message.success(`成功保存 ${success} 个接口${mergedText}${failedText}`)
		} else if (failed > 0) {
			message.error(`保存失败，共 ${failed} 个接口`)
		}
	} catch (error) {
		console.error('批量保存失败:', error)
		message.error('批量保存失败，请稍后重试')
		failed = apis.length
	}

	return { success, failed, merged }
}

// 删除 API
export const deleteApi = async (id: string): Promise<boolean> => {
	try {
		await fetchApiDelete(id)
		message.success('删除成功')
		return true
	} catch (error: any) {
		console.error('删除失败:', error)
		const errorMsg =
			error?.response?.data?.message || error?.message || '删除失败，请稍后重试'
		message.error(errorMsg)
		return false
	}
}

// 获取单个 API
export const getApi = async (
	id: string,
	systemName?: string,
	selectedEnv?: any,
): Promise<ApiData | null> => {
	try {
		const response = await fetchApiInfo(id)
		const backendApi = response.data as BackendApi
		return convertToFrontendFormat(backendApi, systemName, selectedEnv)
	} catch (error: any) {
		console.error('获取接口失败:', error)
		const errorMsg =
			error?.response?.data?.message ||
			error?.message ||
			'获取接口失败，请稍后重试'
		message.error(errorMsg)
		return null
	}
}

// 获取 API 列表（根据 systemId 过滤）
export const getApiList = async (
	systemId?: string,
	systemName?: string,
	selectedEnv?: any,
): Promise<ApiData[]> => {
	try {
		const response = await fetchApiList({
			page: {
				current: 1,
				size: 9999, // 获取所有数据
			},
			entityParam: systemId ? { systemId } : {},
		})

		const records = response.data?.records || []
		return records.map((backendApi: BackendApi) =>
			convertToFrontendFormat(backendApi, systemName, selectedEnv),
		)
	} catch (error: any) {
		console.error('获取接口列表失败:', error)
		const errorMsg =
			error?.response?.data?.message ||
			error?.message ||
			'获取接口列表失败，请稍后重试'
		message.error(errorMsg)
		return []
	}
}
