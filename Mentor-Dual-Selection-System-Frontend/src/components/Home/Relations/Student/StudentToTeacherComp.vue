<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { Right } from "@element-plus/icons-vue";
import { ElDialog, ElButton, ElMessage } from "element-plus";
import { http } from "@/utils/http";

import axios from "axios";

const userStore = useUserInfoStore();

interface UserInfo {
  fullName: string;
  email: string;
  avatarUrl: string;
  grade: string;
  researchDirection: string;
  resume: string;
}

interface TeacherInfo {
  fullName: string;
  teacher_position: string;
  email: string;
  avatarUrl: string;
  research_direction: string;
  resume: string;
}

interface MentorHistoryItem {
  mentorId: number;
  mentorName: string;
  mentorEmail: string;
  photoUrl: string;
  teacherPosition: string;
  researchDirection: string;
  resume: string;
  startDate: string | null;
  endDate: string | null;
  endReason: string | null;
  operatorName: string | null;
  current: boolean;
}

interface SelectedPerson {
  fullName: string;
  email: string;
  avatarUrl: string;
  teacher_position?: string; // 导师特有属性
  research_direction?: string; // 导师特有属性
  grade?: string; // 学生特有属性
  researchDirection?: string; // 学生特有属性
  resume: string;
}

const userInfoComp = ref<UserInfo>({
  fullName: "",
  email: "",
  avatarUrl: "",
  grade: "",
  researchDirection: "",
  resume: "",
});

const targetTeacher = ref<TeacherInfo>({
  fullName: "",
  teacher_position: "",
  email: "",
  avatarUrl: "",
  research_direction: "",
  resume: "",
});

const errorShow = ref(0);
const dialogVisible = ref(false); // 控制对话框的显示
const selectedPerson = ref<SelectedPerson>({} as SelectedPerson); // 存储选中的导师或学生信息
const mentorHistory = ref<MentorHistoryItem[]>([]);
const currentMentorStartDate = ref<string | null>(null);

// 打开详细信息弹窗
function viewDetail(person: any, role: string) {
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
        researchDirection: person.researchDirection || "未提供", // 学生意向研究方向
        resume: person.resume || "未提供",
        avatarUrl: person.photoUrl ? `${person.photoUrl}?t=${new Date().getTime()}` : "",
      };
    }
    dialogVisible.value = true;
  }
}

// 头像加载失败处理函数
function handleImageError(event: Event) {
  (event.target as HTMLImageElement).src = "https://via.placeholder.com/100";  // 设置默认图片
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return date
    .toLocaleString("zh-CN", {
      hour12: false,
      timeZone: "Asia/Shanghai",
    })
    .replace(/\//g, "-");
}

onMounted(() => {
  userStore.fetchUserInfo();
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
          ...userInfoComp.value,
          fullName: res.data.data.fullName || "",
          email: res.data.data.email || "",
        };
        // 获取学生的详细信息 (照片、意向研究方向、简历等)
        axios({
          url: "/student/my-detail",
          method: "GET",
          headers: {
            Authorization: "Bearer " + localStorage.getItem("token"),
          },
        }).then((res) => {
          if (res.data.code === 200) {
            userInfoComp.value = {
              ...userInfoComp.value,
              avatarUrl: res.data.data.photoUrl ? `${res.data.data.photoUrl}?t=${new Date().getTime()}` : "",  // 强制刷新图片
              researchDirection: res.data.data.researchDirection || "未提供",  // 学生意向研究方向
              resume: res.data.data.resume || "未提供",
              grade: res.data.data.studentGrade || "",  // 正确映射年级
            };
          } else {
            ElMessage.warning("获取学生详细信息失败，已使用基础信息继续显示");
          }
        }).catch((err) => {
            console.error(err);
            ElMessage.warning("获取学生详细信息失败，已使用基础信息继续显示");
          })
      } else {
        ElMessage.error("获取学生基本信息失败，请检查网络和登录状态！");
      }
    })
    .catch((err) => {
      console.error(err);
      ElMessage.error("获取学生基本信息失败，请检查网络和登录状态！");
    });

  // 获取当前导师和历史导师关系
  http({
    url: "/application/student/relationship-history",
    method: "GET",
    headers: {
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        const currentRelationship = res.data.data.currentRelationship;
        mentorHistory.value = res.data.data.historicalRelationships || [];

        if (currentRelationship) {
          currentMentorStartDate.value = currentRelationship.startDate || null;
          targetTeacher.value = {
            fullName: currentRelationship.mentorName || "",
            teacher_position: currentRelationship.teacherPosition || "",
            email: currentRelationship.mentorEmail || "",
            avatarUrl: currentRelationship.photoUrl
              ? `${currentRelationship.photoUrl}?t=${new Date().getTime()}`
              : "",
            research_direction: currentRelationship.researchDirection || "未提供",
            resume: currentRelationship.resume || "未提供",
          };
          errorShow.value = 0;
        } else {
          currentMentorStartDate.value = null;
          targetTeacher.value = {
            fullName: "",
            teacher_position: "",
            email: "",
            avatarUrl: "",
            research_direction: "",
            resume: "",
          };
          errorShow.value = 404;
        }
      } else {
        ElMessage.error("获取导师信息失败，请检查网络和登录状态！");
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
            :src="userInfoComp.avatarUrl"
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
      <el-icon :size="48" :color="'var(--app-brand)'" class="center-icon"><Right /></el-icon>
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
            <div>绑定时间：{{ formatDateTime(currentMentorStartDate) }}</div>
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

    <div class="history_box" v-if="mentorHistory.length > 0">
      <h3>历史导师</h3>
      <el-table :data="mentorHistory" stripe class="history_table">
        <el-table-column prop="mentorName" label="导师姓名" min-width="140" />
        <el-table-column prop="teacherPosition" label="导师职称" min-width="120" />
        <el-table-column label="绑定时间" min-width="168">
          <template #default="scope">
            {{ formatDateTime(scope.row.startDate) }}
          </template>
        </el-table-column>
        <el-table-column label="解除时间" min-width="168">
          <template #default="scope">
            {{ formatDateTime(scope.row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column label="处理说明" min-width="280" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ scope.row.endReason || "历史导师关系保留记录" }}</div>
            <div v-if="scope.row.operatorName" class="history_operator">
              操作人：{{ scope.row.operatorName }}
            </div>
          </template>
        </el-table-column>
      </el-table>
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
          <div>
            <strong v-if="selectedPerson.research_direction">研究方向：</strong>
            <strong v-else-if="selectedPerson.researchDirection">意向研究方向：</strong>
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
  color: var(--app-text-primary)

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
    color: var(--app-text-primary)
  .item_box
    display: flex
    align-items: stretch  // 确保左右两边的卡片高度相同
    justify-content: center
    margin: 20px
    .item
      width: 320px
      min-height: 140px
      background: linear-gradient(145deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
      box-shadow: 0px 14px 28px var(--app-shadow)
      border: 1px solid var(--app-border)
      border-radius: 16px
      padding: 20px
      display: flex
      flex-direction: column
      align-items: center
      transition: transform 0.3s ease
      &:hover
        transform: translateY(-5px)
        box-shadow: 0px 20px 36px var(--app-shadow-strong)
      .avatar
        height: 100px
        margin-bottom: 10px
        border-radius: 12px
      .info
        flex: 1
        text-align: center
        margin-bottom: 10px
        color: var(--app-text-primary)

.el-icon
  margin: 100px 20px
  transition: transform 0.3s ease
  &:hover
    transform: rotate(90deg)

.history_box
  width: 70%
  margin: 20px auto 0 auto
  h3
    margin-bottom: 16px
    color: var(--app-text-primary)
  .history_table
    border: 1px solid var(--app-border)
    border-radius: 16px
    overflow: hidden
  .history_operator
    margin-top: 4px
    color: var(--app-text-secondary)
    font-size: 13px

.person-detail
  color: var(--app-text-primary)
  .avatar
    border-radius: 12px
</style>
