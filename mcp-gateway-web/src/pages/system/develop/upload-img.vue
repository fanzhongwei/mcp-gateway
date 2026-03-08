<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/develop/upload-img.vue
 * @Author: teddy
 * @Date: 2024-03-19 15:09:27
 * @Description: 图片上传方法
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-21 16:23:57
-->
<script setup lang="ts">
const imgUrl = ref('')
</script>

<template>
	<a-card title="图片上传组件-功能演示">
		<upload-oss v-model:fileKey="imgUrl" code="develop"></upload-oss>
	</a-card>
	<br />
	<show-code
		:code="`<upload-oss v-model:fileKey=&quot;imgUrl&quot;></upload-oss>`"
	>
		<template #message>
			<table>
				<tr>
					<th>属性</th>
					<th>描述</th>
					<th>默认值</th>
				</tr>
				<tr>
					<td>code</td>
					<td>在图片 url 地址中使用固定的值拼接</td>
					<td>-</td>
				</tr>
				<tr>
					<td>v-model:fileKey</td>
					<td>双向绑定的值</td>
					<td>-</td>
				</tr>
			</table>
		</template>
	</show-code>
</template>

<style lang="scss"></style>
