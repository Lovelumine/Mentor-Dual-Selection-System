<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();
import {useRouter} from "vue-router";
import {http} from "@/utils/http";
const router = useRouter();

const userRole = ref(null);
const uploadForm = ref({
  role: 'STUDENT',
  fullName: null,
  email: null,
  username: null,
  grade: null
})

function uploadClicked () {
  if (uploadForm.value.fullName === null || uploadForm.value.fullName === '') {
    alert('请填写姓名！');
  } else if (uploadForm.value.username === null || uploadForm.value.username === '') {
    alert('请填写学号！');
  } else if (uploadForm.value.grade === null || uploadForm.value.grade === '') {
    alert('请填写年级！');
  } else {
    http({
      url: '/user/update',
      method: 'POST',
      headers: {
        Accept: '*/*',
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      data: uploadForm.value
    }).then(res => {
      if (res.data.code === 200){
        alert('添加成功！');
        window.location.reload();
      } else {
        alert('添加失败！');
      }
    }).catch(err => {
      alert('添加失败！');
      alert(JSON.parse(err.request.responseText).data.error);
    })
  }
}

function resettingClicked () {
  window.location.reload();
}

onMounted(() => {
  if (userRole.value === 'STUDENT') router.push('/');
})

watch(() => userInfoStore.userInfo, (newVal) => {
  if (newVal.role === 'STUDENT') router.push('/');
})
</script>

<template>
  <div class="title">
    <span>手动添加单条学生信息</span>
  </div>
  <div class="container">
    <el-form label-width="auto" style="max-width: 400px" @submit.prevent="uploadClicked">
      <el-form-item label="姓名：">
        <el-input v-model="uploadForm.fullName"/>
      </el-form-item>
      <el-form-item label="学号：">
        <el-input v-model="uploadForm.username"/>
      </el-form-item>
      <el-form-item label="电子邮箱：">
        <el-input v-model="uploadForm.email"/>
      </el-form-item>
      <el-form-item label="学年：">
        <el-input v-model="uploadForm.grade" placeholder="例：2024"/>
      </el-form-item>
      <button class="button" type="submit">上传</button>
      <button class="button" @click="resettingClicked" type="button">重置</button>
    </el-form>
  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
.container
  width: 50%
  padding-top: 20px
  padding-left: 20px
  .button
    border-radius: 5px
    width: 100px
    height: 32px
    background-color: #005826
    color: white
    margin-left: 20px
    border: 1px solid #005826
    font-size: 15px
    transition: .3s ease
  .button:hover
    background-color: #0f7e3f
    border: 1px solid #0f7e3f
  .button:last-child
    background-color: white
    border: 1px solid #bd0000
    color: #bd0000
  .button:last-child:hover
    background-color: #bd0000
    color: white
</style>