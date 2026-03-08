<!--
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/components/ResourceComboSelector.vue
 * @Author: teddy
 * @Date: 2026-01-24
 * @Description: 资源组合选择器组件
-->
<script setup lang="ts">
import { message } from 'ant-design-vue'
import { fetchSystemList } from '~/fetch/http'
import { getApiList } from '~/utils/apiSaveUtils'
import EnvSelect from '~/pages/apiManage/EnvSelect.vue'
import type { ResourceCombo } from '../utils/storage'

// 当前选择状态
interface CurrentSelection {
	systemId: string | null
	systemName: string | null
	envId: string | null | undefined
	envName: string | null
	envBaseUrl: string | null
	selectedApiIds: string[]
}

const emit = defineEmits<{
	add: (combo: ResourceCombo) => void
}>()

// 系统列表
const systemList = ref<any[]>([])
const systemListLoading = ref(false)

// 接口列表
const apiList = ref<any[]>([])
const apiListLoading = ref(false)

// 接口搜索关键词
const apiSearchKeyword = ref('')

// 当前选择
const currentSelection = reactive<CurrentSelection>({
	systemId: null,
	systemName: null,
	envId: null,
	envName: null,
	envBaseUrl: null,
	selectedApiIds: [],
})

// 过滤后的接口列表
const filteredApiList = computed(() => {
	if (!apiSearchKeyword.value) {
		return apiList.value
	}
	const keyword = apiSearchKeyword.value.toLowerCase()
	return apiList.value.filter(
		api =>
			api.apiForm?.name?.toLowerCase().includes(keyword) ||
			api.apiForm?.path?.toLowerCase().includes(keyword) ||
			api.apiForm?.method?.toLowerCase().includes(keyword),
	)
})

// 是否可以添加组合
const canAddCombo = computed(() => {
	return (
		currentSelection.systemId &&
		currentSelection.envId &&
		currentSelection.selectedApiIds.length > 0
	)
})

// 加载系统列表
const loadSystemList = async () => {
	try {
		systemListLoading.value = true
		const { data } = await fetchSystemList({
			page: {
				current: 1,
				size: 1000,
			},
			entityParam: {},
		})
		systemList.value = data.records || []
	} catch (error) {
		console.error('获取系统列表失败:', error)
	} finally {
		systemListLoading.value = false
	}
}

// 系统选择变化
const handleSystemChange = async (systemId: string) => {
	const system = systemList.value.find(s => s.id === systemId)

	if (!system) {
		currentSelection.systemId = null
		currentSelection.systemName = null
		currentSelection.envId = null
		currentSelection.envName = null
		currentSelection.envBaseUrl = null
		apiList.value = []
		return
	}

	currentSelection.systemId = systemId
	currentSelection.systemName = system.name
	currentSelection.envId = null
	currentSelection.envName = null
	currentSelection.envBaseUrl = null
	currentSelection.selectedApiIds = []

	// 加载接口列表
	await loadApiList(systemId, system.name)
}

// 环境选择变化
const handleEnvChange = (env: any | null) => {
	if (env) {
		currentSelection.envId = env.id
		currentSelection.envName = env.name
		currentSelection.envBaseUrl = env.baseUrl
	} else {
		currentSelection.envId = null
		currentSelection.envName = null
		currentSelection.envBaseUrl = null
	}
}

// 加载接口列表
const loadApiList = async (systemId: string, systemName: string) => {
	try {
		apiListLoading.value = true
		const list = await getApiList(systemId, systemName, null)
		apiList.value = list || []
	} catch (error) {
		console.error('获取接口列表失败:', error)
		apiList.value = []
	} finally {
		apiListLoading.value = false
	}
}

// 添加到组合
const handleAddCombo = () => {
	// 校验系统
	if (!currentSelection.systemId) {
		message.error('请先选择系统')
		return
	}

	// 校验环境（必选）
	if (!currentSelection.envId) {
		message.error('请先选择环境')
		return
	}

	// 校验接口
	if (currentSelection.selectedApiIds.length === 0) {
		message.error('请至少选择一个接口')
		return
	}

	// 获取选中的接口信息
	const selectedApis = apiList.value.filter(api =>
		currentSelection.selectedApiIds.includes(api.id),
	)

	// 构建组合对象
	const combo: ResourceCombo = {
		id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
		systemId: currentSelection.systemId,
		systemName: currentSelection.systemName!,
		envId: currentSelection.envId,
		envName: currentSelection.envName!,
		envBaseUrl: currentSelection.envBaseUrl || '',
		apiIds: currentSelection.selectedApiIds,
		apis: selectedApis.map(api => ({
			id: api.id,
			name: api.apiForm.name,
			method: api.apiForm.method,
			path: api.apiForm.path,
		})),
	}

	emit('add', combo)

	// 重置选择（保留系统选择）
	// 注意：envId 设置为 undefined 以触发 EnvSelect 的自动选择逻辑
	currentSelection.envId = undefined
	currentSelection.envName = null
	currentSelection.envBaseUrl = null
	currentSelection.selectedApiIds = []
	apiSearchKeyword.value = ''
}

// 获取方法颜色
const getMethodColor = (method: string) => {
	const colors: Record<string, string> = {
		GET: 'blue',
		POST: 'green',
		PUT: 'orange',
		DELETE: 'red',
		PATCH: 'purple',
	}
	return colors[method] || 'default'
}

onMounted(() => {
	loadSystemList()
})
</script>

<template>
	<div class="resource-combo-selector">
		<!-- 步骤1: 选择系统 -->
		<div class="step-section">
			<h4>步骤1: 选择系统</h4>
			<a-spin :spinning="systemListLoading">
				<a-select
					v-model:value="currentSelection.systemId"
					placeholder="请选择系统"
					style="width: 100%"
					show-search
					:filter-option="
						(input, option) => {
							const label =
								option?.children?.[0]?.children || option?.label || ''
							return label.toLowerCase().includes(input.toLowerCase())
						}
					"
					@change="handleSystemChange"
				>
					<a-select-option
						v-for="system in systemList"
						:key="system.id"
						:value="system.id"
					>
						{{ system.name }}
					</a-select-option>
				</a-select>
				<div
					v-if="!systemListLoading && systemList.length === 0"
					class="empty-tip"
				>
					暂无系统，请先添加业务系统
				</div>
			</a-spin>
		</div>

		<!-- 步骤2: 选择环境 -->
		<div v-if="currentSelection.systemId" class="step-section">
			<h4>
				步骤2: 选择环境
				<span class="required-mark">*</span>
			</h4>
			<div class="env-select-wrapper">
				<EnvSelect
					v-model:value="currentSelection.envId"
					:system-id="currentSelection.systemId"
					:system-name="currentSelection.systemName || ''"
					required
					@change="handleEnvChange"
				/>
			</div>
		</div>

		<!-- 步骤3: 选择接口 -->
		<div v-if="currentSelection.systemId" class="step-section">
			<h4>步骤3: 选择接口</h4>
			<a-input
				v-model:value="apiSearchKeyword"
				placeholder="搜索接口..."
				class="api-search"
				allow-clear
			>
				<template #prefix>
					<i class="ri-search-line"></i>
				</template>
			</a-input>
			<a-spin :spinning="apiListLoading">
				<div v-if="filteredApiList.length === 0" class="empty-tip">
					{{ apiSearchKeyword ? '未找到匹配的接口' : '该系统暂无接口' }}
				</div>
				<a-checkbox-group
					v-else
					v-model:value="currentSelection.selectedApiIds"
					class="api-list"
				>
					<div v-for="api in filteredApiList" :key="api.id" class="api-item">
						<a-checkbox :value="api.id">
							<a-tag
								:color="getMethodColor(api.apiForm.method)"
								class="api-method"
							>
								{{ api.apiForm.method }}
							</a-tag>
							<span class="api-path">{{ api.apiForm.path }}</span>
							<span class="api-name">{{ api.apiForm.name }}</span>
						</a-checkbox>
					</div>
				</a-checkbox-group>
			</a-spin>
		</div>

		<!-- 添加到组合按钮 -->
		<div v-if="canAddCombo" class="step-actions">
			<a-button type="primary" @click="handleAddCombo">
				<i class="ri-add-line mr-1"></i>
				添加到组合
			</a-button>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.resource-combo-selector {
	.step-section {
		margin-bottom: 24px;
		padding: 16px;
		background-color: #fafafa;
		border-radius: 4px;
		.dark & {
			background-color: #1f1f1f;
		}

		h4 {
			margin: 0 0 12px 0;
			font-size: 14px;
			font-weight: 500;
			color: rgba(0, 0, 0, 0.85);
			.dark & {
				color: rgba(255, 255, 255, 0.85);
			}

			.required-mark {
				color: #ff4d4f;
				margin-left: 4px;
			}
		}

		.env-select-wrapper {
			width: 100%;

			:deep(.env-select-wrapper) {
				width: 100%;
				display: block;

				.ant-select {
					width: 100% !important;
					margin-right: 0 !important;
				}
			}
		}

		.env-url {
			margin-left: 8px;
			font-size: 12px;
			color: rgba(0, 0, 0, 0.45);
			.dark & {
				color: rgba(255, 255, 255, 0.45);
			}
		}

		.api-search {
			margin-bottom: 12px;
		}

		.api-list {
			display: flex;
			flex-direction: column;
			gap: 8px;
			max-height: 400px;
			overflow-y: auto;

			.api-item {
				padding: 8px;
				border: 1px solid #f0f0f0;
				border-radius: 4px;
				transition: all 0.2s;
				.dark & {
					border-color: #303030;
				}

				&:hover {
					background-color: #f5f5f5;
					.dark & {
						background-color: #262626;
					}
				}

				:deep(.ant-checkbox-wrapper) {
					width: 100%;
					display: flex;
					align-items: center;
					gap: 8px;
				}

				.api-method {
					flex-shrink: 0;
				}

				.api-path {
					font-family: monospace;
					color: rgba(0, 0, 0, 0.65);
					.dark & {
						color: rgba(255, 255, 255, 0.65);
					}
				}

				.api-name {
					margin-left: 8px;
					color: rgba(0, 0, 0, 0.85);
					.dark & {
						color: rgba(255, 255, 255, 0.85);
					}
				}
			}
		}

		.empty-tip {
			padding: 20px;
			text-align: center;
			color: rgba(0, 0, 0, 0.45);
			.dark & {
				color: rgba(255, 255, 255, 0.45);
			}
		}
	}

	.step-actions {
		margin-top: 16px;
		text-align: center;
	}
}
</style>
