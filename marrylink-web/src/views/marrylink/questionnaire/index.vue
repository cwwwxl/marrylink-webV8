<template>
  <div class="questionnaire-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Document /></el-icon>
        问卷管理
      </h2>
      <p class="page-description">管理婚礼问卷模板和新人提交记录</p>
    </div>

    <el-tabs v-model="activeTab" class="questionnaire-tabs">
      <!-- 问卷模板 -->
      <el-tab-pane name="template">
        <template #label>
          <span class="tab-label">
            <el-icon><Tickets /></el-icon>
            问卷模板
          </span>
        </template>

        <!-- 搜索区域 -->
        <el-card class="search-card" shadow="never">
          <el-form :model="templateQuery" :inline="true">
            <el-form-item label="模板类型">
              <el-select
                v-model="templateQuery.type"
                placeholder="请选择模板类型"
                clearable
                style="width: 200px"
              >
                <el-option
                  v-for="tag in tagDict"
                  :key="tag.id"
                  :label="tag.name"
                  :value="tag.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="templateQuery.status"
                placeholder="全部"
                clearable
                style="width: 120px"
              >
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="fetchTemplateList">搜索</el-button>
              <el-button :icon="Refresh" @click="handleResetTemplateQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 数据表格 -->
        <el-card class="table-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">模板列表</span>
              <el-button type="primary" :icon="Plus" @click="handleOpenTemplateDialog()">
                新增模板
              </el-button>
            </div>
          </template>

          <el-table
            v-loading="templateLoading"
            :data="templateList"
            border
            stripe
            class="data-table"
          >
            <el-table-column label="模板名称" prop="name" min-width="200" show-overflow-tooltip>
              <template #default="scope">
                <div class="template-name">
                  <el-icon class="name-icon"><Document /></el-icon>
                  <span>{{ scope.row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="模板类型" prop="type" width="140" align="center">
              <template #default="scope">
                <el-tag type="primary" effect="plain">
                  {{ getTagName(scope.row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="问题数量" prop="questionCount" width="100" align="center">
              <template #default="scope">
                <el-text class="count-text">{{ scope.row.questionCount || 0 }}</el-text>
              </template>
            </el-table-column>
            <el-table-column label="使用次数" prop="useCount" width="100" align="center">
              <template #default="scope">
                <el-text type="success" class="count-text">{{ scope.row.useCount || 0 }}</el-text>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-switch
                  v-model="scope.row.status"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleToggleTemplateStatus(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" width="180" align="center" />
            <el-table-column label="操作" fixed="right" width="200" align="center">
              <template #default="scope">
                <el-button
                  type="primary"
                  link
                  size="small"
                  :icon="View"
                  @click="handleViewTemplate(scope.row)"
                >
                  查看
                </el-button>
                <el-button
                  type="primary"
                  link
                  size="small"
                  :icon="Edit"
                  @click="handleOpenTemplateDialog(scope.row.id)"
                >
                  编辑
                </el-button>
                <el-button
                  type="danger"
                  link
                  size="small"
                  :icon="Delete"
                  @click="handleDeleteTemplate(scope.row.id)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-if="templateTotal > 0"
            :total="templateTotal"
            :page="templateQuery.current"
            :limit="templateQuery.size"
            @update:page="templateQuery.current = $event"
            @update:limit="templateQuery.size = $event"
            @pagination="fetchTemplateList"
          />
        </el-card>
      </el-tab-pane>

      <!-- 提交记录 -->
      <el-tab-pane name="submission">
        <template #label>
          <span class="tab-label">
            <el-icon><List /></el-icon>
            提交记录
          </span>
        </template>

        <!-- 搜索区域 -->
        <el-card class="search-card" shadow="never">
          <el-form :model="submissionQuery" :inline="true">
            <el-form-item label="模板类型">
              <el-select
                v-model="submissionQuery.templateId"
                placeholder="全部模板"
                clearable
                style="width: 200px"
              >
                <el-option
                  v-for="template in templateListAll"
                  :key="template.id"
                  :label="template.name"
                  :value="template.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select
                v-model="submissionQuery.status"
                placeholder="全部"
                clearable
                style="width: 150px"
              >
                <el-option label="待处理" :value="1" />
                <el-option label="已回复" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="fetchSubmissionList">搜索</el-button>
              <el-button :icon="Refresh" @click="handleResetSubmissionQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 数据表格 -->
        <el-card class="table-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">提交记录列表</span>
            </div>
          </template>

          <el-table
            v-loading="submissionLoading"
            :data="submissionList"
            border
            stripe
            class="data-table"
          >
            <el-table-column label="提交编号" prop="submissionCode" width="180" show-overflow-tooltip>
              <template #default="scope">
                <el-text type="primary" class="code-text">{{ scope.row.submissionCode }}</el-text>
              </template>
            </el-table-column>
            <el-table-column label="用户ID" prop="userId" width="100" align="center" />
            <el-table-column label="模板名称" prop="templateId" width="180" show-overflow-tooltip>
              <template #default="scope">
                {{ getTemplateName(scope.row.templateId) }}
              </template>
            </el-table-column>
            <el-table-column label="主持人ID" prop="hostId" width="100" align="center" />
            <el-table-column label="状态" width="120" align="center">
              <template #default="scope">
                <el-tag :type="getSubmissionStatusType(scope.row.status)">
                  {{ getSubmissionStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" prop="createTime" width="180" align="center" />
            <el-table-column label="操作" fixed="right" width="220" align="center">
              <template #default="scope">
                <el-button
                  type="primary"
                  link
                  size="small"
                  :icon="View"
                  @click="handleViewSubmission(scope.row)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-if="submissionTotal > 0"
            :total="submissionTotal"
            :page="submissionQuery.current"
            :limit="submissionQuery.size"
            @update:page="submissionQuery.current = $event"
            @update:limit="submissionQuery.size = $event"
            @pagination="fetchSubmissionList"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 模板表单对话框 -->
    <el-dialog
      v-model="templateDialog.visible"
      :title="templateDialog.title"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="templateFormRef"
        :model="templateFormData"
        :rules="templateRules"
        label-width="100px"
      >
        <el-form-item label="模板名称" prop="name">
          <el-input
            v-model="templateFormData.name"
            placeholder="请输入模板名称，如：婚礼基础信息问卷"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="模板类型" prop="type">
          <el-select
            v-model="templateFormData.type"
            placeholder="请选择模板类型"
            style="width: 100%"
          >
            <el-option
              v-for="tag in tagDict"
              :key="tag.id"
              :label="tag.name"
              :value="tag.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="问卷内容" prop="content">
          <el-input
            v-model="templateFormData.content"
            type="textarea"
            :rows="10"
            placeholder='请输入JSON格式的问卷内容'
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="templateFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTemplate" :loading="templateDialog.loading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 模板查看对话框 -->
    <el-dialog
      v-model="templateViewDialog.visible"
      title="问卷模板详情"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模板名称" :span="2">
          {{ templateViewData.name }}
        </el-descriptions-item>
        <el-descriptions-item label="模板类型">
          <el-tag type="primary">{{ getTagName(templateViewData.type) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="templateViewData.status === 1 ? 'success' : 'info'">
            {{ templateViewData.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题数量">
          {{ templateViewData.questionCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="使用次数">
          {{ templateViewData.useCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ templateViewData.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="问卷内容" :span="2">
          <div class="content-preview">
            <pre>{{ formatJsonContent(templateViewData.content) }}</pre>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 提交详情对话框 -->
    <el-dialog
      v-model="submissionDialog.visible"
      title="问卷提交详情"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="提交编号" :span="2">
          <el-text type="primary">{{ submissionDetail.submissionCode }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">
          {{ submissionDetail.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="主持人ID">
          {{ submissionDetail.hostId }}
        </el-descriptions-item>
        <el-descriptions-item label="模板名称" :span="2">
          {{ getTemplateName(submissionDetail.templateId) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="getSubmissionStatusType(submissionDetail.status)">
            {{ getSubmissionStatusText(submissionDetail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">
          {{ submissionDetail.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">
          {{ submissionDetail.updateTime }}
        </el-descriptions-item>
        <el-descriptions-item label="问卷答案" :span="2">
          <div class="answer-list">
            <div v-for="(item, index) in formatAnswersList(submissionDetail.answers)" :key="index" class="answer-item">
              <span class="answer-number">{{ index + 1 }}.</span>
              <span class="answer-question">{{ item.question }}:</span>
              <span class="answer-content">{{ item.answer }}</span>
            </div>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="submissionDialog.visible = false">关闭</el-button>
        <el-button
          type="primary"
          :icon="Download"
          :loading="exportLoading"
          @click="handleExportPdf"
        >
          导出为PDF
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Tickets,
  List,
  Search,
  Refresh,
  Plus,
  View,
  Edit,
  Delete,
  ArrowDown,
  Download
} from '@element-plus/icons-vue'
import {
  getQuestionnaireTemplatePage,
  getQuestionnaireTemplateById,
  getQuestionnaireTemplateList,
  saveQuestionnaireTemplate,
  updateQuestionnaireTemplate,
  updateQuestionnaireTemplateStatus,
  deleteQuestionnaireTemplate,
  getQuestionnaireSubmissionPage,
  getQuestionnaireSubmissionById,
  updateQuestionnaireSubmissionStatus,
  exportQuestionnaireSubmissionPdf,
  getTagList
} from '@/api/marrylink-api'
import Pagination from '@/components/Pagination/index.vue'

// 标签字典
const tagDict = ref([])
// 所有模板列表（用于下拉选择）
const templateListAll = ref([])

const activeTab = ref('template')

// 模板相关
const templateQuery = reactive({
  current: 1,
  size: 10,
  type: '',
  status: null
})

const templateList = ref([])
const templateTotal = ref(0)
const templateLoading = ref(false)

const templateDialog = reactive({
  visible: false,
  title: '新增模板',
  loading: false
})

const templateFormData = reactive({
  id: null,
  name: '',
  type: '',
  content: '',
  status: 1
})

const templateFormRef = ref()

const templateRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入问卷内容', trigger: 'blur' }]
}

// 模板查看对话框
const templateViewDialog = reactive({
  visible: false
})

const templateViewData = ref({})

// 提交记录相关
const submissionQuery = reactive({
  current: 1,
  size: 10,
  templateId: null,
  status: null
})

const submissionList = ref([])
const submissionTotal = ref(0)
const submissionLoading = ref(false)

const submissionDialog = reactive({
  visible: false
})

const submissionDetail = ref({})
const exportLoading = ref(false)

// 提交记录统计
const submissionStats = computed(() => {
  const stats = { pending: 0, replied: 0, confirmed: 0 }
  submissionList.value.forEach(item => {
    if (item.status === 1) stats.pending++
    else if (item.status === 2) stats.replied++
    else if (item.status === 3) stats.confirmed++
  })
  return stats
})

function getSubmissionStatusType(status) {
  const types = { 1: 'warning', 2: 'primary', 3: 'success' }
  return types[status] || 'info'
}

function getSubmissionStatusText(status) {
  const texts = { 1: '待填写', 2: '待查阅' ,3:'已查看'}
  return texts[status] || '未知'
}

// 根据标签ID获取标签名称
function getTagName(tagId) {
  const tag = tagDict.value.find(item => item.code === tagId)
  return tag ? tag.name : tagId
}

// 根据模板ID获取模板名称
function getTemplateName(templateId) {
  const template = templateListAll.value.find(item => item.id === templateId)
  return template ? template.name : `模板ID: ${templateId}`
}

// 格式化JSON内容
function formatJsonContent(content) {
  if (!content) return ''
  try {
    const parsed = typeof content === 'string' ? JSON.parse(content) : content
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    return content
  }
}

// 格式化答案列表为有序展示
function formatAnswersList(answers) {
  if (!answers) return []
  try {
    const parsed = typeof answers === 'string' ? JSON.parse(answers) : answers
    // 将对象转换为数组，按键排序
    return Object.keys(parsed)
      .sort((a, b) => parseInt(a) - parseInt(b))
      .map(key => {
        const item = parsed[key]
        return {
          question: item.question || '未知问题',
          answer: Array.isArray(item.answer)
            ? item.answer.join('、')
            : (item.answer || '未填写')
        }
      })
  } catch (e) {
    console.error('解析答案失败:', e)
    return []
  }
}

async function fetchTemplateList() {
  templateLoading.value = true
  try {
    const res = await getQuestionnaireTemplatePage(templateQuery)
    templateList.value = res.records
    templateTotal.value = res.total
  } catch (error) {
    ElMessage.error('获取模板列表失败')
  } finally {
    templateLoading.value = false
  }
}

async function fetchAllTemplates() {
  try {
    const res = await getQuestionnaireTemplateList()
    templateListAll.value = res
  } catch (error) {
    console.error('获取模板列表失败', error)
  }
}

function handleResetTemplateQuery() {
  templateQuery.type = ''
  templateQuery.status = null
  templateQuery.current = 1
  fetchTemplateList()
}

async function handleOpenTemplateDialog(id) {
  templateDialog.visible = true
  if (id) {
    templateDialog.title = '编辑模板'
    try {
      const res = await getQuestionnaireTemplateById(id)
      Object.assign(templateFormData, res)
    } catch (error) {
      ElMessage.error('获取模板详情失败')
      templateDialog.visible = false
    }
  } else {
    templateDialog.title = '新增模板'
    Object.assign(templateFormData, { id: null, name: '', type: '', content: '', status: 1 })
  }
}

async function handleViewTemplate(row) {
  try {
    const res = await getQuestionnaireTemplateById(row.id)
    templateViewData.value = res
    templateViewDialog.visible = true
  } catch (error) {
    ElMessage.error('获取模板详情失败')
  }
}

function handleSubmitTemplate() {
  templateFormRef.value.validate(async (valid) => {
    if (valid) {
      templateDialog.loading = true
      try {
        if (templateFormData.id) {
          await updateQuestionnaireTemplate(templateFormData)
          ElMessage.success('修改成功')
        } else {
          await saveQuestionnaireTemplate(templateFormData)
          ElMessage.success('新增成功')
        }
        templateDialog.visible = false
        fetchTemplateList()
        fetchAllTemplates()
      } catch (error) {
        ElMessage.error(templateFormData.id ? '修改失败' : '新增失败')
      } finally {
        templateDialog.loading = false
      }
    }
  })
}

async function handleToggleTemplateStatus(row) {
  try {
    await updateQuestionnaireTemplateStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    fetchTemplateList()
  } catch (error) {
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

function handleDeleteTemplate(id) {
  ElMessageBox.confirm('确认删除该模板？删除后将无法恢复。', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteQuestionnaireTemplate(id)
      ElMessage.success('删除成功')
      fetchTemplateList()
      fetchAllTemplates()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

async function fetchSubmissionList() {
  submissionLoading.value = true
  try {
    const res = await getQuestionnaireSubmissionPage(submissionQuery)
    submissionList.value = res.records
    submissionTotal.value = res.total
  } catch (error) {
    ElMessage.error('获取提交记录失败')
  } finally {
    submissionLoading.value = false
  }
}

function handleResetSubmissionQuery() {
  submissionQuery.templateId = null
  submissionQuery.status = null
  submissionQuery.current = 1
  fetchSubmissionList()
}

async function handleViewSubmission(row) {
  try {
    const res = await getQuestionnaireSubmissionById(row.id)
    submissionDetail.value = res
    submissionDialog.visible = true
  } catch (error) {
    ElMessage.error('获取提交详情失败')
  }
}

async function handleUpdateSubmissionStatus(id, status) {
  try {
    await updateQuestionnaireSubmissionStatus(id, status)
    ElMessage.success('状态更新成功')
    fetchSubmissionList()
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

async function handleExportPdf() {
  if (!submissionDetail.value.id) {
    ElMessage.warning('无法获取问卷信息')
    return
  }
  
  exportLoading.value = true
  try {
    const response = await exportQuestionnaireSubmissionPdf(submissionDetail.value.id)
    
    // response是axios响应对象，需要获取data
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 生成文件名：问卷提交编号_日期.pdf
    const fileName = `问卷_${submissionDetail.value.submissionCode}_${new Date().getTime()}.pdf`
    link.setAttribute('download', fileName)
    
    // 触发下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('PDF导出成功')
  } catch (error) {
    console.error('导出PDF失败:', error)
    ElMessage.error('PDF导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

onMounted(async () => {
  // 获取标签字典（category_code=02）
  try {
    const res = await getTagList('02')
    tagDict.value = res
  } catch (error) {
    console.error('获取标签字典失败', error)
  }
  
  // 获取所有模板列表
  await fetchAllTemplates()
  
  // 加载数据
  fetchTemplateList()
  fetchSubmissionList()
})
</script>

<style scoped lang="scss">
.questionnaire-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.page-header {
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.15);

  .page-title {
    display: flex;
    align-items: center;
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;

    .title-icon {
      margin-right: 12px;
      font-size: 28px;
    }
  }

  .page-description {
    margin: 0;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.9);
  }
}

.questionnaire-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
    background-color: #ffffff;
    padding: 16px 20px 0;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
    font-weight: 500;
    color: #606266;
    padding: 0 24px;
    height: 44px;
    line-height: 44px;

    &.is-active {
      color: #1d4ed8;
    }

    &:hover {
      color: #1d4ed8;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: #1d4ed8;
    height: 3px;
  }

  .tab-label {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;

  :deep(.el-card__body) {
    padding: 20px;
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.table-card {
  border-radius: 8px;
  border: 1px solid #e4e7ed;

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #e4e7ed;
    background-color: #fafafa;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  :deep(.el-card__body) {
    padding: 0;
  }
}

.data-table {
  :deep(.el-table__header) {
    th {
      background-color: #f5f7fa;
      color: #606266;
      font-weight: 600;
    }
  }

  :deep(.el-table__body) {
    tr:hover > td {
      background-color: #f5f9ff !important;
    }
  }

  .template-name {
    display: flex;
    align-items: center;
    gap: 8px;

    .name-icon {
      color: #1d4ed8;
      font-size: 16px;
    }
  }

  .count-text {
    font-weight: 600;
  }

  .code-text {
    font-family: 'Courier New', monospace;
    font-weight: 500;
  }
}

.content-preview,
.answer-preview {
  max-height: 400px;
  overflow-y: auto;
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 12px;

  pre {
    margin: 0;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.6;
    color: #303133;
    white-space: pre-wrap;
    word-wrap: break-word;
  }
}

.answer-list {
  max-height: 400px;
  overflow-y: auto;
  
  .answer-item {
    display: flex;
    align-items: flex-start;
    padding: 12px;
    margin-bottom: 8px;
    background-color: #f5f7fa;
    border-radius: 6px;
    border-left: 3px solid #1d4ed8;
    transition: all 0.3s ease;

    &:hover {
      background-color: #e8f0fe;
      transform: translateX(4px);
    }

    &:last-child {
      margin-bottom: 0;
    }

    .answer-number {
      flex-shrink: 0;
      width: 32px;
      font-weight: 600;
      color: #1d4ed8;
      font-size: 14px;
    }

    .answer-question {
      flex-shrink: 0;
      margin-right: 8px;
      font-weight: 600;
      color: #303133;
      font-size: 14px;
    }

    .answer-content {
      flex: 1;
      color: #606266;
      font-size: 14px;
      line-height: 1.6;
      word-break: break-word;
    }
  }
}

:deep(.pagination-container) {
  padding: 16px 20px;
  background-color: #ffffff;
}

@media (max-width: 768px) {
  .questionnaire-container {
    padding: 12px;
  }

  .page-header {
    padding: 16px;

    .page-title {
      font-size: 20px;

      .title-icon {
        font-size: 24px;
      }
    }
  }

  .search-card {
    :deep(.el-form) {
      .el-form-item {
        display: block;
        margin-bottom: 12px;

        .el-select,
        .el-input {
          width: 100% !important;
        }
      }
    }
  }
}
</style>