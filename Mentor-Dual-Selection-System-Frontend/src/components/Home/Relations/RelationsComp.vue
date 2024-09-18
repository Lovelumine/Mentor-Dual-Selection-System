<script setup lang="ts">

import {ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userStore = useUserInfoStore();
import AllApplicationListViewByStudentComp from "@/components/Home/Relations/Student/AllApplicationListViewByStudentComp.vue";
import StudentToTeacherComp from "@/components/Home/Relations/Student/StudentToTeacherComp.vue";
import TeacherToStudentComp from "@/components/Home/Relations/Teacher/TeacherToStudentComp.vue";
import AllApplicationListViewByTeacherComp from "@/components/Home/Relations/Teacher/AllApplicationListViewByTeacherComp.vue";
import AllPendingViewComp from "@/components/Home/Relations/Admin/AllPendingViewComp.vue";

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
  <div class="container" v-if="pageRole.en === 'teacher'">
    <TeacherToStudentComp/>
    <AllApplicationListViewByTeacherComp/>
  </div>
  <div class="container" v-if="pageRole.en === 'admin'">
    <AllPendingViewComp/>
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