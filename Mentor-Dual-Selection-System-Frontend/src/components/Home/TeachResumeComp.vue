<!--<script setup lang="ts">-->
<!--import { onMounted, ref, computed } from "vue";-->
<!--import {-->
<!--  ElPagination,-->
<!--  ElSelect,-->
<!--  ElOption,-->
<!--  ElButton,-->
<!--  ElDialog,-->
<!--} from "element-plus";-->
<!--import { http, httpStudent } from "@/utils/http";-->

<!--const allTeacherDetailsList = ref([]);-->
<!--const currentPage = ref(1);-->
<!--const pageSize = ref(6);-->

<!--const positions = [-->
<!--  "教授",-->
<!--  "副教授",-->
<!--  "助理教授",-->
<!--  "博士后",-->
<!--  "学术顾问",-->
<!--  "客座/兼职教授",-->
<!--];-->
<!--const selectedPosition = ref("");-->

<!--const dialogVisible = ref(false);-->
<!--const selectedTeacher = ref(null);-->

<!--onMounted(() => {-->
<!--  http({-->
<!--    url: "/search/teachers",-->
<!--    method: "GET",-->
<!--    headers: {-->
<!--      Accept: "*/*",-->
<!--      Authorization: `Bearer ${localStorage.getItem("token")}`,-->
<!--    },-->
<!--  })-->
<!--    .then((res) => {-->
<!--      if (res.data.code === 200) {-->
<!--        allTeacherDetailsList.value = res.data.data;-->

<!--        // 获取所有教师的额外详细信息-->
<!--        httpStudent({-->
<!--          url: "/teachers",-->
<!--          method: "GET",-->
<!--          headers: {-->
<!--            Accept: "*/*",-->
<!--            Authorization: `Bearer ${localStorage.getItem("token")}`,-->
<!--          },-->
<!--        })-->
<!--          .then((res2) => {-->
<!--            if (res2.data.code === 200) {-->
<!--              const tdl = res2.data.data;-->
<!--              allTeacherDetailsList.value.forEach((teacher) => {-->
<!--                const match = tdl.find((t) => t.uid === teacher.uid);-->
<!--                if (match) {-->
<!--                  Object.assign(teacher, match);-->
<!--                }-->
<!--              });-->
<!--              console.log("All Teachers Data:", allTeacherDetailsList.value);-->
<!--            }-->
<!--          })-->
<!--          .catch((err) => {-->
<!--            console.error(err);-->
<!--          });-->
<!--      } else {-->
<!--        alert("导师信息获取失败！");-->
<!--      }-->
<!--    })-->
<!--    .catch((err) => {-->
<!--      alert("导师信息获取失败！");-->
<!--      console.error(err);-->
<!--    });-->
<!--});-->

<!--const onPositionChange = () => {-->
<!--  console.log("Selected Position:", selectedPosition.value);-->
<!--  // 当筛选条件改变时，重置当前页码为1-->
<!--  currentPage.value = 1;-->
<!--};-->

<!--const resetFilter = () => {-->
<!--  selectedPosition.value = "";-->
<!--  currentPage.value = 1;-->
<!--};-->

<!--const filteredTeachers = computed(() => {-->
<!--  console.log("Computing filteredTeachers");-->
<!--  if (!selectedPosition.value) {-->
<!--    console.log("No position selected, returning all teachers");-->
<!--    return allTeacherDetailsList.value;-->
<!--  }-->
<!--  const filtered = allTeacherDetailsList.value.filter(-->
<!--    (teacher) => teacher.teacherposition === selectedPosition.value-->
<!--  );-->
<!--  console.log("Filtered Teachers:", filtered);-->
<!--  return filtered;-->
<!--});-->

<!--const paginatedTeachers = computed(() => {-->
<!--  console.log("Computing paginatedTeachers");-->
<!--  const start = (currentPage.value - 1) * pageSize.value;-->
<!--  const end = start + pageSize.value;-->
<!--  const paginated = filteredTeachers.value.slice(start, end);-->
<!--  console.log(-->
<!--    `Page ${currentPage.value}, showing items ${start} to ${end}`-->
<!--  );-->
<!--  console.log("Paginated Teachers:", paginated);-->
<!--  return paginated;-->
<!--});-->

<!--const showDetails = (teacher) => {-->
<!--  selectedTeacher.value = teacher;-->
<!--  dialogVisible.value = true;-->
<!--};-->

<!--// 处理分页页码变化-->
<!--const handlePageChange = (page) => {-->
<!--  currentPage.value = page;-->
<!--};-->
<!--</script>-->
<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import {
  ElPagination,
  ElSelect,
  ElOption,
  ElButton,
  ElDialog
} from 'element-plus';
import { http, httpStudent } from '@/utils/http';
import type {TeacherDetailImpl} from "@/interfaces/TeachDetailsImpl";


// 状态变量
const allTeacherDetailsList = ref<TeacherDetailImpl[]>([]);
const currentPage = ref<number>(1);
const pageSize = ref<number>(6);
const positions = ['教授', '副教授', '助理教授', '博士后', '学术顾问', '客座/兼职教授'];
const selectedPosition = ref<string>('');
const dialogVisible = ref<boolean>(false);
const selectedTeacher = ref<TeacherDetailImpl | null>(null);

// 挂载时获取教师数据
onMounted(async () => {
  try {
    const response = await http({
      url: '/search/teachers',
      method: 'GET',
      headers: {
        Accept: '*/*',
        Authorization: `Bearer ${localStorage.getItem('token')}`
      }
    });

    if (response.data.code === 200) {
      allTeacherDetailsList.value = response.data.data;

      // 获取额外详细信息
      const extraDetailsResponse = await httpStudent({
        url: '/teachers',
        method: 'GET',
        headers: {
          Accept: '*/*',
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (extraDetailsResponse.data.code === 200) {
        const extraDetails = extraDetailsResponse.data.data;
        allTeacherDetailsList.value.forEach(teacher => {
          const match = extraDetails.find((t: TeacherDetailImpl) => t.uid === teacher.uid);
          if (match) {
            Object.assign(teacher, match);
          }
        });
        console.log('All Teachers Data:', allTeacherDetailsList.value);
      }
    } else {
      alert('导师信息获取失败！');
    }
  } catch (error) {
    alert('导师信息获取失败！');
    console.error('Error fetching teacher data:', error);
  }
});

// 位置筛选变化处理
const onPositionChange = () => {
  console.log('Selected Position:', selectedPosition.value);
  currentPage.value = 1;
};

// 重置筛选条件
const resetFilter = () => {
  selectedPosition.value = '';
  currentPage.value = 1;
};

// 过滤后的教师列表
const filteredTeachers = computed<TeacherDetailImpl[]>(() => {
  console.log('Computing filteredTeachers');
  if (!selectedPosition.value) {
    console.log('No position selected, returning all teachers');
    return allTeacherDetailsList.value;
  }
  return allTeacherDetailsList.value.filter(teacher => teacher.teacherposition === selectedPosition.value);
});

// 分页后的教师列表
const paginatedTeachers = computed<TeacherDetailImpl[]>(() => {
  console.log('Computing paginatedTeachers');
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredTeachers.value.slice(start, end);
});

// 显示教师详情对话框
const showDetails = (teacher: TeacherDetailImpl) => {
  selectedTeacher.value = teacher;
  dialogVisible.value = true;
};

// 处理分页页码变化
const handlePageChange = (page: number) => {
  currentPage.value = page;
};
</script>

<template>
  <!-- 固定的标题 -->
  <div class="title">
    <span>本院导师卡片</span>
  </div>

  <!-- 主内容 -->
  <div class="main-content">
    <!-- 筛选控件和分页控件 -->
    <div class="top-controls">
      <div class="filter-controls">
        <el-select
          v-model="selectedPosition"
          placeholder="选择职称"
          @change="onPositionChange"
          clearable
        >
          <el-option
            v-for="position in positions"
            :key="position"
            :label="position"
            :value="position"
          ></el-option>
        </el-select>
        <el-button @click="resetFilter" type="warning">重置</el-button>
      </div>
      <!-- 分页控件 -->
      <div class="pagination-controls">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredTeachers.length"
          @current-change="handlePageChange"
        >
        </el-pagination>
      </div>
    </div>

    <!-- 内容的容器 -->
    <div class="content-wrapper">
      <div class="container">
        <div
          class="teacher_resume_item"
          v-for="(item, index) in paginatedTeachers"
          :key="index"
        >
        <div class="photo">
            <img :src="item.photoUrl" alt="photo" />
          </div>
          <div class="item_box">
            <span class="fullname">{{
                `${item.fullName}`
              }}</span>
            <span class="professional">
              职位：{{ item.teacherposition }}
            </span>
            <span class="email">电子邮箱：{{ item.email }}</span>

            <hr class="hea_con_hr" />
            <div class="detail_box">
              <div class="resume">研究方向：{{ item.research_direction }}</div>
              <div class="resume">
                 简历：{{ item.resume.length > 50 ? item.resume.slice(0, 50) + '...' : item.resume }}
              </div>

              <!-- 显示详情按钮 -->
              <el-button type="primary" size="small" @click="showDetails(item)">了解更多</el-button>
            </div>
            <div class="avatar">
              <img :src="item.avatarUrl" alt="avatar" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 详情对话框 -->
  <el-dialog v-model="dialogVisible" :title="selectedTeacher?.fullName">
    <div v-if="selectedTeacher">
      <p>职称：{{ selectedTeacher?.teacherposition }}</p>
      <p>电子邮箱：{{ selectedTeacher?.email }}</p>
      <p>研究方向：{{ selectedTeacher?.research_direction }}</p>
      <p>简历：{{ selectedTeacher?.resume }}</p>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
  <div style="height: 100px;"></div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
  position: fixed
  top: 60px
  left: 200px
  z-index: 1000

.main-content
  margin-top: 80px // 确保主内容不被固定的标题遮挡

.top-controls
  display: flex
  justify-content: space-between
  align-items: center
  margin: 20px
  .filter-controls
    display: flex
    align-items: center
    gap: 10px
    .el-select
      width: 200px
  .pagination-controls
    .el-pagination
      display: flex
      align-items: center

.content-wrapper
  padding: 0 20px

.container
  display: flex
  padding-left: 10vw
  flex-wrap: wrap
  gap: 100px
  row-gap: 50px
  padding-top: 20px

.teacher_resume_item
  width: 500px
  height: 300px
  position: relative
  .item_box
    width: 100%
    height: 250px
    bottom: 0
    position: absolute
    box-shadow: #00582680 0 0 0px
    border-radius: 10px
    .hea_con_hr
      position: absolute
      top: 25%
      width: 90%
      left: 50%
      transform: translateX(-50%)
    .fullname
      position: absolute
      top: -13%
      left: 22%
      font-size: 18px
    .email
      position: absolute
      left: 22%
      display: flex
      align-items: center
      top: 2%
    .professional
      position: absolute
      left: 22%
      top: 12%
    .detail_box
      position: absolute
      top: 30%
      left: 5%
    .avatar
      z-index: -1
      width: 100%
      height: 100%
      overflow: hidden
      position: relative
      border-radius: 10px
      img
        position: absolute
        top: 50%
        left: 50%
        transform: translate(-50%, -50%)
        width: 100%
        opacity: .2
  .photo
    box-shadow: #00000030 0 0 5px
    background-color: white
    border-radius: 10px
    z-index: 1
    left: 5%
    width: 75px
    height: 99px
    overflow: hidden
    position: absolute
    img
      width: 100%
</style>
