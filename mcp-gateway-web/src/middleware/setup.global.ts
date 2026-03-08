/*
 * @FilePath: /mcp-gateway-web/src/middleware/setup.global.ts
 * @Author: teddy
 * @Date: 2024-02-08 16:45:27
 * @Description: 全局路由拦截
 * @LastEditors: teddy
 * @LastEditTime: 2024-04-19 09:24:17
 */
import {
	getFirstPermittedPath,
	getPermissionForPath,
	MENU_DATA_SOURCE,
} from '~/config/menu'

export default defineNuxtRouteMiddleware((to, _from) => {
	// navigateTo - 重定向到给定的路由
	// abortNavigation - 中止导航，并可选择提供错误消息
	// 在实际应用中，你可能不会将每个路由重定向到 `/`
	// 但是在重定向之前检查 `to.path` 是很重要的，否则可能会导致无限重定向循环

	// token
	const payToken = useCookie('pay-token')

	if (payToken.value) {
		// 已登录-访问登录页-跳转主页（会再被 index 逻辑重定向到有权限的首页）
		if (to.name === 'login') {
			return navigateTo({
				name: 'index',
				query: to.query,
			})
		}
		// 根路径：跳转到当前用户有权限的第一个页面
		if (to.name === 'index') {
			let firstPath = ''
			if (import.meta.client && typeof localStorage !== 'undefined') {
				try {
					const raw = localStorage.getItem('pay-user-info')
					const userInfo = raw ? JSON.parse(raw) : {}
					const rightList = userInfo?.rightList || []
					firstPath = getFirstPermittedPath(MENU_DATA_SOURCE, rightList) || ''
				} catch (_) {}
			}
			return navigateTo(
				{ path: firstPath || '/businessSystem', query: to.query },
				{ replace: true },
			)
		}
		// 已登录-访问其它路由：校验是否有该页面权限，无权限则重定向并带 noPermission 提示
		if (import.meta.client && typeof localStorage !== 'undefined') {
			try {
				const path = to.path.replace(/\/$/, '') || '/'
				const required = getPermissionForPath(MENU_DATA_SOURCE, path)
				if (required) {
					const raw = localStorage.getItem('pay-user-info')
					const userInfo = raw ? JSON.parse(raw) : {}
					const rightList = userInfo?.rightList || []
					const has =
						Array.isArray(rightList) &&
						(rightList.includes(required.permission) ||
							rightList.includes('system.superAdmin'))
					if (!has) {
						const firstPath =
							getFirstPermittedPath(MENU_DATA_SOURCE, rightList) ||
							'/businessSystem'
						return navigateTo(
							{ path: firstPath, query: { ...to.query, noPermission: path } },
							{ replace: true },
						)
					}
				}
			} catch (_) {}
		}
	} else if (to.name !== 'login') {
		// 未登录-跳转登录页
		const payOldRouteName401 = useCookie<any>('pay-old-route-name-401')
		payOldRouteName401.value = to.name
		return navigateTo({
			name: 'login',
			query: to.query,
		})
	}

	// // 已登录-访问登录页-跳转主页
	// if (payToken.value && to.name === 'login') {

	// } else {
	// 	// // 当前 path
	// 	// const path = to.path.replace(/\/$/, '') || '/'
	// 	// if (!userInfo.value.rightRoute.includes(path) && to.path !== '/') {
	// 	// 	return navigateTo('/')
	// 	// }
	// }
})
