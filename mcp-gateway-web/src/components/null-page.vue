<!--
 * @FilePath: /mcp-gateway-web/src/components/null-page.vue
 * @Author: teddy
 * @Date: 2024-02-29 16:43:47
 * @Description: 空白页
 * @LastEditors: teddy
 * @LastEditTime: 2024-02-29 16:45:47
-->
<script setup lang="ts">
defineProps({
	msg: {
		type: String,
		default: 'hello',
	},
})
</script>

<template>
	<a-watermark content="首页">
		<div class="page-max flex items-center justify-center">
			<a-result status="404" :sub-title="msg"></a-result>
		</div>
	</a-watermark>
</template>

<style lang="scss">
.page-max {
	height: calc(100vh - 180px);
}
</style>
