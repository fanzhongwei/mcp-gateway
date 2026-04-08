<script setup lang="ts">
import { message } from 'ant-design-vue'
import { saveApis } from '~/utils/apiSaveUtils'
import type { ParsedImportResult } from '../../../utils/apiImport/parsedImportTypes'

const props = defineProps<{
	parsedResults: ParsedImportResult[]
}>()

const emit = defineEmits<{
	cancel: []
	success: []
}>()

const searchValue = ref('')
const editableNameMap = ref<Record<string, string>>({})
const checkedKeys = ref<{ checked: string[]; halfChecked: string[] }>({
	checked: [],
	halfChecked: [],
})
const expandedKeys = ref<string[]>([])

const sourceTreeData = computed(() => {
	const groupMap = new Map<string, any[]>()
	props.parsedResults.forEach(item => {
		const group = item.group || '默认分组'
		if (!groupMap.has(group)) {
			groupMap.set(group, [])
		}
		groupMap.get(group)!.push({
			title: `${item.method} ${item.path}`,
			key: item.key,
			isLeaf: true,
			method: item.method,
			path: item.path,
			summary: item.summary || item.name || `${item.method} ${item.path}`,
			description: item.description || '',
		})
	})
	return Array.from(groupMap.entries()).map(([group, children]) => ({
		title: group,
		key: `group::${group}`,
		children,
	}))
})

const filterTreeData = (nodes: any[], keyword: string): any[] => {
	if (!keyword.trim()) {
		return nodes
	}
	const k = keyword.toLowerCase().trim()
	return nodes
		.map(node => {
			const matchedChildren = node.children
				? filterTreeData(node.children, keyword)
				: []
			const title = String(node.title || '').toLowerCase()
			const summary = String(node.summary || '').toLowerCase()
			const path = String(node.path || '').toLowerCase()
			const method = String(node.method || '').toLowerCase()
			const matched =
				title.includes(k) ||
				summary.includes(k) ||
				path.includes(k) ||
				method.includes(k)
			if (matched || matchedChildren.length > 0) {
				return {
					...node,
					children:
						matchedChildren.length > 0 ? matchedChildren : node.children,
				}
			}
			return null
		})
		.filter(Boolean) as any[]
}

const treeData = computed(() =>
	filterTreeData(sourceTreeData.value, searchValue.value || ''),
)

const selectableApiKeySet = computed(
	() => new Set(props.parsedResults.map(item => item.key)),
)

const selectedCount = computed(
	() =>
		(checkedKeys.value.checked || []).filter(key =>
			selectableApiKeySet.value.has(key),
		).length,
)

watch(
	sourceTreeData,
	value => {
		expandedKeys.value = value.map(item => item.key)
		checkedKeys.value = { checked: [], halfChecked: [] }
		editableNameMap.value = Object.fromEntries(
			props.parsedResults.map(item => [
				item.key,
				item.name || item.summary || item.path || '',
			]),
		)
	},
	{ immediate: true },
)

const handleCheck = (checked: any) => {
	if (typeof checked === 'object' && checked.checked) {
		checkedKeys.value = checked
	} else if (Array.isArray(checked)) {
		checkedKeys.value = { checked, halfChecked: [] }
	}
}

const handleImport = async () => {
	const selectedKeys = new Set(checkedKeys.value.checked || [])
	const selected = props.parsedResults
		.filter(item => selectedKeys.has(item.key))
		.map(item => {
			const apiData = item.apiData
			if (!apiData) return null
			const editedName = (editableNameMap.value[item.key] || '').trim()
			return {
				...apiData,
				apiForm: {
					...apiData.apiForm,
					name: editedName || item.path,
				},
			}
		})
		.filter(Boolean)
	if (!selected.length) {
		message.warning('请至少选择一个可导入接口')
		return
	}
	const result = await saveApis(selected as any[])
	if (result.success > 0) {
		emit('success')
	}
}
</script>

<template>
	<div class="api-select-section">
		<div class="api-select-header">
			<div class="header-title">
				<h3>选择要导入的 API</h3>
				<span class="selected-count">已选择 {{ selectedCount }} 个 API</span>
			</div>
			<a-button type="text" @click="emit('cancel')">
				<i class="ri-close-line"></i>
			</a-button>
		</div>
		<div class="api-search-box">
			<a-input
				v-model:value="searchValue"
				placeholder="搜索 API（支持路径、方法、描述等）"
				allow-clear
				size="large"
			>
				<template #prefix>
					<i class="ri-search-line"></i>
				</template>
			</a-input>
		</div>
		<div class="api-tree-container">
			<a-tree
				:tree-data="treeData"
				:checked-keys="checkedKeys"
				:expanded-keys="expandedKeys"
				checkable
				:check-strictly="false"
				class="api-tree"
				@check="handleCheck"
				@expand="expandedKeys = $event"
			>
				<template #title="{ title, dataRef }">
					<div class="tree-node-title">
						<span class="node-title-text">{{ title }}</span>
						<span
							v-if="dataRef?.isLeaf && dataRef?.method"
							class="method-badge"
							:class="`method-${dataRef.method.toLowerCase()}`"
						>
							{{ dataRef.method }}
						</span>
						<span
							v-if="dataRef?.summary && dataRef?.isLeaf"
							class="node-summary"
						>
							<a-input
								v-model:value="editableNameMap[dataRef.key]"
								size="small"
								class="name-input"
								placeholder="请输入接口名称"
								@click.stop
							/>
						</span>
					</div>
					<div
						v-if="dataRef?.description && !dataRef?.isLeaf"
						class="tree-node-description"
					>
						{{ dataRef.description }}
					</div>
				</template>
			</a-tree>
		</div>
		<div class="api-select-footer">
			<a-space>
				<a-button
					type="primary"
					size="large"
					:disabled="selectedCount === 0"
					@click="handleImport"
				>
					<i class="ri-check-line mr-1"></i>
					确认导入 ({{ selectedCount }})
				</a-button>
				<a-button size="large" @click="emit('cancel')">取消</a-button>
			</a-space>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.api-select-section {
	flex: 1;
	display: flex;
	flex-direction: column;
	overflow: hidden;
}
.api-select-header,
.api-search-box,
.api-select-footer {
	padding: 16px 24px;
	border-bottom: 1px solid #f0f0f0;
}
.api-select-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}
.api-tree-container {
	flex: 1;
	overflow-y: auto;
	padding: 16px 24px;
}
.api-select-footer {
	border-top: 1px solid #f0f0f0;
	border-bottom: none;
}
.tree-node-title {
	display: flex;
	align-items: center;
	gap: 8px;
	flex-wrap: wrap;
}
.method-badge {
	display: inline-block;
	padding: 2px 8px;
	border-radius: 4px;
	font-size: 12px;
	font-weight: 500;
	line-height: 1.5;
	background-color: #e6f7ff;
	color: #1890ff;
}
.node-summary {
	font-size: 12px;
	color: rgba(0, 0, 0, 0.45);
}
.name-input {
	width: 320px;
}
.tree-node-description {
	margin-top: 4px;
	font-size: 12px;
	color: rgba(0, 0, 0, 0.45);
}
</style>
