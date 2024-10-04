<script setup lang="ts">
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {onMounted, ref, watch} from "vue";
const userStore = useUserInfoStore();
import {useRouter} from "vue-router";
import PersonalTitleComp from "@/components/Personal/PersonalTitleComp.vue";
import MyDetailComp from "@/components/Personal/MyDetailComp.vue";
import axios from "axios";
import {http} from "@/utils/http";
const router = useRouter();

const userInfoComp = ref({
  fullName: '',
  email: '',
  username: '',
  role: '',
  avatarUrl: ''
});
const userInfoChange = ref({...userInfoComp.value});
const isStartChangeInfo = ref(false);
const fileInput = ref(null);

function handleFileChange(event) {
  const file = event.target.files[0];
  if (!file) return;
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
      userInfoChange.value.avatarUrl = res.data;
      userInfoComp.value.avatarUrl = res.data;
      alert("文件上传成功！");
    } else {
      alert("文件上传失败！");
    }
  }).catch((err) => {
    alert("文件上传失败！");
    console.error(err);
  });
}

function triggerUploadFile() {
  fileInput.value.click();
}

function startChangeInfoClicked () {
  isStartChangeInfo.value = !isStartChangeInfo.value;
}
function changeInfoClicked () {
  console.log(userInfoChange.value);
  startChangeInfoClicked();
  http({
    url: '/user/update',
    method: "POST",
    headers: {
      Accept: "*/*",
      "Authorization": `Bearer ${localStorage.getItem("token")}`,
    },
    data: userInfoChange.value
  }).then(res => {
    console.log(res);
    if (res.data.code === 200) {
      alert('更新成功！');
      window.location.reload();
    } else alert('更新失败！');
  }).catch((err) => {
    if (err.request.status === 400) {
      alert(JSON.parse(err.request.responseText).data.error);
      window.location.reload();
    } else {
      alert(JSON.parse(err.request.responseText).data.error);
    }
  })
}

onMounted(() => {
  if (localStorage.getItem("token")) {
    userStore.fetchUserInfo();
  } else {
    router.push('/');
  }
  if (userStore.userInfo) {
    userInfoComp.value = userStore.userInfo;
    userInfoChange.value = userStore.userInfo;
  }
})
watch(() => userStore.userInfo, (newValue) => {
  userInfoComp.value = newValue;
  userInfoChange.value = newValue;
  console.log(userInfoComp.value);
})
</script>

<template>
  <PersonalTitleComp/>
  <div class="personal_box">
    <div class="user_info_box">
      <div class="avatar_box">
        <img class="avatar" :src="userInfoComp.avatarUrl" alt="avatar"/>
      </div>
      <ul>
        <li>
          姓名：{{userInfoComp.fullName}}
        </li>
        <li>
          工号/学号：{{userInfoComp.username}}
        </li>
        <li>
          电子邮箱：{{userInfoComp.email}}
        </li>
        <li>
          权限：{{userInfoComp.role}}
        </li>
      </ul>
      <button class="button" @click="startChangeInfoClicked">{{ isStartChangeInfo? '取消修改': '修改资料' }}</button>
    </div>
    <div class="change_info_box" v-if="isStartChangeInfo">
      <form @submit.prevent="changeInfoClicked">
        <ul>
          <li>
            姓名信息：<input type="text" required placeholder="请输入" v-model="userInfoChange.fullName" />
          </li>
          <li>
            工号学号：<input type="text" required placeholder="请输入" v-model="userInfoChange.username" />
          </li>
          <li>
            电子邮箱：<input type="text" required placeholder="请输入" v-model="userInfoChange.email" />
          </li>
          <li class="avatar_list">
            用户头像：<div class="avatar_box">
            <img :src="userInfoComp.avatarUrl" alt="avatar"/>
          </div>
            <input style="display: none" type="file" @change="handleFileChange" ref="fileInput" :accept="['.jpg', '.jpeg', '.png']"/>
            <button type="button" @click="triggerUploadFile">选择图片</button>
          </li>
        </ul>
        <button class="button" type="submit">确认修改</button>
      </form>
    </div>
    <MyDetailComp/>
  </div>
</template>

<style scoped lang="sass">

.personal_box
  width: 80%
  margin: 0 auto
  .user_info_box
    box-shadow: #005826 0 0 5px
    border-radius: 10px
    width: 100%
    margin: 0 auto
    display: flex
    align-items: center
    .avatar_box
      width: 64px
      height: 64px
      overflow: hidden
      margin-left: 50px
      border-radius: 50%
      .avatar
        width: 100%
    ul
      padding: 20px 0 20px 0
      margin-left: 50px
      li
        font-size: 16px
        margin-top: 10px
        list-style: none
      li:first-child
        margin: 0
  .change_info_box
    padding: 20px 0
    box-shadow: #005826 0 0 5px
    border-radius: 10px
    width: 100%
    margin: 30px auto 0 auto
    form
      margin-left: 50px
      ul
        padding: 0
        .avatar_list
          display: flex
          button
            margin: auto auto auto 5%
            background-color: #005826
            border: 1px solid #005826
            color: white
            width: 100px
            height: 32px
            border-radius: 5px
            font-size: 15px
            transition: .3s ease
          button:hover
            cursor: pointer
            background-color: #0f7e3f
            border: 1px solid #0f7e3f
        li
          list-style: none
          margin-top: 20px
          .avatar_box
            width: 64px
            height: 64px
            overflow: hidden
            img
              width: 100%
          input
            width: 220px
            height: 32px
            transition: .3s ease
            border: 1px solid #989898
            border-radius: 5px
          input:hover
            border: 1px solid #005826
          input:focus
            outline: none
            box-shadow: #005826 0 0 5px
            border: 1px #005826 solid
        li:first-child
          margin: 0 auto
        li:last-child
          margin: 20px auto
.button
  margin: auto 5% auto auto
  background-color: #005826
  border: 1px solid #005826
  color: white
  width: 100px
  height: 32px
  border-radius: 5px
  font-size: 15px
  transition: .3s ease
.button:hover
  cursor: pointer
  background-color: #0f7e3f
  border: 1px solid #0f7e3f
</style>