<script setup lang="ts">

import SigninComp from "@/components/SigninComp.vue";
import {onMounted, ref} from "vue";
import SideMenuComp from "@/components/HeaderAndFooter/SideMenuComp.vue";

const isSignin = ref(false);

onMounted(() => {
  if (localStorage.getItem("token")) {
    isSignin.value = true;
  }
})
</script>

<template>
  <div class="container" v-if="!isSignin">
    <h1>
      您好！请先登录
    </h1>
    <h2>
      农业与生物技术学院
    </h2>
    <SigninComp/>
  </div>
  <div class="container_login" v-if="isSignin">
    <SideMenuComp/>
    <div class="router_container">
      <router-view class="router_container"/>
    </div>
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
    flex-grow: 1

</style>