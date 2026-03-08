/*
 * @FilePath: /mcp-gateway-web/src/fetch/http.js
 * @Author: teddy
 * @Date: 2024-02-21 20:18:30
 * @Description: ajax
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-13 21:24:35
 */
import { message } from 'ant-design-vue'
import axios from 'axios'

const http = axios.create({
	baseURL: '/mcp-gateway/api/v1',
	timeout: 60000, // 60秒超时，适用于大多数接口请求
	// `responseType` 表示服务器响应的数据类型，可以是 'arraybuffer', 'blob', 'document', 'json', 'text', 'stream'
})

http.interceptors.request.use(
	config => {
		// token
		const payToken = useCookie('pay-token')
		config.headers.Authorization = payToken.value || null
		config.responseType = 'json'
		// 存储 ajax 请求带 entityParam 参数的数据
		const entityParam = useState('entity-param')
		try {
			entityParam.value = config.data.entityParam
		} catch (error) {}

		// 对导出接口 responseType = blob
		if (config.url.includes('export') || config.url.includes('template')) {
			config.responseType = 'blob'
		}

		return config
	},
	error => {
		return Promise.reject(error)
	},
)

http.interceptors.response.use(
	response => {
		// token
		const payToken = useCookie('pay-token')
		payToken.value = response.headers.authorization || payToken.value
		// 需要过滤的接口
		const filterUrl = [
			{ method: 'get', url: '/code/list' },
			{ method: 'post', url: '/code/page' },
		]
		// 拦截码值数据中valid为2（预留码值不可操作）的码值信息
		const isFilter = filterUrl.some(urlItem => {
			return (
				response.config.url.includes(urlItem.url) &&
				response.config.method === urlItem.method
			)
		})
		if (isFilter) {
			const records = response.data.records
				? response.data.records
				: response.data
			const validCodeList = records.filter(codeItem => {
				return !['2'].includes(codeItem.valid)
			})
			if (response.data.records) {
				response.data.records = validCodeList
			} else {
				response.data = validCodeList
			}
		}

		return response
	},
	error => {
		message.destroy()
		if (error.response.status === 401) {
			message.error('登录状态已过期，请重新登录！')
			// token
			const payToken = useCookie('pay-token')
			payToken.value = null
			// 记录 401 之前的前路由
			const route = useRoute()
			const payOldRouteName401 = useCookie('pay-old-route-name-401')
			payOldRouteName401.value = route.name
			// 登录状态过期，跳转登录页
			navigateTo({
				name: 'login',
				query: { entered: true, ...route.query },
			})
		} else {
			message.error(error.response.data.message || '系统错误，请联系管理员！')
		}
		return Promise.reject(error)
	},
)

// 开发工具-缓存清理-刷新码值缓存
export const fetchCacheClearCode = () => http.get('/code/cache/reload')

// 开发工具-缓存清理-刷新对外接口授权缓存
export const fetchCacheClearDwjkAccess = () =>
	http.get('/dwjkAccess/cache/reload')

// 开发工具-缓存清理-刷新租户缓存
export const fetchCacheClearOrgan = () => http.get('/organ/cache/reload')

// 开发工具-代码值--分页查询接口
export const fetchCodePage = data =>
	http.post('/code/page', {
		...data,
		page: {
			current: 1,
			size: 9999,
			orders: [
				{
					column: 'nOrder',
					asc: true,
				},
			],
		},
	})

// 开发工具-码值接口-根据代码类型获取代码集合
export const fetchCodeList = codeType => http.get(`/code/list/${codeType}`)

// 开发工具-代码类型--分页查询接口
export const fetchCodeTypePage = data =>
	http.post('/codeType/page', {
		...data,
		page: {
			current: 1,
			size: 9999,
		},
		entityParam: {},
	})
// 开发工具-代码值--更新代码类型对应码值列表，代码值顺序同接口传入顺序
export const fetchCodeTypeUpdate = (codeType, data) =>
	http.put(`code/list/${codeType}`, data)

// 用户-登录
export const fetchLogin = data => http.post('/login', data)

// 用户-登出
export const fetchLogout = () => http.get('/logout')

// 用户-列表
export const fetchUserList = data => http.post(`/user/page`, data)

// 用户-列表-导出
export const fetchUserListExport = data => http.post(`/user/page/export`, data)

// 用户-信息
export const fetchUserInfo = id => http.get(`/user/${id}`)

// 用户-新增
export const fetchUserAdd = data => http.post(`/user`, data)

// 用户-编辑
export const fetchUserEdit = data => http.put(`/user`, data)

// 用户-删除
export const fetchUserDelete = id => http.delete(`/user/${id}`)

// 用户-修改角色
export const fetchUserEditRole = data => http.put(`/user/role`, data)

// 角色-新增
export const fetchRoleAdd = data => http.post(`/role`, data)

// 角色-修改
export const fetchRoleEdit = data => http.put(`/role`, data)

// 角色-查询
export const fetchRoleIdInfo = id => http.get(`/roleRight/${id}/list`)

// 角色-分页查询
export const fetchRoleList = data => http.post(`/role/page`, data)

// 角色-删除
export const fetchRoleDelete = id => http.delete(`/role/${id}`)

// 权限点-所有权限点
export const fetchPermissions = () => http.get('/rights/all')

// 权限点-获取用户权限点
export const fetchUserPermissions = id => http.get(`/rights/user/${id}`)

// 权限点-获取权限点信息
export const fetchPermissionKey = key => http.get(`/right/${key}`)

// 权限点-添加
export const fetchAddPermission = data => http.post('/right', data)

// 权限点-编辑
export const fetchEditPermission = data => http.put('/right', data)

// 权限点-删除
export const fetchDeletePermission = key => http.delete(`/right/${key}`)

// 租户-all
export const fetchOrgAll = () => http.get('/organ/tree')

// 租户-新增
export const fetchTenantAdd = data => http.post('/tenant', data)

// 租户-编辑
export const fetchTenantEdit = data => http.put('/tenant', data)

// 租户-删除
export const fetchTenantDelete = id => http.delete(`/tenant/${id}`)

// 部门-新增
export const fetchDeptAdd = data => http.post('/dept', data)

// 部门-编辑
export const fetchDeptEdit = data => http.put('/dept', data)

// 部门-删除
export const fetchDeptDelete = id => http.delete(`/dept/${id}`)

// 授权管理-分页查询
export const fetchAuthorizationList = data =>
	http.post(`/dwjkAccess/page`, data)

// 授权管理-分页查询-导出
export const fetchAuthorizationListExport = data =>
	http.post(`/dwjkAccess/page/export`, data)

// 授权管理-新增
export const fetchAuthorizationAdd = data => http.post(`/dwjkAccess`, data)

// 授权管理-编辑
export const fetchAuthorizationEdit = data => http.put(`/dwjkAccess`, data)

// 授权管理-删除
export const fetchAuthorizationDelete = id => http.delete(`/dwjkAccess/${id}`)

// 获取 aliyun oss 配置
export const fetchAliyunOssConfig = () => http.get(`/oss/signature`)

// 日志分页
export const fetchLogList = data => http.post(`/apiLog/page`, data)

// 日志分页-导出
export const fetchLogListExport = data => http.post(`/apiLog/page/export`, data)

// 业务系统-分页查询
export const fetchSystemList = data => http.post(`/system/page`, data)

// 业务系统-新增
export const fetchSystemAdd = data => http.post(`/system`, data)

// 业务系统-编辑
export const fetchSystemEdit = data => http.put(`/system`, data)

// 业务系统-删除
export const fetchSystemDelete = id => http.delete(`/system/${id}`)

// 业务系统-查询单个
export const fetchSystemInfo = id => http.get(`/system/${id}`)

// 系统环境-分页查询
export const fetchSystemEnvList = data => http.post(`/systemEnv/page`, data)

// 系统环境-新增
export const fetchSystemEnvAdd = data => http.post(`/systemEnv`, data)

// 系统环境-编辑
export const fetchSystemEnvEdit = data => http.put(`/systemEnv`, data)

// 系统环境-删除
export const fetchSystemEnvDelete = id => http.delete(`/systemEnv/${id}`)

// 系统环境-查询单个
export const fetchSystemEnvInfo = id => http.get(`/systemEnv/${id}`)

// 接口转发-用于解决跨域问题（设置更长的超时时间，因为可能转发到外部接口）
export const fetchProxyRequest = (data, config = {}) => {
	const defaultConfig = {
		timeout: 120000, // 120秒超时，适用于转发到外部接口的请求
		responseType: 'blob', // 使用 blob 接收响应，支持文件下载
	}
	return http.post(`/proxy/request`, data, { ...defaultConfig, ...config })
}

// API接口-新增
export const fetchApiAdd = data => http.post(`/api`, data)

// API接口-修改
export const fetchApiEdit = data => http.put(`/api`, data)

// API接口-删除
export const fetchApiDelete = id => http.delete(`/api/${id}`)

// API接口-查询单个
export const fetchApiInfo = id => http.get(`/api/${id}`)

// API接口-分页查询
export const fetchApiList = data => http.post(`/api/page`, data)

// MCP服务-分页查询
export const fetchMcpServiceList = data => http.post(`/mcpService/page`, data)

// MCP服务-查询单个
export const fetchMcpServiceInfo = id => http.get(`/mcpService/${id}`)

// MCP服务-查询详情（包含关联的系统、环境、API信息）
export const fetchMcpServiceDetail = id => http.get(`/mcpService/${id}/detail`)

// MCP服务-保存或更新
export const fetchMcpServiceSaveOrUpdate = data =>
	http.post(`/mcpService/saveOrUpdate`, data)

// MCP服务-删除
export const fetchMcpServiceDelete = id => http.delete(`/mcpService/${id}`)

// MCP服务-发布
export const fetchMcpServicePublish = id =>
	http.post(`/mcpService/${id}/publish`)

// MCP服务-停止
export const fetchMcpServiceStop = id => http.post(`/mcpService/${id}/stop`)
