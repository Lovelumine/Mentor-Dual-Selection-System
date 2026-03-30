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
  <!-- 固定的标题 -->
  <div class="title">
    <span>选择导师</span>
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
          <div class="photo" :class="{ 'photo--placeholder': !getPhotoImageUrl(item.photourl) }">
            <img v-if="getPhotoImageUrl(item.photourl)" :src="getPhotoImageUrl(item.photourl)" alt="photo" />
            <span v-else>{{ getTeacherInitial(item.fullName) }}</span>
          </div>
          <div class="item_box">
            <span class="fullname">{{ item.fullName }}</span>
            <span class="professional">职位：{{ item.teacherposition }}</span>
            <span class="email">电子邮箱：{{ item.email }}</span>

            <hr class="hea_con_hr" />
            <div class="detail_box">
              <div class="resume">研究方向：{{ item.research_direction }}</div>
              <div class="resume">
                简历：{{ item.resume.length > 50 ? item.resume.slice(0, 50) + '...' : item.resume }}
              </div>

              <!-- 显示详情按钮 -->
              <el-button type="primary" size="small" @click="showDetails(item)">了解更多</el-button>
              <el-button type="primary" size="small" @click="showApplicationDialog(item)">选择导师</el-button>
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
      <p>职称：{{ selectedTeacher.teacherposition }}</p>
      <p>电子邮箱：{{ selectedTeacher.email }}</p>
      <p>研究方向：{{ selectedTeacher.research_direction }}</p>
      <p>简历：{{ selectedTeacher.resume }}</p>
    </div>
    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 申请导师对话框 -->
  <el-dialog v-model="applicationDialogVisible" :title="`选择导师：${selectedTeacher?.fullName}`">
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
  flex-wrap: wrap
  padding-left: 0vw
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
