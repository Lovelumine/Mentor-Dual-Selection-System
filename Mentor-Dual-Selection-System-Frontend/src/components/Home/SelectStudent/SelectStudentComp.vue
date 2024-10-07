<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { http } from "@/utils/http";
import { useRouter } from "vue-router";
const router = useRouter();
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { ElMessageBox } from "element-plus";
const userStore = useUserInfoStore();

const pendingList = ref([]);
const allUser = ref([]);
const allStudent = ref([]);
const allTeacher = ref([]);
const userRole = ref();
const dialogVisible = ref(false);
const studentInfoVisible = ref(false); // 控制学生详情卡片的显示
const studentInfo = ref({}); // 存储学生详细信息
const pendingUtilForm = ref({
  applicationId: null,
  approved: null,
  rejectionReason: null,
});

const handleClose = (done: () => void) => {
  ElMessageBox.confirm("您输入的信息将会保留，直到您下次点开该弹窗！")
    .then(() => {
      done();
    })
    .catch(() => {
      // catch error
    });
};

// 定义学生详情对话框的关闭处理函数
const handleStudentInfoClose = (done) => {
  done();
};

function handleAccept(index: number, row) {
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = true;
  http({
    url: "/application/approve",
    method: "POST",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
    params: pendingUtilForm.value,
  })
    .then((res) => {
      if (res.status === 200 || res.data.code === 200) {
        alert("该申请处理完成！");
        fetchApplicationList(); // 刷新申请列表
      } else {
        alert(res.data.error);
      }
    })
    .catch((err) => {
      alert(JSON.parse(err.request.responseText).data.error);
    });
}

function handleReject(index: number, row) {
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = false;
  dialogVisible.value = true;
}

function checkReject() {
  if (
    pendingUtilForm.value.rejectionReason === null ||
    pendingUtilForm.value.rejectionReason === ""
  ) {
    alert("您需要填写拒绝理由。");
    return;
  }
  http({
    url: "/application/approve",
    method: "POST",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
    params: pendingUtilForm.value,
  })
    .then((res) => {
      if (res.status === 200 || res.data.code === 200) {
        alert("该申请处理完成！");
        dialogVisible.value = false;
        fetchApplicationList(); // 刷新申请列表
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

// 获取学生详情
function handleViewStudentInfo(row) {
  getStudentInfo(row.studentId)
    .then((res) => {
      if (res.data.code === 200) {
        studentInfo.value = res.data.data;
        studentInfoVisible.value = true; // 注意这里使用 .value
      } else {
        alert("获取学生详情失败！");
      }
    })
    .catch((err) => {
      console.error(err);
      alert("请求学生详情出错或当前学生未完善详情信息！");
    });
}

// 获取学生信息的API调用函数
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

// 新增：获取申请列表的函数
function fetchApplicationList() {
  // 首先，获取所有用户
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

        // 然后，获取待审核的申请列表
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
  fetchApplicationList(); // 组件挂载时获取申请列表
});

watch(
  () => userStore.userInfo,
  (newValue) => {
    userRole.value = newValue.role;
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
    <input
      type="text"
      placeholder="拒绝理由（必填）"
      required
      v-model="pendingUtilForm.rejectionReason"
    />
    <template #footer>
      <div class="dialog-footer">
        <button class="button" @click="dialogVisible.value = false">关闭</button>
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
      <el-table-column prop="applicationReason" label="申请理由" />
      <el-table-column prop="statusCN" label="当前状态" />
      <!-- 查看学生详情按钮列 -->
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
    max-height:calc(100vh-60px-60px-6000px)
    overflow-y:auto
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
/* 学生详情卡片样式 */
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
  height: 100px // 每个空白行的高度，可以根据需要调整
</style>
