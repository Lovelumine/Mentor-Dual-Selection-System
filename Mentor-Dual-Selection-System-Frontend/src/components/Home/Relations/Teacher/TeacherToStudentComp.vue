<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { http } from "@/utils/http";
import { getPortraitImageUrl } from "@/utils/media";

interface StudentRelationshipRecord {
  studentId: number;
  fullName: string;
  email?: string | null;
  username?: string | null;
  netid?: string | null;
  grade?: string | null;
  photoUrl?: string | null;
  avatarUrl?: string | null;
  resume?: string | null;
  researchDirection?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  endReason?: string | null;
  operatorName?: string | null;
  current: boolean;
  statusLabel?: string | null;
}

interface TeacherRelationshipHistoryResponse {
  code: number;
  data: {
    currentStudents: StudentRelationshipRecord[];
    historicalStudents: StudentRelationshipRecord[];
  };
}

const router = useRouter();
const loading = ref(false);
const currentStudents = ref<StudentRelationshipRecord[]>([]);
const historicalStudents = ref<StudentRelationshipRecord[]>([]);
const dialogVisible = ref(false);
const selectedStudent = ref<StudentRelationshipRecord | null>(null);

function getStudentAvatar(student: StudentRelationshipRecord) {
  return getPortraitImageUrl(student.photoUrl) || getPortraitImageUrl(student.avatarUrl) || "https://via.placeholder.com/100";
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

function openStudentDetail(student: StudentRelationshipRecord) {
  selectedStudent.value = student;
  dialogVisible.value = true;
}

async function fetchStudentRelationshipHistory() {
  if (!localStorage.getItem("token")) {
    router.push("/");
    return;
  }

  loading.value = true;
  try {
    const res = await http<TeacherRelationshipHistoryResponse>({
      url: "/application/teacher/student-relationship-history",
      method: "GET",
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
        Accept: "*/*",
      },
    });

    if (res.data.code !== 200) {
      throw new Error("获取学生关系信息失败");
    }

    currentStudents.value = res.data.data.currentStudents || [];
    historicalStudents.value = res.data.data.historicalStudents || [];
  } catch (error: any) {
    console.error(error);
    const message = error?.response?.data?.data?.error || error?.message || "获取学生关系信息失败";
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchStudentRelationshipHistory();
});
</script>

<template>
  <div class="teacher_relationship_box" v-loading="loading">
    <section class="current_students_section">
      <h3>您指导的学生</h3>
      <div v-if="currentStudents.length > 0" class="current_students_grid">
        <article v-for="student in currentStudents" :key="student.studentId" class="student_card">
          <img :src="getStudentAvatar(student)" alt="学生头像" class="avatar" />
          <div class="info">
            <div class="student_name">{{ student.fullName }}</div>
            <div>{{ student.email || "未填写邮箱" }}</div>
            <div>学号：{{ student.username || student.netid || "未填写" }}</div>
            <div>年级：{{ student.grade || "未填写" }}</div>
            <div>绑定时间：{{ formatDateTime(student.startDate) }}</div>
            <el-button type="primary" @click="openStudentDetail(student)">详细信息</el-button>
          </div>
        </article>
      </div>
      <div v-else class="empty_state">
        当前还没有指导中的学生。
      </div>
    </section>

    <section v-if="historicalStudents.length > 0" class="history_section">
      <h3>历史学生</h3>
      <el-table :data="historicalStudents" row-key="studentId" stripe class="history_table">
        <el-table-column prop="fullName" label="学生姓名" min-width="140" />
        <el-table-column label="学号" min-width="140">
          <template #default="{ row }">
            {{ row.username || row.netid || "未填写" }}
          </template>
        </el-table-column>
        <el-table-column label="年级" min-width="100">
          <template #default="{ row }">
            {{ row.grade || "未填写" }}
          </template>
        </el-table-column>
        <el-table-column label="绑定时间" min-width="168">
          <template #default="{ row }">
            {{ formatDateTime(row.startDate) }}
          </template>
        </el-table-column>
        <el-table-column label="解除时间" min-width="168">
          <template #default="{ row }">
            {{ formatDateTime(row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column label="处理说明" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.endReason || "历史学生关系保留记录" }}</div>
            <div v-if="row.operatorName" class="history_operator">
              操作人：{{ row.operatorName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="详情" width="120" align="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openStudentDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="学生详细信息" width="600px">
      <template v-if="selectedStudent">
        <div class="student_detail">
          <img :src="getStudentAvatar(selectedStudent)" alt="学生头像" class="avatar avatar--large" />
          <div class="info">
            <div><strong>姓名：</strong>{{ selectedStudent.fullName }}</div>
            <div><strong>学号：</strong>{{ selectedStudent.username || selectedStudent.netid || "未填写" }}</div>
            <div><strong>邮箱：</strong>{{ selectedStudent.email || "未填写" }}</div>
            <div><strong>年级：</strong>{{ selectedStudent.grade || "未填写" }}</div>
            <div><strong>意向研究方向：</strong>{{ selectedStudent.researchDirection || "未提供" }}</div>
            <div><strong>个人简介：</strong>{{ selectedStudent.resume || "未提供" }}</div>
            <div><strong>绑定时间：</strong>{{ formatDateTime(selectedStudent.startDate) }}</div>
            <div v-if="selectedStudent.endDate"><strong>解除时间：</strong>{{ formatDateTime(selectedStudent.endDate) }}</div>
            <div v-if="selectedStudent.endReason"><strong>处理说明：</strong>{{ selectedStudent.endReason }}</div>
            <div v-if="selectedStudent.operatorName"><strong>操作人：</strong>{{ selectedStudent.operatorName }}</div>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="sass">
.teacher_relationship_box
  width: min(1120px, calc(100% - 40px))
  margin: 40px auto 0 auto
  color: var(--app-text-primary)

.current_students_section,
.history_section
  margin-bottom: 28px

h3
  margin: 0 0 16px 0
  color: var(--app-text-primary)

.current_students_grid
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr))
  gap: 16px

.student_card
  display: flex
  flex-direction: column
  align-items: center
  gap: 14px
  padding: 20px
  border-radius: 16px
  background: linear-gradient(145deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
  border: 1px solid var(--app-border)
  box-shadow: 0 14px 28px var(--app-shadow)

.avatar
  width: 100px
  height: 100px
  object-fit: cover
  border-radius: 14px
  border: 1px solid var(--app-border)

.avatar--large
  width: 120px
  height: 120px

.info
  width: 100%
  display: flex
  flex-direction: column
  gap: 8px
  color: var(--app-text-primary)

.student_name
  font-size: 20px
  font-weight: 700

.empty_state
  padding: 30px 20px
  text-align: center
  border: 1px dashed var(--app-border-strong)
  border-radius: 16px
  background: var(--app-surface)
  color: var(--app-text-secondary)

.history_table
  border: 1px solid var(--app-border)
  border-radius: 16px
  overflow: hidden

.history_operator
  margin-top: 4px
  color: var(--app-text-secondary)
  font-size: 13px

.student_detail
  display: flex
  gap: 20px
  color: var(--app-text-primary)

@media (max-width: 768px)
  .teacher_relationship_box
    width: calc(100% - 24px)

  .student_detail
    flex-direction: column
</style>
