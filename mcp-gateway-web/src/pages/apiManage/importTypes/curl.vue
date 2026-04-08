<script setup lang="ts">
import { message } from 'ant-design-vue'
import ApiImportResultSelector from './ApiImportResultSelector.vue'
import type { ParsedImportResult } from '../../../utils/apiImport/parsedImportTypes'
import { parseCurlCommand } from '../../../utils/apiImport/sourceImportParsers/curl'

const props = defineProps<{
	systemId?: string
	systemName?: string
	selectedEnv?: any | null
}>()
const emit = defineEmits<{ back: []; success: [] }>()
const inputText = ref('')
const showSelector = ref(false)
const parsedResults = ref<ParsedImportResult[]>([])

const doParse = () => {
	try {
		const results = parseCurlCommand(inputText.value, props)
		parsedResults.value = results
		showSelector.value = true
	} catch (e: any) {
		message.error(`解析失败：${e.message || '未知错误'}`)
	}
}
</script>

<template>
	<div class="source-import-container">
		<div class="import-header">
			<a-button type="text" class="back-button" @click="emit('back')">
				<i class="ri-arrow-left-line mr-1"></i>
				返回
			</a-button>
		</div>
		<div class="import-content" :class="{ 'has-selector': showSelector }">
			<div v-if="!showSelector" class="import-panel">
				<div class="import-description">粘贴 cURL 命令并解析为接口</div>
				<a-textarea
					v-model:value="inputText"
					class="curl-textarea"
					:rows="12"
					placeholder="请输入 cURL 命令"
				/>
				<div class="text-actions">
					<a-button
						type="primary"
						:disabled="!inputText.trim()"
						@click="doParse"
					>
						解析
					</a-button>
				</div>
			</div>
			<ApiImportResultSelector
				v-else
				:parsed-results="parsedResults"
				@cancel="showSelector = false"
				@success="emit('success')"
			/>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.source-import-container {
	display: flex;
	flex-direction: column;
	height: 100%;
	overflow: hidden;
	background: #fff;
	.dark & {
		background: #141414;
	}

	.import-header {
		padding: 12px 16px;
		border-bottom: 1px solid #f0f0f0;
		.dark & {
			border-bottom-color: #303030;
		}
		.back-button {
			padding: 0;
		}
	}

	.import-content {
		flex: 1;
		overflow: auto;
		padding: 24px;
		display: flex;
		flex-direction: column;

		&.has-selector {
			padding: 0;
		}
	}

	.import-panel {
		width: 100%;
		max-width: 900px;
		margin: 0 auto;
		padding: 20px;
		border: 1px solid #f0f0f0;
		border-radius: 10px;
		background: #fff;
		.dark & {
			background: #1f1f1f;
			border-color: #303030;
		}
	}

	.import-description {
		margin-bottom: 16px;
		padding: 10px 12px;
		font-size: 13px;
		color: rgba(0, 0, 0, 0.65);
		background: #f0f5ff;
		border: 1px solid #adc6ff;
		border-radius: 8px;
		.dark & {
			color: rgba(255, 255, 255, 0.75);
			background: rgba(47, 84, 235, 0.16);
			border-color: rgba(47, 84, 235, 0.35);
		}
	}

	.curl-textarea {
		:deep(textarea.ant-input) {
			font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,
				'Liberation Mono', 'Courier New', monospace;
			font-size: 13px;
			line-height: 1.6;
			border-radius: 8px;
		}
	}

	.text-actions {
		margin-top: 16px;
		display: flex;
		justify-content: flex-end;
	}
}
</style>
