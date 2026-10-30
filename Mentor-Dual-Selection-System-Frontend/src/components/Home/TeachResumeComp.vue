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
import { ElMessage } from "element-plus";
import type {TeacherDetailImpl} from "@/interfaces/TeachDetailsImpl";
import { getCardBackgroundImageUrl, getPortraitImageUrl } from "@/utils/media";
import { useRoute, useRouter } from "vue-router";
import { getSingleQueryValue, parsePositiveIntQuery } from "@/utils/routeQuery";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";


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
const { isMobile, isTablet } = useResponsiveLayout();
const dialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "86vw" : "640px"));
const paginationLayout = computed(() => (isMobile.value ? "prev, next" : "prev, pager, next"));

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
      ElMessage.error('导师信息获取失败！');
    }
  } catch (error) {
    ElMessage.error('导师信息获取失败！');
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
  <div class="title">
    <span>本院导师卡片</span>
  </div>

  <div class="main-content">
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
      <div v-if="filteredTeachers.length > 0" class="pagination-controls">
        <el-pagination
          background
          :layout="paginationLayout"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredTeachers.length"
          @current-change="handlePageChange"
        >
        </el-pagination>
      </div>
    </div>

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
          <div class="item_box">
            <div class="avatar" :class="{ 'avatar--empty': !getCardImageUrl(item.avatarUrl) }">
              <img v-if="getCardImageUrl(item.avatarUrl)" :src="getCardImageUrl(item.avatarUrl)" alt="avatar" />
            </div>
            <div class="teacher_head">
              <div class="photo" :class="{ 'photo--placeholder': !getPhotoImageUrl(item.photoUrl) }">
                <img v-if="getPhotoImageUrl(item.photoUrl)" :src="getPhotoImageUrl(item.photoUrl)" alt="photo" />
                <span v-else>{{ getTeacherInitial(item.fullName) }}</span>
              </div>
              <div class="teacher_meta">
                <span class="fullname">{{ item.fullName }}</span>
                <span class="professional">职位：{{ item.teacherposition }}</span>
                <span class="email">电子邮箱：{{ item.email }}</span>
              </div>
            </div>
            <hr class="hea_con_hr" />
            <div class="detail_box">
              <div class="resume">研究方向：{{ item.research_direction }}</div>
              <div class="resume">
                简历：{{ item.resume.length > 80 ? item.resume.slice(0, 80) + '...' : item.resume }}
              </div>
            </div>
            <div class="card_actions">
              <el-button type="primary" size="small" @click="showDetails(item)">了解更多</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" :title="selectedTeacher?.fullName" :width="dialogWidth">
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
  <div class="bottom_spacing"></div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  min-height: 60px
  background-color: var(--app-surface-muted)
  display: flex
  align-items: center
  padding: 0 20px
  font-size: 20px
  color: var(--app-text-primary)
  border-bottom: 1px solid var(--app-border)
  box-shadow: 0 12px 24px var(--app-shadow)

.main-content
  padding-top: 20px

.top-controls
  display: flex
  justify-content: space-between
  align-items: center
  margin: 0 20px 20px
  gap: 16px
  .filter-controls
    display: flex
    align-items: center
    flex-wrap: wrap
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
  width: min(1120px, 100%)
  margin: 0 auto
  display: grid
  grid-template-columns: repeat(2, minmax(0, 1fr))
  gap: 36px 40px
  padding-top: 12px
  align-items: start

.teacher_resume_item
  min-width: 0
  .item_box
    width: 100%
    min-height: 100%
    position: relative
    overflow: hidden
    display: flex
    flex-direction: column
    gap: 16px
    padding: 24px
    box-sizing: border-box
    border-radius: 18px
    border: 1px solid var(--app-border)
    background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
    box-shadow: 0 18px 32px var(--app-shadow)
    isolation: isolate
    .hea_con_hr
      width: 100%
      margin: 0
      border: none
      border-top: 1px solid var(--app-border)
    .teacher_head
      display: flex
      align-items: flex-start
      gap: 18px
    .teacher_meta
      display: flex
      flex-direction: column
      gap: 8px
      min-width: 0
      flex: 1 1 auto
    .fullname
      font-size: 24px
      font-weight: 700
      color: var(--app-text-primary)
    .email,
    .professional
      color: var(--app-text-secondary)
      word-break: break-word
    .detail_box
      position: relative
      z-index: 2
      display: flex
      flex-direction: column
      gap: 12px
    .card_actions
      position: relative
      z-index: 2
      display: flex
      flex-wrap: wrap
      gap: 10px
    .avatar
      z-index: 0
      position: absolute
      inset: 0
      width: 100%
      height: 100%
      overflow: hidden
      border-radius: 18px
      img
        width: 100%
        height: 100%
        object-fit: cover
        opacity: .26
  .photo
    box-shadow: 0 10px 24px rgba(0, 0, 0, .12)
    background-color: white
    border-radius: 14px
    z-index: 3
    width: 75px
    height: 99px
    overflow: hidden
    display: flex
    align-items: center
    justify-content: center
    flex: 0 0 auto
    img
      width: 100%
      height: 100%
      object-fit: cover
  .resume
    color: var(--app-text-primary)
    line-height: 1.7
    word-break: break-word

  .photo--placeholder
    background: linear-gradient(160deg, rgba(255, 255, 255, .92) 0%, rgba(230, 237, 244, .98) 100%)
    color: #5f7180
    font-size: 34px
    font-weight: 700
    letter-spacing: .08em

  .avatar--empty
    background: linear-gradient(135deg, rgba(228, 238, 246, .86) 0%, rgba(212, 225, 241, .78) 100%)

.bottom_spacing
  height: 56px

@media (max-width: 1280px)
  .container
    width: min(980px, 100%)
    gap: 28px 24px

@media (max-width: 1120px)
  .top-controls
    flex-direction: column
    align-items: stretch
    gap: 12px

  .top-controls .filter-controls
    justify-content: space-between
    flex-wrap: wrap

  .top-controls .pagination-controls
    display: flex
    justify-content: flex-end

  .container
    grid-template-columns: 1fr
    width: min(640px, 100%)

@media (max-width: 768px)
  .title
    min-height: 56px
    width: 100%
    padding: 0 16px
    font-size: 18px

  .main-content
    padding-top: 16px

  .content-wrapper
    padding: 0 12px

  .top-controls
    margin: 0 12px 16px

  .top-controls .filter-controls
    width: 100%
    gap: 8px

  .top-controls .filter-controls .el-select,
  .top-controls .filter-controls :deep(.el-button)
    width: 100%

  .container
    width: 100%

  .teacher_resume_item .item_box
    padding: 18px

  .teacher_resume_item .item_box .teacher_head
    flex-direction: column
    align-items: center
    text-align: center

  .teacher_resume_item .item_box .teacher_meta
    align-items: center
</style>
