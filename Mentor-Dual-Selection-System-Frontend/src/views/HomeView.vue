<script setup lang="ts">
import SigninComp from "@/components/SigninComp.vue";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import SideMenuComp from "@/components/HeaderFooterMenu/SideMenuComp.vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
const userStore = useUserInfoStore();
import { useRouter } from "vue-router";
import FooterComp from "@/components/HeaderFooterMenu/FooterComp.vue";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";
const router = useRouter();
const { contentOffset, device } = useResponsiveLayout();

const isCheckingAuth = ref(true);
const isSignin = ref(false);
const routerContainerStyle = computed(() => ({
  paddingLeft: `${contentOffset.value}px`,
}));

async function routeByAuthState(isAuthenticated: boolean) {
  isSignin.value = isAuthenticated;

  if (isAuthenticated) {
    if (router.currentRoute.value.path === "/") {
      await router.replace("/notice");
    }
    return;
  }

  if (router.currentRoute.value.path !== "/") {
    await router.replace("/");
  }
}

async function initAuthState() {
  isCheckingAuth.value = true;

  const hasToken = Boolean(localStorage.getItem("token"));
  const isAuthenticated = hasToken ? await userStore.fetchUserInfo() : false;

  if (!hasToken) {
    userStore.clearSession();
  }

  await routeByAuthState(isAuthenticated);
  isCheckingAuth.value = false;
}

async function handleAuthExpired() {
  userStore.clearSession();
  await routeByAuthState(false);
  isCheckingAuth.value = false;
}

onMounted(() => {
  window.addEventListener("mentor-auth-expired", handleAuthExpired);
  void initAuthState();
});

onUnmounted(() => {
  window.removeEventListener("mentor-auth-expired", handleAuthExpired);
});

watch(() => userStore.isAuthenticated, (newValue) => {
  isSignin.value = newValue;
});
</script>

<template>
  <div class="auth_loading" v-if="isCheckingAuth">
    <el-skeleton :rows="6" animated />
  </div>
  <div class="container_login" v-else-if="isSignin">
    <SideMenuComp />
    <div :class="['router_container', `router_container--${device}`]" :style="routerContainerStyle">
      <router-view />
    </div>
  </div>
  <div class="container" v-else>
    <h1>您好！请先登录</h1>
    <h2>农业与生物技术学院</h2>
    <SigninComp />
    <FooterComp />
  </div>
</template>

<style scoped lang="sass">
.auth_loading
  min-height: calc(100vh - 60px)
  padding: 48px min(48px, 4vw)
  background: transparent

.container
  min-height: calc(100vh - 60px)
  padding-bottom: 40px
  background: transparent
  color: var(--app-text-primary)
  h1, h2
    text-align: center
    font-weight: bold
    color: var(--app-text-primary)
  h1
    margin-top: 30px
    font-size: clamp(40px, 5vw, 58px)
  h2
    letter-spacing: 2px
    margin-top: 18px
    font-size: clamp(26px, 4vw, 44px)

.container_login
  min-height: calc(100vh - 60px)
  background: transparent

.router_container
  width: 100%
  min-height: calc(100vh - 60px)
  padding-right: 24px
  padding-bottom: 24px
  box-sizing: border-box
  background: transparent
  color: var(--app-text-primary)
  transition: padding-left .24s ease, padding-right .24s ease

.router_container--tablet
  padding-right: 18px

.router_container--mobile
  padding-right: 0
  padding-bottom: 16px

@media (max-width: 1023px)
  .container
    padding-top: 28px

@media (max-width: 767px)
  .container
    padding: 24px 12px 32px
    h2
      letter-spacing: 1px

  .router_container
    padding-left: 0 !important
</style>
