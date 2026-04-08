import type { ParsedImportResult } from '../parsedImportTypes'
import {
	normalizePath,
	toApiData,
	type BodyType,
	type SaveContext,
} from '../sourceImportParsers'

const collectApifoxDefinitionMap = (data: any): Record<string, any> => {
	const map: Record<string, any> = {}
	const walk = (nodes: any[]) => {
		nodes.forEach(node => {
			const id = typeof node?.id === 'string' ? node.id : ''
			const schema = node?.schema?.jsonSchema
			if (id && schema) {
				map[id] = schema
				if (id.startsWith('#/definitions/')) {
					map[id.replace('#/definitions/', '')] = schema
				}
			}
			if (Array.isArray(node?.items)) {
				walk(node.items)
			}
		})
	}
	;(data?.schemaCollection || []).forEach((group: any) => {
		if (Array.isArray(group?.items)) {
			walk(group.items)
		}
	})
	return map
}

const resolveApifoxRefSchema = (
	schema: any,
	definitionMap: Record<string, any>,
	seenRefs: Set<string> = new Set(),
): any => {
	if (!schema || typeof schema !== 'object') {
		return schema
	}
	if (schema.$ref && typeof schema.$ref === 'string') {
		const ref = schema.$ref
		const refKey = ref.startsWith('#/definitions/')
			? ref.replace('#/definitions/', '')
			: ref
		if (seenRefs.has(ref)) {
			return {}
		}
		const refSchema = definitionMap[ref] || definitionMap[refKey]
		if (!refSchema) {
			return {}
		}
		seenRefs.add(ref)
		const resolved = resolveApifoxRefSchema(refSchema, definitionMap, seenRefs)
		seenRefs.delete(ref)
		return resolved
	}

	if (Array.isArray(schema.allOf) && schema.allOf.length > 0) {
		return schema.allOf.reduce((acc: any, cur: any) => {
			const resolved = resolveApifoxRefSchema(cur, definitionMap, seenRefs)
			return {
				...acc,
				...resolved,
				properties: {
					...(acc?.properties || {}),
					...(resolved?.properties || {}),
				},
			}
		}, {})
	}

	if (Array.isArray(schema.oneOf) && schema.oneOf.length > 0) {
		return resolveApifoxRefSchema(schema.oneOf[0], definitionMap, seenRefs)
	}
	if (Array.isArray(schema.anyOf) && schema.anyOf.length > 0) {
		return resolveApifoxRefSchema(schema.anyOf[0], definitionMap, seenRefs)
	}

	const next: any = { ...schema }
	if (schema.properties && typeof schema.properties === 'object') {
		next.properties = Object.fromEntries(
			Object.entries(schema.properties).map(([k, v]) => [
				k,
				resolveApifoxRefSchema(v, definitionMap, seenRefs),
			]),
		)
	}
	if (schema.items) {
		next.items = resolveApifoxRefSchema(schema.items, definitionMap, seenRefs)
	}
	return next
}

const schemaToExample = (schema: any): any => {
	if (!schema || typeof schema !== 'object') return {}
	if (schema.example !== undefined) return schema.example
	if (Array.isArray(schema.examples) && schema.examples.length > 0) {
		return schema.examples[0]
	}
	if (schema.default !== undefined) return schema.default
	if (Array.isArray(schema.enum) && schema.enum.length > 0)
		return schema.enum[0]

	switch (schema.type) {
		case 'object': {
			const obj: Record<string, any> = {}
			Object.entries(schema.properties || {}).forEach(([k, v]) => {
				obj[k] = schemaToExample(v)
			})
			return obj
		}
		case 'array':
			return [schemaToExample(schema.items || {})]
		case 'integer':
		case 'number':
			return 0
		case 'boolean':
			return false
		case 'string':
		default:
			return ''
	}
}

export const parseApifoxContent = (
	content: string,
	ctx: SaveContext,
): ParsedImportResult[] => {
	const data = JSON.parse(content)
	const definitionMap = collectApifoxDefinitionMap(data)
	if (data?.paths) {
		const result: ParsedImportResult[] = []
		const methods = ['get', 'post', 'put', 'delete', 'patch', 'head', 'options']
		Object.keys(data.paths).forEach(path => {
			methods.forEach(m => {
				const op = data.paths[path]?.[m]
				if (!op) return
				const method = m.toUpperCase()
				const base = {
					key: `apifox::${method}::${path}`,
					group: op.tags?.[0] || 'Apifox',
					name: op.summary || path,
					method,
					path,
					summary: op.summary || path,
					description: op.description || '',
				}
				result.push({
					...base,
					apiData: toApiData(base, ctx),
				})
			})
		})
		return result
	}

	if (Array.isArray(data?.apiCollection)) {
		const result: ParsedImportResult[] = []
		const projectName = data?.info?.name || 'Apifox'
		const normalizeFolder = (name?: string) => {
			const n = String(name || '').trim()
			if (!n || n === '根目录') return ''
			return n
		}
		const joinFolder = (base: string, part?: string) => {
			const p = normalizeFolder(part)
			if (!p) return base
			return base ? `${base} / ${p}` : p
		}

		const walkCollection = (nodes: any[], folderPath: string) => {
			nodes.forEach(node => {
				if (node?.api) {
					const api = node.api
					const method = String(api.method || 'GET').toUpperCase()
					const rawPath = String(api.path || api.url || '/')
					const { path } = normalizePath(rawPath, ctx)
					const headers = (api.parameters?.header || api.headers || []).map(
						(h: any) => ({
							key: h.name || h.key || '',
							value: h.value || h.defaultValue || '',
							description: h.description || '',
						}),
					)
					const query = Object.fromEntries(
						(api.parameters?.query || [])
							.filter((q: any) => (q?.name || q?.key) && q?.value !== undefined)
							.map((q: any) => [q.name || q.key, String(q.value)]),
					)
					const requestSchema = api.requestBody?.jsonSchema
					let bodyRawText = ''
					const mediaType = String(
						api.requestBody?.mediaType || api.requestBody?.type || '',
					).toLowerCase()
					let bodyType: BodyType = mediaType.includes('json')
						? 'json'
						: mediaType.includes('x-www-form-urlencoded')
							? 'x-www-form-urlencoded'
							: mediaType.includes('form-data')
								? 'form-data'
								: 'raw'
					if (requestSchema && typeof requestSchema === 'object') {
						const resolvedSchema = resolveApifoxRefSchema(
							requestSchema,
							definitionMap,
						)
						bodyRawText = JSON.stringify(
							schemaToExample(resolvedSchema),
							null,
							2,
						)
						bodyType = 'json'
					} else {
						const bodyRaw = api.requestBody?.raw ?? api.requestBody?.data
						if (typeof bodyRaw === 'string') {
							bodyRawText = bodyRaw
						} else if (bodyRaw && typeof bodyRaw === 'object') {
							bodyRawText = JSON.stringify(bodyRaw, null, 2)
						}
					}

					const base = {
						key: `apifox::${api.id || node.id || result.length}::${method}::${path}`,
						group: folderPath || projectName,
						name: api.name || node.name || path,
						method,
						path,
						summary: api.name || node.name || path,
						description: api.description || node.description || '',
					}
					result.push({
						...base,
						apiData: toApiData(
							{ ...base, headers, query, bodyType, bodyRaw: bodyRawText },
							ctx,
						),
					})
				}
				if (Array.isArray(node?.items)) {
					const nextFolder = joinFolder(folderPath, node?.name)
					walkCollection(node.items, nextFolder)
				}
			})
		}
		walkCollection(data.apiCollection, '')
		if (result.length > 0) return result
	}

	const apis = data?.apis || data?.apiCollection?.apis || data?.data?.apis || []
	if (!Array.isArray(apis)) throw new Error('暂不支持该 Apifox 导出结构')
	return apis.map((api: any, idx: number) => {
		const method = String(api.method || 'GET').toUpperCase()
		const rawPath = String(api.path || api.url || '/')
		const { path } = normalizePath(rawPath, ctx)
		const headers = (api.headers || []).map((h: any) => ({
			key: h.name || h.key || '',
			value: h.value || '',
			description: h.description || '',
		}))
		const base = {
			key: `apifox::${idx}::${method}::${path}`,
			group: api.folderName || api.tag || 'Apifox',
			name: api.name || path,
			method,
			path,
			summary: api.name || path,
			description: api.description || '',
		}
		return {
			...base,
			apiData: toApiData({ ...base, headers }, ctx),
		}
	})
}
