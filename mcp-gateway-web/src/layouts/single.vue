<!--
 * @FilePath: /mcp-gateway-web/src/layouts/single.vue
 * @Author: teddy
 * @Date: 2024-02-12 11:02:30
 * @Description: 单页面布局-使用登录页等页面（主要是统一样式和主题）
 * @LastEditors: teddy
 * @LastEditTime: 2024-05-06 10:45:10
-->
<script setup lang="ts">
import { useDark, useStorage } from '@vueuse/core'
import { theme } from 'ant-design-vue'
// 主题切换-深色-浅色
const isDark = useDark()

// 主题
const aliasToken = reactive<any>({
	colorPrimary: '#409EFF',
	colorError: '#F56C6C',
	borderRadius: 3,
})
// 系统主题值
const vueuseXolorScheme = useStorage<any>('vueuse-color-scheme', null)

onMounted(() => {
	if (vueuseXolorScheme.value === 'auto') {
		isDark.value = false
	}
})
</script>

<template>
	<client-only>
		<a-style-provider hash-priority="high">
			<a-config-provider
				:theme="{
					algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
					token: aliasToken,
				}"
			>
				<slot />
			</a-config-provider>
		</a-style-provider>
	</client-only>
</template>

<style lang="scss"></style>
