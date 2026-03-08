<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/develop/tree.vue
 * @Author: teddy
 * @Date: 2024-02-12 12:58:29
 * @Description: 开发工具-tree
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-21 14:45:05
-->
<script setup lang="ts">
import { _getOrgData } from '~/assets/js/util'

// const areaTenant = ref<any>(undefined)
onMounted(() => {
	_getOrgData()
})
</script>

<template>
	<a-alert
		type="info"
		show-icon
		message="支持海量数据,阅读代码查看使用方法。"
	/>
	<a-button
		class="!font-bold"
		type="link"
		target="_blank"
		href="https://antdv.com/components/tree-select-cn"
	>
		可参考 UI 组件库文档地址
	</a-button>
	<!-- 租户选择-所有数据 -->
	<a-card title="租户选择-所有数据">
		<tree-tenant-all></tree-tenant-all>
	</a-card>
	<show-code :code="`<tree-tenant-all></tree-tenant-all>`">
		<template #message>
			<table>
				<tr>
					<th>属性</th>
					<th>描述</th>
					<th>默认值</th>
				</tr>
				<tr>
					<td>expandAll</td>
					<td>是否展开全部</td>
					<td>false</td>
				</tr>
				<tr>
					<td>placeholder</td>
					<td>提示文字</td>
					<td>请选择租户、部门、人员</td>
				</tr>
				<tr>
					<td>v-model:value</td>
					<td>双向绑定的值</td>
					<td>-</td>
				</tr>
				<tr>
					<td>@change</td>
					<td>事件：值变化时调用此函数，table 时建议触发 getTableList 方法</td>
					<td>function(value: string)</td>
				</tr>
			</table>
		</template>
	</show-code>

	<!-- 租户选择-租户层 -->
	<a-card title="租户选择-租户层">
		<tree-tenant></tree-tenant>
	</a-card>
	<show-code :code="`<tree-tenant></tree-tenant>`">
		<template #message>
			<table>
				<tr>
					<th>属性</th>
					<th>描述</th>
					<th>默认值</th>
				</tr>
				<tr>
					<td>expandAll</td>
					<td>是否展开全部</td>
					<td>false</td>
				</tr>
				<tr>
					<td>placeholder</td>
					<td>提示文字</td>
					<td>请选择租户</td>
				</tr>
				<tr>
					<td>v-model:value</td>
					<td>双向绑定的值</td>
					<td>-</td>
				</tr>
				<tr>
					<td>@change</td>
					<td>事件：值变化时调用此函数，table 时建议触发 getTableList 方法</td>
					<td>function(value: string)</td>
				</tr>
			</table>
		</template>
	</show-code>
	<!-- 租户选择-部门层 -->
	<a-card title="租户选择-部门层，非部门不可选">
		<tree-dept></tree-dept>
	</a-card>
	<show-code :code="`<tree-dept></tree-dept>`">
		<template #message>
			<table>
				<tr>
					<th>属性</th>
					<th>描述</th>
					<th>默认值</th>
				</tr>
				<tr>
					<td>expandAll</td>
					<td>是否展开全部</td>
					<td>false</td>
				</tr>
				<tr>
					<td>placeholder</td>
					<td>提示文字</td>
					<td>请选部门</td>
				</tr>
				<tr>
					<td>v-model:value</td>
					<td>双向绑定的值</td>
					<td>-</td>
				</tr>
				<tr>
					<td>@change</td>
					<td>事件：值变化时调用此函数，table 时建议触发 getTableList 方法</td>
					<td>function(value: string)</td>
				</tr>
			</table>
		</template>
	</show-code>
</template>

<style lang="scss"></style>
