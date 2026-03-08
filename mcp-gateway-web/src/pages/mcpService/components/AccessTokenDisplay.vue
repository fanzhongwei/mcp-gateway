<!--
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/components/AccessTokenDisplay.vue
 * @Author: teddy
 * @Date: 2026-01-24
 * @Description: 访问令牌显示组件
-->
<script setup lang="ts">
import { message } from 'ant-design-vue'

interface Props {
	token: string
	serviceId?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
	regenerate: []
}>()

const showToken = ref(false)

// 复制令牌
const handleCopy = async () => {
	try {
		await navigator.clipboard.writeText(props.token)
		message.success('访问令牌已复制到剪贴板')
	} catch (error) {
		message.error('复制失败，请手动复制')
	}
}

// 重新生成令牌
const handleRegenerate = () => {
	emit('regenerate')
}
</script>

<template>
	<div class="access-token-display">
		<div class="token-content">
			<a-input v-if="showToken" :value="token" readonly class="token-input">
				<template #suffix>
					<a-button type="text" size="small" @click="handleCopy">
						<i class="ri-file-copy-line"></i>
					</a-button>
				</template>
			</a-input>
			<div v-else class="token-hidden">••••••••••••••••</div>
		</div>
		<div class="token-actions">
			<a-button type="link" size="small" @click="showToken = !showToken">
				{{ showToken ? '隐藏' : '显示' }}
			</a-button>
			<a-button
				v-if="serviceId"
				type="link"
				size="small"
				@click="handleRegenerate"
			>
				重新生成
			</a-button>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.access-token-display {
	display: flex;
	align-items: center;
	gap: 12px;

	.token-content {
		flex: 1;

		.token-input {
			max-width: 400px;
		}

		.token-hidden {
			padding: 4px 11px;
			font-family: monospace;
			letter-spacing: 2px;
		}
	}

	.token-actions {
		display: flex;
		gap: 8px;
	}
}
</style>
