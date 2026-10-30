<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { useRouter } from "vue-router";
import { httpAdmin } from "@/utils/http";
import axios from "axios";
import { useUploadFileStore } from "@/stores/UploadFileStore";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";

const userInfoStore = useUserInfoStore();
const router = useRouter();
const uploadFileStore = useUploadFileStore();
const { isMobile } = useResponsiveLayout();

const defaultFileName = "未选择";

const noticeForm = ref({
  title: null as string | null,
  content: null as string | null,
  attachmentUrl: null as string | null,
  isPublished: true,
});
const attachmentInput = ref<HTMLInputElement | null>(null);
const uploadStatus = ref("");
const fileName = ref(defaultFileName);
const isAdmin = computed(() => userInfoStore.userInfo.role === "ADMIN");

function resetNoticeForm() {
  noticeForm.value = {
    title: null,
    content: null,
    attachmentUrl: null,
    isPublished: true,
  };
  uploadStatus.value = "";
  fileName.value = defaultFileName;
}

function getErrorMessage(error: any, fallback: string) {
  return error?.response?.data?.data?.error
    || error?.response?.data?.message
    || fallback;
}

function isUnauthorized(error: any) {
  return error?.response?.status === 401;
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files ? input.files[0] : null;
  if (!file) return;

  const formData = new FormData();
  formData.append("file", file);
  fileName.value = file.name;
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
      noticeForm.value.attachmentUrl = res.data;
      uploadStatus.value = "文件上传成功";
      fileName.value = `${file.name} - ${uploadStatus.value}`;
      ElMessage.success(uploadStatus.value);
      return;
    }

    uploadStatus.value = "文件上传失败";
    fileName.value = `${file.name} - ${uploadStatus.value}`;
    ElMessage.error(uploadStatus.value);
  } catch (err) {
    if (isUnauthorized(err)) {
      userInfoStore.clearSession();
      await router.replace("/");
      return;
    }

    uploadStatus.value = "文件上传失败";
    fileName.value = `${file.name} - ${uploadStatus.value}`;
    console.error(err);
    ElMessage.error(uploadStatus.value);
  } finally {
    uploadFileStore.changeIsLoading(false);
    input.value = "";
  }
}

async function uploadRelease() {
  if (!noticeForm.value.title) {
    ElMessage.warning("请填写公告标题");
    return;
  }

  if (!noticeForm.value.content) {
    ElMessage.warning("请填写公告内容");
    return;
  }

  try {
    const res = await httpAdmin({
      url: "/announcements",
      method: "POST",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      data: noticeForm.value,
    });

    if (res.data.code === 201) {
      ElMessage.success("发布成功");
      resetNoticeForm();
      await router.push("/notice");
      return;
    }

    ElMessage.error("发布失败");
  } catch (err) {
    if (isUnauthorized(err)) return;

    console.error(err);
    ElMessage.error(getErrorMessage(err, "发布失败"));
  }
}

function triggerUploadFile() {
  if (attachmentInput.value) {
    attachmentInput.value.click();
    return;
  }

  console.error("附件输入框未找到");
}

watch(
  () => userInfoStore.userInfo,
  (newVal) => {
    if (newVal.role !== "ADMIN") router.replace("/notice");
  },
  { immediate: true }
);
</script>

<template>
  <div class="release_box">
    <el-form
      label-width="auto"
      style="max-width: 600px"
      :label-position="isMobile ? 'top' : 'right'"
      :model="noticeForm"
      @submit.prevent="uploadRelease"
    >
      <el-form-item label="公告标题：">
        <el-input v-model="noticeForm.title" type="text" placeholder="请简略表达" />
      </el-form-item>
      <el-form-item label="公告内容：">
        <el-input v-model="noticeForm.content" type="textarea" :rows="4" placeholder="请简略表达" />
      </el-form-item>
      <el-form-item label="附件：">
        <input ref="attachmentInput" style="display: none" type="file" @change="handleFileChange" />
        <button class="button" type="button" @click="triggerUploadFile">上传附件</button>
        <p>&nbsp;&nbsp;&nbsp;{{ fileName }}</p>
      </el-form-item>
      <div class="release_actions">
        <button type="submit" class="button_out" :disabled="!isAdmin">上传</button>
        <button type="button" class="button_out" @click="resetNoticeForm">重置</button>
      </div>
    </el-form>
  </div>
</template>

<style scoped lang="sass">
.release_box
  width: min(760px, 92%)
  margin: 8px auto 0
  padding: 28px
  border-radius: 18px
  border: 1px solid var(--app-border)
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  box-shadow: 0 18px 36px var(--app-shadow)
  color: var(--app-text-primary)
  p
    color: var(--app-text-secondary)
  :deep(.el-form)
    max-width: 100% !important
  :deep(.el-form-item__label)
    color: var(--app-text-primary)
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner)
    background-color: var(--app-surface)

.button_out, .button
  padding: 0 20px
  height: 32px
  width: 120px
  font-size: 14px
  border-radius: 10px
  border: none
  color: var(--app-text-inverse)
  background-color: var(--app-brand)
  transition: .3s ease

.button_out:hover, button:hover
  background-color: var(--app-brand-hover)

.button_out:disabled
  opacity: .6
  cursor: not-allowed

.button_out:last-child
  background-color: var(--app-danger-soft)
  border: 1px solid var(--app-danger)
  color: var(--app-danger)

.button_out:last-child:hover
  background-color: var(--app-danger)
  color: #170808

.button_out
  margin-left: 20px

.release_actions
  display: flex
  justify-content: flex-end

@media (max-width: 767px)
  .release_box
    width: calc(100% - 24px)
    margin-top: 12px
    padding: 18px 16px
    box-sizing: border-box
    border-radius: 14px

    :deep(.el-form-item__label)
      width: 100%
      text-align: left
      line-height: 1.4
      padding-bottom: 6px

    :deep(.el-form-item__content)
      min-width: 0

    p
      max-width: 100%
      overflow-wrap: anywhere

  .release_actions
    width: 100%
    flex-direction: column-reverse
    gap: 10px

  .button_out,
  .button
    width: 100%
    min-height: 40px
    margin-left: 0
</style>
