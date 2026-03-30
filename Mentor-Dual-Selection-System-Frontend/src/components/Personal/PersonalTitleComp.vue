<script setup lang="ts">

import {Back} from "@element-plus/icons-vue";
import {useRouter} from "vue-router";
const router = useRouter();

function backClicked(){
  router.back();
}
</script>

<template>
  <div class="title">
    <a title="返回上一级" @click="backClicked">
      <div class="user_icon_box">
        <el-icon class="user_icon" size="24"><Back/></el-icon>
      </div>
    </a>
    <h3>个人中心</h3>
  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: var(--app-surface-muted)
  line-height: 60px
  font-size: 20px
  margin-bottom: 20px
  position: fixed
  top: 60px
  left: 0
  z-index: 1000
  color: var(--app-text-primary)
  border-bottom: 1px solid var(--app-border)
  box-shadow: 0 12px 24px var(--app-shadow)
  a
    cursor: pointer
  .user_icon_box:hover
    background-color: var(--app-brand-ring)
  .user_icon_box
    border-radius: 10px
    border: 1px solid var(--app-brand)
    width: 48px
    height: 48px
    background-color: color-mix(in srgb, var(--app-brand-soft) 72%, transparent)
    position: absolute
    top: 50%
    left: 1%
    transform: translate(0, -50%)
    transition: .5s ease background-color
    .user_icon
      position: absolute
      left: 50%
      top: 50%
      transform: translate(-50%, -50%)
      color: var(--app-brand)
  h3
    position: absolute
    left: 50%
    transform: translateX(-50%)
    font-weight: bold
    color: var(--app-text-primary)
</style>
