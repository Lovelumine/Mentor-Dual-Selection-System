<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {http} from "@/utils/http";
const userInfoStore = useUserInfoStore();
import type {RepwdImpl} from "@/interfaces/RepwdImpl";

const pwdForm = ref<RepwdImpl>({
  username: null,
  oldPassword: null,
  newPassword: null,
  confirmPassword: null,
})

function resettingClicked() {
  window.location.reload();
}

function uploadClicked () {
  if (!pwdForm.value.username) {
    alert('未知账号！可能是网络问题！');
    return;
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword){
    alert('两次新密码输入不相同！');
    return;
  }
  http({
    url: '/auth/reset-password',
    method: 'POST',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`
    },
    data: pwdForm.value
  }).then(res => {
    console.log(res);
    if (res.data?.code === 200){
      localStorage.setItem('token', res.data.data?.token || '');
      alert('您的密码已更新！无需重新登录！');
    } else {
      alert('密码修改失败！');
    }
  }).catch(err => {
    if (err.request.status === 401) {
      alert(JSON.parse(err.request.response).data);
    } else {
      alert('密码修改失败！');
    }
    console.error(err);
  })
}

onMounted(() => {
  if (userInfoStore.userInfo) pwdForm.value.username = userInfoStore.userInfo.username || '';
})

watch(() => userInfoStore.userInfo, (newValue) => {
  if (newValue) pwdForm.value.username = newValue.username || '';
},
    { immediate: true })
</script>

<template>
  <div class="title">
    <span>修改密码（{{pwdForm.username}}）</span>
  </div>
  <div class="container">
    <el-form :model="pwdForm" label-width="auto" style="max-width: 400px" @submit.prevent="uploadClicked">
      <el-form-item label="原密码：">
        <el-input v-model="pwdForm.oldPassword" required/>
      </el-form-item>
      <el-form-item label="新密码：">
        <el-input v-model="pwdForm.newPassword" required/>
      </el-form-item>
      <el-form-item label="确认密码：">
        <el-input v-model="pwdForm.confirmPassword" required/>
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