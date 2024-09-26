<script setup lang="ts">

import {Avatar, BellFilled, CircleClose, Connection, User, UserFilled,} from "@element-plus/icons-vue";
import {onMounted, ref, watch} from "vue";
import {useRouter} from "vue-router";
const router = useRouter();
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();
import {useStuListGrade} from "@/stores/StudentListGrade";
const stuListGrade = useStuListGrade();

const sidemenuDet = ref(
    {bacc: '#003c1a', atcolor: '#ffc832', baccsub: '#002912'},
);
const isSelectStudentShow = ref(false);
const isSelectTeacherShow = ref(false);
const isStudentListShow = ref(false);

onMounted(() => {
  if (userInfoStore.userInfo) handleIsShowFunction(userInfoStore.userInfo.role);

})

function studentListClicked(grade: number) {
  stuListGrade.changeGrade(grade);
  router.push(`/student_list${grade}`);
}

function handleIsShowFunction (target: string) {
  isSelectStudentShow.value = ['TEACHER', 'ADMIN'].includes(target);
  isStudentListShow.value = ['TEACHER', 'ADMIN'].includes(target);
  isSelectTeacherShow.value = target === 'STUDENT';

}
function SignoutClicked() {
  localStorage.removeItem("token");
  window.location.reload();
}

watch(() => userInfoStore.userInfo, (newValue) => {
  handleIsShowFunction(newValue.role);
})

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
          <el-sub-menu v-if="isStudentListShow" index="sl">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>学生列表</span>
            </template>
            <el-menu-item-group title="学年" class="sub_menu">
              <el-menu-item index="1-1" @click="studentListClicked(1);console.log(1)">大一</el-menu-item>
              <el-menu-item index="1-2" @click="studentListClicked(2);console.log(2)">大二</el-menu-item>
              <el-menu-item index="1-3" @click="studentListClicked(3);console.log(3)">大三</el-menu-item>
              <el-menu-item index="1-4" @click="studentListClicked(4);console.log(4)">大四</el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
          <el-menu-item @click="router.push('/teach_list')">
            <el-icon><Avatar /></el-icon>
            <span>导师列表</span>
          </el-menu-item>
          <el-menu-item @click="router.push('/select_student')" v-if="isSelectStudentShow">
            <el-icon><Avatar /></el-icon>
            <span>选择学生</span>
          </el-menu-item>
          <el-menu-item @click="router.push('/select_teacher')" v-if="isSelectTeacherShow">
            <el-icon><Avatar /></el-icon>
            <span>选择导师</span>
          </el-menu-item>
          <el-menu-item @click="router.push('/relations')">
            <el-icon><Connection /></el-icon>
            <span>师生关系</span>
          </el-menu-item>
<!--          <el-sub-menu index="stm">-->
<!--            <template #title>-->
<!--              <el-icon><Menu /></el-icon>-->
<!--              <span>状态管理</span>-->
<!--            </template>-->
<!--            <el-menu-item-group title="学年" class="sub_menu">-->
<!--              <el-menu-item index="3-1" @click="router.push('/student_list2');">大一</el-menu-item>-->
<!--              <el-menu-item index="3-2" @click="router.push('/student_list2');">大二</el-menu-item>-->
<!--              <el-menu-item index="3-3" @click="router.push('/student_list2');">大三</el-menu-item>-->
<!--              <el-menu-item index="3-4" @click="router.push('/student_list2');">大四</el-menu-item>-->
<!--            </el-menu-item-group>-->
<!--          </el-sub-menu>-->
          <el-menu-item @click="router.push('/notice');">
            <el-icon><BellFilled /></el-icon>
            <span>通知公告</span>
          </el-menu-item>
          <el-menu-item @click="router.push('/personal');">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
          <el-menu-item @click="SignoutClicked">
            <el-icon><CircleClose /></el-icon>
            <span>退出登录</span>
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