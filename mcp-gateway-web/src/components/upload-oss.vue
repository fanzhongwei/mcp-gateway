<!--
 * @FilePath: /mcp-gateway-web/src/components/upload-oss.vue
 * @Author: teddy
 * @Date: 2024-03-18 16:02:14
 * @Description: 阿里云 OSS
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-21 16:22:06
-->
<script setup lang="ts">
import { message } from 'ant-design-vue'
import { _tmeMsg, _uuid } from '~/assets/js/util'
import { fetchAliyunOssConfig } from '~/fetch/http'
const props = defineProps({
	// 在图片 url 地址中使用固定的值拼接
	code: {
		type: String,
		default: '',
	},
})
const route: any = useRoute()
const fileKey = defineModel('fileKey', {
	type: String,
	default: '',
})

const loading = ref(false)
const uploadInput = ref()
const handleUpload = async (fileData: any) => {
	try {
		const file = fileData.target.files[0]
		// 限制的格式范围
		const txtName = '.jpg|.png|.jpeg'
		// 校验-把路径中的所有字母全部转换为小写
		const extName = file.name
			.substring(file.name.lastIndexOf('.'))
			.toLowerCase()
		const extSize = file.size
		// 判断格式
		if (!txtName.includes(extName)) {
			_tmeMsg.error('请上传正确的文件格式！如：' + txtName)
			uploadInput.value.value = null
			return false
		}
		// 判断大小
		if (extSize / 1000 / 1000 > 10) {
			_tmeMsg.error('请上传小于 10M 的图片！')
			uploadInput.value.value = null
			return false
		}
		if (!file) return false
		// 开始上传
		loading.value = true
		message.loading({
			content: `上传中...`,
			key: 'upload',
			duration: 0,
		})
		const nameList = file.name.split('.')
		const { data } = await fetchAliyunOssConfig()
		const formData = new FormData()
		const key =
			data.dir +
			route.name +
			(props.code || _uuid()) +
			'.' +
			nameList[nameList.length - 1]
		formData.append('key', key)
		formData.append('OSSAccessKeyId', data.accessid)
		formData.append('policy', data.policy)
		formData.append('signature', data.signature)
		formData.append('success_action_status', '200')
		formData.append('file', file)

		await fetch('/aliyunOSS', {
			method: 'post',
			headers: {
				'x-oss-object-acl': 'public-read',
			},
			body: formData,
		})
		_tmeMsg.success('上传成功', 'upload')
		fileKey.value = data.host + '/' + key
		loading.value = false
	} catch (error) {
		_tmeMsg.error('上传失败', 'upload')
		loading.value = false
	}
}
</script>

<template>
	<div class="flex h-8 gap-1 items-center">
		<a-spin tip="上传中..." :spinning="loading">
			<a-image
				v-if="fileKey"
				:width="100"
				:height="32"
				class="object-contain"
				:src="fileKey"
			/>
		</a-spin>
		<label v-show="!loading" for="uploadOSS" class="text-tme cursor-pointer">
			{{ fileKey ? '重新上传' : '上传图片' }}
			<input
				id="uploadOSS"
				ref="uploadInput"
				class="!hidden"
				type="file"
				accept="image/png, image/jpeg, image/jpg"
				@change="handleUpload"
			/>
		</label>
	</div>
</template>

<style lang="scss"></style>
