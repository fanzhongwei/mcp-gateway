import type { ParsedImportResult } from '../parsedImportTypes'
import {
	normalizePath,
	toApiData,
	type SaveContext,
} from '../sourceImportParsers'

export const parseHarContent = (
	content: string,
	ctx: SaveContext,
): ParsedImportResult[] => {
	const json = JSON.parse(content)
	const entries = json?.log?.entries
	if (!Array.isArray(entries)) throw new Error('HAR 格式错误：缺少 log.entries')
	return entries
		.filter((entry: any) => {
			const method = String(entry?.request?.method || '').toUpperCase()
			const url = String(entry?.request?.url || '')
			if (!url || method === 'OPTIONS') return false
			return !/\.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?)($|\?)/i.test(url)
		})
		.map((entry: any, idx: number) => {
			const req = entry.request || {}
			const method = String(req.method || 'GET').toUpperCase()
			const { path, query } = normalizePath(String(req.url || '/'), ctx)
			const headers = (req.headers || []).map((h: any) => ({
				key: h.name || '',
				value: h.value || '',
			}))
			const mime = String(req.postData?.mimeType || '')
			const bodyRaw = String(req.postData?.text || '')
			const bodyType = mime.includes('json')
				? 'json'
				: mime.includes('x-www-form-urlencoded')
					? 'x-www-form-urlencoded'
					: 'raw'
			const base = {
				key: `har::${idx}::${method}::${path}`,
				group: 'HAR',
				name: path,
				method,
				path,
				summary: path,
				description: '',
			}
			return {
				...base,
				apiData: toApiData({ ...base, query, headers, bodyType, bodyRaw }, ctx),
			}
		})
}
