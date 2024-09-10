<script setup lang="ts">
const tableData = [
  {
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },{
    name: '张三',
    id: 100031,
    grade: 2023,
    class: '生命科学学院综合科学班',
    phone: 10086100101,
    email: 'test@test.com',
    is_select_teacher: '已通过',
    teacher_name: '教师一',
    is_teacher_score: '未评分',
    research_training_rating: 95,
    labor_education_rating: 95,
    rating_explanation: '',
    notes: '班主任：教师三'
  },
]
</script>

<template>
  <div class="title">
    <span>列表内容</span>
  </div>
  <el-table :data="tableData" stripe class="table">
    <el-table-column prop="name" label="姓名"  />
    <el-table-column prop="id" label="学号"  />
    <el-table-column prop="grade" label="年级"  />
    <el-table-column prop="class" label="班级" show-overflow-tooltip />
    <el-table-column prop="phone" label="电话"  />
    <el-table-column prop="email" label="邮箱"  />
    <el-table-column prop="is_select_teacher" label="选择导师状态"/>
    <el-table-column prop="teacher_name" label="导师姓名"  />
    <el-table-column prop="is_teacher_score" label="导师评分状态"/>
    <el-table-column prop="research_training_rating" label="科研训练评分"/>
    <el-table-column prop="labor_education_rating" label="劳动教育评分"/>
    <el-table-column prop="rating_explanation" label="评分说明"/>
    <el-table-column prop="notes" label="备注"/>
  </el-table>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 40px
  background-color: #005826
  line-height: 40px
  padding-left: 20px
  font-size: 16px
  margin-bottom: 20px
  color: white
  span
    font-weight: bold
    letter-spacing: 2px
.table
  margin: 0 auto
  width: 98%
  border: 1px solid #005826
  border-radius: 5px
  transition: .3s ease
.table:hover
  box-shadow: #005826 0 0 10px
</style>