<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import {
  ElPagination,
  ElSelect,
  ElOption,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
} from "element-plus";
import { http, httpStudent } from "@/utils/http";
import axios from "axios";

// 定义学生和导师的信息类型
interface StudentInfo {
  studentGrade: string;
  studentClass: string;
  resume: string;
}

interface TeacherInfo {
  uid: number;
  fullName: string;
  teacherposition: string;
  research_direction: string;
  email: string;
  photourl: string;
  resume: string;
  avatarUrl: string;
}

// 学生和教师的信息
const studentInfo = ref<StudentInfo | null>(null); // 类型为 StudentInfo 或 null
const allTeacherDetailsList = ref<TeacherInfo[]>([]); // 类型为 TeacherInfo 数组
const currentPage = ref(1);
const pageSize = ref(6);
const positions = ["教授", "副教授", "助理教授", "博士后", "学术顾问", "客座/兼职教授"];
const selectedPosition = ref("");
const dialogVisible = ref(false);
const applicationDialogVisible = ref(false);
const selectReason = ref<string | null>(null);
const selectTeacherUid = ref<number | null>(null);
const selectedTeacher = ref<TeacherInfo | null>(null); // 类型为 TeacherInfo 或 null
const isGeneratingReason = ref(false);

// 获取学生信息
const fetchStudentInfo = () => {
  httpStudent({
    url: "/my-detail",
    method: "GET",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
  }).then((res) => {
    if (res.data.code === 200) {
      studentInfo.value = res.data.data;
    }
  });
};

// 生成AI申请理由功能
const generateAIReason = async () => {
  if (!studentInfo.value || !selectedTeacher.value) {
    alert("无法获取学生或导师信息！");
    return;
  }
  isGeneratingReason.value = true;
  try {
    const prompt = `根据以下信息生成申请理由：
    学生年级: ${studentInfo.value.studentGrade}
    学生班级: ${studentInfo.value.studentClass}
    学生简介: ${studentInfo.value.resume}
    导师姓名: ${selectedTeacher.value.fullName}
    导师职称: ${selectedTeacher.value.teacherposition}
    导师研究方向: ${selectedTeacher.value.research_direction}`;

    const response = await axios.post(
      "https://api.lqqq.ltd/v1/chat/completions",
      {
        model: "gpt-3.5-turbo-0125",
        prompt: prompt,
        max_tokens: 2000,
        temperature: 0.7,
      },
      {
        headers: {
          Authorization: `Bearer sk-F0G0uD0m1tVmk5OUAa7bF90e0d824d44Ad65CdC9286aFc2a`,
        },
      }
    );

    if (response.status === 200 && response.data.choices) {
      selectReason.value = response.data.choices[0].message.content.trim();
    } else {
      alert("生成申请理由失败，请重试！");
    }
  } catch (error) {
    console.error(error);
    alert("生成申请理由失败，请重试！");
  } finally {
    isGeneratingReason.value = false;
  }
};

// 获取导师信息
onMounted(() => {
  fetchStudentInfo();
  http({
    url: "/search/teachers",
    method: "GET",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        allTeacherDetailsList.value = res.data.data;
        httpStudent({
          url: "/teachers",
          method: "GET",
          headers: {
            Accept: "*/*",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        }).then((res2) => {
          if (res2.data.code === 200) {
            const tdl = res2.data.data;
            allTeacherDetailsList.value.forEach((teacher) => {
              const match = tdl.find((t: TeacherInfo) => t.uid === teacher.uid);
              if (match) {
                Object.assign(teacher, match);
              }
            });
          }
        });
      } else {
        alert("导师信息获取失败！");
      }
    });
});

// 处理筛选条件
const onPositionChange = () => {
  currentPage.value = 1;
};

const resetFilter = () => {
  selectedPosition.value = "";
  currentPage.value = 1;
};

// 过滤导师
const filteredTeachers = computed(() => {
  if (!selectedPosition.value) {
    return allTeacherDetailsList.value;
  }
  return allTeacherDetailsList.value.filter(
    (teacher) => teacher.teacherposition === selectedPosition.value
  );
});

// 分页处理
const paginatedTeachers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredTeachers.value.slice(start, end);
});

const showDetails = (teacher: TeacherInfo) => {
  selectedTeacher.value = teacher;
  dialogVisible.value = true;
};

const showApplicationDialog = (teacher: TeacherInfo) => {
  selectedTeacher.value = teacher;
  selectTeacherUid.value = teacher.uid;
  applicationDialogVisible.value = true;
};

// 提交申请
const checkApplication = () => {
  if (!selectReason.value) {
    alert("请填写您的理由！");
    return;
  }
  http({
    url: "/application/submit",
    method: "POST",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
    data: {
      mentorId: selectTeacherUid.value,
      reason: selectReason.value,
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        alert("申请提交成功，请等待导师处理！\n期间您不可选择其他导师！");
        applicationDialogVisible.value = false;
      } else {
        alert(res.data.data.error);
      }
    })
    .catch((error) => {
      if (error.response && error.response.data && error.response.data.data) {
        alert(error.response.data.data.error);
      } else {
        alert("提交申请时出现未知错误，请稍后重试！");
      }
    });
};

// 处理分页页码变化
const handlePageChange = (page: number) => {
  currentPage.value = page;
};
</script>

<template>
  <!-- 固定的标题 -->
  <div class="title">
    <span>选择导师</span>
  </div>

  <!-- 主内容 -->
  <div class="main-content">
    <!-- 筛选控件和分页控件 -->
    <div class="top-controls">
      <div class="filter-controls">
        <el-select
            v-model="selectedPosition"
            placeholder="选择职称"
            @change="onPositionChange"
            clearable
        >
          <el-option
              v-for="position in positions"
              :key="position"
              :label="position"
              :value="position"
          ></el-option>
        </el-select>
        <el-button @click="resetFilter" type="warning">重置</el-button>
      </div>
      <!-- 分页控件 -->
      <div class="pagination-controls">
        <el-pagination
            background
            layout="prev, pager, next"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="filteredTeachers.length"
            @current-change="handlePageChange"
        >
        </el-pagination>
      </div>
    </div>

    <!-- 内容的容器 -->
    <div class="content-wrapper">
      <div class="container">
        <div
            class="teacher_resume_item"
            v-for="(item, index) in paginatedTeachers"
            :key="index"
        >
          <div class="photo">
            <img :src="item.photourl" alt="photo" />
          </div>
          <div class="item_box">
            <span class="fullname">{{ item.fullName }}</span>
            <span class="professional">职位：{{ item.teacherposition }}</span>
            <span class="email">电子邮箱：{{ item.email }}</span>

            <hr class="hea_con_hr" />
            <div class="detail_box">
              <div class="resume">研究方向：{{ item.research_direction }}</div>
              <div class="resume">
                简历：{{ item.resume.length > 50 ? item.resume.slice(0, 50) + '...' : item.resume }}
              </div>

              <!-- 显示详情按钮 -->
              <el-button type="primary" size="small" @click="showDetails(item)">了解更多</el-button>
              <el-button type="primary" size="small" @click="showApplicationDialog(item)">选择导师</el-button>
            </div>
            <div class="avatar">
              <img :src="item.avatarUrl" alt="avatar" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 详情对话框 -->
  <el-dialog v-model="dialogVisible" :title="selectedTeacher?.fullName">
    <div v-if="selectedTeacher">
      <p>职称：{{ selectedTeacher.teacherposition }}</p>
      <p>电子邮箱：{{ selectedTeacher.email }}</p>
      <p>研究方向：{{ selectedTeacher.research_direction }}</p>
      <p>简历：{{ selectedTeacher.resume }}</p>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 申请导师对话框 -->
  <el-dialog v-model="applicationDialogVisible" :title="`选择导师：${selectedTeacher?.fullName}`">
    <el-form @submit.prevent="checkApplication">
      <el-form-item label="申请理由（必填）：">
        <el-input
          type="textarea"
          v-model="selectReason"
          placeholder="请简述"
          :autosize="{ minRows: 2, maxRows: 10 }"
        />
      </el-form-item>

      <el-button type="primary" native-type="submit">提交申请</el-button>
      <el-button v-if="!isGeneratingReason" type="primary" @click="generateAIReason">AI生成申请理由</el-button>
      <el-button v-if="isGeneratingReason" type="primary" disabled>生成中...</el-button>
    </el-form>
  </el-dialog>

  <div style="height: 100px;"></div>
</template>



<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
  position: fixed
  top: 60px
  left: 200px
  z-index: 1000

.main-content
  margin-top: 80px // 确保主内容不被固定的标题遮挡

.top-controls
  display: flex
  justify-content: space-between
  align-items: center
  margin: 20px
  .filter-controls
    display: flex
    align-items: center
    gap: 10px
    .el-select
      width: 200px
  .pagination-controls
    .el-pagination
      display: flex
      align-items: center

.content-wrapper
  padding: 0 20px

.container
  display: flex
  flex-wrap: wrap
  padding-left: 0vw
  gap: 100px
  row-gap: 50px
  padding-top: 20px

.teacher_resume_item
  width: 500px
  height: 300px
  position: relative
  .item_box
    width: 100%
    height: 250px
    bottom: 0
    position: absolute
    box-shadow: #00582680 0 0 0px
    border-radius: 10px
    .hea_con_hr
      position: absolute
      top: 25%
      width: 90%
      left: 50%
      transform: translateX(-50%)
    .fullname
      position: absolute
      top: -13%
      left: 22%
      font-size: 18px
    .email
      position: absolute
      left: 22%
      display: flex
      align-items: center
      top: 2%
    .professional
      position: absolute
      left: 22%
      top: 12%
    .detail_box
      position: absolute
      top: 30%
      left: 5%
    .avatar
      z-index: -1
      width: 100%
      height: 100%
      overflow: hidden
      position: relative
      border-radius: 10px
      img
        position: absolute
        top: 50%
        left: 50%
        transform: translate(-50%, -50%)
        width: 100%
        opacity: .2
  .photo
    box-shadow: #00000030 0 0 5px
    background-color: white
    border-radius: 10px
    z-index: 1
    left: 5%
    width: 75px
    height: 99px
    overflow: hidden
    position: absolute
    img
      width: 100%
</style>
