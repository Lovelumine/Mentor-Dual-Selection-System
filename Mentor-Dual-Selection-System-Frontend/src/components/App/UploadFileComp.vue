<script setup lang="ts">

import {Loading} from "@element-plus/icons-vue";
</script>

<template>
  <div class="cover_box">
    <img class="logo" src="@/assets/zhuzhanLOGOgreen.png" alt="logo"/>
    <div class="icon_text_box">
      <el-icon size="128" color="#005826"><Loading /></el-icon>
      <div class="icon_text_box">
        <span>正在上传文件中…</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.cover_box
  opacity: .9
  width: 100vw
  height: 100vh
  z-index: 9999999
  background-color: #e5efe8
  position: fixed
  text-align: center
  .logo
    position: absolute
    opacity: .1
    width: 700px
    bottom: -8%
    right: -1%
  .icon_text_box
    width: 50%
    margin: 100px auto
    button
      padding: 0 20px
      height: 32px
      width: 160px
      font-size: 14px
      border-radius: 5px
      border: none
      color: white
      background-color: #005826
      transition: .3s ease
    button:hover
      background-color: #0f7e3f
    button:disabled
      background-color: #55655b
  span:first-child
    font-weight: bold
    font-size: 24px
  span
    font-size: 18px
</style>