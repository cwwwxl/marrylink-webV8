<template>
  <div class="questionnaire-fill-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <el-button :icon="ArrowLeft" circle @click="handleBack" class="back-btn" />
        <div class="header-info">
          <h2 class="page-title">
            <el-icon class="title-icon"><Edit /></el-icon>
            {{ templateData.name || '问卷填写' }}
          </h2>
          <p class="page-description">请认真填写以下信息，我们将为您提供更好的服务</p>
        </div>
      </div>
      <div class="progress-info">
        <el-progress
          :percentage="progressPercentage"
          :color="progressColor"
          :stroke-width="8"
        />
        <span class="progress-text">已完成 {{ answeredCount }}/{{ totalQuestions }} 题</span>
      </div>
    </div>

    <!-- 问卷表单 -->
    <el-card v-loading="loading" class="form-card" shadow="never">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        class="questionnaire-form"
      >
        <div
          v-for="(question, index) in questions"
          :key="question.id"
          class="question-item"
          :class="{ 'answered': isQuestionAnswered(question) }"
        >
          <div class="question-header">
            <span class="question-number">{{ index + 1 }}</span>
            <div class="question-title">
              {{ question.question }}
              <el-tag v-if="question.required" type="danger" size="small" effect="plain">必填</el-tag>
            </div>
          </div>

          <!-- 文本输入 -->
          <el-form-item
            v-if="question.type === 'text'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-input
              v-model="formData[`answer_${question.id}`]"
              :placeholder="question.placeholder || '请输入答案'"
              clearable
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <!-- 多行文本 -->
          <el-form-item
            v-else-if="question.type === 'textarea'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-input
              v-model="formData[`answer_${question.id}`]"
              type="textarea"
              :rows="4"
              :placeholder="question.placeholder || '请输入详细信息'"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <!-- 单选 -->
          <el-form-item
            v-else-if="question.type === 'radio'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-radio-group v-model="formData[`answer_${question.id}`]">
              <el-radio
                v-for="option in question.options"
                :key="option"
                :label="option"
                class="radio-option"
              >
                {{ option }}
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 多选 -->
          <el-form-item
            v-else-if="question.type === 'checkbox'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-checkbox-group v-model="formData[`answer_${question.id}`]">
              <el-checkbox
                v-for="option in question.options"
                :key="option"
                :label="option"
                class="checkbox-option"
              >
                {{ option }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <!-- 日期选择 -->
          <el-form-item
            v-else-if="question.type === 'date'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-date-picker
              v-model="formData[`answer_${question.id}`]"
              type="date"
              placeholder="请选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>

          <!-- 时间选择 -->
          <el-form-item
            v-else-if="question.type === 'time'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-time-picker
              v-model="formData[`answer_${question.id}`]"
              placeholder="请选择时间"
              format="HH:mm"
              value-format="HH:mm"
              style="width: 100%"
            />
          </el-form-item>

          <!-- 数字输入 -->
          <el-form-item
            v-else-if="question.type === 'number'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-input-number
              v-model="formData[`answer_${question.id}`]"
              :min="question.min || 0"
              :max="question.max || 999999"
              :placeholder="question.placeholder || '请输入数字'"
              style="width: 100%"
            />
          </el-form-item>

          <!-- 评分 -->
          <el-form-item
            v-else-if="question.type === 'rate'"
            :prop="`answer_${question.id}`"
            class="question-content"
          >
            <el-rate
              v-model="formData[`answer_${question.id}`]"
              :max="question.max || 5"
              show-text
              :texts="question.texts || ['极差', '失望', '一般', '满意', '惊喜']"
            />
          </el-form-item>
        </div>
      </el-form>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button size="large" @click="handleBack">
          取消
        </el-button>
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          @click="handleSubmit"
        >
          <el-icon><Check /></el-icon>
          提交问卷
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Edit,
  Check
} from '@element-plus/icons-vue'
import {
  getQuestionnaireTemplateById,
  getQuestionnaireSubmissionById,
  submitQuestionnaire,
  updateQuestionnaireSubmission
} from '@/api/marrylink-api'

const router = useRouter()
const route = useRoute()

// 模拟用户ID和主持人ID，实际应从登录状态获取
const currentUserId = ref(1)
const currentHostId = ref(1)

const loading = ref(false)
const submitting = ref(false)

// 模板数据
const templateData = ref({})
const questions = ref([])

// 提交记录ID（编辑模式）
const submissionId = ref(null)

// 表单数据
const formData = reactive({})
const formRef = ref()
const formRules = reactive({})

// 进度计算
const totalQuestions = computed(() => questions.value.length)
const answeredCount = computed(() => {
  let count = 0
  questions.value.forEach(q => {
    if (isQuestionAnswered(q)) count++
  })
  return count
})

const progressPercentage = computed(() => {
  if (totalQuestions.value === 0) return 0
  return Math.round((answeredCount.value / totalQuestions.value) * 100)
})

const progressColor = computed(() => {
  const percentage = progressPercentage.value
  if (percentage < 30) return '#f56c6c'
  if (percentage < 70) return '#e6a23c'
  return '#67c23a'
})

// 判断问题是否已回答
function isQuestionAnswered(question) {
  const answer = formData[`answer_${question.id}`]
  if (answer === null || answer === undefined || answer === '') return false
  if (Array.isArray(answer) && answer.length === 0) return false
  return true
}

// 加载模板数据
async function loadTemplate() {
  const templateId = route.query.templateId
  if (!templateId) {
    ElMessage.error('缺少模板ID')
    handleBack()
    return
  }

  loading.value = true
  try {
    const res = await getQuestionnaireTemplateById(templateId)
    console.log(res)
    templateData.value = res
    
    // 解析问卷内容
    if (res.content) {
      const content = typeof res.content === 'string' ? JSON.parse(res.content) : res.content
      questions.value = content
      
      // 初始化表单规则
      questions.value.forEach(q => {
        if (q.required) {
          formRules[`answer_${q.id}`] = [
            { required: true, message: '此题为必填项', trigger: 'change' }
          ]
        }
        
        // 初始化表单数据
        if (q.type === 'checkbox') {
          formData[`answer_${q.id}`] = []
        } else if (q.type === 'rate') {
          formData[`answer_${q.id}`] = 0
        } else {
          formData[`answer_${q.id}`] = ''
        }
      })
    }
  } catch (error) {
    ElMessage.error('加载问卷模板失败')
    handleBack()
  } finally {
    loading.value = false
  }
}

// 加载已有答案（编辑模式）
async function loadSubmission() {
  const id = route.query.submissionId
  if (!id) return

  submissionId.value = id
  try {
    const res = await getQuestionnaireSubmissionById(id)
    
    // 填充已有答案
    if (res.answers) {
      const answers = typeof res.answers === 'string' ? JSON.parse(res.answers) : res.answers
      Object.keys(answers).forEach(key => {
        formData[key] = answers[key]
      })
    }
  } catch (error) {
    console.error('加载已有答案失败', error)
  }
}

// 返回
function handleBack() {
  router.back()
}

// 提交问卷
async function handleSubmit() {
  // 验证表单
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请完成所有必填项')
    return
  }

  // 检查是否所有题目都已回答
  const unansweredCount = totalQuestions.value - answeredCount.value
  if (unansweredCount > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unansweredCount} 道题未填写，确定要提交吗？`,
        '提示',
        {
          confirmButtonText: '确定提交',
          cancelButtonText: '继续填写',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    const answers = {}
    questions.value.forEach(q => {
      answers[`answer_${q.id}`] = formData[`answer_${q.id}`]
    })

    const data = {
      userId: currentUserId.value,
      templateId: templateData.value.id,
      hostId: currentHostId.value,
      answers: JSON.stringify(answers),
      status: 2 // 已提交状态
    }

    if (submissionId.value) {
      // 更新模式
      data.id = submissionId.value
      await updateQuestionnaireSubmission(data)
      ElMessage.success('问卷更新成功')
    } else {
      // 新增模式
      await submitQuestionnaire(data)
      ElMessage.success('问卷提交成功')
    }

    // 延迟返回，让用户看到成功提示
    setTimeout(() => {
      handleBack()
    }, 1000)
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadTemplate()
  await loadSubmission()
})
</script>

<style scoped lang="scss">
.questionnaire-fill-container {
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

  .header-content {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-btn {
      background-color: rgba(255, 255, 255, 0.2);
      border: none;
      color: #ffffff;
      
      &:hover {
        background-color: rgba(255, 255, 255, 0.3);
      }
    }

    .header-info {
      flex: 1;

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
  }

  .progress-info {
    .progress-text {
      display: block;
      margin-top: 8px;
      font-size: 13px;
      color: rgba(255, 255, 255, 0.9);
      text-align: right;
    }
  }
}

.form-card {
  border-radius: 12px;
  border: 1px solid #e4e7ed;

  :deep(.el-card__body) {
    padding: 32px;
  }
}

.questionnaire-form {
  .question-item {
    margin-bottom: 32px;
    padding: 24px;
    background-color: #fafafa;
    border-radius: 12px;
    border: 2px solid transparent;
    transition: all 0.3s;

    &.answered {
      background-color: #f0f9ff;
      border-color: #d1e7ff;
    }

    &:hover {
      background-color: #f5f9ff;
    }

    .question-header {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 16px;

      .question-number {
        flex-shrink: 0;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%);
        color: #ffffff;
        border-radius: 50%;
        font-weight: 600;
        font-size: 14px;
      }

      .question-title {
        flex: 1;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        line-height: 32px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .question-content {
      margin-left: 44px;
      margin-bottom: 0;

      .radio-option,
      .checkbox-option {
        display: block;
        margin-bottom: 12px;
        padding: 12px 16px;
        background-color: #ffffff;
        border-radius: 8px;
        border: 1px solid #e4e7ed;
        transition: all 0.3s;

        &:hover {
          border-color: #1d4ed8;
          background-color: #f5f9ff;
        }

        :deep(.el-radio__label),
        :deep(.el-checkbox__label) {
          font-size: 14px;
          color: #606266;
        }
      }
    }
  }
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
  padding-top: 32px;
  border-top: 2px solid #e4e7ed;

  .el-button {
    min-width: 140px;
  }
}

@media (max-width: 768px) {
  .questionnaire-fill-container {
    padding: 12px;
  }

  .page-header {
    padding: 16px;

    .header-content {
      .page-title {
        font-size: 18px;

        .title-icon {
          font-size: 22px;
        }
      }
    }
  }

  .form-card {
    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  .questionnaire-form {
    .question-item {
      padding: 16px;

      .question-content {
        margin-left: 0;
      }
    }
  }

  .form-actions {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>