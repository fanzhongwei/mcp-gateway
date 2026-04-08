<script setup lang="ts">
import { message } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import ApiImportResultSelector from './ApiImportResultSelector.vue'
import type { ParsedImportResult } from '../../../utils/apiImport/parsedImportTypes'
import { parseApifoxContent } from '../../../utils/apiImport/sourceImportParsers/apifox'

const props = defineProps<{
	systemId?: string
	systemName?: string
	selectedEnv?: any | null
}>()
const emit = defineEmits<{ back: []; success: [] }>()
const showSelector = ref(false)
const parsedResults = ref<ParsedImportResult[]>([])
const fileList = ref<UploadFile[]>([])

const parsing = ref(false)

const beforeUpload: UploadProps['beforeUpload'] = file => {
	const isJson = file.type === 'application/json' || file.name.endsWith('.json')
	if (!isJson) {
		message.warning('仅支持 JSON 文件')
	}
	return false
}

const handleParse = async () => {
	const currentFile = fileList.value[0]?.originFileObj as File | undefined
	if (!currentFile) {
		message.warning('请先上传 JSON 文件')
		return
	}

	try {
		parsing.value = true
		const content = await currentFile.text()
		const results = parseApifoxContent(content, props)
		parsedResults.value = results
		showSelector.value = true
		message.success(`解析成功，共 ${results.length} 个接口`)
	} catch (e: any) {
		message.error(`解析失败：${e.message || '未知错误'}`)
	} finally {
		parsing.value = false
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
				<div class="import-description">
					上传 Apifox 导出 JSON 文件，解析后可勾选接口导入
				</div>
				<a-upload-dragger
					v-model:fileList="fileList"
					class="upload-dragger"
					:before-upload="beforeUpload"
					:max-count="1"
					accept=".json"
				>
					<p class="ant-upload-drag-icon"><i class="ri-cloud-line"></i></p>
					<p class="ant-upload-text">点击或拖拽 JSON 文件到此处上传</p>
				</a-upload-dragger>
				<div class="text-actions">
					<a-button
						type="primary"
						:loading="parsing"
						:disabled="!fileList.length"
						@click="handleParse"
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
		overflow-x: hidden;
		padding: 24px;
		display: flex;
		flex-direction: column;
		align-items: center;

		&.has-selector {
			padding: 0;
			align-items: stretch;
		}
	}

	.import-panel {
		width: min(840px, 100%);
		max-width: 840px;
		padding: 20px;
		box-sizing: border-box;
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
		background: #fff7e6;
		border: 1px solid #ffe7ba;
		border-radius: 8px;
		.dark & {
			color: rgba(255, 255, 255, 0.75);
			background: rgba(250, 173, 20, 0.14);
			border-color: rgba(250, 173, 20, 0.35);
		}
	}

	.upload-dragger {
		display: block;
		width: 100%;
		box-sizing: border-box;

		:deep(.ant-upload-wrapper) {
			display: block;
			width: 100%;
			box-sizing: border-box;
		}
		:deep(.ant-upload) {
			width: 100%;
			box-sizing: border-box;
		}
		:deep(.ant-upload-drag) {
			width: 100%;
			box-sizing: border-box;
			min-height: 240px;
			border-radius: 8px;
			border: 1px dashed #ffbb96;
			background: #fffbf5;
			display: flex;
			flex-direction: column;
			justify-content: center;
			transition: all 0.2s;
			.dark & {
				background: #1a1a1a;
				border-color: #8c5f3f;
			}
		}
		:deep(.ant-upload-drag:hover) {
			border-color: #fa8c16;
			background: #fff7e6;
			.dark & {
				background: rgba(250, 140, 22, 0.1);
			}
		}
		:deep(.ant-upload-drag-icon i) {
			font-size: 44px;
			color: #fa8c16;
		}
		:deep(.ant-upload-text) {
			color: rgba(0, 0, 0, 0.85);
			.dark & {
				color: rgba(255, 255, 255, 0.85);
			}
		}
	}

	.text-actions {
		margin-top: 16px;
		display: flex;
		width: 100%;
		justify-content: flex-end;
	}
}
</style>
