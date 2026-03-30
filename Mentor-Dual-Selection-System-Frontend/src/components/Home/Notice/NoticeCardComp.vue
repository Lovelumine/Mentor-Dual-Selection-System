<script setup lang="ts">
import { Paperclip } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { http, httpAdmin } from "@/utils/http";
import axios from "axios";
import { useUploadFileStore } from "@/stores/UploadFileStore";

const router = useRouter();
const userStore = useUserInfoStore();
const uploadFileStore = useUploadFileStore();

interface Notice {
  id: number;
  title: string;
  content: string;
  attachmentUrl: string | null;
  lastModified: string;
  isPublished?: boolean;
  lastModifiedCN?: string;
}

interface NoticeForm {
  id: number | null;
  title: string | null;
  content: string | null;
  attachmentUrl: string | null;
  isPublished: boolean;
}

const defaultFileName = "未选择（若您先前有附件，您就无需再选附件）";

const allNotice = ref<Notice[]>([]);
const dialogVisible = ref(false);
const isLoading = ref(true);
const loadError = ref("");
const noticeDetails = ref<NoticeForm>({
  id: null,
  title: null,
  content: null,
  attachmentUrl: null,
  isPublished: true,
});
const attachmentInput = ref<HTMLInputElement | null>(null);
const fileName = ref(defaultFileName);

const userPermi = computed(() => userStore.userInfo.role ?? "");
const isAdmin = computed(() => userPermi.value === "ADMIN");

function formatNoticeDate(lastModified: string) {
  const date = new Date(lastModified);
  const options: Intl.DateTimeFormatOptions = {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  };
  const formatter = new Intl.DateTimeFormat("zh-CN", options);
  const formattedParts = formatter.formatToParts(date);

  return `${formattedParts.find((part) => part.type === "year")?.value}-${formattedParts.find((part) => part.type === "month")?.value}-${formattedParts.find((part) => part.type === "day")?.value} `
    + `${formattedParts.find((part) => part.type === "hour")?.value}:${formattedParts.find((part) => part.type === "minute")?.value}:${formattedParts.find((part) => part.type === "second")?.value}`;
}

function normalizeNotice(notice: Notice): Notice {
  return {
    ...notice,
    lastModifiedCN: formatNoticeDate(notice.lastModified),
  };
}

function getFileName(url: string) {
  if (!url) return "";
  const decodedUrl = decodeURIComponent(url);
  const parts = decodedUrl.split("/");
  return parts[parts.length - 1];
}

function getErrorMessage(error: any, fallback: string) {
  return error?.response?.data?.data?.error
    || error?.response?.data?.message
    || fallback;
}

function isUnauthorized(error: any) {
  return error?.response?.status === 401;
}

async function fetchAnnouncements() {
  isLoading.value = true;
  loadError.value = "";

  try {
    const res = await http({
      url: "/announcements",
      method: "GET",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.data.code === 200 && Array.isArray(res.data.data)) {
      allNotice.value = res.data.data.map((item: Notice) => normalizeNotice(item));
      return;
    }

    allNotice.value = [];
  } catch (err) {
    if (isUnauthorized(err)) return;

    console.error(err);
    allNotice.value = [];
    loadError.value = getErrorMessage(err, "公告加载失败，请稍后重试");
  } finally {
    isLoading.value = false;
  }
}

function resetDialogFileState() {
  fileName.value = defaultFileName;
}

function triggerUploadFile() {
  if (attachmentInput.value) {
    attachmentInput.value.click();
    return;
  }

  ElMessage.error("未找到附件输入框");
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files ? input.files[0] : null;
  if (!file) return;

  fileName.value = file.name;
  const formData = new FormData();
  formData.append("file", file);
  uploadFileStore.changeIsLoading(true);

  try {
    const res = await axios({
      url: "/upload",
      method: "POST",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
        "Content-Type": "multipart/form-data",
      },
      data: formData,
    });

    if (res.status === 200 && res.data) {
      noticeDetails.value.attachmentUrl = res.data;
      fileName.value = `${file.name} - 上传成功`;
      ElMessage.success("文件上传成功");
      return;
    }

    fileName.value = `${file.name} - 上传失败`;
    ElMessage.error("文件上传失败");
  } catch (err) {
    if (isUnauthorized(err)) {
      userStore.clearSession();
      await router.replace("/");
      return;
    }

    console.error(err);
    fileName.value = `${file.name} - 上传失败`;
    ElMessage.error("文件上传失败");
  } finally {
    uploadFileStore.changeIsLoading(false);
    input.value = "";
  }
}

function changeNotice(target: Notice) {
  noticeDetails.value = {
    id: target.id,
    title: target.title,
    content: target.content,
    attachmentUrl: target.attachmentUrl,
    isPublished: target.isPublished ?? true,
  };
  fileName.value = target.attachmentUrl ? getFileName(target.attachmentUrl) : defaultFileName;
  dialogVisible.value = true;
}

function resettingOrinFile() {
  noticeDetails.value.attachmentUrl = null;
  fileName.value = defaultFileName;
}

function applyUpdatedNotice(updatedNotice: Notice) {
  const formattedNotice = normalizeNotice(updatedNotice);
  const targetIndex = allNotice.value.findIndex((item) => item.id === updatedNotice.id);

  if (targetIndex !== -1) {
    allNotice.value[targetIndex] = formattedNotice;
  }
}

async function changeNoticeCheck() {
  if (noticeDetails.value.id === null) {
    ElMessage.error("页面错误，请重新操作");
    return;
  }

  if (!noticeDetails.value.title) {
    ElMessage.warning("请填写公告标题");
    return;
  }

  if (!noticeDetails.value.content) {
    ElMessage.warning("请填写公告内容");
    return;
  }

  try {
    const res = await httpAdmin({
      url: `/announcements/${noticeDetails.value.id}`,
      method: "PUT",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      data: noticeDetails.value,
    });

    if (res.data.code === 200) {
      applyUpdatedNotice(res.data.data);
      dialogVisible.value = false;
      resetDialogFileState();
      ElMessage.success("修改成功");
      return;
    }

    ElMessage.error("修改失败");
  } catch (err) {
    if (isUnauthorized(err)) return;

    console.error(err);
    ElMessage.error(getErrorMessage(err, "修改失败"));
  }
}

async function deleteNotice(target: number) {
  try {
    await ElMessageBox.confirm("删除后将无法恢复，是否继续？", "删除公告", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }

  try {
    const res = await httpAdmin({
      url: `/announcements/${target}`,
      method: "DELETE",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.data.code === 200) {
      allNotice.value = allNotice.value.filter((item) => item.id !== target);
      ElMessage.success("删除成功");
      return;
    }

    ElMessage.error("删除失败");
  } catch (err) {
    if (isUnauthorized(err)) return;

    console.error(err);
    ElMessage.error(getErrorMessage(err, "删除失败"));
  }
}

onMounted(() => {
  void fetchAnnouncements();
});
</script>

<template>
  <div class="card_container">
    <el-dialog v-model="dialogVisible" :title="`修改公告：${noticeDetails.title}`">
      <el-form label-width="auto" :model="noticeDetails" @submit.prevent="changeNoticeCheck">
        <el-form-item label="公告标题：">
          <el-input v-model="noticeDetails.title" type="text" />
        </el-form-item>
        <el-form-item label="公告内容：">
          <el-input v-model="noticeDetails.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="附件：">
          <input ref="attachmentInput" style="display: none" type="file" @change="handleFileChange" />
          <el-button native-type="button" @click="triggerUploadFile">选择文件</el-button>
          <p class="dialog_file_name">文件：{{ fileName }}</p>
          <el-button native-type="button" @click="resettingOrinFile" title="若您先前有附件，现不再需要，可点此！">
            移除原有附件
          </el-button>
        </el-form-item>
        <el-button native-type="submit" type="primary">确认修改</el-button>
        <el-button native-type="button" @click="dialogVisible = false; resetDialogFileState()">取消修改</el-button>
      </el-form>
    </el-dialog>

    <div v-if="isLoading" class="state_box">
      <el-skeleton :rows="6" animated />
    </div>
    <el-result v-else-if="loadError" class="state_box" icon="error" title="公告加载失败" :sub-title="loadError">
      <template #extra>
        <el-button type="primary" @click="fetchAnnouncements">重新加载</el-button>
      </template>
    </el-result>
    <el-empty v-else-if="!allNotice.length" class="state_box" description="暂无公告" />
    <div v-else class="card_list">
      <div v-for="item in allNotice" :key="item.id" class="card_box">
        <div class="notice_title_box">
          <h3 class="notice_title">
            {{ item.title }}
          </h3>
          <span class="release_date">
            发布时间：{{ item.lastModifiedCN }}
          </span>
        </div>
        <hr />
        <div class="notice_content">
          {{ item.content }}
        </div>
        <div class="annex">
          <a v-if="item.attachmentUrl" :href="item.attachmentUrl" target="_blank" rel="noreferrer">
            <el-icon size="20"><Paperclip /></el-icon>
            {{ getFileName(item.attachmentUrl) }}
          </a>
          <div v-else class="annex_placeholder">无附件</div>
          <div v-if="isAdmin" class="button_box">
            <button class="button" @click="changeNotice(item)">修改</button>
            <button class="button" @click="deleteNotice(item.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.card_container
  width: 100%
  min-height: calc(100vh - 150px)
  max-height: calc(100vh - 150px)
  overflow: auto
  padding: 6px 0 12px

.card_list
  display: flex
  flex-direction: column

.state_box
  width: min(960px, 94%)
  margin: 24px auto
  padding: 24px
  border-radius: 18px
  border: 1px solid var(--app-border)
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  box-shadow: 0 18px 36px var(--app-shadow)

.dialog_file_name
  margin: 0
  padding: 0 12px
  color: var(--app-text-secondary)

.card_box
  margin: 0 auto 30px auto
  width: 97%
  height: 250px
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  color: var(--app-text-primary)
  padding: 24px
  border-radius: 18px
  border: 1px solid var(--app-border)
  box-shadow: 0 18px 36px var(--app-shadow)
  transition: .3s ease
  .notice_title_box
    display: flex
    gap: 16px
    align-items: flex-start
    h3
      margin: 0
      font-size: 23px
      color: var(--app-text-primary)
    .release_date
      margin: 2px 0 0 auto
      color: var(--app-text-secondary)
      white-space: nowrap
  hr
    margin: 10px auto
    border-color: var(--app-border)
  .notice_content
    height: 100px
    margin-bottom: 20px
    display: -webkit-box
    -webkit-line-clamp: 4
    -webkit-box-orient: vertical
    overflow: hidden
    text-overflow: ellipsis
    color: var(--app-text-secondary)
    line-height: 1.75
  .annex
    display: flex
    align-items: center
    gap: 12px
    a
      text-decoration: none
      display: inline-flex
      align-items: center
      gap: 6px
      color: var(--app-text-secondary)
      transition: color .25s ease
    a:hover
      color: var(--app-brand)
    .annex_placeholder
      color: var(--app-text-secondary)
      opacity: .78
    .button_box
      margin: 0 0 0 auto
      .button
        background-color: var(--app-brand)
        border: 1px solid var(--app-brand)
        color: var(--app-text-inverse)
        width: 60px
        height: 32px
        border-radius: 10px
        font-size: 15px
        transition: .3s ease
      .button:hover
        cursor: pointer
        background-color: var(--app-brand-hover)
        border: 1px solid var(--app-brand-hover)
      .button:last-child
        margin-left: 10px
        border: 1px solid var(--app-danger)
        background-color: var(--app-danger-soft)
        color: var(--app-danger)
      .button:last-child:hover
        background-color: var(--app-danger)
        color: #170808

.card_box:hover
  box-shadow: 0 24px 44px var(--app-shadow-strong)
  transform: translateY(-1%)
</style>
