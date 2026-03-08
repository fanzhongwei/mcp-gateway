<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/develop/page-template.vue
 * @Author: teddy
 * @Date: 2024-03-08 11:27:16
 * @Description: 开发工具-页面模板
 * @LastEditors: teddy
 * @LastEditTime: 2024-05-06 10:15:01
-->

<script setup lang="tsx">
import { _deleteModal, _getPermissionsData, _tmeMsg } from '@/assets/js/util'
// import {
// 	fetchXXXList,
// 	fetchXXAdd,
// 	fetchXXDelete,
// 	fetchXXEdit,
// } from '~/fetch/http'

// table ref
const tableRef = ref()
const searchName = ref(null)
const changeTableInput = () => {
	if (!searchName.value) {
		tableRef.value.getTableList(1)
	}
}

// table-ajax
const getXXList = async (page: any) => {
	try {
		const res = await fetchXXXList({
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
		title: 'XX名称',
		dataIndex: 'name',
	},
	{
		title: 'XX描述',
		dataIndex: 'descript',
	},
	{
		title: '操作',
		dataIndex: 'active',
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div class='flex'>
					<a-button
						size='small'
						v-permission=''
						onClick={() => {
							handleEdit(data)
						}}
					>
						<i class='ri-edit-box-line mr-1'></i>
						编辑
					</a-button>
					<a-button
						type='link'
						size='small'
						danger
						v-permission=''
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

// XX-modal
const XXFormData = reactive<any>({
	visible: false,
	loading: false,
	id: '',
	name: '',
	descript: '',
	// XX权限点
	XXRightKeyList: [],
})
// XX-modal-ref
const XXFormRef = ref()
// XX-modal-提交
const handleXX = async () => {
	try {
		await XXFormRef.value.validate()
		const XXRightKeyList: any = []
		XXFormData.XXRightKeyList.forEach((item: any) => {
			XXRightKeyList.push(item.value)
		})
		if (XXFormData.id) {
			await fetchXXEdit({
				id: XXFormData.id,
				name: XXFormData.name,
				descript: XXFormData.descript,
				XXRightKeyList,
			})
		} else {
			await fetchXXAdd({
				name: XXFormData.name,
				descript: XXFormData.descript,
				XXRightKeyList,
			})
		}
		XXFormData.visible = false
		_tmeMsg.success(XXFormData.id ? '编辑XX成功' : '添加XX成功')
		// 重新获取 table 数据
		tableRef.value.getTableList()
	} catch (error) {}
}
// XX-modal-编辑
const handleEdit = (row: any) => {
	try {
		XXFormData.visible = true
		// 因 reactive 问题，需要使用 nextTick，
		// 否则先编辑再新增，form 的值为上次编辑的值
		nextTick(() => {
			XXFormData.id = row.id
			XXFormData.name = row.name
			XXFormData.descript = row.descript
			XXFormData.XXRightKeyList = []
		})
	} catch (error) {}
}

//  XX-删除
const handleDelete = (id: string, name: string) => {
	_deleteModal(
		'XX',
		name,
		() => fetchXXDelete(id),
		() => tableRef.value.getTableList(),
	)
}
// XX-modal-监听显示状态
watch(
	() => XXFormData.visible,
	val => {
		if (!val) {
			// 重置表单
			XXFormRef.value.resetFields()
			XXFormData.id = ''
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
		:ajax="getXXList"
		:columns="columns"
		:pagination="false"
	>
		<template #search>
			<div class="flex gap-3">
				<a-input
					v-model:value="searchName"
					placeholder="请输入XX名称"
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
				v-permission="''"
				type="primary"
				@click="XXFormData.visible = true"
			>
				<i class="ri-add-line mr-1"></i>
				新增XX
			</a-button>
		</template>
	</pay-table>
	<!-- XX-modal -->
	<a-modal
		v-model:open="XXFormData.visible"
		:title="XXFormData.id ? '编辑XX' : '新增XX'"
	>
		<a-form
			ref="XXFormRef"
			:model="XXFormData"
			:label-col="{ span: 4 }"
			:wrapper-col="{ span: 20 }"
			autocomplete="off"
		>
			<a-form-item
				label="名称"
				name="name"
				:rules="[{ required: true, message: '请输入名称！' }]"
			>
				<a-input v-model:value="XXFormData.name" placeholder="请输入名称" />
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="XXFormData.visible = false">取消</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="XXFormData.loading"
				@click="handleXX"
			>
				提交
			</a-button>
		</template>
	</a-modal>
</template>

<style lang="scss"></style>
