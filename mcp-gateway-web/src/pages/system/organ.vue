<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/organ.vue
 * @Author: teddy
 * @Date: 2024-02-12 12:14:59
 * @Description: 租户管理
 * @LastEditors: teddy
 * @LastEditTime: 2024-05-06 10:25:16
-->
<script setup lang="tsx">
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import { createVNode } from 'vue'
import { _tmeMsg, _getOrgData } from '@/assets/js/util'
import { useTableScroll } from '@/utils/tableScrollUtils'
import { useStorage } from '@vueuse/core'
import {
	fetchCodeList,
	fetchRoleList,
	fetchUserAdd,
	fetchUserDelete,
	fetchUserEdit,
	fetchUserList,
	fetchUserListExport,
	fetchTenantAdd,
	fetchTenantEdit,
	fetchTenantDelete,
	fetchDeptAdd,
	fetchDeptEdit,
	fetchDeptDelete,
	fetchCacheClearOrgan,
} from '~/fetch/http'
// 用户登录历史信息
const userInfo = useStorage<any>('pay-user-info', {})

// 租户数据
const orgAllStorage = useStorage('pay-org-all-data', [])
const treeData = ref<any[]>([])
const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])

// 在树中查找节点及其父节点路径
const findNodePath = (
	targetId: string,
	nodes: any[],
	path: any[] = [],
): any[] | null => {
	for (const item of nodes) {
		const currentPath = [...path, item]
		if (item.id === targetId) {
			return currentPath
		}
		if (item.children && item.children.length > 0) {
			const result = findNodePath(targetId, item.children, currentPath)
			if (result) return result
		}
	}
	return null
}

// 查找节点所属的租户ID
const findTenantId = (node: any, tree: any[]): string | undefined => {
	if (node.type === 'TENANT') {
		return node.id
	}
	// 如果是部门，向上查找租户
	const nodePath = findNodePath(node.id, tree)
	if (nodePath) {
		// 从路径中倒序查找第一个 TENANT 类型的节点
		for (let i = nodePath.length - 1; i >= 0; i--) {
			if (nodePath[i].type === 'TENANT') {
				return nodePath[i].id
			}
		}
	}
	return undefined
}

// 树节点选择事件
const handleTreeSelect = (selectedKeysValue: string[]) => {
	if (selectedKeysValue.length === 0) {
		return
	}
	const selectedKey = selectedKeysValue[0]

	// 从树数据中查找选中的节点，确保获取完整的节点信息
	const selectedNode = findNodeById(selectedKey, treeData.value)

	if (!selectedNode) {
		return
	}

	// 根据节点类型设置查询条件
	if (selectedNode.type === 'TENANT') {
		tableSearch.tenant = selectedNode.id
		tableSearch.dept = undefined
	} else if (selectedNode.type === 'DEPT') {
		// 部门节点：传所属租户和所属部门一起过滤
		const tenantId = findTenantId(selectedNode, treeData.value)
		if (tenantId) {
			tableSearch.tenant = tenantId
		}
		tableSearch.dept = selectedNode.id
	}

	// 刷新用户列表
	tableRef.value?.getTableList(1, true)
}

// 在树中查找指定ID的节点
const findNodeById = (targetId: string, nodes: any[]): any | null => {
	for (const item of nodes) {
		if (item.id === targetId) {
			return item
		}
		if (item.children && item.children.length > 0) {
			const result = findNodeById(targetId, item.children)
			if (result) return result
		}
	}
	return null
}

// 查找第一个租户节点
const findFirstTenantNode = (nodes: any[]): any | null => {
	for (const item of nodes) {
		if (item.type === 'TENANT') {
			return item
		}
		if (item.children && item.children.length > 0) {
			const result = findFirstTenantNode(item.children)
			if (result) return result
		}
	}
	return null
}

// 本级同类型节点的最大序号+1（用于添加时的顺序默认值）
const getNextOrder = (siblings: any[], nodeType: string): number => {
	if (!siblings || siblings.length === 0) return 1
	const sameType = siblings.filter((n: any) => n.type === nodeType)
	if (sameType.length === 0) return 1
	const maxOrder = Math.max(
		0,
		...sameType.map((n: any) => (n.nOrder != null ? Number(n.nOrder) : 0)),
	)
	return maxOrder + 1
}

// 初始化树数据并选中根节点
const initTree = async () => {
	try {
		await _getOrgData()
		treeData.value = orgAllStorage.value

		// 确定默认选中的节点
		let defaultSelectedNode = null

		if (treeData.value.length > 0) {
			// 如果用户有权限限制（tenant !== '0'），选中用户所属的租户节点
			if (userInfo.value.tenant && userInfo.value.tenant !== '0') {
				const userTenantNode = findNodeById(
					userInfo.value.tenant,
					treeData.value,
				)
				if (userTenantNode) {
					defaultSelectedNode = userTenantNode
				}
			}

			// 如果没有找到用户租户节点，则选中根节点
			if (!defaultSelectedNode) {
				defaultSelectedNode = treeData.value[0]
			}

			// 设置选中和展开状态
			selectedKeys.value = [defaultSelectedNode.id]
			expandedKeys.value = [defaultSelectedNode.id]

			// 设置默认查询条件
			if (defaultSelectedNode.type === 'TENANT') {
				tableSearch.tenant = defaultSelectedNode.id
				tableSearch.dept = undefined
			} else if (defaultSelectedNode.type === 'DEPT') {
				const tenantId = findTenantId(defaultSelectedNode, treeData.value)
				if (tenantId) {
					tableSearch.tenant = tenantId
				}
				tableSearch.dept = defaultSelectedNode.id
			}

			// 刷新用户列表
			nextTick(() => {
				tableRef.value?.getTableList(1, true)
			})
		}
	} catch (error) {
		console.error('初始化租户数据失败', error)
	}
}

// table ref
const tableRef = ref()
// 用户列表仅表格区域滚动：容器 ref 与滚动高度
const userTableContainerRef = ref<HTMLElement | null>(null)
const userTableScrollY = ref<number | undefined>(undefined)
const userTableScrollConfig = [
	{
		containerRef: userTableContainerRef,
		scrollY: userTableScrollY,
		headerSelector: '.page-table > div:first-child',
		heightAdjustment: 0,
	},
]
const {
	calculate: calculateUserTableScrollY,
	initResizeObserver: initUserTableResizeObserver,
	cleanup: cleanupUserTableScroll,
	initWindowResize: initUserTableWindowResize,
} = useTableScroll(userTableScrollConfig)
const changeTableInput = () => {
	if (!tableSearch.nameOrLoginIdLike) {
		tableRef.value.getTableList(1, true)
	}
}

// table search
const tableSearch = reactive<any>({
	nameOrLoginIdLike: '',
	userType: undefined,
	type: [],
	tenant: userInfo.value.tenant === '0' ? undefined : userInfo.value.tenant,
	dept: undefined,
})

// table-ajax
const getUserList = async (page: any) => {
	try {
		const res = await fetchUserList({
			...page,
			entityParam: {
				nameOrLoginIdLike: tableSearch.nameOrLoginIdLike,
				userType: tableSearch.userType,
				tenant: tableSearch.tenant,
				dept: tableSearch.dept,
			},
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
		title: '姓名',
		dataIndex: 'name',
	},
	{
		title: '账号',
		dataIndex: 'loginid',
	},
	{
		title: '用户类型',
		dataIndex: 'userTypeTranslateText',
	},
	{
		title: '租户',
		dataIndex: 'tenantTranslateText',
	},
	{
		title: '部门',
		dataIndex: 'deptTranslateText',
	},
	{
		title: '角色',
		dataIndex: 'roleTranslateText',
		width: 300,
		customRender: (row: any) => {
			const data = toRaw(row.record)
			return (
				<div>
					{data.roleId
						? data.roleId.map((item: any) => (
								<a-tag color='#2db7f5'>{roleMap.value[item]}</a-tag>
							))
						: '-'}
				</div>
			)
		},
	},
	{
		title: '操作',
		width: 200,
		dataIndex: 'active',
		customRender: (row: any) => {
			const data = toRaw(row.record)
			const isMcpAdmin = data.loginid === 'mcpAdmin'
			return (
				<div class='flex gap-1'>
					{data.disableEdit !== '1' && (
						<a-button
							size='small'
							type='link'
							v-permission='system.system.list.edit'
							onClick={() => {
								handleEdit(data)
							}}
						>
							<i class='ri-edit-box-line mr-1'></i>
							编辑用户
						</a-button>
					)}
					{!isMcpAdmin && (
						<a-button
							size='small'
							type='link'
							danger
							v-permission='system.system.list.delete'
							onClick={() => {
								handleDeleteUser(data)
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

// 用户-modal（新增：所属租户、所属部门、密码；编辑：仅角色）
const userFormData = reactive<any>({
	visible: false,
	loading: false,
	id: '',
	name: '',
	loginid: '',
	password: '',
	tenant: undefined,
	dept: undefined,
	roleId: [],
})
// 是否新增用户模式（无 id 为新增）
const isAddUserMode = computed(() => !userFormData.id)
// 从树中收集所有租户节点（type === 'TENANT'）
// const collectTenantNodes = (nodes: any[], list: any[] = []): any[] => {
// 	if (!nodes || !nodes.length) return list
// 	for (const item of nodes) {
// 		if (item.type === 'TENANT') {
// 			list.push({ id: item.id, name: item.label || item.name })
// 		}
// 		if (item.children && item.children.length) {
// 			collectTenantNodes(item.children, list)
// 		}
// 	}
// 	return list
// }
// 租户下拉选项（排除根节点 0，只取 TENANT）
// const tenantOptionsForUser = computed(() => collectTenantNodes(treeData.value))
// 根据选中的租户，收集其下所有部门节点（type === 'DEPT'）
// const collectDeptNodesUnderTenant = (tenantId: string, nodes: any[], list: any[] = []): any[] => {
// 	if (!nodes || !nodes.length || !tenantId) return list
// 	for (const item of nodes) {
// 		if (item.type === 'TENANT' && item.id === tenantId && item.children) {
// 			const collect = (arr: any[]) => {
// 				if (!arr || !arr.length) return
// 				for (const c of arr) {
// 					if (c.type === 'DEPT') {
// 						list.push({ id: c.id, name: c.label || c.name })
// 					}
// 					if (c.children && c.children.length) collect(c.children)
// 				}
// 			}
// 			collect(item.children)
// 			return list
// 		}
// 		if (item.children && item.children.length) {
// 			const res = collectDeptNodesUnderTenant(tenantId, item.children, list)
// 			if (res.length > 0) return res
// 		}
// 	}
// 	return list
// }
// 部门下拉选项（依赖所选租户联动）
// const deptOptionsForUser = computed(() => {
// 	const tenantId = userFormData.tenant
// 	if (!tenantId) return []
// 	return collectDeptNodesUnderTenant(tenantId, treeData.value)
// })
// 用户-modal-ref
const userFormRef = ref()
// 用户-modal-提交（新增走 fetchUserAdd，编辑走 fetchUserEdit）
const handleUser = async () => {
	try {
		await userFormRef.value.validate()
		userFormData.loading = true
		if (isAddUserMode.value) {
			await fetchUserAdd({
				name: userFormData.name,
				loginid: userFormData.loginid,
				password: userFormData.password,
				tenant: userFormData.tenant,
				dept: userFormData.dept || undefined,
				roleId: userFormData.roleId || [],
			})
			_tmeMsg.success('新增用户成功')
		} else {
			const editPayload: any = {
				id: userFormData.id,
				name: userFormData.name,
				loginid: userFormData.loginid,
				tenant: userFormData.tenant,
				dept: userFormData.dept || undefined,
				roleId: userFormData.roleId || [],
			}
			if (userFormData.password) {
				editPayload.password = userFormData.password
			}
			await fetchUserEdit(editPayload)
			_tmeMsg.success('编辑用户成功')
		}
		userFormData.visible = false
		// 添加、修改用户后刷新用户列表
		tableRef.value?.getTableList(1, true)
	} catch (error) {
		// 校验或接口报错
	} finally {
		userFormData.loading = false
	}
}
// 新增用户：打开弹窗并重置为新增态
const handleAddUser = () => {
	Object.assign(userFormData, {
		visible: true,
		loading: false,
		id: '',
		name: '',
		loginid: '',
		password: '',
		tenant: undefined,
		dept: undefined,
		roleId: [],
	})
	nextTick(() => userFormRef.value?.resetFields())
}
// 选择所属租户后清空所属部门并清除租户字段的校验状态
const onUserTenantChange = () => {
	userFormData.dept = undefined
	nextTick(() => {
		userFormRef.value?.clearValidate('tenant')
	})
}
// 用户-modal-编辑（回填与新增相同的字段，密码留空表示不修改）
const handleEdit = (row: any) => {
	userFormData.visible = true
	nextTick(() => {
		Object.assign(userFormData, {
			id: row.id,
			name: row.name,
			loginid: row.loginid,
			password: '',
			tenant: row.tenant,
			dept: row.dept ?? undefined,
			roleId: row.roleId || [],
		})
	})
}
// 删除用户（mcpAdmin 不允许删除，前端不展示删除按钮；后端也会校验）
const handleDeleteUser = (row: any) => {
	Modal.confirm({
		title: '删除用户确认',
		icon: createVNode(ExclamationCircleOutlined),
		content: `确定要删除用户「${row.name}」（${row.loginid}）吗？`,
		okText: '删除',
		okType: 'danger',
		cancelText: '取消',
		onOk: async () => {
			await fetchUserDelete(row.id)
			_tmeMsg.success('删除用户成功')
			// 删除用户后刷新用户列表
			tableRef.value?.getTableList(1, true)
		},
	})
}
// // 查看 rowData
// const handleCatRowData = () => {
// 	console.log('rowKeys', tableRef.value.rowKeys)
// 	console.log('rowObjs', tableRef.value.rowObjs)
// }

// 用户-modal-监听显示状态（关闭时重置）
watch(
	() => userFormData.visible,
	val => {
		if (!val && userFormRef.value) {
			userFormRef.value.resetFields()
		}
	},
)

// 获取用户类型
const getUserType = async () => {
	try {
		const { data } = await fetchCodeList(100002)
		tableSearch.type = data
	} catch (error) {}
}

// 获取角色列表
const roleList = ref<any>([])
const roleMap = ref<any>({})
const getRoleList = async () => {
	try {
		const { data } = await fetchRoleList({
			page: {
				current: 1,
				size: 1000,
			},
		})
		data.records.map((item: any) => {
			roleMap.value[item.id] = item.name
		})
		roleList.value = data.records
	} catch (error) {}
}

// 导出
const exportObj = reactive({
	exportName: '用户列表',
	exportMethod: fetchUserListExport,
})

// 添加/编辑租户/部门弹窗（id 有值为编辑模式）
const addFormData = reactive<any>({
	visible: false,
	loading: false,
	id: '', // 编辑时有值
	type: '', // 'TENANT' 或 'DEPT'
	parentNode: null, // 父节点
	name: '',
	alias: '',
	nOrder: 1, // 顺序，默认本级同类型节点最大序号+1
	pid: '', // 父节点ID（自动回填，从租户添加部门时为空）
	pidName: '', // 父节点名称（用于显示）
	tenant: '', // 部门需要所属租户
	tenantName: '', // 所属租户名称（用于显示）
	parentTenantName: '', // 所属租户名称（用于显示，租户用）
})
const addFormRef = ref()

// 判断节点是否可以添加租户
const canAddTenant = (node: any) => {
	// 根节点且ID为0的允许添加租户
	return node && node.id === '0'
}

// 判断节点是否可以添加部门
const canAddDept = (node: any) => {
	if (!node) return false
	// 根节点且ID为0的允许添加部门
	if (node.id === '0') return true
	// 租户节点允许添加部门
	if (node.type === 'TENANT') return true
	// 部门节点允许添加下级部门
	if (node.type === 'DEPT') return true
	return false
}

// 打开添加租户弹窗
const handleAddTenant = (node: any, e: Event) => {
	e.stopPropagation()
	// 只有根节点允许添加租户
	if (node.id !== '0') {
		return
	}

	addFormData.visible = true
	addFormData.id = ''
	addFormData.type = 'TENANT'
	addFormData.parentNode = node
	addFormData.name = ''
	addFormData.alias = ''
	addFormData.tenant = ''
	addFormData.tenantName = ''
	addFormData.pid = node.id // '0'
	addFormData.parentTenantName = node.label || node.name || '根节点'
	// 顺序：本级（根节点下）同类型租户 = 根节点的 children
	const nextOrder = getNextOrder(node.children || [], 'TENANT')
	addFormData.nOrder = nextOrder

	nextTick(() => {
		addFormRef.value?.resetFields()
		addFormData.nOrder = nextOrder
	})
}

// 打开添加部门弹窗
// 所属租户回填规则：1、从租户上添加 → 回填该租户；2、从部门上添加 → 回填部门所属的租户
const handleAddDept = (node: any, e: Event) => {
	e.stopPropagation()
	addFormData.visible = true
	addFormData.id = ''
	addFormData.type = 'DEPT'
	addFormData.parentNode = node
	addFormData.name = ''
	addFormData.alias = ''
	addFormData.tenant = ''
	addFormData.tenantName = ''
	addFormData.pidName = ''
	addFormData.pid = ''

	// 自动回填父节点、所属租户、顺序（按来源：根节点 / 租户 / 部门）
	if (node.id === '0') {
		// 根节点下添加部门：pid 为空，所属租户取第一个租户
		addFormData.pid = ''
		addFormData.pidName = node.label || node.name || '根节点'
		const firstTenant = findFirstTenantNode(treeData.value)
		if (firstTenant) {
			addFormData.tenant = firstTenant.id
			addFormData.tenantName =
				firstTenant.label || firstTenant.name || firstTenant.id
			addFormData.nOrder = getNextOrder(firstTenant.children || [], 'DEPT')
		} else {
			addFormData.tenantName = '暂无租户'
			addFormData.nOrder = 1
		}
	} else if (node.type === 'TENANT') {
		// 从租户上添加部门：pid 为空，不显示父节点；回填该租户；顺序取该租户下部门最大+1
		addFormData.pid = ''
		addFormData.pidName = ''
		addFormData.tenant = node.id
		addFormData.tenantName = node.label || node.name || node.id
		addFormData.nOrder = getNextOrder(node.children || [], 'DEPT')
	} else if (node.type === 'DEPT') {
		// 从部门上添加：回填部门所属的租户、父节点；顺序取该部门下子部门最大+1
		addFormData.pid = node.id
		addFormData.pidName = node.label || node.name || node.id
		const tenantId = findTenantId(node, treeData.value)
		addFormData.tenant = tenantId || ''
		if (tenantId) {
			const tenantNode = findNodeById(tenantId, treeData.value)
			addFormData.tenantName = tenantNode?.label || tenantNode?.name || tenantId
		} else {
			addFormData.tenantName = '暂无租户'
		}
		addFormData.nOrder = getNextOrder(node.children || [], 'DEPT')
	}

	const nextOrder = addFormData.nOrder
	nextTick(() => {
		addFormRef.value?.resetFields()
		addFormData.nOrder = nextOrder
	})
}

// 编辑租户
const handleEditTenant = (node: any, e: Event) => {
	e.stopPropagation()
	addFormData.visible = true
	addFormData.id = node.id
	addFormData.type = 'TENANT'
	addFormData.parentNode =
		node.pid === '0'
			? { id: '0', label: '根节点' }
			: findNodeById(node.pid, treeData.value)
	addFormData.name = node.name || node.label || ''
	addFormData.alias = node.alias || ''
	addFormData.nOrder = node.nOrder != null ? node.nOrder : 1
	addFormData.pid = node.pid || '0'
	addFormData.parentTenantName =
		node.pid === '0'
			? '根节点'
			: addFormData.parentNode?.label || addFormData.parentNode?.name || ''
	addFormData.tenant = ''
	addFormData.tenantName = ''
	nextTick(() => addFormRef.value?.resetFields())
}

// 编辑部门
const handleEditDept = (node: any, e: Event) => {
	e.stopPropagation()
	addFormData.visible = true
	addFormData.id = node.id
	addFormData.type = 'DEPT'
	addFormData.name = node.name || node.label || ''
	addFormData.alias = node.alias || ''
	addFormData.nOrder = node.nOrder != null ? node.nOrder : 1
	addFormData.tenant = node.tenant || ''
	const tenantNode = findNodeById(node.tenant, treeData.value)
	addFormData.tenantName =
		tenantNode?.label || tenantNode?.name || node.tenant || ''
	addFormData.pid = node.pid || ''
	const pidNode = node.pid ? findNodeById(node.pid, treeData.value) : null
	addFormData.pidName = pidNode ? pidNode.label || pidNode.name || '' : ''
	nextTick(() => addFormRef.value?.resetFields())
}

// 删除租户（级联删除其下所有部门及用户）
const handleDeleteTenant = (node: any, e: Event) => {
	e.stopPropagation()
	const name = node.label || node.name || node.id
	Modal.confirm({
		title: '删除租户确认',
		icon: createVNode(ExclamationCircleOutlined),
		content: `删除租户将级联删除其下所有部门及用户，确定要删除租户「${name}」吗？`,
		okText: '删除',
		cancelText: '取消',
		okType: 'danger',
		async onOk() {
			await fetchTenantDelete(node.id)
			await fetchCacheClearOrgan()
			await initTree()
			_tmeMsg.success('删除租户成功')
		},
	})
}

// 删除部门（级联删除其下所有子部门及用户）
const handleDeleteDept = (node: any, e: Event) => {
	e.stopPropagation()
	const name = node.label || node.name || node.id
	Modal.confirm({
		title: '删除部门确认',
		icon: createVNode(ExclamationCircleOutlined),
		content: `删除部门将级联删除其下所有子部门及用户，确定要删除部门「${name}」吗？`,
		okText: '删除',
		cancelText: '取消',
		okType: 'danger',
		async onOk() {
			await fetchDeptDelete(node.id)
			await fetchCacheClearOrgan()
			await initTree()
			_tmeMsg.success('删除部门成功')
		},
	})
}

// 提交添加/编辑表单
const handleAddSubmit = async () => {
	try {
		await addFormRef.value.validate()
		addFormData.loading = true
		const isEdit = !!addFormData.id

		if (addFormData.type === 'TENANT') {
			const tenantData: any = {
				name: addFormData.name,
				pid: addFormData.pid || addFormData.parentNode?.id || '0',
				valid: '1',
				nOrder: addFormData.nOrder != null ? addFormData.nOrder : 1,
			}
			if (addFormData.alias) tenantData.alias = addFormData.alias
			if (isEdit) {
				tenantData.id = addFormData.id
				await fetchTenantEdit(tenantData)
				_tmeMsg.success('编辑租户成功')
			} else {
				await fetchTenantAdd(tenantData)
				_tmeMsg.success('添加租户成功')
			}
		} else if (addFormData.type === 'DEPT') {
			if (!addFormData.tenant) {
				_tmeMsg.error('所属租户不能为空')
				addFormData.loading = false
				return
			}
			const deptData: any = {
				name: addFormData.name,
				tenant: addFormData.tenant,
				valid: '1',
				nOrder: addFormData.nOrder != null ? addFormData.nOrder : 1,
			}
			if (addFormData.alias) deptData.alias = addFormData.alias
			if (addFormData.pid) deptData.pid = addFormData.pid
			if (isEdit) {
				deptData.id = addFormData.id
				await fetchDeptEdit(deptData)
				_tmeMsg.success('编辑部门成功')
			} else {
				await fetchDeptAdd(deptData)
				_tmeMsg.success('添加部门成功')
			}
		}

		await fetchCacheClearOrgan()
		addFormData.visible = false
		await initTree()
	} catch (error) {
		console.error('提交失败', error)
	} finally {
		addFormData.loading = false
	}
}

onMounted(() => {
	getRoleList()
	getUserType()
	initTree()
	// 用户列表仅表格区域滚动：计算高度并监听尺寸变化
	nextTick(() => {
		calculateUserTableScrollY()
		initUserTableResizeObserver()
		initUserTableWindowResize()
	})
})

onBeforeUnmount(() => {
	cleanupUserTableScroll()
})
</script>

<template>
	<div class="organ-page-container">
		<!-- 左侧树 -->
		<div class="organ-tree-container">
			<a-tree
				v-model:selectedKeys="selectedKeys"
				v-model:expandedKeys="expandedKeys"
				:tree-data="treeData"
				:field-names="{ children: 'children', title: 'label', key: 'id' }"
				show-line
				block-node
				@select="handleTreeSelect"
			>
				<template #title="{ dataRef }">
					<div class="tree-node-title">
						<span class="tree-node-label">{{ dataRef.label }}</span>
						<span class="tree-node-actions" @click.stop>
							<template v-if="canAddTenant(dataRef)">
								<a-tooltip title="添加租户">
									<a-button
										type="text"
										size="small"
										class="tree-action-btn"
										@click="handleAddTenant(dataRef, $event)"
									>
										<i class="ri-building-line"></i>
									</a-button>
								</a-tooltip>
							</template>
							<template v-if="canAddDept(dataRef)">
								<a-tooltip title="添加部门">
									<a-button
										type="text"
										size="small"
										class="tree-action-btn"
										@click="handleAddDept(dataRef, $event)"
									>
										<i class="ri-folder-add-line"></i>
									</a-button>
								</a-tooltip>
							</template>
							<template v-if="dataRef.id !== '0'">
								<a-tooltip
									:title="dataRef.type === 'TENANT' ? '编辑租户' : '编辑部门'"
								>
									<a-button
										type="text"
										size="small"
										class="tree-action-btn"
										@click="
											dataRef.type === 'TENANT'
												? handleEditTenant(dataRef, $event)
												: handleEditDept(dataRef, $event)
										"
									>
										<i class="ri-edit-line"></i>
									</a-button>
								</a-tooltip>
								<a-tooltip
									:title="dataRef.type === 'TENANT' ? '删除租户' : '删除部门'"
								>
									<a-button
										type="text"
										size="small"
										class="tree-action-btn tree-action-btn-danger"
										@click="
											dataRef.type === 'TENANT'
												? handleDeleteTenant(dataRef, $event)
												: handleDeleteDept(dataRef, $event)
										"
									>
										<i class="ri-delete-bin-line"></i>
									</a-button>
								</a-tooltip>
							</template>
						</span>
					</div>
				</template>
			</a-tree>
		</div>
		<!-- 右侧表格（仅表格区域滚动） -->
		<div ref="userTableContainerRef" class="organ-table-container">
			<pay-table
				ref="tableRef"
				:ajax="getUserList"
				:columns="columns"
				:exportObj="exportObj"
				:scroll-y="userTableScrollY"
			>
				<template #search>
					<div class="flex gap-3">
						<a-input
							v-model:value="tableSearch.nameOrLoginIdLike"
							allowClear
							placeholder="请输入姓名或账号"
							@press-enter="() => tableRef.getTableList(1, true)"
							@change="changeTableInput"
						></a-input>
						<a-select
							v-model:value="tableSearch.userType"
							class="block"
							allowClear
							placeholder="请选择用户类型"
							:disabled="!tableSearch.type.length"
							@change="() => tableRef.getTableList(1, true)"
						>
							<a-select-option
								v-for="item in tableSearch.type"
								:key="item.code"
								:value="item.code"
							>
								{{ item.name }}
							</a-select-option>
						</a-select>
						<a-button
							type="primary"
							@click="() => tableRef.getTableList(1, true)"
						>
							<i class="ri-search-line mr-1"></i>
							查询
						</a-button>
					</div>
					<a-button
						v-permission="'system.system.list.add'"
						type="primary"
						@click="handleAddUser"
					>
						<i class="ri-add-line mr-1"></i>
						新增用户
					</a-button>
					<!-- <a-button type="primary" @click="handleCatRowData">
						<i class="ri-edit-box-line mr-1"></i>
						打开控制台查看
					</a-button> -->
				</template>
			</pay-table>
		</div>
	</div>
	<!-- 用户-modal（新增用户 / 编辑用户，表单字段一致） -->
	<a-modal
		v-model:open="userFormData.visible"
		:title="isAddUserMode ? '新增用户' : '编辑用户'"
	>
		<a-form
			ref="userFormRef"
			:model="userFormData"
			:label-col="{ span: 4 }"
			:wrapper-col="{ span: 20 }"
			autocomplete="off"
		>
			<a-form-item
				label="所属租户"
				name="tenant"
				:rules="[{ required: true, message: '请选择所属租户' }]"
			>
				<tree-tenant
					v-model:value="userFormData.tenant"
					placeholder="请选择所属租户"
					@change="onUserTenantChange"
				/>
			</a-form-item>
			<a-form-item label="所属部门" name="dept">
				<tree-dept
					v-model:value="userFormData.dept"
					:tenant="userFormData.tenant"
					placeholder="请选择所属部门（可选）"
				/>
			</a-form-item>
			<a-form-item
				label="用户名称"
				name="name"
				:rules="[{ required: true, message: '请输入用户名称' }]"
			>
				<a-input
					v-model:value="userFormData.name"
					placeholder="请输入用户名称"
				/>
			</a-form-item>
			<a-form-item
				label="用户账号"
				name="loginid"
				:rules="[{ required: true, message: '请输入用户账号' }]"
			>
				<a-input
					v-model:value="userFormData.loginid"
					placeholder="请输入用户账号"
				/>
			</a-form-item>
			<a-form-item
				label="密码"
				name="password"
				:rules="
					isAddUserMode ? [{ required: true, message: '请输入密码' }] : []
				"
			>
				<a-input-password
					v-model:value="userFormData.password"
					:placeholder="isAddUserMode ? '请输入密码' : '留空表示不修改'"
					autocomplete="new-password"
				/>
			</a-form-item>
			<a-form-item label="用户角色" name="roleId">
				<a-select
					v-model:value="userFormData.roleId"
					allowClear
					mode="multiple"
					placeholder="请选择用户角色"
				>
					<a-select-option
						v-for="item in roleList"
						:key="item.id"
						:value="item.id"
					>
						{{ item.name }}
					</a-select-option>
				</a-select>
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="userFormData.visible = false">取消</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="userFormData.loading"
				@click="handleUser"
			>
				提交
			</a-button>
		</template>
	</a-modal>
	<!-- 添加/编辑租户/部门-modal -->
	<a-modal
		v-model:open="addFormData.visible"
		:title="
			(addFormData.id ? '编辑' : '添加') +
			(addFormData.type === 'TENANT' ? '租户' : '部门')
		"
		:width="500"
	>
		<a-form
			ref="addFormRef"
			:model="addFormData"
			:label-col="{ span: 6 }"
			:wrapper-col="{ span: 18 }"
			autocomplete="off"
		>
			<!-- 所属租户放在第一行 -->
			<a-form-item v-if="addFormData.type === 'TENANT'" label="所属租户">
				<a-input :value="addFormData.parentTenantName || '根节点'" disabled />
			</a-form-item>
			<!-- 部门：所属租户放在第一行（只读显示） -->
			<a-form-item v-if="addFormData.type === 'DEPT'" label="所属租户">
				<a-input :value="addFormData.tenantName || '-'" disabled />
			</a-form-item>
			<!-- 部门：父节点（仅从部门添加时显示，从租户添加时 pid 为空不显示） -->
			<a-form-item
				v-if="addFormData.type === 'DEPT' && addFormData.pid"
				label="父节点"
			>
				<a-input :value="addFormData.pidName || '-'" disabled />
			</a-form-item>
			<a-form-item
				label="顺序"
				name="nOrder"
				:rules="[{ required: true, message: '请输入顺序' }]"
			>
				<a-input-number
					v-model:value="addFormData.nOrder"
					:min="1"
					placeholder="本级同类型节点最大序号+1"
					class="w-full"
					style="width: 100%"
				/>
			</a-form-item>
			<a-form-item
				label="名称"
				name="name"
				:rules="[{ required: true, message: '请输入名称' }]"
			>
				<a-input v-model:value="addFormData.name" placeholder="请输入名称" />
			</a-form-item>
			<a-form-item label="别名" name="alias">
				<a-input
					v-model:value="addFormData.alias"
					placeholder="请输入别名（可选）"
				/>
			</a-form-item>
		</a-form>
		<template #footer>
			<a-button key="back" @click="addFormData.visible = false">取消</a-button>
			<a-button
				key="submit"
				type="primary"
				:loading="addFormData.loading"
				@click="handleAddSubmit"
			>
				提交
			</a-button>
		</template>
	</a-modal>
</template>

<style lang="scss" scoped>
.organ-page-container {
	display: flex;
	height: calc(100vh - 200px);
	min-height: 600px;
}

.organ-tree-container {
	width: 256px;
	min-width: 256px;
	border-right: 1px solid #e1e1e1;
	padding-right: 16px;
	overflow: hidden;
	display: flex;
	flex-direction: column;

	:deep(.ant-tree) {
		flex: 1;
		overflow: auto;
		height: 100%;
	}
}

.organ-table-container {
	flex: 1;
	padding-left: 16px;
	overflow: hidden;
	display: flex;
	flex-direction: column;
	min-height: 0; /* flex 子项参与收缩，否则 overflow 不生效 */

	:deep(.page-table) {
		flex: 1;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		min-height: 0;
	}

	/* 单层 :deep 穿透到 pay-table 内的表格 body；!important 覆盖 pay-table 内部同规则 */
	:deep(.page-table .ant-table-body) {
		min-height: calc(100vh - 330px) !important;
	}
}

.dark {
	.organ-tree-container {
		border-right-color: #434343;
	}
}

.tree-node-title {
	display: flex;
	align-items: center;
	justify-content: space-between;
	width: 100%;
	padding-right: 8px;

	.tree-node-label {
		flex: 1;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.tree-node-actions {
		display: flex;
		align-items: center;
		gap: 4px;
		margin-left: 8px;

		.tree-action-btn {
			padding: 0 4px;
			height: 20px;
			line-height: 20px;
			font-size: 14px;
			color: #1890ff;

			&:hover {
				color: #40a9ff;
			}

			&.tree-action-btn-danger {
				color: #ff4d4f;

				&:hover {
					color: #ff7875;
				}
			}
		}
	}
}
</style>
