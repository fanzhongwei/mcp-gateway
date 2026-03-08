<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/importTypes/JsonParamRowRecursive.vue
 * @Author: teddy
 * @Date: 2026-01-27
 * @Description: JSON参数递归行组件
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-27
-->
<script lang="ts">
import { defineComponent, type PropType, computed as computedFn, h } from 'vue'
import { Input, Checkbox, Select, Button, Space } from 'ant-design-vue'

// 使用类型别名，避免循环导入
type JsonParamNodeType = {
	id: string
	name: string
	required: boolean
	type: 'string' | 'number' | 'boolean' | 'object' | 'array'
	example: string
	description: string
	children?: JsonParamNodeType[]
}

// 递归组件，使用渲染函数
export const JsonParamRowRecursive: ReturnType<typeof defineComponent> =
	defineComponent({
		name: 'JsonParamRowRecursive',
		props: {
			param: {
				type: Object as PropType<JsonParamNodeType>,
				required: true,
			},
			level: {
				type: Number,
				default: 0,
			},
			expandedKeys: {
				type: Array as PropType<string[]>,
				default: () => [],
			},
		},
		emits: ['toggle-expand', 'add-child', 'delete', 'update-param'],
		setup(props, { emit }) {
			const isParamExpanded = computedFn(() => {
				return props.expandedKeys.includes(props.param.id)
			})
			const canParamExpand = computedFn(() => {
				return props.param.type === 'object' || props.param.type === 'array'
			})
			const getParamIndent = (level: number) => level * 24
			const handleUpdate = (field: keyof JsonParamNodeType, value: any) => {
				emit('update-param', props.param.id, field, value)
			}
			const handleToggleExpand = () => {
				if (canParamExpand.value) {
					emit('toggle-expand', props.param.id)
				}
			}
			const handleAddChild = () => {
				emit('add-child', props.param.id)
			}
			const handleDelete = () => {
				emit('delete', props.param.id)
			}

			return () => {
				const indent = getParamIndent(props.level)
				const expanded = isParamExpanded.value
				const canExpand = canParamExpand.value

				const childrenNodes: any[] = [
					h('div', { class: 'json-param-row-content' }, [
						// 名字列
						h('div', { class: 'json-param-cell', style: { width: '20%' } }, [
							h(
								'div',
								{
									style: { display: 'flex', alignItems: 'center', gap: '8px' },
								},
								[
									h(
										'div',
										{
											class: 'json-param-expand-icon',
											style: { paddingLeft: `${indent}px` },
											onClick: handleToggleExpand,
										},
										[
											canExpand
												? h('i', {
														class: expanded
															? 'ri-arrow-down-s-line'
															: 'ri-arrow-right-s-line',
													})
												: h('span', {
														style: { width: '16px', display: 'inline-block' },
													}),
										],
									),
									h(Input, {
										value: props.param.name,
										'onUpdate:value': (val: string) =>
											handleUpdate('name', val),
										placeholder: '参数名',
										size: 'small',
										class: 'param-input',
										style: { flex: '1' },
										disabled: props.param.name === 'items',
									}),
								],
							),
						]),
						// 必填列
						h(
							'div',
							{
								class: 'json-param-cell json-param-cell-checkbox',
								style: { width: '8%' },
							},
							[
								h(Checkbox, {
									checked: props.param.required,
									'onUpdate:checked': (val: boolean) =>
										handleUpdate('required', val),
								}),
							],
						),
						// 类型列
						h('div', { class: 'json-param-cell', style: { width: '12%' } }, [
							h(
								Select,
								{
									value: props.param.type,
									'onUpdate:value': (val: any) => handleUpdate('type', val),
									size: 'small',
									class: 'param-select',
								},
								{
									default: () => [
										h(
											Select.Option,
											{ value: 'string' },
											{ default: () => 'string' },
										),
										h(
											Select.Option,
											{ value: 'number' },
											{ default: () => 'number' },
										),
										h(
											Select.Option,
											{ value: 'array' },
											{ default: () => 'array' },
										),
										h(
											Select.Option,
											{ value: 'object' },
											{ default: () => 'object' },
										),
										h(
											Select.Option,
											{ value: 'boolean' },
											{ default: () => 'boolean' },
										),
									],
								},
							),
						]),
						// 示例列
						h('div', { class: 'json-param-cell', style: { width: '20%' } }, [
							h(Input, {
								value: props.param.example,
								'onUpdate:value': (val: string) => handleUpdate('example', val),
								placeholder: '示例值',
								size: 'small',
								class: 'param-input',
							}),
						]),
						// 描述列
						h('div', { class: 'json-param-cell', style: { width: '25%' } }, [
							h(Input, {
								value: props.param.description,
								'onUpdate:value': (val: string) =>
									handleUpdate('description', val),
								placeholder: '描述',
								size: 'small',
								class: 'param-input',
							}),
						]),
						// 操作列
						h(
							'div',
							{
								class: 'json-param-cell json-param-cell-actions',
								style: { width: '15%' },
							},
							[
								h(
									Space,
									{},
									{
										default: () => [
											props.param.type === 'object' ||
											props.param.type === 'array'
												? h(
														Button,
														{
															type: 'link',
															size: 'small',
															onClick: handleAddChild,
														},
														{
															default: () => h('i', { class: 'ri-add-line' }),
														},
													)
												: null,
											h(
												Button,
												{
													type: 'link',
													danger: true,
													size: 'small',
													onClick: handleDelete,
												},
												{
													default: () =>
														h('i', { class: 'ri-delete-bin-line' }),
												},
											),
										],
									},
								),
							],
						),
					]),
				]

				// 子节点
				if (
					expanded &&
					canExpand &&
					props.param.children &&
					props.param.children.length > 0
				) {
					childrenNodes.push(
						h(
							'div',
							{ class: 'json-param-children' },
							props.param.children.map((childParam: JsonParamNodeType) =>
								h(JsonParamRowRecursive as any, {
									key: childParam.id,
									param: childParam,
									level: props.level + 1,
									expandedKeys: props.expandedKeys,
									onToggleExpand: (id: string) => emit('toggle-expand', id),
									onAddChild: (id: string) => emit('add-child', id),
									onDelete: (id: string) => emit('delete', id),
									onUpdateParam: (id: string, field: any, value: any) =>
										emit('update-param', id, field, value),
								}),
							),
						),
					)
				}

				return h('div', { class: 'json-param-row' }, childrenNodes)
			}
		},
	})

export default JsonParamRowRecursive
</script>
