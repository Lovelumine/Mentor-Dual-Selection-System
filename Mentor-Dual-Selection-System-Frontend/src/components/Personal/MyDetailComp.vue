<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {http, httpStudent, httpTeacher} from "@/utils/http";
import axios from "axios";
const userInfoStore = useUserInfoStore();

const userRole = ref(null);
const userDetail = ref({
  uid: -1,
  photoUrl: '',
  teacherPosition: '',
  researchDirection: '',
  professionalDirection: '',
  resume: '',
  netid: '',
  studentClass: null,
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
  studentClass: null,
  studentGrade: null,
});
const isChangeDetailDisabled = ref(true);
const fileInput = ref(null);
const fileName = ref('未选择');

function triggerUploadFile() {
  fileInput.value.click();
}

function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;
  fileName.value = file.name;
  const formData = new FormData();
  formData.append("file", file);
  axios({
    url: "/upload",
    method: "POST",
    headers: {
      Accept: "*/*",
      "Authorization": `Bearer ${localStorage.getItem("token")}`,
      "Content-Type": "multipart/form-data", // 确保使用 multipart/form-data
    },
    data: formData,
  }).then((res) => {
    if (res.status === 200 && res.data) {
      // 假设响应返回文件的访问链接
      userDetailTemp.value.photoUrl = res.data;
      fileName.value += '-上传成功';
      alert("文件上传成功！");
    } else {
      fileName.value += '-上传失败';
      alert("文件上传失败！");
    }
  }).catch((err) => {
    fileName.value += '-上传失败';
    alert("文件上传失败！");
    console.error(err);
  });
}

function changeDetailDisabled(){
  isChangeDetailDisabled.value = !isChangeDetailDisabled.value;
  if (isChangeDetailDisabled.value) {
    window.location.reload();
  }
}

function changeDetailChecked(target: string){
  if (target === 'STUDENT'){
    httpStudent({
      url: '/update',
      method: 'PUT',
      headers: {
        Accept: '*/*',
        Authorization: `Bearer ${localStorage.getItem("token")}`,
        'Content-Type': 'application/json'
      },
      data: userDetailTemp.value
    }).then(res => {
      if (res.data.code === 200){
        alert('修改成功！');
        window.location.reload();
      } else {
        alert('修改失败！');
        console.log(res);
      }
    }).catch(err => {
      alert('修改失败！');
      console.error(err);
    })
  } else if (target === 'TEACHER'){
    httpTeacher({
      url: '/update',
      method: 'PUT',
      headers: {
        Accept: '*/*',
        Authorization: `Bearer ${localStorage.getItem("token")}`,
        'Content-Type': 'application/json'
      },
      data: userDetailTemp.value
    }).then(res => {
      if (res.data.code === 200){
        alert('修改成功！');
        window.location.reload();
      } else {
        alert('修改失败！');
        console.log(res);
      }
    }).catch(err => {
      alert('修改失败！');
      console.error(err);
    })
  }
}

onMounted(() => {
  if (userInfoStore.userInfo) getUserDetail(userInfoStore.userInfo.role);

})

function getUserDetail(targetInfo) {
  if (targetInfo.role === 'TEACHER') {
    httpTeacher({
      url: '/my-detail',
      method: 'GET',
      headers: {
        Accept: '*/*',
        Authorization: 'Bearer ' + localStorage.getItem('token')
      }
    }).then(res => {
      console.log(res);
      if (res.data.code === 200) {
        userDetail.value = res.data.data;
      } else {
        if (targetInfo){
          userDetail.value.uid = targetInfo.uid;
          userDetail.value.netid = targetInfo.username;
        }
      }
      userDetailTemp.value = userDetail.value;
      console.log(userDetailTemp.value);
    }).catch(err => {
      alert(JSON.parse(err.requests.responseText).data.error);
      console.error(err);
    })
  } else if (targetInfo.role === 'STUDENT') {
    httpStudent({
      url: '/my-detail',
      method: 'GET',
      headers: {
        Accept: '*/*',
        Authorization: 'Bearer ' + localStorage.getItem('token')
      }
    }).then(res => {
      if (res.data.code === 200) {
        userDetail.value = res.data.data;
      } else {
        if (targetInfo){
          userDetail.value.uid = targetInfo.uid;
          userDetail.value.netid = targetInfo.username;
        }
      }
      userDetailTemp.value = userDetail.value;
    }).catch(err => {
      alert(JSON.parse(err.requests.responseText).data.error);
    })
  }
}

watch(() => userInfoStore.userInfo, (newValue) => {
  userRole.value = newValue.role;
  getUserDetail(newValue);
})
</script>

<template>
  <div class="my_detail_box">
    <el-form label-width="auto" style="max-width: 600px; margin: 0 auto" v-if="userRole === 'TEACHER'">
      <h3>个人详细信息</h3>
      <el-form-item label="照片：">
        <div class="photo_box">
          <img :src="userDetailTemp.photoUrl" alt="photo"/>
        </div>
        <input type="file" style="display: none" ref="fileInput" @change="handleFileChange"/>
        <button type="button" class="button" @click="triggerUploadFile" :disabled="isChangeDetailDisabled">选择图片</button>
        <p>&nbsp;&nbsp;&nbsp;{{fileName}}&nbsp;&nbsp;&nbsp;</p>
      </el-form-item>
      <el-form-item label="职称：">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.teacherPosition"/>
      </el-form-item>
      <el-form-item label="研究方向：">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.researchDirection"/>
      </el-form-item>
      <el-form-item label="专业方向：">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.professionalDirection"/>
      </el-form-item>
      <el-form-item label="简介：">
        <el-input type="textarea" :rows="4" :disabled="isChangeDetailDisabled" v-model="userDetailTemp.resume"/>
      </el-form-item>
      <button class="button" type="button" @click="changeDetailDisabled">{{isChangeDetailDisabled? '开启修改': '关闭修改'}}</button>
      <button class="button" type="button" @click="changeDetailChecked('TEACHER')" :disabled="isChangeDetailDisabled">确认修改</button>
    </el-form>
    <el-form label-width="auto" style="max-width: 600px; margin: 0 auto" v-if="userRole === 'STUDENT'">
      <h3>个人详细信息</h3>
      <el-form-item label="照片：">
        <div class="photo_box">
          <img :src="userDetailTemp.photoUrl" alt="photo"/>
        </div>
        <input type="file" style="display: none" ref="fileInput" @change="handleFileChange"/>
        <button type="button" class="button" @click="triggerUploadFile" :disabled="isChangeDetailDisabled">选择图片</button>
        <p>&nbsp;&nbsp;&nbsp;{{fileName}}&nbsp;&nbsp;&nbsp;</p>
      </el-form-item>
      <el-form-item label="年级：">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.studentGrade"/>
      </el-form-item>
      <el-form-item label="班级：">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.studentClass"/>
      </el-form-item>
      <el-form-item label="意向研究方向：">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.researchDirection"/>
      </el-form-item>
      <el-form-item label="专业：">
        <el-input :disabled="isChangeDetailDisabled" v-model="userDetailTemp.professionalDirection"/>
      </el-form-item>
      <el-form-item label="简介：">
        <el-input type="textarea" :rows="4" :disabled="isChangeDetailDisabled" v-model="userDetailTemp.resume"/>
      </el-form-item>
      <button class="button" type="button" @click="changeDetailDisabled">{{isChangeDetailDisabled? '开启修改': '关闭修改'}}</button>
      <button class="button" type="button" @click="changeDetailChecked('STUDENT')" :disabled="isChangeDetailDisabled">确认修改</button>
    </el-form>
  </div>
</template>

<style scoped lang="sass">
.my_detail_box
  box-shadow: #005826 0 0 5px
  border-radius: 10px
  width: 100%
  margin: 10px auto
  text-align: center
  .photo_box
    width: 75px
    height: 105px
    overflow: hidden
    img
      width: 100%
  h3
    color: #005826
    font-weight: bold
  .button
    border-radius: 5px
    width: 100px
    height: 32px
    background-color: #005826
    color: white
    margin-left: 20px
    border: none
    font-size: 15px
    transition: .3s ease
  .button:hover
    background-color: #0f7e3f
  .button:disabled
    background-color: #55655b
</style>