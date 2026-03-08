<!--
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/components/SelectedComboList.vue
 * @Author: teddy
 * @Date: 2026-01-24
 * @Description: 已选组合列表组件
-->
<script setup lang="ts">
import { message } from 'ant-design-vue'
import type { ResourceCombo } from '../utils/storage'
import { validateMcpName, formatMcpName } from '../utils/mcpNameValidator'

interface Props {
	combos: ResourceCombo[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
	remove: (comboId: string) => void
	update: (combos: ResourceCombo[]) => void
}>()

// 统计信息
const totalApiCount = computed(() => {
	return props.combos.reduce((sum, combo) => sum + combo.apis.length, 0)
})

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

// 删除组合
const handleRemove = (comboId: string) => {
	emit('remove', comboId)
}

// 删除单个接口
const handleRemoveApi = (comboId: string, apiId: string) => {
	const combo = props.combos.find(c => c.id === comboId)
	if (!combo) return

	// 从 apiIds 数组中移除
	const apiIndex = combo.apiIds.indexOf(apiId)
	if (apiIndex !== -1) {
		combo.apiIds.splice(apiIndex, 1)
	}

	// 从 apis 数组中移除
	const apiObjIndex = combo.apis.findIndex(a => a.id === apiId)
	if (apiObjIndex !== -1) {
		combo.apis.splice(apiObjIndex, 1)
	}

	// 清理该接口的缓存
	const cacheKey = `${comboId}-${apiId}`
	delete apiMcpNameCache.value[cacheKey]

	// 如果组合中没有接口了，提示用户是否删除整个组合
	if (combo.apis.length === 0) {
		message.warning('该组合已无接口，将自动删除')
		emit('remove', comboId)
	} else {
		message.success('接口已删除')
		emit('update', [...props.combos])
	}
}

// 存储每个输入框的原始值，用于验证失败时恢复
const systemMcpNameCache = ref<Record<string, string>>({})
const apiMcpNameCache = ref<Record<string, string>>({})

// 更新系统级别的自定义 MCP 名字
const handleSystemMcpNameChange = (comboId: string, value: string) => {
	const combo = props.combos.find(c => c.id === comboId)
	if (!combo) return

	const formattedValue = formatMcpName(value)
	const validation = validateMcpName(formattedValue)

	if (!validation.valid) {
		message.error(validation.message || 'MCP 名字格式不正确')
		// 恢复为缓存的值
		combo.customMcpName = systemMcpNameCache.value[comboId] || undefined
		return
	}

	// 验证通过，更新缓存和值
	systemMcpNameCache.value[comboId] = formattedValue || ''
	combo.customMcpName = formattedValue || undefined
	emit('update', [...props.combos])
}

// 更新接口级别的自定义 MCP 名字
const handleApiMcpNameChange = (
	comboId: string,
	apiId: string,
	value: string,
) => {
	const combo = props.combos.find(c => c.id === comboId)
	if (!combo) return

	const api = combo.apis.find(a => a.id === apiId)
	if (!api) return

	const formattedValue = formatMcpName(value)
	const validation = validateMcpName(formattedValue)

	if (!validation.valid) {
		message.error(validation.message || 'MCP 名字格式不正确')
		// 恢复为缓存的值
		const cacheKey = `${comboId}-${apiId}`
		api.customMcpName = apiMcpNameCache.value[cacheKey] || undefined
		return
	}

	// 验证通过，更新缓存和值
	const cacheKey = `${comboId}-${apiId}`
	apiMcpNameCache.value[cacheKey] = formattedValue || ''
	api.customMcpName = formattedValue || undefined
	emit('update', [...props.combos])
}

// 初始化缓存
watch(
	() => props.combos,
	combos => {
		combos.forEach(combo => {
			systemMcpNameCache.value[combo.id] = combo.customMcpName || ''
			combo.apis.forEach(api => {
				const cacheKey = `${combo.id}-${api.id}`
				apiMcpNameCache.value[cacheKey] = api.customMcpName || ''
			})
		})
	},
	{ immediate: true, deep: true },
)
</script>

<template>
	<div class="selected-combo-list">
		<div class="list-header">
			<h4>已选组合列表</h4>
			<div class="statistics">
				共 {{ combos.length }} 个组合，{{ totalApiCount }} 个接口
			</div>
		</div>

		<div v-if="combos.length === 0" class="empty-state">
			<a-empty description="暂无组合，请先添加组合" />
		</div>

		<div v-else class="combo-cards">
			<div v-for="combo in combos" :key="combo.id" class="combo-card">
				<div class="combo-header">
					<div class="combo-title">
						<span class="system-name">{{ combo.systemName }}</span>
						<span class="separator">/</span>
						<span class="env-name">{{ combo.envName }}</span>
						<a-tag color="blue" class="api-count">
							{{ combo.apis.length }} 个接口
						</a-tag>
					</div>
					<div class="combo-actions">
						<a-input
							v-model:value="combo.customMcpName"
							placeholder="如果不指定则由系统自动生成"
							class="mcp-name-input"
							:maxlength="128"
							@blur="
								handleSystemMcpNameChange(combo.id, combo.customMcpName || '')
							"
						>
							<template #prefix>
								<span class="input-hint">MCP系统名</span>
							</template>
						</a-input>
						<a-button
							type="text"
							danger
							size="small"
							@click="handleRemove(combo.id)"
						>
							<i class="ri-delete-bin-line mr-1"></i>
							删除
						</a-button>
					</div>
				</div>

				<div class="combo-apis">
					<div v-for="api in combo.apis" :key="api.id" class="api-item">
						<a-tag :color="getMethodColor(api.method)">
							{{ api.method }}
						</a-tag>
						<span class="api-path">{{ api.path }}</span>
						<span class="api-name">{{ api.name }}</span>
						<a-input
							v-model:value="api.customMcpName"
							placeholder="如果不指定则由系统自动生成"
							class="mcp-name-input api-mcp-input"
							:maxlength="128"
							@blur="
								handleApiMcpNameChange(
									combo.id,
									api.id,
									api.customMcpName || '',
								)
							"
						>
							<template #prefix>
								<span class="input-hint">MCP工具名</span>
							</template>
						</a-input>
						<a-button
							type="text"
							danger
							size="small"
							class="api-delete-btn"
							@click="handleRemoveApi(combo.id, api.id)"
						>
							<i class="ri-delete-bin-line"></i>
						</a-button>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.selected-combo-list {
	.list-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 16px;

		h4 {
			margin: 0;
			font-size: 14px;
			font-weight: 500;
			color: rgba(0, 0, 0, 0.85);
			.dark & {
				color: rgba(255, 255, 255, 0.85);
			}
		}

		.statistics {
			font-size: 12px;
			color: rgba(0, 0, 0, 0.45);
			.dark & {
				color: rgba(255, 255, 255, 0.45);
			}
		}
	}

	.empty-state {
		padding: 40px 0;
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
					flex: 1;

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

				.combo-actions {
					display: flex;
					align-items: center;
					gap: 12px;
					margin-left: 16px;

					.mcp-name-input {
						width: 300px;
					}

					.input-hint {
						font-size: 12px;
						color: rgba(0, 0, 0, 0.45);
						white-space: nowrap;
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
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

					.api-mcp-input {
						width: 300px;
						margin-left: auto;
					}

					.api-mcp-input :deep(.ant-input-prefix) {
						.input-hint {
							font-size: 12px;
							color: rgba(0, 0, 0, 0.45);
							white-space: nowrap;
							.dark & {
								color: rgba(255, 255, 255, 0.45);
							}
						}
					}

					.api-delete-btn {
						flex-shrink: 0;
						color: rgba(0, 0, 0, 0.45);
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}

						&:hover {
							color: #ff4d4f;
							.dark & {
								color: #ff7875;
							}
						}
					}
				}
			}
		}
	}
}
</style>
