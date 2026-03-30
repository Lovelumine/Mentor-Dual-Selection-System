<script setup lang="ts">
import zhuzhanLogo from "@/assets/zhuzhanLOGO.png";
import {computed} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userInfoStore = useUserInfoStore();

const isLogin = computed(() => userInfoStore.isAuthenticated);
const userInfoComp = computed(() => userInfoStore.userInfo);
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
    <div class="logo_box">
      <img class="logo" :src="zhuzhanLogo" alt="logo" title="学校主页" @click="logoClicked"/>
      <span class="logo_title">农业与生物技术学院 - 师生双选系统</span>
    </div>
    <div class="user_bar" v-if="isLogin">
      欢迎您！{{ userRoleCN }}：{{ userInfoComp.fullName }}
    </div>
   <!-- <div class="user_bar" v-if="isLogin">-->
<!--      <a href="/relations" title="师生关系">-->
<!--        <div class="user_icon_box">-->
<!--          <el-icon class="user_icon" :size="iconProperty.size" :color="iconProperty.color"><Connection /></el-icon>-->
<!--        </div>-->
<!--      </a>-->
<!--      <a href="/personal" title="个人中心">-->
<!--        <div class="user_icon_box">-->
<!--          <el-icon class="user_icon" :size="iconProperty.size" :color="iconProperty.color"><User /></el-icon>-->
<!--        </div>-->
<!--      </a>-->
<!--      <a title="退出登录" @click="SignoutClicked">-->
<!--        <div class="user_icon_box">-->
<!--          <el-icon class="user_icon" :size="iconProperty.size" :color="iconProperty.color"><CircleClose /></el-icon>/-->
<!--        </div>-->
<!--      </a>-->
<!--    </div> -->
  </Header>
</template>

<style scoped lang="sass">
.header_box
  width: 100%
  height: 60px
  background: linear-gradient(90deg, var(--app-header-bg) 0%, color-mix(in srgb, var(--app-header-bg) 82%, black) 100%)
  display: flex
  justify-content: center
  align-items: center
  box-shadow: 0 10px 24px var(--app-shadow)
  .logo_box
    width: 100%
    height: 100%
    display: flex
    align-items: center
    .logo
      height: 80%
      margin-left: 20px
    .logo:hover
      cursor: pointer
    .logo_title
      font-size: 25px
      color: var(--app-header-text)
      font-weight: bold
      margin-left: 15px
      letter-spacing: 2px
  a
    text-decoration: none
  .user_bar
    width: 500px
    color: var(--app-header-text)
    margin: 0 20px 0 auto
    flex: 0 1 auto
    display: flex
    gap: 12px
    justify-content: flex-end
    align-items: center
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
</style>
