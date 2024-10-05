<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { Right } from "@element-plus/icons-vue";
import { http } from "@/utils/http";
const userStore = useUserInfoStore();
import { useRouter } from "vue-router";
const router = useRouter();

const userInfoComp = ref({
  fullName: "",
  email: "",
  avatarUrl: "",
  role: "",
});
const targetTeacher = ref({
  fullName: "",
  email: "",
  avatarUrl: "",
});
const errorShow = ref(0);

onMounted(() => {
  userStore.fetchUserInfo();
  http({
    url: "/application/mentor-student-relations",
    method: "GET",
    headers: {
      "Content-Type": "*/*",
      Authorization: "Bearer " + localStorage.getItem("token"),
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        targetTeacher.value = { ...res.data.data[0].mentor };
      } else {
        alert("获取关键信息失败，请检查网络和登录状态！");
      }
    })
    .catch((err) => {
      console.error(err);
      if (err.request.status === 404) errorShow.value = 404;
      else if (err.request.status === 500) errorShow.value = 500;
    });
});

watch(
  () => userStore.userInfo,
  (newValue) => {
    userInfoComp.value = { ...newValue };
  }
);
</script>

<template>
  <div class="container">
    <div class="student_to_teacher_box" v-if="errorShow === 0">
      <h3>您的目标导师</h3>
      <div class="item_box">
        <div class="item">
          <img class="avatar" :src="userInfoComp.avatarUrl" alt="avatar" />
          <div>学生姓名：{{ userInfoComp.fullName }}</div>
          <div>学生邮箱：{{ userInfoComp.email }}</div>
        </div>
      </div>
      <el-icon :size="48" :color="'#005826'"><Right /></el-icon>
      <div class="item_box">
        <div class="item">
          <div>导师姓名：{{ targetTeacher.fullName }}</div>
          <div>导师邮箱：{{ targetTeacher.email }}</div>
        </div>
      </div>
    </div>
    <div class="student_to_teacher_box" v-if="errorShow === 404">
      <h3>您还未选择导师</h3>
    </div>
  </div>
</template>

<style scoped lang="sass">
/* 页面整体容器 */
.container
  margin-top: 60px  /* 向下移动60px */
  overflow-y: auto   /* 当内容超出页面范围时，显示滚动条 */

.student_to_teacher_box
  display: flex
  align-items: center
  justify-content: center
  margin: 50px auto 20px auto
  width: 50%
  position: relative
  h3
    position: absolute
    top: -40px
    font-weight: bold
  .item_box:last-child
    margin: auto auto auto 20px
  .item_box
    margin: auto 20px auto auto
    display: flex
    .item
      width: 320px
      height: 140px
      box-shadow: #005826 0 0 5px
      border-radius: 10px
      position: relative
      overflow: hidden
      padding: 20px
      .avatar
        border-radius: 5px
        width: 48px
        height: 48px
</style>
