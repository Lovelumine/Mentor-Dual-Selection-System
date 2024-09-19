<script setup lang="ts">

import {onMounted, ref, watch} from "vue";
import {http} from "@/utils/http";
import {useRouter} from "vue-router";
const router = useRouter();
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userStore = useUserInfoStore();


const allUser = ref([]);
const allStudent = ref([]);
const pendingList = ref([]);
const userRole = ref();
const pendingUtilForm = ref({
  applicationId: null,
  approved: null,
  rejectionReason: null
});
const dialogVisible = ref(false)

const handleClose = (done: () => void) => {
  ElMessageBox.confirm('Are you sure to close this dialog?')
      .then(() => {
        done()
      })
      .catch(() => {
        // catch error
      })
}
function handleAccept(index: number, row){
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = true;
  http({
    url: '/application/approve',
    method: 'POST',
    headers: {
      'Accept': '*/*',
      'Authorization': `Bearer ${localStorage.getItem("token")}`
    },
    params: pendingUtilForm.value,
  }).then(res => {
    if (res.status === 200) {
      alert('该申请处理完成！');
    } else {
      alert(res.data.error);
    }
  }).catch((err) => {
    alert(JSON.parse(err.request.responseText).data.error);
  })
}
function handleReject(index: number, row){
  console.log(index, row);
  pendingUtilForm.value.applicationId = row.id;
  pendingUtilForm.value.approved = false;
  dialogVisible.value = true;
}
function checkReject(){
  dialogVisible.value = false;
  console.log(pendingUtilForm.value);
}

onMounted(() => {
  userStore.fetchUserInfo();
  http({
    url: '/user/all',
    method: 'POST',
    headers: {
      'Content-Type': '*/*',
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  }).then(res => {
    if (res.data.code === 200) {
      allUser.value = res.data.data;
      for (let i = 0; i < allUser.value.length; i++) {
        if (allUser.value[i].role == 'STUDENT') {
          allStudent.value.push(allUser.value[i]);
        }
      }
      http({
        url: '/application/pending',
        method: 'GET',
        headers: {
          'Content-Type': '*/*',
          'Authorization': 'Bearer ' + localStorage.getItem('token'),
        },
        params: {
          Authorization: localStorage.getItem('token'),
        }
      }).then(res => {
        if (res.data.code === 200) {
          pendingList.value = res.data.data;
          for (let i = 0; i < pendingList.value.length; i++) {
            switch (pendingList.value[i].status) {
              case 'REJECTED':
                pendingList.value[i].statusCN = '已拒绝';
                break;
              case 'PENDING':
                pendingList.value[i].statusCN = '待审核';
                break;
              case 'ACCEPTED':
                pendingList.value[i].statusCN = '已通过';
                break;
              default:
                break;
            }
            for (let j = 0; j < allStudent.value.length; j++) {
              if (pendingList.value[i].studentId === allStudent.value[j].uid) {
                pendingList.value[i].studentName = allStudent.value[j].fullName;
              }
            }
          }
        } else{
          alert('获取学生列表失败，若您有学生申请，请联系管理员修复系统！');
        }
      }).catch(err => {
        console.error(err);
        alert('出现错误，或请重新登录！');
      })
    }else{
      alert('身份验证出错！请重新登入');
      localStorage.removeItem('token');
      window.location.reload();
    }
  }).catch(err => {
    console.error(err);
    alert('请求信息出错！');
    router.back();
  })
})
watch(() => userStore.userInfo, (newValue) => {
  userRole.value = newValue.role;
  console.log('watch', userRole.value);
})
</script>

<template>
  <el-dialog
      v-model="dialogVisible"
      title="拒绝理由"
      width="500"
      :before-close="handleClose"
  >
    <input type="text" placeholder="精简20字以内" required v-model="pendingUtilForm.rejectionReason"/>
    <template #footer>
      <div class="dialog-footer">
        <button class="button" @click="dialogVisible = false">关闭</button>
        <button class="button" type="submit" @click="checkReject">提交</button>
      </div>
    </template>
  </el-dialog>
  <div class="application_list_box">
    <div class="title">
      <span>所有申请</span>
    </div>
    <el-table :data="pendingList" stripe class="table">
      <el-table-column prop="id" label="申请编号"/>
      <el-table-column prop="studentName" label="学生姓名"/>
      <el-table-column prop="applicationReason" label="申请理由"/>
      <el-table-column prop="statusCN" label="当前状态"/>
      <el-table-column prop="rejectionReason" label="拒绝理由"/>
      <el-table-column v-if="userRole === 'TEACHER' || userRole === 'ADMIN'" label="处理">
        <template #default="scope">
          <button class="button" @click="handleAccept(scope.$index, scope.row)">同意</button>
          <button class="button" @click="handleReject(scope.$index, scope.row)">拒绝</button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="sass">
.application_list_box
  .title
    width: 100%
    height: 40px
    background-color: #e2e2e2
    line-height: 40px
    padding-left: 20px
    font-size: 16px
    margin-bottom: 20px
  .table
    margin: 0 auto
    width: 98%
    border: 1px solid #005826
    border-radius: 5px
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
</style>