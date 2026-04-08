import type { ApiData } from '~/utils/apiSaveUtils'

export interface ParsedImportResult {
	key: string
	name?: string
	group?: string
	method: string
	path: string
	summary?: string
	description?: string
	operation?: any
	apiData?: ApiData
}
