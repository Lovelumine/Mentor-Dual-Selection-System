<script setup lang="ts">
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();
import {useRouter} from "vue-router";
import {onMounted, ref, watch} from "vue";
const router = useRouter();

const isFuncShow = ref(false);

function releaseNoticeClicked(){
  router.push('/notice/release');
}

onMounted(() => {
  if (userInfoStore.userInfo) isFuncShow.value = userInfoStore.userInfo.role === 'ADMIN';
})

watch(() => userInfoStore.userInfo, (newVal) => {
  isFuncShow.value = newVal.role === 'ADMIN';
})
</script>

<template>
  <div class="title">
    <span>通知公告（发送给全体导师和学生）</span>
    <button v-if="isFuncShow" @click="releaseNoticeClicked" class="button">发布公告</button>
  </div>
  <div class="container">
    <RouterView/>
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
  display: flex
  align-items: center
  .button
    margin: auto 32px auto auto
    padding: 0 20px
    height: 32px
    width: 120px
    font-size: 14px
    border-radius: 5px
    border: none
    color: white
    background-color: #005826
    transition: .3s ease
  button:hover
    background-color: #0f7e3f
.container
  padding-top: 30px
  width: 100%
  min-height: calc(100vh - 120px)
  background-color: #d5ddda
</style>