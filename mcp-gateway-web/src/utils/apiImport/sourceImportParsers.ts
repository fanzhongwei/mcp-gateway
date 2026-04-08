import {
	generateId,
	parseJsonToParams,
	type ApiData,
} from '~/utils/apiSaveUtils'
import type { ParsedImportResult } from './parsedImportTypes'

export type SaveContext = {
	systemId?: string
	systemName?: string
	selectedEnv?: any | null
}

export type BodyType = 'json' | 'raw' | 'form-data' | 'x-www-form-urlencoded'

const getEnvContextPath = (ctx?: SaveContext): string => {
	const baseUrl = String(ctx?.selectedEnv?.baseUrl || '').trim()
	if (!baseUrl) return ''
	try {
		const url = new URL(baseUrl)
		const path = (url.pathname || '').replace(/\/+$/, '')
		return path === '/' ? '' : path
	} catch {
		const normalized = baseUrl
			.replace(/^https?:\/\/[^/]+/i, '')
			.replace(/\/+$/, '')
		return normalized === '/' ? '' : normalized
	}
}

export const normalizePath = (
	urlLike: string,
	ctx?: SaveContext,
): { path: string; query: Record<string, string> } => {
	const contextPath = getEnvContextPath(ctx)
	const stripContextPath = (path: string) => {
		if (!contextPath || !path.startsWith('/')) return path || '/'
		if (path === contextPath) return '/'
		if (path.startsWith(`${contextPath}/`)) {
			const stripped = path.slice(contextPath.length)
			return stripped.startsWith('/') ? stripped : `/${stripped}`
		}
		return path || '/'
	}
	try {
		const url = new URL(urlLike)
		const query: Record<string, string> = {}
		url.searchParams.forEach((value, key) => {
			query[key] = value
		})
		return { path: stripContextPath(url.pathname || '/'), query }
	} catch {
		const [path, search] = urlLike.split('?')
		const query: Record<string, string> = {}
		if (search) {
			const params = new URLSearchParams(search)
			params.forEach((value, key) => {
				query[key] = value
			})
		}
		return { path: stripContextPath(path || '/'), query }
	}
}

const parseJsonBodyParams = (raw: string) => {
	try {
		return parseJsonToParams(JSON.parse(raw))
	} catch {
		return []
	}
}

export const toApiData = (
	item: Omit<ParsedImportResult, 'apiData'> & {
		headers?: Array<{ key: string; value: string; description?: string }>
		query?: Record<string, string>
		bodyType?: BodyType
		bodyRaw?: string
	},
	ctx: SaveContext,
): ApiData => {
	const bodyType = item.bodyType || 'raw'
	const bodyRaw = item.bodyRaw || ''
	return {
		id: generateId(),
		apiForm: {
			method: item.method,
			path: item.path,
			name: item.name || item.summary || item.path,
			description: item.description || '',
		},
		queryParams: Object.entries(item.query || {}).map(([key, value]) => ({
			key,
			value,
			type: 'string',
			description: '',
			required: false,
		})),
		headers: (item.headers || []).map(h => ({
			key: h.key,
			value: h.value,
			description: h.description || '',
			required: false,
		})),
		bodyConfig: {
			type: bodyType,
			raw: bodyRaw,
			jsonParams: bodyType === 'json' ? parseJsonBodyParams(bodyRaw) : [],
		},
		formDataParams: [],
		urlEncodedParams: [],
		cookies: [],
		responseConfig: {
			validateResponse: false,
			successStatus: '200',
		},
		systemId: ctx.systemId,
		systemName: ctx.systemName,
		selectedEnv: ctx.selectedEnv,
		saveTime: new Date().toISOString(),
		updateTime: new Date().toISOString(),
	}
}
