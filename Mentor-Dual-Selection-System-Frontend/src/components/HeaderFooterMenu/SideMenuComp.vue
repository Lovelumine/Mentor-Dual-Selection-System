<script setup lang="ts">
import { Avatar, BellFilled, CircleClose, Connection, Lock, Position, User, UserFilled } from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
const router = useRouter();
const route = useRoute();
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();
import SideFooterComp from "@/components/HeaderFooterMenu/SideFooterComp.vue";
import { getStudentListMenuItems } from "@/utils/studentCohort";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";

const isSelectStudentShow = ref(false);
const isSelectTeacherShow = ref(false);
const isStudentListShow = ref(false);
const isTeacherResumeShow = ref(false);
const studentListMenuItems = getStudentListMenuItems();
const {
  isDesktop,
  isMobile,
  isTablet,
  sidebarOpen,
  closeSidebar,
} = useResponsiveLayout();

const activeMenu = ref(route.path);
const isCollapsed = computed(() => isTablet.value && !sidebarOpen.value);
const showBackdrop = computed(() => !isDesktop.value && sidebarOpen.value);
const showFooter = computed(() => isDesktop.value || !isCollapsed.value);
const sidebarClasses = computed(() => ({
  "sidemenu_box--desktop": isDesktop.value,
  "sidemenu_box--tablet": isTablet.value,
  "sidemenu_box--tablet-open": isTablet.value && sidebarOpen.value,
  "sidemenu_box--tablet-collapsed": isTablet.value && !sidebarOpen.value,
  "sidemenu_box--mobile": isMobile.value,
  "sidemenu_box--mobile-open": isMobile.value && sidebarOpen.value,
}));

onMounted(() => {
  if (userInfoStore.userInfo) handleIsShowFunction(userInfoStore.userInfo.role);
  activeMenu.value = route.path; // 初始化当前高亮菜单项

});

function navigateTo(path: string) {
  activeMenu.value = path;
  router.push(path);
  closeSidebar();
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
  closeSidebar();
}

watch(() => userInfoStore.userInfo, (newValue) => {
  handleIsShowFunction(newValue.role);
});

watch(route, (newRoute) => {
  activeMenu.value = newRoute.path; // 路由变化时高亮相应菜单项
});
</script>

<template>
  <div v-if="showBackdrop" class="menu_backdrop" @click="closeSidebar"></div>
  <aside :class="['sidemenu_box', sidebarClasses]">
    <div class="sidemenu_inner">
      <el-row class="tac">
        <el-col>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          :collapse-transition="false"
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
                @click="navigateTo(item.path)"
              >
                {{ item.label }}
              </el-menu-item>
            </el-menu-item-group>
          </el-sub-menu>
          <el-menu-item index="/teach_list" @click="navigateTo('/teach_list')">
            <el-icon><Avatar /></el-icon>
            <span>导师列表</span>
          </el-menu-item>
          <el-menu-item index="/select_student" @click="navigateTo('/select_student')" v-if="isSelectStudentShow">
            <el-icon><Avatar /></el-icon>
            <span>学生申请</span>
          </el-menu-item>
          <el-menu-item index="/select_teacher" @click="navigateTo('/select_teacher')" v-if="isSelectTeacherShow">
            <el-icon><Avatar /></el-icon>
            <span>选择导师</span>
          </el-menu-item>
          <el-menu-item index="/relations" @click="navigateTo('/relations')">
            <el-icon><Connection /></el-icon>
            <span>师生关系</span>
          </el-menu-item>
          <el-menu-item index="/notice" @click="navigateTo('/notice')">
            <el-icon><BellFilled /></el-icon>
            <span>通知公告</span>
          </el-menu-item>
          <el-menu-item index="/teach_resume" @click="navigateTo('/teach_resume')" v-if="isTeacherResumeShow">
            <el-icon><Position /></el-icon>
            <span>导师卡片</span>
          </el-menu-item>
          <el-menu-item index="/personal" @click="navigateTo('/personal')">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
          <el-menu-item index="/reset_pwd" @click="navigateTo('/reset_pwd')">
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
      <div v-if="showFooter" class="sidemenu_footer">
        <SideFooterComp />
      </div>
    </div>
  </aside>
</template>

<style scoped lang="sass">
.menu_backdrop
  position: fixed
  top: 60px
  left: 0
  right: 0
  bottom: 0
  background: rgba(8, 13, 10, .48)
  backdrop-filter: blur(2px)
  z-index: 1180

.sidemenu_box
  position: fixed
  top: 60px
  left: 0
  height: calc(100vh - 60px)
  background-color: var(--app-menu-bg)
  color: var(--app-menu-text)
  border-right: 1px solid color-mix(in srgb, var(--app-menu-text) 12%, transparent)
  z-index: 1190
  transition: width .24s ease, transform .24s ease, box-shadow .24s ease
  overflow: hidden
  .sidemenu_inner
    display: flex
    flex-direction: column
    height: 100%
  .el-menu-vertical-demo
    flex: 1 1 auto
    min-height: 0
    width: 100%
  .sidemenu_footer
    padding: 14px 12px 18px

.sidemenu_box--desktop
  width: 200px
  transform: translateX(0)

.sidemenu_box--tablet
  width: 72px
  transform: translateX(0)

.sidemenu_box--tablet-open
  width: 200px
  box-shadow: 0 20px 40px rgba(0, 0, 0, .18)

.sidemenu_box--mobile
  width: min(82vw, 300px)
  transform: translateX(-100%)
  box-shadow: 0 20px 40px rgba(0, 0, 0, .22)

.sidemenu_box--mobile-open
  transform: translateX(0)

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

@media (max-width: 767px)
  .sidemenu_box
    width: min(84vw, 300px)
</style>
