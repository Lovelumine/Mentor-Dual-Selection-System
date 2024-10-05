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
const studentInfoVisible = ref(false); // 新增：控制学生详情卡片的显示
const studentInfo = ref({}); // 新增：存储学生详细信息
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
      if (res.status === 200) {
        alert("该申请处理完成！");
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
  dialogVisible.value = false;
  if (pendingUtilForm.value.rejectionReason === null || pendingUtilForm.value.rejectionReason === "") {
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
      if (res.status === 200) {
        alert("该申请处理完成！");
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

// 新增：获取学生详情
function handleViewStudentInfo(row) {
  getStudentInfo(row.studentId)
    .then((res) => {
      if (res.data.code === 200) {
        studentInfo.value = res.data.data;
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

// 新增：获取学生信息的API调用函数
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

onMounted(() => {
  userStore.fetchUserInfo();
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
        for (let i = 0; i < allUser.value.length; i++) {
          if (allUser.value[i].role == "STUDENT") {
            allStudent.value.push(allUser.value[i]);
          } else if (allUser.value[i].role == "TEACHER") {
            allTeacher.value.push(allUser.value[i]);
          }
        }
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
                for (let j = 0; j < allStudent.value.length; j++) {
                  if (pendingList.value[i].studentId === allStudent.value[j].uid) {
                    pendingList.value[i].studentName = allStudent.value[j].fullName;
                  }
                }
                for (let k = 0; k < allTeacher.value.length; k++) {
                  if (pendingList.value[i].mentorId === allTeacher.value[k].uid) {
                    pendingList.value[i].teacherName = allTeacher.value[k].fullName;
                  }
                }
              }
            } else {
              alert("获取学生列表失败，若您有学生申请，请联系管理员修复系统！");
            }
          })
          .catch((err) => {
            console.error(err);
            alert("出现错误，或请重新登录！");
          });
      } else {
        alert("身份验证出错！请重新登入");
        localStorage.removeItem("token");
        window.location.reload();
      }
    })
    .catch((err) => {
      console.error(err);
      alert("请求学生信息出错！");
      router.back();
    });
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
      placeholder="精简您的理由（必填）"
      required
      v-model="pendingUtilForm.rejectionReason"
    />
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
    :before-close="() => (studentInfoVisible.value = false)"
  >
    <div class="student-info-card">
      <img :src="studentInfo.photourl" alt="头像" class="avatar" />
      <div class="info">
        <p><strong>姓名：</strong>{{ studentInfo.fullName }}</p>
        <p><strong>学号：</strong>{{ studentInfo.uid }}</p>
        <p><strong>年级：</strong>{{ studentInfo.grade }}</p>
        <p><strong>意向研究方向：</strong>{{ studentInfo.research_direction }}</p>
        <p><strong>班级：</strong>{{ studentInfo.class }}</p>
        <p><strong>邮箱：</strong>{{ studentInfo.email }}</p>
        <p><strong>个人简介：</strong>{{ studentInfo.resume }}</p>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <button class="button" @click="studentInfoVisible = false">关闭</button>
      </div>
    </template>
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
      <!-- 新增：查看学生详情按钮列 -->
      <el-table-column label="详细信息">
        <template #default="scope">
          <button
            class="button"
            @click="handleViewStudentInfo(scope.row)"
          >
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
            <button class="button" @click="handleAccept(scope.$index, scope.row)">
              同意
            </button>
            <button class="button" @click="handleReject(scope.$index, scope.row)">
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
/* 新增：学生详情卡片样式 */
.student-info-card
  display: flex
  .avatar
    height: 120px
    margin-right: 20px
  .info
    p
      font-size: 16px
      margin: 5px 0
</style>
