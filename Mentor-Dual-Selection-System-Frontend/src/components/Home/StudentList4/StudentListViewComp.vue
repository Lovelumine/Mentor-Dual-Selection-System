<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { http } from "@/utils/http";
import { useStudentListStore } from "@/stores/StudentListStore";

// 定义学生对象类型
interface Student {
  fullName: string;
  username: string;
  grade: string;
  class: string;
  email: string;
  mentor_id: number | null;
  isMentor: string;
  teacherName?: string;
  resume: string;
}

const studentListStore = useStudentListStore();
const grade = ref<string | null>(null); // 允许 null 类型
const tableData = ref<Student[]>([]); // 定义为 Student 类型的数组

function getCurrentYearMonth(): string {
  const now = new Date();
  const year = now.getFullYear().toString().padStart(4, '0');
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  return `${year}-${month}`;
}

function isStudentGradeFourth(): string {
  const currentYearMonth = getCurrentYearMonth();
  const [currentYear, currentMonth] = currentYearMonth.split('-').map(Number);
  let entryYear = new Date().getFullYear() - 3;
  if (entryYear === (currentYear - 3) && 9 <= currentMonth) {
    console.log(1, entryYear);
    return entryYear.toString().padStart(4, '0');
  } else if ((entryYear + 1) === (currentYear - 2) && currentMonth <= 8) {
    console.log(2, entryYear - 1);
    return (entryYear - 1).toString().padStart(4, '0');
  }
  return ''; // 添加默认返回值，防止 TypeScript 错误
}

// 定义函数参数的类型
function selectStudentByName(targetStudentList: Student[], targetIsSelectTeacher: string | null) {
  const grade = isStudentGradeFourth();
  let tempList: Student[] = [];
  for (let i = 0; i < targetStudentList.length; i++) {
    if (targetStudentList[i].grade === grade) {
      if (targetStudentList[i].mentor_id !== null) {
        targetStudentList[i].isMentor = '已通过';
        http({
          url: '/search/teacher',
          method: 'GET',
          headers: {
            Accept: '*/*',
            Authorization: 'Bearer ' + localStorage.getItem('token')
          },
          params: {
            uid: targetStudentList[i].mentor_id
          }
        }).then(res => {
          if (res.data.code === 200) {
            targetStudentList[i].teacherName = res.data.data.fullName;
          }
        })
      } else {
        targetStudentList[i].isMentor = '未选择';
      }
      switch (targetIsSelectTeacher) {
        case null:
          tempList.push(targetStudentList[i]);
          break;
        case 'no':
          if (targetStudentList[i].mentor_id === null) tempList.push(targetStudentList[i]);
          break;
        case 'accepted':
          if (targetStudentList[i].mentor_id !== null) tempList.push(targetStudentList[i]);
          break;
        default:
          break;
      }
    }
  }
  tableData.value = tempList; // 确保 tempList 具有正确的类型
}

onMounted(() => {
  grade.value = isStudentGradeFourth();
  http({
    url: '/search/students/filter',
    method: 'GET',
    headers: {
      Accept: '*/*',
      Authorization: 'Bearer ' + localStorage.getItem('token')
    },
    params: {
      grade: grade.value,
    }
  }).then(res => {
    if (res.data.code === 200) {
      tableData.value = res.data.data; // 保证返回的数据符合 Student[] 类型
      for (let i = 0; i < tableData.value.length; i++) {
        if (tableData.value[i].mentor_id !== null) {
          tableData.value[i].isMentor = '已通过';
          http({
            url: '/search/teacher',
            method: 'GET',
            headers: {
              Accept: '*/*',
              Authorization: 'Bearer ' + localStorage.getItem('token')
            },
            params: {
              uid: tableData.value[i].mentor_id
            }
          }).then(res => {
            if (res.data.code === 200) {
              tableData.value[i].teacherName = res.data.data.fullName;
            }
          })
        } else {
          tableData.value[i].isMentor = '未选择';
        }
      }
    } else {
      alert(res.data.data.error);
    }
  }).catch(err => {
    alert(JSON.parse(err.requests.responseText).data.error);
  });
})

watch([() => studentListStore.isSelectTeacher, () => studentListStore.studentListSt], ([newIsSelectTeacherValue, newStudentList]) => {
  if (newStudentList) selectStudentByName(newStudentList, newIsSelectTeacherValue);
});
</script>


<template>
  <div class="title">
    <span>列表内容（{{grade}}）级</span>
  </div>
  <el-table :data="tableData" stripe class="table">
    <el-table-column prop="fullName" show-overflow-tooltip label="姓名"  />
    <el-table-column prop="username" label="学号"  />
    <el-table-column prop="grade" label="年级"  />
    <el-table-column prop="class" label="班级" show-overflow-tooltip />
    <el-table-column prop="email" show-overflow-tooltip label="邮箱"  />
    <el-table-column prop="isMentor" label="选择导师状态"/>
    <el-table-column prop="teacherName" show-overflow-tooltip label="导师姓名"  />
    <!--    <el-table-column prop="is_teacher_score" label="导师评分状态"/>-->
    <!--    <el-table-column prop="research_training_rating" label="科研训练评分"/>-->
    <!--    <el-table-column prop="labor_education_rating" label="劳动教育评分"/>-->
    <!--    <el-table-column prop="rating_explanation" label="评分说明"/>-->
    <el-table-column prop="resume" label="简介" show-overflow-tooltip width="300"/>
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
  max-height: calc(100vh - 60px - 60px - 260px)
  overflow: auto
.table:hover
  box-shadow: #005826 0 0 10px
</style>