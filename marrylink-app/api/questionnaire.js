import { get, post } from '@/utils/request'

/**
 * 获取问卷列表
 */
export function getQuestionnaireList(params) {
  return get('/questionnaire-submission/page', params)
}

/**
 * 获取问卷模板详情
 */
export function getQuestionnaireDetail(id) {
  return get(`/questionnaire-template/${id}`)
}

/**
 * 获取问卷提交记录详情
 */
export function getQuestionnaireSubmissionDetail(id) {
  return get(`/questionnaire-submission/${id}`)
}

/**
 * 提交问卷
 */
export function submitQuestionnaire(data) {
  return post('/questionnaire-submission/submit', data)
}

/**
 * 获取用户问卷
 */
export function getMySubmissions(params) {
  return get('/questionnaire-submission/userQuestion', params)
}

/**
 * 获取问卷提交详情
 */
export function getSubmissionDetail(id) {
  return get(`/questionnaire-submission/submission/${id}`)
}

/**
 * 更新问卷状态
 */
export function updateQuestionnaireStatus(data) {
  return post('/questionnaire-submission/updateStatus', data)
}