<script setup lang="ts">
import SigninComp from "@/components/SigninComp.vue";
import { onMounted, ref } from "vue";
import SideMenuComp from "@/components/HeaderFooterMenu/SideMenuComp.vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
const userStore = useUserInfoStore();
import { useRouter } from "vue-router";
import FooterComp from "@/components/HeaderFooterMenu/FooterComp.vue";
const router = useRouter();

const isSignin = ref(false);

onMounted(() => {
  if (localStorage.getItem("token")) {
    userStore.fetchUserInfo();
    isSignin.value = true;
  } else {
    router.replace('/');
  }
});
</script>

<template>
  <div class="container_login" v-if="isSignin">
    <SideMenuComp/>
    <div class="router_container">
      <router-view/>
    </div>
  </div>
  <div class="container" v-else>
    <h1>您好！请先登录</h1>
    <h2>农业与生物技术学院</h2>
    <SigninComp/>
    <FooterComp/>
  </div>
</template>

<style scoped lang="sass">
.container
  h1, h2
    text-align: center
    font-weight: bold
  h1
    margin-top: 30px
  h2
    letter-spacing: 2px

.container_login
  display: flex

.router_container
  flex: 1
  margin-left: 200px  // 确保右侧内容不被左侧菜单栏遮挡
  overflow-y: auto
  height: 100vh
</style>
