<!--
 * @FilePath: /mcp-gateway-web/src/components/tree-dept.vue
 * @Author: teddy
 * @Date: 2024-03-20 11:40:34
 * @Description: tree 部门层
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-24 10:21:52
-->

<script setup lang="ts">
import { useStorage } from '@vueuse/core'
import { _deepClone } from '~/assets/js/util'

const props = defineProps({
	// 是否展开全部
	expandAll: {
		type: Boolean,
		default: false,
	},
	placeholder: {
		type: String,
		default: '请选择部门',
	},
	// 租户
	tenant: {
		type: String || undefined,
		default: undefined,
	},
})
defineEmits(['change'])

// 租户选择-租户层-租户数据-storage
const orgDeptStorage = useStorage('pay-org-dept-data', [])

const searchValue = ref<any>('')
const deptData = ref<any[]>([])

const dept: any = defineModel('value', {
	type: String || undefined,
	default: undefined,
})

watch(
	() => props.tenant,
	(newVal, oldVal) => {
		searchValue.value = ''
		// 仅当租户从有值变为另一值时清空部门（用户切换了租户），初始回填或 tenant 首次设置时保留部门
		if (oldVal !== undefined && newVal !== oldVal) {
			dept.value = undefined
		}
		// 清洗区域数据
		const setOrgAllData = (arr = [], is = false) => {
			arr.map((item: any) => {
				if (item.tenant !== newVal && newVal) {
					item.disabled = true
				}
				if (item.children && item.children.length > 0) {
					setOrgAllData(item.children, is)
				}
			})
		}
		const data = _deepClone(orgDeptStorage.value)
		// 清洗区域数据
		setOrgAllData(data)
		deptData.value = data
	},
)

onMounted(() => {
	deptData.value = _deepClone(orgDeptStorage.value)
})
</script>

<template>
	<a-tree-select
		v-model:value="dept"
		v-model:searchValue="searchValue"
		:tree-default-expand-all="expandAll"
		class="w-full"
		show-search
		:treeLine="{
			showLeafIcon: false,
		}"
		:placeholder="placeholder"
		allow-clear
		:height="233"
		:tree-data="deptData"
		tree-node-filter-prop="label"
		@change="(value: any) => $emit('change', 1)"
	>
		<template #title="{ title }">
			<template
				v-for="(fragment, i) in (title || '')
					.toString()
					.split(new RegExp(`(?<=${searchValue})|(?=${searchValue})`, 'i'))"
			>
				<span
					v-if="fragment.toLowerCase() === searchValue.toLowerCase()"
					:key="i"
					style="color: #08c"
				>
					{{ fragment }}
				</span>
				<template v-else>{{ fragment }}</template>
			</template>
		</template>
	</a-tree-select>
</template>

<style lang="scss"></style>
