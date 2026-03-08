<!--
 * @FilePath: /mcp-gateway-web/src/components/show-code.vue
 * @Author: teddy
 * @Date: 2024-03-20 11:53:05
 * @Description: code 使用方法
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-29 16:42:41
-->
<script setup lang="ts">
import ClipboardJS from 'clipboard'
import hljs from 'highlight.js/lib/core'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
// theme
import 'highlight.js/styles/github.css'
import { _tmeMsg, _uuidPart } from '~/assets/js/util'
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('ts', typescript)

const props = defineProps({
	code: {
		type: String,
		default: '',
	},
	language: {
		type: String,
		default: 'xml',
	},
})

const id = ref('')

const setHtml = (code: any) =>
	hljs.highlight(code, { language: props.language }).value

const clipboard = ref<any>(null)

onMounted(() => {
	id.value = _uuidPart()
	clipboard.value = new ClipboardJS('.authorization-copy_' + id.value)
	clipboard.value.on('success', (e: any) => {
		// message.info('复制成功 ' + e.text)
		_tmeMsg.success('复制成功 ')
		// message.error('复制成功 ' + e.text)
		e.clearSelection()
	})
})
onUnmounted(() => {
	clipboard.value.destroy()
})
</script>

<template>
	<a-card title="使用方法" class="!mb-20 !mt-3">
		<pre v-html="setHtml(code)"></pre>
		<a-button
			type="primary"
			:class="'authorization-copy_' + id"
			:data-clipboard-text="code"
		>
			<i class="ri-file-copy-2-line mr-1"></i>
			复制
		</a-button>
		<div class="show-code-table">
			<slot name="message" />
		</div>
	</a-card>
</template>

<style lang="scss">
.show-code-table {
	margin-top: 10px;
	table {
		border: 1px solid #e8e8e8;
		width: 100%;
		text-align: left;
		text-indent: 20px;
		th,
		td {
			border: 1px solid #e8e8e8;
			min-width: 150px;
			padding: 6px 12px;
		}
	}
}
</style>
