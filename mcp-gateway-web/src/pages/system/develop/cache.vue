<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/develop/cache.vue
 * @Author: teddy
 * @Date: 2024-03-07 14:06:31
 * @Description: 开发工具-缓存清理
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-09 10:26:01
-->
<script setup lang="ts">
import { _tmeMsg } from '~/assets/js/util'
import {
	fetchCacheClearCode,
	fetchCacheClearDwjkAccess,
	fetchCacheClearOrgan,
} from '~/fetch/http'
const cashe = reactive({
	codeLoading: false,
	dwjkAccessLoading: false,
	organLoading: false,
	areaLoading: false,
	cardTypeLoading: false,
})
// 刷新码值缓存
const deleteCacheClearCode = async () => {
	try {
		cashe.codeLoading = true
		await fetchCacheClearCode()
		cashe.codeLoading = false
		_tmeMsg.success('刷新码值缓存-成功')
	} catch (error) {
		cashe.codeLoading = false
	}
}

// 刷新对外接口授权缓存
const deleteCacheClearDwjkAccess = async () => {
	try {
		cashe.dwjkAccessLoading = true
		await fetchCacheClearDwjkAccess()
		cashe.dwjkAccessLoading = false
		_tmeMsg.success('刷新对外接口授权缓存-成功')
	} catch (error) {
		cashe.dwjkAccessLoading = false
	}
}

// 刷新租户缓存
const deleteCacheClearOrgan = async () => {
	try {
		cashe.organLoading = true
		await fetchCacheClearOrgan()
		cashe.organLoading = false
		_tmeMsg.success('刷新租户缓存-成功')
	} catch (error) {
		cashe.organLoading = false
	}
}
</script>

<template>
	<div>
		<a-button
			class="!block mb-2"
			type="primary"
			:loading="cashe.codeLoading"
			@click="deleteCacheClearCode"
		>
			<i class="ri-delete-bin-line mr-1"></i>
			刷新码值缓存
		</a-button>
		<a-button
			class="!block mb-2"
			type="primary"
			:loading="cashe.dwjkAccessLoading"
			@click="deleteCacheClearDwjkAccess"
		>
			<i class="ri-delete-bin-line mr-1"></i>
			刷新对外接口授权缓存
		</a-button>
		<a-button
			class="!block mb-2"
			type="primary"
			:loading="cashe.organLoading"
			@click="deleteCacheClearOrgan"
		>
			<i class="ri-delete-bin-line mr-1"></i>
			刷新租户缓存
		</a-button>
	</div>
</template>

<style lang="scss"></style>
