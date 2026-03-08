/**
 * 菜单数据-唯一数据源，layouts/default.vue、登录首屏跳转等均从此引入
 */
export const MENU_DATA_SOURCE = [
	{
		key: '/businessSystem',
		icon: "<i class='ri-computer-line'></i>",
		permissions: 'system.businessSystem',
		label: '业务系统',
	},
	{
		key: '/apiManage',
		icon: "<i class='ri-links-line'></i>",
		permissions: 'system.apiManage',
		label: '接口管理',
	},
	{
		key: '/mcpService',
		icon: "<i class='ri-server-line'></i>",
		permissions: 'system.mcpService',
		label: 'MCP服务管理',
	},
	{
		key: '/system',
		icon: "<i class='ri-settings-line'></i>",
		permissions: 'system.system',
		label: '系统设置',
		children: [
			{
				key: '/system/organ',
				icon: "<i class='ri-folder-user-line'></i>",
				permissions: 'system.system.user',
				label: '租户管理',
			},
			{
				key: '/system/role',
				icon: "<i class='ri-shield-user-line'></i>",
				permissions: 'system.system.role',
				label: '角色列表',
			},
			{
				key: '/system/permissions',
				icon: "<i class='ri-keyboard-line'></i>",
				permissions: 'system.system.permissions',
				label: '权限点管理',
			},
			{
				key: '/system/authorization',
				icon: '<i class="ri-copyright-line"></i>',
				permissions: 'system.system.authorization',
				label: '对外授权管理',
			},
			{
				key: '/system/log',
				icon: "<i class='ri-menu-line'></i>",
				permissions: 'system.system.log',
				label: '日志',
			},
			{
				key: '/system/develop',
				icon: "<i class='ri-menu-line'></i>",
				permissions: 'system.system.develop',
				label: '开发工具',
				children: [
					{
						key: '/system/develop/cache',
						permissions: 'system.system.develop',
						label: '缓存清理',
					},
					{
						key: '/system/develop/select',
						permissions: 'system.system.develop',
						label: '码值管理',
					},
					{
						key: '/system/develop/tree',
						permissions: 'system.system.develop',
						label: 'tree 数据',
					},
					{
						key: '/system/develop/table',
						permissions: 'system.system.develop',
						label: 'table 注意事项',
					},
					{
						key: '/system/develop/upload-img',
						permissions: 'system.system.develop',
						label: '图片上传组件',
					},
					{
						key: '/system/develop/date-picker',
						permissions: 'system.system.develop',
						label: '日期区间组件',
					},
				],
			},
		],
	},
]

/**
 * 根据路由 path 从菜单中解析所需权限（精确匹配或最长前缀匹配）
 * @param {Array} menu - 菜单配置（含 key、permissions、children）
 * @param {string} path - 当前访问路径，如 /system/organ
 * @returns {{ permission: string, label?: string }|null} 所需权限信息，非菜单内 path 返回 null
 */
export function getPermissionForPath(menu, path) {
	if (!menu || !path) return null
	const normalized = path.replace(/\/$/, '') || '/'
	const flat = []
	function collect(items) {
		for (const item of items) {
			flat.push({
				path: item.key,
				permission: item.permissions,
				label: item.label,
			})
			if (item.children?.length) collect(item.children)
		}
	}
	collect(menu)
	// 精确匹配
	const exact = flat.find(
		p => (p.path.replace(/\/$/, '') || '/') === normalized,
	)
	if (exact) return { permission: exact.permission, label: exact.label }
	// 最长前缀匹配（子路由归属到父级菜单权限）
	const withPrefix = flat
		.filter(p => {
			const pPath = p.path.replace(/\/$/, '') || '/'
			return normalized !== pPath && (normalized + '/').startsWith(pPath + '/')
		})
		.sort((a, b) => b.path.length - a.path.length)
	if (withPrefix.length)
		return { permission: withPrefix[0].permission, label: withPrefix[0].label }
	return null
}

/**
 * 根据用户权限列表，返回有权限的第一个菜单 path（深度优先）
 * @param {Array} menu - 菜单配置（含 key、permissions、children）
 * @param {Array|Set} rightList - 用户权限点列表
 * @returns {string|null} 第一个有权限的 path，无则返回 null
 */
export function getFirstPermittedPath(menu, rightList) {
	if (!menu || !rightList || !rightList.length) return null
	const list = Array.isArray(rightList) ? rightList : [...rightList]
	const has = p => list.includes(p) || list.includes('system.superAdmin')

	function find(items) {
		for (const item of items) {
			if (!has(item.permissions)) continue
			if (!item.children || !item.children.length) return item.key
			const inChild = find(item.children)
			if (inChild) return inChild
			// 有父级权限但子项都无权限时，进入父级 path
			return item.key
		}
		return null
	}

	return find(menu) || null
}
