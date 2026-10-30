<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { http } from "@/utils/http";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { ElMessage, ElMessageBox } from "element-plus";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";
const userStore = useUserInfoStore();
const { isMobile, isTablet } = useResponsiveLayout();

const pendingList = ref<any[]>([]);
const userRole = ref<string | null>(null);
const dialogVisible = ref<boolean>(false);
const studentInfoVisible = ref<boolean>(false);
const studentInfo = ref<Record<string, any>>({});
const pendingUtilForm = ref({
  applicationId: null as number | null,
  approved: null as boolean | null,
  rejectionReason: null as string | null,
});

const studentApplicationInfo = ref<Record<string, any>>({});
const rejectDialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "84vw" : "500px"));
const studentInfoDialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "88vw" : "600px"));

// Checkbox state to allow skipping rejection reason
const skipRejectionReason = ref<boolean>(false);

const handleClose = (done: () => void) => {
  ElMessageBox.confirm("您输入的信息将会保留，直到您下次点开该弹窗！")
    .then(() => {
      done();
    })
    .catch(() => {
      // catch error
    });
};

const handleStudentInfoClose = (done: () => void) => {
  done();
};

const canAdminHandleSelectedPending = computed(() => {
  return (
    userRole.value === "ADMIN" &&
    scopeRowGetStatus(studentApplicationInfo.value) &&
    !isAdminUnbindRecord(studentApplicationInfo.value)
  );
});

function translateStatus(status: string | null | undefined) {
  switch (status) {
    case "REJECTED":
      return "已拒绝";
    case "PENDING":
      return "待审核";
    case "ACCEPTED":
      return "已通过";
    case "ADMIN_UNBOUND":
      return "已解除";
    default:
      return status || "未知状态";
  }
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

function isAdminUnbindRecord(row: any) {
  return row.recordType === "ADMIN_UNBIND";
}

function handleAccept(index: number, row: any) {
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = true;
  pendingUtilForm.value.rejectionReason = null;
  http({
    url: "/application/approve",
    method: "POST",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
    data: {
      applicationId: pendingUtilForm.value.applicationId,
      approved: pendingUtilForm.value.approved,
      rejectionReason: pendingUtilForm.value.rejectionReason || null,
    },
  })
    .then((res) => {
      if (res.status === 200 || res.data.code === 200) {
        ElMessage.success("该申请处理完成！");
        studentInfoVisible.value = false;
        fetchApplicationList();
      } else {
        ElMessage.error(res.data.error || "处理申请失败");
      }
    })
    .catch((err) => {
      const errorMessage =
        err?.response?.data?.data?.error ||
        err?.response?.data?.error ||
        "处理申请失败";
      ElMessage.error(errorMessage);
    });
}

function handleReject(index: number, row: any) {
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = false;
  pendingUtilForm.value.rejectionReason = null;
  skipRejectionReason.value = false;
  dialogVisible.value = true;
}

// Handle rejection with an optional reason
function checkReject() {
  // If skipRejectionReason is checked or no reason provided, set default reason
  if (skipRejectionReason.value || !pendingUtilForm.value.rejectionReason) {
    pendingUtilForm.value.rejectionReason =
      userRole.value === "ADMIN"
        ? "管理员未填写拒绝理由"
        : "导师未填写拒绝理由";
  }

  http({
    url: "/application/approve",
    method: "POST",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
    data: {
      applicationId: pendingUtilForm.value.applicationId,
      approved: pendingUtilForm.value.approved,
      rejectionReason: pendingUtilForm.value.rejectionReason,
    },
  })
    .then((res) => {
      if (res.status === 200 || res.data.code === 200) {
        ElMessage.success("该申请处理完成！");
        dialogVisible.value = false;
        studentInfoVisible.value = false;
        fetchApplicationList();
      } else {
        ElMessage.error(res.data.error || "处理申请失败");
      }
    })
    .catch((err) => {
      const errorMessage =
        err?.response?.data?.data?.error ||
        err?.response?.data?.error ||
        "处理申请失败";
      ElMessage.error(errorMessage);
    });
}

function scopeRowGetStatus(row: any) {
  return row.status === "PENDING" && !isAdminUnbindRecord(row);
}

function getProcessStatusLabel(row: any) {
  if (isAdminUnbindRecord(row)) {
    return row.processingStatusLabel || "管理员解除";
  }
  if (row.status === "PENDING") {
    return userRole.value === "ADMIN" ? "待审核，可在详情中处理" : "待处理";
  }
  return row.processingStatusLabel || "已完成";
}

function handleViewStudentInfo(row: any) {
  getStudentInfo(row.studentId)
    .then((res) => {
      if (res.data.code === 200) {
        studentInfo.value = res.data.data;
        studentApplicationInfo.value = row;
        studentInfoVisible.value = true;
      } else {
        ElMessage.error("获取学生详情失败！");
      }
    })
    .catch((err) => {
      console.error(err);
      ElMessage.error("请求学生详情出错或当前学生未完善详情信息！");
    });
}

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

function fetchApplicationList() {
  http({
    url: "/application/pending",
    method: "GET",
    headers: {
      Accept: "*/*",
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
    params: {
      Authorization: localStorage.getItem("token"),
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        pendingList.value = (res.data.data || []).map((item: any) => ({
          ...item,
          statusCN: item.statusCN || translateStatus(item.status),
        }));
      } else {
        ElMessage.error("获取申请列表失败，请联系管理员！");
      }
    })
    .catch((err) => {
      console.error(err);
      ElMessage.error("出现错误，请重新登录！");
    });
}

onMounted(() => {
  userStore.fetchUserInfo();
  fetchApplicationList();
});

watch(
  () => userStore.userInfo,
  (newValue) => {
    userRole.value = newValue.role || null;
  }
);
</script>

<template>
  <!-- 拒绝理由弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    title="拒绝理由"
    :width="rejectDialogWidth"
    :before-close="handleClose"
  >
    <el-input
      type="textarea"
      v-model="pendingUtilForm.rejectionReason"
      placeholder="拒绝理由（选填）"
      :autosize="{ minRows: 2, maxRows: 5 }"
      style="width: 100%;"
      :disabled="skipRejectionReason" 
    />
    <el-checkbox v-model="skipRejectionReason">不填写拒绝理由</el-checkbox>
    <template #footer>
      <div class="dialog-footer">
        <button class="button" @click="dialogVisible = false">关闭</button>
        <button class="button" type="submit" @click="checkReject">提交</button>
      </div>
    </template>
  </el-dialog>

  <!-- 学生详情弹窗 -->
  <el-dialog
    v-model="studentInfoVisible"
    title="学生详情"
    :width="studentInfoDialogWidth"
    :before-close="handleStudentInfoClose"
  >
    <div class="student-info-card">
      <img :src="studentInfo.photourl" alt="头像" class="avatar" />
      <div class="info">
        <p><strong>姓名：</strong>{{ studentInfo.fullName }}</p>
        <p><strong>学号：</strong>{{ studentInfo.username }}</p>
        <p><strong>年级：</strong>{{ studentInfo.grade }}</p>
        <p><strong>意向研究方向：</strong>{{ studentInfo.research_direction }}</p>
        <p><strong>邮箱：</strong>{{ studentInfo.email }}</p>
        <p><strong>个人简介：</strong>{{ studentInfo.resume }}</p>
        <p>
          <strong>{{ studentApplicationInfo.recordType === 'ADMIN_UNBIND' ? '记录说明：' : '申请理由：' }}</strong>
          {{ studentApplicationInfo.applicationReason || "无" }}
        </p>
        <p><strong>申请状态：</strong>{{ studentApplicationInfo.statusCN }}</p>
        <p><strong>申请时间：</strong>{{ formatDateTime(studentApplicationInfo.applicationDate) }}</p>
        <p><strong>处理时间：</strong>{{ formatDateTime(studentApplicationInfo.decisionDate) }}</p>
        <p v-if="studentApplicationInfo.operatorName">
          <strong>操作人：</strong>{{ studentApplicationInfo.operatorName }}
        </p>
        <p v-if="studentApplicationInfo.recordType !== 'ADMIN_UNBIND'">
          <strong>拒绝理由：</strong>{{ studentApplicationInfo.rejectionReason || "无" }}
        </p>
        <div v-if="canAdminHandleSelectedPending" class="detail-action-row">
          <button class="button" @click="handleAccept(0, studentApplicationInfo)">
            同意
          </button>
          <button
            class="button reject-button"
            @click="handleReject(0, studentApplicationInfo)"
          >
            拒绝
          </button>
        </div>
      </div>
    </div>
  </el-dialog>


  <div class="title">
    <span>学生申请</span>
  </div>
  <div class="container">
    <div class="title">
      <span>所有申请</span>
    </div>
    <div class="table_shell">
      <p class="table_scroll_hint">左右滑动表格可查看完整申请信息</p>
      <el-table :data="pendingList" stripe class="table">
        <el-table-column :label="userRole === 'ADMIN' ? '记录编号' : '申请编号'" prop="id" min-width="110" />
        <el-table-column prop="studentName" label="学生姓名" min-width="130" />
        <el-table-column
          v-if="userRole === 'ADMIN'"
          prop="teacherName"
          label="导师姓名"
          min-width="130"
        />
        <el-table-column prop="applicationReason" label="申请理由 / 记录说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="申请时间" min-width="168">
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.applicationDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理时间" min-width="168">
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.decisionDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusCN" label="当前状态" min-width="120" />
        <el-table-column label="详细信息" width="120" fixed="right">
          <template #default="scope">
            <button class="button" @click="handleViewStudentInfo(scope.row)">
              查看
            </button>
          </template>
        </el-table-column>
        <el-table-column
          v-if="userRole === 'TEACHER' || userRole === 'ADMIN'"
          label="处理状态"
          min-width="180"
        >
          <template #default="scope">
            <div v-if="userRole === 'TEACHER' && scopeRowGetStatus(scope.row)" class="process_actions">
              <button
                class="button"
                @click="handleAccept(scope.$index, scope.row)"
              >
                同意
              </button>
              <button
                class="button"
                @click="handleReject(scope.$index, scope.row)"
              >
                拒绝
              </button>
            </div>
            <div v-else>
              <span>{{ getProcessStatusLabel(scope.row) }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
  <div class="spacer"></div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  min-height: 60px
  background-color: var(--app-surface-muted)
  display: flex
  align-items: center
  padding: 0 20px
  font-size: 20px
  color: var(--app-text-primary)
.container
  padding: 0 0 24px 0
  .title
    width: 100%
    min-height: 40px
    background-color: var(--app-surface-muted)
    display: flex
    align-items: center
    padding: 0 20px
    font-size: 16px
    margin-bottom: 20px
    color: var(--app-text-primary)
  .table
    width: 100%
    min-width: 0
    .button
      margin: 0
    .button:last-child
      margin-left: 10px
      border: 1px solid #bd0000
      background-color: #bd000000
      color: #bd0000
    .button:last-child:hover
      background-color: #bd0000
      color: white
.button
  background-color: #005826
  border: 1px solid #005826
  color: white
  width: 60px
  height: 32px
  border-radius: 5px
  font-size: 15px
  transition: .3s ease
  margin-right: 10px
.button:hover
  cursor: pointer
  background-color: #0f7e3f
  border: 1px solid #0f7e3f
.table_shell
  width: 100%
  overflow-x: auto
  padding: 0 1%
  -webkit-overflow-scrolling: touch

.table_scroll_hint
  display: none
input
  width: 320px
  height: 32px
  transition: .3s ease
  border: 1px solid #989898
  border-radius: 5px
input:hover
  border: 1px solid #005826
input:focus
  outline: none
  box-shadow: #005826 0 0 5px
  border: 1px #005826 solid
.student-info-card
  display: flex
  gap: 20px
  .avatar
    height: 120px
    width: 100px
    object-fit: cover
    border-radius: 12px
  .info
    min-width: 0
    p
      font-size: 16px
      margin: 5px 0
    .detail-action-row
      margin-top: 18px
      display: flex
      gap: 12px
    .reject-button
      border: 1px solid #bd0000
      background-color: #bd000000
      color: #bd0000
    .reject-button:hover
      background-color: #bd0000
      color: white
.spacer
  height: 100px

.process_actions
  display: flex
  flex-wrap: wrap
  gap: 10px

@media (max-width: 980px)
  .container .table
    min-width: 0

@media (max-width: 768px)
  .title
    min-height: 56px
    padding: 0 16px
    font-size: 18px

  .container .title
    min-height: 48px
    padding: 0 16px
    margin-bottom: 16px

  .table_shell
    padding: 0

  .table_scroll_hint
    display: block
    margin: 0 12px 8px
    color: var(--app-text-secondary)
    font-size: 12px

  .student-info-card
    flex-direction: column

  .student-info-card .avatar
    align-self: center

  .student-info-card .info .detail-action-row
    flex-direction: column

  .dialog-footer
    width: 100%
    flex-direction: column-reverse

  .dialog-footer .button
    width: 100%
    margin-right: 0
</style>
