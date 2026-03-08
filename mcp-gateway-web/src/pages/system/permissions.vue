<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/permissions.vue
 * @Author: teddy
 * @Date: 2024-02-25 22:01:46
 * @Description: 权限管理
 * @LastEditors: tangchen 190854524@qq.com
 * @LastEditTime: 2024-06-24 20:13:19
-->
<script setup lang="tsx">
import { _deleteModal, _getPermissionsData, _tmeMsg } from '@/assets/js/util'
import { useStorage } from '@vueuse/core'
import {
	fetchAddPermission,
	fetchDeletePermission,
	fetchEditPermission,
} from '~/fetch/http'

// 所有权限点数据
const permissionsStorage = useStorage('pay-permissions-data', [])
const loading = ref(false)

// 用户登录历史信息
const userInfo = useStorage<any>('pay-user-info', {})

// 权限点-form
const permissionsFormRef = ref()
// 权限点-modal
const permissionsFormData = reactive({
	visible: false,
	loading: false,
	edit: false,
	name: '',
	rightkey: '',
	descript: '',
	tenant: userInfo.value.tenant === '0' ? undefined : userInfo.value.tenant,
})

// 权限点-编辑
const handlePermissionsEdit = (
	edit: boolean,
	name: string,
	rightkey: string,
	descript: string,
) => {
	permissionsFormData.visible = true
	nextTick(() => {
		permissionsFormData.edit = edit
		permissionsFormData.name = name
		permissionsFormData.rightkey = rightkey
		permissionsFormData.descript = descript
	})
}

// 权限点-删除
const handlePermissionsDelete = (rightkey: string, name: string) => {
	_deleteModal(
		'权限点',
		name,
		() => fetchDeletePermission(rightkey),
		() => _getPermissionsData(),
	)
}
// 权限点-提交
const handlePermissions = async () => {
	try {
		await permissionsFormRef.value.validate()
		if (permissionsFormData.edit) {
			await fetchEditPermission({
				name: permissionsFormData.name,
				rightkey: permissionsFormData.rightkey,
				descript: permissionsFormData.descript,
				tenant: permissionsFormData.tenant,
			})
		} else {
			await fetchAddPermission({
				name: permissionsFormData.name,
				rightkey: permissionsFormData.rightkey,
				descript: permissionsFormData.descript,
				tenant: permissionsFormData.tenant,
			})
		}

		permissionsFormData.visible = false
		_tmeMsg.success(
			permissionsFormData.edit ? '编辑权限点成功' : '添加权限点成功',
		)
		_getPermissionsData()
	} catch (error) {}
}

// 监听权限点-modal显示状态
watch(
	() => permissionsFormData.visible,
	val => {
		if (!val) {
			// 重置表单
			permissionsFormRef.value.resetFields()
			permissionsFormData.edit = false
		}
	},
)

// 监听权限点-数据变化，重置 loading 状态
watch(
	() => permissionsStorage.value,
	() => {
		loading.value = false
	},
)
onMounted(() => {
	_getPermissionsData()
	// 权限-获取所有权限点
	if (!permissionsStorage.value.length) {
		loading.value = true
	}
})
</script>

<template>
	<div class="flex items-center justify-between">
		<a-button
			v-permission="'system.system.permissions.add'"
			type="primary"
			class="add-button"
			@click="permissionsFormData.visible = true"
		>
			<template #icon>
				<i class="ri-add-line mr-1"></i>
			</template>
			新增权限点
		</a-button>
		<a-alert
			message="权限管理为严格的管控手段，非开发人员请勿做任何修改！编辑与删除为右键权限点。"
			type="warning"
			show-icon
		/>
	</div>
	<a-divider class="!mt-2 !mb-2" />
	<a-spin :spinning="loading">
		<a-tree
			v-if="permissionsStorage.length"
			:tree-data="permissionsStorage"
			defaultExpandAll
			:show-line="{
				showLeafIcon: false,
			}"
		>
			<template #title="{ title, rightkey, descript, children }">
				<a-dropdown :trigger="['contextmenu']">
					<span>{{ title }}-{{ rightkey }}</span>
					<template v-if="rightkey !== 'system.superAdmin'" #overlay>
						<a-menu>
							<div v-permission="'system.system.permissions.edit'">
								<a-menu-item
									key="edit"
									@click="
										handlePermissionsEdit(true, title, rightkey, descript)
									"
								>
									编辑权限点
								</a-menu-item>
							</div>
							<div v-permission="'system.system.permissions.delete'">
								<a-menu-item
									v-if="children.length === 0"
									key="delete"
									@click="handlePermissionsDelete(rightkey, title)"
								>
									删除权限点
								</a-menu-item>
							</div>
						</a-menu>
					</template>
				</a-dropdown>
			</template>
		</a-tree>
	</a-spin>
	<!-- 权限点-modal -->
	<a-modal
		v-model:open="permissionsFormData.visible"
		:title="permissionsFormData.edit ? '编辑权限点' : '新增权限点'"
	>
		<a-form
			ref="permissionsFormRef"
			:model="permissionsFormData"
			:label-col="{ span: 4 }"
			:wrapper-col="{ span: 20 }"
			autocomplete="off"
		>
			<a-form-item
				label="权限名称"
				name="name"
				:rules="[{ required: true, message: '请输入权限名称！' }]"
			>
				<a-input
					v-model:value="permissionsFormData.name"
					placeholder="请输入权限名称"
				/>
			</a-form-item>
			<a-form-item
				label="权限 key"
				name="rightkey"
				:rules="[
					{ required: true, message: '请输入权限 key！如：system.xx.xx' },
				]"
			>
				<a-input
					v-model:value="permissionsFormData.rightkey"
					:disabled="permissionsFormData.edit ? true : false"
					placeholder="请输入权限 key，如：system.xx.xx"
				/>
			</a-form-item>
			<a-form-item
				label="权限描述"
				name="descript"
				:rules="[{ required: true, message: '请输入权限描述！' }]"
			>
				<a-textarea
					v-model:value="permissionsFormData.descript"
					:auto-size="{ minRows: 3, maxRows: 3 }"
					placeholder="请输入权限描述"
				/>
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="permissionsFormData.visible = false">
				取消
			</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="permissionsFormData.loading"
				@click="handlePermissions"
			>
				提交
			</a-button>
		</template>
	</a-modal>
</template>

<style lang="scss"></style>
