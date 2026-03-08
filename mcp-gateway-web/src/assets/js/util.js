/*
 * @FilePath: /mcp-gateway-web/src/assets/js/util.js
 * @Author: teddy
 * @Date: 2024-02-26 16:56:57
 * @Description: 复用功能
 * @LastEditors: teddy
 * @LastEditTime: 2024-06-26 15:04:45
 */
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { useStorage } from '@vueuse/core'
import { Modal, message, notification } from 'ant-design-vue'
import dayjs from 'dayjs'
import { createVNode } from 'vue'
import { fetchOrgAll, fetchPermissions } from '~/fetch/http'

// 区域数据-storage
const areaStorage = useStorage('pay-area-data-all', [])
const areaStorageNoCode = useStorage('pay-area-data-no-code', [])
// 租户数据-storage
const orgAllStorage = useStorage('pay-org-all-data', [])
// 租户数据-storage-层级到租户
const orgStorage = useStorage('pay-org-data', [])
// 租户数据-storage-层级到部门
const orgDeptStorage = useStorage('pay-org-dept-data', [])
// 权限点数据-storage
const permissionsStorage = useStorage('pay-permissions-data', [])
// 用户登录历史信息
const userInfo = useStorage('pay-user-info', {})

/**
 * @description: 操作通知
 * @param {text}  通知内容
 * @author: teddy
 */
export const _tmeNot = {
	success: (text = '') => {
		_tmeNot.not(text, 'success')
	},
	error: (text = '') => {
		_tmeNot.not(text, 'error')
	},
	info: (text = '') => {
		_tmeNot.not(text, 'info')
	},
	warning: (text = '') => {
		_tmeNot.not(text, 'warning')
	},
	not: (text, type = '') => {
		notification[type]({
			message: text,
			placement: 'bottomRight',
			class: 'tme-notification',
			duration: 3,
		})
	},
}
/**
 * @description: 操作反馈
 * @param {text}  通知内容
 * @author: teddy
 */
export const _tmeMsg = {
	success: (text = '', key) => {
		_tmeMsg.msg(text, 'success', key)
	},
	error: (text = '', key) => {
		_tmeMsg.msg(text, 'error', key)
	},
	info: (text = '', key) => {
		_tmeMsg.msg(text, 'info', key)
	},
	warning: (text = '', key) => {
		_tmeMsg.msg(text, 'warning', key)
	},
	loading: (text = '', key) => {
		_tmeMsg.msg(text, 'loading', key)
	},
	destroy: () => {
		message.destroy()
	},
	msg: (text, type = '', key) => {
		message.destroy()
		message[type]({
			content: text,
			key,
			class: 'tme-message',
		})
	},
}

/**
 * @description: 通用删除弹窗
 * @param {title} 提示删除 title，如“用户”
 * @param {name} 提示删除的名称
 * @param {bus} 删除事件
 * @param {callback} 删除成功回调
 * @author: teddy
 */
export const _deleteModal = (title = 'xx', name, bus, callback) => {
	try {
		Modal.confirm({
			title: `删除${title}确认?`,
			icon: createVNode(ExclamationCircleOutlined),
			content: `确认删除${title}<${name}>吗？`,
			okText: '删除',
			cancelText: '取消',
			okType: 'danger',
			async onOk() {
				try {
					await bus()
					_tmeMsg.success('删除成功')
					callback()
					return Promise.resolve()
				} catch (error) {
					return Promise.reject(error)
				}
			},
		})
	} catch (error) {}
}

/**
 * @description:全屏控制
 * @author: teddy
 */
// 使用useState钩子函数创建一个名为fullscreen的state，其初始值为document.fullscreenElement是否存在
const fullscreen = useState('fullscreen', () => !!document.fullscreenElement)
// 创建一个名为fullscreenEvent的变量，并将其初始值设为true
let fullscreenEvent = true
export const _toggleFullScreen = () => {
	// 如果fullscreenEvent为true
	if (fullscreenEvent) {
		// 将fullscreenEvent设为false
		fullscreenEvent = false
		// 监听fullscreenchange事件
		document.addEventListener('fullscreenchange', function () {
			// 如果存在全屏元素
			if (document.fullscreenElement) {
				// 将fullscreen的值设为true
				fullscreen.value = true
			} else {
				// 如果退出全屏
				// 将fullscreen的值设为false
				fullscreen.value = false
			}
		})
	}
	// 如果未进入全屏
	if (!document.fullscreenElement) {
		// 将fullscreen的值设为true
		fullscreen.value = true
		// 请求全屏
		document.documentElement.requestFullscreen()
	} else if (document.exitFullscreen) {
		// 如果已进入全屏
		// 将fullscreen的值设为false
		fullscreen.value = false
		// 退出全屏
		document.exitFullscreen()
	}
}

/**
 * @description: 递归深拷贝
 * @param {*} 原数组
 * @return {*} 深拷贝后的数组
 * @author: teddy
 */
export const _deepClone = obj => {
	const objClone = Array.isArray(obj) ? [] : {}
	if (obj && typeof obj === 'object') {
		for (const key in obj) {
			if (Object.prototype.hasOwnProperty.call(obj, key)) {
				// 判断ojb子元素是否为对象，如果是，递归复制
				if (obj[key] && typeof obj[key] === 'object') {
					objClone[key] = _deepClone(obj[key])
				} else {
					// 如果不是，简单复制
					objClone[key] = obj[key]
				}
			}
		}
	}
	return objClone
}

/**
 * @description: 获取租户数据
 * @return {*} 返回租户数据
 * @author: teddy
 */
export const _getOrgData = async () => {
	try {
		const { data } = await fetchOrgAll()
		// 租户所有数据
		const orgAllData = []
		// 包含根节点（如果根节点有 name），否则包含根节点的子节点
		if (data) {
			if (data.name) {
				// 根节点有 name，直接使用根节点（它已经包含了 children）
				orgAllData.push({ ...data })
			} else if (data.children && data.children.length > 0) {
				// 根节点没有 name，使用根节点的子节点
				data.children.map(item => {
					if (item.name) {
						orgAllData.push({ ...item })
					}
				})
			}
		}
		// 清洗租户数据
		const setOrgAllData = (arr = []) => {
			arr.map(item => {
				item.label = item.name
				item.value = item.id
				item.key = item.id
				item.title = item.name
				if (item.children && item.children.length > 0) {
					setOrgAllData(item.children)
				}
			})
		}

		// 设置租户数据-根据 list 控制层级
		function setOrgData(list = [], arr = [], disabled = true) {
			let index = arr.length
			while (index--) {
				if (list[1] === 'DEPT') {
					if (arr[index].type !== 'DEPT') {
						if (disabled) arr[index].disabled = true
					}
				}
				if (!list.includes(arr[index].type)) {
					arr.splice(index, 1)
				} else if (arr[index].children) {
					setOrgData(list, arr[index].children, disabled)
				}
			}
		}
		setOrgAllData(orgAllData)

		// 设置租户数据-不要人
		const orgAll = _deepClone(orgAllData)
		setOrgData(['TENANT', 'DEPT'], orgAll, false)
		orgAllStorage.value = orgAll
		// 设置租户数据-层级到租户
		const orgData = _deepClone(orgAllData)
		setOrgData(['TENANT'], orgData)
		orgStorage.value = orgData
		// 设置租户数据-层级到部门
		const orgdeptData = _deepClone(orgAllData)
		setOrgData(['TENANT', 'DEPT'], orgdeptData)
		orgDeptStorage.value = orgdeptData
		return Promise.resolve(orgAllStorage.value)
	} catch (error) {
		return Promise.reject(error)
	}
}

/**
 * @description: 获取所有权限点数据
 * @return {*} 返回所有权限点数据
 * @author: teddy
 */

export const _getPermissionsData = async () => {
	try {
		const res = await fetchPermissions()
		// 处理数据-生成 parentId
		res.data.map(item => {
			const list = item.rightkey.split('.')
			if (list.length > 2) {
				list.pop()
				item.parentId = list.join('.')
			} else {
				// 设置根节点
				item.parentId = 'system'
			}
			item.title = item.name
			item.label = item.name
			item.key = item.rightkey
			item.value = item.rightkey
		})
		// 处理数据-数组转树结构（父节点不在列表中的视为根节点，避免新增权限不展示）
		const rightkeySet = new Set(res.data.map(item => item.rightkey))
		const arrayToTree = (arr, root) => {
			return arr
				.filter(
					item =>
						item.parentId === root ||
						(root === 'system' && !rightkeySet.has(item.parentId)),
				)
				.map(item => ({
					...item,
					children: arrayToTree(arr, item.rightkey),
				}))
		}
		// 赋值 tree 权限点数据
		permissionsStorage.value = arrayToTree(res.data, 'system')
		return Promise.resolve(permissionsStorage.value)
	} catch (error) {
		return Promise.reject(error)
	}
}

/**
 * @description: 生成uuid part(4位)
 * @return {string} uuidPart(4位)
 * @author: teddy
 */
export const _uuidPart = () =>
	(((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1)

/**
 * @description: 生成32位uuid
 * @return {string} 32位uuid
 * @author: teddy
 */
export const _uuid = () =>
	_uuidPart() +
	_uuidPart() +
	_uuidPart() +
	_uuidPart() +
	_uuidPart() +
	_uuidPart() +
	_uuidPart() +
	_uuidPart()

/**
 * @description: dayjs 时间转化
 * @param {data} 需要格式化的 dayjs 格式数据
 * @param {format} 格式化规则，默认 YYYY-MM-DD，支持 YYYY-MM-DD HH:mm:ss
 * @return {text} 格式化后的数据
 * @author: teddy
 */
export const _toDayjs = (date, format = 'YYYY-MM-DD') =>
	dayjs(date).format(format)

/**
 * @description: 清除本地存储
 * @author: teddy
 */
export const _clearStorage = (entered = null) => {
	areaStorage.value = null
	areaStorageNoCode.value = null
	orgAllStorage.value = null
	orgStorage.value = null
	orgDeptStorage.value = null
	permissionsStorage.value = null
	// 非 401，重置部分 storage 信息
	if (!entered) {
		userInfo.value.tabsMap = {}
		userInfo.value.tabsPane = []
		userInfo.value.name = ''
		userInfo.value.id = ''
	}
}
