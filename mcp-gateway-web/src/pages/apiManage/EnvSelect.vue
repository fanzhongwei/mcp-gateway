<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/EnvSelect.vue
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: 业务系统环境选择下拉组件
-->
<script setup lang="ts">
import { defineComponent } from 'vue'
import { fetchSystemEnvList } from '~/fetch/http'
import SystemEnvManage from '../businessSystem/SystemEnvManage.vue'

const props = defineProps<{
	systemId?: string
	systemName?: string
	// 当前选中的环境 ID（可选）
	value?: string
	// 是否必选（必选时不允许清除）
	required?: boolean
}>()

const emit = defineEmits<{
	(e: 'update:value', value?: string): void
	(e: 'change', env: any | null): void
}>()

// 渲染 a-select 自定义下拉内容用的小组件
const VNodes = defineComponent({
	props: {
		vnodes: {
			type: Object,
			required: true,
		},
	},
	setup(innerProps) {
		return () => innerProps.vnodes
	},
})

// 环境列表
const envList = ref<any[]>([])
const envListLoading = ref(false)

// 环境下拉展开状态
const envDropdownOpen = ref(false)

// 选中的环境 ID（内部维护，和 props.value 同步）
const innerValue = ref<string | undefined>(props.value)

// 环境管理组件引用
const envManageRef = ref()

// 获取环境列表
const getEnvList = async () => {
	if (!props.systemId) {
		envList.value = []
		innerValue.value = undefined
		emit('update:value', undefined)
		emit('change', null)
		return
	}
	try {
		envListLoading.value = true
		const { data } = await fetchSystemEnvList({
			page: {
				current: 1,
				size: 1000,
			},
			entityParam: {
				systemId: props.systemId,
			},
		})
		envList.value = data.records || []

		// 根据当前 value 或默认选中第一个环境
		if (envList.value.length === 0) {
			innerValue.value = undefined
			emit('update:value', undefined)
			emit('change', null)
			return
		}

		const current =
			envList.value.find(
				(e: any) => e.id === (innerValue.value || props.value),
			) || envList.value[0]

		innerValue.value = current.id
		emit('update:value', current.id)
		emit('change', current)
	} catch (error) {
		console.error('获取环境列表失败', error)
		envList.value = []
		innerValue.value = undefined
		emit('update:value', undefined)
		emit('change', null)
	} finally {
		envListLoading.value = false
	}
}

// 获取环境颜色（根据环境名称生成颜色）
const getEnvColor = (name: string) => {
	const colors = [
		'#722ed1', // 紫色 - 正式环境
		'#eb2f96', // 粉色 - 本地测试环境
		'#52c41a', // 绿色 - 远端测试环境
		'#fa8c16', // 橙色 - 体验环境
		'#13c2c2', // 青色 - 本地 Mock
		'#faad14', // 金色 - 云端 Mock
	]
	// 根据名称关键字匹配颜色
	if (name.includes('正式') || name.includes('生产')) {
		return colors[0]
	} else if (name.includes('本地') && name.includes('测试')) {
		return colors[1]
	} else if (name.includes('远端') || name.includes('远程')) {
		return colors[2]
	} else if (name.includes('体验')) {
		return colors[3]
	} else if (name.includes('本地') && name.includes('Mock')) {
		return colors[4]
	} else if (
		name.includes('云端') ||
		(name.includes('云') && name.includes('Mock'))
	) {
		return colors[5]
	}
	// 默认颜色（根据名称hash）
	const hash = name.split('').reduce((acc, char) => {
		return char.charCodeAt(0) + ((acc << 5) - acc)
	}, 0)
	return colors[Math.abs(hash) % colors.length]
}

// 打开环境管理
const handleOpenEnvManage = () => {
	// 先关闭下拉框
	envDropdownOpen.value = false
	if (envManageRef.value) {
		envManageRef.value.open()
	}
}

// 环境更新后刷新列表
const handleEnvUpdate = () => {
	getEnvList()
}

// 下拉展开/收起
const handleDropdownVisibleChange = (open: boolean) => {
	envDropdownOpen.value = open
}

// 选择环境
const handleSelectEnv = (value: string | undefined) => {
	if (value === undefined) {
		// 如果必选，不允许清除
		if (props.required) {
			// 恢复之前的值
			const prevEnv = envList.value.find((e: any) => e.id === innerValue.value)
			if (prevEnv) {
				innerValue.value = prevEnv.id
				emit('update:value', prevEnv.id)
				emit('change', prevEnv)
			}
			return
		}
		// 非必选时允许清除
		innerValue.value = undefined
		emit('update:value', undefined)
		emit('change', null)
		return
	}
	const env = envList.value.find((e: any) => e.id === value) || null
	innerValue.value = value
	emit('update:value', value)
	emit('change', env)
}

// 监听外部 value 变化，同步内部值
watch(
	() => props.value,
	(val, oldVal) => {
		// 如果必选且 value 变为 undefined/null，且有环境列表，自动选择第一个环境
		if (
			props.required &&
			(val === undefined || val === null) &&
			envList.value.length > 0 &&
			props.systemId
		) {
			// 只有当从有值变为无值时才自动选择（避免初始化时重复选择）
			if (oldVal !== undefined && oldVal !== null) {
				const firstEnv = envList.value[0]
				if (firstEnv) {
					innerValue.value = firstEnv.id
					emit('update:value', firstEnv.id)
					emit('change', firstEnv)
					return
				}
			}
		}
		innerValue.value = val
	},
)

// 监听 systemId 变化，刷新环境列表
watch(
	() => props.systemId,
	() => {
		getEnvList()
	},
	{ immediate: true },
)
</script>

<template>
	<div class="env-select-wrapper">
		<a-select
			v-if="systemId"
			v-model:value="innerValue"
			placeholder="请选择环境"
			style="width: 200px; margin-right: 12px"
			:loading="envListLoading"
			show-search
			:filter-option="false"
			:allow-clear="!required"
			:open="envDropdownOpen"
			@dropdown-visible-change="handleDropdownVisibleChange"
			@change="handleSelectEnv"
		>
			<template #dropdownRender="{ menuNode }">
				<div class="env-dropdown">
					<VNodes :vnodes="menuNode" />
					<div class="env-dropdown-footer">
						<a-divider style="margin: 4px 0" />
						<a-button
							block
							type="link"
							class="env-manage-button"
							@click.stop="handleOpenEnvManage"
						>
							<i class="ri-settings-3-line" style="margin-right: 4px"></i>
							<span>管理环境</span>
						</a-button>
					</div>
				</div>
			</template>
			<template #suffixIcon>
				<i class="ri-search-line"></i>
			</template>
			<a-select-option v-for="env in envList" :key="env.id" :value="env.id">
				<div class="env-option">
					<span
						class="env-icon"
						:style="{ backgroundColor: getEnvColor(env.name) }"
					></span>
					<span class="env-text">
						{{ env.name }}（{{ env.baseUrl || '-' }}）
					</span>
				</div>
			</a-select-option>
		</a-select>

		<!-- 环境管理组件 -->
		<SystemEnvManage
			v-if="systemId"
			ref="envManageRef"
			:system-id="systemId"
			:system-name="systemName || ''"
			@update="handleEnvUpdate"
		/>
	</div>
</template>

<style scoped lang="scss">
.env-select-wrapper {
	display: inline-flex;
	align-items: center;
}

.env-select-wrapper :deep(.ant-select-selector) {
	display: flex;
	align-items: center;
	overflow: hidden;
	min-width: 0;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.env-select-wrapper :deep(.ant-select-selection-item) {
	display: flex;
	align-items: center;
	flex: 1 1 auto;
	min-width: 0;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.env-select-wrapper :deep(.ant-select-selection-item > span) {
	flex: 1 1 auto;
	min-width: 0;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.env-select-wrapper :deep(.ant-select-selection-search) {
	flex-shrink: 0;
}

.env-dropdown-footer {
	padding: 0 8px 8px;
}

.env-manage-button {
	justify-content: flex-start;
	padding-left: 4px;
	padding-right: 4px;
	color: rgba(0, 0, 0, 0.65);
	.dark & {
		color: rgba(255, 255, 255, 0.65);
	}
}

.env-option {
	display: flex;
	align-items: center;
	justify-content: flex-start;
	gap: 8px;
	flex: 1 1 auto;
	min-width: 0;

	.env-icon {
		width: 12px;
		height: 12px;
		border-radius: 2px;
		flex-shrink: 0;
	}

	.env-text {
		display: inline-block;
		flex: 1 1 auto;
		min-width: 0;
		font-size: 13px;
		line-height: 1.4;
		text-align: left;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
}
</style>
