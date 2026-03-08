<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/authorization.vue
 * @Author: teddy
 * @Date: 2024-03-18 10:03:51
 * @Description: 系统设置-dwjk授权
 * @LastEditors: teddy
 * @LastEditTime: 2024-06-26 15:11:28
-->
<script setup lang="tsx">
import { useStorage } from '@vueuse/core'
import dayjs from 'dayjs'
import { _deleteModal, _tmeMsg, _toDayjs } from '~/assets/js/util'
import {
	fetchAuthorizationAdd,
	fetchAuthorizationDelete,
	fetchAuthorizationEdit,
	fetchAuthorizationList,
	fetchAuthorizationListExport,
} from '~/fetch/http'

import ClipboardJS from 'clipboard'
// import hljs from 'highlight.js/lib/core'
// import typescript from 'highlight.js/lib/languages/typescript'
// // theme
// import 'highlight.js/styles/github.css'
// hljs.registerLanguage('typescript', typescript)
// const setHtml = (code: any) =>
// 	hljs.highlight(code, { language: 'typescript' }).value

const html = (
	id: string,
	accessToken: string,
	accessTimeStart: string,
	accessTimeEnd: string,
) => `systemId：${id}
systemToken：${accessToken}
授权开始时间：${accessTimeStart}
授权结束时间：${accessTimeEnd}
调用业务接口前需使用systemId和systemToken访问接口https://ip:port/payment-platform/dwjk/api/v1/access/${id}/token/${accessToken}
获取业务接口AccessToken（有效期2分钟）
调用业务接口时将获取到的AccessToken放入业务接口请求header中：AccessToken=xxx`

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

// tableSearch
const tableSearch = reactive({
	// 系统名称
	systemName: undefined,
	tenant: userInfo.value.tenant === '0' ? undefined : userInfo.value.tenant,
})

// table ref
const tableRef = ref()
const changeTableInput = () => {
	if (!tableSearch.systemName) {
		tableRef.value.getTableList(1, true)
	}
}

// 导出
const exportObj = reactive({
	exportName: '对外授权列表',
	exportMethod: fetchAuthorizationListExport,
})

// table-ajax
const getUserList = async (page: any) => {
	try {
		const res = await fetchAuthorizationList({
			...page,
			entityParam: {
				systemName: tableSearch.systemName || undefined,
				tenant: tableSearch.tenant || undefined,
			},
		})
		return Promise.resolve(res)
	} catch (error) {
		return Promise.reject(error)
	}
}

// 授权数据
const formData = reactive<any>({
	visible: false,
	msgVisible: false,
	loading: false,
	id: undefined,
	systemName: undefined,
	accessToken: undefined,
	ipWhitelist: undefined,
	ipBlacklist: undefined,
	accessTimeStart: undefined,
	accessTimeEnd: undefined,
	contact: undefined,
	contactNumber: undefined,
	// 授权人，需前端隐式录入
	authorizer: userInfo.value.username,
	tenant: userInfo.value.tenant,
})

// formDataRef
const formDataRef = ref()

// 时间校验-开始时间
const validateAccessTimeStart = () => {
	if (
		formData.accessTimeEnd &&
		formData.accessTimeStart &&
		_toDayjs(formData.accessTimeEnd) <= _toDayjs(formData.accessTimeStart)
	) {
		return Promise.reject(new Error('开始时间不能大于等于结束时间'))
	}
	return Promise.resolve()
}
// 时间校验-结束时间
const validateAccessTimeEnd = () => {
	if (
		formData.accessTimeStart &&
		formData.accessTimeEnd &&
		_toDayjs(formData.accessTimeEnd) <= _toDayjs(formData.accessTimeStart)
	) {
		return Promise.reject(new Error('结束时间不能小于等于开始时间'))
	}
	return Promise.resolve()
}

// 授权编辑
const handleEdit = (data: any) => {
	formData.visible = true
	nextTick(() => {
		Object.assign(formData, {
			...data,
			accessTimeStart: data.accessTimeStart
				? dayjs(data.accessTimeStart)
				: undefined,
			accessTimeEnd: data.accessTimeEnd ? dayjs(data.accessTimeEnd) : undefined,
			authorizationTime: _toDayjs(data.authorizationTime),
		})
	})
}

// 授权编辑-提交
const handleEditSubmit = async () => {
	try {
		await formDataRef.value.validate()
		if (formData.id) {
			// 编辑
			await fetchAuthorizationEdit(toRaw(formData))
		} else {
			// 新增
			await fetchAuthorizationAdd({
				...toRaw(formData),
				authorizationTime: dayjs(new Date()),
			})
		}
		_tmeMsg.success('操作成功')
		formData.visible = false
		tableRef.value.getTableList()
	} catch (error) {}
}

// 对外授权使用方法
const handleMsg = (data: any) => {
	formData.msgVisible = true
	nextTick(() => {
		formData.id = data.id
		formData.accessToken = data.accessToken
		formData.accessTimeStart = data.accessTimeStart
			? _toDayjs(data.accessTimeStart)
			: ''
		formData.accessTimeEnd = data.accessTimeEnd
			? _toDayjs(data.accessTimeEnd)
			: ''
	})
}

//  设备-删除
const handleDelete = (id: string, name: string) => {
	_deleteModal(
		'授权',
		name,
		() => fetchAuthorizationDelete(id),
		() => tableRef.value.getTableList(),
	)
}
// 列
const columns = reactive([
	{
		title: '序号',
		dataIndex: 'index',
		width: 80,
		customFilterDropdown: true,
	},
	{
		title: '系统名称',
		dataIndex: 'systemName',
	},
	{
		title: 'IP白名单',
		dataIndex: 'ipWhitelist',
	},
	{
		title: 'IP黑名单',
		dataIndex: 'ipBlacklist',
	},
	{
		title: '授权开始时间',
		dataIndex: 'accessTimeStart',
		customRender: row =>
			row.record.accessTimeStart
				? _toDayjs(row.record.accessTimeStart, 'YYYY-MM-DD HH:mm:ss')
				: '-',
	},
	{
		title: '授权结束时间',
		dataIndex: 'accessTimeEnd',
		customRender: row =>
			row.record.accessTimeEnd
				? _toDayjs(row.record.accessTimeEnd, 'YYYY-MM-DD HH:mm:ss')
				: '-',
	},
	{
		title: '系统联系人',
		dataIndex: 'contact',
	},
	{
		title: '系统联系人电话',
		dataIndex: 'contactNumber',
	},
	{
		title: '授权人',
		dataIndex: 'authorizer',
	},
	{
		title: '授权时间',
		dataIndex: 'authorizationTime',
		customRender: row =>
			row.record.accessTimeEnd
				? _toDayjs(row.record.accessTimeEnd, 'YYYY-MM-DD HH:mm:ss')
				: '-',
	},
	{
		title: '操作',
		width: 260,
		dataIndex: 'active',
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div class='flex'>
					{data.disableEdit !== '1' && (
						<a-button
							v-permission='system.system.authorization.edit'
							size='small'
							type='link'
							onClick={() => {
								handleEdit(data)
							}}
						>
							<i class='ri-edit-box-line mr-1'></i>
							编辑
						</a-button>
					)}

					<a-button
						size='small'
						type='link'
						onClick={() => {
							handleMsg(data)
						}}
					>
						<i class='ri-file-copy-2-line mr-1'></i>
						使用方法
					</a-button>
					{data.disableDelete !== '1' && (
						<a-button
							v-permission='system.system.authorization.delete'
							type='link'
							danger
							size='small'
							onClick={() => {
								handleDelete(data.id, data.systemName)
							}}
						>
							<i class='ri-delete-bin-line mr-1'></i>
							删除
						</a-button>
					)}
				</div>
			)
		},
	},
])

const clipboard = ref<any>(null)

watch(
	() => formData.visible,
	(val: boolean) => {
		if (!val) {
			// 重置表单
			formDataRef.value.resetFields()
			formData.id = undefined
			formData.authorizationTime = undefined
		}
	},
)

watch(
	() => formData.msgVisible,
	val => {
		if (val) {
			nextTick(() => {
				clipboard.value = new ClipboardJS('.authorization-copy')
				clipboard.value.on('success', (e: any) => {
					// message.info('复制成功 ' + e.text)
					_tmeMsg.success('复制成功 ')
					// message.error('复制成功 ' + e.text)
					e.clearSelection()
				})
			})
		} else {
			clipboard.value.destroy()
		}
	},
)

onMounted(() => {})
onUnmounted(() => {
	try {
		if (clipboard.value) {
			clipboard.value.destroy()
		}
	} catch (error) {}
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
					v-model:value="tableSearch.systemName"
					allowClear
					placeholder="请输入系统名称"
					@press-enter="() => tableRef.getTableList(1, true)"
					@change="changeTableInput"
				></a-input>
				<a-button type="primary" @click="() => tableRef.getTableList(1, true)">
					<i class="ri-search-line mr-1"></i>
					查询
				</a-button>
			</div>
			<a-button
				v-permission="'system.system.authorization.add'"
				type="primary"
				class="add-button"
				@click="formData.visible = true"
			>
				<i class="ri-add-line mr-1"></i>
				新增对外授权
			</a-button>
		</template>
	</pay-table>
	<!-- 对外授权 form  -->
	<a-modal
		v-model:open="formData.visible"
		:title="formData.id ? '编辑对外授权' : '新增对外授权'"
	>
		<a-form
			ref="formDataRef"
			:model="formData"
			:label-col="{ span: 6 }"
			:wrapper-col="{ span: 18 }"
			autocomplete="off"
		>
			<a-form-item v-if="formData.id" label="系统授权">
				<a-input v-model:value="formData.accessToken" disabled />
			</a-form-item>
			<a-form-item v-if="formData.id" label="授权人">
				<a-input v-model:value="formData.authorizer" disabled />
			</a-form-item>
			<a-form-item v-if="formData.id" label="授权时间">
				<a-input v-model:value="formData.authorizationTime" disabled />
			</a-form-item>
			<a-form-item
				label="系统名称"
				name="systemName"
				:rules="[{ required: true, message: '请输入系统名称！' }]"
			>
				<a-input
					v-model:value="formData.systemName"
					placeholder="请输入系统名称"
				/>
			</a-form-item>

			<a-form-item label="IP白名单" name="ipWhitelist">
				<a-textarea
					v-model:value="formData.ipWhitelist"
					placeholder="请输入IP白名单"
				/>
				<div class="text-xs mt-1">
					多个以;隔开，如果设置则仅允许白名单内的IP访问
				</div>
			</a-form-item>
			<a-form-item label="IP黑名单" name="ipBlacklist">
				<a-textarea
					v-model:value="formData.ipBlacklist"
					placeholder="请输入IP黑名单"
				/>
				<div class="text-xs mt-1">
					多个以;隔开，如果设置则不允许黑名单内的IP访问
				</div>
			</a-form-item>
			<a-form-item
				label="授权开始时间"
				name="accessTimeStart"
				:rules="[{ validator: validateAccessTimeStart }]"
			>
				<a-date-picker
					v-model:value="formData.accessTimeStart"
					class="w-full"
					placeholder="请输入授权开始时间"
				/>
			</a-form-item>
			<a-form-item
				label="授权结束时间"
				name="accessTimeEnd"
				:rules="[{ validator: validateAccessTimeEnd }]"
			>
				<a-date-picker
					v-model:value="formData.accessTimeEnd"
					class="w-full"
					placeholder="请输入授权结束时间"
				/>
			</a-form-item>
			<a-form-item label="系统联系人" name="contact">
				<a-input
					v-model:value="formData.contact"
					placeholder="请输入系统联系人"
				/>
			</a-form-item>
			<a-form-item label="系统联系人电话" name="contactNumber">
				<a-input
					v-model:value="formData.contactNumber"
					placeholder="请输入系统联系人电话"
					:maxlength="11"
				/>
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="formData.visible = false">取消</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="formData.loading"
				@click="handleEditSubmit"
			>
				提交
			</a-button>
		</template>
	</a-modal>
	<!-- 对外授权使用方法 -->
	<a-modal
		v-model:open="formData.msgVisible"
		title="对外授权使用方法"
		style="width: 680px"
	>
		<a-card class="w-full">
			<div>
				<b>systemId</b>
				：{{ formData.id }}
			</div>
			<div>
				<b>systemToken</b>
				：{{ formData.accessToken }}
			</div>
			<div>
				<b>授权开始时间</b>
				：{{ formData.accessTimeStart }}
			</div>
			<div>
				<b>授权结束时间</b>
				：{{ formData.accessTimeStart }}
			</div>
			<div>
				调用业务接口前需使用
				<b>systemId</b>
				和
				<b>systemToken</b>
				访问接口:
			</div>
			<div class="break-all">
				<b>
					https://ip:port/payment-platform/dwjk/api/v1/access/
					{{ formData.id }}
					/token/
					{{ formData.accessToken }}
				</b>
			</div>
			<div>
				获取业务接口
				<b>AccessToken</b>
				（有效期2分钟）
			</div>
			<div>
				调用业务接口时将获取到的
				<b>AccessToken</b>
				放入业务接口请求
				<b>header</b>
				中：AccessToken=xxx
			</div>
		</a-card>

		<template #footer>
			<a-button key="back" @click="formData.msgVisible = false">取消</a-button>
			<a-button
				type="primary"
				class="authorization-copy"
				:data-clipboard-text="
					html(
						formData.id,
						formData.accessToken,
						formData.accessTimeStart,
						formData.accessTimeEnd,
					)
				"
			>
				复制
			</a-button>
		</template>
	</a-modal>
</template>

<style lang="scss"></style>
