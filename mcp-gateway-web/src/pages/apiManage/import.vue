<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/import.vue
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: 接口导入页面
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-23
-->
<script setup lang="tsx">
import { h } from 'vue'
import { getApi } from '~/utils/apiSaveUtils'
import EnvSelect from './EnvSelect.vue'
import ManualImport from './importTypes/manual.vue'
import OpenApiImport from './importTypes/openapi.vue'

// 接收父组件传递的业务系统信息
const props = defineProps<{
	systemId?: string
	systemName?: string
	editingApiId?: string | null
}>()

// 选中的环境（预留给后续导入逻辑使用）
const selectedEnv = ref<any | null>(null)

const handleEnvChange = (env: any | null) => {
	selectedEnv.value = env
}

// 数据源格式类型
type DataSourceType = 'manual' | 'openapi' | 'postman' | 'curl' | 'apifox'

// 数据源格式选项
const dataSourceOptions = [
	{
		type: 'manual' as DataSourceType,
		name: '手动录入',
		icon: 'ri-edit-box-line',
		color: '#1890ff',
		description: '手动填写接口信息',
	},
	{
		type: 'openapi' as DataSourceType,
		name: 'OpenAPI/Swagger',
		icon: 'ri-file-code-line',
		color: '#52c41a',
		description: '导入 OpenAPI 或 Swagger 规范文件',
	},
	{
		type: 'postman' as DataSourceType,
		name: 'Postman',
		icon: 'ri-file-list-3-line',
		color: '#ff6b35',
		description: '导入 Postman 集合文件',
	},
	{
		type: 'curl' as DataSourceType,
		name: 'cURL',
		icon: 'ri-terminal-box-line',
		color: '#1890ff',
		description: '导入 cURL 命令',
	},
	{
		type: 'apifox' as DataSourceType,
		name: 'Apifox',
		icon: 'ri-file-list-2-line',
		color: '#ff4d4f',
		description: '导入 Apifox 项目文件',
	},
]

// 导入相关状态
const importForm = reactive({
	loading: false,
	selectedType: null as DataSourceType | null,
})

// 选择数据源格式
const handleSelectDataSource = (type: DataSourceType) => {
	importForm.selectedType = type
}

// 返回数据源选择页面
const handleBackToSource = () => {
	// 如果是编辑模式，直接关闭导入页面
	if (props.editingApiId) {
		handleCancel()
		return
	}
	// 否则返回到数据源选择页面
	importForm.selectedType = null
}

// 获取编辑的接口数据
const editingApiData = ref<any | null>(null)
const editingApiDataLoading = ref(false)

// 加载编辑的接口数据
const loadEditingApiData = async () => {
	if (!props.editingApiId) {
		editingApiData.value = null
		return
	}

	try {
		editingApiDataLoading.value = true
		const api = await getApi(
			props.editingApiId,
			props.systemName,
			selectedEnv.value,
		)
		editingApiData.value = api
	} catch (error) {
		console.error('读取编辑数据失败:', error)
		editingApiData.value = null
	} finally {
		editingApiDataLoading.value = false
	}
}

// 监听编辑ID变化，加载数据
watch(
	() => props.editingApiId,
	async newId => {
		if (newId) {
			await loadEditingApiData()
		} else {
			editingApiData.value = null
		}
	},
	{ immediate: true },
)

// 监听环境变化，重新加载编辑数据
watch(
	() => selectedEnv.value,
	async () => {
		if (props.editingApiId) {
			await loadEditingApiData()
		}
	},
)

// 检查是否是未实现的数据源
const isUnimplementedDataSource = computed(() => {
	return (
		importForm.selectedType &&
		['postman', 'curl', 'apifox'].includes(importForm.selectedType)
	)
})

// 根据类型获取对应的导入组件
const getImportComponent = () => {
	switch (importForm.selectedType) {
		case 'manual':
			return h(ManualImport, {
				systemId: props.systemId,
				systemName: props.systemName,
				selectedEnv: selectedEnv.value,
				editingApiData: editingApiData.value,
				editingApiDataLoading: editingApiDataLoading.value,
				onBack: handleBackToSource,
				onSuccess: handleCancel,
				onSaved: handleSaved,
			})
		case 'openapi':
			return h(OpenApiImport, {
				systemId: props.systemId,
				systemName: props.systemName,
				selectedEnv: selectedEnv.value,
				onBack: handleBackToSource,
				onSuccess: handleCancel,
			})
		case 'postman':
		case 'curl':
		case 'apifox':
			// 未实现的数据源，返回 null，在模板中显示提示
			return null
		default:
			return null
	}
}

// 监听编辑ID，自动选择手动录入
watch(
	() => props.editingApiId,
	newId => {
		if (newId) {
			importForm.selectedType = 'manual'
		}
	},
	{ immediate: true },
)

// 取消导入，通知父组件返回列表页
const emit = defineEmits<{
	close: []
	saved: [] // 编辑模式下保存成功事件
}>()

const handleCancel = () => {
	emit('close')
}

// 编辑模式下保存成功，不关闭页面，只刷新数据
const handleSaved = async () => {
	if (props.editingApiId) {
		// 重新加载编辑数据
		await loadEditingApiData()
		// 通知父组件刷新列表
		emit('saved')
	}
}

// 暴露当前状态，供父组件检查
defineExpose({
	selectedType: computed(() => importForm.selectedType),
})
</script>

<template>
	<div class="api-import-container">
		<div class="import-header">
			<div class="import-header-left">
				<h3 class="import-title">
					{{ props.editingApiId ? '编辑接口' : '导入接口' }}
				</h3>
				<p v-if="props.systemName" class="import-subtitle">
					业务系统：{{ props.systemName }}
				</p>
			</div>
			<div class="import-header-right">
				<EnvSelect
					v-if="props.systemId"
					:system-id="props.systemId"
					:system-name="props.systemName"
					class="env-select-wide"
					@change="handleEnvChange"
				/>
			</div>
		</div>
		<div class="import-content">
			<!-- 数据源选择页面 -->
			<div v-if="!importForm.selectedType" class="import-data-source-wrapper">
				<!-- 返回按钮和标题 -->
				<div class="import-back-header">
					<a-button type="text" class="back-button" @click="handleCancel">
						<i class="ri-arrow-left-line mr-1"></i>
						返回
					</a-button>
					<div class="data-source-title-section">
						<h4 class="data-source-title">导入 API 数据</h4>
						<p class="data-source-subtitle">请选择对应的数据源格式</p>
					</div>
				</div>
				<div class="import-data-source">
					<div class="data-source-options">
						<div
							v-for="option in dataSourceOptions"
							:key="option.type"
							class="data-source-card"
							@click="handleSelectDataSource(option.type)"
						>
							<div class="card-icon" :style="{ backgroundColor: option.color }">
								<i :class="option.icon"></i>
							</div>
							<div class="card-content">
								<div class="card-name">{{ option.name }}</div>
								<div class="card-description">{{ option.description }}</div>
							</div>
						</div>
					</div>
				</div>
			</div>
			<!-- 具体导入页面 -->
			<div v-else class="import-form-container">
				<!-- 未实现的数据源 -->
				<div v-if="isUnimplementedDataSource" class="unimplemented-source">
					<div class="unimplemented-header">
						<a-button
							type="text"
							class="back-button"
							@click="handleBackToSource"
						>
							<i class="ri-arrow-left-line mr-1"></i>
							返回
						</a-button>
					</div>
					<div class="unimplemented-content">
						<a-empty description="功能开发中，敬请期待" />
					</div>
				</div>
				<!-- 已实现的导入组件 -->
				<component :is="getImportComponent()" v-else />
			</div>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.api-import-container {
	display: flex;
	flex-direction: column;
	height: 100%;

	.import-header {
		padding: 16px;
		border-bottom: 1px solid #f0f0f0;
		display: flex;
		align-items: center;
		gap: 16px;
		min-height: 65px;
		.dark & {
			border-bottom-color: #303030;
		}

		.import-header-left {
			flex: 0 0 50%;
			display: flex;
			align-items: center;
			gap: 16px;
		}

		.import-header-right {
			flex: 0 0 50%;
			display: flex;
			align-items: center;
			justify-content: flex-end;
			gap: 12px;

			.env-select-wide {
				width: 100%;

				:deep(.ant-select) {
					width: 100% !important;
				}
			}
		}

		.import-title {
			margin: 0;
			font-size: 16px;
			font-weight: 600;
			color: rgba(0, 0, 0, 0.9);
			white-space: nowrap;
			.dark & {
				color: rgba(255, 255, 255, 0.9);
			}
		}

		.import-subtitle {
			margin: 0;
			font-size: 14px;
			color: rgba(0, 0, 0, 0.65);
			white-space: nowrap;
			.dark & {
				color: rgba(255, 255, 255, 0.65);
			}
		}
	}

	.import-content {
		flex: 1;
		overflow: hidden;
		display: flex;
		flex-direction: column;

		.import-form-container {
			flex: 1;
			display: flex;
			flex-direction: column;
			overflow: hidden;

			.unimplemented-source {
				flex: 1;
				display: flex;
				flex-direction: column;
				overflow: hidden;

				.unimplemented-header {
					padding: 12px 16px;
					border-bottom: 1px solid #f0f0f0;
					.dark & {
						border-bottom-color: #303030;
					}

					.back-button {
						padding: 0;
					}
				}

				.unimplemented-content {
					flex: 1;
					display: flex;
					align-items: center;
					justify-content: center;
				}
			}
		}

		.import-data-source-wrapper {
			display: flex;
			flex-direction: column;
			height: 100%;

			.import-back-header {
				display: flex;
				align-items: center;
				gap: 16px;
				padding: 12px 16px;
				border-bottom: 1px solid #f0f0f0;
				width: 100%;
				box-sizing: border-box;
				flex-shrink: 0;
				.dark & {
					border-bottom-color: #303030;
				}

				.back-button {
					padding: 0;
					flex-shrink: 0;
				}

				.data-source-title-section {
					flex: 1;
					padding-left: 18px;
					min-width: 0;
				}
			}

			.import-data-source {
				max-width: 1200px;
				margin: 0 auto;
				padding: 0;
				overflow-y: auto;
				flex: 1;
				width: 100%;

				.data-source-options {
					padding: 24px;
				}
			}

			.data-source-title {
				margin: 0 0 8px 0;
				font-size: 18px;
				font-weight: 600;
				color: rgba(0, 0, 0, 0.9);
				.dark & {
					color: rgba(255, 255, 255, 0.9);
				}
			}

			.data-source-subtitle {
				margin: 0;
				font-size: 14px;
				color: rgba(0, 0, 0, 0.65);
				.dark & {
					color: rgba(255, 255, 255, 0.65);
				}
			}

			.data-source-options {
				display: grid;
				grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
				gap: 16px;

				.data-source-card {
					display: flex;
					align-items: center;
					padding: 16px;
					background-color: #fff;
					border: 1px solid #d9d9d9;
					border-radius: 8px;
					cursor: pointer;
					transition: all 0.3s;
					.dark & {
						background-color: #1f1f1f;
						border-color: #434343;
					}

					&:hover {
						border-color: #1890ff;
						box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
						.dark & {
							border-color: #1890ff;
						}
					}

					&.is-selected {
						border-color: #1890ff;
						background-color: #e6f7ff;
						box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
						.dark & {
							background-color: rgba(24, 144, 255, 0.1);
							border-color: #1890ff;
						}
					}

					.card-icon {
						width: 48px;
						height: 48px;
						display: flex;
						align-items: center;
						justify-content: center;
						border-radius: 8px;
						margin-right: 12px;
						flex-shrink: 0;

						i {
							font-size: 24px;
							color: #fff;
						}
					}

					.card-content {
						flex: 1;
						min-width: 0;

						.card-name {
							font-size: 14px;
							font-weight: 500;
							color: rgba(0, 0, 0, 0.9);
							margin-bottom: 4px;
							.dark & {
								color: rgba(255, 255, 255, 0.9);
							}
						}

						.card-description {
							font-size: 12px;
							color: rgba(0, 0, 0, 0.45);
							line-height: 1.5;
							.dark & {
								color: rgba(255, 255, 255, 0.45);
							}
						}
					}
				}
			}
		}
	}
}
</style>
