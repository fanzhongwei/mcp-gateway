<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/log.vue
 * @Author: teddy
 * @Date: 2024-04-01 13:59:05
 * @Description: 日志
 * @LastEditors: teddy
 * @LastEditTime: 2024-04-19 22:16:38
-->
<script setup lang="tsx">
import { fetchLogList, fetchLogListExport } from '~/fetch/http'

import { useStorage } from '@vueuse/core'
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

// table ref
const tableRef = ref()
const changeTableInput = () => {
	if (!tableSearch.userName) {
		tableRef.value.getTableList(1, true)
	}
}

const columns = reactive<any>([
	{
		title: '序号',
		dataIndex: 'index',
		width: 80,
		customFilterDropdown: true,
	},
	{
		title: '访问IP',
		dataIndex: 'ip',
	},
	{
		title: '访问API的用户ID',
		dataIndex: 'user',
		width: 130,
	},
	{
		title: '用户名',
		dataIndex: 'userName',
	},
	{
		title: '访问模块',
		dataIndex: 'module',
	},
	{
		title: '访问的api地址',
		dataIndex: 'api',
	},
	{
		title: '接口名',
		dataIndex: 'apiName',
	},
	{
		title: '请求内容',
		dataIndex: 'request',
	},
	{
		title: '请求状态',
		dataIndex: 'status',
	},
	{
		title: '请求结果',
		dataIndex: 'response',
	},
	{
		title: '请求开始时间',
		dataIndex: 'requestStartTime',
	},
	{
		title: '请求结束时间',
		dataIndex: 'requestEndTime',
	},
	{
		title: '请求耗时',
		dataIndex: 'requestTimes',
	},
])
// table search
const tableSearch = reactive<any>({
	userName: '',
	userType: undefined,
	deptId: undefined,
	tenant: userInfo.value.tenant === '0' ? undefined : userInfo.value.tenant,
	type: [],
})

// table-ajax
const getUserList = async (page: any) => {
	try {
		const res = await fetchLogList({
			...page,
			entityParam: {
				tenant: tableSearch.tenant || undefined,
				userName: tableSearch.userName || undefined,
			},
		})
		return Promise.resolve(res)
	} catch (error) {
		return Promise.reject(error)
	}
}
// 导出
const exportObj = reactive({
	exportName: '日志列表',
	exportMethod: fetchLogListExport,
})
</script>

<template>
	<!-- table -->
	<pay-table
		ref="tableRef"
		:ajax="getUserList"
		:columns="columns"
		:exportObj="exportObj"
	>
		<template #search>
			<div class="flex gap-3">
				<a-input
					v-model:value="tableSearch.userName"
					allowClear
					placeholder="请输入姓名或账号"
					@press-enter="() => tableRef.getTableList(1, true)"
					@change="changeTableInput"
				></a-input>
				<a-button type="primary" @click="() => tableRef.getTableList(1, true)">
					<i class="ri-search-line mr-1"></i>
					查询
				</a-button>
			</div>
		</template>
	</pay-table>
</template>

<style lang="scss"></style>
