/*
 * @FilePath: /mcp-gateway-web/tailwind.config.ts
 * @Author: teddy
 * @Date: 2024-02-02 15:11:22
 * @Description: tailwindcss 配置
 * @LastEditors: teddy
 * @LastEditTime: 2024-03-20 13:34:04
 */

import type { Config } from 'tailwindcss'

export default <Partial<Config>>{
	darkMode: 'class',
	theme: {
		extend: {
			colors: {
				dark: '#141414',
				tme: '#3686dc',
			},
		},
	},
	plugins: [
		function ({ addVariant }: any) {
			addVariant('child', '& > *')
			addVariant('child-hover', '& > *:hover')
		},
	],
}
