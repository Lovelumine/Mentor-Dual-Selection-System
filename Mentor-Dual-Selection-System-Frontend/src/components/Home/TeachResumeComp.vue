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
import { onMounted, ref, computed, watch } from 'vue';
import {
  ElPagination,
  ElSelect,
  ElOption,
  ElButton,
  ElDialog
} from 'element-plus';
import { http, httpStudent } from '@/utils/http';
import type {TeacherDetailImpl} from "@/interfaces/TeachDetailsImpl";
import { getCardBackgroundImageUrl, getPortraitImageUrl } from "@/utils/media";
import { useRoute, useRouter } from "vue-router";
import { getSingleQueryValue, parsePositiveIntQuery } from "@/utils/routeQuery";


// 状态变量
const allTeacherDetailsList = ref<TeacherDetailImpl[]>([]);
const currentPage = ref<number>(1);
const pageSize = ref<number>(6);
const positions = ['教授', '副教授', '助理教授', '博士后', '学术顾问', '客座/兼职教授'] as const;
const selectedPosition = ref<string>('');
const dialogVisible = ref<boolean>(false);
const selectedTeacher = ref<TeacherDetailImpl | null>(null);
const route = useRoute();
const router = useRouter();
const hasLoadedTeachers = ref(false);

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

      hasLoadedTeachers.value = true;
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
  void syncQueryFromState();
};

// 重置筛选条件
const resetFilter = () => {
  selectedPosition.value = '';
  currentPage.value = 1;
  void syncQueryFromState();
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
  void syncQueryFromState();
};

const getCardImageUrl = (url?: string | null) => getCardBackgroundImageUrl(url);
const getPhotoImageUrl = (url?: string | null) => getPortraitImageUrl(url);
const getTeacherInitial = (name?: string | null) => name?.trim()?.slice(0, 1) || '师';

function getPositionQueryValue() {
  const queryValue = getSingleQueryValue(route.query.position);
  return positions.includes(queryValue as (typeof positions)[number]) ? queryValue : '';
}

function syncStateFromRouteQuery() {
  selectedPosition.value = getPositionQueryValue();
  currentPage.value = parsePositiveIntQuery(route.query.page, 1);
}

async function syncQueryFromState() {
  const nextPosition = selectedPosition.value;
  const nextPage = currentPage.value;
  const currentPosition = getPositionQueryValue();
  const currentPageQuery = parsePositiveIntQuery(route.query.page, 1);

  if (currentPosition === nextPosition && currentPageQuery === nextPage) {
    return;
  }

  const nextQuery = { ...route.query };
  delete nextQuery.position;
  delete nextQuery.page;

  if (nextPosition) {
    nextQuery.position = nextPosition;
  }

  if (nextPage > 1) {
    nextQuery.page = String(nextPage);
  }

  await router.replace({
    path: route.path,
    query: nextQuery,
  });
}

watch(
  () => route.query,
  () => {
    syncStateFromRouteQuery();
  },
  { immediate: true },
);

watch(filteredTeachers, (teachers) => {
  if (!hasLoadedTeachers.value) {
    return;
  }

  const totalPages = Math.max(1, Math.ceil(teachers.length / pageSize.value));
  if (currentPage.value > totalPages) {
    currentPage.value = totalPages;
    void syncQueryFromState();
  }
});
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
      <div v-if="filteredTeachers.length > 0" class="pagination-controls">
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
      <div v-if="filteredTeachers.length === 0" class="empty-state">
        <div class="empty-state__title">暂时没有结果</div>
        <div class="empty-state__desc">当前筛选条件下没有匹配的导师，试试切换职称或点击重置。</div>
      </div>
      <div v-else class="container">
        <div
          class="teacher_resume_item"
          v-for="(item, index) in paginatedTeachers"
          :key="index"
        >
        <div class="photo" :class="{ 'photo--placeholder': !getPhotoImageUrl(item.photoUrl) }">
            <img v-if="getPhotoImageUrl(item.photoUrl)" :src="getPhotoImageUrl(item.photoUrl)" alt="photo" />
            <span v-else>{{ getTeacherInitial(item.fullName) }}</span>
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
            <div class="avatar" :class="{ 'avatar--empty': !getCardImageUrl(item.avatarUrl) }">
              <img v-if="getCardImageUrl(item.avatarUrl)" :src="getCardImageUrl(item.avatarUrl)" alt="avatar" />
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

.empty-state
  display: flex
  flex-direction: column
  align-items: center
  justify-content: center
  min-height: 360px
  border: 1px dashed rgba(0, 88, 38, .18)
  border-radius: 16px
  background: rgba(255, 255, 255, .42)
  color: #355345

.empty-state__title
  font-size: 24px
  font-weight: 600

.empty-state__desc
  margin-top: 10px
  font-size: 14px
  color: #688171

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
      z-index: 2
    .fullname
      position: absolute
      top: -13%
      left: 22%
      font-size: 18px
      z-index: 2
    .email
      position: absolute
      left: 22%
      display: flex
      align-items: center
      top: 2%
      z-index: 2
    .professional
      position: absolute
      left: 22%
      top: 12%
      z-index: 2
    .detail_box
      position: absolute
      top: 30%
      left: 5%
      z-index: 2
    .avatar
      z-index: 0
      width: 100%
      height: 100%
      overflow: hidden
      position: absolute
      inset: 0
      border-radius: 10px
      img
        width: 100%
        height: 100%
        object-fit: cover
        opacity: .26
  .photo
    box-shadow: #00000030 0 0 5px
    background-color: white
    border-radius: 10px
    z-index: 3
    left: 5%
    width: 75px
    height: 99px
    overflow: hidden
    position: absolute
    display: flex
    align-items: center
    justify-content: center
    img
      width: 100%
      height: 100%
      object-fit: cover

  .photo--placeholder
    background: linear-gradient(160deg, rgba(255, 255, 255, .92) 0%, rgba(230, 237, 244, .98) 100%)
    color: #5f7180
    font-size: 34px
    font-weight: 700
    letter-spacing: .08em

  .avatar--empty
    background: linear-gradient(135deg, rgba(228, 238, 246, .86) 0%, rgba(212, 225, 241, .78) 100%)
</style>
