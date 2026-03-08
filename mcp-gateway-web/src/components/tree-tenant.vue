<!--
 * @FilePath: /mcp-gateway-web/src/components/tree-tenant.vue
 * @Author: teddy
 * @Date: 2024-03-20 10:15:48
 * @Description: tree 租户层
 * @LastEditors: teddy
 * @LastEditTime: 2024-04-19 10:36:27
-->
<script setup lang="ts">
import { useStorage } from '@vueuse/core'

defineProps({
	// 是否展开全部
	expandAll: {
		type: Boolean,
		default: false,
	},
	placeholder: {
		type: String,
		default: '请选择租户',
	},
})
defineEmits(['change'])

// 租户选择-租户层-租户数据-storage
const orgStorage = useStorage('pay-org-data', [])

const searchValue = ref<any>('')

const tenant = defineModel('value', {
	type: String || undefined,
	default: undefined,
})
</script>

<template>
	<a-tree-select
		v-model:value="tenant"
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
		:tree-data="orgStorage"
		tree-node-filter-prop="label"
		@change="() => $emit('change', tenant)"
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
