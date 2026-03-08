/*
 * @FilePath: /mcp-gateway-web/src/plugins/plugins.js
 * @Author: teddy
 * @Date: 2024-02-02 14:57:17
 * @Description: 插件
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-15 17:29:24
 */

import { useStorage } from '@vueuse/core'
import Antd from 'ant-design-vue'

export default defineNuxtPlugin(nuxtApp => {
	// const route = useRoute() // 路由
	// const tmeMenuState = useState('tme-menu-state') // 有效菜单数据
	// 用户登录历史信息
	const userInfo = useStorage('pay-user-info')
	nuxtApp.vueApp.use(Antd)

	// 自定义指令，根据按钮权限控制按钮显示与隐藏
	// https://cn.vuejs.org/guide/reusability/custom-directives.html
	nuxtApp.vueApp.directive('permission', {
		mounted: (_el, data) => {
			try {
				if (userInfo.value) {
					try {
						const rightList = JSON.parse(userInfo.value).rightList
						if (
							!rightList.includes(data.value) &&
							!rightList.includes('system.superAdmin')
						) {
							_el.remove()
						}
					} catch (error) {
						_el.remove()
					}
				}
			} catch (error) {}
		},
	})
})
