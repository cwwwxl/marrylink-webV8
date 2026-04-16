<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 标签分类 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>标签分类</span>
              <el-button type="primary" size="small" @click="handleOpenCategoryDialog()">新增分类</el-button>
            </div>
          </template>

          <el-table :data="categoryList" border stripe>
            <el-table-column label="分类名称" prop="name" />
            <el-table-column label="分类编码" prop="code" />
            <el-table-column label="标签数量" prop="tagCount" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status == 1 ? 'success' : 'info'">
                  {{ scope.row.status == 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button type="primary" link size="small" @click="handleSelectCategory(scope.row)">
                  查看标签
                </el-button>
                <el-button type="primary" link size="small" @click="handleOpenCategoryDialog(scope.row.id)">
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 标签列表 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>标签列表 {{ selectedCategory ? `- ${selectedCategory.name}` : '' }}</span>
              <el-button 
                type="primary" 
                size="small" 
                :disabled="!selectedCategory"
                @click="handleOpenTagDialog()"
              >
                新增标签
              </el-button>
            </div>
          </template>

          <el-table v-loading="loading" :data="tagList" border stripe>
            <el-table-column label="标签名称" prop="name" />
            <el-table-column label="标签编码" prop="code" />
            <el-table-column label="使用次数" prop="useCount" width="100" />
            <el-table-column label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status == 1 ? 'success' : 'info'">
                  {{ scope.row.status == 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button type="primary" link size="small" @click="handleOpenTagDialog(scope.row.id)">
                  编辑
                </el-button>
                <el-button type="danger" link size="small" @click="handleDeleteTag(scope.row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分类表单 -->
    <el-dialog v-model="categoryDialog.visible" :title="categoryDialog.title" width="500px">
      <el-form ref="categoryFormRef" :model="categoryFormData" :rules="categoryRules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryFormData.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input v-model="categoryFormData.code" placeholder="请输入分类编码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="categoryFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCategory">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标签表单 -->
    <el-dialog v-model="tagDialog.visible" :title="tagDialog.title" width="500px">
      <el-form ref="tagFormRef" :model="tagFormData" :rules="tagRules" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="tagFormData.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签编码" prop="code">
          <el-input v-model="tagFormData.code" placeholder="请输入标签编码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="tagFormData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTag">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTagCategoryList,
  getTagCategoryById,
  saveTagCategory,
  updateTagCategory,
  getTagList,
  getTagById,
  saveTag,
  updateTag,
  deleteTag
} from '@/api/marrylink-api'

const categoryList = ref([])
const tagList = ref([])
const loading = ref(false)
const selectedCategory = ref(null)

const categoryDialog = reactive({
  visible: false,
  title: '新增分类'
})

const tagDialog = reactive({
  visible: false,
  title: '新增标签'
})

const categoryFormData = reactive({
  id: null,
  name: '',
  code: '',
  status: 1
})

const tagFormData = reactive({
  id: null,
  categoryCode: null,
  name: '',
  code: '',
  status: 1
})

const categoryFormRef = ref()
const tagFormRef = ref()

const categoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入分类编码', trigger: 'blur' }]
}

const tagRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入标签编码', trigger: 'blur' }]
}

async function fetchCategoryList() {
  const res = await getTagCategoryList()
  categoryList.value = res
}

async function fetchTagList(categoryCode) {
  loading.value = true
  try {
    const res = await getTagList(categoryCode)
    tagList.value = res
  } finally {
    loading.value = false
  }
}

function handleSelectCategory(category) {
  selectedCategory.value = category
  fetchTagList(category.code)
}

async function handleOpenCategoryDialog(id) {
  categoryDialog.visible = true
  if (id) {
    categoryDialog.title = '编辑分类'
    const res = await getTagCategoryById(id)
    Object.assign(categoryFormData, res)
  } else {
    categoryDialog.title = '新增分类'
    Object.assign(categoryFormData, { id: null, name: '', code: '', status: 1 })  
  }
}

function handleSubmitCategory() {
  categoryFormRef.value.validate(async (valid) => {
    if (valid) {
      if (categoryFormData.id) {
        await updateTagCategory(categoryFormData)
        ElMessage.success('修改成功')
      } else {
        await saveTagCategory(categoryFormData)
        ElMessage.success('新增成功')
      }
      categoryDialog.visible = false
      fetchCategoryList()
      Object.assign(categoryFormData, { id: null, name: '', code: '', status: 1 })
    }
  })
}

async function handleOpenTagDialog(id) {
  if (!selectedCategory.value) {
    ElMessage.warning('请先选择标签分类')
    return
  }
  tagDialog.visible = true
  if (id) {
    tagDialog.title = '编辑标签'
    const res = await getTagById(id)
    Object.assign(tagFormData, res)
  } else {
    tagDialog.title = '新增标签'
    Object.assign(tagFormData, {
      id: null,
      categoryCode: selectedCategory.value.code,
      name: '',
      code: '',
      status: 1
    })
  }
}

function handleSubmitTag() {
  tagFormRef.value.validate(async (valid) => {
    if (valid) {
      if (tagFormData.id) {
        await updateTag(tagFormData)
        ElMessage.success('修改成功')
      } else {
        await saveTag(tagFormData)
        ElMessage.success('新增成功')
      }
      tagDialog.visible = false
      fetchTagList(selectedCategory.value.code)
      Object.assign(tagFormData, { id: null, categoryCode: null, name: '', code: '', status: 1 })
    }
  })
}

function handleDeleteTag(id) {
  ElMessageBox.confirm('确认删除该标签?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteTag(id)
    ElMessage.success('删除成功')
    fetchTagList(selectedCategory.value.code)
  })
}

onMounted(() => {
  fetchCategoryList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>