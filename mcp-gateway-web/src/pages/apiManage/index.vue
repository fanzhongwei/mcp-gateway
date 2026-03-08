<!--
 * @FilePath: /mcp-gateway-web/src/pages/apiManage/index.vue
 * @Author: teddy
 * @Date: 2026-01-23
 * @Description: 接口管理
 * @LastEditors: teddy
 * @LastEditTime: 2026-01-23
-->
<script setup lang="tsx">
import { Modal } from 'ant-design-vue'
import { fetchSystemList } from '~/fetch/http'
import { getApiList, deleteApi } from '~/utils/apiSaveUtils'
import ApiImport from './import.vue'

// 选中的业务系统
const selectedSystem = ref<any>(null)

// 是否显示导入页面
const showImport = ref(false)

// 业务系统列表
const systemList = ref<any[]>([])
const systemListLoading = ref(false)

// 分页配置
const pagination = reactive({
	current: 1,
	pageSize: 10,
	total: 0,
	showSizeChanger: true,
	showTotal: (total: number) => `共 ${total} 条`,
	pageSizeOptions: ['10', '20', '50', '100'],
})

// 获取业务系统列表
const getSystemList = async (current?: number) => {
	try {
		systemListLoading.value = true
		const currentPage = current || pagination.current
		const { data } = await fetchSystemList({
			page: {
				current: currentPage,
				size: pagination.pageSize,
			},
			entityParam: {},
		})
		systemList.value = data.records || []
		pagination.total = data.total || 0
		pagination.current = currentPage

		// 检查当前选中的系统是否在新列表中
		if (selectedSystem.value) {
			const found = systemList.value.find(
				(item: any) => item.id === selectedSystem.value.id,
			)
			if (!found) {
				// 如果选中的系统不在当前页，清除选中状态
				selectedSystem.value = null
			}
		}

		// 默认选中第一个（仅在首次加载或没有选中项时）
		if (systemList.value.length > 0 && !selectedSystem.value) {
			selectedSystem.value = systemList.value[0]
		}
	} catch (error) {
		console.error('获取业务系统列表失败', error)
	} finally {
		systemListLoading.value = false
	}
}

// 分页变化处理
const handlePaginationChange = (page: number, pageSize: number) => {
	if (pagination.pageSize !== pageSize) {
		pagination.pageSize = pageSize
		pagination.current = 1
		getSystemList(1)
	} else {
		getSystemList(page)
	}
}

// 导入组件引用
const importComponentRef = ref<InstanceType<typeof ApiImport> | null>(null)

// 选择业务系统
const handleSelectSystem = (system: any) => {
	// 如果当前在导入页面，且已选择数据源（不在数据源选择页面），需要提示用户
	if (showImport.value && importComponentRef.value) {
		const selectedType = importComponentRef.value.selectedType
		// 如果已选择数据源（selectedType不为null），需要确认
		// 如果还未选择数据源（selectedType为null），可以直接切换，无需提示
		if (selectedType !== null) {
			Modal.confirm({
				title: '切换业务系统',
				content:
					'当前正在编辑/导入接口，切换业务系统将关闭当前页面，是否继续？',
				okText: '确定',
				cancelText: '取消',
				async onOk() {
					// 关闭导入页面
					showImport.value = false
					editingApiId.value = null
					// 切换业务系统
					selectedSystem.value = system
					// 加载新系统的接口列表
					await loadApiList()
				},
			})
			return
		}
		// 如果还未选择数据源，直接关闭导入页面并切换
		showImport.value = false
		editingApiId.value = null
	}

	// 切换业务系统
	selectedSystem.value = system
	// 根据选中的系统加载接口列表
	loadApiList()
}

// 接口列表
const apiList = ref<any[]>([])
const apiListLoading = ref(false)

// 从后端加载接口列表
const loadApiList = async () => {
	if (!selectedSystem.value) {
		apiList.value = []
		return
	}

	try {
		apiListLoading.value = true
		const list = await getApiList(
			selectedSystem.value.id,
			selectedSystem.value.name,
			null,
		)
		apiList.value = list || []
	} catch (error) {
		console.error('读取接口列表失败:', error)
		apiList.value = []
	} finally {
		apiListLoading.value = false
	}
}

// 获取当前业务系统的接口列表
const getCurrentSystemApiList = computed(() => {
	if (!selectedSystem.value) {
		return []
	}
	return apiList.value.filter(
		(api: any) => api.systemId === selectedSystem.value.id,
	)
})

// 跳转到业务系统页面
const handleGoToBusinessSystem = () => {
	navigateTo('/businessSystem')
}

// 编辑的接口ID
const editingApiId = ref<string | null>(null)

// 打开导入页面
const handleOpenImport = () => {
	if (!selectedSystem.value) {
		// TODO: 可以添加提示，需要先选择业务系统
		return
	}
	editingApiId.value = null
	showImport.value = true
}

// 关闭导入页面
const handleCloseImport = async () => {
	showImport.value = false
	editingApiId.value = null
	// 重新加载接口列表，因为可能保存了新接口或编辑了接口
	await loadApiList()
}

// 编辑模式下保存成功，不关闭页面，只刷新列表
const handleSaved = async () => {
	// 重新加载接口列表
	await loadApiList()
}

// 编辑接口
const handleEditApi = (api: any) => {
	if (!selectedSystem.value) {
		return
	}
	editingApiId.value = api.id
	showImport.value = true
}

// 删除接口
const handleDeleteApi = (api: any) => {
	Modal.confirm({
		title: '确认删除',
		content: `确定要删除接口"${api.apiForm.name}"吗？`,
		okText: '确定',
		cancelText: '取消',
		async onOk() {
			const success = await deleteApi(api.id)
			if (success) {
				// 重新加载接口列表
				await loadApiList()
			}
		},
	})
}

onMounted(async () => {
	await getSystemList()
	// 等待系统列表加载完成后再加载接口列表
	if (selectedSystem.value) {
		await loadApiList()
	}
})
</script>

<template>
	<div class="api-manage-container">
		<!-- 左侧：业务系统列表 -->
		<div class="system-list-container">
			<div class="system-list-header">
				<h3 class="system-list-title">业务系统</h3>
			</div>
			<div class="system-list-content">
				<!-- 没有业务系统时的提示 -->
				<div
					v-if="!systemListLoading && systemList.length === 0"
					class="system-empty"
				>
					<a-empty description="暂无业务系统">
						<template #image>
							<i
								class="ri-computer-line"
								style="font-size: 48px; color: #d9d9d9"
							></i>
						</template>
						<template #description>
							<div class="empty-description">
								<p class="empty-text">还没有业务系统</p>
								<p class="empty-hint">请先添加业务系统，然后才能管理接口</p>
							</div>
						</template>
					</a-empty>
					<a-button
						type="primary"
						class="empty-button"
						@click="handleGoToBusinessSystem"
					>
						<i class="ri-add-line mr-1"></i>
						前往添加业务系统
					</a-button>
				</div>
				<!-- 有业务系统时的列表 -->
				<a-spin v-else :spinning="systemListLoading">
					<a-list
						:data-source="systemList"
						:loading="systemListLoading"
						class="system-list"
					>
						<template #renderItem="{ item }">
							<a-list-item
								class="system-list-item"
								:class="{
									'is-selected': selectedSystem?.id === item.id,
								}"
								@click="handleSelectSystem(item)"
							>
								<a-list-item-meta>
									<template #title>
										<span class="system-name">
											{{ item.name }}
										</span>
									</template>
									<template #description>
										<div class="system-desc">
											{{ item.desc || '-' }}
										</div>
									</template>
								</a-list-item-meta>
							</a-list-item>
						</template>
						<template #empty>
							<a-empty description="暂无业务系统" />
						</template>
					</a-list>
				</a-spin>
			</div>
			<!-- 分页 -->
			<div
				v-if="!systemListLoading && systemList.length > 0"
				class="system-list-pagination"
			>
				<a-pagination
					v-model:current="pagination.current"
					v-model:page-size="pagination.pageSize"
					:total="pagination.total"
					:show-size-changer="pagination.showSizeChanger"
					:show-total="pagination.showTotal"
					:page-size-options="pagination.pageSizeOptions"
					size="small"
					@change="handlePaginationChange"
					@show-size-change="handlePaginationChange"
				/>
			</div>
		</div>

		<!-- 右侧：接口列表或导入页面 -->
		<div class="api-list-container">
			<!-- 导入页面 -->
			<ApiImport
				v-if="showImport"
				ref="importComponentRef"
				:system-id="selectedSystem?.id"
				:system-name="selectedSystem?.name"
				:editing-api-id="editingApiId"
				@close="handleCloseImport"
				@saved="handleSaved"
			/>
			<!-- 接口列表 -->
			<template v-else>
				<div class="api-list-header">
					<div class="api-list-header-left">
						<h3 class="api-list-title">
							{{
								selectedSystem
									? `${selectedSystem.name} - 接口列表`
									: '请选择业务系统'
							}}
						</h3>
					</div>
					<div class="api-list-header-right">
						<a-button
							v-if="selectedSystem"
							type="primary"
							@click="handleOpenImport"
						>
							<i class="ri-upload-2-line mr-1"></i>
							导入
						</a-button>
					</div>
				</div>
				<div class="api-list-content">
					<div v-if="!selectedSystem" class="api-empty">
						<a-empty description="请从左侧选择一个业务系统" />
					</div>
					<a-spin v-else :spinning="apiListLoading">
						<div
							v-if="getCurrentSystemApiList.length === 0 && !apiListLoading"
							class="api-empty"
						>
							<a-empty description="暂无接口数据" />
						</div>
						<div
							v-else-if="getCurrentSystemApiList.length > 0"
							class="api-list-wrapper"
						>
							<a-list :data-source="getCurrentSystemApiList" class="api-list">
								<template #renderItem="{ item }">
									<a-list-item class="api-list-item">
										<a-list-item-meta>
											<template #title>
												<div class="api-item-header">
													<span class="api-name">{{ item.apiForm.name }}</span>
													<a-tag
														:color="
															item.apiForm.method === 'GET'
																? 'blue'
																: item.apiForm.method === 'POST'
																	? 'green'
																	: item.apiForm.method === 'PUT'
																		? 'orange'
																		: item.apiForm.method === 'DELETE'
																			? 'red'
																			: 'default'
														"
													>
														{{ item.apiForm.method }}
													</a-tag>
												</div>
											</template>
											<template #description>
												<div class="api-item-info">
													<div class="api-path">
														<i class="ri-link mr-1"></i>
														{{ item.apiForm.path }}
													</div>
													<div v-if="item.apiForm.description" class="api-desc">
														{{ item.apiForm.description }}
													</div>
													<div class="api-meta">
														<span class="api-meta-item">
															<i class="ri-time-line mr-1"></i>
															{{ new Date(item.saveTime).toLocaleString() }}
														</span>
													</div>
												</div>
											</template>
										</a-list-item-meta>
										<div class="api-item-actions">
											<a-button
												type="link"
												size="small"
												@click="handleEditApi(item)"
											>
												<i class="ri-edit-line mr-1"></i>
												编辑
											</a-button>
											<a-button
												type="link"
												danger
												size="small"
												@click="handleDeleteApi(item)"
											>
												<i class="ri-delete-bin-line mr-1"></i>
												删除
											</a-button>
										</div>
									</a-list-item>
								</template>
							</a-list>
						</div>
					</a-spin>
				</div>
			</template>
		</div>
	</div>
</template>

<style lang="scss" scoped>
.api-manage-container {
	display: flex;
	height: 100%;
	background-color: #fff;
	.dark & {
		background-color: #1f1f1f;
	}

	// 左侧：业务系统列表
	.system-list-container {
		width: 256px;
		display: flex;
		flex-direction: column;
		border-right: 1px solid #f0f0f0;
		background-color: #fff;
		.dark & {
			border-right-color: #303030;
			background-color: #1f1f1f;
		}

		.system-list-header {
			padding: 16px;
			border-bottom: 1px solid #f0f0f0;
			min-height: 65px;
			display: flex;
			align-items: center;
			.dark & {
				border-bottom-color: #303030;
			}

			.system-list-title {
				margin: 0;
				font-size: 16px;
				font-weight: 600;
				color: rgba(0, 0, 0, 0.9);
				line-height: 1.5;
				.dark & {
					color: rgba(255, 255, 255, 0.9);
				}
			}
		}

		.system-list-content {
			flex: 1;
			overflow-y: auto;
			min-height: 0;
			display: flex;
			flex-direction: column;

			.system-empty {
				flex: 1;
				display: flex;
				flex-direction: column;
				align-items: center;
				justify-content: center;
				padding: 40px 20px;

				.empty-description {
					margin-top: 16px;
					text-align: center;

					.empty-text {
						margin: 0 0 8px 0;
						font-size: 16px;
						font-weight: 500;
						color: rgba(0, 0, 0, 0.85);
						.dark & {
							color: rgba(255, 255, 255, 0.85);
						}
					}

					.empty-hint {
						margin: 0;
						font-size: 14px;
						color: rgba(0, 0, 0, 0.45);
						.dark & {
							color: rgba(255, 255, 255, 0.45);
						}
					}
				}

				.empty-button {
					margin-top: 24px;
				}
			}
		}

		.system-list-pagination {
			padding: 12px 16px;
			border-top: 1px solid #f0f0f0;
			background-color: #fff;
			.dark & {
				border-top-color: #303030;
				background-color: #1f1f1f;
			}
		}

		.system-list {
			.system-list-item {
				padding: 12px 16px;
				cursor: pointer;
				transition: all 0.2s;
				border-bottom: 1px solid #f0f0f0;
				.dark & {
					border-bottom-color: #303030;
				}

				&:hover {
					background-color: #f5f5f5;
					.dark & {
						background-color: #262626;
					}
				}

				&.is-selected {
					background-color: #e6f7ff;
					.dark & {
						background-color: rgba(24, 144, 255, 0.2);
					}

					.system-name {
						color: #1890ff;
						.dark & {
							color: #69b7ff;
						}
					}
				}

				.system-name {
					font-weight: 500;
					color: rgba(0, 0, 0, 0.9);
					.dark & {
						color: rgba(255, 255, 255, 0.9);
					}
				}

				.system-desc {
					margin-top: 4px;
					font-size: 12px;
					color: rgba(0, 0, 0, 0.5);
					.dark & {
						color: rgba(255, 255, 255, 0.4);
					}
				}
			}
		}
	}

	// 右侧：接口列表
	.api-list-container {
		flex: 1;
		display: flex;
		flex-direction: column;
		background-color: #fff;
		.dark & {
			background-color: #1f1f1f;
		}

		.api-list-header {
			padding: 16px;
			border-bottom: 1px solid #f0f0f0;
			display: flex;
			justify-content: space-between;
			align-items: center;
			.dark & {
				border-bottom-color: #303030;
			}

			.api-list-header-left {
				flex: 1;
			}

			.api-list-header-right {
				flex-shrink: 0;
				display: flex;
				align-items: center;
				gap: 12px;
			}

			.api-list-title {
				margin: 0;
				font-size: 16px;
				font-weight: 600;
				color: rgba(0, 0, 0, 0.9);
				.dark & {
					color: rgba(255, 255, 255, 0.9);
				}
			}
		}

		.api-list-content {
			flex: 1;
			padding: 16px;
			overflow-y: auto;

			.api-empty {
				display: flex;
				align-items: center;
				justify-content: center;
				height: 100%;
			}

			.api-list-wrapper {
				.api-list {
					:deep(.ant-list-item) {
						display: flex;
						align-items: flex-start;
						justify-content: space-between;
					}

					.api-list-item {
						display: flex;
						align-items: flex-start;
						justify-content: space-between;
						padding: 16px;
						border: 1px solid #f0f0f0;
						border-radius: 4px;
						margin-bottom: 12px;
						background-color: #fff;
						transition: all 0.2s;
						.dark & {
							border-color: #303030;
							background-color: #1f1f1f;
						}

						&:hover {
							box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
							.dark & {
								box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
							}
						}

						:deep(.ant-list-item-meta) {
							flex: 1;
							min-width: 0;
						}

						.api-item-actions {
							display: flex;
							align-items: center;
							gap: 8px;
							flex-shrink: 0;
							margin-left: 16px;
						}

						.api-item-header {
							display: flex;
							align-items: center;
							gap: 12px;

							.api-name {
								font-size: 16px;
								font-weight: 500;
								color: rgba(0, 0, 0, 0.85);
								.dark & {
									color: rgba(255, 255, 255, 0.85);
								}
							}
						}

						.api-item-info {
							margin-top: 8px;

							.api-path {
								font-size: 14px;
								color: rgba(0, 0, 0, 0.65);
								margin-bottom: 8px;
								.dark & {
									color: rgba(255, 255, 255, 0.65);
								}
							}

							.api-desc {
								font-size: 13px;
								color: rgba(0, 0, 0, 0.45);
								margin-bottom: 8px;
								.dark & {
									color: rgba(255, 255, 255, 0.45);
								}
							}

							.api-meta {
								display: flex;
								align-items: center;
								gap: 16px;
								margin-top: 8px;

								.api-meta-item {
									font-size: 12px;
									color: rgba(0, 0, 0, 0.45);
									.dark & {
										color: rgba(255, 255, 255, 0.45);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
</style>
