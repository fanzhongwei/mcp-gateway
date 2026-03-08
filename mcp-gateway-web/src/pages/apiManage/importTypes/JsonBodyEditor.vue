<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/importTypes/JsonBodyEditor.vue
 * @Author: teddy
 * @Date: 2026-01-27
 * @Description: JSON Body编辑器组件
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-27
-->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
	generateId,
	convertJsonParamsToJsonString,
	parseJsonToParams,
} from '~/utils/apiSaveUtils'
import type { JsonParamNode } from '~/utils/jsonParamTypes'
import { JsonParamRowRecursive } from './JsonParamRowRecursive.vue'

// 防抖函数
function debounce<T extends (...args: any[]) => any>(
	func: T,
	wait: number,
): (...args: Parameters<T>) => void {
	let timeout: ReturnType<typeof setTimeout> | null = null
	return function (this: any, ...args: Parameters<T>) {
		if (timeout) {
			clearTimeout(timeout)
		}
		timeout = setTimeout(() => {
			func.apply(this, args)
		}, wait)
	}
}

// 查找节点在树中的位置
const findNodeInTree = (
	tree: JsonParamNode[],
	id: string,
): { node: JsonParamNode; parent: JsonParamNode[]; index: number } | null => {
	for (let i = 0; i < tree.length; i++) {
		if (tree[i].id === id) {
			return { node: tree[i], parent: tree, index: i }
		}
		if (tree[i].children) {
			const found = findNodeInTree(tree[i].children!, id)
			if (found) return found
		}
	}
	return null
}

// Props
const props = defineProps<{
	modelValue: JsonParamNode[]
}>()

// Emits
const emit = defineEmits<{
	'update:modelValue': [value: JsonParamNode[]]
}>()

// 展开/折叠状态
const expandedRowKeys = ref<string[]>([])

// JSON输入框模式开关
const showJsonInput = ref(false)
const jsonInputText = ref('')

// 内部数据 - 使用 computed，确保响应式
const jsonParams = computed(() => {
	const value = props.modelValue || []
	console.log(
		'JsonBodyEditor - get jsonParams:',
		value,
		'length:',
		value.length,
	)
	return value
})

// 监听切换到 JSON 输入框模式，更新 JSON 字符串
watch(
	() => showJsonInput.value,
	newVal => {
		if (newVal) {
			// 切换到 JSON 输入框模式，将 jsonParams 转换为 JSON 字符串
			jsonInputText.value = convertJsonParamsToJsonString(jsonParams.value)
		}
	},
)

// 处理更新，通过 emit 通知父组件
const updateJsonParams = (newValue: JsonParamNode[]) => {
	console.log('JsonBodyEditor - updateJsonParams:', newValue)
	emit('update:modelValue', newValue)
}

// 切换展开/折叠
const handleToggleExpand = (id: string) => {
	const index = expandedRowKeys.value.indexOf(id)
	if (index > -1) {
		expandedRowKeys.value.splice(index, 1)
	} else {
		expandedRowKeys.value.push(id)
	}
}

// 添加JSON参数节点
const handleAddJsonParam = (parentId?: string) => {
	const newNode: JsonParamNode = {
		id: generateId(),
		name: '',
		required: false,
		type: 'string',
		example: '',
		description: '',
	}

	const currentParams = [...jsonParams.value]

	if (parentId) {
		const found = findNodeInTree(currentParams, parentId)
		if (found && found.node.type === 'object') {
			// object 类型可以添加多个子节点
			if (!found.node.children) {
				found.node.children = []
			}
			found.node.children.push(newNode)
			// 自动展开父节点
			if (!expandedRowKeys.value.includes(parentId)) {
				expandedRowKeys.value.push(parentId)
			}
			updateJsonParams(currentParams)
		} else if (found && found.node.type === 'array') {
			// array 类型只能有一个 items 子节点
			if (!found.node.children || found.node.children.length === 0) {
				// 如果没有 items 节点，创建一个
				if (!found.node.children) {
					found.node.children = []
				}
				found.node.children.push({
					id: generateId(),
					name: 'items',
					required: false,
					type: 'string',
					example: '',
					description: '',
				})
				// 自动展开父节点
				if (!expandedRowKeys.value.includes(parentId)) {
					expandedRowKeys.value.push(parentId)
				}
				updateJsonParams(currentParams)
			} else {
				// 已经有 items 节点，提示用户
				message.warning('数组类型只能有一个 items 子节点用于标识元素类型')
			}
		}
	} else {
		currentParams.push(newNode)
		updateJsonParams(currentParams)
	}
}

// 添加JSON参数子节点
const handleAddJsonParamChild = (parentId: string) => {
	handleAddJsonParam(parentId)
}

// 删除JSON参数节点
const handleDeleteJsonParam = (id: string) => {
	const currentParams = JSON.parse(JSON.stringify(jsonParams.value))
	const found = findNodeInTree(currentParams, id)
	if (found) {
		found.parent.splice(found.index, 1)
		updateJsonParams(currentParams)
	}
}

// 更新JSON参数节点
const handleUpdateParam = (
	id: string,
	field: keyof JsonParamNode,
	value: any,
) => {
	const currentParams = JSON.parse(JSON.stringify(jsonParams.value))
	const found = findNodeInTree(currentParams, id)
	if (found && found.node) {
		// 如果节点名称是 items，不允许修改名称
		if (field === 'name' && found.node.name === 'items' && value !== 'items') {
			message.warning('items 节点的名称不允许修改')
			return
		}
		;(found.node as any)[field] = value
		// 当类型改变为 object 时，如果 children 不存在，初始化为空数组
		if (field === 'type' && value === 'object') {
			if (!found.node.children) {
				found.node.children = []
			}
			// 自动展开该节点
			if (!expandedRowKeys.value.includes(id)) {
				expandedRowKeys.value.push(id)
			}
		}
		// 当类型改变为 array 时，如果 children 不存在或为空，自动添加一个 items 子节点
		if (field === 'type' && value === 'array') {
			if (!found.node.children || found.node.children.length === 0) {
				found.node.children = [
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
			// 自动展开该节点
			if (!expandedRowKeys.value.includes(id)) {
				expandedRowKeys.value.push(id)
			}
		}
		// 当类型改变为非 object 和 array 时，清除 children
		if (field === 'type' && value !== 'object' && value !== 'array') {
			delete found.node.children
			// 如果节点已展开，则折叠
			const index = expandedRowKeys.value.indexOf(id)
			if (index > -1) {
				expandedRowKeys.value.splice(index, 1)
			}
		}
		updateJsonParams(currentParams)
	}
}

// 导入JSON模态框
const showImportJsonModal = ref(false)
const importJsonText = ref('')

// 从JSON字符串导入结构
const importJsonStructure = (jsonString: string) => {
	try {
		const json = JSON.parse(jsonString)
		// 解析并更新
		const newParams = parseJsonToParams(json)
		updateJsonParams(newParams)
		message.success('JSON结构导入成功')
	} catch (error) {
		message.error('JSON格式错误，请检查JSON数据')
	}
}

// 处理导入JSON
const handleImportJson = () => {
	if (!importJsonText.value.trim()) {
		message.warning('请输入JSON数据')
		return
	}
	importJsonStructure(importJsonText.value)
	showImportJsonModal.value = false
	importJsonText.value = ''
}

// 判断节点是否展开
const isParamExpanded = (param: JsonParamNode) => {
	return expandedRowKeys.value.includes(param.id)
}

// 判断节点是否可以展开
const canParamExpand = (param: JsonParamNode) => {
	return param.type === 'object' || param.type === 'array'
}

// 计算缩进
const getParamIndent = (level: number) => {
	return level * 24
}

// 处理参数更新（带防抖，300ms延迟）
const handleParamUpdateDebounced = debounce(
	(param: JsonParamNode, field: keyof JsonParamNode, value: any) => {
		handleUpdateParam(param.id, field, value)
	},
	300,
)

// 处理参数更新
const handleParamUpdate = (
	param: JsonParamNode,
	field: keyof JsonParamNode,
	value: any,
) => {
	handleParamUpdateDebounced(param, field, value)
}

// 切换 JSON 输入框模式
const handleToggleJsonInput = (checked: boolean) => {
	if (checked) {
		// 切换到 JSON 输入框模式，将 jsonParams 转换为 JSON 字符串
		showJsonInput.value = true
		jsonInputText.value = convertJsonParamsToJsonString(jsonParams.value)
	} else {
		// 切换回参数编辑器模式，直接切换显示模式，不进行反向解析
		showJsonInput.value = false
	}
}

// 获取当前 JSON 字符串（用于发送请求）
// 如果手动输入过 JSON（jsonInputText 有内容），返回手动输入的字符串；否则将 jsonParams 转换为 JSON 字符串
const getJsonString = (): string => {
	// 如果 jsonInputText 有内容，说明用户手动输入过，优先使用手动输入的 JSON
	if (jsonInputText.value && jsonInputText.value.trim()) {
		return jsonInputText.value.trim()
	}
	// 否则将 jsonParams 转换为 JSON 字符串
	return convertJsonParamsToJsonString(jsonParams.value)
}

// 暴露方法给父组件
defineExpose({
	getJsonString,
})
</script>

<template>
	<div class="json-body-editor">
		<div class="tab-content-header">
			<div class="tab-content-title-wrapper">
				<h4 class="tab-content-title">JSON 参数</h4>
				<a-switch
					v-model:checked="showJsonInput"
					size="small"
					@change="handleToggleJsonInput"
				>
					<template #checkedChildren>Schema</template>
					<template #unCheckedChildren>测试参数</template>
				</a-switch>
			</div>
			<div class="tab-content-actions">
				<a-button
					v-if="!showJsonInput"
					type="primary"
					size="small"
					@click="showImportJsonModal = true"
				>
					<i class="ri-file-upload-line mr-1"></i>
					导入 JSON
				</a-button>
				<a-button
					v-if="!showJsonInput"
					type="link"
					@click="handleAddJsonParam()"
				>
					<i class="ri-add-line mr-1"></i>
					添加参数
				</a-button>
			</div>
		</div>
		<!-- JSON 输入框模式 -->
		<div v-if="showJsonInput" class="json-input-container">
			<a-textarea
				v-model:value="jsonInputText"
				placeholder='请输入 JSON 数据，例如：{"key": "value"}'
				:rows="20"
				class="json-input-textarea"
			/>
		</div>
		<!-- 自定义JSON参数树形结构 -->
		<div v-else class="json-params-container">
			<!-- 表头 -->
			<div class="json-params-header">
				<div class="json-param-cell" style="width: 20%">名字</div>
				<div class="json-param-cell" style="width: 8%">必填</div>
				<div class="json-param-cell" style="width: 12%">类型</div>
				<div class="json-param-cell" style="width: 20%">示例</div>
				<div class="json-param-cell" style="width: 25%">描述</div>
				<div class="json-param-cell" style="width: 15%">操作</div>
			</div>
			<!-- 参数列表 -->
			<div class="json-params-body">
				<!-- 调试信息 -->
				<div
					v-if="jsonParams.length === 0"
					style="padding: 20px; text-align: center; color: #999"
				>
					暂无参数数据
				</div>
				<template v-for="jsonParam in jsonParams" v-else :key="jsonParam.id">
					<div class="json-param-row">
						<div class="json-param-row-content">
							<!-- 名字（包含展开/折叠图标） -->
							<div class="json-param-cell" style="width: 20%">
								<div style="display: flex; align-items: center; gap: 8px">
									<div
										class="json-param-expand-icon"
										:style="{ paddingLeft: getParamIndent(0) + 'px' }"
										@click="
											canParamExpand(jsonParam) &&
												handleToggleExpand(jsonParam.id)
										"
									>
										<i
											v-if="canParamExpand(jsonParam)"
											:class="
												isParamExpanded(jsonParam)
													? 'ri-arrow-down-s-line'
													: 'ri-arrow-right-s-line'
											"
										></i>
										<span
											v-else
											style="width: 16px; display: inline-block"
										></span>
									</div>
									<a-input
										:value="jsonParam.name"
										placeholder="参数名"
										size="small"
										class="param-input"
										style="flex: 1"
										:disabled="jsonParam.name === 'items'"
										@change="
											(e: any) =>
												handleParamUpdate(jsonParam, 'name', e.target.value)
										"
									/>
								</div>
							</div>
							<!-- 必填 -->
							<div
								class="json-param-cell json-param-cell-checkbox"
								style="width: 8%"
							>
								<a-checkbox
									:checked="jsonParam.required"
									@update:checked="
										(val: boolean) =>
											handleParamUpdate(jsonParam, 'required', val)
									"
								/>
							</div>
							<!-- 类型 -->
							<div class="json-param-cell" style="width: 12%">
								<a-select
									:value="jsonParam.type"
									size="small"
									class="param-select"
									@change="
										(val: any) => handleParamUpdate(jsonParam, 'type', val)
									"
								>
									<a-select-option value="string">string</a-select-option>
									<a-select-option value="number">number</a-select-option>
									<a-select-option value="array">array</a-select-option>
									<a-select-option value="object">object</a-select-option>
									<a-select-option value="boolean">boolean</a-select-option>
								</a-select>
							</div>
							<!-- 示例 -->
							<div class="json-param-cell" style="width: 20%">
								<a-input
									:value="jsonParam.example"
									placeholder="示例值"
									size="small"
									class="param-input"
									@change="
										(e: any) =>
											handleParamUpdate(jsonParam, 'example', e.target.value)
									"
								/>
							</div>
							<!-- 描述 -->
							<div class="json-param-cell" style="width: 25%">
								<a-input
									:value="jsonParam.description"
									placeholder="描述"
									size="small"
									class="param-input"
									@change="
										(e: any) =>
											handleParamUpdate(
												jsonParam,
												'description',
												e.target.value,
											)
									"
								/>
							</div>
							<!-- 操作 -->
							<div
								class="json-param-cell json-param-cell-actions"
								style="width: 15%"
							>
								<a-space>
									<a-button
										v-if="
											jsonParam.type === 'object' || jsonParam.type === 'array'
										"
										type="link"
										size="small"
										@click="handleAddJsonParamChild(jsonParam.id)"
									>
										<i class="ri-add-line"></i>
									</a-button>
									<a-button
										type="link"
										danger
										size="small"
										@click="handleDeleteJsonParam(jsonParam.id)"
									>
										<i class="ri-delete-bin-line"></i>
									</a-button>
								</a-space>
							</div>
						</div>
						<!-- 子节点 - 使用递归组件 -->
						<div
							v-if="isParamExpanded(jsonParam) && canParamExpand(jsonParam)"
							class="json-param-children"
						>
							<template
								v-if="jsonParam.children && jsonParam.children.length > 0"
							>
								<JsonParamRowRecursive
									v-for="childParam in jsonParam.children"
									:key="childParam.id"
									:param="childParam"
									:level="1"
									:expanded-keys="expandedRowKeys"
									@toggle-expand="handleToggleExpand"
									@add-child="handleAddJsonParamChild"
									@delete="handleDeleteJsonParam"
									@update-param="handleUpdateParam"
								/>
							</template>
						</div>
					</div>
				</template>
			</div>
		</div>
		<!-- 导入JSON模态框 -->
		<a-modal
			v-model:open="showImportJsonModal"
			title="导入 JSON"
			:width="600"
			@ok="handleImportJson"
		>
			<a-textarea
				v-model:value="importJsonText"
				placeholder='请输入 JSON 数据，例如：{"key": "value"}'
				:rows="10"
			/>
		</a-modal>
	</div>
</template>

<style lang="scss" scoped>
.json-body-editor {
	display: flex;
	flex-direction: column;
	height: 100%;
	min-height: 0;
	overflow: hidden;

	.tab-content-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 16px;
		position: sticky;
		top: 0;
		background-color: #fff;
		z-index: 10;
		padding-top: 0;
		padding-bottom: 0;
		.dark & {
			background-color: #1f1f1f;
		}
	}

	.tab-content-title-wrapper {
		display: flex;
		align-items: center;
		gap: 12px;
	}

	.tab-content-title {
		margin: 0;
		font-size: 14px;
		font-weight: 500;
		color: rgba(0, 0, 0, 0.85);
		.dark & {
			color: rgba(255, 255, 255, 0.85);
		}
	}

	.json-input-container {
		flex: 1;
		display: flex;
		flex-direction: column;
		min-height: 0;
		border: 1px solid #f0f0f0;
		border-radius: 4px;
		overflow: hidden;
		background-color: #fff;
		.dark & {
			border-color: #303030;
			background-color: #1f1f1f;
		}

		.json-input-textarea {
			flex: 1;
			min-height: 0;
			:deep(.ant-input) {
				border: none;
				border-radius: 0;
				resize: none;
				font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas',
					'source-code-pro', monospace;
				font-size: 13px;
				line-height: 1.6;
				.dark & {
					background-color: #1f1f1f;
					color: rgba(255, 255, 255, 0.85);
				}
			}
		}
	}

	.tab-content-actions {
		display: flex;
		align-items: baseline;
		gap: 8px;
	}

	// JSON参数自定义结构样式
	.json-params-container {
		flex: 1;
		display: flex;
		flex-direction: column;
		min-height: 0;
		border: 1px solid #f0f0f0;
		border-radius: 4px;
		overflow: hidden;
		background-color: #fff;
		.dark & {
			border-color: #303030;
			background-color: #1f1f1f;
		}

		.json-params-header {
			display: flex !important;
			flex-direction: row !important;
			flex-wrap: nowrap !important;
			background-color: #fafafa;
			border-bottom: 1px solid #f0f0f0;
			font-weight: 500;
			font-size: 14px;
			flex-shrink: 0;
			.dark & {
				background-color: #262626;
				border-bottom-color: #303030;
			}

			.json-param-cell {
				padding: 12px 8px;
				text-align: left;
				color: rgba(0, 0, 0, 0.85);
				box-sizing: border-box;
				flex-shrink: 0;
				text-align: center;
				.dark & {
					color: rgba(255, 255, 255, 0.85);
				}
			}
		}

		.json-params-body {
			flex: 1;
			overflow-y: auto;
			min-height: 0;
			// 使用通用选择器，让所有层级的 .json-param-row 都应用相同样式
			:deep(.json-param-row) {
				border-bottom: 1px solid #f0f0f0;
				.dark & {
					border-bottom-color: #303030;
				}

				&:last-child {
					border-bottom: none;
				}
			}
			:deep(.json-param-row .json-param-row-content) {
				display: flex !important;
				flex-direction: row !important;
				flex-wrap: nowrap !important;
				align-items: center !important;
				min-height: 48px;
				width: 100%;
				background-color: #fff;
				transition: background-color 0.2s;
				.dark & {
					background-color: #1f1f1f;
				}

				&:hover {
					background-color: #fafafa;
					.dark & {
						background-color: #262626;
					}
				}
			}

			:deep(.json-param-row .json-param-expand-icon) {
				display: flex;
				align-items: center;
				justify-content: center;
				cursor: pointer;
				color: rgba(0, 0, 0, 0.45);
				transition: color 0.2s;
				flex-shrink: 0;
				min-width: 24px;
				.dark & {
					color: rgba(255, 255, 255, 0.45);
				}

				&:hover {
					color: rgba(0, 0, 0, 0.85);
					.dark & {
						color: rgba(255, 255, 255, 0.85);
					}
				}

				i {
					font-size: 16px;
				}
			}

			:deep(.json-param-row .json-param-cell) {
				padding: 0 8px;
				display: flex !important;
				flex-direction: row !important;
				align-items: center !important;
				box-sizing: border-box;
				flex-shrink: 0 !important;
				flex-grow: 0;
				min-height: 32px;
				overflow: hidden;

				&.json-param-cell-checkbox {
					justify-content: center;
				}

				&.json-param-cell-actions {
					justify-content: center;
				}

				.param-input {
					flex: 1;
					min-width: 0;
					height: 38px;

					.ant-input {
						border: 1px solid #d9d9d9;
						border-radius: 4px;
						width: 100%;
						.dark & {
							border-color: #434343;
							background-color: #1f1f1f;
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.ant-input:focus {
						border-color: #40a9ff;
						box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
					}
				}

				.param-select {
					width: 100%;
					flex: 1;
					min-width: 0;
					height: 38px;

					.ant-select {
						width: 100%;
					}

					.ant-select-selector {
						border: 1px solid #d9d9d9;
						border-radius: 4px;
						width: 100%;
						height: 38px;
						.dark & {
							border-color: #434343;
							background-color: #1f1f1f;
						}
						.ant-select-selection-item {
							line-height: 38px;
						}
					}

					.ant-select-focused .ant-select-selector {
						border-color: #40a9ff;
						box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
					}
				}

				:deep(.ant-checkbox) {
					:deep(.ant-checkbox-inner) {
						border: 1px solid #d9d9d9;
						border-radius: 2px;
						.dark & {
							border-color: #434343;
							background-color: #1f1f1f;
						}
					}
				}

				:deep(.ant-space) {
					display: flex;
					align-items: center;
				}

				:deep(.ant-btn-link) {
					padding: 0 4px;
					height: auto;
					line-height: 1.5;
				}
			}
		}

		.json-param-children {
			background-color: #fafafa;
			.dark & {
				background-color: #1f1f1f;
			}
		}
	}
}
</style>
