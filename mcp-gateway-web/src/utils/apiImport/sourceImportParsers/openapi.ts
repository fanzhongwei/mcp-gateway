import {
	generateId,
	parseJsonToParams,
	type ApiData,
} from '~/utils/apiSaveUtils'
import type { ParsedImportResult } from '../parsedImportTypes'
import { normalizePath, type SaveContext } from '../sourceImportParsers'

type OpenApiLeaf = {
	title: string
	key: string
	isLeaf: true
	path: string
	method: string
	operation: any
	summary: string
	description: string
}

export const validateApiSpec = (spec: any): boolean => {
	if (!spec || typeof spec !== 'object') return false
	if (
		spec.openapi &&
		(spec.openapi.startsWith('3.0') || spec.openapi.startsWith('3.1'))
	) {
		return true
	}
	if (spec.swagger && spec.swagger.startsWith('2.0')) {
		return true
	}
	return false
}

const parseApiSpecToTree = (spec: any): any[] => {
	const tree: any[] = []
	if (!spec || !spec.paths) return tree
	const isOpenAPI3 =
		spec.openapi &&
		(spec.openapi.startsWith('3.0') || spec.openapi.startsWith('3.1'))
	const isSwagger2 = spec.swagger && spec.swagger.startsWith('2.0')
	if (!isOpenAPI3 && !isSwagger2) return tree

	const httpMethods = [
		'get',
		'post',
		'put',
		'delete',
		'patch',
		'head',
		'options',
		'trace',
	]
	const tagMap = new Map<string, OpenApiLeaf[]>()
	const untaggedApis: OpenApiLeaf[] = []

	Object.keys(spec.paths).forEach(path => {
		const pathItem = spec.paths[path]
		httpMethods.forEach(method => {
			if (!pathItem[method]) return
			const operation = pathItem[method]
			const operationId = operation.operationId || path
			const summary = operation.summary || operationId
			const description = operation.description || ''
			const tags = operation.tags || []
			const key = `${path}::${method.toUpperCase()}`
			const apiNode: OpenApiLeaf = {
				title: `${method.toUpperCase()} ${path}`,
				key,
				isLeaf: true,
				path,
				method: method.toUpperCase(),
				operation,
				summary,
				description,
			}
			if (tags && tags.length > 0) {
				tags.forEach((tag: string) => {
					if (!tagMap.has(tag)) tagMap.set(tag, [])
					tagMap.get(tag)!.push(apiNode)
				})
			} else {
				untaggedApis.push(apiNode)
			}
		})
	})

	tagMap.forEach((apis, tag) => {
		let tagDescription = ''
		if (spec.tags && Array.isArray(spec.tags)) {
			const tagInfo = spec.tags.find((t: any) => t.name === tag)
			if (tagInfo?.description) tagDescription = tagInfo.description
		}
		tree.push({
			title: tag,
			key: `tag::${tag}`,
			children: apis,
			description: tagDescription,
		})
	})

	if (untaggedApis.length > 0) {
		tree.push({
			title: '未分组',
			key: 'tag::untagged',
			children: untaggedApis,
		})
	}

	if (tree.length === 0) {
		const pathMap = new Map<string, OpenApiLeaf[]>()
		Object.keys(spec.paths).forEach(path => {
			const pathItem = spec.paths[path]
			const pathChildren: OpenApiLeaf[] = []
			httpMethods.forEach(method => {
				if (!pathItem[method]) return
				const operation = pathItem[method]
				const operationId = operation.operationId || path
				const summary = operation.summary || operationId
				const description = operation.description || ''
				const key = `${path}::${method.toUpperCase()}`
				pathChildren.push({
					title: `${method.toUpperCase()} ${path}`,
					key,
					isLeaf: true,
					path,
					method: method.toUpperCase(),
					operation,
					summary,
					description,
				})
			})
			if (pathChildren.length > 0) pathMap.set(path, pathChildren)
		})
		pathMap.forEach((apis, path) => {
			tree.push({
				title: path,
				key: `path::${path}`,
				children: apis,
			})
		})
	}

	return tree
}

const generateJsonExample = (schema: any, spec: any = null): any => {
	if (!schema) return {}
	if (schema.$ref) {
		let refPath = ''
		let refSchema: any = null
		if (schema.$ref.startsWith('#/components/schemas/')) {
			refPath = schema.$ref.replace('#/components/schemas/', '')
			refSchema = spec?.components?.schemas?.[refPath]
		} else if (schema.$ref.startsWith('#/definitions/')) {
			refPath = schema.$ref.replace('#/definitions/', '')
			refSchema = spec?.definitions?.[refPath]
		}
		if (refSchema) return generateJsonExample(refSchema, spec)
		return {}
	}
	if (schema.allOf) {
		const result: any = {}
		schema.allOf.forEach((item: any) => {
			Object.assign(result, generateJsonExample(item, spec))
		})
		return result
	}
	if (schema.anyOf || schema.oneOf) {
		const firstItem = (schema.anyOf || schema.oneOf)[0]
		return generateJsonExample(firstItem, spec)
	}
	const type = schema.type || 'object'
	switch (type) {
		case 'object': {
			const obj: any = {}
			if (schema.properties) {
				Object.keys(schema.properties).forEach(key => {
					obj[key] = generateJsonExample(schema.properties[key], spec)
				})
			}
			return obj
		}
		case 'array':
			return schema.items ? [generateJsonExample(schema.items, spec)] : []
		case 'string':
			if (schema.enum && schema.enum.length > 0) return schema.enum[0]
			if (schema.format === 'date') return '2024-01-01'
			if (schema.format === 'date-time') return '2024-01-01T00:00:00Z'
			if (schema.format === 'email') return 'example@example.com'
			if (schema.format === 'uri') return 'https://example.com'
			return schema.example || schema.default || ''
		case 'number':
		case 'integer':
			return schema.example ?? schema.default ?? 0
		case 'boolean':
			return schema.example ?? schema.default ?? false
		case 'null':
			return null
		default:
			return schema.example || schema.default || null
	}
}

const resolveParameterRef = (param: any, spec: any): any => {
	if (!param.$ref || !spec) return param
	const refPath = param.$ref.replace('#/', '').split('/')
	if (
		refPath.length >= 3 &&
		refPath[0] === 'components' &&
		refPath[1] === 'parameters'
	) {
		return spec?.components?.parameters?.[refPath[2]] || param
	}
	return param
}

const getParameterValue = (param: any): string => {
	if (param.example !== undefined && param.example !== null) {
		return String(param.example)
	}
	if (
		param.schema &&
		param.schema.example !== undefined &&
		param.schema.example !== null
	) {
		return String(param.schema.example)
	}
	if (param.default !== undefined && param.default !== null) {
		return String(param.default)
	}
	if (
		param.schema &&
		param.schema.default !== undefined &&
		param.schema.default !== null
	) {
		return String(param.schema.default)
	}
	return ''
}

const extractSecurityHeaders = (operation: any, spec: any): any[] => {
	const securityHeaders: any[] = []
	if (!operation.security && (!spec.security || spec.security.length === 0)) {
		return securityHeaders
	}
	const security = operation.security || spec.security || []
	security.forEach((sec: any) => {
		Object.keys(sec).forEach((schemeName: string) => {
			const scheme = spec.components?.securitySchemes?.[schemeName]
			if (!scheme) return
			if (scheme.type === 'http') {
				if (scheme.scheme === 'bearer') {
					securityHeaders.push({
						key: 'Authorization',
						value: 'Bearer ',
						description:
							scheme.description ||
							`Bearer token authentication (${schemeName})`,
					})
				} else if (scheme.scheme === 'basic') {
					securityHeaders.push({
						key: 'Authorization',
						value: 'Basic ',
						description:
							scheme.description || `Basic authentication (${schemeName})`,
					})
				}
			} else if (scheme.type === 'apiKey' && scheme.in === 'header') {
				securityHeaders.push({
					key: scheme.name || 'X-API-Key',
					value: '',
					description:
						scheme.description || `API Key authentication (${schemeName})`,
				})
			} else if (scheme.type === 'oauth2') {
				securityHeaders.push({
					key: 'Authorization',
					value: 'Bearer ',
					description:
						scheme.description || `OAuth2 authentication (${schemeName})`,
				})
			}
		})
	})
	return securityHeaders
}

const convertOperationToApiData = (
	apiInfo: ParsedImportResult,
	spec: any,
	ctx: SaveContext,
): ApiData => {
	const { path, method, operation, summary, description } = apiInfo
	const pathItem = spec.paths?.[path] || {}
	const pathParameters = pathItem.parameters || []
	const operationParameters = operation.parameters || []
	const allParameters = [...pathParameters, ...operationParameters]

	const queryParams: any[] = []
	const headers: any[] = []
	const cookies: any[] = []
	let swagger2BodyParam: any = null

	allParameters.forEach((param: any) => {
		const resolvedParam = resolveParameterRef(param, spec)
		if (resolvedParam.in === 'body') {
			swagger2BodyParam = resolvedParam
			return
		}
		const paramData = {
			key: resolvedParam.name || '',
			value: getParameterValue(resolvedParam),
			type: resolvedParam.schema?.type || 'string',
			description: resolvedParam.description || '',
			required: resolvedParam.required || false,
		}
		if (resolvedParam.in === 'query') queryParams.push(paramData)
		else if (resolvedParam.in === 'header') {
			headers.push({
				key: resolvedParam.name || '',
				value: getParameterValue(resolvedParam),
				description: resolvedParam.description || '',
			})
		} else if (resolvedParam.in === 'cookie') {
			cookies.push({
				key: resolvedParam.name || '',
				value: getParameterValue(resolvedParam),
				description: resolvedParam.description || '',
			})
		}
	})

	extractSecurityHeaders(operation, spec).forEach((secHeader: any) => {
		const existingIndex = headers.findIndex(
			h => h.key.toLowerCase() === secHeader.key.toLowerCase(),
		)
		if (existingIndex === -1) headers.push(secHeader)
		else headers[existingIndex].description ||= secHeader.description
	})

	const requestBody = operation.requestBody
	const bodyConfig = { type: 'json', jsonParams: [] as any[], raw: '' }
	const formDataParams: any[] = []
	const urlEncodedParams: any[] = []

	if (requestBody) {
		const content = requestBody.content || {}
		if (content['application/json']) {
			bodyConfig.type = 'json'
			const schema = content['application/json'].schema
			bodyConfig.jsonParams = schema
				? parseJsonToParams(generateJsonExample(schema, spec))
				: []
		} else if (content['multipart/form-data']) {
			bodyConfig.type = 'form-data'
			const schema = content['multipart/form-data'].schema
			if (schema?.properties) {
				Object.keys(schema.properties).forEach(key => {
					const prop = schema.properties[key]
					formDataParams.push({
						key,
						value: '',
						type:
							prop.type === 'string' && prop.format === 'binary'
								? 'file'
								: 'text',
						description: prop.description || '',
					})
				})
			}
		} else if (content['application/x-www-form-urlencoded']) {
			bodyConfig.type = 'x-www-form-urlencoded'
			const schema = content['application/x-www-form-urlencoded'].schema
			if (schema?.properties) {
				Object.keys(schema.properties).forEach(key => {
					const prop = schema.properties[key]
					urlEncodedParams.push({
						key,
						value: '',
						description: prop.description || '',
					})
				})
			}
		}
	} else if (swagger2BodyParam) {
		bodyConfig.type = 'json'
		const schema = swagger2BodyParam.schema
		bodyConfig.jsonParams = schema
			? parseJsonToParams(generateJsonExample(schema, spec))
			: []
	}

	return {
		id: generateId(),
		apiForm: {
			method,
			path,
			name: summary || path,
			description: description || '',
		},
		queryParams,
		headers,
		bodyConfig,
		formDataParams,
		urlEncodedParams,
		cookies,
		responseConfig: { validateResponse: false, successStatus: '200' },
		systemId: ctx.systemId,
		systemName: ctx.systemName,
		selectedEnv: ctx.selectedEnv,
		saveTime: new Date().toISOString(),
		updateTime: new Date().toISOString(),
	}
}

export const parseOpenApiContent = (
	spec: any,
	ctx: SaveContext,
): ParsedImportResult[] => {
	const tree = parseApiSpecToTree(spec)
	const results: ParsedImportResult[] = []
	const traverse = (nodes: any[], groupName: string) => {
		nodes.forEach(node => {
			if (node.isLeaf) {
				const leafGroup = groupName || 'OpenAPI'
				const normalized = normalizePath(node.path, ctx)
				const base = {
					key: `${normalized.path}::${node.method}`,
					group: leafGroup,
					path: normalized.path,
					method: node.method,
					operation: node.operation,
					summary: node.summary,
					description: node.description,
				}
				results.push({
					...base,
					apiData: convertOperationToApiData(base, spec, ctx),
				})
			}
			if (node.children) {
				const nextGroup = node.isLeaf ? groupName : node.title || groupName
				traverse(node.children, nextGroup)
			}
		})
	}
	traverse(tree, 'OpenAPI')
	return results
}
