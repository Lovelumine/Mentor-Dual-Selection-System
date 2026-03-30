<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { httpStudent, httpTeacher } from "@/utils/http";
import axios from "axios";
import { getPortraitImageUrl } from "@/utils/media";

const userInfoStore = useUserInfoStore();
import { useUploadFileStore } from "@/stores/UploadFileStore";
const uploadFileStore = useUploadFileStore();

const userRole = ref<string | null>(null);
const userDetail = ref({
  uid: -1,
  photoUrl: '',
  teacherPosition: '',
  researchDirection: '',
  professionalDirection: '',
  resume: '',
  netid: '',
  studentGrade: null,
});
const userDetailTemp = ref({
  uid: -1,
  photoUrl: '',
  teacherPosition: '',
  researchDirection: '',
  professionalDirection: '',
  resume: '',
  netid: '',
  studentGrade: null,
});
const isChangeDetailDisabled = ref(true);
const fileInput = ref<HTMLInputElement | null>(null);
const fileName = ref('未选择');

function triggerUploadFile() {
  if (fileInput.value) fileInput.value.click();
}

function handleFileChange(event: Event) {
  const inputElement = event.target as HTMLInputElement | null;
  let file: any;
  if (inputElement && inputElement.files){
    file = inputElement.files[0];
  }
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
        userDetailTemp.value.photoUrl = res.data;
        fileName.value += '-上传成功';
        alert("文件上传成功！");
        uploadFileStore.changeIsLoading(false);
      } else {
        fileName.value += '-上传失败';
        alert("文件上传失败！");
        uploadFileStore.changeIsLoading(false);
      }
    })
    .catch((err) => {
      fileName.value += '-上传失败';
      alert("文件上传失败！");
      console.error(err);
      uploadFileStore.changeIsLoading(false);
    });
}

function changeDetailDisabled() {
  isChangeDetailDisabled.value = !isChangeDetailDisabled.value;
  if (isChangeDetailDisabled.value) {
    window.location.reload();
  }
}

function changeDetailChecked(target: string) {
  const updateApi = target === 'STUDENT' ? httpStudent : httpTeacher;
  
  updateApi({
    url: '/update',
    method: 'PUT',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`,
      'Content-Type': 'application/json'
    },
    data: userDetailTemp.value
  })
    .then((res) => {
      if (res.data.code === 200) {
        alert('信息更新成功！');
        window.location.reload();
      } else {
        alert('信息更新失败！');
        console.log(res);
      }
    })
    .catch((err) => {
      alert('信息更新失败！');
      console.error(err);
    });
}

onMounted(() => {
  userInfoStore.fetchUserInfo();
});

function getUserDetail(targetInfo: any) {
  const fetchDetailApi = targetInfo?.role === 'TEACHER' || targetInfo?.role === 'ADMIN' ? httpTeacher : httpStudent;

  fetchDetailApi({
    url: '/my-detail',
    method: 'GET',
    headers: {
      Accept: '*/*',
      Authorization: 'Bearer ' + localStorage.getItem('token'),
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        userDetail.value = res.data.data;
      } else {
        if (targetInfo) {
          userDetail.value.uid = targetInfo.uid;
          userDetail.value.netid = targetInfo.username;
        }
      }
      userDetailTemp.value = { ...userDetail.value };
    })
    .catch((err) => {
      alert(JSON.parse(err.request.responseText).data.error);
      console.error(err);
    });
}

watch(
  () => userInfoStore.userInfo,
  (newValue) => {
    if (newValue){
      userRole.value = newValue.role;
      getUserDetail(newValue);
    }
  }
);

const getProfilePhotoUrl = (url?: string | null) => getPortraitImageUrl(url);
</script>

<template>
  <div class="my_detail_box">
    <!-- 教师用户表单 -->
    <el-form v-if="userRole === 'TEACHER'" label-width="auto" style="max-width: 600px; margin: 0 auto">
      <h3 class="form-title">详细信息</h3>
      <el-form-item label="照片：" class="form-label">
        <div class="photo_box">
          <img v-if="getProfilePhotoUrl(userDetailTemp.photoUrl)" :src="getProfilePhotoUrl(userDetailTemp.photoUrl)" alt="photo"/>
          <div v-else class="photo_placeholder">未设置照片</div>
        </div>
        <input type="file" style="display: none" ref="fileInput" @change="handleFileChange"/>
        <button v-if="!isChangeDetailDisabled" type="button" class="button" @click="triggerUploadFile">选择图片</button>
        <p v-if="!isChangeDetailDisabled">&nbsp;&nbsp;&nbsp;{{ fileName }}&nbsp;&nbsp;&nbsp;</p>
      </el-form-item>
      <el-form-item label="职称：" class="form-label">
        <el-input v-if="!isChangeDetailDisabled" v-model="userDetailTemp.teacherPosition"/>
        <span v-else class="form-text">{{ userDetail.teacherPosition }}</span>
      </el-form-item>
      <el-form-item label="研究方向：" class="form-label">
        <el-input
          v-if="!isChangeDetailDisabled"
          v-model="userDetailTemp.researchDirection"
          type="textarea"
          :rows="4"
          maxlength="5000"
          show-word-limit
        />
        <span v-else class="form-text">{{ userDetail.researchDirection }}</span>
      </el-form-item>
      <el-form-item label="简介：" class="form-label">
        <el-input v-if="!isChangeDetailDisabled" type="textarea" :rows="4" v-model="userDetailTemp.resume"/>
        <span v-else class="form-text">{{ userDetail.resume }}</span>
      </el-form-item>
      <button class="button" type="button" @click="changeDetailDisabled">{{ isChangeDetailDisabled ? '编辑信息' : '取消编辑' }}</button>
      <button v-if="!isChangeDetailDisabled" class="button" type="button" @click="changeDetailChecked('TEACHER')">保存更改</button>
      <br><br>
    </el-form>

    <!-- 学生用户表单 -->
    <el-form v-if="userRole === 'STUDENT'" label-width="auto" style="max-width: 600px; margin: 0 auto">
      <h3 class="form-title">详细信息</h3>
      <el-form-item label="照片：" class="form-label">
        <div class="photo_box">
          <img v-if="getProfilePhotoUrl(userDetailTemp.photoUrl)" :src="getProfilePhotoUrl(userDetailTemp.photoUrl)" alt="photo"/>
          <div v-else class="photo_placeholder">未设置照片</div>
        </div>
        <input type="file" style="display: none" ref="fileInput" @change="handleFileChange"/>
        <button v-if="!isChangeDetailDisabled" type="button" class="button" @click="triggerUploadFile">选择图片</button>
        <p v-if="!isChangeDetailDisabled">&nbsp;&nbsp;&nbsp;{{ fileName }}&nbsp;&nbsp;&nbsp;</p>
      </el-form-item>
      <el-form-item label="年级：" class="form-label">
        <el-input v-if="!isChangeDetailDisabled" v-model="userDetailTemp.studentGrade"/>
        <span v-else class="form-text">{{ userDetail.studentGrade }}</span>
      </el-form-item>
      <el-form-item label="意向研究方向：" class="form-label">
        <el-input
          v-if="!isChangeDetailDisabled"
          v-model="userDetailTemp.researchDirection"
          type="textarea"
          :rows="4"
          maxlength="5000"
          show-word-limit
        />
        <span v-else class="form-text">{{ userDetail.researchDirection }}</span>
      </el-form-item>
      <el-form-item label="专业：" class="form-label">
        <el-input v-if="!isChangeDetailDisabled" v-model="userDetailTemp.professionalDirection"/>
        <span v-else class="form-text">{{ userDetail.professionalDirection }}</span>
      </el-form-item>
      <el-form-item label="简介：" class="form-label">
        <el-input v-if="!isChangeDetailDisabled" type="textarea" :rows="4" v-model="userDetailTemp.resume"/>
        <span v-else class="form-text">{{ userDetail.resume }}</span>
      </el-form-item>
      <button class="button" type="button" @click="changeDetailDisabled">{{ isChangeDetailDisabled ? '编辑信息' : '取消编辑' }}</button>
      <button v-if="!isChangeDetailDisabled" class="button" type="button" @click="changeDetailChecked('STUDENT')">保存更改</button>
      <br><br>
    </el-form>
  </div>
</template>

<style scoped lang="sass">
.my_detail_box
  box-shadow: 0 20px 40px var(--app-shadow)
  border-radius: 18px
  border: 1px solid var(--app-border)
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  width: 100%
  margin: 16px auto
  text-align: center
  color: var(--app-text-primary)
  padding: 24px 0
  :deep(.el-form-item__label)
    color: var(--app-text-primary)
    font-weight: 600
  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner)
    background-color: var(--app-surface)
  .photo_box
    width: 75px
    height: 99px
    overflow: hidden
    border-radius: 12px
    border: 1px solid var(--app-border)
    display: flex
    align-items: center
    justify-content: center
    background: linear-gradient(160deg, color-mix(in srgb, var(--app-surface) 82%, #eef4fa 18%) 0%, color-mix(in srgb, var(--app-surface-elevated) 88%, #dbe8f5 12%) 100%)
    img
      width: 100%
      height: 100%
      object-fit: cover
  .photo_placeholder
    padding: 0 8px
    font-size: 12px
    line-height: 1.4
    color: var(--app-text-tertiary)
    text-align: center
  .form-title
    color: var(--app-brand)
    font-weight: bold
    font-size: 1.5em
    margin-bottom: 10px
  .form-label
    font-weight: bold
  .form-text
    font-size: 1em
    color: var(--app-text-secondary)
  .button
    border-radius: 10px
    width: 100px
    height: 32px
    background-color: var(--app-brand)
    color: var(--app-text-inverse)
    margin-left: 20px
    border: none
    font-size: 15px
    transition: .3s ease
  .button:hover
    background-color: var(--app-brand-hover)
  .button:disabled
    background-color: color-mix(in srgb, var(--app-surface-muted) 78%, var(--app-border-strong) 22%)
</style>
