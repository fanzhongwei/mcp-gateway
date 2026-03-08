<!--
 * @FilePath: /mcp-gateway-web/src/pages/system/develop/table.vue
 * @Author: teddy
 * @Date: 2024-03-01 17:07:11
 * @Description: 开发工具-table
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-29 20:07:10
-->

<script setup lang="tsx">
const code1 = `// 导出
const exportObj = reactive({
  exportName: '用户列表',
  exportMethod: fetchUserListExport,
})`

const code2 = `// 静态 table
const parentData = reactive([
	{
		name: '现金钱包',
		type: '电子钱包',
		swatch: true,
	},
	{
		name: '补助钱包',
		type: '电子钱包',
		swatch: true,
	},
	{
		name: '次数钱包',
		type: '电子钱包',
		swatch: true,
	},
])
`

const code3 = `// 查看 rowData
const handleCatRowData = () => {
	console.log('rowKeys', tableRef.value.rowKeys)
	console.log('rowObjs', tableRef.value.rowObjs)
}`
</script>

<template>
	<a-alert
		message="pay-table 组件库使用注意事项。阅读代码查看使用方法。"
		type="info"
		show-icon
	/>
	<a-button
		class="!font-bold"
		type="link"
		target="_blank"
		href="https://antdv.com/components/table-cn"
	>
		可参考 UI 组件库文档地址
	</a-button>
	<br />
	<show-code language="xml" code="<admin-pay-table></admin-pay-table>">
		<template #message>
			<table>
				<tr>
					<th>属性</th>
					<th>类型</th>
					<th>描述</th>
					<th>默认值</th>
				</tr>
				<tr>
					<td>ajax</td>
					<td>function</td>
					<td>列表ajax数据</td>
					<td>无</td>
				</tr>
				<tr>
					<td>columns</td>
					<td>array</td>
					<td>表格列配置</td>
					<td>[]</td>
				</tr>
				<tr>
					<td>pagination</td>
					<td>boolean</td>
					<td>是否分页</td>
					<td>true</td>
				</tr>
				<tr>
					<td>exportObj</td>
					<td>0bject</td>
					<td>导出功能</td>
					<td>
						{ exportName: '', exportType: 'xlsx', exportMethod: () => {}, }
					</td>
				</tr>
				<tr>
					<td>parentData</td>
					<td>array</td>
					<td>静态数据，不请求 ajax 和 分页</td>
					<td>[]</td>
				</tr>
				<tr>
					<td>isAjax</td>
					<td>boolean</td>
					<td>是否加载自动请求</td>
					<td>true</td>
				</tr>
				<tr>
					<td>isRowSelection</td>
					<td>boolean</td>
					<td>是否开启选择</td>
					<td>false</td>
				</tr>
				<tr>
					<td>modalTableName</td>
					<td>number</td>
					<td>
						方便弹窗控制 th 的显示（storage 保存用户选择，同页面多个 pay-table
						是必填，否则页面异常）, 设置后分页默认
						10，不再自动计算可是区域生产默认分页值
					</td>
					<td>无</td>
				</tr>
			</table>
			<div>
				<br />
				<p>
					1、参考《用户列表》页，已包含
					<b>增</b>
					、
					<b>删</b>
					、
					<b>改</b>
					、
					<b>查</b>
				</p>
				<p>
					2、table 字段修改后，
					<b>建议更新</b>
					《nuxt.config.ts》文件的
					<b>version</b>
					字段，一般只在最后一位数字 +1
				</p>
				<p>
					3、table 列自动支持
					<b>显示列</b>
					、
					<b>隐藏列</b>
					，并在当前版本号下在用户本地永久性存储用户对该 table 列的操作记录
				</p>
				<p>
					4、
					<b>td 数据为空</b>
					，在不做任何处理的情况下，会自动显示
					<b>无</b>
					占位，故对为空字段
					<b>无需做任何处理</b>
				</p>
				<p>
					5、table 组件会自动根据屏幕可视区域
					<b>分页</b>
					，分页切换器可自动使用，
					<b>无需做任何处理</b>
				</p>
				<p>
					6、table
					<b>排序</b>
					，对需要排序的 columns 字段加上
					<b>sorter: true</b>
					（单字段排序，
					<b>无特殊需求，均采用单字段排序</b>
					）
					<b>sorter: { multiple: 1, }</b>
					（多字段排序） 属性，目前需求只对 date
					、金额、次数字段添加排序，pay-table 组件会自动支持设置的字段排序
				</p>
				<p>
					7、需要
					<b>导出</b>
					的页面，需配置
					<b>pay-table</b>
					的
					<b>exportObj</b>
					属性，赋值为：
				</p>
				<show-code language="ts" :code="code1">
					<template #message>
						<table>
							<tr>
								<th>属性</th>
								<th>描述</th>
								<th>默认值</th>
							</tr>
							<tr>
								<td>exportObj</td>
								<td>导出 Objec</td>
								<td>无</td>
							</tr>
							<tr>
								<td>exportObj.exportName</td>
								<td>导出文件名称（必填）</td>
								<td>无</td>
							</tr>
							<tr>
								<td>exportObj.exportMethod</td>
								<td>
									导出方法（必填），即可在 pay-table
									搜索区域最右侧使用导出功能。
								</td>
								<td>无</td>
							</tr>
						</table>
					</template>
				</show-code>
				<p>
					8、
					<b>parentData</b>
					属性，静态数据，设置后不支持分页，不支持 table ajax 请求
				</p>
				<show-code language="ts" :code="code2">
					<template #message>
						<table>
							<tr>
								<th>属性</th>
								<th>描述</th>
								<th>默认值</th>
							</tr>
							<tr>
								<td>parentData</td>
								<td>静态数据，设置后不支持分页，不支持 table ajax 请求</td>
								<td>无</td>
							</tr>
						</table>
					</template>
				</show-code>
				<div>
					<p>9、table 选择列数据</p>
					<p>
						- 需设置 pay-table 属性
						<b>isRowSelection</b>
					</p>
					<p>
						- 调用 getTableList 方法时必传 2 个参数，控制选择列的数据清空，
						<b>tableRef.getTableList(1, true)</b>
					</p>
					<p>- 获取用户选择</p>
					<show-code language="ts" :code="code3">
						<template #message>
							<table>
								<tr>
									<th>属性</th>
									<th>描述</th>
									<th>默认值</th>
								</tr>
								<tr>
									<td>isRowSelection</td>
									<td>
										是否显示 checkbox，true 时，使用 handleCatRowData
										示例获取用户选择
									</td>
									<td>false</td>
								</tr>
							</table>
						</template>
					</show-code>
				</div>
			</div>
		</template>
	</show-code>
</template>

<style lang="scss"></style>
