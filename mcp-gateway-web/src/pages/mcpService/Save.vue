<!--
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/Save.vue
 * @Author: teddy
 * @Date: 2026-01-24
 * @Description: MCP服务编辑页面
-->
<script setup lang="ts">
import { message } from 'ant-design-vue'
import { nextTick } from 'vue'
import { _tmeMsg, _uuid } from '@/assets/js/util'
import {
	fetchMcpServiceDetail,
	fetchMcpServiceSaveOrUpdate,
} from '~/fetch/http'
import type { ResourceCombo } from './utils/storage'
import { validateMcpName } from './utils/mcpNameValidator'
import ResourceComboSelector from './components/ResourceComboSelector.vue'
import SelectedComboList from './components/SelectedComboList.vue'
import AccessTokenDisplay from './components/AccessTokenDisplay.vue'
import ServiceStatusTag from './components/ServiceStatusTag.vue'

interface Props {
	visible: boolean
	serviceId?: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
	'update:visible': [value: boolean]
	close: []
}>()

// 表单数据
const formData = reactive({
	name: '',
	desc: '',
	status: 'draft' as 'draft' | 'published' | 'stopped',
	accessToken: '',
	endpoint: '',
})

// 已选组合列表
const selectedCombos = ref<ResourceCombo[]>([])

// 表单引用
const formRef = ref()

// 是否加载中
const loading = ref(false)

// 计算属性：服务端点
const serviceEndpoint = computed(() => {
	if (props.serviceId) {
		return `/mcp/service/${props.serviceId}`
	}
	return ''
})

// 加载服务数据
const loadServiceData = async () => {
	if (!props.serviceId) {
		// 新增模式，重置表单
		formData.name = ''
		formData.desc = ''
		formData.status = 'draft'
		formData.accessToken = _uuid()
		formData.endpoint = ''
		selectedCombos.value = []
		return
	}

	// 编辑模式，加载数据
	try {
		const response = await fetchMcpServiceDetail(props.serviceId)
		const detail = response.data
		if (!detail) {
			message.error('服务不存在')
			handleClose()
			return
		}

		formData.name = detail.name
		formData.desc = detail.desc || ''
		formData.status = detail.status
		formData.accessToken = detail.accessToken
		formData.endpoint = detail.endpoint || serviceEndpoint.value

		// 转换配置为组合列表
		selectedCombos.value = []
		if (detail.configApis && detail.configApis.length > 0) {
			for (const configApi of detail.configApis) {
				// 从 apis 数组中提取接口ID
				const apis = (configApi.apis || []).map((api: any) => ({
					id: api.id,
					name: api.name,
					method: api.method,
					path: api.path,
					customMcpName: api.customMcpName || undefined,
				}))
				const apiIds = apis.map((api: any) => api.id)

				selectedCombos.value.push({
					id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
					systemId: configApi.systemId,
					systemName: configApi.systemName || '',
					envId: configApi.envId || '',
					envName: configApi.envName || '未选择环境',
					envBaseUrl: configApi.envBaseUrl || '',
					apiIds,
					customMcpName: configApi.customMcpName || undefined,
					apis,
				})
			}
		}
	} catch (error) {
		console.error('加载服务数据失败:', error)
		message.error('加载服务数据失败')
		handleClose()
	}
}

// 添加组合
const handleAddCombo = (combo: ResourceCombo) => {
	// 检查是否已存在相同的系统+环境组合
	const existingIndex = selectedCombos.value.findIndex(
		c => c.systemId === combo.systemId && c.envId === combo.envId,
	)

	if (existingIndex !== -1) {
		// 如果存在，合并接口列表（去重）
		const existing = selectedCombos.value[existingIndex]
		const existingApiIds = new Set(existing.apiIds)
		const existingApiMap = new Map(existing.apis.map(api => [api.id, api]))

		// 添加新接口（去重）
		combo.apiIds.forEach(apiId => {
			if (!existingApiIds.has(apiId)) {
				existingApiIds.add(apiId)
				const newApi = combo.apis.find(a => a.id === apiId)
				if (newApi) {
					existingApiMap.set(apiId, newApi)
				}
			}
		})

		// 更新现有组合
		existing.apiIds = Array.from(existingApiIds)
		existing.apis = Array.from(existingApiMap.values())

		message.success(`组合已合并，当前包含 ${existing.apis.length} 个接口`)
	} else {
		// 如果不存在，直接添加
		selectedCombos.value.push(combo)
		message.success('组合已添加')
	}
}

// 删除组合
const handleRemoveCombo = (comboId: string) => {
	const index = selectedCombos.value.findIndex(c => c.id === comboId)
	if (index !== -1) {
		selectedCombos.value.splice(index, 1)
		message.success('组合已删除')
	}
}

// 更新组合列表
const handleUpdateCombos = (combos: ResourceCombo[]) => {
	selectedCombos.value = combos
}

// 重新生成访问令牌
const handleRegenerateToken = () => {
	formData.accessToken = _uuid()
	message.success('访问令牌已重新生成')
}

// 滚动到第一个校验失败的字段
const scrollToFirstError = (errorInfo: any) => {
	if (!errorInfo) return

	// Ant Design Vue Form.validate() 失败时，errorInfo 可能是：
	// 1. { errorFields: [...], values: {...} } 格式
	// 2. 直接是 errorFields 数组
	const errorFields = errorInfo.errorFields || errorInfo

	if (!errorFields || !Array.isArray(errorFields) || errorFields.length === 0) {
		return
	}

	// 获取第一个错误的字段名
	const firstErrorField = errorFields[0]
	const fieldName = firstErrorField.name?.[0]

	if (fieldName && formRef.value) {
		// 使用 nextTick 确保 DOM 已更新
		nextTick(() => {
			// 使用 Ant Design Vue Form 的 scrollToField 方法
			formRef.value?.scrollToField(fieldName, {
				behavior: 'smooth',
				block: 'center',
			})
		})
	}
}

// 保存服务
const handleSave = async (publish = false) => {
	try {
		await formRef.value.validate()

		// 验证组合
		if (selectedCombos.value.length === 0) {
			message.error('请至少添加一个资源组合')
			// 滚动到资源组合选择区域
			nextTick(() => {
				const comboSection = document.querySelector(
					'.form-section:nth-of-type(2)',
				)
				if (comboSection) {
					comboSection.scrollIntoView({ behavior: 'smooth', block: 'center' })
				}
			})
			return
		}

		// 验证每个组合
		for (let i = 0; i < selectedCombos.value.length; i++) {
			const combo = selectedCombos.value[i]
			// 验证环境（必选）
			if (!combo.envId || !combo.envName || combo.envName === '未选择环境') {
				message.error(`组合"${combo.systemName}"必须选择环境`)
				// 滚动到已选组合列表区域
				nextTick(() => {
					const comboListSection = document.querySelector(
						'.form-section:nth-of-type(3)',
					)
					if (comboListSection) {
						comboListSection.scrollIntoView({
							behavior: 'smooth',
							block: 'center',
						})
					}
				})
				return
			}

			// 验证接口
			if (combo.apiIds.length === 0) {
				message.error(
					`组合"${combo.systemName}/${combo.envName}"至少需要选择一个接口`,
				)
				// 滚动到已选组合列表区域
				nextTick(() => {
					const comboListSection = document.querySelector(
						'.form-section:nth-of-type(3)',
					)
					if (comboListSection) {
						comboListSection.scrollIntoView({
							behavior: 'smooth',
							block: 'center',
						})
					}
				})
				return
			}

			// 验证系统级别的自定义 MCP 名字
			if (combo.customMcpName) {
				const systemValidation = validateMcpName(combo.customMcpName)
				if (!systemValidation.valid) {
					message.error(
						`组合"${combo.systemName}/${combo.envName}"的系统 MCP 名字：${systemValidation.message}`,
					)
					// 滚动到已选组合列表区域
					nextTick(() => {
						const comboListSection = document.querySelector(
							'.form-section:nth-of-type(3)',
						)
						if (comboListSection) {
							comboListSection.scrollIntoView({
								behavior: 'smooth',
								block: 'center',
							})
						}
					})
					return
				}
			}

			// 验证接口级别的自定义 MCP 名字
			for (const api of combo.apis) {
				if (api.customMcpName) {
					const apiValidation = validateMcpName(api.customMcpName)
					if (!apiValidation.valid) {
						message.error(
							`接口"${api.name}"的 MCP 名字：${apiValidation.message}`,
						)
						// 滚动到已选组合列表区域
						nextTick(() => {
							const comboListSection = document.querySelector(
								'.form-section:nth-of-type(3)',
							)
							if (comboListSection) {
								comboListSection.scrollIntoView({
									behavior: 'smooth',
									block: 'center',
								})
							}
						})
						return
					}
				}
			}
		}

		loading.value = true

		// 构建保存数据
		const saveData = {
			id: props.serviceId || undefined,
			name: formData.name,
			desc: formData.desc,
			status: publish ? 'published' : formData.status,
			accessToken: formData.accessToken,
			endpoint: props.serviceId ? serviceEndpoint.value : undefined,
			config: {
				selections: selectedCombos.value.map(combo => {
					// 构建接口 ID 到自定义 MCP 名字的映射
					const apiCustomMcpNames: Record<string, string> = {}
					combo.apis.forEach(api => {
						if (api.customMcpName) {
							apiCustomMcpNames[api.id] = api.customMcpName
						}
					})

					return {
						systemId: combo.systemId,
						systemName: combo.systemName,
						envId: combo.envId,
						envName: combo.envName,
						envBaseUrl: combo.envBaseUrl,
						apiIds: combo.apiIds,
						customMcpName: combo.customMcpName,
						apiCustomMcpNames:
							Object.keys(apiCustomMcpNames).length > 0
								? apiCustomMcpNames
								: undefined,
					}
				}),
			},
		}

		await fetchMcpServiceSaveOrUpdate(saveData)

		_tmeMsg.success(publish ? '保存并发布成功' : '保存成功')
		handleClose()
	} catch (error: any) {
		console.error('保存失败:', error)

		// 如果是表单校验错误，滚动到第一个错误字段
		if (error?.errorFields || (Array.isArray(error) && error.length > 0)) {
			scrollToFirstError(error)
			// 不显示额外的错误消息，因为表单已经显示了字段级别的错误提示
			return
		}

		const errorMsg =
			error?.response?.data?.message || error?.message || '保存失败，请稍后重试'
		message.error(errorMsg)
	} finally {
		loading.value = false
	}
}

// 关闭弹窗
const handleClose = () => {
	emit('update:visible', false)
	emit('close')
}

// 重置表单状态
const resetFormState = () => {
	formData.name = ''
	formData.desc = ''
	formData.status = 'draft'
	formData.accessToken = ''
	formData.endpoint = ''
	selectedCombos.value = []
	formRef.value?.resetFields()
}

// 监听visible变化
watch(
	() => props.visible,
	val => {
		if (val) {
			// 先清空之前的状态
			resetFormState()
			// 然后加载数据
			loadServiceData()
		} else {
			// 关闭时也清空状态
			resetFormState()
		}
	},
	{ immediate: true },
)
</script>

<template>
	<a-modal
		:open="visible"
		:title="serviceId ? '编辑MCP服务' : '新增MCP服务'"
		width="1200px"
		:confirm-loading="loading"
		class="mcp-service-save-modal"
		@cancel="handleClose"
	>
		<a-form
			ref="formRef"
			:model="formData"
			:label-col="{ span: 4 }"
			:wrapper-col="{ span: 20 }"
			autocomplete="off"
			class="mcp-service-form"
		>
			<!-- 基本信息模块 -->
			<div class="form-section">
				<div class="section-header">
					<h4 class="section-title">基本信息</h4>
				</div>
				<div class="section-content">
					<a-form-item
						label="服务名称"
						name="name"
						:rules="[{ required: true, message: '请输入服务名称！' }]"
					>
						<a-input
							v-model:value="formData.name"
							placeholder="请输入服务名称"
							:maxlength="300"
						/>
					</a-form-item>

					<a-form-item label="服务描述" name="desc">
						<a-textarea
							v-model:value="formData.desc"
							placeholder="请输入服务描述"
							:auto-size="{ minRows: 3, maxRows: 5 }"
							:maxlength="500"
						/>
					</a-form-item>
				</div>
			</div>

			<!-- 资源组合选择模块 -->
			<div class="form-section">
				<div class="section-header">
					<h4 class="section-title">资源组合选择</h4>
				</div>
				<div class="section-content">
					<ResourceComboSelector @add="handleAddCombo" />
				</div>
			</div>

			<!-- 已选组合列表模块 -->
			<div class="form-section">
				<div class="section-header">
					<h4 class="section-title">已选组合列表</h4>
				</div>
				<div class="section-content">
					<SelectedComboList
						:combos="selectedCombos"
						@remove="handleRemoveCombo"
						@update="handleUpdateCombos"
					/>
				</div>
			</div>

			<!-- 服务配置模块 -->
			<div class="form-section">
				<div class="section-header">
					<h4 class="section-title">服务配置</h4>
				</div>
				<div class="section-content">
					<a-form-item label="访问令牌">
						<AccessTokenDisplay
							:token="formData.accessToken"
							:service-id="serviceId || undefined"
							@regenerate="handleRegenerateToken"
						/>
					</a-form-item>

					<a-form-item label="服务端点">
						<a-input
							:value="serviceId ? serviceEndpoint : '保存后自动生成'"
							readonly
						/>
					</a-form-item>

					<a-form-item v-if="serviceId" label="服务状态">
						<ServiceStatusTag :status="formData.status" />
					</a-form-item>
				</div>
			</div>
		</a-form>

		<template #footer>
			<a-button key="back" @click="handleClose">取消</a-button>
			<a-button
				key="save"
				type="primary"
				:loading="loading"
				@click="handleSave(false)"
			>
				保存
			</a-button>
			<a-button
				v-if="!serviceId || formData.status !== 'published'"
				key="publish"
				type="primary"
				:loading="loading"
				@click="handleSave(true)"
			>
				保存并发布
			</a-button>
		</template>
	</a-modal>
</template>

<style lang="scss" scoped>
.mcp-service-form {
	.form-section {
		margin-bottom: 24px;
		background-color: #fff;
		border: 1px solid #f0f0f0;
		border-radius: 4px;
		overflow: hidden;
		transition: all 0.2s;

		.dark & {
			background-color: #1f1f1f;
			border-color: #303030;
		}

		&:last-child {
			margin-bottom: 0;
		}

		&:hover {
			box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
			.dark & {
				box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
			}
		}

		.section-header {
			padding: 12px 16px;
			background-color: #fafafa;
			border-bottom: 1px solid #f0f0f0;
			.dark & {
				background-color: #262626;
				border-bottom-color: #303030;
			}

			.section-title {
				margin: 0;
				font-size: 14px;
				font-weight: 500;
				color: rgba(0, 0, 0, 0.85);
				.dark & {
					color: rgba(255, 255, 255, 0.85);
				}
			}
		}

		.section-content {
			padding: 16px;
		}
	}
}
</style>

<style lang="scss">
.mcp-service-save-modal {
	max-height: 80vh;
	.ant-modal-wrap {
		overflow: hidden;
	}

	.ant-modal {
		max-height: 90vh;
		top: 5vh;
		padding-bottom: 0;
		display: flex;
		flex-direction: column;
	}

	.ant-modal-content {
		max-height: calc(90vh - 100px);
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.ant-modal-header {
		flex-shrink: 0;
	}

	.ant-modal-body {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		padding: 24px;
		min-height: 0;
	}

	.ant-modal-footer {
		flex-shrink: 0;
	}
}
</style>
