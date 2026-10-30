<script setup lang="ts">
import { onMounted, ref, computed, watch } from "vue";
import {
  ElPagination,
  ElSelect,
  ElOption,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
} from "element-plus";
import { http, httpStudent } from "@/utils/http";
import { getCardBackgroundImageUrl, getPortraitImageUrl } from "@/utils/media";
import { useRoute, useRouter } from "vue-router";
import { getSingleQueryValue, parsePositiveIntQuery } from "@/utils/routeQuery";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";

interface TeacherInfo {
  uid: number;
  fullName: string;
  teacherposition: string;
  research_direction: string;
  email: string;
  photourl: string | null;
  resume: string;
  avatarUrl: string | null;
}

const allTeacherDetailsList = ref<TeacherInfo[]>([]); // 类型为 TeacherInfo 数组
const currentPage = ref(1);
const pageSize = ref(6);
const positions = ["教授", "副教授", "助理教授", "博士后", "学术顾问", "客座/兼职教授"] as const;
const selectedPosition = ref("");
const dialogVisible = ref(false);
const applicationDialogVisible = ref(false);
const selectReason = ref<string | null>(null);
const selectTeacherUid = ref<number | null>(null);
const selectedTeacher = ref<TeacherInfo | null>(null); // 类型为 TeacherInfo 或 null
const isGeneratingReason = ref(false);
const router = useRouter();
const route = useRoute();
const hasLoadedTeachers = ref(false);
const { isMobile, isTablet } = useResponsiveLayout();
const dialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "86vw" : "640px"));
const applicationDialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "88vw" : "720px"));
const paginationLayout = computed(() => (isMobile.value ? "prev, next" : "prev, pager, next"));

const consumeReasonStream = async (response: Response) => {
  if (!response.body) {
    throw new Error("浏览器不支持流式返回");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  const processEventBlock = (block: string) => {
    const normalizedBlock = block.trim();
    if (!normalizedBlock) {
      return;
    }

    let eventName = "message";
    const dataLines: string[] = [];

    normalizedBlock.split(/\r?\n/).forEach((line) => {
      if (line.startsWith("event:")) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice(5).trimStart());
      }
    });

    if (!dataLines.length) {
      return;
    }

    const payloadText = dataLines.join("\n");
    const payload = JSON.parse(payloadText);

    if (eventName === "chunk") {
      selectReason.value = (selectReason.value || "") + (payload.content || "");
      return;
    }

    if (eventName === "error") {
      throw new Error(payload.message || "生成申请理由失败，请稍后重试！");
    }
  };

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });

    const blocks = buffer.split(/\n\n/);
    buffer = blocks.pop() || "";

    for (const block of blocks) {
      processEventBlock(block);
    }

    if (done) {
      if (buffer.trim()) {
        processEventBlock(buffer);
      }
      break;
    }
  }
};

// 生成AI申请理由功能
const generateAIReason = async () => {
  if (!selectedTeacher.value?.uid) {
    ElMessage.warning("无法获取导师信息！");
    return;
  }
  isGeneratingReason.value = true;
  selectReason.value = "";
  try {
    const response = await fetch("/student/generate-application-reason/stream", {
      method: "POST",
      headers: {
        Accept: "text/event-stream",
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      body: JSON.stringify({
        mentorId: selectedTeacher.value.uid,
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      try {
        const errorJson = JSON.parse(errorText);
        const message = errorJson?.data?.message || errorJson?.data?.error || "生成申请理由失败，请稍后重试！";
        throw new Error(message);
      } catch (parseError) {
        if (parseError instanceof Error) {
          throw parseError;
        }
        throw new Error(errorText || "生成申请理由失败，请稍后重试！");
      }
    }

    await consumeReasonStream(response);
  } catch (error: unknown) {
    console.error(error);
    const message = error instanceof Error ? error.message : "生成申请理由失败，请稍后重试！";
    if (!selectReason.value) {
      ElMessage.error(message);
    }
  } finally {
    isGeneratingReason.value = false;
  }
};

// 获取导师信息
onMounted(() => {
  http({
    url: "/search/teachers",
    method: "GET",
    headers: {
      Accept: "*/*",
      Authorization: `Bearer ${localStorage.getItem("token")}`,
    },
  })
    .then((res) => {
      if (res.data.code === 200) {
        allTeacherDetailsList.value = res.data.data;
        httpStudent({
          url: "/teachers",
          method: "GET",
          headers: {
            Accept: "*/*",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        }).then((res2) => {
          if (res2.data.code === 200) {
            const tdl = res2.data.data;
            allTeacherDetailsList.value.forEach((teacher) => {
              const match = tdl.find((t: TeacherInfo) => t.uid === teacher.uid);
              if (match) {
                Object.assign(teacher, match);
              }
            });
          }
        });

        hasLoadedTeachers.value = true;
      } else {
        ElMessage.error("导师信息获取失败！");
      }
    });
});

// 处理筛选条件
const onPositionChange = () => {
  currentPage.value = 1;
  void syncQueryFromState();
};

const resetFilter = () => {
  selectedPosition.value = "";
  currentPage.value = 1;
  void syncQueryFromState();
};

// 过滤导师
const filteredTeachers = computed(() => {
  if (!selectedPosition.value) {
    return allTeacherDetailsList.value;
  }
  return allTeacherDetailsList.value.filter(
    (teacher) => teacher.teacherposition === selectedPosition.value
  );
});

// 分页处理
const paginatedTeachers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredTeachers.value.slice(start, end);
});

const showDetails = (teacher: TeacherInfo) => {
  selectedTeacher.value = teacher;
  dialogVisible.value = true;
};

const showApplicationDialog = (teacher: TeacherInfo) => {
  selectedTeacher.value = teacher;
  selectTeacherUid.value = teacher.uid;
  applicationDialogVisible.value = true;
};

// 提交申请
const checkApplication = async () => {
  if (!selectReason.value) {
    ElMessage.warning("请填写申请理由");
    return;
  }

  try {
    await ElMessageBox.confirm(
      "提交后将进入待处理状态，期间你将不能再选择其他导师，也不能自行撤销。确认现在提交吗？",
      "确认提交申请",
      {
        confirmButtonText: "确认提交",
        cancelButtonText: "再想想",
        type: "warning",
        closeOnClickModal: false,
        closeOnPressEscape: false,
      }
    );
  } catch {
    return;
  }

  try {
    const res = await http({
      url: "/application/submit",
      method: "POST",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      data: {
        mentorId: selectTeacherUid.value,
        reason: selectReason.value,
      },
    });

    if (res.data.code === 200) {
      applicationDialogVisible.value = false;
      ElMessage.success("申请提交成功，正在跳转到师生关系页面");
      await router.push("/relations");
      return;
    }

    ElMessage.error(res.data?.data?.error || "申请提交失败，请稍后重试！");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.data?.error || "提交申请时出现未知错误，请稍后重试！");
  }
};

// 处理分页页码变化
const handlePageChange = (page: number) => {
  currentPage.value = page;
  void syncQueryFromState();
};

const getCardImageUrl = (url?: string | null) => getCardBackgroundImageUrl(url);
const getPhotoImageUrl = (url?: string | null) => getPortraitImageUrl(url);
const getTeacherInitial = (name?: string | null) => name?.trim()?.slice(0, 1) || "师";

function getPositionQueryValue() {
  const queryValue = getSingleQueryValue(route.query.position);
  return positions.includes(queryValue as (typeof positions)[number]) ? queryValue : "";
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
    <span>选择导师</span>
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
      <div class="pagination-controls">
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
      <div class="container">
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
              <div class="photo" :class="{ 'photo--placeholder': !getPhotoImageUrl(item.photourl) }">
                <img v-if="getPhotoImageUrl(item.photourl)" :src="getPhotoImageUrl(item.photourl)" alt="photo" />
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
              <el-button type="primary" size="small" @click="showApplicationDialog(item)">选择导师</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="dialogVisible" :title="selectedTeacher?.fullName" :width="dialogWidth">
    <div v-if="selectedTeacher">
      <p>职称：{{ selectedTeacher.teacherposition }}</p>
      <p>电子邮箱：{{ selectedTeacher.email }}</p>
      <p>研究方向：{{ selectedTeacher.research_direction }}</p>
      <p>简历：{{ selectedTeacher.resume }}</p>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="applicationDialogVisible" :title="`选择导师：${selectedTeacher?.fullName}`" :width="applicationDialogWidth">
    <el-form @submit.prevent="checkApplication">
      <el-form-item label="申请理由（必填）：">
        <el-input
          type="textarea"
          v-model="selectReason"
          placeholder="请简述"
          :autosize="{ minRows: 2, maxRows: 10 }"
        />
      </el-form-item>

      <el-button type="primary" native-type="submit">提交申请</el-button>
      <el-button v-if="!isGeneratingReason" type="primary" @click="generateAIReason">AI生成申请理由</el-button>
      <el-button v-if="isGeneratingReason" type="primary" disabled>生成中...</el-button>
    </el-form>
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

.container
  width: min(1120px, 100%)
  margin: 0 auto
  display: grid
  grid-template-columns: repeat(2, minmax(0, 1fr))
  gap: 28px
  padding-top: 12px

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
    border-radius: 18px
    border: 1px solid var(--app-border)
    padding: 24px
    box-sizing: border-box
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

@media (max-width: 1120px)
  .top-controls
    flex-direction: column
    align-items: stretch

  .top-controls .pagination-controls
    display: flex
    justify-content: flex-end

  .container
    grid-template-columns: 1fr
    width: min(720px, 100%)

@media (max-width: 767px)
  .title
    min-height: 56px
    padding: 0 16px
    font-size: 18px

  .main-content
    padding-top: 16px

  .top-controls
    margin: 0 12px 16px

  .top-controls .filter-controls,
  .top-controls .pagination-controls
    width: 100%

  .top-controls .filter-controls .el-select,
  .top-controls .filter-controls :deep(.el-button)
    width: 100%

  .top-controls .pagination-controls
    justify-content: flex-start

  .content-wrapper
    padding: 0 12px

  .teacher_resume_item .item_box
    padding: 18px

  .teacher_resume_item .item_box .teacher_head
    flex-direction: column
    align-items: center
    text-align: center

  .teacher_resume_item .item_box .teacher_meta
    align-items: center

  .teacher_resume_item .item_box .card_actions
    flex-direction: column
    :deep(.el-button)
      margin-left: 0
</style>
