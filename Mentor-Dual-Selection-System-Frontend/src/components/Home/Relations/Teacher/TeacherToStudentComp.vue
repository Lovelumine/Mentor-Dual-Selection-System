<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { http } from "@/utils/http";
import { ElDialog, ElButton } from 'element-plus';

const router = useRouter();
const studentList = ref<any[]>([]); // 使用 any[] 来定义数组类型
const dialogVisible = ref(false); // 控制对话框的显示
const selectedStudent = ref<{
  photourl?: string,
  fullName?: string,
  email?: string,
  grade?: string,
  class?: string,
  resume?: string,
  research_direction?: string,
  netid?: string
}>({});  // 存储选中的学生信息

function getStudentInfo(uid: number) {
  return http({
    url: `/search/student?uid=${uid}`,
    method: "GET",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
  });
}

async function fetchStudentDetails() {
  const detailedStudents: any[] = [];

  // 使用 Promise.all 并行获取所有学生的详细信息
  await Promise.all(
    studentList.value.map(async (student) => {
      try {
        const res = await getStudentInfo(student.uid);
        if (res.data.code === 200) {
          console.log('Student data:', res.data.data); // Log API response
          detailedStudents.push(res.data.data);
        } else {
          detailedStudents.push({
            ...student, // 这里确保 student 是对象类型
            photourl: '',
            grade: '',
            class: '',
            resume: '',
            research_direction: ''
          });
        }
      } catch (error) {
        console.error(error);
        detailedStudents.push({
          ...student, // 这里确保 student 是对象类型
          photourl: '',
          grade: '',
          class: '',
          resume: '',
          research_direction: ''
        });
      }
    })
  );

  studentList.value = detailedStudents;
}

onMounted(async () => {
  if (!localStorage.getItem("token")) {
    router.push("/");
  } else {
    try {
      const res = await http({
        url: "/application/mentor-student-relations",
        method: "GET",
        headers: {
          Authorization: "Bearer " + localStorage.getItem("token"),
          Accept: "*/*",
        },
      });

      if (res.data.code === 200) {
        studentList.value = res.data.data[0].students;
        await fetchStudentDetails(); // 获取每个学生的详细信息
      } else {
        alert(res.data.data.error);
      }
    } catch (err) {
      console.error(err);
      alert("获取学生列表失败，请重新登录！");
      router.push("/");
    }
  }
});

// 定义查看详细信息的函数
function viewDetail(student: any) { // 给 student 显式添加 any 类型
  if (student) {
    selectedStudent.value = {
      photourl: student.photourl || '',
      fullName: student.fullName || '',
      email: student.email || '',
      grade: student.grade || '',
      class: student.class || '',
      resume: student.resume || '',
      research_direction: student.research_direction || student.researchDirection || '',
      netid: student.netid || student.uid || ''
    };
    console.log('Selected student:', selectedStudent.value); // Log selected student
    dialogVisible.value = true;
  } else {
    console.error('viewDetail: student is undefined');
  }
}
</script>

<template>
  <div class="teacher_to_student_box">
    <h3>您指导的学生</h3>
    <div class="item_box">
      <div class="item" v-for="(item, index) in studentList" :key="index">
        <img :src="item.photourl || 'https://via.placeholder.com/100'" alt="头像" class="avatar" />
        <div class="info">
          <div>姓名：{{ item.fullName }}</div>
          <div>邮箱：{{ item.email }}</div>
          <div v-if="item.grade">年级：{{ item.grade }}</div>
          <div v-if="item.class">班级：{{ item.class }}</div>
          <!-- 使用 Element Plus 的按钮 -->
          <el-button type="primary" @click="viewDetail(item)">详细信息</el-button>
        </div>
      </div>
    </div>
  </div>

  <!-- 学生详细信息对话框 -->
  <el-dialog
    v-model="dialogVisible"  
    title="学生详细信息"
    width="600px"
    :before-close="() => (dialogVisible = false)" 
  >
    <div class="student-detail">
      <img :src="selectedStudent.photourl || 'https://via.placeholder.com/100'" alt="头像" class="avatar" />
      <div class="info">
        <div><strong>姓名：</strong>{{ selectedStudent.fullName }}</div>
        <div><strong>学号：</strong>{{ selectedStudent.netid }}</div>
        <div><strong>邮箱：</strong>{{ selectedStudent.email }}</div>
        <div><strong>年级：</strong>{{ selectedStudent.grade || '未提供' }}</div>
        <div><strong>班级：</strong>{{ selectedStudent.class || '未提供' }}</div>
        <div><strong>意向研究方向：</strong>{{ selectedStudent.research_direction || '未提供' }}</div>
        <div><strong>个人简介：</strong>{{ selectedStudent.resume || '未提供' }}</div>
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button> 
      </span>
    </template>
  </el-dialog>
</template>

<style scoped lang="sass">
.teacher_to_student_box
  position: relative
  display: flex
  flex-direction: column
  align-items: center
  justify-content: center
  margin: 50px auto 20px auto
  width: 80%
  h3
    margin-bottom: 20px
    font-weight: bold
  .item_box
    display: flex
    flex-wrap: wrap
    justify-content: center
    .item
      box-shadow: rgba(0, 0, 0, 0.1) 0 0 10px
      margin: 10px
      padding: 20px
      border-radius: 10px
      width: 220px
      display: flex
      flex-direction: column
      align-items: center
      text-align: center
      background-color: #fff
      transition: transform 0.3s, box-shadow 0.3s
      cursor: pointer
      &:hover
        transform: translateY(-5px)
        box-shadow: rgba(0, 0, 0, 0.2) 0 10px 15px
      .avatar
        height: 100px
        margin-bottom: 10px
        object-fit: cover
      .info
        width: 100%
        .el-button
          margin-top: 10px

/* 对话框内学生详细信息样式 */
.student-detail
  display: flex
  .avatar
    height: 150px
    margin-right: 20px
    object-fit: cover
  .info
    flex: 1
    display: flex
    flex-direction: column
    justify-content: center
    div
      margin-bottom: 10px
      text-align: left
</style>
