// JSON参数树形结构类型
export interface JsonParamNode {
	id: string
	name: string
	required: boolean
	type: 'string' | 'number' | 'boolean' | 'object' | 'array'
	example: string
	description: string
	children?: JsonParamNode[]
}
