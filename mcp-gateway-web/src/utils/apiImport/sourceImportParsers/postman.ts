import type { ParsedImportResult } from '../parsedImportTypes'
import {
	normalizePath,
	toApiData,
	type BodyType,
	type SaveContext,
} from '../sourceImportParsers'

const parsePostmanItems = (
	items: any[],
	groupName: string,
	ctx: SaveContext,
): ParsedImportResult[] => {
	const result: ParsedImportResult[] = []
	items.forEach(item => {
		if (item.item && Array.isArray(item.item)) {
			result.push(...parsePostmanItems(item.item, item.name || groupName, ctx))
			return
		}
		const req = item.request
		if (!req) return
		const method = (req.method || 'GET').toUpperCase()
		const rawUrl = typeof req.url === 'string' ? req.url : req.url?.raw || ''
		const { path, query } = normalizePath(rawUrl || '/', ctx)
		const headers =
			(req.header || []).map((h: any) => ({
				key: h.key || '',
				value: h.value || '',
				description: h.description || '',
			})) || []
		let bodyRaw = ''
		let bodyType: BodyType = 'raw'
		if (req.body?.mode === 'raw') {
			bodyRaw = req.body.raw || ''
			bodyType = bodyRaw.trim().startsWith('{') ? 'json' : 'raw'
		} else if (req.body?.mode === 'urlencoded') {
			bodyType = 'x-www-form-urlencoded'
		} else if (req.body?.mode === 'formdata') {
			bodyType = 'form-data'
		}
		const base = {
			key: `postman::${method}::${path}::${item.name || ''}`,
			group: groupName || 'Postman',
			name: item.name || path,
			method,
			path,
			summary: item.name || path,
			description: '',
		}
		result.push({
			...base,
			apiData: toApiData({ ...base, query, headers, bodyType, bodyRaw }, ctx),
		})
	})
	return result
}

export const parsePostmanContent = (
	content: string,
	ctx: SaveContext,
): ParsedImportResult[] => {
	const json = JSON.parse(content)
	if (!Array.isArray(json?.item))
		throw new Error('Postman 文件格式错误：缺少 item 数组')
	return parsePostmanItems(json.item, json?.info?.name || 'Postman', ctx)
}
