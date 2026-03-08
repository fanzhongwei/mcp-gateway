<!--
 * @FilePath: /mcp-gateway-web/src/layouts/default.vue
 * @Author: teddy
 * @Date: 2024-02-02 15:08:01
 * @Description: 默认布局
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-09 14:17:22
-->
<script setup lang="tsx">
import { _toggleFullScreen } from '@/assets/js/util'
import { useDark, useStorage } from '@vueuse/core'
import { message, theme } from 'ant-design-vue'
import { fetchLogout, fetchUserInfo, fetchUserPermissions } from '~/fetch/http'
// 默认语言为 en-US，如果你需要设置其他语言，推荐在入口文件全局设置 locale
// import locale from 'ant-design-vue/es/date-picker/locale/zh_CN'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

// 菜单数据-从 config/menu 引入，与登录首屏跳转等共用同一数据源
import { MENU_DATA_SOURCE } from '~/config/menu'
// eslint-disable-next-line import/no-named-as-default-member
dayjs.locale('zh-cn')

// route
const route = useRoute()
// 全屏
const fullscreen = useState('fullscreen', () => !!document.fullscreenElement)
// token
const payToken = useCookie('pay-token')

const isDevelop = process.env.NODE_ENV === 'development'

// 系统主题值
const vueuseXolorScheme = useStorage<any>('vueuse-color-scheme', null)
// 用户登录历史信息
const userInfo = useStorage<any>('pay-user-info', {
	username: '',
	password: '',
	name: '',
	id: '',
	// 记住密码
	remember: false,
	// tabs.map
	tabsMap: {},
	// tabs.pane
	tabsPane: [],
	// 权限点
	rightList: [],
})

// 主题切换-深色-浅色
const isDark = useDark()

// 主题
const aliasToken = reactive<any>({
	colorPrimary: '#409EFF',
	colorError: '#F56C6C',
	borderRadius: 3,
	// 用于派生文本色梯度的基础变量，v5 中我们添加了一层文本色的派生算法可以产出梯度明确的文本色的梯度变量。但**请不要在代码中直接使用该 Seed Token**！
	colorTextBase: '#333',
	fontFamily:
		"'Helvetica Neue', 'Helvetica', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', 'Arial', 'sans-serif', 'blacktitle',",
})

// 退出登录
const handleLogout = async () => {
	try {
		await fetchLogout()
		payToken.value = null
		navigateTo('/login')
	} catch (error) {}
}

const menuData = reactive(MENU_DATA_SOURCE)

// menu-菜单选择数据
const menu = useStorage<any>('pay-menu', {
	// 菜单是否折叠
	collapsed: false,
	key: ['/'],
	keyPath: ['/'],
	breadcrumb: ['首页'],
})

//  menu-菜单选择
const handleSelect = (val: any) => {
	menu.value.key = [val.key]
	navigateTo(val.key)
	setBreadcrumb(toRaw(menuData), val.key)
}

// breadcrumb-设置面包屑
const setBreadcrumb = (data: any[], key: string) => {
	menu.value.breadcrumb = []
	menu.value.keyPath = []
	// 定义面包屑变量
	let breadcrumb: string[] = []
	// 定义路径变量
	let keyPath: string[] = []
	const findIds = (
		arr: any[],
		breadcrumbTemp: string[] = [],
		keyPathTemp: string[] = [],
	) => {
		for (const node of arr) {
			if (node.key === key) {
				breadcrumb = [...breadcrumbTemp, node.label]
				keyPath = [...keyPathTemp, node.key]
				return
			}
			if (node.children) {
				findIds(
					node.children,
					[...breadcrumbTemp, node.label],
					[...keyPathTemp, node.key],
				)
			}
		}
	}
	findIds(data)
	menu.value.breadcrumb = breadcrumb
	menu.value.keyPath = keyPath
}

// drawer-个人设置抽屉
const drawerFormRef = ref()
const drawer = reactive({
	open: false,
	loading: false,
	password: '',
	affirmPassword: '',
})
// drawer-抽屉 form 验证
const drawerRulesRef = reactive<any>({
	password: [
		{
			validator: (_: any, value: string) => {
				if (drawer.affirmPassword.length && !value.length) {
					return Promise.reject(new Error('请输入密码'))
				}
				return Promise.resolve()
			},
		},
	],
	affirmPassword: [
		{
			validator: (_: any, value: string) => {
				if (drawer.password.length && !value.length) {
					return Promise.reject(new Error('请输入确认密码'))
				}
				if (drawer.password !== value) {
					return Promise.reject(new Error('两次输入的密码不一致'))
				}
				return Promise.resolve()
			},
		},
	],
})

// drawer-个人设置提交
const handleDrawerFinish = () => {
	// TODO 修改密码 Ajax
	console.log('submit')
}

// tabs-data
const tabs = reactive<any>({
	activeKey: '',
	pane: [],
	map: {},
})
// tabs-关闭当前页
const handleTabsClose = (activeKey: string) => {
	// tabs-删除选中的 activeKey tabs
	delete tabs.map[activeKey]
	// tabs-重新复制 tabs 数据
	tabs.pane = []
	Object.keys(tabs.map).forEach(key => {
		tabs.pane.push(tabs.map[key])
	})

	// tabs-当 tabs 只有一个时，默认选中第一个
	if (tabs.pane.length === 1) {
		tabs.activeKey = tabs.pane[0].key
		navigateTo({
			name: tabs.pane[0].key,
		})
	} else if (activeKey === tabs.activeKey) {
		// tabs-当 tabs 关闭的是激活的时，打开最后一个 tabs
		const lastKey = tabs.pane.length - 1
		tabs.activeKey = tabs.pane[lastKey].key
		navigateTo({
			name: tabs.pane[lastKey].key,
		})
	}
	// tabs-存储 tabsMap 数据到本地
	userInfo.value.tabsMap = toRaw(tabs.map)
	// tabs-存储 tabsPane 数据到本地
	userInfo.value.tabsPane = toRaw(tabs.pane)
}
// tabs-change
const handleTabsChange = (activeKey: string) => {
	navigateTo({
		name: activeKey,
	})
}
// tabs-设置数据
const setTabs = (routeName: string, path: string) => {
	// 生成 tabs 数据
	// 读取本地 tabsMap 数据
	tabs.map = userInfo.value.tabsMap
	// 读取本地 tabsPane 数据
	tabs.pane = userInfo.value.tabsPane
	const getTabTitle = (data: any) => {
		data.map((item: any) => {
			if (item.key === path) {
				tabs.map[routeName] = {
					title: item.label,
					key: routeName,
				}
			} else if (item.children) {
				getTabTitle(item.children)
			}
		})
	}
	// 生成 tabs 数据
	getTabTitle(menuData)
	// 设置 tabs activeKey
	tabs.activeKey = routeName
	tabs.pane = []
	Object.keys(tabs.map).forEach(key => {
		tabs.pane.push(tabs.map[key])
	})
	// 存储 tabsMap 数据到本地
	userInfo.value.tabsMap = toRaw(tabs.map)
	// 存储 tabsPane 数据到本地
	userInfo.value.tabsPane = toRaw(tabs.pane)
}
// tabs-关闭其他页
const handleTabsCloseRests = (data: any) => {
	tabs.map = {}
	tabs.map[data.key] = data
	tabs.pane = [data]
	tabs.activeKey = data.key
	// tabs-存储 tabsMap 数据到本地
	userInfo.value.tabsMap = toRaw(tabs.map)
	// tabs-存储 tabsPane 数据到本地
	userInfo.value.tabsPane = toRaw(tabs.pane)
	navigateTo({
		name: data.key,
	})
}

watch(
	() => drawer.open,
	val => {
		if (!val) {
			drawerFormRef.value.resetFields()
		}
	},
)

// 无权限跳转时的提示（中间件重定向会带上 noPermission query）
watch(
	() => route.query.noPermission,
	val => {
		if (val) {
			message.warning('您没有该页面的访问权限')
			const q = { ...route.query }
			delete q.noPermission
			navigateTo(
				{ path: route.path, query: Object.keys(q).length ? q : undefined },
				{ replace: true },
			)
		}
	},
	{ immediate: true },
)

// 监听页面加载时，菜单高亮
watch(
	() => route.path,
	val => {
		// 当前路由 path
		const path = val.replace(/\/$/, '') || '/'
		// 没有权限，跳转首页
		// if (!userInfo.value.rightRoute.includes(path) && path !== '/login') {
		// 	message.destroy()
		// 	message.error('您没有权限访问该页面，已跳转至首页！')
		// 	navigateTo('/')
		// } else {
		// 当前路由 name
		const routeName: any = route.name
		// menu-高亮菜单
		menu.value.key = [path]
		// 生成面包屑数据
		setBreadcrumb(toRaw(menuData), path)
		// tabs-设置数据
		setTabs(routeName, path)
		// }
	},
	{ immediate: true },
)

// 获取用户信息
const getUserInfo = async () => {
	try {
		const { data } = await fetchUserInfo(userInfo.value.id)
		userInfo.value.name = data.name
		userInfo.value.id = data.id
		getUserPermissions(data.id)
	} catch (error) {}
}

// 权限-获取用户权限点
const getUserPermissions = async (id: string) => {
	try {
		const { data } = await fetchUserPermissions(id)
		userInfo.value.rightList = data
	} catch (error) {}
}

// Table 数据渲染前可以再次改变，一般用户空数据的默认配置
const transformCellText = ({ text }: any) => {
	return text || '-'
}

// 版本
const runtimeConfig = useRuntimeConfig()
const version = useCookie('version')
onMounted(() => {
	getUserInfo()
	version.value = runtimeConfig.public.version
	if (vueuseXolorScheme.value === 'auto') {
		isDark.value = false
	}
})
</script>

<template>
	<client-only>
		<a-style-provider hash-priority="high">
			<a-config-provider
				:locale="zhCN"
				:transformCellText="transformCellText"
				:theme="{
					algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
					token: aliasToken,
				}"
			>
				<header class="h-16 !p-0 !bg-white dark:!bg-dark flex items-center">
					<div
						class="header-logo-text text-dark/90 dark:text-white/90 zrlziti text-center text-2xl"
						:class="{
							'w-52': !menu.collapsed,
							'w-20': menu.collapsed,
						}"
					>
						{{ menu.collapsed ? 'MCP' : 'MCP Gateway' }}
					</div>
					<!-- 面包屑 -->
					<div class="grow flex items-center gap-2">
						<a-button
							type="text"
							size="large"
							@click="menu.collapsed = !menu.collapsed"
						>
							<template #icon>
								<i v-if="menu.collapsed" class="ri-indent-increase"></i>
								<i v-else class="ri-indent-decrease"></i>
							</template>
						</a-button>
						<i class="ri-map-pin-line ml-3 text-dark/90 dark:text-white/90"></i>
						<a-breadcrumb class="select-none">
							<a-breadcrumb-item
								v-for="(item, index) in menu.breadcrumb"
								:key="index"
							>
								{{ item }}
							</a-breadcrumb-item>
						</a-breadcrumb>
					</div>

					<div class="flex gap-2 items-center pr-3">
						<a-popover placement="bottom">
							<template #content>
								<div>{{ fullscreen ? '退出全屏' : '全屏' }}</div>
							</template>
							<a-button type="text" size="large" @click="_toggleFullScreen">
								<template #icon>
									<i v-if="fullscreen" class="ri-fullscreen-exit-line"></i>
									<i v-else class="ri-fullscreen-line"></i>
								</template>
							</a-button>
						</a-popover>
						<a-popover placement="bottom">
							<template #content>
								<div>{{ isDark ? '浅色' : '深色' }}</div>
							</template>
							<a-button type="text" size="large" @click="isDark = !isDark">
								<template #icon>
									<i v-if="isDark" class="ri-sun-line"></i>
									<i v-else class="ri-moon-line"></i>
								</template>
							</a-button>
						</a-popover>
						<a-dropdown>
							<div
								class="cursor-pointer text-black/90 dark:text-white/90 select-none"
							>
								<span class="font-bold text-sm">{{ userInfo.name }}</span>
								<i class="ri-arrow-down-s-line"></i>
							</div>
							<template #overlay>
								<a-menu class="select-none">
									<!-- <a-menu-item @click="drawer.open = true">
										个人设置
									</a-menu-item> -->
									<a-menu-item @click="handleLogout">退出登录</a-menu-item>
								</a-menu>
							</template>
						</a-dropdown>
					</div>
				</header>
				<div class="flex">
					<nav
						class="transition-all"
						:class="{
							'w-52': !menu.collapsed,
							'w-20': menu.collapsed,
						}"
					>
						<!-- menu 左侧菜单 -->
						<a-menu
							v-model:openKeys="menu.keyPath"
							v-model:selectedKeys="menu.key"
							class="h-full !border-0 select-none default-menu"
							mode="inline"
							:inline-collapsed="menu.collapsed"
							@select="handleSelect"
						>
							<template v-for="item in menuData" :key="item.key">
								<!-- 一级菜单 -->
								<div v-if="!item.children">
									<a-menu-item
										v-if="
											userInfo.rightList.includes(item.permissions) ||
											userInfo.rightList.includes('system.superAdmin')
										"
										:key="item.key"
									>
										<template #icon>
											<span v-html="item.icon"></span>
										</template>
										{{ item.label }}
									</a-menu-item>
								</div>
								<!-- 二级菜单 -->
								<div v-else>
									<a-sub-menu
										v-if="
											userInfo.rightList.includes(item.permissions) ||
											userInfo.rightList.includes('system.superAdmin')
										"
										:key="item.key"
									>
										<template #icon>
											<span v-html="item.icon"></span>
										</template>
										<template #title>{{ item.label }}</template>
										<template
											v-for="children in item.children"
											:key="children.key"
										>
											<!-- 二级菜单-单个 -->
											<div v-if="!children.children">
												<a-menu-item
													v-if="
														userInfo.rightList.includes(children.permissions) ||
														userInfo.rightList.includes('system.superAdmin')
													"
													:key="children.key"
												>
													<template #icon>
														<span v-html="children.icon"></span>
													</template>
													{{ children.label }}
												</a-menu-item>
											</div>
											<!-- 二级菜单-组 -->
											<div v-else>
												<!-- 三级菜单 -->
												<a-sub-menu
													v-if="
														userInfo.rightList.includes(children.permissions) ||
														userInfo.rightList.includes('system.superAdmin')
													"
													:key="children.key"
												>
													<template #icon>
														<span v-html="children.icon"></span>
													</template>
													<template #title>{{ children.label }}</template>
													<template v-for="son in children.children">
														<a-menu-item
															v-if="
																userInfo.rightList.includes(son.permissions) ||
																userInfo.rightList.includes('system.superAdmin')
															"
															:key="son.key"
														>
															{{ son.label }}
														</a-menu-item>
													</template>
												</a-sub-menu>
											</div>
										</template>
									</a-sub-menu>
								</div>
							</template>
						</a-menu>
					</nav>
					<a-layout class="p-3 pb-0 !flex-1 overflow-auto">
						<a-tabs
							v-model:activeKey="tabs.activeKey"
							class="layouts-default-tabs select-none"
							hide-add
							size="small"
							type="editable-card"
							@edit="handleTabsClose"
							@change="handleTabsChange"
						>
							<a-tab-pane
								v-for="pane in tabs.pane"
								:key="pane.key"
								class="!m-0"
								:closable="tabs.pane.length === 1 ? false : true"
							>
								<template #tab>
									<a-dropdown :trigger="['contextmenu']">
										<div>{{ pane.title }}</div>
										<template #overlay>
											<a-menu class="select-none">
												<a-menu-item
													key="delete"
													:disabled="tabs.pane.length === 1"
													@click="handleTabsCloseRests(pane)"
												>
													关闭其他页面
												</a-menu-item>
												<a-menu-item
													key="edit"
													:disabled="tabs.pane.length === 1"
													@click="handleTabsClose(pane.key)"
												>
													关闭当前页面
												</a-menu-item>
											</a-menu>
										</template>
									</a-dropdown>
								</template>
							</a-tab-pane>
						</a-tabs>
						<div
							class="layout-content bg-white dark:bg-dark p-3 text-black/90 dark:text-white/90"
						>
							<!-- 页面主要内容 -->
							<div class="layouts-slot">
								<slot />
							</div>
						</div>
						<footer
							class="h-8 leading-8 text-center text-dark/30 dark:text-white/30 select-none"
						>
							{{ isDevelop ? 'develop' : 'MCP Gateway' }}
							<!-- v{{ version }} -->
						</footer>
					</a-layout>
				</div>
				<!-- 个人设置抽屉 -->
				<a-drawer
					v-model:open="drawer.open"
					class="custom-class"
					root-class-name="root-class-name"
					title="个人设置"
					placement="right"
				>
					<a-form
						ref="drawerFormRef"
						:model="drawer"
						:rules="drawerRulesRef"
						:label-col="{ span: 6 }"
						:wrapper-col="{ span: 18 }"
						autocomplete="off"
						@finish="handleDrawerFinish"
					>
						<a-divider
							orientation="left"
							plain
							class="!text-black/60 dark:!text-white/60"
						>
							基本信息
						</a-divider>
						<a-form-item label="账号">
							<a-input v-model:value="userInfo.loginid" :disabled="true" />
						</a-form-item>
						<a-form-item label="姓名">
							<a-input v-model:value="userInfo.name" :disabled="true" />
						</a-form-item>
						<a-form-item label="租户">
							<a-input
								v-model:value="userInfo.tenantTranslateText"
								:disabled="true"
							/>
						</a-form-item>
						<a-divider
							orientation="left"
							plain
							class="!text-black/60 dark:!text-white/60"
						>
							修改密码
						</a-divider>
						<a-form-item label="旧密码" name="password">
							<a-input
								v-model:value="drawer.password"
								placeholder="请输入旧密码"
							/>
						</a-form-item>
						<a-form-item label="确认密码" name="affirmPassword">
							<a-input
								v-model:value="drawer.affirmPassword"
								placeholder="请输入确认密码"
							/>
						</a-form-item>
						<a-form-item :wrapper-col="{ offset: 6, span: 18 }">
							<a-button type="primary" html-type="submit">提交</a-button>
							<a-button class="ml-3" @click="drawer.open = false">
								关闭
							</a-button>
						</a-form-item>
					</a-form>
				</a-drawer>
			</a-config-provider>
		</a-style-provider>
	</client-only>
</template>

<style lang="scss">
@import '@/assets/style/data.scss';

// 主体内容布局
.layout-content {
	height: calc(100vh - 64px - 12px - 32px - 36px);
	border-bottom-left-radius: 6px;
	border-bottom-right-radius: 6px;
	// 页面主要内容-超出显示滚动条
	.layouts-slot {
		height: calc(100vh - 64px - 12px - 32px - 12px * 3 - 26px);
		overflow: auto;
	}
}
// 默认布局的tabs样式
.layouts-default-tabs {
	.ant-tabs-nav {
		margin: 0 !important;
	}
}
// 折叠后的 tabs 下拉框样式优化
.ant-tabs-dropdown {
	.ant-tabs-dropdown-menu-title-content {
		display: flex;
		justify-content: space-between;
	}
}
// 菜单
.default-menu {
	height: calc(100vh - 64px);
	overflow-y: auto;
}

// 菜单全局样式
.ant-menu {
	color: $menu-color !important;
}
// .ant-menu-light .ant-menu-item-selected {
// 	background-color: $menu-bg !important;
// }
.ant-menu-light {
	color: $menu-color !important;
	background-color: $menu-bg !important;
}

/**参考网站效果
 */
// .ant-menu-sub {
// 	background-color: #001529 !important;
// }
// // 二级菜单选中样式
// .ant-menu-item-active,
// .ant-menu-item-selected {
// 	color: $menu-hover-color !important;
// 	background-color: #000c17 !important;
// 	border-radius: 0 !important;
// }
// // 一级菜单悬浮样式
// .ant-menu-submenu-active {
// 	color: $primary-color !important;
// 	background-color: $menu-bg !important;
// }
// // 一级菜单悬浮样式
// .ant-menu-submenu-title {
// 	color: $menu-hover-color !important;
// 	&:hover {
// 		color: $menu-hover-color !important;
// 	}
// }

/**原效果
 */
.ant-menu-item-active,
.ant-menu-item-selected {
	// color: $menu-hover-color !important;
	color: #fff !important;
	background-color: #000c17 !important;
}
// 一级菜单
.ant-menu-submenu-selected > .ant-menu-submenu-title {
	color: #fff !important;
}
// 一级菜单悬浮样式
.ant-menu-submenu-title {
	&:hover {
		color: #fff !important;
	}
}

// 菜单展开
.ant-menu-sub.ant-menu-inline {
	background-color: #001529 !important;
}

.header-logo-text {
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	// color: $menu-color;
	color: #fff;
	background-color: $menu-bg;
}
.ant-menu-inline-collapsed {
	.ant-menu-title-content {
		display: none;
	}
}
</style>
