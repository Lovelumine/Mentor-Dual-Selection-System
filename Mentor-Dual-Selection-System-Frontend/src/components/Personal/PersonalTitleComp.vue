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
        <el-icon class="user_icon" size="24" color="#005826"><Back/></el-icon>
      </div>
    </a>
    <h3>个人中心</h3>
  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  font-size: 20px
  margin-bottom: 20px
  position: relative
  .user_icon_box:hover
    background-color: #00582690
  .user_icon_box
    border-radius: 4px
    border: 1px solid #005826
    width: 48px
    height: 48px
    background-color: #00582640
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
  h3
    position: absolute
    left: 50%
    transform: translateX(-50%)
    font-weight: bold
</style>