<script setup lang="ts">
import { Paperclip } from "@element-plus/icons-vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { onMounted, ref, watch } from "vue";
import { httpAdmin } from "@/utils/http";
import axios from "axios";
const userStore = useUserInfoStore();
import { useUploadFileStore } from "@/stores/UploadFileStore";
const uploadFileStore = useUploadFileStore();

const userPermi = ref<string | null>('');
interface Notice {
  id: number;
  title: string;
  content: string;
  attachmentUrl: string | null;
  lastModified: string;
  lastModifiedCN?: string;  // 可选字段，用于格式化的日期
}

const allNotice = ref<Notice[]>([]);
const dialogVisible = ref(false);
const noticeDetails = ref({
  id: null as number | null,
  title: null as string | null,
  content: null as string | null,
  attachmentUrl: null as string | null,
  published: true
});
const attachmentInput = ref<HTMLInputElement | null>(null);
const fileName = ref('未选择（若您先前有附件，您就无需再选附件）');

function triggerUploadFile() {
  if (attachmentInput.value) {
    attachmentInput.value.click();
  } else {
    alert("未找到附件输入框！");
  }
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files ? input.files[0] : null;
  if (!file) return;
  fileName.value = file.name;
  const formData = new FormData();
  formData.append("file", file);
  uploadFileStore.changeIsLoading(true);
  axios({
    url: "/upload",
    method: "POST",
    headers: {
      Accept: "*/*",
      "Authorization": `Bearer ${localStorage.getItem("token")}`,
      "Content-Type": "multipart/form-data",
    },
    data: formData,
  })
    .then((res) => {
      if (res.status === 200 && res.data) {
        noticeDetails.value.attachmentUrl = res.data;
        alert("文件上传成功！");
        fileName.value += '-上传成功';
        uploadFileStore.changeIsLoading(false);
      } else {
        alert("文件上传失败！");
        fileName.value += '-上传失败';
        uploadFileStore.changeIsLoading(false);
      }
    })
    .catch((err) => {
      alert("文件上传失败！");
      fileName.value += '-上传失败';
      console.error(err);
      uploadFileStore.changeIsLoading(false);
    });
}

function getFileName(url: string): string {
  if (!url) return '';
  const decodedUrl = decodeURIComponent(url);
  const parts = decodedUrl.split('/');
  return parts[parts.length - 1];
}

function changePermi(target: any) {
  userPermi.value = target.role;
}

function changeNotice(target: number, index: number) {
  noticeDetails.value.id = target;
  noticeDetails.value.title = allNotice.value[index].title;
  noticeDetails.value.content = allNotice.value[index].content;
  noticeDetails.value.attachmentUrl = allNotice.value[index].attachmentUrl;
  dialogVisible.value = true;
}

function resettingOrinFile() {
  noticeDetails.value.attachmentUrl = null;
}

function changeNoticeCheck() {
  if (noticeDetails.value.id === null) {
    alert('页面错误！请重新操作！');
    return;
  } else if (noticeDetails.value.title === null || noticeDetails.value.title === '') {
    alert('请填写公告标题！');
    return;
  } else if (noticeDetails.value.content === null || noticeDetails.value.content === '') {
    alert('请填写公告内容！');
    return;
  }
  httpAdmin({
    url: `/announcements/${noticeDetails.value.id}`,
    method: 'PUT',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`
    },
    data: noticeDetails.value
  }).then(res => {
    if (res.data.code === 200){
      alert('修改成功！');
      window.location.reload();
    } else alert('修改失败！');
  }).catch(err => {
    alert('修改失败！');
    console.error(err);
  })
}

function deleteNotice(target: number){
  httpAdmin({
    url: `/announcements/${target}`,
    method: 'DELETE',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    }
  }).then(res => {
    if (res.data.code === 200){
      alert('删除成功！');
      window.location.reload();
    } else alert('删除失败！');
  }).catch(err => {
    alert('删除失败！');
    console.error(err);
  })
}

onMounted(() => {
  if (userStore.userInfo){
    userPermi.value = userStore.userInfo.role;
  }
  httpAdmin({
    url: '/announcements',
    method: 'GET',
    headers: {
      Accept: '*/*',
      Authorization: 'Bearer ' + localStorage.getItem('token'),
    }
  }).then(res => {
    if (res.data.code === 200) {
      allNotice.value = res.data.data;
      for (let i = 0; i < allNotice.value.length; i++) {
        const date = new Date(allNotice.value[i].lastModified);
        const options: Intl.DateTimeFormatOptions = {
          timeZone: 'Asia/Shanghai',
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: false,
        };
        const formatter = new Intl.DateTimeFormat('zh-CN', options);
        const formattedParts = formatter.formatToParts(date);
        const formattedDate = `${formattedParts.find(p => p.type === 'year')?.value}-${formattedParts.find(p => p.type === 'month')?.value}-${formattedParts.find(p => p.type === 'day')?.value} `
            + `${formattedParts.find(p => p.type === 'hour')?.value}:${formattedParts.find(p => p.type === 'minute')?.value}:${formattedParts.find(p => p.type === 'second')?.value}`;
        allNotice.value[i].lastModifiedCN = formattedDate;
      }
    } else {
      alert('公告获取异常！');
    }
  }).catch(err => {
    alert('公告获取异常！');
    console.error(err);
  })
})

watch(() => userStore.userInfo, (newValue) => {
  changePermi(newValue);
})
</script>

<template>
  <div class="card_container">
    <el-dialog v-model="dialogVisible" :title="`修改公告：${noticeDetails.title}`">
      <el-form label-width="auto" :model="noticeDetails" @submit.prevent="changeNoticeCheck">
        <el-form-item label="公告标题：">
          <el-input type="text" v-model="noticeDetails.title"/>
        </el-form-item>
        <el-form-item label="公告内容：">
          <el-input type="textarea" :rows="4" v-model="noticeDetails.content"/>
        </el-form-item>
        <el-form-item label="附件：">
          <input style="display: none" type="file" ref="attachmentInput" @change="handleFileChange"/>
          <el-button native-type="button" @click="triggerUploadFile">选择文件</el-button>
          <p>&nbsp;&nbsp;&nbsp;文件：{{fileName}}&nbsp;&nbsp;&nbsp;</p>
          <el-button native-type="button" @click="resettingOrinFile" title="若您先前有附件，现不再需要，可点此！">移除原有附件</el-button>
        </el-form-item>
        <el-button native-type="submit" type="primary">确认修改</el-button>
        <el-button native-type="button" @click="dialogVisible = false">取消修改</el-button>
      </el-form>
    </el-dialog>
    <div class="card_box" v-for="(item, index) in allNotice" :key="index">
      <div class="notice_title_box">
        <h3 class="notice_title">
          {{item.title}}
        </h3>
        <span class="release_date">
        发布时间：{{item.lastModifiedCN}}
      </span>
      </div>
      <hr/>
      <div class="notice_content">
        {{item.content}}
      </div>
      <div class="annex">
        <a :href="item.attachmentUrl" v-if="item.attachmentUrl">
          <el-icon size="20"><Paperclip /></el-icon>
          {{ getFileName(item.attachmentUrl) }}
        </a>
        <div class="button_box" v-if="userPermi === 'ADMIN'">
          <button class="button" @click="changeNotice(item.id, index)">修改</button>
          <button class="button" @click="deleteNotice(item.id)">删除</button>
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
  overflow: scroll
.card_box
  margin: 0 auto 30px auto
  width: 97%
  height: 250px
  background-color: white
  padding: 20px
  border-radius: 10px
  transition: .3s ease
  .notice_title_box
    display: flex
    h3
      font-size: 23px
    .release_date
      margin: auto 0 0 auto
  hr
    margin: 10px auto
  .notice_content
    height: 100px
    margin-bottom: 20px
    display: -webkit-box
    -webkit-line-clamp: 4
    -webkit-box-orient: vertical
    overflow: hidden
    text-overflow: ellipsis
  .annex
    display: flex
    a
      text-decoration: none
      display: inline-flex
      align-items: center

    .button_box
      margin: 0 0 0 auto
      .button
        background-color: #005826
        border: 1px solid #005826
        color: white
        width: 60px
        height: 32px
        border-radius: 5px
        font-size: 15px
        transition: .3s ease
      .button:hover
        cursor: pointer
        background-color: #0f7e3f
        border: 1px solid #0f7e3f
      .button:last-child
        margin-left: 10px
        border: 1px solid #bd0000
        background-color: #bd000000
        color: #bd0000
      .button:last-child:hover
        background-color: #bd0000
        color: white

.card_box:hover
  box-shadow: 0 10px 20px 0 rgba(0,0,0,0.2)
  transform: translateY(-1%)
</style>
