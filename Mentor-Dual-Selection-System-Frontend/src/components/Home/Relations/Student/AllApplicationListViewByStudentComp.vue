<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";
import { useRouter } from "vue-router";

const router = useRouter();

// 定义用户和申请的接口类型
interface User {
  uid: number;
  fullName: string;
  role: string; // 用户角色，例如 'TEACHER'
}

interface Application {
  id: number;
  mentorId: number;
  applicationReason: string;
  status: string;
  statusCN?: string; // 增加一个中文状态字段
  teacherName?: string; // 增加一个导师姓名字段
  rejectionReason?: string;
  applicationDate?: string | null;
  decisionDate?: string | null;
}

interface MentorHistoryItem {
  mentorId: number;
  current: boolean;
}

const allUser = ref<User[]>([]);
const allTeacher = ref<User[]>([]);
const pendingList = ref<Application[]>([]);
const nowStatus = ref('未选择');

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '—';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }

  return date
    .toLocaleString('zh-CN', {
      hour12: false,
      timeZone: 'Asia/Shanghai',
    })
    .replace(/\//g, '-');
}

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
        if (allUser.value[i].role === 'TEACHER') {
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

          // 更新状态
          let hasPending = false;
          for (let i = 0; i < pendingList.value.length; i++) {
            switch (pendingList.value[i].status) {
              case 'REJECTED':
                pendingList.value[i].statusCN = '已拒绝';
                break;
              case 'PENDING':
                pendingList.value[i].statusCN = '待审核';
                hasPending = true;
                break;
              case 'ACCEPTED':
                pendingList.value[i].statusCN = '已通过';
                break;
              default:
                break;
            }
            // 匹配导师名字
            for (let j = 0; j < allTeacher.value.length; j++) {
              if (pendingList.value[i].mentorId === allTeacher.value[j].uid) {
                pendingList.value[i].teacherName = allTeacher.value[j].fullName;
              }
            }
          }
          http({
            url: '/application/student/relationship-history',
            method: 'GET',
            headers: {
              'Content-Type': '*/*',
              'Authorization': 'Bearer ' + localStorage.getItem('token'),
            },
          }).then(historyRes => {
            if (historyRes.data.code === 200) {
              const currentRelationship = historyRes.data.data.currentRelationship;
              const historicalRelationships = (historyRes.data.data.historicalRelationships || []) as MentorHistoryItem[];

              if (currentRelationship?.current) {
                nowStatus.value = '已通过';
              } else if (hasPending) {
                nowStatus.value = '待审核';
              } else if (historicalRelationships.length > 0) {
                nowStatus.value = '已解除';
              } else {
                nowStatus.value = '未选择';
              }
            } else {
              alert(historyRes.data.data.error);
            }
          }).catch(err => {
            console.error(err);
            alert(JSON.parse(err.request.responseText).data.error);
          });
        } else {
          alert(res.data.data.error);
        }
      }).catch(err => {
        console.error(err);
        alert(JSON.parse(err.request.responseText).data.error);
      });
    } else {
      alert(res.data.data.error);
      localStorage.removeItem('token');
      window.location.reload();
    }
  }).catch(err => {
    alert(JSON.parse(err.request.responseText).data.error);
    router.back();
  });
});

// 动态返回状态对应的 CSS 类
function statusClass(statusCN: string) {
  switch (statusCN) {
    case '已通过':
      return 'accepted-status';
    case '已拒绝':
      return 'rejected-status';
    case '待审核':
      return 'pending-status';
    default:
      return '';
  }
}
</script>

<template>
  <div class="application_list_box">
    <div class="title">
      <span>所有申请&nbsp;&nbsp;&nbsp;当前状态：{{nowStatus}}</span>
    </div>
    <el-table :data="pendingList" stripe class="table">
      <el-table-column prop="id" label="申请编号"/>
      <el-table-column prop="teacherName" label="导师姓名"/>
      <el-table-column label="申请时间" min-width="168">
        <template #default="scope">
          {{ formatDateTime(scope.row.applicationDate) }}
        </template>
      </el-table-column>
      <el-table-column label="处理时间" min-width="168">
        <template #default="scope">
          {{ formatDateTime(scope.row.decisionDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="applicationReason" label="申请理由" show-overflow-tooltip/>
      
      <!-- 动态改变状态栏颜色 -->
      <el-table-column prop="statusCN" label="当前状态">
        <template #default="scope">
          <span :class="statusClass(scope.row.statusCN)">
            {{ scope.row.statusCN }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="rejectionReason" label="拒绝理由"/>
    </el-table>
  </div>
</template>

<style scoped lang="sass">
.application_list_box
  .title
    width: 100%
    height: 40px
    background-color: var(--app-surface-muted)
    line-height: 40px
    padding-left: 20px
    font-size: 16px
    margin-bottom: 20px
    font-family: 'Microsoft YaHei', sans-serif // 确保和前面统一的字体
    color: var(--app-text-primary)
    border: 1px solid var(--app-border)
    border-radius: 14px

  .table
    font-family: 'Microsoft YaHei', sans-serif // 确保表格字体统一
    .accepted-status
      background-color: color-mix(in srgb, var(--app-brand-soft) 72%, var(--app-surface) 28%)
      color: var(--app-brand)
      font-weight: bold
      padding: 5px 10px
      border-radius: 999px
      font-family: 'Microsoft YaHei', sans-serif // 确保状态文本的字体统一

    .rejected-status
      background-color: var(--app-danger-soft)
      color: var(--app-danger)
      font-weight: bold
      padding: 5px 10px
      border-radius: 999px
      font-family: 'Microsoft YaHei', sans-serif // 确保状态文本的字体统一

    .pending-status
      background-color: rgba(255, 213, 107, 0.2)
      color: #c98b07
      font-weight: bold
      padding: 5px 10px
      border-radius: 999px
      font-family: 'Microsoft YaHei', sans-serif // 确保状态文本的字体统一
</style>
