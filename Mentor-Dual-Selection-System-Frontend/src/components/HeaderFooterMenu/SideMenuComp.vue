<script setup lang="ts">

import {Avatar, BellFilled, UserFilled,} from "@element-plus/icons-vue";
import {ref} from "vue";
import {useRouter} from "vue-router";
const router = useRouter();

const sidemenuDet = ref(
    {bacc: '#003c1a', atcolor: '#ffc832', baccsub: '#002912'},
);

function sidemenuClicked(target: number, subTarget: string = null){
  console.log(target, subTarget);
  switch (target) {
    case 1: {
      console.log('第一菜单');
      switch (subTarget) {
        case '1-1': router.push('/student_list'); break;
        case '1-2': console.log('第二按钮'); break;
        case '1-3': console.log('第三按钮'); break;
        case '1-4': console.log('第四按钮'); break;
      }
      break;
    } case 2: router.push('/teacher_list'); break;
    case 3: {
      console.log('第三菜单');
      switch (subTarget) {
        case '3-1': console.log('第一按钮'); break;
        case '3-2': console.log('第二按钮'); break;
        case '3-3': console.log('第三按钮'); break;
        case '3-4': console.log('第四按钮'); break;
      }
      break;
    } case 4: router.push('/notice'); break;
  }
}

// const handleOpen = (key: string, keyPath: string[]) => {
//   console.log(key, keyPath)
// }
// const handleClose = (key: string, keyPath: string[]) => {
//   console.log(key, keyPath)
// }
</script>

<template>
  <div class="sidemenu_box">
    <el-row class="tac">
      <el-col >
        <el-menu
            :active-text-color="sidemenuDet.atcolor"
            :background-color="sidemenuDet.bacc"
            class="el-menu-vertical-demo"
            text-color="#fff"
        >
          <el-sub-menu index="1">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>学生列表</span>
            </template>
            <el-menu-item-group title="学年" class="sub_menu">
              <el-menu-item index="1-1" @click="sidemenuClicked(1, '1-1')">大一</el-menu-item>
              <el-menu-item index="1-2" @click="sidemenuClicked(1, '1-2')">大二</el-menu-item>
              <el-menu-item index="1-3" @click="sidemenuClicked(1, '1-3')">大三</el-menu-item>
              <el-menu-item index="1-4" @click="sidemenuClicked(1, '1-4')">大四</el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
          <el-menu-item index="2" @click="sidemenuClicked(2)">
            <el-icon><Avatar /></el-icon>
            <span>导师列表</span>
          </el-menu-item>
          <el-sub-menu index="3">
            <template #title>
              <el-icon><Menu /></el-icon>
              <span>状态管理</span>
            </template>
            <el-menu-item-group title="学年" class="sub_menu">
              <el-menu-item index="3-1" @click="sidemenuClicked(3, '3-1')">大一</el-menu-item>
              <el-menu-item index="3-2" @click="sidemenuClicked(3, '3-2')">大二</el-menu-item>
              <el-menu-item index="3-3" @click="sidemenuClicked(3, '3-3')">大三</el-menu-item>
              <el-menu-item index="3-4" @click="sidemenuClicked(3, '3-4')">大四</el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
          <el-menu-item index="4" @click="sidemenuClicked(4)">
            <el-icon><BellFilled /></el-icon>
            <span>通知公告</span>
          </el-menu-item>
        </el-menu>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="sass">
.sidemenu_box
  overflow: clip
  .el-menu-vertical-demo
    min-height: calc(100vh - 60px)
    width: 200px
    .sub_menu
      background-color: #002912
</style>