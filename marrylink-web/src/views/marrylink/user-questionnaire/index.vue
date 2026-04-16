<template>
  <div class="user-questionnaire-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">
        <el-icon class="title-icon"><Document /></el-icon>
        我的问卷
      </h2>
      <p class="page-description">查看和填写您的婚礼问卷</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card pending-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pending }}</div>
              <div class="stat-label">待填写</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card processing-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Edit /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.replied }}</div>
              <div class="stat-label">已提交</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card completed-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.confirmed }}</div>
              <div class="stat-label">已确认</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 问卷列表 -->
    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">问卷列表</span>
          <el-radio-group v-model="queryStatus" @change="handleStatusChange">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="1">待填写</el-radio-button>
            <el-radio-button :label="2">已提交</el-radio-button>
            <el-radio-button :label="3">已确认</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-empty v-if="questionnaireList.length === 0 && !loading" description="暂无问卷" />

      <div v-else v-loading="loading" class="questionnaire-list">
        <div
          v-for="item in questionnaireList"
          :key="item.id"
          class="questionnaire-item"
          @click="handleViewQuestionnaire(item)"
        >
          <div class="item-header">
            <div class="item-title">
              <el-icon class="title-icon"><Document /></el-icon>
              <span>{{ getTemplateName(item.templateId) }}</span>
            </div>
            <el-tag :type="getStatusType(item.status)" size="large">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
          
          <div class="item-content">
            <div class="item-info">
              <div class="info-item">
                <el-icon><Calendar /></el-icon>
                <span>提交时间：{{ item.createTime || '未提交' }}</span>
              </div>
              <div class="info-item">
                <el-icon><User /></el-icon>
                <span>主持人ID：{{ item.hostId || '未分配' }}</span>
              </div>
              <div class="info-item">
                <el-icon><Tickets /></el-icon>
                <span>编号：{{ item.submissionCode || '待生成' }}</span>
              </div>
            </div>
          </div>

          <div class="item-footer">
            <el-button
              v-if="item.status === 1 || !item.id"
              type="primary"
              :icon="Edit"
              @click.stop="handleFillQuestionnaire(item)"
            >
              {{ item.id ? '继续填写' : '开始填写' }}
            </el-button>
            <el-button
              v-else
              type="primary"
              plain
              :icon="View"
              @click.stop="handleViewDetail(item)"
            >
              查看详情
            </el-button>
          </div>
        </div>
      </div>

      <pagination
        v-if="total > 0"
        :total="total"
        :page="queryParams.current"
        :limit="queryParams.size"
        @update:page="queryParams.current = $event"
        @update:limit="queryParams.size = $event"
        @pagination="fetchQuestionnaireList"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialog.visible"
      title="问卷详情"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="提交编号" :span="2">
          <el-text type="primary">{{ detailData.submissionCode }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="问卷模板">
          {{ getTemplateName(detailData.templateId) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">
            {{ getStatusText(detailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="主持人ID">
          {{ detailData.hostId || '未分配' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ detailData.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">
          {{ detailData.updateTime }}
        </el-descriptions-item>
        <el-descriptions-item label="问卷答案" :span="2">
          <div class="answer-preview">
            <pre>{{ formatJsonContent(detailData.answers) }}</pre>
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
        <el-button
          v-if="detailData.status === 1"
          type="primary"
          @click="handleEditFromDetail"
        >
          继续编辑
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  Clock,
  Edit,
  CircleCheck,
  Calendar,
  User,
  Tickets,
  View
} from '@element-plus/icons-vue'
import {
  getUserQuestionnaireSubmissions,
  getQuestionnaireSubmissionById,
  getQuestionnaireTemplateList
} from '@/api/marrylink-api'
import Pagination from '@/components/Pagination/index.vue'

const router = useRouter()

// 模拟用户ID，实际应从登录状态获取
const currentUserId = ref()

// 所有模板列表
const templateList = ref([])

// 查询参数
const queryParams = reactive({
  current: 1,
  size: 10
})

const queryStatus = ref(null)

const questionnaireList = ref([])
const total = ref(0)
const loading = ref(false)

// 统计数据
const stats = computed(() => {
  const result = { pending: 0, replied: 0, confirmed: 0 }
  questionnaireList.value.forEach(item => {
    if (item.status === 1) result.pending++
    else if (item.status === 2) result.replied++
    else if (item.status === 3) result.confirmed++
  })
  return result
})

// 详情对话框
const detailDialog = reactive({
  visible: false
})

const detailData = ref({})

// 获取模板名称
function getTemplateName(templateId) {
  const template = templateList.value.find(item => item.id === templateId)
  return template ? template.name : `模板ID: ${templateId}`
}

// 获取状态类型
function getStatusType(status) {
  const types = { 1: 'warning', 2: 'primary', 3: 'success' }
  return types[status] || 'info'
}

// 获取状态文本
function getStatusText(status) {
  const texts = { 1: '待填写', 2: '已提交', 3: '已确认' }
  return texts[status] || '未知'
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

// 获取问卷列表
async function fetchQuestionnaireList() {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      status: queryStatus.value
    }
    const res = await getUserQuestionnaireSubmissions(currentUserId.value, params)
    questionnaireList.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    ElMessage.error('获取问卷列表失败')
  } finally {
    loading.value = false
  }
}

// 获取所有模板
async function fetchTemplateList() {
  try {
    const res = await getQuestionnaireTemplateList()
    templateList.value = res
  } catch (error) {
    console.error('获取模板列表失败', error)
  }
}

// 状态筛选变化
function handleStatusChange() {
  queryParams.current = 1
  fetchQuestionnaireList()
}

// 查看问卷
function handleViewQuestionnaire(item) {
  if (item.status === 1 || !item.id) {
    handleFillQuestionnaire(item)
  } else {
    handleViewDetail(item)
  }
}

// 填写问卷
function handleFillQuestionnaire(item) {
  router.push({
    name: 'UserQuestionnaireFill',
    query: {
      templateId: item.templateId,
      submissionId: item.id || ''
    }
  })
}

// 查看详情
async function handleViewDetail(item) {
  try {
    const res = await getQuestionnaireSubmissionById(item.id)
    detailData.value = res
    detailDialog.visible = true
  } catch (error) {
    ElMessage.error('获取问卷详情失败')
  }
}

// 从详情编辑
function handleEditFromDetail() {
  detailDialog.visible = false
  handleFillQuestionnaire(detailData.value)
}

onMounted(async () => {
  await fetchTemplateList()
  fetchQuestionnaireList()
})
</script>

<style scoped lang="scss">
.user-questionnaire-container {
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

.stats-row {
  margin-bottom: 20px;

  .stat-card {
    border-radius: 12px;
    border: none;
    transition: all 0.3s;
    cursor: pointer;

    &:hover {
      transform: translateY(-4px);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 56px;
        height: 56px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
      }

      .stat-info {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: 700;
          line-height: 1.2;
          margin-bottom: 4px;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
        }
      }
    }

    &.pending-card {
      .stat-icon {
        background-color: #fef0f0;
        color: #f56c6c;
      }
      .stat-value {
        color: #f56c6c;
      }
    }

    &.processing-card {
      .stat-icon {
        background-color: #ecf5ff;
        color: #1d4ed8;
      }
      .stat-value {
        color: #1d4ed8;
      }
    }

    &.completed-card {
      .stat-icon {
        background-color: #f0f9ff;
        color: #67c23a;
      }
      .stat-value {
        color: #67c23a;
      }
    }
  }
}

.list-card {
  border-radius: 12px;
  border: 1px solid #e4e7ed;

  :deep(.el-card__header) {
    padding: 20px;
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
    padding: 20px;
  }
}

.questionnaire-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;

  .questionnaire-item {
    background: #ffffff;
    border: 1px solid #e4e7ed;
    border-radius: 12px;
    padding: 20px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #1d4ed8;
      box-shadow: 0 4px 12px rgba(29, 78, 216, 0.15);
      transform: translateY(-2px);
    }

    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
      padding-bottom: 16px;
      border-bottom: 1px solid #f0f0f0;

      .item-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        flex: 1;

        .title-icon {
          color: #1d4ed8;
          font-size: 20px;
        }
      }
    }

    .item-content {
      margin-bottom: 16px;

      .item-info {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .info-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
          color: #606266;

          .el-icon {
            color: #909399;
            font-size: 14px;
          }
        }
      }
    }

    .item-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  }
}

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

:deep(.pagination-container) {
  padding: 16px 0 0;
  background-color: transparent;
}

@media (max-width: 768px) {
  .user-questionnaire-container {
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

  .stats-row {
    .el-col {
      margin-bottom: 12px;
    }
  }

  .questionnaire-list {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start !important;
  }
}
</style>