<script setup lang="ts">

import {onMounted} from "vue";
import {http} from "@/utils/http";

onMounted(() => {
  http({
    url: '/application/pending',
    method: 'GET',
    headers: {
      'Content-Type': '*/*',
      'Authorization': 'Bearer ' + localStorage.getItem('token'),
    },
    params: {
      Authorization: localStorage.getItem('token'),
    }
  })
})
</script>

<template>
  <div class="title">
    <span>选择学生</span>
  </div>
  <div class="container">

  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
</style>