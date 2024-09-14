<script setup lang="ts">

import {ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userStore = useUserInfoStore();
import AllApplicationListViewByStudentComp from "@/components/Home/Relations/AllApplicationListViewByStudentComp.vue";
import StudentToTeacherComp from "@/components/Home/Relations/StudentToTeacherComp.vue";

const pageRole = ref({
  en: '',
  cn: ''
});

watch(() => userStore.userInfo, (newValue) => {
  switch (newValue.role){
    case 'STUDENT': {
      pageRole.value.cn = '学生';
      pageRole.value.en = 'student';
      break;
    } case 'TEACHER': {
      pageRole.value.cn = '导师';
      pageRole.value.en = 'teacher';
      break;
    } case 'ADMIN': {
      pageRole.value.cn = '管理员';
      pageRole.value.en = 'admin';
      break;
    } default: break;
  }
})
</script>

<template>
  <div class="title">
    <span>师生关系（{{pageRole.cn}}）</span>
  </div>
  <div class="container" v-if="pageRole.en === 'student'">
    <StudentToTeacherComp/>
    <AllApplicationListViewByStudentComp/>
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