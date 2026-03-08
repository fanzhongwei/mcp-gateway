<!--
 * @FilePath: /mcp-gateway-web/src/pages/login.vue
 * @Author: teddy
 * @Date: 2024-02-02 14:56:16
 * @Description: 登录页
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-02 13:38:37
-->
<script setup lang="ts">
import { useStorage } from '@vueuse/core'
import { message } from 'ant-design-vue'
import { _clearStorage } from '~/assets/js/util'
import { getFirstPermittedPath, MENU_DATA_SOURCE } from '~/config/menu'
import { fetchLogin, fetchUserInfo } from '~/fetch/http'

definePageMeta({
	layout: 'single',
})

const route: any = useRoute()
const isDevelop = process.env.NODE_ENV === 'development'

// 视频加载
const refVideoLogin2 = ref()
const videoLogin2Visible = ref(true)
const onEnded = () => {
	videoLogin2Visible.value = false
	refVideoLogin2.value.play()
}

// loading
const loading = ref(false)

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

// 表单
const formState = reactive<{
	username: string
	password: string
}>({
	username: '',
	password: '',
})
// 登录
const onFinish = async (values: any) => {
	try {
		loading.value = true
		const res = await fetchLogin(values)
		// 存储登录账号
		userInfo.value.username = values.username
		Object.assign(userInfo.value, res.data)
		// 记住密码
		if (userInfo.value.remember) {
			userInfo.value.password = values.password
		} else {
			userInfo.value.password = ''
		}
		message.destroy()
		message.success(`欢迎<${res.data.name}>登录`)

		// 是否有 401 之前的路由
		const payOldRouteName401: any = useCookie('pay-old-route-name-401')
		if (payOldRouteName401.value) {
			navigateTo({
				name: payOldRouteName401.value,
			})
		} else {
			// 进入当前用户有权限的第一个页面
			debugger
			const { data } = await fetchUserInfo(res.data.id)
			const rightList = data?.rightList || []
			userInfo.value.rightList = rightList
			const firstPath = getFirstPermittedPath(MENU_DATA_SOURCE, rightList)
			navigateTo(firstPath || '/businessSystem')
		}
		// 登录成功-清空 401 记录的旧路由地址
		payOldRouteName401.value = null
		loading.value = false
	} catch (error) {
		loading.value = false
	}
}

// 辅助单点登录
const handlerSSO = async (loginAccount: string, schoolid: string) => {
	formState.username = loginAccount
	formState.password = schoolid
	await onFinish(toRaw(formState))
}

// 页面加载
onMounted(() => {
	formState.username = userInfo.value.username
	formState.password = userInfo.value.password
	// 清除本地存储
	_clearStorage(route?.query?.entered || null)
	// 获取 route.query，辅助单点登录
	if (route?.query?.loginAccount && route?.query?.schoolid) {
		// 辅助单点登录
		handlerSSO(route?.query?.loginAccount, route?.query?.schoolid)
	}
})
</script>

<template>
	<!-- login video 1 -->
	<video
		class="absolute inset-0 w-full h-full object-cover -z-10"
		src="@/assets/video/login1.mp4"
		muted
		autoplay
		playsinline
		tabindex="-1"
		@ended="() => onEnded()"
	></video>
	<!-- login video 2 -->
	<video
		ref="refVideoLogin2"
		class="absolute inset-0 w-full h-full object-cover -z-10"
		:class="{
			hidden: videoLogin2Visible,
		}"
		src="@/assets/video/login2.mp4"
		loop
		muted
		tabindex="-1"
	></video>
	<div class="page-login bg-white/40 dark:bg-dark/60">
		<div
			class="login-welcome text-center text-2xl m-6 font-bold text-black/60 dark:text-white/60 before:bg-black/60 dark:before:bg-white/60 after:bg-dark/60 dark:after:bg-white/60 select-none"
		>
			<span>欢迎登录</span>
		</div>
		<h1
			class="text-center text-4xl font-semibold mb-10 text-black/90 dark:text-white/90 select-none zrlziti"
		>
			{{ isDevelop ? 'develop' : 'MCP Gateway' }}
		</h1>
		<a-form
			:model="formState"
			name="basic"
			autocomplete="off"
			@finish="onFinish"
		>
			<a-form-item
				name="username"
				:rules="[{ required: true, message: '请输入账号!' }]"
			>
				<a-input
					v-model:value="formState.username"
					size="large"
					placeholder="请输入账号"
				>
					<template #prefix>
						<i class="ri-user-line"></i>
					</template>
				</a-input>
			</a-form-item>

			<a-form-item
				name="password"
				:rules="[{ required: true, message: '请输入密码!' }]"
			>
				<a-input-password
					v-model:value="formState.password"
					size="large"
					placeholder="请输入密码"
				>
					<template #prefix>
						<i class="ri-lock-line"></i>
					</template>
				</a-input-password>
			</a-form-item>

			<a-form-item>
				<a-checkbox v-model:checked="userInfo.remember" class="select-none">
					记住密码
				</a-checkbox>
			</a-form-item>

			<a-form-item>
				<a-button
					:loading="loading"
					class="w-full"
					size="large"
					type="primary"
					html-type="submit"
				>
					登录
				</a-button>
			</a-form-item>
		</a-form>
	</div>
</template>

<style lang="scss">
.page-login {
	width: 500px;
	border-image: linear-gradient(0deg, #cae7f2, #dcf4ff) 1 1;
	box-shadow: 0 11px 21px 0 rgba(12, 56, 94, 0.19);
	border-radius: 20px;
	position: fixed;
	right: 10%;
	top: 20%;
	padding: 50px;
	.login-welcome {
		position: relative;
		&::before {
			position: absolute;
			width: 100px;
			height: 1px;
			left: 0;
			top: calc(50% - 1px);
		}
		&::after {
			position: absolute;
			width: 100px;
			height: 1px;
			right: 0;
			top: calc(50% - 1px);
		}
	}
}
</style>
