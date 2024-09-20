<script setup lang="ts">
import {onMounted, ref} from "vue";
import {http} from "@/utils/http";

const allRelations = ref([]);

onMounted(() => {
  http({
    url: '/application/mentor-student-relations',
    method: 'GET',
    headers: {
      'Accept': '*/*',
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  }).then(res => {
    if (res.data.code === 200){
      allRelations.value = res.data.data;
    } else {
      alert(res.data.data.error);
    }
  }).catch(err => {
    alert(JSON.parse(err.requests.responseText).data.error);
  })
})
</script>

<template>
  <div class="all_pending_box">
    <h3>所有师生选择的学生</h3>
    <div v-for="(item, index) in allRelations" :key="index" class="for_teacher_box">
      <div class="teacher_item">
        导师姓名：{{item.mentor.fullName}}
      </div>
      <div v-for="(sItem, sIndex) in item.students" :key="sIndex" class="for_student_box">
        <div class="student_item">
          学生姓名：{{sItem.fullName}}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.all_pending_box
  position: relative
  margin: 50px auto 20px auto
  width: 50%
  h3
    position: absolute
    top: -40px
    left: 50%
    transform: translateX(-50%)
    font-weight: bold
  .for_teacher_box
    margin-bottom: 20px
    .teacher_item
      padding: 5px
      text-align: center
      box-shadow: #005826 0 0 5px
      border-radius: 10px
      margin: 0 auto 20px auto
      font-size: 17px
      font-weight: bold
    .for_student_box
      display: flex
      .student_item
        width: 50%
        padding: 3px
        text-align: center
        box-shadow: #005826 0 0 5px
        border-radius: 10px
        margin: 0 auto 20px auto
</style>