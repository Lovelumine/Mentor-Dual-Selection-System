<script setup lang="ts">

import SigninComp from "@/components/SigninComp.vue";
import {ref} from "vue";

const isSignin = ref(false);
</script>

<template>
  <div class="container" v-if="!isSignin">
    <h1>
      您好！正在访问中山大学师生双选系统！
    </h1>
    <h2>
      生命科学学院
    </h2>
    <SigninComp/>
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

</style>