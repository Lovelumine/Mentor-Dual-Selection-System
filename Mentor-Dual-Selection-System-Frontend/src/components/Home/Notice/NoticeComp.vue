<script setup lang="ts">

import NoticeCardComp from "@/components/Home/Notice/NoticeCardComp.vue";
</script>

<template>
  <div class="title">
    <span>通知公告（发送给全体导师和学生）</span>
  </div>
  <div class="container">
    <NoticeCardComp/>
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
  padding-top: 30px
  width: 100%
  min-height: calc(100vh - 120px)
  background-color: #d5ddda
</style>