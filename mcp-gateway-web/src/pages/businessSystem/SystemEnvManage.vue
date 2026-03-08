<!--
 * @FilePath: /mcp-gateway-web/src/pages/businessSystem/SystemEnvManage.vue
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: 系统环境管理组件
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-23
-->
<script setup lang="tsx">
import { _deleteModal, _tmeMsg, _toDayjs } from '@/assets/js/util'
import {
	fetchSystemEnvAdd,
	fetchSystemEnvDelete,
	fetchSystemEnvEdit,
	fetchSystemEnvInfo,
	fetchSystemEnvList,
} from '~/fetch/http'

// Props
const props = defineProps<{
	systemId: string
	systemName: string
}>()

// Emits
const emit = defineEmits<{
	update: []
}>()

// 环境管理弹窗
const envManageFormData = reactive<any>({
	visible: false,
})

// 环境表格
const envTableRef = ref()
const envSearchName = ref(null)

const changeEnvTableInput = () => {
	if (!envSearchName.value) {
		envTableRef.value.getTableList(1)
	}
}

// 环境列表-ajax
const getSystemEnvList = async (page: any) => {
	try {
		if (!props.systemId) {
			return Promise.resolve({ data: { records: [], total: 0 } })
		}
		const res = await fetchSystemEnvList({
			entityParam: {
				systemId: props.systemId,
				name: envSearchName.value,
			},
			...page,
		})
		return Promise.resolve(res)
	} catch (error) {
		return Promise.reject(error)
	}
}

// 环境列表-columns
const envColumns = reactive<any>([
	{
		title: '序号',
		dataIndex: 'index',
		width: 80,
		customFilterDropdown: true,
	},
	{
		title: '环境名称',
		dataIndex: 'name',
	},
	{
		title: '简介',
		dataIndex: 'desc',
	},
	{
		title: '系统基础地址',
		dataIndex: 'baseUrl',
		width: 300,
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
		width: 150,
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div class='flex'>
					<a-button
						size='small'
						type='link'
						v-permission='system.system.businessSystem.env.edit'
						onClick={() => {
							handleEnvEdit(data)
						}}
					>
						<i class='ri-edit-box-line mr-1'></i>
						编辑
					</a-button>
					<a-button
						type='link'
						danger
						size='small'
						v-permission='system.system.businessSystem.env.delete'
						onClick={() => {
							handleEnvDelete(data.id, data.name)
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

// 环境-modal
const envFormData = reactive<any>({
	visible: false,
	loading: false,
	id: '',
	name: '',
	desc: '',
	baseUrl: '',
})
const envFormRef = ref()

// 环境-modal-提交
const handleEnv = async () => {
	try {
		await envFormRef.value.validate()
		if (envFormData.id) {
			await fetchSystemEnvEdit({
				id: envFormData.id,
				systemId: props.systemId,
				name: envFormData.name,
				desc: envFormData.desc,
				baseUrl: envFormData.baseUrl,
			})
		} else {
			await fetchSystemEnvAdd({
				systemId: props.systemId,
				name: envFormData.name,
				desc: envFormData.desc,
				baseUrl: envFormData.baseUrl,
			})
		}
		envFormData.visible = false
		_tmeMsg.success(envFormData.id ? '编辑环境成功' : '添加环境成功')
		// 重新获取 table 数据
		envTableRef.value.getTableList()
		// 通知父组件更新
		emit('update')
	} catch (error) {}
}

// 环境-modal-编辑
const handleEnvEdit = async (row: any) => {
	try {
		const { data } = await fetchSystemEnvInfo(row.id)
		envFormData.visible = true
		nextTick(() => {
			Object.assign(envFormData, {
				id: data.id,
				name: data.name,
				desc: data.desc,
				baseUrl: data.baseUrl,
			})
		})
	} catch (error) {}
}

// 环境-删除
const handleEnvDelete = (id: string, name: string) => {
	_deleteModal(
		'环境',
		name,
		() => fetchSystemEnvDelete(id),
		() => {
			envTableRef.value.getTableList()
			emit('update')
		},
	)
}

// 打开环境管理弹窗
const open = () => {
	if (!props.systemId) {
		return
	}
	envManageFormData.visible = true
	envSearchName.value = null
	nextTick(() => {
		if (envTableRef.value) {
			envTableRef.value.getTableList(1)
		}
	})
}

// 环境-modal-监听显示状态
watch(
	() => envFormData.visible,
	val => {
		if (!val) {
			envFormRef.value.resetFields()
			envFormData.id = ''
			envFormData.name = ''
			envFormData.desc = ''
			envFormData.baseUrl = ''
		}
	},
)

// 暴露方法
defineExpose({
	open,
})
</script>

<template>
	<!-- 环境管理-modal -->
	<a-modal
		v-model:open="envManageFormData.visible"
		width="1300px"
		:title="`环境管理 - ${systemName}`"
		wrap-class-name="env-manage-modal-wrap"
	>
		<pay-table
			v-if="envManageFormData.visible"
			ref="envTableRef"
			:ajax="getSystemEnvList"
			:columns="envColumns"
			modal-table-name="envTableRef"
			class="env-manage-table"
		>
			<template #search>
				<div class="flex gap-3">
					<a-input
						v-model:value="envSearchName"
						placeholder="请输入环境名称"
						allowClear
						@change="changeEnvTableInput"
						@press-enter="() => envTableRef.getTableList(1)"
					></a-input>
					<a-button type="primary" @click="() => envTableRef.getTableList(1)">
						<i class="ri-search-line mr-1"></i>
						查询
					</a-button>
				</div>
				<a-button
					v-permission="'system.system.businessSystem.env.add'"
					type="primary"
					class="add-button"
					@click="envFormData.visible = true"
				>
					<i class="ri-add-line mr-1"></i>
					新增环境
				</a-button>
			</template>
		</pay-table>
		<template #footer></template>
	</a-modal>
	<!-- 环境-modal -->
	<a-modal
		v-model:open="envFormData.visible"
		:title="envFormData.id ? '编辑环境' : '新增环境'"
	>
		<a-form
			ref="envFormRef"
			:model="envFormData"
			:label-col="{ span: 4 }"
			:wrapper-col="{ span: 20 }"
			autocomplete="off"
		>
			<a-form-item
				label="环境名称"
				name="name"
				:rules="[{ required: true, message: '请输入环境名称！' }]"
			>
				<a-input
					v-model:value="envFormData.name"
					placeholder="请输入环境名称"
				/>
			</a-form-item>
			<a-form-item label="简介" name="desc">
				<a-textarea
					v-model:value="envFormData.desc"
					:auto-size="{ minRows: 3, maxRows: 3 }"
					placeholder="请输入简介"
				/>
			</a-form-item>
			<a-form-item
				label="系统基础地址"
				name="baseUrl"
				:rules="[{ required: true, message: '请输入系统基础地址！' }]"
			>
				<a-input
					v-model:value="envFormData.baseUrl"
					placeholder="请输入系统基础地址"
				/>
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="envFormData.visible = false">取消</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="envFormData.loading"
				@click="handleEnv"
			>
				提交
			</a-button>
		</template>
	</a-modal>
</template>

<style lang="scss">
.env-manage-modal-wrap {
	.ant-modal {
		max-height: 90vh;
		top: 5vh;
		padding-bottom: 0;
	}

	.ant-modal-content {
		max-height: 90vh;
		display: flex;
		flex-direction: column;
	}

	.ant-modal-body {
		flex: 1;
		overflow: hidden;
		display: flex;
		flex-direction: column;
		padding: 16px;
	}

	.env-manage-table.page-table
		.ant-table-wrapper
		.ant-table
		.ant-table-container
		.ant-table-body,
	.page-table.env-manage-table
		.ant-table-wrapper
		.ant-table
		.ant-table-container
		.ant-table-body,
	.env-manage-table
		.page-table
		.ant-table-wrapper
		.ant-table
		.ant-table-container
		.ant-table-body,
	.page-table .ant-table-body {
		min-height: 0 !important;
		max-height: none !important;
		height: auto !important;
	}

	.env-manage-table.page-table {
		height: 100% !important;
		display: flex !important;
		flex-direction: column !important;
		overflow: hidden !important;
		position: relative !important;

		> div:first-child {
			flex-shrink: 0 !important;
		}

		.page-table-search {
			flex-shrink: 0 !important;
		}

		.ant-table-wrapper {
			flex: 1 !important;
			overflow: hidden !important;
			display: flex !important;
			flex-direction: column !important;
			min-height: 0 !important;

			.ant-table {
				flex: 1 !important;
				display: flex !important;
				flex-direction: column !important;
				overflow: hidden !important;
				min-height: 0 !important;

				.ant-table-container {
					flex: 1 !important;
					overflow: hidden !important;
					display: flex !important;
					flex-direction: column !important;
					min-height: 0 !important;

					.ant-table-body {
						flex: 1 !important;
						overflow: auto !important;
						min-height: 0 !important;
						max-height: none !important;
						height: auto !important;
					}
				}
			}
		}

		.ant-pagination {
			flex-shrink: 0 !important;
			margin: 8px 0 0 0 !important;
		}
	}
}
</style>
