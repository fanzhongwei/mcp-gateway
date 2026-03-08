<!--
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/components/ServiceStatusTag.vue
 * @Author: teddy
 * @Date: 2026-01-24
 * @Description: 服务状态标签组件
-->
<script setup lang="ts">
interface Props {
	status: 'draft' | 'published' | 'stopped'
}

const props = defineProps<Props>()

const statusConfig = {
	draft: {
		text: '草稿',
		color: 'default',
	},
	published: {
		text: '已发布',
		color: 'success',
	},
	stopped: {
		text: '已停止',
		color: 'error',
	},
}

const currentStatus = computed(() => statusConfig[props.status])
</script>

<template>
	<a-tag :color="currentStatus.color">
		{{ currentStatus.text }}
	</a-tag>
</template>
