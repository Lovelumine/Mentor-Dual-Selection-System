<script setup lang="ts">
import { Avatar, BellFilled, CircleClose, Connection, Lock, Position, User, UserFilled } from "@element-plus/icons-vue";
import { onMounted, ref, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
const router = useRouter();
const route = useRoute();
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();
import SideFooterComp from "@/components/HeaderFooterMenu/SideFooterComp.vue";
import { getStudentListMenuItems } from "@/utils/studentCohort";

const isSelectStudentShow = ref(false);
const isSelectTeacherShow = ref(false);
const isStudentListShow = ref(false);
const isTeacherResumeShow = ref(false);
const studentListMenuItems = getStudentListMenuItems();


// 用于高亮选中的菜单项
const activeMenu = ref(route.path);

onMounted(() => {
  if (userInfoStore.userInfo) handleIsShowFunction(userInfoStore.userInfo.role);
  activeMenu.value = route.path; // 初始化当前高亮菜单项

});

function studentListClicked(path: string) {
  activeMenu.value = path;
  router.push(path);
}

function handleIsShowFunction(target: string | null) {
  if (target) {
    isSelectStudentShow.value = ['TEACHER', 'ADMIN'].includes(target);
    isStudentListShow.value = ['TEACHER', 'ADMIN'].includes(target);
    isSelectTeacherShow.value = target === 'STUDENT';
    isTeacherResumeShow.value = ['TEACHER', 'ADMIN'].includes(target);
  }
}

function SignoutClicked() {
  userInfoStore.clearSession();
  router.replace("/");
}

watch(() => userInfoStore.userInfo, (newValue) => {
  handleIsShowFunction(newValue.role);
});

watch(route, (newRoute) => {
  activeMenu.value = newRoute.path; // 路由变化时高亮相应菜单项
});
</script>

<template>
  <div class="sidemenu_box">
    <el-row class="tac">
      <el-col>
        <el-menu
          :default-active="activeMenu"
          active-text-color="var(--app-menu-active-text)"
          background-color="var(--app-menu-bg)"
          class="el-menu-vertical-demo"
          text-color="var(--app-menu-text)"
        >
          <el-sub-menu v-if="isStudentListShow" index="sl">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>学生列表</span>
            </template>
            <el-menu-item-group title="学年" class="sub_menu">
              <el-menu-item
                v-for="item in studentListMenuItems"
                :key="item.path"
                :index="item.path"
                @click="studentListClicked(item.path)"
              >
                {{ item.label }}
              </el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
          <el-menu-item index="/teach_list" @click="activeMenu = '/teach_list'; router.push('/teach_list')">
            <el-icon><Avatar /></el-icon>
            <span>导师列表</span>
          </el-menu-item>
          <el-menu-item index="/select_student" @click="activeMenu = '/select_student'; router.push('/select_student')" v-if="isSelectStudentShow">
            <el-icon><Avatar /></el-icon>
            <span>学生申请</span>
          </el-menu-item>
          <el-menu-item index="/select_teacher" @click="activeMenu = '/select_teacher'; router.push('/select_teacher')" v-if="isSelectTeacherShow">
            <el-icon><Avatar /></el-icon>
            <span>选择导师</span>
          </el-menu-item>
          <el-menu-item index="/relations" @click="activeMenu = '/relations'; router.push('/relations')">
            <el-icon><Connection /></el-icon>
            <span>师生关系</span>
          </el-menu-item>
          <el-menu-item index="/notice" @click="activeMenu = '/notice'; router.push('/notice')">
            <el-icon><BellFilled /></el-icon>
            <span>通知公告</span>
          </el-menu-item>
          <el-menu-item index="/teach_resume" @click="activeMenu = '/teach_resume'; router.push('/teach_resume')" v-if="isTeacherResumeShow">
            <el-icon><Position /></el-icon>
            <span>导师卡片</span>
          </el-menu-item>
          <el-menu-item index="/personal" @click="activeMenu = '/personal'; router.push('/personal')">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
          <el-menu-item index="/reset_pwd" @click="activeMenu = '/reset_pwd'; router.push('/reset_pwd')">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </el-menu-item>
          <el-menu-item @click="SignoutClicked">
            <el-icon><CircleClose /></el-icon>
            <span>退出登录</span>
          </el-menu-item>
        </el-menu>
      </el-col>
    </el-row>
    <SideFooterComp/>
  </div>
</template>

<style scoped lang="sass">
.sidemenu_box
  position: fixed
  top: 60px
  left: 0
  width: 200px
  height: calc(100vh - 60px)
  background-color: var(--app-menu-bg)
  color: var(--app-menu-text)
  .el-menu-vertical-demo
    height: 100%
    width: 100%
  :deep(.el-menu)
    border-right: none
    background-color: var(--app-menu-bg)
  :deep(.el-menu-item)
    color: var(--app-menu-text)
    background-color: transparent
  :deep(.el-sub-menu__title)
    color: var(--app-menu-text)
    background-color: transparent
  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover)
    background-color: var(--app-menu-hover)
  :deep(.el-menu-item.is-active)
    background-color: var(--app-menu-active-bg)
  :deep(.el-menu-item-group__title)
    color: color-mix(in srgb, var(--app-menu-text) 72%, transparent)
  :deep(.sub_menu)
    background-color: var(--app-menu-sub-bg)
</style>
