<!--
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/index.vue
 * @Author: teddy
 * @Date: 2026-01-24
 * @Description: MCP服务管理主页面
-->
<script setup lang="tsx">
import { Modal, message } from 'ant-design-vue'
import {
	_deleteModal,
	_getPermissionsData,
	_tmeMsg,
	_toDayjs,
} from '@/assets/js/util'
import {
	fetchMcpServiceList,
	fetchMcpServiceDelete,
	fetchMcpServicePublish,
	fetchMcpServiceStop,
	fetchMcpServiceDetail,
} from '~/fetch/http'
import ServiceStatusTag from './components/ServiceStatusTag.vue'
import Save from './Save.vue'

// table ref
const tableRef = ref()
const searchName = ref(null)
const searchStatus = ref<string | null>(null)

const changeTableInput = () => {
	if (!searchName.value) {
		tableRef.value.getTableList(1)
	}
}

// table-ajax
const getServiceList = async (page: any) => {
	try {
		const { current, size } = page.page
		const entityParam: any = {}

		// 搜索过滤
		if (searchName.value) {
			entityParam.name = searchName.value
		}

		// 状态过滤
		if (searchStatus.value) {
			entityParam.status = searchStatus.value
		}

		const response = await fetchMcpServiceList({
			page: {
				current,
				size,
			},
			entityParam,
		})

		const records = response.data?.records || []

		// 直接使用后端返回的数据，不需要额外处理
		const recordsWithStats = records.map((service: any) => ({
			...service,
			apiCount: service.apiCount || 0,
		}))

		return {
			data: {
				records: recordsWithStats,
				total: response.data?.total || 0,
			},
		}
	} catch (error) {
		console.error('获取服务列表失败:', error)
		return Promise.reject(error)
	}
}

// table-th
const columns = reactive<any>([
	{
		title: '序号',
		dataIndex: 'index',
		width: 80,
		customFilterDropdown: true,
	},
	{
		title: '服务名称',
		dataIndex: 'name',
		customRender: (row: any) => {
			return (
				<a-button type='link' onClick={() => handleViewDetail(row.record)}>
					{row.record.name}
				</a-button>
			)
		},
	},
	{
		title: '服务描述',
		dataIndex: 'desc',
		customRender: (row: any) => {
			const desc = row.record.desc || '-'
			return desc.length > 100 ? desc.substring(0, 100) + '...' : desc
		},
	},
	{
		title: '状态',
		dataIndex: 'status',
		width: 100,
		customRender: (row: any) => {
			return <ServiceStatusTag status={row.record.status} />
		},
	},
	{
		title: '接口总数',
		dataIndex: 'apiCount',
		width: 100,
	},
	{
		title: '创建时间',
		dataIndex: 'createTime',
		width: 180,
		customRender: (row: any) =>
			row.record.createTime
				? _toDayjs(row.record.createTime, 'YYYY-MM-DD HH:mm:ss')
				: '-',
	},
	{
		title: '发布时间',
		dataIndex: 'publishTime',
		width: 180,
		customRender: (row: any) =>
			row.record.publishTime
				? _toDayjs(row.record.publishTime, 'YYYY-MM-DD HH:mm:ss')
				: '-',
	},
	{
		title: '操作',
		dataIndex: 'active',
		width: 300,
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div class='flex'>
					<a-button
						size='small'
						type='link'
						onClick={() => {
							handleEdit(data)
						}}
					>
						<i class='ri-edit-box-line mr-1'></i>
						编辑
					</a-button>
					{data.status === 'draft' || data.status === 'stopped' ? (
						<a-button
							size='small'
							type='link'
							onClick={() => {
								handlePublish(data.id, data.name)
							}}
						>
							<i class='ri-play-line mr-1'></i>
							发布
						</a-button>
					) : (
						<a-button
							size='small'
							type='link'
							onClick={() => {
								handleStop(data.id, data.name)
							}}
						>
							<i class='ri-stop-line mr-1'></i>
							停止
						</a-button>
					)}
					<a-button
						type='link'
						danger
						size='small'
						onClick={() => {
							handleDelete(data.id, data.name)
						}}
					>
						<i class='ri-delete-bin-line mr-1'></i>
						删除
					</a-button>
					<a-button
						size='small'
						type='link'
						onClick={() => {
							handleViewDetail(data)
						}}
					>
						<i class='ri-eye-line mr-1'></i>
						详情
					</a-button>
				</div>
			)
		},
	},
])

// 服务编辑弹窗
const saveFormData = reactive<any>({
	visible: false,
	serviceId: null,
})

// 服务详情弹窗
const detailFormData = reactive<any>({
	visible: false,
	service: null,
})

// 编辑服务
const handleEdit = (row: any) => {
	saveFormData.serviceId = row.id
	saveFormData.visible = true
}

// 新增服务
const handleAdd = () => {
	saveFormData.serviceId = null
	saveFormData.visible = true
}

// 删除服务
const handleDelete = (id: string, name: string) => {
	_deleteModal(
		'MCP服务',
		name,
		async () => {
			try {
				await fetchMcpServiceDelete(id)
				_tmeMsg.success('删除成功')
				tableRef.value.getTableList()
			} catch (error) {
				_tmeMsg.error('删除失败')
			}
		},
		() => tableRef.value.getTableList(),
	)
}

// 发布服务
const handlePublish = (id: string, name: string) => {
	Modal.confirm({
		title: '确认发布',
		content: `确定要发布服务"${name}"吗？`,
		okText: '确定',
		cancelText: '取消',
		async onOk() {
			try {
				await fetchMcpServicePublish(id)
				_tmeMsg.success('发布成功')
				tableRef.value.getTableList()
			} catch (error) {
				_tmeMsg.error('发布失败')
			}
		},
	})
}

// 停止服务
const handleStop = (id: string, name: string) => {
	Modal.confirm({
		title: '确认停止',
		content: `确定要停止服务"${name}"吗？`,
		okText: '确定',
		cancelText: '取消',
		async onOk() {
			try {
				await fetchMcpServiceStop(id)
				_tmeMsg.success('停止成功')
				tableRef.value.getTableList()
			} catch (error) {
				_tmeMsg.error('停止失败')
			}
		},
	})
}

// 查看详情
const handleViewDetail = async (row: any) => {
	try {
		const response = await fetchMcpServiceDetail(row.id)
		const detail = response.data
		if (detail) {
			// 转换为前端格式
			const service = {
				id: detail.id,
				name: detail.name,
				desc: detail.desc,
				status: detail.status,
				accessToken: detail.accessToken,
				endpoint: detail.endpoint,
				createTime: detail.createTime,
				publishTime: detail.publishTime,
				config: {
					selections: (detail.configApis || []).map((configApi: any) => ({
						systemId: configApi.systemId,
						systemName: configApi.systemName,
						envId: configApi.envId,
						envName: configApi.envName,
						envBaseUrl: configApi.envBaseUrl,
						customMcpName: configApi.customMcpName,
						apis: (configApi.apis || []).map((api: any) => ({
							id: api.id,
							name: api.name,
							method: api.method,
							path: api.path,
							customMcpName: api.customMcpName,
						})),
					})),
				},
			}
			detailFormData.service = service
			detailFormData.visible = true
		}
	} catch (error) {
		console.error('获取服务详情失败:', error)
		_tmeMsg.error('获取服务详情失败')
	}
}

// 关闭编辑弹窗
const handleSaveClose = () => {
	saveFormData.visible = false
	saveFormData.serviceId = null
	tableRef.value.getTableList()
}

// 获取HTTP方法对应的颜色
const getMethodColor = (method: string) => {
	const methodUpper = method?.toUpperCase() || ''
	switch (methodUpper) {
		case 'GET':
			return 'blue'
		case 'POST':
			return 'green'
		case 'PUT':
			return 'orange'
		case 'DELETE':
			return 'red'
		case 'PATCH':
			return 'purple'
		default:
			return 'default'
	}
}

// 获取完整的服务端点URL（包含context-path）
const getFullEndpointUrl = (endpoint: string) => {
	if (!endpoint) return ''
	// context-path 从 nuxt.config.ts 中配置的 baseURL 获取
	const baseURL = '/mcp-gateway'
	const origin = window.location.origin
	// 确保 endpoint 以 / 开头
	const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
	// 确保 baseURL 不以 / 结尾
	const cleanBaseURL = baseURL.endsWith('/') ? baseURL.slice(0, -1) : baseURL
	return `${origin}${cleanBaseURL}${cleanEndpoint}`
}

// 复制访问令牌
const copyAccessToken = async (token: string) => {
	try {
		await navigator.clipboard.writeText(token)
		message.success('访问令牌已复制到剪贴板')
	} catch (error) {
		message.error('复制失败，请手动复制')
	}
}

// 复制端点URL
const copyEndpoint = async (endpoint: string) => {
	try {
		const fullUrl = getFullEndpointUrl(endpoint)
		await navigator.clipboard.writeText(fullUrl)
		message.success('服务端点已复制到剪贴板')
	} catch (error) {
		message.error('复制失败，请手动复制')
	}
}

// 生成客户端配置JSON
const getClientConfigJson = (service: any) => {
	const baseURL = '/mcp-gateway'
	const origin = window.location.origin
	const cleanEndpoint = service.endpoint?.startsWith('/')
		? service.endpoint
		: `/${service.endpoint || ''}`
	const cleanBaseURL = baseURL.endsWith('/') ? baseURL.slice(0, -1) : baseURL
	const fullUrl = `${origin}${cleanBaseURL}${cleanEndpoint}`

	const config = {
		timeout: 60,
		type: 'streamableHttp',
		url: fullUrl,
		headers: {
			Authorization: `Bearer ${service.accessToken}`,
		},
	}

	// 只返回 "mcp-gateway-http": { ... } 这一段配置
	return `"mcp-gateway-http": ${JSON.stringify(config, null, 2)}`
}

// 复制客户端配置
const copyClientConfig = async (service: any) => {
	try {
		const configJson = getClientConfigJson(service)
		await navigator.clipboard.writeText(configJson)
		message.success('客户端配置已复制到剪贴板')
	} catch (error) {
		message.error('复制失败，请手动复制')
	}
}

onMounted(() => {
	_getPermissionsData()
})
</script>

<template>
	<!-- table -->
	<pay-table ref="tableRef" :ajax="getServiceList" :columns="columns">
		<template #search>
			<div class="flex gap-3">
				<a-input
					v-model:value="searchName"
					placeholder="请输入服务名称或描述"
					allow-clear
					@change="changeTableInput"
					@press-enter="() => tableRef.getTableList(1)"
				></a-input>
				<a-select
					v-model:value="searchStatus"
					placeholder="请选择状态"
					allow-clear
					style="width: 150px"
					@change="() => tableRef.getTableList(1)"
				>
					<a-select-option value="draft">草稿</a-select-option>
					<a-select-option value="published">已发布</a-select-option>
					<a-select-option value="stopped">已停止</a-select-option>
				</a-select>
				<a-button type="primary" @click="() => tableRef.getTableList(1)">
					<i class="ri-search-line mr-1"></i>
					查询
				</a-button>
			</div>
			<a-button type="primary" class="add-button" @click="handleAdd">
				<i class="ri-add-line mr-1"></i>
				新增服务
			</a-button>
		</template>
	</pay-table>

	<!-- 编辑弹窗 -->
	<Save
		v-model:visible="saveFormData.visible"
		:service-id="saveFormData.serviceId"
		@close="handleSaveClose"
	/>

	<!-- 详情弹窗 -->
	<a-modal
		v-model:open="detailFormData.visible"
		title="服务详情"
		width="800px"
		:footer="null"
	>
		<div v-if="detailFormData.service" class="service-detail">
			<a-descriptions :column="2" bordered>
				<a-descriptions-item label="服务名称">
					{{ detailFormData.service.name }}
				</a-descriptions-item>
				<a-descriptions-item label="服务状态">
					<ServiceStatusTag :status="detailFormData.service.status" />
				</a-descriptions-item>
				<a-descriptions-item label="服务描述" :span="2">
					{{ detailFormData.service.desc || '-' }}
				</a-descriptions-item>
				<a-descriptions-item label="访问令牌">
					<div class="copyable-text">
						<span>{{ detailFormData.service.accessToken }}</span>
						<a-button
							type="text"
							size="small"
							class="copy-btn"
							@click="copyAccessToken(detailFormData.service.accessToken)"
						>
							<i class="ri-file-copy-line"></i>
						</a-button>
					</div>
				</a-descriptions-item>
				<a-descriptions-item label="服务端点">
					<div class="copyable-text">
						<span>
							{{ getFullEndpointUrl(detailFormData.service.endpoint) }}
						</span>
						<a-button
							type="text"
							size="small"
							class="copy-btn"
							@click="copyEndpoint(detailFormData.service.endpoint)"
						>
							<i class="ri-file-copy-line"></i>
						</a-button>
					</div>
				</a-descriptions-item>
				<a-descriptions-item label="客户端配置" :span="2">
					<a-tooltip placement="top" :overlay-style="{ maxWidth: '600px' }">
						<template #title>
							<pre class="config-tooltip">{{
								getClientConfigJson(detailFormData.service)
							}}</pre>
						</template>
						<div class="copyable-text">
							<span>客户端MCP配置示例</span>
							<a-button
								type="text"
								size="small"
								class="copy-btn"
								@click="copyClientConfig(detailFormData.service)"
							>
								<i class="ri-file-copy-line"></i>
							</a-button>
						</div>
					</a-tooltip>
				</a-descriptions-item>
				<a-descriptions-item label="创建时间">
					{{
						detailFormData.service.createTime
							? _toDayjs(
									detailFormData.service.createTime,
									'YYYY-MM-DD HH:mm:ss',
								)
							: '-'
					}}
				</a-descriptions-item>
				<a-descriptions-item label="发布时间">
					{{
						detailFormData.service.publishTime
							? _toDayjs(
									detailFormData.service.publishTime,
									'YYYY-MM-DD HH:mm:ss',
								)
							: '-'
					}}
				</a-descriptions-item>
			</a-descriptions>

			<a-divider>资源组合</a-divider>

			<div class="combo-cards">
				<div
					v-for="(selection, index) in detailFormData.service.config.selections"
					:key="index"
					class="combo-card"
				>
					<div class="combo-header">
						<div class="combo-title">
							<span class="system-name">{{ selection.systemName }}</span>
							<span class="separator">/</span>
							<span class="env-name">{{ selection.envName }}</span>
							<a-tag v-if="selection.apis" color="blue" class="api-count">
								{{ selection.apis.length }} 个接口
							</a-tag>
							<a-tag v-else color="blue" class="api-count">0 个接口</a-tag>
							<a-tag
								v-if="selection.customMcpName"
								color="purple"
								class="mcp-name-tag"
							>
								MCP系统名: {{ selection.customMcpName }}
							</a-tag>
						</div>
					</div>

					<div
						v-if="selection.apis && selection.apis.length > 0"
						class="combo-apis"
					>
						<div v-for="api in selection.apis" :key="api.id" class="api-item">
							<a-tag :color="getMethodColor(api.method)">
								{{ api.method }}
							</a-tag>
							<span class="api-path">{{ api.path }}</span>
							<span class="api-name">{{ api.name }}</span>
							<a-tag
								v-if="api.customMcpName"
								color="orange"
								class="mcp-name-tag"
							>
								MCP工具名: {{ api.customMcpName }}
							</a-tag>
						</div>
					</div>
				</div>
			</div>
		</div>
	</a-modal>
</template>

<style lang="scss" scoped>
.service-detail {
	.copyable-text {
		display: flex;
		align-items: center;
		gap: 8px;

		span {
			flex: 1;
			word-break: break-all;
		}

		.copy-btn {
			flex-shrink: 0;
			color: rgba(0, 0, 0, 0.45);
			.dark & {
				color: rgba(255, 255, 255, 0.45);
			}

			&:hover {
				color: rgba(0, 0, 0, 0.85);
				.dark & {
					color: rgba(255, 255, 255, 0.85);
				}
			}
		}
	}

	.combo-cards {
		display: flex;
		flex-direction: column;
		gap: 12px;

		.combo-card {
			padding: 16px;
			border: 1px solid #f0f0f0;
			border-radius: 4px;
			background-color: #fff;
			transition: all 0.2s;
			.dark & {
				border-color: #303030;
				background-color: #1f1f1f;
			}

			&:hover {
				box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
				.dark & {
					box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
				}
			}

			.combo-header {
				display: flex;
				justify-content: space-between;
				align-items: center;
				margin-bottom: 12px;
				padding-bottom: 12px;
				border-bottom: 1px solid #f0f0f0;
				.dark & {
					border-bottom-color: #303030;
				}

				.combo-title {
					display: flex;
					align-items: center;
					gap: 8px;
					flex-wrap: wrap;

					.mcp-name-tag {
						margin-left: 8px;
					}

					.system-name {
						font-weight: 500;
						color: rgba(0, 0, 0, 0.85);
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.separator {
						color: rgba(0, 0, 0, 0.45);
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}

					.env-name {
						color: rgba(0, 0, 0, 0.65);
						.dark & {
							color: rgba(255, 255, 255, 0.65);
						}
					}

					.api-count {
						margin-left: 8px;
					}
				}
			}

			.combo-apis {
				display: flex;
				flex-direction: column;
				gap: 8px;

				.api-item {
					display: flex;
					align-items: center;
					gap: 8px;
					padding: 8px;
					background-color: #fafafa;
					border-radius: 4px;
					.dark & {
						background-color: #262626;
					}

					.api-path {
						font-family: monospace;
						font-size: 12px;
						color: rgba(0, 0, 0, 0.65);
						.dark & {
							color: rgba(255, 255, 255, 0.65);
						}
					}

					.api-name {
						font-size: 12px;
						color: rgba(0, 0, 0, 0.85);
						flex: 1;
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.mcp-name-tag {
						margin-left: auto;
					}
				}
			}
		}
	}
}

.config-tooltip {
	margin: 0;
	padding: 8px;
	background-color: #1f1f1f;
	color: #d4d4d4;
	border-radius: 4px;
	font-size: 12px;
	line-height: 1.5;
	white-space: pre-wrap;
	word-break: break-all;
	max-width: 600px;
	overflow-x: auto;
}
</style>
