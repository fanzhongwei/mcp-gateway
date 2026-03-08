<!--
 * @FilePath: /mcp-gateway-web/src/pages/businessSystem/index.vue
 * @Author: teddy
 * @Date: 2026-01-22
 * @Description: 业务系统列表
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-22
-->
<script setup lang="tsx">
import {
	_deleteModal,
	_getPermissionsData,
	_tmeMsg,
	_toDayjs,
} from '@/assets/js/util'
import {
	fetchSystemAdd,
	fetchSystemDelete,
	fetchSystemEdit,
	fetchSystemInfo,
	fetchSystemList,
} from '~/fetch/http'
import SystemEnvManage from './SystemEnvManage.vue'

// table ref
const tableRef = ref()
const searchName = ref(null)
const changeTableInput = () => {
	if (!searchName.value) {
		tableRef.value.getTableList(1)
	}
}

// table-ajax
const getSystemList = async (page: any) => {
	try {
		const res = await fetchSystemList({
			entityParam: {
				name: searchName.value,
			},
			...page,
		})
		return Promise.resolve(res)
	} catch (error) {
		return Promise.reject(error)
	}
}

// table-th
const columns = reactive<any>([
	{
		title: '序号',
		dataIndex: 'index',
		width: 80,
		customFilterDropdown: true,
	},
	{
		title: '系统名称',
		dataIndex: 'name',
	},
	{
		title: '简介',
		dataIndex: 'desc',
	},
	{
		title: '管理员',
		dataIndex: 'admin',
	},
	{
		title: '管理员联系方式',
		dataIndex: 'adminContact',
	},
	{
		title: '创建时间',
		dataIndex: 'createTime',
		width: 180,
		customRender: (row: any) =>
			row.record.createTime
				? _toDayjs(row.record.createTime, 'YYYY-MM-DD HH:mm:ss')
				: '-',
	},
	{
		title: '操作',
		dataIndex: 'active',
		width: 200,
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div class='flex'>
					<a-button
						size='small'
						type='link'
						v-permission='system.system.businessSystem.edit'
						onClick={() => {
							handleEdit(data)
						}}
					>
						<i class='ri-edit-box-line mr-1'></i>
						编辑
					</a-button>
					<a-button
						size='small'
						type='link'
						v-permission='system.system.businessSystem.env'
						onClick={() => {
							handleEnvManage(data)
						}}
					>
						<i class='ri-settings-3-line mr-1'></i>
						环境管理
					</a-button>
					<a-button
						type='link'
						danger
						size='small'
						v-permission='system.system.businessSystem.delete'
						onClick={() => {
							handleDelete(data.id, data.name)
						}}
					>
						<i class='ri-delete-bin-line mr-1'></i>
						删除
					</a-button>
				</div>
			)
		},
	},
])

// 系统-modal
const systemFormData = reactive<any>({
	visible: false,
	loading: false,
	id: '',
	name: '',
	desc: '',
	admin: '',
	adminContact: '',
})
// 系统-modal-ref
const systemFormRef = ref()
// 系统-modal-提交
const handleSystem = async () => {
	try {
		await systemFormRef.value.validate()
		if (systemFormData.id) {
			await fetchSystemEdit({
				id: systemFormData.id,
				name: systemFormData.name,
				desc: systemFormData.desc,
				admin: systemFormData.admin,
				adminContact: systemFormData.adminContact,
			})
		} else {
			await fetchSystemAdd({
				name: systemFormData.name,
				desc: systemFormData.desc,
				admin: systemFormData.admin,
				adminContact: systemFormData.adminContact,
			})
		}
		systemFormData.visible = false
		_tmeMsg.success(systemFormData.id ? '编辑系统成功' : '添加系统成功')
		// 重新获取 table 数据
		tableRef.value.getTableList()
	} catch (error) {}
}
// 系统-modal-编辑
const handleEdit = async (row: any) => {
	try {
		const { data } = await fetchSystemInfo(row.id)
		systemFormData.visible = true
		// 因 reactive 问题，需要使用 nextTick，
		// 否则先编辑再新增，form 的值为上次编辑的值
		nextTick(() => {
			Object.assign(systemFormData, {
				id: data.id,
				name: data.name,
				desc: data.desc,
				admin: data.admin,
				adminContact: data.adminContact,
			})
		})
	} catch (error) {}
}

//  系统-删除
const handleDelete = (id: string, name: string) => {
	_deleteModal(
		'系统',
		name,
		() => fetchSystemDelete(id),
		() => tableRef.value.getTableList(),
	)
}
// 系统-modal-监听显示状态
watch(
	() => systemFormData.visible,
	val => {
		if (!val) {
			// 重置表单
			systemFormRef.value.resetFields()
			systemFormData.id = ''
			systemFormData.name = ''
			systemFormData.desc = ''
			systemFormData.admin = ''
			systemFormData.adminContact = ''
		}
	},
)

// 环境管理相关
const envManageRef = ref()
const envManageData = reactive({
	systemId: '',
	systemName: '',
})

// 环境管理-打开弹窗
const handleEnvManage = (row: any) => {
	envManageData.systemId = row.id
	envManageData.systemName = row.name
	nextTick(() => {
		if (envManageRef.value) {
			envManageRef.value.open()
		}
	})
}

onMounted(() => {
	_getPermissionsData()
})
</script>

<template>
	<!-- table -->
	<pay-table ref="tableRef" :ajax="getSystemList" :columns="columns">
		<template #search>
			<div class="flex gap-3">
				<a-input
					v-model:value="searchName"
					placeholder="请输入系统名称"
					allowClear
					@change="changeTableInput"
					@press-enter="() => tableRef.getTableList(1)"
				></a-input>
				<a-button type="primary" @click="() => tableRef.getTableList(1)">
					<i class="ri-search-line mr-1"></i>
					查询
				</a-button>
			</div>
			<a-button
				v-permission="'system.system.businessSystem.add'"
				type="primary"
				class="add-button"
				@click="systemFormData.visible = true"
			>
				<i class="ri-add-line mr-1"></i>
				新增系统
			</a-button>
		</template>
	</pay-table>
	<!-- 系统-modal -->
	<a-modal
		v-model:open="systemFormData.visible"
		:title="systemFormData.id ? '编辑系统' : '新增系统'"
	>
		<a-form
			ref="systemFormRef"
			:model="systemFormData"
			:label-col="{ span: 4 }"
			:wrapper-col="{ span: 20 }"
			autocomplete="off"
		>
			<a-form-item
				label="系统名称"
				name="name"
				:rules="[{ required: true, message: '请输入系统名称！' }]"
			>
				<a-input
					v-model:value="systemFormData.name"
					placeholder="请输入系统名称"
				/>
			</a-form-item>
			<a-form-item label="简介" name="desc">
				<a-textarea
					v-model:value="systemFormData.desc"
					:auto-size="{ minRows: 3, maxRows: 3 }"
					placeholder="请输入简介"
				/>
			</a-form-item>
			<a-form-item label="管理员" name="admin">
				<a-input
					v-model:value="systemFormData.admin"
					placeholder="请输入管理员"
				/>
			</a-form-item>
			<a-form-item label="管理员联系方式" name="adminContact">
				<a-input
					v-model:value="systemFormData.adminContact"
					placeholder="请输入管理员联系方式"
				/>
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="systemFormData.visible = false">
				取消
			</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="systemFormData.loading"
				@click="handleSystem"
			>
				提交
			</a-button>
		</template>
	</a-modal>
	<!-- 环境管理组件 -->
	<SystemEnvManage
		ref="envManageRef"
		:system-id="envManageData.systemId || ''"
		:system-name="envManageData.systemName || ''"
	/>
</template>

<style lang="scss" scoped></style>
