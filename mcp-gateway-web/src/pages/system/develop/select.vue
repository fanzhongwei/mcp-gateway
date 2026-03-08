<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/develop/select.vue
 * @Author: teddy
 * @Date: 2024-03-01 16:22:20
 * @Description: 开发工具-select
 * @LastEditors: teddy
 * @LastEditTime: 2024-05-06 09:58:18
-->
<script setup lang="tsx">
import { VueDraggableNext } from 'vue-draggable-next'
import { _deepClone, _deleteModal, _tmeMsg } from '~/assets/js/util'
import {
	fetchCodeList,
	fetchCodePage,
	fetchCodeTypePage,
	fetchCodeTypeUpdate,
} from '~/fetch/http'

const tableRef = ref()
// 码值
const codeData = reactive<any>({
	data: [],
	code: [],
	codeVal: undefined,
	codeName: '',
	sonVal: undefined,
})
// 获取码值数据
const getcodeData = async (code: number) => {
	try {
		const { data } = await fetchCodeList(code)
		codeData.data = data
	} catch (error) {}
}

// 码值变化
const codeChange = (val: number, _option: any) => {
	codeData.sonVal = null
	codeData.code.map((item: any) => {
		if (item.code === val) {
			codeData.codeName = item.name
		}
	})
	getcodeData(val)
}

// table data
const tableData = reactive<any>({
	name: undefined,
	visible: false,
})

const changeTableInput = () => {
	if (!tableData.name) {
		tableRef.value.getTableList(1)
	}
}

// 代码值--分页查询接口
const getSelectList = async (page: any) => {
	try {
		const codeType = await fetchCodeTypePage()
		const codeTypeMap: any = {}
		codeType.data.records.map((item: any) => {
			// 右侧 parent 数据
			codeTypeMap[item.idTranslateText] = {
				name: item.name,
				key: item.id,
				code: item.id,
				children: [],
				active: !!item.editable,
			}
			// 左侧 select
			codeData.code.push({
				name: item.name,
				code: item.id,
			})
		})
		const res = await fetchCodePage({
			...page,
			entityParam: {
				name: tableData.name,
			},
		})

		// 拆分一维数组为二维数组，children
		const data = [...res.data.records]
		const map: any = {}
		const list: any = []

		data.map((item: any) => {
			if (!map[item.pid]) {
				map[item.pid] = {
					code: item.pid,
					id: item.pid,
					key: item.pid,
					name: codeTypeMap[item.pid],
					children: [],
				}
				map[item.pid].children.push(item)
			} else {
				map[item.pid].children.push(item)
			}
		})

		// 根据 map, list 生成二维数据
		Object.keys(codeTypeMap).map((item: any) => {
			list.push({ ...codeTypeMap[item], children: map[item]?.children || [] })
		})
		res.data.records = list
		return Promise.resolve(res)
	} catch (error) {
		return Promise.reject(error)
	}
}

// 编辑码值-data
const codeEditData = reactive<any>({
	loading: false,
	visible: false,
	data: [],
	addVisible: false,
	editIndex: undefined,
	name: '',
	code: '',
	pid: '',
})
// 编辑码值
const handleEdit = async () => {
	try {
		codeEditData.loading = true
		await fetchCodeTypeUpdate(codeEditData.pid, codeEditData.data.children)
		_tmeMsg.success('编辑码值成功')
		codeEditData.visible = false
		codeEditData.loading = false
		tableRef.value.getTableList(1)
	} catch (error) {
		codeEditData.loading = false
	}
}
// 编辑码值-添加码值-ref
const codeEditDataAddRef = ref()
// 编辑码值-添加码值
const handleEditAdd = async () => {
	try {
		await codeEditDataAddRef.value.validate()
		if (codeEditData.editIndex === undefined) {
			codeEditData.data.children.push({
				name: codeEditData.name + '',
				code: codeEditData.code + '',
				pid: codeEditData.pid,
			})
		} else {
			codeEditData.data.children[codeEditData.editIndex].name =
				toRaw(codeEditData.name) + ''
			codeEditData.data.children[codeEditData.editIndex].code = toRaw(
				codeEditData.code,
			)
		}
		codeEditData.addVisible = false
		_tmeMsg.success('已提交在内存中，请在排序后提交保存！')
	} catch (error) {}
}
// 编辑码值-编辑
const codeEditEdit = (data: any, index: number) => {
	codeEditData.name = data.name
	codeEditData.code = data.code
	codeEditData.editIndex = index
	codeEditData.addVisible = true
}
// 编辑码值-删除-index
const codeEditDelete = (data: any, index: any) => {
	_deleteModal(
		'码值',
		data.name,
		() => codeEditData.data.children.splice(index, 1),
		() => {},
	)
}

// th 参数
const columns = reactive([
	{
		title: '代码名称',
		dataIndex: 'name',
	},
	{
		title: '代码值',
		dataIndex: 'code',
	},
	{
		title: '操作',
		width: 100,
		dataIndex: 'active',
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div class='flex'>
					{data.active && (
						<a-button
							size='small'
							v-permission='system.setting.role.edit'
							onClick={() => {
								codeEditData.data = _deepClone(data)
								codeEditData.pid = data.code
								codeEditData.visible = true
							}}
						>
							<i class='ri-edit-box-line mr-1'></i>
							编辑
						</a-button>
					)}
				</div>
			)
		},
	},
])

watch(
	() => codeEditData.addVisible,
	newValue => {
		if (!newValue) {
			codeEditData.name = ''
			codeEditData.code = ''
			codeEditData.editIndex = null
			codeEditDataAddRef.value.resetFields()
		}
	},
)
</script>

<template>
	<div class="flex gap-3">
		<div class="w-1/4">
			<a-alert type="info" show-icon message="阅读代码查看使用方法。" />
			<a-button
				class="!font-bold"
				type="link"
				target="_blank"
				href="https://antdv.com/components/select-cn"
			>
				可参考 UI 组件库文档地址
			</a-button>
			<a-card
				:title="codeData.codeVal ? '码值:' + codeData.codeVal : '请选择码值'"
				:bordered="false"
			>
				<!-- select 变化，触发 table 查询 -->
				<!-- @change="() => tableRef.getTableList(1)" -->
				<a-select
					v-model:value="codeData.codeVal"
					class="block w-52"
					allowClear
					placeholder="请选择码值"
					@change="codeChange"
				>
					<a-select-option
						v-for="item in codeData.code"
						:key="item.code"
						:value="item.code"
					>
						{{ item.name }}
					</a-select-option>
				</a-select>
			</a-card>
			<a-card
				:title="
					codeData.codeVal
						? `${codeData.codeName}，码值：` +
							codeData.codeVal +
							(codeData.sonVal ? `，子码值：` + codeData.sonVal : '')
						: '--'
				"
				:bordered="false"
			>
				<!-- select 变化，触发 table 查询 -->
				<!-- @change="() => tableRef.getTableList(1)" -->
				<a-select
					v-model:value="codeData.sonVal"
					class="block w-52"
					allowClear
					:placeholder="codeData.codeVal ? `请选择${codeData.codeName}` : '--'"
					:disabled="!codeData.codeVal"
				>
					<a-select-option
						v-for="item in codeData.data"
						:key="item.code"
						:value="item.code"
					>
						{{ item.name }}
					</a-select-option>
				</a-select>
			</a-card>
		</div>
		<div class="w-3/4">
			<pay-table
				ref="tableRef"
				:ajax="getSelectList"
				:columns="columns"
				:pagination="false"
			>
				<template #search>
					<div class="flex gap-3">
						<a-input
							v-model:value="tableData.name"
							placeholder="请输入代码名称"
							allowClear
							@press-enter="() => tableRef.getTableList(1)"
							@change="changeTableInput"
						></a-input>
						<a-button type="primary" @click="() => tableRef.getTableList(1)">
							<i class="ri-search-line mr-1"></i>
							查询
						</a-button>
					</div>
				</template>
			</pay-table>
		</div>
	</div>
	<!-- 编辑码值 -->
	<a-modal v-model:open="codeEditData.visible" title="编辑码值" :width="360">
		<div>
			<a-divider orientation="right">
				<i class="ri-arrow-up-down-line"></i>
				拖拽排序
			</a-divider>
			<VueDraggableNext :list="codeEditData.data.children">
				<div
					v-for="(element, key) in codeEditData.data.children"
					:key="element.name"
					class="m-1 p-3 pt-1 pb-1 rounded-md text-center border-tme border-solid border flex justify-between hover:bg-tme/50"
				>
					<div>名称：{{ element.name }}</div>
					<div>码值：{{ element.code }}</div>
					<div class="flex gap-3">
						<i
							class="ri-edit-box-line cursor-pointer hover:text-white"
							@click="codeEditEdit(element, key)"
						></i>
						<i
							class="ri-delete-bin-line cursor-pointer hover:text-white"
							@click="codeEditDelete(element, key)"
						></i>
					</div>
				</div>
			</VueDraggableNext>
		</div>
		<a-divider />
		<template #footer>
			<div class="flex justify-between">
				<a-button type="primary" @click="codeEditData.addVisible = true">
					<i class="ri-add-line mr-1"></i>
					新增码值
				</a-button>
				<div>
					<a-button
						key="back"
						:disabled="codeEditData.loading"
						@click="codeEditData.visible = false"
					>
						取消
					</a-button>
					<a-button
						key="submit"
						type="primary"
						:loading="codeEditData.loading"
						@click="handleEdit"
					>
						提交
					</a-button>
				</div>
			</div>
		</template>
	</a-modal>
	<!-- 新增码值  -->
	<a-modal v-model:open="codeEditData.addVisible" title="新增码值" :width="360">
		<a-form
			ref="codeEditDataAddRef"
			:model="codeEditData"
			:label-col="{ span: 6 }"
			:wrapper-col="{ span: 18 }"
			autocomplete="off"
		>
			<a-form-item
				label="代码名称"
				name="name"
				:rules="[{ required: true, message: '请输入代码名称！' }]"
			>
				<a-input
					v-model:value="codeEditData.name"
					placeholder="请输入代码名称"
				/>
			</a-form-item>
			<a-form-item
				label="代码值"
				name="code"
				:rules="[{ required: true, message: '请输入代码值！' }]"
			>
				<a-input v-model:value="codeEditData.code" placeholder="请输入代码值" />
			</a-form-item>
		</a-form>
		<template #footer>
			<div class="flex justify-between">
				<div>
					<a-button key="back" @click="codeEditData.addVisible = false">
						取消
					</a-button>
					<a-button key="submit" type="primary" @click="handleEditAdd">
						提交
					</a-button>
				</div>
			</div>
		</template>
	</a-modal>
</template>

<style lang="scss"></style>
