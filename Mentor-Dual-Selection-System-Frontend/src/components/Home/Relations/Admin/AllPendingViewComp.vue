<script setup lang="ts">
import {onMounted, ref, computed} from "vue";
import {http} from "@/utils/http";
//Mentor接口
interface MentorImpl {
  uid: number;
  fullName: string;
  email: string;
}
//Students接口
interface StudentsImpl {
  uid: number;
  fullName: string;
}
// 定义接口来描述从服务器返回的数据结构
interface MentorStudentRelation {
  mentor: MentorImpl;
  students: StudentsImpl[];
}
interface ApiResponse {
  code: number;
  data: MentorStudentRelation[];
  error?: string; // 可选错误字段，用于非 200 响应
}

// 使用 ref 来存储从服务器获取的关系数据
const allRelations = ref<MentorStudentRelation[]>([]);

// 统计信息
const totalMentors = computed(() => allRelations.value.length);
const totalStudents = computed(() =>
  allRelations.value.reduce((sum, item) => sum + item.students.length, 0)
);
const mentorStudentRatio = computed(() => {
  return totalStudents.value === 0
    ? "无数据"
    : (totalMentors.value / totalStudents.value).toFixed(2);
});

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
});
</script>

<template>
  <div class="all_pending_box">
    <h3>导师与学生配对列表</h3>
    
    <!-- 统计信息展示部分 -->
    <div class="statistics">
      <p>目前已有 <strong>{{ totalMentors }}</strong> 位导师选择了 <strong>{{ totalStudents }}</strong> 位学生</p>
      <p>导师和学生的比例为：<strong>{{ mentorStudentRatio }}</strong></p>
    </div>

    <!-- 导师与学生配对列表 -->
    <div class="teacher_student_container">
      <div v-for="(item, index) in allRelations" :key="index" class="for_teacher_box">
        <div class="teacher_item">
          导师姓名：{{item.mentor.fullName}}
        </div>
        <div v-for="(sItem, sIndex) in item.students" :key="sIndex" class="student_item">
          学生姓名：{{sItem.fullName}}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.all_pending_box
  margin: 50px auto 20px auto
  padding: 20px
  width: 90%
  background-color: #f9f9f9
  border-radius: 10px
  box-shadow: rgba(0, 0, 0, 0.1) 0px 4px 12px
  h3
    text-align: center
    font-weight: bold
    font-size: 24px
    color: #333

.statistics
  text-align: center
  margin-bottom: 20px
  p
    font-size: 16px
    color: #555
    strong
      color: #4CAF50

.teacher_student_container
  display: flex
  flex-wrap: wrap
  justify-content: space-between

.for_teacher_box
  flex: 1 1 calc(50% - 20px)
  padding: 20px
  margin: 10px
  background-color: #e6f5e9
  border-radius: 10px
  box-shadow: rgba(0, 0, 0, 0.1) 0px 4px 12px
  .teacher_item
    padding: 10px
    text-align: center
    background-color: #4CAF50
    color: white
    border-radius: 10px
    font-size: 18px
    font-weight: bold

  .student_item
    margin-top: 10px
    padding: 10px
    background-color: #fff
    color: #333
    border: 1px solid #005826
    border-radius: 10px
    box-shadow: rgba(0, 0, 0, 0.1) 0px 4px 12px
    text-align: center

@media (max-width: 768px)
  .for_teacher_box
    flex: 1 1 100%
</style>
