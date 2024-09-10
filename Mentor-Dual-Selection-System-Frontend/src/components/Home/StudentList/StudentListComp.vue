<script setup lang="ts">
import {ref} from "vue";
import StudentListSearchComp from "@/components/Home/StudentList/StudentListSearchComp.vue";
import StudentListFuncComp from "@/components/Home/StudentList/StudentListFuncComp.vue";
import StudentListViewComp from "@/components/Home/StudentList/StudentListViewComp.vue";

const grade = ref('一');
</script>

<template>
  <div class="title">
    <span>学生列表（大{{grade}}）</span>
  </div>
  <StudentListSearchComp/>
  <StudentListFuncComp/>
  <StudentListViewComp/>
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