<!--
 * @FilePath: /mcp-gateway-web/src/components/date-picker.vue
 * @Author: teddy
 * @Date: 2024-07-05 09:48:20
 * @Description: 日期区间组件
 * @LastEditors: teddy
 * @LastEditTime: 2024-07-09 11:03:23
-->
<script setup lang="ts">
const adminDateRange = ref<any>([])
// 当前年份
const currentYear = ref<any>(null)

// 不可选择的日期
const disabledDate = (current: { $y: number }) => {
	const year = currentYear.value
	return year !== null && year !== current.$y
}

// 待选日期发生变化的回调
const onCalendarChange = (val: (string | null)[]) => {
	const [firstVal, secondVal] = val
	const value = firstVal || secondVal || null

	if (value) {
		const yearMatch = value.match(/^(\d{4})-/)
		if (yearMatch) {
			currentYear.value = Number(yearMatch[1])
		} else {
			currentYear.value = null
		}
	} else {
		currentYear.value = null
	}
}

// 弹出日历和关闭日历的回调
const onOpenChange = () => {
	// 获取设置的日期范围
	const [startDate, endDate] = adminDateRange.value
	// 获取当前日期
	const value = startDate || endDate || null
	// 如果当前日期为空，则将当前年份设置为空
	if (!value) {
		currentYear.value = null
	}
}
// 双向绑定时间范围
const dateRange = defineModel('value', {
	type: Array,
	default: [],
})

watch(
	() => adminDateRange.value,
	val => {
		if (val && val.length === 2) {
			const array = [...val]
			array[0] = array[0] + ' 00:00:00'
			array[1] = array[1] + ' 23:59:59'
			dateRange.value = array
		}
	},
)

onMounted(() => {
	// 获取当前日期的格式化字符串
	const formatDate = (date: any) => {
		const year = date.getFullYear()
		const month = String(date.getMonth() + 1).padStart(2, '0')
		const day = String(date.getDate()).padStart(2, '0')
		// 设置当年年份
		currentYear.value = year
		return `${year}-${month}-${day}`
	}

	// 获取今天的日期
	const today = new Date()
	const formattedDate = formatDate(today)

	// 获取前6天的日期
	const pastDate = new Date(today)
	pastDate.setDate(today.getDate() - 6)

	// 如果前6天的日期跨年，则取当年的第一天
	if (pastDate.getFullYear() < today.getFullYear()) {
		pastDate.setFullYear(today.getFullYear())
		pastDate.setMonth(0, 1) // 设置为当年的1月1日
	}

	const pastFormattedDate = formatDate(pastDate)

	// 设置日期范围
	adminDateRange.value = [pastFormattedDate, formattedDate]
})
</script>

<template>
	<a-range-picker
		v-model:value="adminDateRange"
		valueFormat="YYYY-MM-DD"
		:disabled-date="disabledDate"
		:placeholder="['开始日期(必填)', '结束日期(必填)']"
		:allowClear="false"
		@calendar-change="onCalendarChange"
		@open-change="onOpenChange"
	/>
</template>

<style lang="scss"></style>
