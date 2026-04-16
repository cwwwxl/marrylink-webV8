<template>
  <div class="flex">
    <template v-if="!isMobile">
      <!--全屏 -->
      <div class="nav-action-item" @click="toggle">
        <svg-icon
          :icon-class="isFullscreen ? 'fullscreen-exit' : 'fullscreen'"
        />
      </div>

      <!-- 布局大小 -->
      <el-tooltip
        :content="$t('sizeSelect.tooltip')"
        effect="dark"
        placement="bottom"
      >
        <size-select class="nav-action-item" />
      </el-tooltip>

      <!-- 语言选择 -->
      <lang-select class="nav-action-item" />

      <!-- 消息通知 -->
      <el-dropdown class="message nav-action-item" trigger="click" @visible-change="handleDropdownVisible">
        <el-badge :is-dot="unreadCount > 0">
          <div class="flex-center h100% p10px">
            <i-ep-bell />
          </div>
        </el-badge>
        <template #dropdown>
          <div class="w-[400px]">
            <el-tabs v-model="activeTab" @tab-change="handleTabChange" stretch>
              <el-tab-pane label="未读" name="unread" />
              <el-tab-pane label="全部" name="all" />
              <el-tab-pane label="已读" name="read" />
            </el-tabs>
            <div class="px-5 pb-2">
              <div v-if="activeTab === 'unread'" class="flex justify-end mb-2">
                <el-button size="small" @click="handleMarkAllRead">全部已读</el-button>
              </div>
              <div v-if="messages.length === 0" class="text-center py-4 text-gray-400">
                暂无消息
              </div>
              <div
                v-else
                class="py-2"
                v-for="message in messages"
                :key="message.id"
              >
                <el-text class="w-350px" size="default">
                  {{ message.content }}
                </el-text>
                <div class="text-xs text-gray-400 mt-1">
                  {{ formatTime(message.createTime) }}
                </div>
              </div>
            </div>
          </div>
        </template>
      </el-dropdown>
    </template>

    <!-- 用户头像 -->
    <el-dropdown class="nav-action-item" trigger="click">
      <div class="flex-center h100% p10px">
        <img
          :src="userStore.user.avatar + '?imageView2/1/w/80/h/80'"
          class="rounded-full mr-10px w24px h24px"
        />
        <span>{{ userStore.user.username }}</span>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item @click="openChangePasswordDialog">
            {{ $t("navbar.gitee") }}
          </el-dropdown-item>
          <el-dropdown-item divided @click="logout">
            {{ $t("navbar.logout") }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 设置 -->
    <template v-if="defaultSettings.showSettings">
      <div class="nav-action-item" @click="settingStore.settingsVisible = true">
        <svg-icon icon-class="setting" />
      </div>
    </template>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="changePasswordVisible"
      title="修改密码"
      width="420px"
      :close-on-click-modal="false"
      @closed="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        label-position="left"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（6-20位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changePasswordVisible = false">关闭</el-button>
        <el-button type="primary" :loading="changePasswordLoading" @click="handleChangePassword">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import {
  useAppStore,
  useTagsViewStore,
  useUserStore,
  useSettingsStore,
} from "@/store";
import defaultSettings from "@/settings";
import { DeviceEnum } from "@/enums/DeviceEnum";
import { getMessages, getUnreadCount, markAllRead } from "@/api/message";
import AuthAPI from "@/api/auth";
import type { FormInstance, FormRules } from "element-plus";

const appStore = useAppStore();
const tagsViewStore = useTagsViewStore();
const userStore = useUserStore();
const settingStore = useSettingsStore();

const route = useRoute();
const router = useRouter();

const isMobile = computed(() => appStore.device === DeviceEnum.MOBILE);

const { isFullscreen, toggle } = useFullscreen();

const messages = ref([]);
const unreadCount = ref(0);
const activeTab = ref("unread");

// ========== 修改密码相关 ==========
const changePasswordVisible = ref(false);
const changePasswordLoading = ref(false);
const passwordFormRef = ref<FormInstance>();

const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

// 自定义校验：确认密码与新密码一致
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const passwordRules = reactive<FormRules>({
  oldPassword: [{ required: true, message: "请输入旧密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度为6-20位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
});

/** 打开修改密码弹窗 */
function openChangePasswordDialog() {
  changePasswordVisible.value = true;
}

/** 重置表单 */
function resetPasswordForm() {
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  passwordFormRef.value?.resetFields();
}

/** 提交修改密码 */
async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  changePasswordLoading.value = true;
  try {
    await AuthAPI.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    });
    ElMessage.success("密码修改成功");
    changePasswordVisible.value = false;
  } catch (error: any) {
    // 错误信息由 request 拦截器统一处理
    console.error("修改密码失败", error);
  } finally {
    changePasswordLoading.value = false;
  }
}

// ========== 消息通知相关 ==========
const loadUnreadCount = async () => {
  try {
    const res = await getUnreadCount();
    unreadCount.value = res.total;
  } catch (error) {
    console.error("获取未读消息数量失败", error);
  }
};

const loadMessages = async () => {
  try {
    const status = activeTab.value === "all" ? undefined : activeTab.value === "unread" ? 1 : 2;
    const res = await getMessages(status);
    messages.value = res.records;
  } catch (error) {
    console.error("获取消息失败", error);
  }
};

const handleDropdownVisible = (visible: boolean) => {
  if (visible) {
    activeTab.value = "unread";
    loadMessages();
  }
};

const handleTabChange = () => {
  loadMessages();
};

const handleMarkAllRead = async () => {
  try {
    await markAllRead();
    ElMessage.success("已全部标记为已读");
    loadMessages();
    loadUnreadCount();
  } catch (error) {
    console.error("标记已读失败", error);
    ElMessage.error("操作失败");
  }
};

const formatTime = (time: string) => {
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString();
};

onMounted(() => {
  loadUnreadCount();
  setInterval(loadUnreadCount, 30000);
});

/* 注销 */
function logout() {
  ElMessageBox.confirm("确定注销并退出系统吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
    lockScroll: false,
  }).then(() => {
    userStore
      .logout()
      .then(() => {
        tagsViewStore.delAllViews();
      })
      .then(() => {
        router.push(`/login?redirect=${route.fullPath}`);
      });
  });
}
</script>
<style lang="scss" scoped>
.nav-action-item {
  display: inline-block;
  min-width: 40px;
  height: $navbar-height;
  line-height: $navbar-height;
  color: var(--el-text-color);
  text-align: center;
  cursor: pointer;

  &:hover {
    background: rgb(0 0 0 / 10%);
  }
}

:deep(.message .el-badge__content.is-fixed.is-dot) {
  top: 5px;
  right: 10px;
}

:deep(.el-divider--horizontal) {
  margin: 10px 0;
}

.dark .nav-action-item:hover {
  background: rgb(255 255 255 / 20%);
}

.layout-top .nav-action-item,
.layout-mix .nav-action-item {
  color: #fff;
}
</style>
