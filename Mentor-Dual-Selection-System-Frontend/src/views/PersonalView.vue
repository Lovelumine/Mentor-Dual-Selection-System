<script setup lang="ts">
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {onMounted, ref, watch} from "vue";
const userStore = useUserInfoStore();
import {useRouter} from "vue-router";
import PersonalTitleComp from "@/components/Personal/PersonalTitleComp.vue";
import MyDetailComp from "@/components/Personal/MyDetailComp.vue";
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

function startChangeInfoClicked () {
  isStartChangeInfo.value = !isStartChangeInfo.value;
}
function changeInfoClicked () {
  console.log(userInfoChange.value);
  startChangeInfoClicked();
  window.location.reload();
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
  userInfoComp.value = {...newValue};
  userInfoChange.value = {...newValue};
  console.log(userInfoComp.value);
})
</script>

<template>
  <PersonalTitleComp/>
  <div class="personal_box">
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
        li
          list-style: none
          margin-top: 20px
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