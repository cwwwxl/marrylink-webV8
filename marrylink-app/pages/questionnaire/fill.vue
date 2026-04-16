<template>
  <view class="fill-container">
    <scroll-view class="fill-scroll" scroll-y>
      <!-- 只读模式提示 -->
      <view class="readonly-tip" v-if="isReadOnly">
        <text class="tip-icon">👁️</text>
        <text class="tip-text">此问卷为只读模式，无法编辑</text>
      </view>
      
      <!-- 问卷标题 -->
      <view class="questionnaire-header">
        <text class="questionnaire-title">{{ questionnaireInfo.title }}</text>
        <text class="questionnaire-desc">{{ questionnaireInfo.description }}</text>
      </view>
      
      <!-- 问题列表 -->
      <view class="question-list">
        <view 
          class="question-item" 
          v-for="(question, index) in questions" 
          :key="question.id"
        >
          <view class="question-header">
            <text class="question-number">{{ index + 1 }}</text>
            <text class="question-title">{{ question.question || question.title }}</text>
            <text class="question-required" v-if="question.required">*</text>
          </view>
          
          <!-- 单选题 (radio) -->
          <radio-group
            v-if="question.type === 'radio' || question.type === 'SINGLE'"
            @change="handleRadioChange($event, question.id)"
          >
            <label
              class="option-item"
              v-for="(option, idx) in question.options"
              :key="idx"
            >
              <radio
                :value="typeof option === 'string' ? option : option.value"
                :checked="answers[question.id] === (typeof option === 'string' ? option : option.value)"
                :disabled="isReadOnly"
                color="#1d4ed8"
              />
              <text class="option-text">{{ typeof option === 'string' ? option : option.label }}</text>
            </label>
          </radio-group>
          
          <!-- 多选题 (checkbox) -->
          <checkbox-group
            v-if="question.type === 'checkbox' || question.type === 'MULTIPLE'"
            @change="handleCheckboxChange($event, question.id)"
          >
            <label
              class="option-item"
              v-for="(option, idx) in question.options"
              :key="idx"
            >
              <checkbox
                :value="typeof option === 'string' ? option : option.value"
                :checked="(answers[question.id] || []).includes(typeof option === 'string' ? option : option.value)"
                :disabled="isReadOnly"
                color="#1d4ed8"
              />
              <text class="option-text">{{ typeof option === 'string' ? option : option.label }}</text>
            </label>
          </checkbox-group>
          
          <!-- 文本题 (text) -->
          <input
            v-if="question.type === 'text'"
            class="text-input"
            v-model="answers[question.id]"
            :placeholder="question.placeholder || '请输入您的答案'"
            :disabled="isReadOnly"
          />
          
          <!-- 多行文本题 (textarea) -->
          <textarea
            v-if="question.type === 'textarea' || question.type === 'TEXT'"
            class="textarea-input"
            v-model="answers[question.id]"
            :placeholder="question.placeholder || '请输入您的答案'"
            :maxlength="question.maxLength || 500"
            :disabled="isReadOnly"
          ></textarea>
          
          <!-- 数字题 (number) -->
          <input
            v-if="question.type === 'number'"
            class="text-input"
            type="number"
            v-model="answers[question.id]"
            :placeholder="question.placeholder || '请输入数字'"
            :min="question.min"
            :max="question.max"
            :disabled="isReadOnly"
          />
          
          <!-- 日期题 (date) -->
          <picker
            v-if="question.type === 'date' || question.type === 'DATE'"
            mode="date"
            :value="answers[question.id]"
            @change="handleDateChange($event, question.id)"
            :disabled="isReadOnly"
          >
            <view class="date-picker" :class="{ 'disabled': isReadOnly }">
              <text v-if="answers[question.id]">{{ answers[question.id] }}</text>
              <text v-else class="placeholder">请选择日期</text>
            </view>
          </picker>
          
          <!-- 时间题 (time) -->
          <picker
            v-if="question.type === 'time' || question.type === 'TIME'"
            mode="time"
            :value="answers[question.id]"
            @change="handleTimeChange($event, question.id)"
            :disabled="isReadOnly"
          >
            <view class="time-picker" :class="{ 'disabled': isReadOnly }">
              <text v-if="answers[question.id]">{{ answers[question.id] }}</text>
              <text v-else class="placeholder">请选择时间</text>
            </view>
          </picker>
        </view>
      </view>
    </scroll-view>
    
    <!-- 底部提交按钮 -->
    <view class="bottom-bar" v-if="!isReadOnly">
      <button class="btn-submit" @click="handleSubmit" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交问卷' }}
      </button>
    </view>
  </view>
</template>

<script>
import { getQuestionnaireDetail, submitQuestionnaire } from '@/api/questionnaire'

export default {
  data() {
    return {
      questionnaireId: '',
      hostId: '',
      questionnaireItem: null, // 存储从上一页传递过来的完整item对象
      questionnaireInfo: {},
      questions: [],
      answers: {},
      submitting: false
    }
  },
  
  computed: {
    // 判断是否为只读模式：status不为1时只能查看
    isReadOnly() {
      return this.questionnaireItem && this.questionnaireItem.status !== 1
    }
  },
  
  onLoad(options) {
    // 从缓存中读取完整的item对象
    const cachedItem = uni.getStorageSync('currentQuestionnaire')
    if (cachedItem) {
      this.questionnaireItem = cachedItem
      console.log('从缓存获取到的问卷对象:', cachedItem)
      // 清除缓存，避免影响下次使用
      uni.removeStorageSync('currentQuestionnaire')
    }
    
    if (options.id) {
      this.questionnaireId = options.id
    }
    if (options.hostId) {
      this.hostId = options.hostId
    }
    
    // 如果有缓存的item对象，可以从中提取hostId等信息
    if (this.questionnaireItem) {
      if (this.questionnaireItem.hostId) {
        this.hostId = this.questionnaireItem.hostId
      }
      // 可以根据需要使用item中的其他字段
      console.log('问卷名称:', this.questionnaireItem.submissionName || this.questionnaireItem.questionnaireName)
      console.log('问卷状态:', this.questionnaireItem.status)
    }
    
    this.loadQuestionnaireDetail()
  },
  
  methods: {
    // 加载问卷详情
    async loadQuestionnaireDetail() {
      try {
        const res = await getQuestionnaireDetail(this.questionnaireId)
        console.log('问卷详情响应:', res)
        
        const data = res.data || {}
        this.questionnaireInfo = {
          title: data.name || '问卷',
          description: data.description || '请认真填写以下问题'
        }
        
        // 解析content字段（JSON字符串）
        if (data.content) {
          try {
            this.questions = typeof data.content === 'string'
              ? JSON.parse(data.content)
              : data.content
          } catch (e) {
            console.error('解析问卷内容失败:', e)
            this.questions = []
          }
        } else {
          this.questions = []
        }
        
        console.log('解析后的题目列表:', this.questions)
        
        // 初始化答案对象
        this.questions.forEach(question => {
          if (question.type === 'checkbox' || question.type === 'MULTIPLE') {
            this.$set(this.answers, question.id, [])
          } else {
            this.$set(this.answers, question.id, '')
          }
        })
        
        // 如果是只读模式，加载之前的填写内容
        if (this.isReadOnly && this.questionnaireItem && this.questionnaireItem.answers) {
          this.loadPreviousAnswers()
        }
      } catch (error) {
        console.error('加载问卷详情失败:', error)
        uni.showToast({
          title: '加载问卷失败',
          icon: 'none'
        })
      }
    },
    
    // 加载之前的填写内容（只读模式）
    loadPreviousAnswers() {
      try {
        let previousAnswers = this.questionnaireItem.answers
        
        // 如果answers是字符串，解析为JSON对象
        if (typeof previousAnswers === 'string') {
          previousAnswers = JSON.parse(previousAnswers)
        }
        
        console.log('加载之前的答案:', previousAnswers)
        
        // 将之前的答案填充到answers对象中
        if (previousAnswers && typeof previousAnswers === 'object') {
          Object.keys(previousAnswers).forEach(questionId => {
            const answerData = previousAnswers[questionId]
            
            // 兼容新旧格式：如果是对象且包含answer字段，取answer值；否则直接使用原值
            if (answerData && typeof answerData === 'object' && 'answer' in answerData) {
              this.$set(this.answers, questionId, answerData.answer)
            } else {
              this.$set(this.answers, questionId, answerData)
            }
          })
        }
      } catch (error) {
        console.error('解析之前的答案失败:', error)
        uni.showToast({
          title: '加载答案失败',
          icon: 'none'
        })
      }
    },
    
    // 单选变更
    handleRadioChange(e, questionId) {
      this.$set(this.answers, questionId, e.detail.value)
    },
    
    // 多选变更
    handleCheckboxChange(e, questionId) {
      this.$set(this.answers, questionId, e.detail.value)
    },
    
    // 日期变更
    handleDateChange(e, questionId) {
      this.$set(this.answers, questionId, e.detail.value)
    },
    
    // 时间变更
    handleTimeChange(e, questionId) {
      this.$set(this.answers, questionId, e.detail.value)
    },
    
    // 验证表单
    validateForm() {
      for (let question of this.questions) {
        if (question.required) {
          const answer = this.answers[question.id]
          
          if (!answer || (Array.isArray(answer) && answer.length === 0)) {
            uni.showToast({
              title: `请回答第${this.questions.indexOf(question) + 1}题`,
              icon: 'none'
            })
            return false
          }
        }
      }
      return true
    },
    
    // 提交问卷
    async handleSubmit() {
      // 只读模式下不允许提交
      if (this.isReadOnly) {
        uni.showToast({
          title: '此问卷为只读模式，无法提交',
          icon: 'none'
        })
        return
      }
      
      if (!this.validateForm()) {
        return
      }
      
      this.submitting = true
      console.log(this.questionnaireItem)
      
      try {
        // 构建包含问题文本的答案对象
        const answersWithQuestions = {}
        this.questions.forEach(question => {
          const questionId = question.id
          const answer = this.answers[questionId]
          
          answersWithQuestions[questionId] = {
            question: question.question || question.title,
            answer: answer
          }
        })
        
        this.questionnaireItem.answers = JSON.stringify(answersWithQuestions)
        
        await submitQuestionnaire(this.questionnaireItem)
        
        uni.showToast({
          title: '提交成功',
          icon: 'success'
        })
        
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (error) {
        console.error('提交问卷失败:', error)
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.fill-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 120rpx;
}

.readonly-tip {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
  
  .tip-icon {
    font-size: 32rpx;
    margin-right: 12rpx;
  }
  
  .tip-text {
    flex: 1;
    font-size: 26rpx;
    color: #ffffff;
  }
}

.fill-scroll {
  height: calc(100vh - 120rpx);
}

.questionnaire-header {
  background-color: #ffffff;
  padding: 40rpx 32rpx;
  margin-bottom: 20rpx;
  
  .questionnaire-title {
    display: block;
    font-size: 36rpx;
    font-weight: bold;
    color: #333333;
    margin-bottom: 16rpx;
  }
  
  .questionnaire-desc {
    font-size: 26rpx;
    color: #666666;
    line-height: 1.6;
  }
}

.question-list {
  padding: 0 32rpx;
  
  .question-item {
    background-color: #ffffff;
    border-radius: 16rpx;
    padding: 32rpx;
    margin-bottom: 20rpx;
    
    .question-header {
      display: flex;
      align-items: flex-start;
      margin-bottom: 24rpx;
      
      .question-number {
        display: inline-block;
        width: 48rpx;
        height: 48rpx;
        line-height: 48rpx;
        text-align: center;
        background-color: #1d4ed8;
        color: #ffffff;
        border-radius: 50%;
        font-size: 24rpx;
        font-weight: bold;
        margin-right: 16rpx;
        flex-shrink: 0;
      }
      
      .question-title {
        flex: 1;
        font-size: 28rpx;
        color: #333333;
        line-height: 1.6;
      }
      
      .question-required {
        color: #ef4444;
        font-size: 32rpx;
        margin-left: 8rpx;
      }
    }
    
    .option-item {
      display: flex;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #f5f7fa;
      
      &:last-child {
        border-bottom: none;
      }
      
      radio, checkbox {
        margin-right: 16rpx;
      }
      
      .option-text {
        flex: 1;
        font-size: 28rpx;
        color: #333333;
      }
    }
    
    .text-input {
      width: 100%;
      padding: 20rpx;
      background-color: #f5f7fa;
      border-radius: 12rpx;
      font-size: 28rpx;
      color: #333333;
      line-height: 1.6;
      
      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
    
    .textarea-input {
      width: 100%;
      min-height: 200rpx;
      padding: 20rpx;
      background-color: #f5f7fa;
      border-radius: 12rpx;
      font-size: 28rpx;
      color: #333333;
      line-height: 1.6;
      
      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
    
    .date-picker,
    .time-picker {
      padding: 24rpx;
      background-color: #f5f7fa;
      border-radius: 12rpx;
      font-size: 28rpx;
      color: #333333;
      
      &.disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
      
      .placeholder {
        color: #999999;
      }
    }
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 32rpx;
  background-color: #ffffff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  
  .btn-submit {
    width: 100%;
    height: 80rpx;
    background-color: #1d4ed8;
    color: #ffffff;
    border: none;
    border-radius: 40rpx;
    font-size: 32rpx;
    font-weight: bold;
    line-height: 80rpx;
    
    &:disabled {
      opacity: 0.6;
    }
  }
}
</style>