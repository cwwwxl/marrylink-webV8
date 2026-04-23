<template>
  <div class="app-container">
    <el-card shadow="hover" header="平台设置">
      <el-form
        ref="formRef"
        v-loading="loading"
        :model="formData"
        :rules="rules"
        label-width="180px"
        style="max-width: 600px;"
      >
        <el-form-item label="佣金比例 (%)" prop="commissionRate">
          <el-input-number
            v-model="formData.commissionRate"
            :min="0"
            :max="100"
            :precision="2"
            :step="0.5"
          />
        </el-form-item>

        <el-form-item label="佣金缴纳期限 (天)" prop="commissionDeadlineDays">
          <el-input-number
            v-model="formData.commissionDeadlineDays"
            :min="1"
            :max="90"
            :step="1"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPlatformSettings, updatePlatformSetting } from '@/api/marrylink-api'

const loading = ref(false)
const formRef = ref()

const formData = reactive({
  commissionRate: 10,
  commissionDeadlineDays: 30
})

const rules = {
  commissionRate: [{ required: true, message: '请输入佣金比例', trigger: 'blur' }],
  commissionDeadlineDays: [{ required: true, message: '请输入佣金缴纳期限', trigger: 'blur' }]
}

async function fetchSettings() {
  loading.value = true
  try {
    const res = await getPlatformSettings()
    if (res && Array.isArray(res)) {
      res.forEach(item => {
        if (item.settingKey === 'commission_rate') {
          formData.commissionRate = Number(item.settingValue)
        } else if (item.settingKey === 'commission_deadline_days') {
          formData.commissionDeadlineDays = Number(item.settingValue)
        }
      })
    } else if (res) {
      if (res.commissionRate !== undefined) formData.commissionRate = res.commissionRate
      if (res.commissionDeadlineDays !== undefined) formData.commissionDeadlineDays = res.commissionDeadlineDays
    }
  } finally {
    loading.value = false
  }
}

function handleSave() {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await updatePlatformSetting({
          commissionRate: formData.commissionRate,
          commissionDeadlineDays: formData.commissionDeadlineDays
        })
        ElMessage.success('保存成功')
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchSettings()
})
</script>
