<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/importTypes/openapi.vue
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: OpenAPI/Swagger 导入页面
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-23
-->
<script setup lang="tsx">
import { message } from 'ant-design-vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import { fetchProxyRequest } from '~/fetch/http'
import ApiImportResultSelector from './ApiImportResultSelector.vue'
import type { ParsedImportResult } from '../../../utils/apiImport/parsedImportTypes'
import {
	parseOpenApiContent,
	validateApiSpec,
} from '../../../utils/apiImport/sourceImportParsers/openapi'

// 接收父组件传递的业务系统信息和环境信息
const props = defineProps<{
	systemId?: string
	systemName?: string
	selectedEnv?: any | null
}>()

// 定义事件
const emit = defineEmits<{
	back: []
	success: []
}>()

// 当前激活的标签页
const activeTab = ref('url')

// 文件上传相关状态
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)

// URL 导入相关状态
const urlForm = reactive({
	url: '',
	loading: false,
})

// 解析结果选择相关状态
const showTreeSelect = ref(false)
const parsedResults = ref<ParsedImportResult[]>([])

const normalizeSingleQuotedJsonStrings = (input: string): string => {
	let result = ''
	let inDoubleQuote = false
	let inSingleQuote = false
	let escaped = false

	for (let i = 0; i < input.length; i += 1) {
		const char = input[i]
		if (escaped) {
			if (inSingleQuote && char === '"') {
				result += '\\"'
			} else {
				result += char
			}
			escaped = false
			continue
		}

		if (char === '\\') {
			result += char
			escaped = true
			continue
		}

		if (!inSingleQuote && char === '"') {
			inDoubleQuote = !inDoubleQuote
			result += char
			continue
		}

		if (!inDoubleQuote && char === "'") {
			inSingleQuote = !inSingleQuote
			result += '"'
			continue
		}

		result += char
	}

	return result
}

const parseJsonWithFallback = (text: string, sourceObj?: unknown): any => {
	if (sourceObj && typeof sourceObj === 'object') {
		return sourceObj
	}

	try {
		return JSON.parse(text)
	} catch (firstError) {
		try {
			const normalized = normalizeSingleQuotedJsonStrings(text)
			return JSON.parse(normalized)
		} catch {
			throw firstError
		}
	}
}

// 文件上传前的验证
const beforeUpload: UploadProps['beforeUpload'] = file => {
	const isJson = file.type === 'application/json' || file.name.endsWith('.json')
	const isYaml =
		file.type === 'text/yaml' ||
		file.type === 'application/x-yaml' ||
		file.name.endsWith('.yaml') ||
		file.name.endsWith('.yml')

	if (!isJson && !isYaml) {
		message.error('只支持上传 JSON 或 YAML 格式的文件！')
		return false
	}

	const isLt10M = file.size / 1024 / 1024 < 10
	if (!isLt10M) {
		message.error('文件大小不能超过 10MB！')
		return false
	}

	return true
}

// 处理文件上传
const handleUpload = async (info: any) => {
	const { file, fileList: newFileList } = info

	// 如果是上传中状态，更新文件列表
	if (file.status === 'uploading') {
		fileList.value = newFileList
		uploading.value = true
		return
	}

	// 如果是完成状态
	if (file.status === 'done') {
		uploading.value = false
		message.success('文件上传成功！')
		// TODO: 处理文件内容，解析 OpenAPI/Swagger 规范
		await processFileContent(file)
	} else if (file.status === 'error') {
		uploading.value = false
		message.error('文件上传失败！')
	}
}

// 处理文件内容
const processFileContent = async (file: UploadFile) => {
	try {
		const fileObj = file.originFileObj || file
		if (!fileObj) {
			message.error('文件对象不存在')
			return
		}

		// 将文件对象转换为 File 类型并读取内容
		const fileInstance = fileObj instanceof File ? fileObj : (fileObj as any)
		const text = await fileInstance.text()
		let apiSpec: any = null

		// 判断是 JSON 还是 YAML
		if (file.name.endsWith('.json')) {
			apiSpec = parseJsonWithFallback(text)
		} else {
			// YAML 解析需要引入 yaml 库，这里先做简单处理
			message.warning('YAML 格式解析功能待实现')
			return
		}

		// 验证 OpenAPI/Swagger 格式
		if (!validateApiSpec(apiSpec)) {
			message.error(
				'文件格式不正确，请确保是 OpenAPI 3.0、3.1 或 Swagger 2.x 格式',
			)
			return
		}

		// 解析并显示树形选择器
		handleParsedApiSpec(apiSpec)
	} catch (error: any) {
		message.error('文件解析失败：' + (error.message || '未知错误'))
	}
}

// 处理解析后的 API 规范
const handleParsedApiSpec = (spec: any) => {
	parsedResults.value = parseOpenApiContent(spec, {
		systemId: props.systemId,
		systemName: props.systemName,
		selectedEnv: props.selectedEnv,
	})
	showTreeSelect.value = true
	message.success('解析成功，请选择要导入的 API')
}

// 执行导入
// 处理 URL 导入
const handleUrlImport = async () => {
	if (!urlForm.url || !urlForm.url.trim()) {
		message.warning('请输入 URL 地址')
		return
	}

	// 验证 URL 格式
	try {
		// eslint-disable-next-line no-new
		new URL(urlForm.url)
	} catch {
		message.error('URL 格式不正确')
		return
	}

	urlForm.loading = true
	try {
		// 通过后端接口转发请求，避免跨域问题
		const response = await fetchProxyRequest({
			url: urlForm.url,
			method: 'GET',
			headers: {
				Accept: 'application/json, application/yaml, text/yaml, */*',
			},
		})

		// 代理接口现在直接返回原始响应，不再包装
		// 检查响应状态码
		if (response.status < 200 || response.status >= 300) {
			throw new Error(`请求失败，状态码: ${response.status}`)
		}

		// 获取响应数据，可能是字符串或对象
		let text: string = ''
		const responseData = response.data

		// 如果响应是字符串，直接使用
		if (typeof responseData === 'string') {
			text = responseData
		} else if (responseData !== null && responseData !== undefined) {
			// 如果是对象，转换为字符串
			text = JSON.stringify(responseData)
		}

		// 获取 Content-Type 判断文件类型
		const contentType =
			response.headers?.['content-type'] ||
			response.headers?.['Content-Type'] ||
			''

		let apiSpec: any = null

		// 判断是 JSON 还是 YAML
		if (contentType.includes('json') || urlForm.url.endsWith('.json')) {
			// JSON 格式
			try {
				apiSpec = parseJsonWithFallback(text, responseData)
			} catch (e) {
				message.error(
					'JSON 解析失败：' + (e instanceof Error ? e.message : '未知错误'),
				)
				urlForm.loading = false
				return
			}
		} else if (
			contentType.includes('yaml') ||
			contentType.includes('yml') ||
			urlForm.url.endsWith('.yaml') ||
			urlForm.url.endsWith('.yml')
		) {
			// YAML 解析需要引入 yaml 库，这里先做简单处理
			message.warning('YAML 格式解析功能待实现')
			urlForm.loading = false
			return
		} else {
			// 尝试作为 JSON 解析
			try {
				apiSpec = parseJsonWithFallback(text, responseData)
			} catch {
				message.error('无法识别文件格式，请确保 URL 指向的是 JSON 或 YAML 文件')
				urlForm.loading = false
				return
			}
		}

		// 验证 OpenAPI/Swagger 格式
		if (!validateApiSpec(apiSpec)) {
			message.error(
				'文件格式不正确，请确保是 OpenAPI 3.0、3.1 或 Swagger 2.x 格式',
			)
			urlForm.loading = false
			return
		}

		// 解析并显示树形选择器
		handleParsedApiSpec(apiSpec)
	} catch (error: any) {
		// 处理错误响应
		let errorMessage = '未知错误'
		if (error.response) {
			// 有响应但状态码不是 2xx
			errorMessage = `请求失败，状态码: ${error.response.status}`
			if (error.response.data) {
				const errorData =
					typeof error.response.data === 'string'
						? error.response.data
						: JSON.stringify(error.response.data)
				errorMessage += ` - ${errorData}`
			}
		} else if (error.message) {
			errorMessage = error.message
		}
		message.error('URL 导入失败：' + errorMessage)
	} finally {
		urlForm.loading = false
	}
}

// 自定义上传（不实际上传到服务器，只读取文件内容）
const customRequest: UploadProps['customRequest'] = async options => {
	const { file, onSuccess, onError } = options

	try {
		// 模拟上传成功，实际是读取文件内容
		await processFileContent(file as UploadFile)
		onSuccess?.(file)
	} catch (error: any) {
		onError?.(error)
	}
}
</script>

<template>
	<div class="openapi-import-container">
		<div class="import-header">
			<a-button type="text" class="back-button" @click="emit('back')">
				<i class="ri-arrow-left-line mr-1"></i>
				返回
			</a-button>
		</div>

		<div class="import-content" :class="{ 'has-tree': showTreeSelect }">
			<!-- 说明文字 -->
			<div v-if="!showTreeSelect" class="import-description">
				支持导入 OpenAPI 3.0、3.1 或 Swagger 2.0 数据格式的 JSON 或 YAML 文件
			</div>

			<!-- 标签页 -->
			<a-tabs
				v-if="!showTreeSelect"
				v-model:activeKey="activeTab"
				class="import-tabs"
			>
				<a-tab-pane key="url" tab="URL 导入">
					<div class="url-content">
						<a-form :model="urlForm" layout="vertical" class="url-form">
							<a-form-item label="URL 地址" required>
								<a-input
									v-model:value="urlForm.url"
									placeholder="请输入 OpenAPI/Swagger 文件的 URL 地址"
									size="large"
									:disabled="urlForm.loading"
									@press-enter="handleUrlImport"
								>
									<template #prefix>
										<i class="ri-link"></i>
									</template>
								</a-input>
							</a-form-item>
							<a-form-item>
								<a-button
									type="primary"
									size="large"
									:loading="urlForm.loading"
									block
									@click="handleUrlImport"
								>
									<i class="ri-download-line mr-1"></i>
									导入
								</a-button>
							</a-form-item>
						</a-form>
					</div>
				</a-tab-pane>

				<a-tab-pane key="upload" tab="上传文件">
					<div class="upload-content">
						<a-upload-dragger
							v-model:fileList="fileList"
							name="file"
							:multiple="false"
							:beforeUpload="beforeUpload"
							:customRequest="customRequest"
							accept=".json,.yaml,.yml"
							class="upload-dragger"
							@change="handleUpload"
						>
							<p class="ant-upload-drag-icon">
								<i class="ri-cloud-line"></i>
								<i class="ri-arrow-up-line upload-arrow"></i>
							</p>
							<p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
							<p class="ant-upload-hint">
								支持单个文件上传，仅支持 JSON 或 YAML 格式
							</p>
						</a-upload-dragger>
					</div>
				</a-tab-pane>
			</a-tabs>

			<ApiImportResultSelector
				v-if="showTreeSelect"
				:parsed-results="parsedResults"
				@success="emit('success')"
				@cancel="showTreeSelect = false"
			/>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.openapi-import-container {
	display: flex;
	flex-direction: column;
	height: 100%;
	overflow: hidden;

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
		display: flex;
		flex-direction: column;
		overflow: hidden;
		padding: 24px;
		max-width: 800px;
		margin: 0 auto;
		width: 100%;

		&.has-tree {
			max-width: 100%;
			padding: 0;
		}

		.import-description {
			margin-bottom: 24px;
			padding: 12px 16px;
			background-color: #f5f5f5;
			border-radius: 6px;
			font-size: 14px;
			color: rgba(0, 0, 0, 0.65);
			line-height: 1.5;
			.dark & {
				background-color: #1f1f1f;
				color: rgba(255, 255, 255, 0.65);
			}
		}

		.import-tabs {
			:deep(.ant-tabs-content-holder) {
				padding-top: 24px;
			}

			.upload-content {
				.upload-dragger {
					:deep(.ant-upload-drag) {
						background-color: #fafafa;
						border: 2px dashed #d9d9d9;
						border-radius: 8px;
						padding: 64px 24px;
						transition: all 0.3s;
						min-height: 300px;
						display: flex;
						flex-direction: column;
						align-items: center;
						justify-content: center;
						.dark & {
							background-color: #1f1f1f;
							border-color: #434343;
						}

						&:hover {
							border-color: #1890ff;
							background-color: #f0f7ff;
							.dark & {
								border-color: #1890ff;
								background-color: rgba(24, 144, 255, 0.05);
							}
						}
					}

					.ant-upload-drag-icon {
						margin-bottom: 16px;
						position: relative;
						display: inline-block;

						i {
							font-size: 64px;
							color: #1890ff;
							display: block;
						}

						.upload-arrow {
							position: absolute;
							top: 50%;
							left: 50%;
							transform: translate(-50%, -50%);
							font-size: 32px;
							margin-top: -8px;
							color: #1890ff;
						}
					}

					.ant-upload-text {
						font-size: 16px;
						color: rgba(0, 0, 0, 0.85);
						margin-bottom: 8px;
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.ant-upload-hint {
						font-size: 14px;
						color: rgba(0, 0, 0, 0.45);
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}
				}
			}

			.url-content {
				.url-form {
					max-width: 600px;
					margin: 0 auto;
				}
			}
		}

		.api-select-section {
			flex: 1;
			display: flex;
			flex-direction: column;
			overflow: hidden;
			background-color: #fff;
			.dark & {
				background-color: #141414;
			}

			.api-select-header {
				display: flex;
				align-items: center;
				justify-content: space-between;
				padding: 16px 24px;
				border-bottom: 1px solid #f0f0f0;
				.dark & {
					border-bottom-color: #303030;
				}

				.header-title {
					display: flex;
					align-items: center;
					gap: 16px;

					h3 {
						margin: 0;
						font-size: 16px;
						font-weight: 500;
						color: rgba(0, 0, 0, 0.85);
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.selected-count {
						font-size: 14px;
						color: rgba(0, 0, 0, 0.45);
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}
				}
			}

			.api-search-box {
				padding: 16px 24px;
				border-bottom: 1px solid #f0f0f0;
				.dark & {
					border-bottom-color: #303030;
				}
			}

			.api-tree-container {
				flex: 1;
				overflow-y: auto;
				padding: 16px 24px;

				.api-tree {
					:deep(.ant-tree-node-content-wrapper) {
						padding: 4px 8px;
						border-radius: 4px;

						&:hover {
							background-color: #f5f5f5;
							.dark & {
								background-color: #1f1f1f;
							}
						}
					}

					.tree-node-title {
						display: flex;
						align-items: center;
						gap: 8px;
						flex-wrap: wrap;

						.node-title-text {
							font-size: 14px;
							color: rgba(0, 0, 0, 0.85);
							.dark & {
								color: rgba(255, 255, 255, 0.85);
							}
						}

						.method-badge {
							display: inline-block;
							padding: 2px 8px;
							border-radius: 4px;
							font-size: 12px;
							font-weight: 500;
							line-height: 1.5;

							&.method-get {
								background-color: #e6f7ff;
								color: #1890ff;
								.dark & {
									background-color: rgba(24, 144, 255, 0.2);
									color: #69c0ff;
								}
							}

							&.method-post {
								background-color: #f6ffed;
								color: #52c41a;
								.dark & {
									background-color: rgba(82, 196, 26, 0.2);
									color: #95de64;
								}
							}

							&.method-put {
								background-color: #fff7e6;
								color: #fa8c16;
								.dark & {
									background-color: rgba(250, 140, 22, 0.2);
									color: #ffc069;
								}
							}

							&.method-delete {
								background-color: #fff1f0;
								color: #f5222d;
								.dark & {
									background-color: rgba(245, 34, 45, 0.2);
									color: #ff7875;
								}
							}

							&.method-patch {
								background-color: #f9f0ff;
								color: #722ed1;
								.dark & {
									background-color: rgba(114, 46, 209, 0.2);
									color: #b37feb;
								}
							}
						}

						.node-summary {
							font-size: 12px;
							color: rgba(0, 0, 0, 0.45);
							.dark & {
								color: rgba(255, 255, 255, 0.45);
							}
						}
					}

					.tree-node-description {
						margin-top: 4px;
						font-size: 12px;
						color: rgba(0, 0, 0, 0.45);
						line-height: 1.5;
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}
				}
			}

			.api-select-footer {
				padding: 16px 24px;
				border-top: 1px solid #f0f0f0;
				.dark & {
					border-top-color: #303030;
				}
			}
		}
	}
}
</style>
