<script setup lang="ts">
import zhuzhanLogo from "@/assets/zhuzhanLOGO.png";
import {User, Operation, ChatDotSquare} from "@element-plus/icons-vue";
import {onMounted, ref} from "vue";

const iconProperty = ref({color: '', size: 0});
const isLogin = ref(true);
onMounted(() => {
  iconProperty.value.color = '#fff';
  iconProperty.value.size = 24;
})

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
      <a href="/communicate" title="工作通讯">
        <div class="user_icon_box">
          <el-icon class="user_icon" :size="iconProperty.size" :color="iconProperty.color"><ChatDotSquare /></el-icon>
        </div>
      </a>
      <a href="/rights_management" title="权限管理">
        <div class="user_icon_box">
          <el-icon class="user_icon" :size="iconProperty.size" :color="iconProperty.color"><Operation /></el-icon>
        </div>
      </a>
      <a href="/personal/" title="个人中心">
        <div class="user_icon_box">
          <el-icon class="user_icon" :size="iconProperty.size" :color="iconProperty.color"><User /></el-icon>
        </div>
      </a>
    </div>
  </Header>
</template>

<style scoped lang="sass">
.header_box
  width: 100%
  height: 60px
  background-color: #005826
  display: flex
  justify-content: center
  align-items: center
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
      color: white
      font-weight: bold
      margin-left: 15px
      letter-spacing: 2px
  a
    text-decoration: none
  .user_bar
    width: 230px
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
      border: 1px solid #fff
    .user_icon_box:hover
      background-color: #ffffff90
    .user_icon_box
      border-radius: 4px
      border: 1px solid white
      width: 32px
      height: 32px
      background-color: #ffffff40
      position: relative
      transition: .5s ease background-color
      .user_icon
        position: absolute
        left: 3px
        top: 3px
        p
          padding: 0
          color: white
</style>