<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { useRouter } from "vue-router";
import {http, httpAdmin} from "@/utils/http";
import axios from 'axios';
import type {TeacherDetailImpl} from "@/interfaces/TeachDetailsImpl";
import type {TeachDetailsFullImpl} from "@/interfaces/TeachDetailsFullImpl";  // 这里导入 axios
const userInfoStore = useUserInfoStore();
const router = useRouter();

// 定义接口
interface UploadForm {
  role: string;
  fullName: string | null;
  email: string | null;
  username: string | null;
  teacherPosition: string;
  researchDirection: string;
  resume: string;
  studentGrade: string;
}

const userRole = ref<string | null>(null); // 初始化 userRole
const uploadForm = ref<UploadForm>({
  role: 'TEACHER',
  fullName: null,
  email: null,
  username: null,
  teacherPosition: '', // 职位信息
  researchDirection: '', // 研究方向
  resume: '', // 简历
  studentGrade: '' // 空的年级信息，必传
});

function uploadClicked() {
  // 验证表单
  if (!uploadForm.value.fullName) {
    alert('请填写姓名！');
    return;
  }
  if (!uploadForm.value.username) {
    alert('请填写工号！');
    return;
  }
  if (!uploadForm.value.email) {
    alert('请填写电子邮箱！');
    return;
  }
  // 先上传基本信息
  http({
    url: '/user/update',
    method: 'POST',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
    data: {
      fullName: uploadForm.value.fullName,
      username: uploadForm.value.username,
      email: uploadForm.value.email,
      role: uploadForm.value.role,
    }
  }).then(res => {
    if (res.data.code === 200){
      alert('基本信息添加成功！');
      // 获取所有老师信息，筛选出刚添加的老师的uid
      getTeacherUid();
    } else {
      alert('添加基本信息失败！');
    }
  }).catch(err => {
    alert('添加失败！');
    console.error('错误信息:', err);
  });
}

// 获取所有教师信息，并筛选出刚添加的教师
function getTeacherUid() {
  http({
    url: '/search/teachers',
    method: 'GET',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
  }).then(res => {
    if (res.data.code === 200) {
      // 在返回的教师列表中查找匹配的教师
      const teacher = res.data.data.find((t: TeachDetailsFullImpl) => t.username === uploadForm.value.username && t.fullName === uploadForm.value.fullName);
      if (teacher && teacher.uid) {
        // 找到教师，进行额外信息上传
        uploadAdditionalInfo(teacher.uid);
      } else {
        alert('未找到匹配的教师，请检查输入信息');
      }
    } else {
      alert('获取教师信息失败！');
    }
  }).catch(err => {
    alert('获取教师信息失败！');
    console.error('错误信息:', err);
  });
}

// 上传职位、研究方向、简历和空的年级信息
function uploadAdditionalInfo(uid: number) {
  httpAdmin({
    url: `/update/${uid}`,
    method: 'PUT',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
    data: {
      teacherPosition: uploadForm.value.teacherPosition,
      researchDirection: uploadForm.value.researchDirection,
      resume: uploadForm.value.resume,
      studentGrade: uploadForm.value.studentGrade // 传递空的年级信息
    }
  }).then(res => {
    if (res.data.code === 200) {
      alert('职位信息、研究方向、简历添加成功！');
      window.location.reload();
    } else {
      alert('职位信息、研究方向、简历添加失败！');
    }
  }).catch(err => {
    alert('职位信息、研究方向、简历添加失败！');
    console.error('错误信息:', err);
  });
}

function resettingClicked() {
  window.location.reload();
}

onMounted(() => {
  // 确保 userRole 已经被初始化
  if (userRole.value === 'STUDENT') {
    router.push('/');
  } else {
    // 从 userInfoStore 获取 userRole 的值
    userRole.value = userInfoStore.userInfo?.role || null;
    if (userRole.value === 'STUDENT') {
      router.push('/');
    }
  }
});

watch(() => userInfoStore.userInfo, (newVal) => {
  if (newVal?.role === 'STUDENT') router.push('/');
});
</script>

<template>
  <div class="title">
    <span>手动添加单条导师信息</span>
  </div>
  <div class="container">
    <el-form label-width="auto" style="max-width: 400px" @submit.prevent="uploadClicked">
      <el-form-item label="姓名：">
        <el-input v-model="uploadForm.fullName" />
      </el-form-item>
      <el-form-item label="工号：">
        <el-input v-model="uploadForm.username" />
      </el-form-item>
      <el-form-item label="电子邮箱：">
        <el-input v-model="uploadForm.email" />
      </el-form-item>
      <!-- 新增职位信息、研究方向和简历的输入框 -->
      <el-form-item label="职位信息：">
        <el-input v-model="uploadForm.teacherPosition" />
      </el-form-item>
      <el-form-item label="研究方向：">
        <el-input v-model="uploadForm.researchDirection" />
      </el-form-item>
      <el-form-item label="简历：">
        <el-input type="textarea" v-model="uploadForm.resume" />
      </el-form-item>
      <button class="button" type="submit">上传</button>
      <button class="button" @click="resettingClicked" type="button">重置</button>
    </el-form>
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
  width: 50%
  padding-top: 20px
  padding-left: 20px
  .button
    border-radius: 5px
    width: 100px
    height: 32px
    background-color: #005826
    color: white
    margin-left: 20px
    border: 1px solid #005826
    font-size: 15px
    transition: .3s ease
  .button:hover
    background-color: #0f7e3f
    border: 1px solid #0f7e3f
  .button:last-child
    background-color: white
    border: 1px solid #bd0000
    color: #bd0000
  .button:last-child:hover
    background-color: #bd0000
    color: white
</style>
