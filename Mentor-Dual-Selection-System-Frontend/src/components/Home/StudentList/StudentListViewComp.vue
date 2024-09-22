<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useStuListGrade} from "@/stores/StudentListGrade";
const stuListGrade = useStuListGrade();
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {httpAdmin, httpTeacher} from "@/utils/http";
const userInfoStore = useUserInfoStore();

const userRole = ref(null);
const tableData = ref([]);
const handledTableData = ref([]);
const grade = ref(0);

function fetchHttp (targetRole: string, ) {
  let gradeN = grade.value
  let gradeCN = null;
  if (gradeN === 1) {
    gradeCN = '大一';
  }
  else if (gradeN === 2) {
    gradeCN = '大二';
  }
  else if (gradeN === 3) {
    gradeCN = '大三';
  }
  else if (gradeN === 4) {
    gradeCN = '大四';
  }
  console.log(gradeCN);
  switch (targetRole) {
    case 'TEACHER': {
      httpTeacher({
        url: '/students',
        method: 'GET',
        headers: {
          Accept: '*/*',
          Authorization: 'Bearer ' + localStorage.getItem('token'),
        }
      }).then(res => {
        if (res.data.code === 200) {
          tableData.value = res.data.data;
        } else alert(res.data.data.error);
      }).catch(err => {
        alert(JSON.parse(err.requests.responseText).data.error);
      }); break;
    }
    case 'ADMIN': {
      httpAdmin({
        url: '/students',
        method: 'GET',
        headers: {
          Accept: '*/*',
          Authorization: 'Bearer ' + localStorage.getItem('token'),
        }
      }).then(res => {
        if (res.data.code === 200) {
          tableData.value = res.data.data;
        } else alert(res.data.data.error);
      }).catch(err => {
        alert(JSON.parse(err.requests.responseText).data.error);
      }); break;
    }
    default: break;
  }
}
onMounted(() => {
})

watch([
  () => stuListGrade.gradeStatus,
  () => userInfoStore.userInfo,
], ([newGradeStatus, newUserInfo]) => {
  grade.value = newGradeStatus;
  userRole.value = newUserInfo.role;
  fetchHttp(newUserInfo.role, newGradeStatus)
  handledTableData.value = [];
})

</script>

<template>
  <div class="title">
    <span>列表内容</span>
  </div>
  <el-table :data="handledTableData" stripe class="table">
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