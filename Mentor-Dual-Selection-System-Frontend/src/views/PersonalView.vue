<script setup lang="ts">
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {onMounted, ref, watch} from "vue";
const userStore = useUserInfoStore();
import {useRouter} from "vue-router";
import PersonalTitleComp from "@/components/Personal/PersonalTitleComp.vue";
const router = useRouter();

const userInfoComp = ref({
  fullName: '',
  email: '',
  username: '',
  role: '',
  avatarUrl: ''
});
const isStartChangeInfo = ref(false);

function startChangeInfoClicked () {
  isStartChangeInfo.value = !isStartChangeInfo.value;
}

onMounted(() => {
  if (localStorage.getItem("token")) {
    userStore.fetchUserInfo();
  } else {
    router.push('/');
  }
  if (userStore.userInfo) {
    userInfoComp.value = userStore.userInfo;
  }
})
watch(() => userStore.userInfo, (newValue) => {
  userInfoComp.value = {...newValue};
  console.log(userInfoComp.value);
})
</script>

<template>
  <div class="personal_box">
    <PersonalTitleComp/>
    <div class="user_info_box">
      <img class="avatar" :src="userInfoComp.avatarUrl" alt="avatar"/>
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
    <div class="change_info_box">

    </div>
  </div>
</template>

<style scoped lang="sass">

.personal_box
  .user_info_box
    box-shadow: #005826 0 0 5px
    border-radius: 10px
    width: 80%
    margin: 0 auto
    display: flex
    align-items: center
    .avatar
      width: 64px
      height: 64px
      border-radius: 50%
      margin-left: 50px
    ul
      padding: 20px 0 20px 0
      margin-left: 50px
      li
        font-size: 16px
        margin-top: 10px
        list-style: none
      li:first-child
        margin: 0
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