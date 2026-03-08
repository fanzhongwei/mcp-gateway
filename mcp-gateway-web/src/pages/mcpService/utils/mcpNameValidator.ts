/*
 * @FilePath: /mcp-gateway-web/src/pages/mcpService/utils/mcpNameValidator.ts
 * @Author: teddy
 * @Date: 2026-01-27
 * @Description: MCP 名字验证工具
 */

/**
 * 验证 MCP 名字是否符合协议规范
 * 根据 MCP 协议规范，工具名称应该：
 * - 长度在 1-128 个字符之间
 * - 只包含 ASCII 字母、数字、下划线、连字符和点
 * - 不包含中文字符、空格、逗号等特殊字符
 */
export function validateMcpName(name: string): {
	valid: boolean
	message?: string
} {
	if (!name || name.trim() === '') {
		return { valid: true } // 空值允许，表示使用系统自动生成
	}

	const trimmedName = name.trim()

	// 检查长度
	if (trimmedName.length < 1 || trimmedName.length > 128) {
		return {
			valid: false,
			message: 'MCP 名字长度必须在 1-128 个字符之间',
		}
	}

	// 检查字符：只允许 ASCII 字母、数字、下划线、连字符和点
	const validPattern = /^[a-zA-Z0-9._-]+$/
	if (!validPattern.test(trimmedName)) {
		return {
			valid: false,
			message:
				'MCP 名字只能包含 ASCII 字母、数字、下划线(_)、连字符(-)和点(.)，不能包含中文字符、空格、逗号等特殊字符',
		}
	}

	return { valid: true }
}

/**
 * 格式化 MCP 名字（去除首尾空格）
 */
export function formatMcpName(name: string): string {
	if (!name) return ''
	return name.trim()
}
