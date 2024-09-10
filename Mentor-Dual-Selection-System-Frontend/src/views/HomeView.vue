<script setup lang="ts">

import SigninComp from "@/components/SigninComp.vue";
import {ref} from "vue";
import SideHeaderComp from "@/components/HeaderAndFooter/SideHeaderComp.vue";

const isSignin = ref(true);
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
    <SideHeaderComp/>
    <router-view/>
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

</style>