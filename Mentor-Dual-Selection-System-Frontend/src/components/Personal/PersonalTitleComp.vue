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
  min-height: 60px
  background-color: var(--app-surface-muted)
  font-size: 20px
  margin-bottom: 20px
  padding: 8px 16px
  box-sizing: border-box
  display: flex
  align-items: center
  justify-content: center
  position: sticky
  top: 60px
  z-index: 100
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
    left: 16px
    top: 50%
    transform: translateY(-50%)
    transition: .5s ease background-color
    .user_icon
      position: absolute
      left: 50%
      top: 50%
      transform: translate(-50%, -50%)
      color: var(--app-brand)
  h3
    font-weight: bold
    color: var(--app-text-primary)
    margin: 0

@media (max-width: 767px)
  .title
    min-height: 56px
    padding: 6px 12px
    font-size: 18px
    .user_icon_box
      width: 42px
      height: 42px
      left: 12px
</style>
