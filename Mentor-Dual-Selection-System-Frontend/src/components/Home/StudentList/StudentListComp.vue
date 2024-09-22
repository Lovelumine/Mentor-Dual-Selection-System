<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import StudentListSearchComp from "@/components/Home/StudentList/StudentListSearchComp.vue";
import StudentListFuncComp from "@/components/Home/StudentList/StudentListFuncComp.vue";
import StudentListViewComp from "@/components/Home/StudentList/StudentListViewComp.vue";
import {useStuListGrade} from "@/stores/StudentListGrade";

const grade = ref('一');

function changeTitleGrade (target: number) {
  switch (target) {
    case 1: grade.value = '一'; break;
    case 2: grade.value = '二'; break;
    case 3: grade.value = '三'; break;
    case 4: grade.value = '四'; break;
    default: break;
  }
}
onMounted(() => {
  if (useStuListGrade().gradeStatus) changeTitleGrade(useStuListGrade().gradeStatus);
})

watch(() => useStuListGrade().gradeStatus, (newVal) => {
  changeTitleGrade(newVal)
})

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