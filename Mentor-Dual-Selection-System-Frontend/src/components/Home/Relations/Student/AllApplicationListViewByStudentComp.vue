<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

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
    url: '/user/allteacher',
    method: 'GET',
    headers: {
      'Content-Type': '*/*',
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  }).then(res => {
    if (res.data.code === 200) {
      allTeacher.value = res.data.data;
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
              ElMessage.error(historyRes.data.data.error || "获取师生关系信息失败");
            }
          }).catch(err => {
            console.error(err);
            ElMessage.error(JSON.parse(err.request.responseText).data.error || "获取师生关系信息失败");
          });
        } else {
          ElMessage.error(res.data.data.error || "获取申请列表失败");
        }
      }).catch(err => {
        console.error(err);
        ElMessage.error(JSON.parse(err.request.responseText).data.error || "获取申请列表失败");
      });
    } else {
      ElMessage.error(res.data.data.error || "获取用户信息失败");
      localStorage.removeItem('token');
      window.location.reload();
    }
  }).catch(err => {
    ElMessage.error(JSON.parse(err.request.responseText).data.error || "获取用户信息失败");
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
    <div class="table_shell">
      <p class="table_scroll_hint">左右滑动表格可查看完整申请记录</p>
      <el-table :data="pendingList" stripe class="table">
        <el-table-column prop="id" label="申请编号" min-width="110" />
        <el-table-column prop="teacherName" label="导师姓名" min-width="130" />
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
        <el-table-column prop="applicationReason" label="申请理由" min-width="260" show-overflow-tooltip />
        <el-table-column prop="statusCN" label="当前状态" min-width="120">
          <template #default="scope">
            <span :class="statusClass(scope.row.statusCN)">
              {{ scope.row.statusCN }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="rejectionReason" label="拒绝理由" min-width="220" />
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="sass">
.application_list_box
  .title
    width: 100%
    min-height: 40px
    background-color: var(--app-surface-muted)
    display: flex
    align-items: center
    padding: 0 20px
    font-size: 16px
    margin-bottom: 20px
    font-family: 'Microsoft YaHei', sans-serif // 确保和前面统一的字体
    color: var(--app-text-primary)
    border: 1px solid var(--app-border)
    border-radius: 14px

  .table
    font-family: 'Microsoft YaHei', sans-serif // 确保表格字体统一
    width: 100%
    min-width: 0
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

.table_shell
  width: 100%
  overflow-x: auto
  -webkit-overflow-scrolling: touch

.table_scroll_hint
  display: none

@media (max-width: 768px)
  .application_list_box .title
    min-height: 48px
    height: auto
    line-height: 1.5
    padding: 12px 16px

  .application_list_box .table
    min-width: 0

  .table_scroll_hint
    display: block
    margin: 0 12px 8px
    color: var(--app-text-secondary)
    font-size: 12px
</style>
