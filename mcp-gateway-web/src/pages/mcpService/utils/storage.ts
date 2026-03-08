/*
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/utils/storage.ts
 * @Author: teddy
 * @Date: 2026-01-24
 * @Description: MCP服务数据存储工具（使用localStorage）
 */

// 资源组合接口
export interface ResourceCombo {
	id: string
	systemId: string
	systemName: string
	envId: string
	envName: string
	envBaseUrl: string
	apiIds: string[]
	customMcpName?: string // 系统级别的自定义 MCP 名字
	apis: Array<{
		id: string
		name: string
		method: string
		path: string
		customMcpName?: string // 接口级别的自定义 MCP 名字
	}>
}

// MCP服务数据接口
export interface McpServiceData {
	id: string
	name: string
	desc: string
	status: 'draft' | 'published' | 'stopped'
	config: {
		selections: Array<{
			systemId: string
			systemName: string
			envId: string
			envName: string
			envBaseUrl: string
			apiIds: string[]
			customMcpName?: string // 系统级别的自定义 MCP 名字
			apiCustomMcpNames?: Record<string, string> // 接口 ID 到自定义 MCP 名字的映射
		}>
		auth?: {
			type: 'bearer' | 'basic' | 'none'
			token?: string
		}
	}
	accessToken: string
	endpoint: string
	createTime: string
	modifyTime: string
	publishTime?: string
}

const STORAGE_KEY = 'mcp-services'

// 生成UUID
const generateId = (): string => {
	return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

// 获取所有服务
export const getAllServices = (): McpServiceData[] => {
	try {
		const data = localStorage.getItem(STORAGE_KEY)
		return data ? JSON.parse(data) : []
	} catch (error) {
		console.error('获取服务列表失败:', error)
		return []
	}
}

// 根据ID获取服务
export const getServiceById = (id: string): McpServiceData | null => {
	const services = getAllServices()
	return services.find(s => s.id === id) || null
}

// 保存服务（新增或更新）
export const saveService = (
	service: Partial<McpServiceData>,
): McpServiceData => {
	const services = getAllServices()
	const now = new Date().toISOString()

	if (service.id) {
		// 更新
		const index = services.findIndex(s => s.id === service.id)
		if (index !== -1) {
			services[index] = {
				...services[index],
				...service,
				modifyTime: now,
			} as McpServiceData
		}
	} else {
		// 新增
		const newService: McpServiceData = {
			id: generateId(),
			name: service.name || '',
			desc: service.desc || '',
			status: service.status || 'draft',
			config: service.config || { selections: [] },
			accessToken: service.accessToken || generateId(),
			endpoint: service.endpoint || '',
			createTime: now,
			modifyTime: now,
			...service,
		}
		services.push(newService)
	}

	localStorage.setItem(STORAGE_KEY, JSON.stringify(services))
	return service.id
		? services.find(s => s.id === service.id)!
		: services[services.length - 1]
}

// 删除服务
export const deleteService = (id: string): boolean => {
	try {
		const services = getAllServices()
		const filtered = services.filter(s => s.id !== id)
		localStorage.setItem(STORAGE_KEY, JSON.stringify(filtered))
		return true
	} catch (error) {
		console.error('删除服务失败:', error)
		return false
	}
}

// 发布服务
export const publishService = (id: string): boolean => {
	try {
		const services = getAllServices()
		const service = services.find(s => s.id === id)
		if (!service) return false

		service.status = 'published'
		service.publishTime = new Date().toISOString()
		service.modifyTime = new Date().toISOString()

		localStorage.setItem(STORAGE_KEY, JSON.stringify(services))
		return true
	} catch (error) {
		console.error('发布服务失败:', error)
		return false
	}
}

// 停止服务
export const stopService = (id: string): boolean => {
	try {
		const services = getAllServices()
		const service = services.find(s => s.id === id)
		if (!service) return false

		service.status = 'stopped'
		service.modifyTime = new Date().toISOString()

		localStorage.setItem(STORAGE_KEY, JSON.stringify(services))
		return true
	} catch (error) {
		console.error('停止服务失败:', error)
		return false
	}
}

// 重新生成访问令牌
export const regenerateAccessToken = (id: string): string | null => {
	try {
		const services = getAllServices()
		const service = services.find(s => s.id === id)
		if (!service) return null

		const newToken = generateId()
		service.accessToken = newToken
		service.modifyTime = new Date().toISOString()

		localStorage.setItem(STORAGE_KEY, JSON.stringify(services))
		return newToken
	} catch (error) {
		console.error('重新生成令牌失败:', error)
		return null
	}
}
