import type { ParsedImportResult } from '../parsedImportTypes'
import {
	normalizePath,
	toApiData,
	type SaveContext,
} from '../sourceImportParsers'

const normalizeCurlInput = (input: string) =>
	input
		.replace(/\\\r?\n/g, ' ')
		.replace(/\r?\n/g, ' ')
		.trim()

const stripShellQuotes = (value: string) => {
	if (!value) return value
	if (
		(value.startsWith("'") && value.endsWith("'")) ||
		(value.startsWith('"') && value.endsWith('"'))
	) {
		return value.slice(1, -1)
	}
	return value
}

const tokenizeShellLike = (input: string): string[] => {
	const tokens: string[] = []
	let current = ''
	let quote: "'" | '"' | null = null
	let escapeNext = false

	for (let i = 0; i < input.length; i++) {
		const ch = input[i]
		if (escapeNext) {
			current += ch
			escapeNext = false
			continue
		}
		if (ch === '\\' && quote === '"') {
			escapeNext = true
			continue
		}
		if (quote) {
			if (ch === quote) {
				quote = null
			} else {
				current += ch
			}
			continue
		}
		if (ch === "'" || ch === '"') {
			quote = ch
			continue
		}
		if (/\s/.test(ch)) {
			if (current) {
				tokens.push(current)
				current = ''
			}
			continue
		}
		current += ch
	}
	if (current) {
		tokens.push(current)
	}
	return tokens
}

export const parseCurlCommand = (
	content: string,
	ctx: SaveContext,
): ParsedImportResult[] => {
	const tokens = tokenizeShellLike(normalizeCurlInput(content))
	if (!tokens.length || tokens[0] !== 'curl')
		throw new Error('请输入以 curl 开头的命令')
	let method = 'GET'
	let url = ''
	let bodyRaw = ''
	const headers: Array<{ key: string; value: string }> = []
	for (let i = 1; i < tokens.length; i++) {
		const t = tokens[i]
		if ((t === '-X' || t === '--request') && tokens[i + 1]) {
			method = stripShellQuotes(tokens[++i]).toUpperCase()
		} else if (t === '--url' && tokens[i + 1]) {
			url = stripShellQuotes(tokens[++i])
		} else if ((t === '-H' || t === '--header') && tokens[i + 1]) {
			const [k, ...rest] = stripShellQuotes(tokens[++i]).split(':')
			headers.push({ key: k.trim(), value: rest.join(':').trim() })
		} else if (
			(t === '-d' ||
				t === '--data' ||
				t === '--data-raw' ||
				t === '--data-binary') &&
			tokens[i + 1]
		) {
			bodyRaw = stripShellQuotes(tokens[++i])
			if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) method = 'POST'
		} else if (!t.startsWith('-') && !url) {
			const candidate = stripShellQuotes(t)
			if (/^https?:\/\//i.test(candidate) || candidate.startsWith('/')) {
				url = candidate
			}
		}
	}
	if (!url) throw new Error('未识别到 URL')
	const { path, query } = normalizePath(url, ctx)
	const contentType =
		headers.find(h => h.key.toLowerCase() === 'content-type')?.value || ''
	const bodyType = contentType.includes('json')
		? 'json'
		: contentType.includes('x-www-form-urlencoded')
			? 'x-www-form-urlencoded'
			: 'raw'
	const base = {
		key: `curl::${method}::${path}`,
		group: 'cURL',
		name: path,
		method,
		path,
		summary: path,
		description: '',
	}
	return [
		{
			...base,
			apiData: toApiData({ ...base, query, headers, bodyType, bodyRaw }, ctx),
		},
	]
}
