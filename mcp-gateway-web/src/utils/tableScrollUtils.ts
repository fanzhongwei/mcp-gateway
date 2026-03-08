/*
 * @FilePath: /mcp-gateway-web/src/utils/tableScrollUtils.ts
 * @Author: teddy
 * @Date: 2026-01-28
 * @Description: 表格滚动高度计算工具函数
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-28
 */
import { nextTick, type Ref } from 'vue'

/**
 * 表格滚动配置接口
 */
export interface TableScrollConfig {
	containerRef: Ref<HTMLElement | null>
	scrollY: Ref<number | undefined>
	headerSelector?: string // 可选的 header 选择器，默认为 '.tab-content-header'
	heightAdjustment?: number // 高度计算的修订值，用于微调不同页面布局的高度偏差，默认 0
}

/**
 * 计算单个表格的滚动高度
 * @param config 表格配置对象
 * @param options 可选配置
 */
export const calculateSingleTableScrollY = (
	config: TableScrollConfig,
	options?: {
		headerMarginBottom?: number // header 的 margin-bottom，默认 12
		containerPadding?: number // 容器的上下 padding 总和，默认 32
		tableHeaderHeight?: number // 表格表头高度，默认 40
		minHeight?: number // 最小高度，默认 200
	},
) => {
	if (!config.containerRef.value) {
		config.scrollY.value = undefined
		return
	}

	const container = config.containerRef.value
	if (!container) {
		config.scrollY.value = undefined
		return
	}

	// 获取 header 元素（支持自定义选择器）
	const headerSelector = config.headerSelector || '.tab-content-header'
	const header = container.querySelector(headerSelector)
	if (!header) {
		config.scrollY.value = undefined
		return
	}

	// 使用 getBoundingClientRect 获取实际渲染高度
	// 这样可以准确获取 flex 布局计算后的实际高度
	const containerRect = container.getBoundingClientRect()
	const headerRect = (header as HTMLElement).getBoundingClientRect()

	const containerHeight = containerRect.height
	const headerHeight = headerRect.height + (options?.headerMarginBottom ?? 12) // header 高度 + margin-bottom
	const padding = options?.containerPadding ?? 32 // 上下 padding 16px * 2
	const tableHeaderHeight = options?.tableHeaderHeight ?? 40 // Ant Design Table 表头高度
	const minHeight = options?.minHeight ?? 200 // 最小高度
	const heightAdjustment = config.heightAdjustment ?? 0 // 高度修订值，用于微调不同页面布局的高度偏差
	const availableHeight =
		containerHeight -
		headerHeight -
		padding -
		tableHeaderHeight +
		heightAdjustment

	// 设置最小高度，最大高度为可用高度
	// 如果可用高度小于最小高度，不设置滚动，让 table 自然显示
	// 同时确保高度不会为负数
	if (availableHeight > minHeight) {
		config.scrollY.value = Math.max(minHeight, availableHeight)
	} else if (availableHeight > 0) {
		// 即使小于最小高度，如果还有空间，也设置一个较小的滚动高度
		config.scrollY.value = availableHeight
	} else {
		config.scrollY.value = undefined // 如果高度太小或为负，不设置滚动
	}
}

/**
 * 计算所有表格的滚动高度
 * @param configs 表格配置数组
 * @param options 可选配置
 */
export const calculateAllTablesScrollY = (
	configs: TableScrollConfig[],
	options?: {
		headerMarginBottom?: number
		containerPadding?: number
		tableHeaderHeight?: number
		minHeight?: number
	},
) => {
	nextTick(() => {
		configs.forEach(config => {
			calculateSingleTableScrollY(config, options)
		})
	})
}

/**
 * 创建表格滚动高度管理的 composable
 * @param configs 表格配置数组
 * @param options 可选配置
 * @returns 返回计算函数和清理函数
 */
export const useTableScroll = (
	configs: TableScrollConfig[],
	options?: {
		headerMarginBottom?: number
		containerPadding?: number
		tableHeaderHeight?: number
		minHeight?: number
	},
) => {
	let resizeObserver: ResizeObserver | null = null

	/**
	 * 计算所有表格的滚动高度
	 */
	const calculate = () => {
		calculateAllTablesScrollY(configs, options)
	}

	/**
	 * 初始化 ResizeObserver，监听容器大小变化
	 * @param additionalContainers 额外的容器，如父容器等
	 */
	const initResizeObserver = (additionalContainers?: HTMLElement[]) => {
		// 清理旧的 observer
		if (resizeObserver) {
			resizeObserver.disconnect()
		}

		// 收集所有需要监听的容器
		const allContainers: HTMLElement[] = []
		configs.forEach(config => {
			if (config.containerRef.value) {
				allContainers.push(config.containerRef.value)
			}
		})

		if (additionalContainers) {
			allContainers.push(...additionalContainers)
		}

		if (allContainers.length > 0) {
			resizeObserver = new ResizeObserver(() => {
				calculate()
			})

			// 监听所有容器
			allContainers.forEach(container => {
				resizeObserver!.observe(container)
			})
		}
	}

	/**
	 * 清理资源
	 */
	const cleanup = () => {
		if (resizeObserver) {
			resizeObserver.disconnect()
			resizeObserver = null
		}
		window.removeEventListener('resize', calculate)
	}

	/**
	 * 初始化窗口大小监听
	 */
	const initWindowResize = () => {
		window.addEventListener('resize', calculate)
	}

	return {
		calculate,
		initResizeObserver,
		cleanup,
		initWindowResize,
	}
}
