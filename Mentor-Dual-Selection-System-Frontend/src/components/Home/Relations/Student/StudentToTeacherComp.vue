<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { Right } from "@element-plus/icons-vue";
import { ElDialog, ElButton } from "element-plus";
import { http } from "@/utils/http";
import { useRouter } from "vue-router";
import axios from "axios";

const userStore = useUserInfoStore();
const router = useRouter();

const userInfoComp = ref({
  fullName: "",
  email: "",
  avatarUrl: "", // 对应 photoUrl
  grade: "",
  class: "",
  researchDirection: "",  // 学生的意向研究方向
  resume: "",
});
const targetTeacher = ref({
  fullName: "",
  teacher_position: "",  // 导师职称
  email: "",
  avatarUrl: "",
  research_direction: "",  // 导师的研究方向
  resume: "",
});
const errorShow = ref(0);
const dialogVisible = ref(false); // 控制对话框的显示
const selectedPerson = ref({}); // 存储选中的导师或学生信息

// 打开详细信息弹窗
function viewDetail(person, role) {
  if (person) {
    if (role === 'teacher') {
      // 导师详细信息
      selectedPerson.value = {
        fullName: person.fullName || "",
        teacher_position: person.teacher_position || "",  // 职称
        email: person.email || "",
        research_direction: person.research_direction || "未提供", // 导师研究方向
        resume: person.resume || "未提供",
        avatarUrl: person.avatarUrl ? `${person.avatarUrl}?t=${new Date().getTime()}` : "",
      };
    } else {
      // 学生详细信息
      selectedPerson.value = {
        fullName: person.fullName || "",
        email: person.email || "",
        grade: person.studentGrade || "", // 正确映射年级
        class: person.studentClass || "", // 正确映射班级
        researchDirection: person.researchDirection || "未提供", // 学生意向研究方向
        resume: person.resume || "未提供",
        avatarUrl: person.photoUrl ? `${person.photoUrl}?t=${new Date().getTime()}` : "",
      };
    }
    dialogVisible.value = true;
  }
}

// 头像加载失败处理函数
function handleImageError(event) {
  event.target.src = "https://via.placeholder.com/100";  // 设置默认图片
}

onMounted(() => {
  // 获取学生的基本信息 (姓名、邮箱等)
  axios({
    url: "/api/user/me",
    method: "GET",
    headers: {
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        userInfoComp.value = {
          fullName: res.data.data.fullName || "",
          email: res.data.data.email || "",
        };
      } else {
        alert("获取学生基本信息失败，请检查网络和登录状态！");
      }
    })
    .catch((err) => {
      console.error(err);
      errorShow.value = err.response ? err.response.status : 500;
    });

  // 获取学生的详细信息 (照片、意向研究方向、简历等)
  axios({
    url: "/student/my-detail",
    method: "GET",
    headers: {
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        userInfoComp.value = {
          ...userInfoComp.value,
          avatarUrl: res.data.data.photoUrl ? `${res.data.data.photoUrl}?t=${new Date().getTime()}` : "",  // 强制刷新图片
          researchDirection: res.data.data.researchDirection || "未提供",  // 学生意向研究方向
          resume: res.data.data.resume || "未提供",
          grade: res.data.data.studentGrade || "",  // 正确映射年级
          class: res.data.data.studentClass || "",  // 正确映射班级
        };
      } else {
        alert("获取学生详细信息失败，请检查网络和登录状态！");
      }
    })
    .catch((err) => {
      console.error(err);
      errorShow.value = err.response ? err.response.status : 500;
    });

  // 获取导师信息 (通过 mentor-student-relations 获取导师uid，再获取详细信息)
  http({
    url: "/application/mentor-student-relations",
    method: "GET",
    headers: {
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  })
    .then((res) => {
      if (res.data.code === 200 && res.data.data.length > 0) {
        const mentorUid = res.data.data[0].mentor.uid;
        return http({
          url: `/search/teacher`,
          method: "GET",
          params: { uid: mentorUid },
          headers: {
            Authorization: "Bearer " + localStorage.getItem("token"),
          },
        });
      } else {
        throw new Error("导师信息未找到");
      }
    })
    .then((res) => {
      if (res.data.code === 200) {
        targetTeacher.value = {
          fullName: res.data.data.fullName || "",
          teacher_position: res.data.data.teacher_position || "",  // 获取职称
          email: res.data.data.email || "",
          avatarUrl: res.data.data.photourl ? `${res.data.data.photourl}?t=${new Date().getTime()}` : "",
          research_direction: res.data.data.research_direction || "未提供",  // 导师研究方向
          resume: res.data.data.resume || "未提供",
        };
      } else {
        alert("获取导师信息失败，请检查网络和登录状态！");
      }
    })
    .catch((err) => {
      console.error(err);
      errorShow.value = err.response ? err.response.status : 500;
    });
});
</script>

<template>
  <div class="container">
    <div class="student_to_teacher_box" v-if="errorShow === 0">
      <h3>您的目标导师</h3>
      <div class="item_box">
        <!-- 左侧学生信息 -->
        <div class="item">
          <img
            class="avatar"
            :src="userInfoComp.avatarUrl || 'https://via.placeholder.com/100'"
            :key="userInfoComp.avatarUrl" 
            @error="handleImageError" 
            alt="avatar"
          />
          <div class="info">
            <div>学生姓名：{{ userInfoComp.fullName }}</div>
            <div>学生邮箱：{{ userInfoComp.email }}</div>
          </div>
          <el-button type="primary" @click="viewDetail(userInfoComp, 'student')">详细信息</el-button>
        </div>
      </div>
      <el-icon :size="48" :color="'#005826'" class="center-icon"><Right /></el-icon>
      <div class="item_box">
        <!-- 右侧导师信息 -->
        <div class="item">
          <img
            class="avatar"
            :src="targetTeacher.avatarUrl || 'https://via.placeholder.com/100'"
            :key="targetTeacher.avatarUrl"  
            @error="handleImageError"  
            alt="avatar"
            style="height: 100px;" 
          />
          <div class="info">
            <div>导师姓名：{{ targetTeacher.fullName }}</div>
            <div>导师职称：{{ targetTeacher.teacher_position }}</div> <!-- 显示职称 -->
            <div>导师邮箱：{{ targetTeacher.email }}</div>
          </div>
          <el-button type="primary" @click="viewDetail(targetTeacher, 'teacher')">详细信息</el-button>
        </div>
      </div>
    </div>
    <div class="student_to_teacher_box" v-else-if="errorShow === 404">
      <h3>您还未选择导师</h3>
    </div>
    <div class="student_to_teacher_box" v-else>
      <h3>获取信息出错，请稍后重试</h3>
    </div>

    <!-- 详细信息对话框 -->
    <el-dialog v-model="dialogVisible" title="详细信息" width="600px">
      <div class="person-detail">
        <img
          class="avatar"
          :src="selectedPerson.avatarUrl || 'https://via.placeholder.com/100'"
          :key="selectedPerson.avatarUrl" 
          @error="handleImageError"  
          alt="头像"
          style="height: 100px;" 
        />
        <div class="info">
          <div><strong>姓名：</strong>{{ selectedPerson.fullName }}</div>
          <div v-if="selectedPerson.teacher_position"><strong>职称：</strong>{{ selectedPerson.teacher_position }}</div> <!-- 导师显示职称 -->
          <div><strong>邮箱：</strong>{{ selectedPerson.email }}</div>
          <div v-if="selectedPerson.grade"><strong>年级：</strong>{{ selectedPerson.grade }}</div> <!-- 学生显示年级 -->
          <div v-if="selectedPerson.class"><strong>班级：</strong>{{ selectedPerson.class }}</div>
          <div>
            <strong v-if="selectedPerson.research_direction">研究方向：</strong>
            <strong v-if="selectedPerson.researchDirection">意向研究方向：</strong>
            {{ selectedPerson.research_direction || selectedPerson.researchDirection }}
          </div>
          <div><strong>个人简介：</strong>{{ selectedPerson.resume }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="sass">
/* 页面整体容器 */
.container
  margin-top: 60px
  overflow-y: auto

.student_to_teacher_box
  display: flex
  justify-content: center
  margin: 50px auto 20px auto
  width: 70%
  position: relative
  flex-direction: row
  h3
    position: absolute
    top: -40px
    font-weight: bold
  .item_box
    display: flex
    align-items: stretch  // 确保左右两边的卡片高度相同
    justify-content: center
    margin: 20px
    .item
      width: 320px
      min-height: 140px
      background: linear-gradient(145deg, #f0f0f0, #fafafa)
      box-shadow: 0px 10px 15px rgba(0, 0, 0, 0.1)
      border-radius: 12px
      padding: 20px
      display: flex
      flex-direction: column
      align-items: center
      transition: transform 0.3s ease
      &:hover
        transform: translateY(-5px)
        box-shadow: 0px 15px 25px rgba(0, 0, 0, 0.15)
      .avatar
        height: 60px
        margin-bottom: 10px
      .info
        flex: 1
        text-align: center
        margin-bottom: 10px

.el-icon
  margin: 100px 20px
  transition: transform 0.3s ease
  &:hover
    transform: rotate(90deg)
</style>
