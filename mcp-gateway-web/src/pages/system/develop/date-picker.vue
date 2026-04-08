<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/develop/date-picker.vue
 * @Author: teddy
 * @Date: 2024-07-04 14:08:43
 * @Description: 日期区间
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-05 10:31:26
-->
<script setup lang="ts">
const code = `<date-picker v-model:value="dateRange"></date-picker>`

const dateRange = ref([])
</script>

<template>
	<a-card title="日期区间组件，默认7天内，遇到跨年开始时间取当年第一天">
		<date-picker v-model:value="dateRange"></date-picker>
		<div class="mt-3">选择结果：{{ dateRange }}</div>
	</a-card>
	<br />
	<show-code :code="code">
		<template #message>
			<table>
				<tbody>
					<tr>
						<th>属性</th>
						<th>描述</th>
						<th>默认值</th>
					</tr>
					<tr>
						<td>v-model:value</td>
						<td>双向绑定的值</td>
						<td>[]</td>
					</tr>
				</tbody>
			</table>
		</template>
	</show-code>
</template>

<style lang="scss"></style>
