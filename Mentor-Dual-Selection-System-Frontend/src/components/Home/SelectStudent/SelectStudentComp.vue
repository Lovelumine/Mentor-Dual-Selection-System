<script setup lang="ts">

import {onMounted, ref} from "vue";
import {http} from "@/utils/http";
import {useRouter} from "vue-router";
import UtilPendingComp from "@/components/Home/SelectStudent/UtilPendingComp.vue";
const router = useRouter();

const pendingList = ref([]);
const allUser = ref([]);
const allStudent = ref([]);


onMounted(() => {
  http({
    url: '/user/all',
    method: 'POST',
    headers: {
      'Content-Type': '*/*',
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  }).then(res => {
    if (res.data.code === 200){
      allUser.value = res.data.data;
      for (let i = 0; i < allUser.value.length; i++){
        if (allUser.value[i].role == 'STUDENT'){
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
        console.log('allstu', allStudent.value);
        console.log('allu', allUser.value);

        if (res.data.code === 200){
          pendingList.value = res.data.data;
          console.log('pend', pendingList.value);
          for (let i = 0; i < pendingList.value.length; i++){
            switch (pendingList.value[i].status) {
              case 'REJECTED': pendingList.value[i].statusCN = '已拒绝'; break;
              case 'PENDING': pendingList.value[i].statusCN = '待审核'; break;
              case 'ACCEPTED': pendingList.value[i].statusCN = '已通过'; break;
              default: break;
            }
            for (let j = 0; j < allStudent.value.length; j++){
              if (pendingList.value[i].studentId === allStudent.value[j].uid){
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
    } else{
      alert('身份验证出错！请重新登入');
      localStorage.removeItem('token');
      window.location.reload();
    }
  }).catch(err => {
    console.error(err);
    alert('请求学生信息出错！');
    router.back();
  })
})
</script>

<template>
  <div class="title">
    <span>选择学生</span>
  </div>
  <div class="container">
    <UtilPendingComp/>
    <div class="title">
      <span>所有申请</span>
    </div>
    <el-table :data="pendingList" stripe class="table">
      <el-table-column prop="id" label="申请编号"/>
      <el-table-column prop="studentName" label="学生姓名"/>
      <el-table-column prop="applicationReason" label="申请理由"/>
      <el-table-column prop="statusCN" label="当前状态"/>
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
</style>