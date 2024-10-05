<script setup lang="ts">
import StudentListSearchComp from "@/components/Home/StudentList2/StudentListSearchComp.vue";
import StudentListFuncComp from "@/components/Home/StudentList2/StudentListFuncComp.vue";
import StudentListViewComp from "@/components/Home/StudentList2/StudentListViewComp.vue";
</script>

<template>
  <div class="title">
    <span>学生列表（大二）</span>
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