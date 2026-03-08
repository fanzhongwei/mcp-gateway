<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/role.vue
 * @Author: teddy
 * @Date: 2024-02-12 12:15:20
 * @Description: 角色角色
 * @LastEditors: teddy
 * @LastEditTime: 2024-05-06 10:14:54
-->

<script setup lang="tsx">
import { _deleteModal, _getPermissionsData, _tmeMsg } from '@/assets/js/util'
import { useStorage } from '@vueuse/core'
import { TreeSelect } from 'ant-design-vue'
import {
	fetchRoleAdd,
	fetchRoleDelete,
	fetchRoleEdit,
	fetchRoleIdInfo,
	fetchRoleList,
} from '~/fetch/http'

// 所有权限点数据
const permissionsStorage = useStorage('pay-permissions-data', [])
// table ref
const tableRef = ref()
const searchName = ref(null)
const changeTableInput = () => {
	if (!searchName.value) {
		tableRef.value.getTableList(1)
	}
}

// tree select 显示所有
const SHOW_ALL = TreeSelect.SHOW_ALL

// table-ajax
const getRoleList = async (page: any) => {
	try {
		const res = await fetchRoleList({
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
		title: '角色名称',
		dataIndex: 'name',
	},
	{
		title: '角色描述',
		dataIndex: 'descript',
	},
	{
		title: '操作',
		dataIndex: 'active',
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div class='flex'>
					{data.disableEdit !== '1' && (
						<a-button
							size='small'
							type='link'
							v-permission='system.system.role.edit'
							onClick={() => {
								handleEdit(data)
							}}
						>
							<i class='ri-edit-box-line mr-1'></i>
							编辑
						</a-button>
					)}
					{data.disableDelete !== '1' && (
						<a-button
							type='link'
							danger
							size='small'
							v-permission='system.system.role.delete'
							onClick={() => {
								handleDelete(data.id, data.name)
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

// 角色-modal
const roleFormData = reactive<any>({
	visible: false,
	loading: false,
	id: '',
	name: '',
	descript: '',
	// 角色权限点
	roleRightKeyList: [],
})
// 角色-modal-ref
const roleFormRef = ref()
// 角色-modal-提交
const handleRole = async () => {
	try {
		await roleFormRef.value.validate()
		const roleRightKeyList: any = []
		roleFormData.roleRightKeyList.forEach((item: any) => {
			if (!item) return
			if (typeof item === 'string' || typeof item === 'number') {
				roleRightKeyList.push(item)
			} else if (item.value) {
				roleRightKeyList.push(item.value)
			}
		})
		if (roleFormData.id) {
			await fetchRoleEdit({
				id: roleFormData.id,
				name: roleFormData.name,
				descript: roleFormData.descript,
				roleRightKeyList,
			})
		} else {
			await fetchRoleAdd({
				name: roleFormData.name,
				descript: roleFormData.descript,
				roleRightKeyList,
			})
		}
		roleFormData.visible = false
		_tmeMsg.success(roleFormData.id ? '编辑角色成功' : '添加角色成功')
		// 重新获取 table 数据
		tableRef.value.getTableList()
	} catch (error) {}
}
// 角色-modal-编辑
const handleEdit = async (row: any) => {
	try {
		const { data } = await fetchRoleIdInfo(row.id)
		roleFormData.visible = true
		// 因 reactive 问题，需要使用 nextTick，
		// 否则先编辑再新增，form 的值为上次编辑的值
		nextTick(() => {
			const list: any = []
			data.map((item: any) => {
				list.push(item.rightkey)
			})
			Object.assign(roleFormData, {
				...row,
				roleRightKeyList: list,
			})
		})
	} catch (error) {}
}

//  角色-删除
const handleDelete = (id: string, name: string) => {
	_deleteModal(
		'角色',
		name,
		() => fetchRoleDelete(id),
		() => tableRef.value.getTableList(),
	)
}
// 角色-modal-监听显示状态
watch(
	() => roleFormData.visible,
	val => {
		if (!val) {
			// 重置表单
			roleFormRef.value.resetFields()
			roleFormData.id = ''
		}
	},
)

onMounted(() => {
	_getPermissionsData()
})
</script>

<template>
	<!-- table -->
	<pay-table
		ref="tableRef"
		:ajax="getRoleList"
		:columns="columns"
		:pagination="false"
	>
		<template #search>
			<div class="flex gap-3">
				<a-input
					v-model:value="searchName"
					placeholder="请输入角色名称"
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
				v-permission="'system.system.role.add'"
				type="primary"
				class="add-button"
				@click="roleFormData.visible = true"
			>
				<i class="ri-add-line mr-1"></i>
				新增角色
			</a-button>
		</template>
	</pay-table>
	<!-- 角色-modal -->
	<a-modal
		v-model:open="roleFormData.visible"
		:title="roleFormData.id ? '编辑角色' : '新增角色'"
	>
		<a-form
			ref="roleFormRef"
			:model="roleFormData"
			:label-col="{ span: 4 }"
			:wrapper-col="{ span: 20 }"
			autocomplete="off"
		>
			<a-form-item
				label="角色名称"
				name="name"
				:rules="[{ required: true, message: '请输入角色名称！' }]"
			>
				<a-input
					v-model:value="roleFormData.name"
					placeholder="请输入角色名称"
				/>
			</a-form-item>
			<a-form-item label="角色描述" name="descript">
				<a-textarea
					v-model:value="roleFormData.descript"
					:auto-size="{ minRows: 3, maxRows: 3 }"
					placeholder="请输入角色描述"
				/>
			</a-form-item>
			<a-form-item label="角色权限点" name="roleRightKeyList">
				<a-tree-select
					v-model:value="roleFormData.roleRightKeyList"
					style="width: 100%"
					treeCheckStrictly
					:tree-data="permissionsStorage"
					:showCheckedStrategy="SHOW_ALL"
					tree-checkable
					treeDefaultExpandAll
					:treeLine="{
						showLeafIcon: false,
					}"
					allow-clear
					placeholder="请选择角色权限点"
					tree-node-filter-prop="label"
				/>
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="roleFormData.visible = false">取消</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="roleFormData.loading"
				@click="handleRole"
			>
				提交
			</a-button>
		</template>
	</a-modal>
</template>

<style lang="scss"></style>
