<script setup lang="ts">
import { ref, watch } from "vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { useRouter } from "vue-router";
import { http, httpAdmin } from "@/utils/http";
import axios from "axios";
const userInfoStore = useUserInfoStore();
const router = useRouter();

const noticeForm = ref({
  title: null,
  content: null,
  attachmentUrl: null,
  published: true,
});
const attachmentInput = ref(null);
const uploadStatus = ref("");

function resettingClicked() {
  window.location.reload();
}

// 上传文件的函数
function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append("file", file);

  // 上传文件到 /upload 接口（不是 /admin/upload）
  axios({
    url: "/upload", 
    method: "POST",
    headers: {
      Accept: "*/*",
      "Authorization": `Bearer ${localStorage.getItem("token")}`,
      "Content-Type": "multipart/form-data", // 确保使用 multipart/form-data
    },
    data: formData,
  })
    .then((res) => {
      if (res.status === 200 && res.data) {
        // 假设响应返回文件的访问链接
        noticeForm.value.attachmentUrl = res.data;
        uploadStatus.value = "文件上传成功！";
      } else {
        uploadStatus.value = "文件上传失败！";
      }
    })
    .catch((err) => {
      uploadStatus.value = "文件上传失败！";
      console.error(err);
    });
}

// 提交表单的函数
function uploadRelease() {
  if (noticeForm.value.title === null || noticeForm.value.title === "") {
    alert("请填写公告标题！");
    return;
  } else if (noticeForm.value.content === null || noticeForm.value.content === "") {
    alert("请填写公告内容！");
    return;
  }

  httpAdmin({
    url: "/announcements",
    method: "POST",
    headers: {
      Accept: "*/*",
      "Authorization": `Bearer ${localStorage.getItem("token")}`,
    },
    data: noticeForm.value,
  })
    .then((res) => {
      if (res.data.code === 201) {
        alert("发布成功！");
        window.location.reload();
      } else {
        alert("发布失败！");
      }
    })
    .catch((err) => {
      alert("发布失败！");
      console.error(err);
    });
}

function triggerUploadFile() {
  attachmentInput.value.click();
}

watch(
  () => userInfoStore.userInfo,
  (newVal) => {
    if (newVal.role !== "ADMIN") router.back();
  }
);
</script>

<template>
  <div class="release_box">
    <p v-if="uploadStatus">{{ uploadStatus }}</p>
    <el-form label-width="auto" style="max-width: 600px" :model="noticeForm" @submit.prevent="uploadRelease">
      <el-form-item label="公告标题：">
        <el-input type="text" v-model="noticeForm.title" placeholder="请简略表达" />
      </el-form-item>
      <el-form-item label="公告内容：">
        <el-input type="textarea" :rows="4" v-model="noticeForm.content" placeholder="请简略表达" />
      </el-form-item>
      <el-form-item label="附件：">
        <input style="display: none" @change="handleFileChange" type="file" ref="attachmentInput" :accept="['.doc', '.docx']" />
        <button class="button" @click="triggerUploadFile" type="button">上传附件</button>
      </el-form-item>
      <button type="submit" class="button_out">上传</button>
      <button type="button" @click="resettingClicked" class="button_out">重置</button>
    </el-form>
  </div>
</template>

<style scoped lang="sass">
.button_out, .button
  margin-left: 20px
  padding: 0 20px
  height: 32px
  width: 120px
  font-size: 14px
  border-radius: 5px
  border: none
  color: white
  background-color: #005826
  transition: .3s ease
.button_out:hover, button:hover
  background-color: #0f7e3f
.button_out:last-child
  background-color: white
  border: 1px solid #bd0000
  color: #bd0000
.button_out:last-child:hover
  background-color: #bd0000
  color: white
</style>
