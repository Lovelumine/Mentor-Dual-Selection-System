<script setup lang="ts">

import {onMounted, ref} from "vue";
import {http} from "@/utils/http";
import {useRouter} from "vue-router";
const router = useRouter();


const allUser = ref([]);
const allTeacher = ref([]);
const pendingList = ref([]);

onMounted(() => {
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
        if (allUser.value[i].role == 'TEACHER') {
          allTeacher.value.push(allUser.value[i]);
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
            for (let j = 0; j < allTeacher.value.length; j++) {
              if (pendingList.value[i].mentorId === allTeacher.value[j].uid) {
                pendingList.value[i].teacherName = allTeacher.value[j].fullName;
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
</script>

<template>
  <div class="application_list_box">
    <div class="title">
      <span>所有申请</span>
    </div>
    <el-table :data="pendingList" stripe class="table">
      <el-table-column prop="id" label="申请编号"/>
      <el-table-column prop="teacherName" label="导师姓名"/>
      <el-table-column prop="applicationReason" label="申请理由"/>
      <el-table-column prop="statusCN" label="当前状态"/>
      <el-table-column prop="rejectionReason" label="拒绝理由"/>
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
</style>