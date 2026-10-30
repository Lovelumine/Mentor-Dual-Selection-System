<script setup lang="ts">
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();
import {useRouter} from "vue-router";
import {computed} from "vue";
const router = useRouter();

const isFuncShow = computed(() => userInfoStore.userInfo.role === 'ADMIN');

function releaseNoticeClicked(){
  router.push('/notice/release');
}
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
  background-color: var(--app-surface-muted)
  line-height: 60px
  padding-left: 20px
  font-size: 20px
  display: flex
  align-items: center
  color: var(--app-text-primary)
  border-bottom: 1px solid var(--app-border)
  box-shadow: 0 12px 24px var(--app-shadow)
  .button
    margin: auto 32px auto auto
    padding: 0 20px
    height: 32px
    width: 120px
    font-size: 14px
    border-radius: 10px
    border: none
    color: var(--app-text-inverse)
    background-color: var(--app-brand)
    transition: .3s ease
  button:hover
    background-color: var(--app-brand-hover)
.container
  padding: 24px 0 32px
  width: 100%
  min-height: calc(100vh - 120px)
  background: linear-gradient(180deg, var(--app-page-bg-alt) 0%, var(--app-page-bg) 100%)

@media (max-width: 767px)
  .title
    height: auto
    min-height: 84px
    padding: 12px 16px
    box-sizing: border-box
    font-size: 18px
    line-height: 1.45
    gap: 12px

    span
      flex: 1 1 auto
      min-width: 0
      overflow-wrap: anywhere

    .button
      flex: 0 0 auto
      width: auto
      min-height: 40px
      margin: 0
      padding: 0 16px

  .container
    min-height: auto
    padding-top: 16px
</style>
