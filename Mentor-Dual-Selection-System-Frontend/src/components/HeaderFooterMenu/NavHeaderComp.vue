<script setup lang="ts">
import zhuzhanLogo from "@/assets/zhuzhanLOGO.png";
import { Menu } from "@element-plus/icons-vue";
import { computed } from "vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";
const userInfoStore = useUserInfoStore();
const { isDesktop, toggleSidebar } = useResponsiveLayout();

const isLogin = computed(() => userInfoStore.isAuthenticated);
const userInfoComp = computed(() => userInfoStore.userInfo);
const showMenuToggle = computed(() => isLogin.value && !isDesktop.value);
const userRoleCN = computed(() => {
  switch (userInfoStore.userInfo.role) {
    case 'TEACHER': return '导师';
    case 'ADMIN': return '管理员';
    case 'STUDENT': return '学生';
    default: return '';
  }
});

function logoClicked() {
  window.location.href = "https://www.sysu.edu.cn/";
}
</script>


<template>
  <Header class="header_box">
    <button v-if="showMenuToggle" class="menu_toggle" type="button" @click="toggleSidebar" aria-label="展开侧边菜单">
      <el-icon><Menu /></el-icon>
    </button>
    <div class="logo_box">
      <img class="logo" :src="zhuzhanLogo" alt="logo" title="学校主页" @click="logoClicked" />
      <span class="logo_title">
        <span class="logo_title__college">农业与生物技术学院</span>
        <span class="logo_title__separator"> - </span>
        <span class="logo_title__system">师生双选系统</span>
      </span>
    </div>
    <div class="user_bar" v-if="isLogin">
      <span class="user_bar__prefix">欢迎您！</span>
      <span class="user_bar__name">{{ userRoleCN }}：{{ userInfoComp.fullName }}</span>
    </div>
  </Header>
</template>

<style scoped lang="sass">
.header_box
  width: 100%
  height: 60px
  background: linear-gradient(90deg, var(--app-header-bg) 0%, color-mix(in srgb, var(--app-header-bg) 82%, black) 100%)
  display: flex
  justify-content: space-between
  align-items: center
  gap: 12px
  padding: 0 20px 0 12px
  box-sizing: border-box
  box-shadow: 0 10px 24px var(--app-shadow)
  position: sticky
  top: 0
  z-index: 1100
  .menu_toggle
    width: 44px
    height: 44px
    border-radius: 12px
    border: 1px solid color-mix(in srgb, var(--app-header-text) 22%, transparent)
    background: color-mix(in srgb, var(--app-header-bg) 82%, rgba(255, 255, 255, .14) 18%)
    color: var(--app-header-text)
    display: flex
    align-items: center
    justify-content: center
    flex: 0 0 auto
    cursor: pointer
    transition: background-color .2s ease, transform .2s ease
  .menu_toggle:hover
    background: color-mix(in srgb, var(--app-header-bg) 68%, rgba(255, 255, 255, .22) 32%)
  .menu_toggle:active
    transform: scale(.97)
  .logo_box
    display: flex
    align-items: center
    min-width: 0
    flex: 1 1 auto
    .logo
      height: 80%
      max-height: 46px
    .logo:hover
      cursor: pointer
    .logo_title
      font-size: clamp(18px, 2vw, 25px)
      color: var(--app-header-text)
      font-weight: bold
      margin-left: 15px
      letter-spacing: 1px
      white-space: nowrap
      overflow: hidden
      text-overflow: ellipsis
  a
    text-decoration: none
  .user_bar
    color: var(--app-header-text)
    display: flex
    gap: 6px
    justify-content: flex-end
    align-items: center
    flex: 0 0 auto
    max-width: min(42vw, 420px)
    text-align: right
    min-width: 0
    &__prefix
      flex: 0 0 auto
    &__name
      overflow: hidden
      white-space: nowrap
      text-overflow: ellipsis
    .user_avatar:hover
      cursor: pointer
    .user_avatar
      width: 32px
      height: 32px
      border-radius: 50%
      border: 1px solid var(--app-header-text)
    .user_icon_box:hover
      background-color: rgba(255, 255, 255, 0.56)
    .user_icon_box
      border-radius: 4px
      border: 1px solid var(--app-header-text)
      width: 32px
      height: 32px
      background-color: rgba(255, 255, 255, 0.22)
      position: relative
      transition: .5s ease background-color
      .user_icon
        position: absolute
        left: 3px
        top: 3px
        p
          padding: 0
          color: var(--app-header-text)

@media (max-width: 1023px)
  .header_box
    padding-right: 16px
    .logo_box .logo_title
      font-size: clamp(16px, 2.7vw, 22px)
    .user_bar
      max-width: min(40vw, 320px)
      font-size: 14px

@media (max-width: 767px)
  .header_box
    padding: 0 12px
    gap: 8px
    .logo_box .logo
      max-height: 40px
    .logo_box .logo_title
      font-size: 15px
      margin-left: 10px
      .logo_title__college,
      .logo_title__separator
        display: none
      .logo_title__system
        display: inline
    .user_bar
      max-width: 44vw
      font-size: 12px
      line-height: 1.35
</style>
