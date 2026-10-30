<script setup lang="ts">
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { onMounted, ref, watch } from "vue";
const userStore = useUserInfoStore();
import { useRouter } from "vue-router";
import PersonalTitleComp from "@/components/Personal/PersonalTitleComp.vue";
import MyDetailComp from "@/components/Personal/MyDetailComp.vue";
import axios from "axios";
import { http } from "@/utils/http";
import { getCardBackgroundImageUrl } from "@/utils/media";
const router = useRouter();
import { useUploadFileStore } from "@/stores/UploadFileStore";
import type { UserInfo } from "@/interfaces/UserInfoImpl";
const uploadFileStore = useUploadFileStore();

const userInfoComp = ref<UserInfo>({
  fullName: '',
  email: '',
  username: '',
  role: '',
  avatarUrl: '',
  uid: -1
});
const userInfoChange = ref<UserInfo>({
  fullName: '',
  email: '',
  username: '',
  role: '',
  avatarUrl: '',
  uid: -1
});
const isStartChangeInfo = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

function handleFileChange(event: Event) {
  const inputElement = event.target as HTMLInputElement | null;
  let file: any;
  if (inputElement && inputElement.files) {
    file = inputElement.files[0];
  }
  if (!file) return;
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
  }).then((res) => {
    if (res.status === 200 && res.data) {
      userInfoChange.value.avatarUrl = res.data;
      userInfoComp.value.avatarUrl = res.data;
      alert("文件上传成功！");
      uploadFileStore.changeIsLoading(false);
    } else {
      alert("文件上传失败！");
      uploadFileStore.changeIsLoading(false);
    }
  }).catch((err) => {
    alert("文件上传失败！");
    console.error(err);
    uploadFileStore.changeIsLoading(false);
  });
}

function triggerUploadFile() {
  if (fileInput.value) fileInput.value.click();
}

function startChangeInfoClicked() {
  isStartChangeInfo.value = !isStartChangeInfo.value;
}

function changeInfoClicked() {
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
  });
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
});
watch(() => userStore.userInfo, (newValue) => {
  if (newValue) {
    console.log(newValue);
    userInfoComp.value = newValue;
    userInfoChange.value = newValue;
  }
});

const getProfileCardBackgroundUrl = (url?: string | null) => getCardBackgroundImageUrl(url);
</script>

<template>
  <div class="container">
    <PersonalTitleComp />

    <div class="personal_box">
      <div class="user_info_box">
        <!-- 卡片背景图仅展示给教师角色 -->
        <div class="avatar_box" v-if="userInfoComp?.role === 'TEACHER'">
          <img
            v-if="getProfileCardBackgroundUrl(userInfoComp?.avatarUrl)"
            class="avatar"
            :src="getProfileCardBackgroundUrl(userInfoComp?.avatarUrl)"
            alt="卡片背景图"
          />
          <div v-else class="avatar_placeholder">未设置卡片背景</div>
        </div>
        <ul>
          <li>姓名：{{ userInfoComp?.fullName }}</li>
          <li v-if="userInfoComp?.role === 'TEACHER'">
            工号：{{ userInfoComp?.username }}
          </li>
          <li v-else-if="userInfoComp?.role === 'STUDENT'">
            学号：{{ userInfoComp?.username }}
          </li>
          <li>电子邮箱：{{ userInfoComp?.email }}</li>
          <li>角色：{{ userInfoComp?.role === 'TEACHER' ? '教师' : (userInfoComp?.role === 'STUDENT' ? '学生' : '管理员') }}</li>
        </ul>
        <button class="button" @click="startChangeInfoClicked">{{ isStartChangeInfo ? '取消修改' : '修改信息' }}</button>
      </div>

      <div class="change_info_box" v-if="isStartChangeInfo">
        <form @submit.prevent="changeInfoClicked">
          <ul>
            <li>
              姓名：<input type="text" required placeholder="请输入姓名" v-model="userInfoChange.fullName" />
            </li>
            <li v-if="userInfoComp?.role === 'TEACHER'">
              工号：<input type="text" required placeholder="请输入工号" v-model="userInfoChange.username" />
            </li>
            <li v-else-if="userInfoComp?.role === 'STUDENT'">
              学号：<input type="text" required placeholder="请输入学号" v-model="userInfoChange.username" />
            </li>
            <li>
              电子邮箱：<input type="text" required placeholder="请输入邮箱" v-model="userInfoChange.email" />
            </li>
            <!-- 仅教师角色可设置卡片背景图 -->
            <li class="avatar_list" v-if="userInfoComp?.role === 'TEACHER'">
              卡片背景图：
              <div class="avatar_box">
                <img
                  v-if="getProfileCardBackgroundUrl(userInfoComp?.avatarUrl)"
                  :src="getProfileCardBackgroundUrl(userInfoComp?.avatarUrl)"
                  alt="卡片背景图"
                />
                <div v-else class="avatar_placeholder">未设置卡片背景</div>
              </div>
              <input style="display: none" type="file" @change="handleFileChange" ref="fileInput" :accept="'.jpg,.jpeg,.png'" />
              <button type="button" @click="triggerUploadFile">选择图片</button>
            </li>
          </ul>
          <button class="button" type="submit">确认修改</button>
        </form>
      </div>
      <MyDetailComp />
    </div>
  </div>
</template>

<style scoped lang="sass">
.personal_box
  width: min(1080px, calc(100% - 32px))
  margin: 0 auto 32px auto
  color: var(--app-text-primary)

.page-title
  text-align: center
  font-size: 24px
  color: var(--app-brand)
  margin-bottom: 20px
  font-weight: bold

.user_info_box
  box-shadow: 0 20px 40px var(--app-shadow)
  border-radius: 18px
  border: 1px solid var(--app-border)
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  width: 100%
  padding: 28px
  margin: 20px auto
  display: flex
  align-items: center
  gap: 28px
  flex-wrap: wrap
  .avatar_box
    width: 200px   // 设置图片框宽度
    height: 100px  // 设置图片框高度，宽高比为2:1
    overflow: hidden
    border-radius: 10px  // 设置为圆角
    border: 1px solid var(--app-border)
    .avatar
      width: 100%  // 确保图片适应框宽度
      height: 100%
      object-fit: cover
    .avatar_placeholder
      width: 100%
      height: 100%
      display: flex
      align-items: center
      justify-content: center
      font-size: 13px
      color: var(--app-text-tertiary)
      background: linear-gradient(135deg, color-mix(in srgb, var(--app-surface) 84%, #dce8f3 16%) 0%, color-mix(in srgb, var(--app-surface-elevated) 88%, #d1dfef 12%) 100%)
  ul
    padding: 20px 0 20px 0
    margin: 0
    flex: 1 1 320px
    li
      margin-top: 10px
      list-style: none
      font-weight: normal
      color: var(--app-text-secondary)
    li:first-child
      margin: 0
      color: var(--app-text-primary)
.change_info_box
  padding: 24px 0
  box-shadow: 0 20px 40px var(--app-shadow)
  border-radius: 18px
  border: 1px solid var(--app-border)
  background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  width: 100%
  margin: 30px auto 0 auto
  form
    margin: 0 24px
    ul
      padding: 0
      .avatar_list
        display: flex
        button
          margin: auto auto auto 5%
          background-color: var(--app-brand)
          border: 1px solid var(--app-brand)
          color: var(--app-text-inverse)
          width: 120px
          height: 36px
          border-radius: 10px
          font-size: 15px
          transition: .3s ease
        button:hover
          cursor: pointer
          background-color: var(--app-brand-hover)
          border: 1px solid var(--app-brand-hover)
      li
        list-style: none
        margin-top: 20px
        color: var(--app-text-primary)
        .avatar_box
          width: 200px  // 调整为宽高比2:1
          height: 100px
          overflow: hidden
          border: 1px solid var(--app-border)
          border-radius: 10px
          img
            width: 100%  // 确保图片适应框宽度
            height: 100%
            object-fit: cover
          .avatar_placeholder
            width: 100%
            height: 100%
            display: flex
            align-items: center
            justify-content: center
            font-size: 13px
            color: var(--app-text-tertiary)
            background: linear-gradient(135deg, color-mix(in srgb, var(--app-surface) 84%, #dce8f3 16%) 0%, color-mix(in srgb, var(--app-surface-elevated) 88%, #d1dfef 12%) 100%)
        input
          width: 300px
          height: 36px
          transition: .3s ease
          border: 1px solid var(--app-border)
          border-radius: 10px
          background-color: var(--app-surface)
          color: var(--app-text-primary)
        input:hover
          border: 1px solid var(--app-brand)
        input:focus
          outline: none
          box-shadow: 0 0 0 3px var(--app-brand-ring)
          border: 1px var(--app-brand) solid
        input::placeholder
          color: var(--app-text-secondary)
      li:first-child
        margin: 0 auto
      li:last-child
        margin: 20px auto
.button
  margin: auto 0 auto auto
  background-color: var(--app-brand)
  border: 1px solid var(--app-brand)
  color: var(--app-text-inverse)
  width: 120px
  height: 36px
  border-radius: 10px
  font-size: 16px
  transition: .3s ease
.button:hover
  cursor: pointer
  background-color: var(--app-brand-hover)
  border: 1px solid var(--app-brand-hover)

.container
  box-sizing: border-box       // 确保内边距不会影响容器的尺寸
  min-height: calc(100vh - 60px)
  background: transparent
  padding-top: 18px

@media (max-width: 1023px)
  .personal_box
    width: min(100%, calc(100% - 24px))

  .user_info_box
    padding: 24px 20px

  .change_info_box
    form
      margin: 0 20px

@media (max-width: 767px)
  .personal_box
    width: min(100%, calc(100% - 16px))

  .user_info_box
    gap: 18px
    padding: 20px 16px
    justify-content: center
    .avatar_box
      width: min(100%, 280px)
      margin: 0 auto
    ul
      flex-basis: 100%
    .button
      width: 100%
      margin: 0

  .change_info_box
    padding: 20px 0
    form
      margin: 0 16px
      ul .avatar_list
        display: flex
        flex-direction: column
        align-items: flex-start
        gap: 12px
        button
          margin: 0
      ul li input
        width: min(100%, 320px)

  .button
    width: 100%
    margin-top: 10px
</style>
