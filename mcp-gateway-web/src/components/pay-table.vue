<!--
 * @FilePath: /mcp-gateway-web/src/components/pay-table.vue
 * @Author: teddy
 * @Date: 2024-02-21 21:25:02
 * @Description: table 组件
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-09 14:20:19
-->
<script setup lang="tsx">
import { useStorage } from '@vueuse/core'
import { message } from 'ant-design-vue'
import { _deepClone, _tmeMsg } from '~/assets/js/util'

const props = defineProps({
	// 表格数据
	ajax: {
		type: Function,
		default: () => {},
	},
	// 表格列配置
	columns: {
		type: Array,
		default: () => [],
	},
	// 是否拥有子表格
	isSonTable: {
		type: Boolean,
		default: false,
	},
	// 子表格列信息
	sonTableColumns: {
		type: Array,
		default: () => [],
	},
	// 是否分页
	pagination: {
		type: Boolean,
		default: true,
	},
	// 导出功能
	exportObj: {
		type: Object,
		default: () => ({
			exportName: '',
			exportType: 'xlsx',
			exportMethod: () => {},
		}),
	},
	// 静态数据，不请求 ajax 和 分页
	parentData: {
		type: Array,
		default: () => [],
	},
	// 是否加载自动请求
	isAjax: {
		type: Boolean,
		default: true,
	},
	// 是否开启选择，默认 false
	isRowSelection: {
		type: Boolean,
		default: false,
	},
	// modalTableName，方便弹窗控制 th 列
	modalTableName: {
		type: String,
		default: '',
	},
	// 是否是小页面 table
	isPageTableMin: {
		type: Boolean,
		default: false,
	},
	// 外部传入的表格 body 滚动高度（与 tableScrollUtils 配合使用，仅表格区域滚动）
	scrollY: {
		type: Number,
		default: undefined,
	},
})
// route
const route: any = useRoute()
// 版本
const version = useCookie('version')

const tableData: any = defineModel('value', {
	type: Array,
	default: [],
})

// table storage
const tableStorage = useStorage<any>(
	`pay-table-${route.name}${props.modalTableName}`,
	{
		list: [],
		values: [],
		columns: [],
		checkAll: false,
		indeterminate: false,
		init: false, // 是否设置过 th
		version: '',
	},
)

// table-列显示控制-设置筛选的默认值
const columnsCheckbox = reactive<any>({
	list: [],
	values: [],
	columns: [],
	checkAll: false,
	indeterminate: false,
})
// table-列显示控制-全选监听
const onCheckAllChange = () => {
	columnsCheckbox.indeterminate = false
	if (columnsCheckbox.checkAll) {
		columnsCheckbox.values = columnsCheckbox.list.map(
			(item: any) => item.dataIndex,
		)
	} else {
		columnsCheckbox.values = []
	}
}
// table-设置列数据-设置 th 数据
const setColumnsCheckbox = (checkboxList = [], all = false) => {
	// table-列显示控制-获取所有需要筛选的字段，customFilterDropdown = true 的字段强制显示
	// table-列显示控制-赛选列数据默认全选
	// table-每次进入都设置一次 th 所有列数据，便于为对来新增的 th 数据做默认显示操作
	// table-重置列数据
	columnsCheckbox.columns = []
	// table-版本升级后，重置 th 选项为全选
	if (version.value !== tableStorage.value.version) {
		columnsCheckbox.list = []
		columnsCheckbox.values = []
	}
	props.columns.map((item: any, key: number) => {
		// 优化 td 默认的省略、拖拽宽度、最小拖拽宽度、默认宽度等属性
		item = {
			ellipsis: true,
			resizable: true,
			minWidth: 120,
			width: 120,
			...item,
		}
		// table-key === 0 th，固定左侧
		if (key === 0) item = { ...item, fixed: 'left' }
		// 序号/操作，禁止拖动列宽度
		if (item.dataIndex === 'index' || item.dataIndex === 'active') {
			item = {
				...item,
				resizable: false,
			}
		}
		// table-active 操作 th，固定右侧
		if (item.dataIndex === 'active') {
			item = {
				...item,
				fixed: 'right',
				// table-显示赛选功能的字段，对于需要筛选的字段，需要设置该属性，true 该字段强制显示，否则筛选按钮不会显示
				customFilterDropdown: true,
			}
		}
		// 气泡提示框
		if (
			item.dataIndex !== 'index' &&
			item.dataIndex !== 'active' &&
			!item.customRender
		) {
			item = {
				...item,
				customRender: (row: any) => {
					// {/* getPopupContainer={(triggerNode: any) => triggerNode.parentNode} */}
					const msg = row.record[row.column.dataIndex]
					return msg ? (
						<a-popover
							overlayClassName='max-w-96 break-all pay-table-popover'
							autoAdjustOverflow={false}
							title={row.column.title}
							content={msg}
							placement='topLeft'
						>
							<div class='truncate'>{msg}</div>
						</a-popover>
					) : (
						'-'
					)
				},
			}
		}
		// 时间字段宽度加宽
		if (item.dataIndex.includes('Time')) {
			item = {
				...item,
				width: 160,
				minWidth: 160,
			}
		}
		// 多排序代码
		// sorter: {
		// 	multiple: 1,
		// },

		if (item.customFilterDropdown || item.dataIndex === 'active') {
			columnsCheckbox.columns.push({
				...item,
			})
		} else if (all || version.value !== tableStorage.value.version) {
			// table-设置列数据-所有列数据
			columnsCheckbox.columns.push(item)
			// table-设置列数据-可选择的列数据
			columnsCheckbox.list.push(item)
			// table-设置列数据-已选数据
			columnsCheckbox.values.push(item.dataIndex)
			// table-设置列数据-默认全选
			columnsCheckbox.checkAll = true
		} else {
			checkboxList.map((key: string) => {
				if (key === item.dataIndex) {
					columnsCheckbox.columns.push(item)
				}
			})
		}
	})
	// table-更新 table 版本
	tableStorage.value.version = version.value
}
// table-列显示控制-根据选择
// table-设置列数据
// table-动态赋值全选状态
watch(
	() => columnsCheckbox.values,
	checkboxList => {
		if (version.value === tableStorage.value.version) {
			// table-设置列数据
			setColumnsCheckbox(checkboxList)
		}
		// table-动态赋值全选状态
		if (columnsCheckbox.values.length > 0) {
			columnsCheckbox.indeterminate = true
			if (columnsCheckbox.values.length === columnsCheckbox.list.length) {
				columnsCheckbox.checkAll = true
				columnsCheckbox.indeterminate = false
			} else {
				columnsCheckbox.checkAll = false
			}
		} else {
			columnsCheckbox.indeterminate = false
		}
		// table-赋值历史数据
		tableStorage.value.init = true
		tableStorage.value.list = columnsCheckbox.list
		tableStorage.value.values = columnsCheckbox.values
		tableStorage.value.checkAll = columnsCheckbox.checkAll
		tableStorage.value.indeterminate = columnsCheckbox.indeterminate
	},
)
// 选择的列 keys
const rowKeys = ref<any>([])
// 选择的列 objs
const rowObjs = ref<any>([])

// table-数据
const dataSource = reactive<any>({
	loading: false,
	list: [],
	y: 0,
	// 排序
	sorter: [],
	// pagination
	pagination: {
		// pagination-是否展示 pageSize 切换器，当 total 大于 50 时默认为 true
		showSizeChanger: true,
		// pagination-每页条数
		pageSize: 10,
		// pagination-当前页数
		current: 1,
		// pagination-数据总数
		total: 0,
		// pagination-当为「small」时，是小尺寸分页
		size: 'large',
		// pagination-指定每页可以显示多少条
		pageSizeOptions: ['20', '50', '100'],
		// pagination-页码或 pageSize 改变的回调，参数是改变后的页码及每页条数
		onChange: (current: number, size: number) => {
			if (dataSource.pagination.pageSize !== size) {
				dataSource.pagination.pageSize = size
				getTableList(1)
			} else {
				getTableList(current)
			}
		},
		// pagination-用于显示数据总量和当前数据顺序
		showTotal: (_total: string, range: any) =>
			`共 ${dataSource.pagination.total} 条，当前显示第 ${range[0]} 条至 ${range[1]} 条`,
	},
})
// 排序控制
const orders = ref<any>([])
// table-获取数据，// 清空选择的列 checkbox key
const getTableList = async (current = 1, clearKey = false) => {
	try {
		// 清空选择的列 key
		if (clearKey) {
			rowKeys.value = []
			rowObjs.value = []
		}
		if (dataSource.loading) return
		dataSource.loading = true
		dataSource.pagination.current = current

		// 排序控制
		orders.value = []
		if (dataSource.sorter && dataSource.sorter.length) {
			if (Array.isArray(dataSource.sorter)) {
				// 当排序数据是数组时
				dataSource.sorter.map((item: any) => {
					orders.value.push({
						column: item.field,
						asc: item.order === 'ascend',
					})
				})
			} else {
				// 当排序数据是对象时
				orders.value.push({
					column: dataSource.sorter.field,
					asc: dataSource.sorter.order === 'ascend',
				})
			}
		}
		const { data } = await props.ajax({
			page: {
				current: dataSource.pagination.current,
				size: dataSource.pagination.pageSize,
				orders: orders.value,
			},
		})
		// table-设置 index 根据 current 和 size 计算
		dataSource.list = data.records.map((item: any, key: number) => ({
			...item,
			index: key + 1 + (current - 1) * dataSource.pagination.pageSize,
			key: item.id || key + 1 + (current - 1) * dataSource.pagination.pageSize,
		}))
		dataSource.pagination.total = data.total
		dataSource.loading = false
		exportData.isDisabled = dataSource.list.length === 0
		tableData.value = dataSource.list
	} catch (error) {
		dataSource.loading = false
	}
}

// table-change
const tableChange = (_pagination: any, _filters: any, sorter: any) => {
	dataSource.sorter = []
	columnsCheckbox.columns.map((item: any) => {
		if (Array.isArray(sorter)) {
			// 排序多个，是数组时
			sorter.map((target: any) => {
				// 控制 th 变化
				if (target.field === item.dataIndex) {
					item.order = target.order
				}
			})
		} else if (item.dataIndex === sorter.field) {
			// 排序只有1个时，非数组时
			// 控制 th 变化
			item.order = sorter.order
		} else {
			// 排序只有1个时，非数组时，其他的字段排序均设为 undefined
			item.order = undefined
		}
	})
	if (Array.isArray(sorter)) {
		sorter.map((target: any) => {
			// 控制 ajax 参数
			dataSource.sorter.push({
				field: target.field,
				order: target.order,
			})
		})
	} else if (sorter.order) {
		// 控制 ajax 参数
		dataSource.sorter.push({
			field: sorter.field,
			order: sorter.order,
		})
	}
	getTableList(1)
}

// 拖动宽度
const handleResizeColumn = (w: any, col: { width: any }) => {
	col.width = w
}

// 导出数据
const exportData = reactive<any>({
	visible: false,
	loading: false,
	checkAll: false,
	indeterminate: false,
	isAll: false,
	list: [],
	select: [],
	map: {},
	isDisabled: true,
})
// 导出列确认方法
const handleSelectExport = () => {
	exportData.list = []
	exportData.select = []
	exportData.map = {}
	// 生产可选择的导出列字段
	_deepClone(props.columns).map((item: any) => {
		if (item.dataIndex !== 'index' && item.dataIndex !== 'active') {
			exportData.list.push(item)
			exportData.select.push(item.dataIndex)
			exportData.map[item.dataIndex] = item
		}
	})
	exportData.visible = true
	exportData.isAll = false
	exportData.checkAll = true
}
// 确认导出
const handleExport = async () => {
	try {
		if (exportData.loading) return
		exportData.loading = true
		message.loading({
			content: `下载<${props.exportObj.exportName}>中...`,
			key: 'export',
			duration: 0,
		})
		const exportColumns: any = []
		exportData.select.map((item: any) => {
			exportColumns.push({
				name: exportData.map[item].dataIndex,
				title: exportData.map[item].title,
			})
		})
		// 存储 ajax 请求带 entityParam 参数的数据
		const entityParam = useState('entity-param')
		const res = await props.exportObj.exportMethod({
			exportExcelParam: {
				columns: exportColumns,
				isAll: exportData.isAll,
				fileName: props.exportObj.exportName,
				title: props.exportObj.exportName,
				sheetName: props.exportObj.exportName,
			},
			entityParam: entityParam.value || undefined,
			page: {
				current: dataSource.pagination.current,
				size: dataSource.pagination.pageSize,
				orders: orders.value,
			},
		})

		// 生成文件流
		const xlsUrl = window.URL.createObjectURL(new Blob([res.data]))
		// 创建下载对象
		const xlsA = document.createElement('a')
		// 隐藏下载显示
		xlsA.style.display = 'none'
		// 赋值下载链接
		xlsA.href = xlsUrl
		// 设置 download name
		xlsA.setAttribute(
			'download',
			`${props.exportObj.exportName}.${props.exportObj.exportType || 'xlsx'}`,
		)
		// 添加下载对象
		document.body.appendChild(xlsA)
		// 执行下载
		xlsA.click()
		// 释放下载对象
		window.URL.revokeObjectURL(xlsA.href)
		// 删除下载创建的临时标签
		document.body.removeChild(xlsA)
		_tmeMsg.success('导出成功', 'export')
		exportData.loading = false
		exportData.visible = false
	} catch (error) {
		exportData.loading = false
		// _tmeMsg.error('导出失败', 'export')
	}
}

// 导出列变化
const onExportCheckAllChange = (e: any) => {
	Object.assign(exportData, {
		select: e.target.checked ? exportData.list : [],
		indeterminate: false,
	})
	if (exportData.checkAll) {
		exportData.select = []
		exportData.list.map((item: any) => {
			exportData.select.push(item.dataIndex)
		})
	} else {
		exportData.select = []
	}
}

// 选择方法
const rowSelection = {
	preserveSelectedRowKeys: true,
	selectedRowKeys: rowKeys,
	onChange: (selectedRowKeys: any, selectedRows: any) => {
		rowKeys.value = selectedRowKeys
		rowObjs.value = selectedRows
	},
	getCheckboxProps: (record: any) => ({
		// disabled: !record.disabled,
		props: {
			disabled: !record.disabled,
		},
	}),
}

watch(
	() => exportData.select,
	val => {
		exportData.indeterminate =
			!!val.length && val.length < exportData.list.length
		exportData.checkAll = val.length === exportData.list.length
	},
)
// table-导出组件-暴露方法
defineExpose({
	// 查询方法
	getTableList,
	// 选择的列 key
	rowKeys,
	// 选择的列 obj
	rowObjs,
})

onMounted(() => {
	// table-获取屏幕高度；若外部传入 scrollY 则不再内部计算
	const screenHeight = window.innerHeight
	if (props.scrollY == null) {
		// table-设置 table-body 最大高度控制
		dataSource.y = screenHeight - 320
	} else {
		dataSource.y = props.scrollY
	}
	// table-获取 table 距离顶部的高度
	const element = document.querySelector('#aTable')
	const distanceToTop = (element && element.getBoundingClientRect().top) || 0
	// 当是弹窗 modal table 时，不计算高度，默认展示每页 10 条数据
	if (props.modalTableName) {
		dataSource.pagination.pageSize = 10
		// pagination-设置指定每页可以显示多少条
		dataSource.pagination.pageSizeOptions.unshift('10')
	} else {
		// pagination-每页最佳 size 数
		const pageSize =
			Math.floor(
				(screenHeight -
					distanceToTop -
					55 -
					32 -
					16 * 2 -
					(props.isPageTableMin ? 62 : 0)) /
					50,
			) || 10
		// pagination-设置每页条数
		dataSource.pagination.pageSize = pageSize
		// pagination-设置指定每页可以显示多少条
		dataSource.pagination.pageSizeOptions.unshift(pageSize + '')
	}

	// table-列显示控制-当有 storage 历史数据时，进行回显，添加版本控制
	if (tableStorage.value.init && version.value === tableStorage.value.version) {
		columnsCheckbox.list = tableStorage.value.list
		columnsCheckbox.values = tableStorage.value.values
		columnsCheckbox.checkAll = tableStorage.value.checkAll
		columnsCheckbox.indeterminate = tableStorage.value.indeterminate
	} else {
		// table-设置列数据
		setColumnsCheckbox([], true)
	}
	// table-ajax
	if (props.parentData.length === 0 && props.isAjax) {
		nextTick(() => {
			getTableList()
		})
	} else {
		tableData.value = props.parentData
	}
})
</script>

<template>
	<div
		class="page-table"
		:class="{
			'page-table-min': isPageTableMin,
		}"
	>
		<div class="flex justify-between">
			<div class="mb-2 flex justify-between page-table-search grow">
				<slot name="search" />
			</div>
			<div
				v-if="exportObj.exportName"
				class="w-24 text-right export-search-btn"
			>
				<a-button
					:disabled="exportData.isDisabled"
					type="primary"
					@click="handleSelectExport"
				>
					<i class="ri-upload-2-line mr-1"></i>
					导出
				</a-button>
			</div>
		</div>

		<a-table
			id="aTable"
			:rowSelection="isRowSelection ? rowSelection : null"
			:columns="columnsCheckbox.columns"
			:data-source="parentData.length ? parentData : dataSource.list"
			:loading="dataSource.loading"
			:pagination="
				pagination && !parentData.length ? dataSource.pagination : false
			"
			:scroll="{ y: props.scrollY != null ? props.scrollY : dataSource.y }"
			size="middle"
			:row-class-name="
				(_record: any, index: number) =>
					index % 2 === 1 ? 'table-striped' : null
			"
			:expand-column-width="100"
			@resize-column="handleResizeColumn"
			@change="tableChange"
		>
			<!-- 自定义表头样式 -->
			<template #headerCell="{ column, title }">
				<template v-if="column.sorter">
					<div class="flex justify-between">
						<div class="select-none">{{ title }}</div>
						<i v-if="!column.order" class="ri-arrow-up-down-line"></i>
						<i
							v-else-if="column.order === 'ascend'"
							class="ri-arrow-up-line text-tme"
						></i>
						<i
							v-else-if="column.order === 'descend'"
							class="ri-arrow-down-line text-tme"
						></i>
					</div>
				</template>
			</template>
			<!-- 自定义赛选 -->
			<template #customFilterDropdown>
				<div class="p-3">
					<div>
						<i class="ri-quote-text"></i>
						列显示控制
					</div>
					<a-divider class="!mt-1 !mb-1" />
					<a-checkbox
						v-model:checked="columnsCheckbox.checkAll"
						class="!flex select-none"
						:indeterminate="columnsCheckbox.indeterminate"
						@change="onCheckAllChange"
					>
						全选
					</a-checkbox>
					<a-divider class="!mt-1 !mb-1" />
					<div
						class="columns-checkbox-group"
						:class="{
							'!overflow-y-scroll': columnsCheckbox.list.length > 25,
						}"
					>
						<a-checkbox-group
							v-model:value="columnsCheckbox.values"
							class="!block"
						>
							<template
								v-for="(item, index) in columnsCheckbox.list"
								:key="index"
							>
								<a-checkbox
									v-if="!item.customFilterDropdown"
									:key="item.dataIndex"
									class="!flex select-none"
									:value="item.dataIndex"
								>
									{{ item.title }}
								</a-checkbox>
							</template>
						</a-checkbox-group>
					</div>
				</div>
			</template>
			<!-- 自定义筛选图标 -->
			<template #customFilterIcon>
				<i class="ri-quote-text"></i>
			</template>
			<!--如果存在子表格，自定义子表格操作列-->
			<template v-if="isSonTable" #bodyCell="{ column }">
				<template v-if="column.key === 'operation'">
					<a>{{ column }}</a>
				</template>
			</template>
			<!--如果存在子表格，自定义子表格展开列-->
			<template v-if="isSonTable" #expandColumnTitle>
				<span>餐段信息</span>
			</template>
			<!--如果存在子表格，自定义子表格内容-->
			<template v-if="isSonTable" #expandedRowRender="{ record }">
				<a-table
					:columns="sonTableColumns"
					:data-source="[
						{ ...record.mealPeriodParamsOne, mealPeriodName: '餐段参数1' },
						{ ...record.mealPeriodParamsTwo, mealPeriodName: '餐段参数2' },
						{ ...record.mealPeriodParamsThree, mealPeriodName: '餐段参数3' },
						{ ...record.mealPeriodParamsFour, mealPeriodName: '餐段参数4' },
						{ ...record.mealPeriodParamsFive, mealPeriodName: '餐段参数5' },
					]"
					:pagination="false"
				>
					<template #bodyCell="{ column, text }">
						<template v-if="column.dataIndex === 'name'">
							{{ text.first }} {{ text.last }}
						</template>
					</template>
				</a-table>
			</template>
		</a-table>
		<div
			v-if="!dataSource.loading && dataSource.list.length"
			class="pay-table-total"
		>
			<slot name="total" />
		</div>
	</div>
	<!-- 导出列确认弹窗 -->
	<a-modal v-model:open="exportData.visible" title="请选择需要导出的字段：">
		<div>
			<div class="flex">
				<div class="w-20 export-modal-left">
					<a-checkbox
						v-model:checked="exportData.checkAll"
						class="select-none"
						:indeterminate="exportData.indeterminate"
						@change="onExportCheckAllChange"
					>
						全选
					</a-checkbox>
				</div>
				<div class="flex-1">
					<a-checkbox-group v-model:value="exportData.select">
						<template v-for="(item, index) in exportData.list" :key="index">
							<a-checkbox class="select-none" :value="item.dataIndex">
								{{ item.title }}
							</a-checkbox>
						</template>
					</a-checkbox-group>
				</div>
			</div>
		</div>
		<template #footer>
			<div class="flex justify-between">
				<a-switch
					v-model:checked="exportData.isAll"
					:checked-children="
						'导出全部，共' + dataSource.pagination.total + '条数据'
					"
					un-checked-children="导出当前页数据"
				/>
				<div>
					<a-button
						key="back"
						:disabled="exportData.loading"
						@click="exportData.visible = false"
					>
						取消
					</a-button>
					<a-button
						key="submit"
						type="primary"
						:loading="exportData.loading"
						@click="handleExport"
					>
						提交
					</a-button>
				</div>
			</div>
		</template>
	</a-modal>
</template>
<style lang="scss">
.page-table {
	position: relative;
	// table body 最小高度控制
	.ant-table-body {
		min-height: calc(100vh - 300px);
		overflow: auto !important;
	}

	&.page-table-min {
		// table body 最小高度控制
		.ant-table-body {
			min-height: calc(100vh - 360px);
		}
	}
	// 查询条件区域样式优化
	.page-table-search {
		.ant-select,
		.ant-input,
		.ant-input-affix-wrapper {
			width: 208px;
		}
	}
	// 分页样式优化
	.ant-pagination {
		margin: 8px 0 0 0 !important;
	}
	// 隐藏默认排序图标
	.ant-table-column-sorter {
		display: none;
	}
	// total
	.pay-table-total {
		position: absolute;
		bottom: 0;
		left: 0;
	}

	td {
		line-height: 24px;
	}
}
// 筛选区域最高高度
.columns-checkbox-group {
	max-height: 500px;
	overflow: hide;
}
// 导出-btn
.export-search-btn {
	position: relative;
	&::before {
		content: '';
		position: absolute;
		display: block;
		width: 2px;
		height: 60%;
		left: 8px;
		top: 10%;
		background-color: #e1e1e1;
	}
}
// 导出-modal-left
.export-modal-left {
	position: relative;
	&::before {
		content: '';
		position: absolute;
		display: block;
		width: 2px;
		height: 90%;
		right: 12px;
		top: 5%;
		background-color: #e1e1e1;
	}
}
// 斑马纹
.table-striped {
	td {
		background: #fafafa !important;
	}
}
.dark {
	.table-striped {
		// :not(.ant-table-cell-fix-right):not(.ant-table-cell-fix-left)
		td {
			background: #000 !important;
		}
	}
}

// 气泡弹窗
.pay-table-popover {
	.ant-popover-inner-content {
		max-height: 150px;
		overflow: auto;
	}
}

.ant-table-cell-fix-right-first {
	right: 0 !important;
}
</style>
