<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { http } from "@/utils/http";
import { useRouter } from "vue-router";
const router = useRouter();
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { ElMessageBox } from "element-plus";
const userStore = useUserInfoStore();

const pendingList = ref<any[]>([]);
const allUser = ref<any[]>([]);
const allStudent = ref<any[]>([]);
const allTeacher = ref<any[]>([]);
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

function handleAccept(index: number, row: any) {
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = true;
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
        alert("该申请处理完成！");
        fetchApplicationList();
      } else {
        alert(res.data.error);
      }
    })
    .catch((err) => {
      alert(JSON.parse(err.request.responseText).data.error);
    });
}

function handleReject(index: number, row: any) {
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = false;
  dialogVisible.value = true;
}

// Handle rejection with an optional reason
function checkReject() {
  // If skipRejectionReason is checked or no reason provided, set default reason
  if (skipRejectionReason.value || !pendingUtilForm.value.rejectionReason) {
    pendingUtilForm.value.rejectionReason = "导师未填写拒绝理由";
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
        alert("该申请处理完成！");
        dialogVisible.value = false;
        fetchApplicationList();
      } else {
        alert(res.data.error);
      }
    })
    .catch((err) => {
      alert(JSON.parse(err.request.responseText).data.error);
    });
}

function scopeIndexGetStatus(index: number) {
  return pendingList.value[index].status === "PENDING";
}

function handleViewStudentInfo(row: any) {
  getStudentInfo(row.studentId)
    .then((res) => {
      if (res.data.code === 200) {
        studentInfo.value = res.data.data;
        studentApplicationInfo.value = row;
        studentInfoVisible.value = true;
      } else {
        alert("获取学生详情失败！");
      }
    })
    .catch((err) => {
      console.error(err);
      alert("请求学生详情出错或当前学生未完善详情信息！");
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
    url: "/user/all",
    method: "POST",
    headers: {
      "Content-Type": "*/*",
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        allUser.value = res.data.data;
        allStudent.value = allUser.value.filter((user) => user.role === "STUDENT");
        allTeacher.value = allUser.value.filter((user) => user.role === "TEACHER");

        http({
          url: "/application/pending",
          method: "GET",
          headers: {
            "Content-Type": "*/*",
            Authorization: "Bearer " + localStorage.getItem("token"),
          },
          params: {
            Authorization: localStorage.getItem("token"),
          },
        })
          .then((res) => {
            if (res.data.code === 200) {
              pendingList.value = res.data.data;
              for (let i = 0; i < pendingList.value.length; i++) {
                switch (pendingList.value[i].status) {
                  case "REJECTED":
                    pendingList.value[i].statusCN = "已拒绝";
                    break;
                  case "PENDING":
                    pendingList.value[i].statusCN = "待审核";
                    break;
                  case "ACCEPTED":
                    pendingList.value[i].statusCN = "已通过";
                    break;
                  default:
                    break;
                }
                const student = allStudent.value.find(
                  (stu) => stu.uid === pendingList.value[i].studentId
                );
                if (student) {
                  pendingList.value[i].studentName = student.fullName;
                }
                const teacher = allTeacher.value.find(
                  (tea) => tea.uid === pendingList.value[i].mentorId
                );
                if (teacher) {
                  pendingList.value[i].teacherName = teacher.fullName;
                }
              }
            } else {
              alert("获取申请列表失败，请联系管理员！");
            }
          })
          .catch((err) => {
            console.error(err);
            alert("出现错误，请重新登录！");
          });
      } else {
        alert("身份验证出错！请重新登录");
        localStorage.removeItem("token");
        window.location.reload();
      }
    })
    .catch((err) => {
      console.error(err);
      alert("请求用户信息出错！");
      router.back();
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
    width="500"
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
    width="600"
    :before-close="handleStudentInfoClose"
  >
    <div class="student-info-card">
      <img :src="studentInfo.photourl" alt="头像" class="avatar" />
      <div class="info">
        <p><strong>姓名：</strong>{{ studentInfo.fullName }}</p>
        <p><strong>学号：</strong>{{ studentInfo.username }}</p>
        <p><strong>年级：</strong>{{ studentInfo.grade }}</p>
        <p><strong>意向研究方向：</strong>{{ studentInfo.research_direction }}</p>
        <p><strong>班级：</strong>{{ studentInfo.class }}</p>
        <p><strong>邮箱：</strong>{{ studentInfo.email }}</p>
        <p><strong>个人简介：</strong>{{ studentInfo.resume }}</p>
        <p><strong>申请理由：</strong>{{ studentApplicationInfo.applicationReason }}</p>
        <p><strong>申请状态：</strong>{{ studentApplicationInfo.statusCN }}</p>
        <p><strong>拒绝理由：</strong>{{ studentApplicationInfo.rejectionReason || '无' }}</p>
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
    <el-table :data="pendingList" stripe class="table">
      <el-table-column prop="id" label="申请编号" />
      <el-table-column prop="studentName" label="学生姓名" />
      <el-table-column
        prop="teacherName"
        label="导师姓名"
        v-if="userRole === 'ADMIN'"
      />
      <el-table-column prop="applicationReason" label="申请理由" show-overflow-tooltip />
      <el-table-column prop="statusCN" label="当前状态" />
      <el-table-column label="详细信息">
        <template #default="scope">
          <button class="button" @click="handleViewStudentInfo(scope.row)">
            查看
          </button>
        </template>
      </el-table-column>
      <el-table-column
        v-if="userRole === 'TEACHER' || userRole === 'ADMIN'"
        label="处理状态"
      >
        <template #default="scope">
          <div v-if="scopeIndexGetStatus(scope.$index)">
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
            <span>已完成</span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
  <div class="spacer"></div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
.container
  .title
    width: 100%
    height: 40px
    background-color: #e2e2e2
    line-height: 40px
    padding-left: 20px
    font-size: 16px
    margin-bottom: 20px
  .table
    width: 98%
    margin: 0 auto
    border: 1px solid #005826
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
  .avatar
    height: 120px
    margin-right: 20px
  .info
    p
      font-size: 16px
      margin: 5px 0
.spacer
  height: 100px
</style>
