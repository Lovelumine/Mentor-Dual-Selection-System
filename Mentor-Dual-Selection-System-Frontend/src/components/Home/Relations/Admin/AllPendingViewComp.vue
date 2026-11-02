<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { http } from "@/utils/http";
import { getPortraitImageCandidates } from "@/utils/media";
import { getSingleQueryValue, parseEnumQuery } from "@/utils/routeQuery";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";

interface MentorSummary {
  uid: number;
  fullName: string;
  email: string;
  photoUrl?: string | null;
  avatarUrl?: string | null;
  acceptedStudents?: number | null;
  studentCount: number;
  pendingApplicationCount: number;
  capacity: number;
  status: "ACTIVE" | "EMPTY" | "FULL";
}

interface StudentSummary {
  uid: number;
  fullName: string;
  username: string;
  email: string;
  avatarUrl?: string | null;
  mentorId?: number | null;
  grade?: string | null;
}

interface MentorGroup {
  mentor: MentorSummary;
  students: StudentSummary[];
}

interface RelationsSummary {
  mentorCount: number;
  assignedStudentCount: number;
  unassignedStudentCount: number;
  fullMentorCount: number;
}

interface AdminMentorGroupData {
  summary: RelationsSummary;
  groups: MentorGroup[];
  unassignedStudents: StudentSummary[];
}

interface ApiResponse {
  code: number;
  data: AdminMentorGroupData;
}

interface ResetBatchRecord {
  id: number;
  reason: string;
  status: string;
  statusCN: string;
  createdAt: string | null;
  createdByName: string | null;
  affectedStudentCount: number;
  affectedMentorCount: number;
  restoreCount: number;
  lastRestoredAt: string | null;
  lastRestoredByName: string | null;
  restorable: boolean;
}

interface ResetBatchApiResponse {
  code: number;
  data: ResetBatchRecord[];
}

interface StudentSlot {
  index: number;
  student: StudentSummary | null;
}

type StatusFilter = "all" | "hasStudents" | "empty" | "full";
type SortMode = "countDesc" | "countAsc" | "nameAsc";

const route = useRoute();
const router = useRouter();
const statusFilterOptions = ["all", "hasStudents", "empty", "full"] as const;
const sortModeOptions = ["countDesc", "countAsc", "nameAsc"] as const;
const loading = ref(false);
const searchKeyword = ref("");
const statusFilter = ref<StatusFilter>("all");
const sortMode = ref<SortMode>("countDesc");
const expandedMentorIds = ref<string[]>([]);

const summary = ref<RelationsSummary>({
  mentorCount: 0,
  assignedStudentCount: 0,
  unassignedStudentCount: 0,
  fullMentorCount: 0,
});
const mentorGroups = ref<MentorGroup[]>([]);
const unassignedStudents = ref<StudentSummary[]>([]);

const selectedStudentIdsByMentor = ref<Record<string, number[]>>({});
const unbindDialogVisible = ref(false);
const studentsToUnbind = ref<StudentSummary[]>([]);
const targetMentor = ref<MentorSummary | null>(null);
const unbinding = ref(false);
const brokenMentorImageMap = ref<Record<string, boolean>>({});
const resetBatches = ref<ResetBatchRecord[]>([]);
const resetAllDialogVisible = ref(false);
const resetAllReason = ref("");
const resettingAll = ref(false);
const restoringBatchId = ref<number | null>(null);
const { isMobile, isTablet } = useResponsiveLayout();
const unbindDialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "86vw" : "520px"));
const resetAllDialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "88vw" : "560px"));

const keyword = computed(() => searchKeyword.value.trim().toLowerCase());

function syncFiltersFromRouteQuery() {
  searchKeyword.value = getSingleQueryValue(route.query.keyword);
  statusFilter.value = parseEnumQuery(route.query.scope, statusFilterOptions, "all");
  sortMode.value = parseEnumQuery(route.query.sort, sortModeOptions, "countDesc");
}

async function syncFilterQuery() {
  const nextKeyword = searchKeyword.value.trim();
  const nextScope = statusFilter.value;
  const nextSort = sortMode.value;
  const currentKeyword = getSingleQueryValue(route.query.keyword);
  const currentScope = parseEnumQuery(route.query.scope, statusFilterOptions, "all");
  const currentSort = parseEnumQuery(route.query.sort, sortModeOptions, "countDesc");

  if (currentKeyword === nextKeyword && currentScope === nextScope && currentSort === nextSort) {
    return;
  }

  const nextQuery = { ...route.query };
  delete nextQuery.keyword;
  delete nextQuery.scope;
  delete nextQuery.sort;

  if (nextKeyword) {
    nextQuery.keyword = nextKeyword;
  }

  if (nextScope !== "all") {
    nextQuery.scope = nextScope;
  }

  if (nextSort !== "countDesc") {
    nextQuery.sort = nextSort;
  }

  await router.replace({
    path: route.path,
    query: nextQuery,
  });
}

function normalizeText(value: unknown) {
  if (value == null) {
    return "";
  }
  return String(value).trim().toLowerCase();
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return date
    .toLocaleString("zh-CN", {
      hour12: false,
      timeZone: "Asia/Shanghai",
    })
    .replace(/\//g, "-");
}

function mentorMatchesKeyword(mentor: MentorSummary, normalizedKeyword: string) {
  if (!normalizedKeyword) {
    return true;
  }

  return (
    normalizeText(mentor.fullName).includes(normalizedKeyword) ||
    normalizeText(mentor.email).includes(normalizedKeyword)
  );
}

function studentMatchesKeyword(student: StudentSummary, normalizedKeyword: string) {
  if (!normalizedKeyword) {
    return true;
  }

  return (
    normalizeText(student.fullName).includes(normalizedKeyword) ||
    normalizeText(student.username).includes(normalizedKeyword) ||
    normalizeText(student.email).includes(normalizedKeyword) ||
    normalizeText(student.grade).includes(normalizedKeyword)
  );
}

const filteredGroups = computed<MentorGroup[]>(() => {
  const normalizedKeyword = keyword.value;

  return mentorGroups.value
    .map((group) => {
      const mentorMatched = mentorMatchesKeyword(group.mentor, normalizedKeyword);

      const students = mentorMatched
        ? group.students
        : group.students.filter((student) => studentMatchesKeyword(student, normalizedKeyword));

      return {
        ...group,
        students,
      };
    })
    .filter((group) => {
      const originalGroup = mentorGroups.value.find((item) => item.mentor.uid === group.mentor.uid);
      if (!originalGroup) {
        return false;
      }

      if (statusFilter.value === "hasStudents" && originalGroup.mentor.studentCount === 0) {
        return false;
      }
      if (statusFilter.value === "empty" && originalGroup.mentor.studentCount > 0) {
        return false;
      }
      if (statusFilter.value === "full" && originalGroup.mentor.status !== "FULL") {
        return false;
      }

      if (normalizedKeyword.length === 0) {
        return true;
      }

      return mentorMatchesKeyword(group.mentor, normalizedKeyword) || group.students.length > 0;
    })
    .sort((left, right) => {
      if (sortMode.value === "nameAsc") {
        return left.mentor.fullName.localeCompare(right.mentor.fullName, "zh-CN");
      }

      const delta = left.mentor.studentCount - right.mentor.studentCount;
      if (delta !== 0) {
        return sortMode.value === "countAsc" ? delta : -delta;
      }
      return left.mentor.fullName.localeCompare(right.mentor.fullName, "zh-CN");
    });
});

const filteredUnassignedStudents = computed(() => {
  if (!keyword.value) {
    return unassignedStudents.value;
  }

  return unassignedStudents.value.filter((student) => studentMatchesKeyword(student, keyword.value));
});

const visibleStudentCount = computed(() =>
  filteredGroups.value.reduce((total, group) => total + group.students.length, 0),
);

const visibleMentorCount = computed(() => filteredGroups.value.length);
const MAX_STUDENT_SLOTS = 9;

function getSelectionKey(mentorId: number) {
  return String(mentorId);
}

function getDefaultExpandedMentors(groups: MentorGroup[]) {
  return groups
    .filter((group) => group.mentor.studentCount > 0 || group.mentor.pendingApplicationCount > 0)
    .slice(0, 6)
    .map((group) => getSelectionKey(group.mentor.uid));
}

function applyData(data: AdminMentorGroupData) {
  summary.value = data.summary;
  mentorGroups.value = data.groups;
  unassignedStudents.value = data.unassignedStudents;
  selectedStudentIdsByMentor.value = {};

  if (expandedMentorIds.value.length === 0) {
    expandedMentorIds.value = getDefaultExpandedMentors(data.groups);
    return;
  }

  const validMentorIds = new Set(data.groups.map((group) => getSelectionKey(group.mentor.uid)));
  expandedMentorIds.value = expandedMentorIds.value.filter((mentorId) => validMentorIds.has(mentorId));
}

async function fetchRelations() {
  loading.value = true;
  try {
    const res = await http<ApiResponse>({
      url: "/application/admin/mentor-groups",
      method: "GET",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.data.code !== 200) {
      throw new Error("获取导师分组失败");
    }

    applyData(res.data.data);
  } catch (error: any) {
    const message = error?.response?.data?.data?.error || error?.message || "获取导师分组失败";
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
}

async function fetchResetBatches() {
  try {
    const res = await http<ResetBatchApiResponse>({
      url: "/application/admin/reset-batches",
      method: "GET",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.data.code !== 200) {
      throw new Error("获取重置批次失败");
    }

    resetBatches.value = res.data.data;
  } catch (error: any) {
    const message = error?.response?.data?.data?.error || error?.message || "获取重置批次失败";
    ElMessage.error(message);
  }
}

async function fetchPageData() {
  await Promise.all([fetchRelations(), fetchResetBatches()]);
}

function getStatusLabel(status: MentorSummary["status"]) {
  switch (status) {
    case "FULL":
      return "已满额";
    case "EMPTY":
      return "空组";
    default:
      return "进行中";
  }
}

function getStatusType(status: MentorSummary["status"]) {
  switch (status) {
    case "FULL":
      return "danger";
    case "EMPTY":
      return "info";
    default:
      return "success";
  }
}

function getMentorInitial(fullName: string) {
  const value = fullName.trim();
  return value ? value.slice(0, 1) : "导";
}

function getCandidateMentorImages(mentor: MentorSummary) {
  return Array.from(new Set([
    ...getPortraitImageCandidates(mentor.photoUrl),
    ...getPortraitImageCandidates(mentor.avatarUrl),
  ]));
}

function getMentorImageUrl(mentor: MentorSummary) {
  for (const url of getCandidateMentorImages(mentor)) {
    if (!brokenMentorImageMap.value[url]) {
      return url;
    }
  }
  return "";
}

function hasMentorImage(mentor: MentorSummary) {
  return Boolean(getMentorImageUrl(mentor));
}

function markMentorImageBroken(mentor: MentorSummary) {
  const currentUrl = getMentorImageUrl(mentor);
  if (currentUrl) {
    brokenMentorImageMap.value[currentUrl] = true;
  }
}

function getStudentSlots(students: StudentSummary[]): StudentSlot[] {
  return Array.from({ length: MAX_STUDENT_SLOTS }, (_, index) => ({
    index,
    student: students[index] ?? null,
  }));
}

function selectedCountFor(mentorId: number) {
  return selectedStudentIdsByMentor.value[getSelectionKey(mentorId)]?.length ?? 0;
}

function handleSelectionChange(mentorId: number, rows: StudentSummary[]) {
  selectedStudentIdsByMentor.value[getSelectionKey(mentorId)] = rows.map((row) => row.uid);
}

function openUnbindDialog(students: StudentSummary[], mentor: MentorSummary) {
  studentsToUnbind.value = students;
  targetMentor.value = mentor;
  unbindDialogVisible.value = true;
}

function handleSingleUnbind(student: StudentSummary, mentor: MentorSummary) {
  openUnbindDialog([student], mentor);
}

function handleBatchUnbind(group: MentorGroup) {
  const selectedIds = new Set(selectedStudentIdsByMentor.value[getSelectionKey(group.mentor.uid)] ?? []);
  if (selectedIds.size === 0) {
    ElMessage.warning("请先勾选要解除关系的学生");
    return;
  }

  openUnbindDialog(
    group.students.filter((student) => selectedIds.has(student.uid)),
    group.mentor,
  );
}

async function confirmUnbind() {
  if (studentsToUnbind.value.length === 0) {
    return;
  }

  unbinding.value = true;
  try {
    const res = await http({
      url: "/application/admin/unbind",
      method: "POST",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      data: {
        studentIds: studentsToUnbind.value.map((student) => student.uid),
      },
    });

    if (res.data.code !== 200) {
      throw new Error(res.data?.data?.error || "解除关系失败");
    }

    ElMessage.success(`已解除 ${res.data.data.removedCount} 名学生的导师关系`);
    unbindDialogVisible.value = false;
    studentsToUnbind.value = [];
    targetMentor.value = null;
    await fetchPageData();
  } catch (error: any) {
    const message = error?.response?.data?.data?.error || error?.message || "解除关系失败";
    ElMessage.error(message);
  } finally {
    unbinding.value = false;
  }
}

function openResetAllDialog() {
  resetAllReason.value = "";
  resetAllDialogVisible.value = true;
}

async function confirmResetAll() {
  resettingAll.value = true;
  try {
    const res = await http({
      url: "/application/admin/unbind-all",
      method: "POST",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      data: {
        reason: resetAllReason.value,
      },
    });

    if (res.data.code !== 200) {
      throw new Error(res.data?.data?.error || "统一重置导师关系失败");
    }

    ElMessage.success(
      `已统一解除 ${res.data.data.removedCount} 名学生与 ${res.data.data.affectedMentorCount} 位导师的关系`,
    );
    resetAllDialogVisible.value = false;
    resetAllReason.value = "";
    await fetchPageData();
  } catch (error: any) {
    const message = error?.response?.data?.data?.error || error?.message || "统一重置导师关系失败";
    ElMessage.error(message);
  } finally {
    resettingAll.value = false;
  }
}

async function restoreBatch(batch: ResetBatchRecord) {
  try {
    await ElMessageBox.confirm(
      `将批次 #${batch.id} 中涉及的 ${batch.affectedStudentCount} 名学生恢复到解除前的导师关系。该批次之外的学生，其当前关系不受影响；若这些学生已重新分配导师，会被批次内的原导师覆盖。历史记录仍会保留。`,
      "确认恢复批次",
      {
        confirmButtonText: "确认恢复",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  } catch {
    return;
  }

  restoringBatchId.value = batch.id;
  try {
    const res = await http({
      url: `/application/admin/reset-batches/${batch.id}/restore`,
      method: "POST",
      headers: {
        Accept: "*/*",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.data.code !== 200) {
      throw new Error(res.data?.data?.error || "恢复批次失败");
    }

    ElMessage.success(`已恢复批次 #${batch.id} 的导师关系快照`);
    await fetchPageData();
  } catch (error: any) {
    const message = error?.response?.data?.data?.error || error?.message || "恢复批次失败";
    ElMessage.error(message);
  } finally {
    restoringBatchId.value = null;
  }
}

function expandAll() {
  expandedMentorIds.value = filteredGroups.value.map((group) => getSelectionKey(group.mentor.uid));
}

function collapseAll() {
  expandedMentorIds.value = [];
}

watch(
  () => route.query,
  () => {
    syncFiltersFromRouteQuery();
  },
  { immediate: true },
);

watch(
  [searchKeyword, statusFilter, sortMode],
  () => {
    void syncFilterQuery();
  },
);

watch(
  filteredGroups,
  (groups) => {
    const visibleIds = new Set(groups.map((group) => getSelectionKey(group.mentor.uid)));
    if (keyword.value) {
      expandedMentorIds.value = groups.map((group) => getSelectionKey(group.mentor.uid));
      return;
    }
    expandedMentorIds.value = expandedMentorIds.value.filter((mentorId) => visibleIds.has(mentorId));
  },
  { deep: true },
);

onMounted(() => {
  fetchPageData();
});
</script>

<template>
  <div class="all_pending_box admin-relations" v-loading="loading">
    <section class="overview">
      <div class="stats-grid">
        <article class="stat-card">
          <span class="stat-card__label">导师总数</span>
          <strong class="stat-card__value">{{ summary.mentorCount }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-card__label">已分配学生</span>
          <strong class="stat-card__value">{{ summary.assignedStudentCount }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-card__label">未分配学生</span>
          <strong class="stat-card__value">{{ summary.unassignedStudentCount }}</strong>
        </article>
        <article class="stat-card">
          <span class="stat-card__label">已满额导师</span>
          <strong class="stat-card__value">{{ summary.fullMentorCount }}</strong>
        </article>
      </div>
    </section>

    <section class="toolbar">
      <div class="toolbar__filters">
        <div class="toolbar-field toolbar-field--search">
          <label class="toolbar-field__label" for="relations-search">搜索</label>
          <el-input
            id="relations-search"
            v-model="searchKeyword"
            clearable
            placeholder="输入导师姓名、学生姓名、学号或邮箱"
            class="toolbar__search"
          />
        </div>
        <div class="toolbar-field">
          <span class="toolbar-field__label">分组范围</span>
          <el-select v-model="statusFilter" class="toolbar__select">
            <el-option label="全部导师" value="all" />
            <el-option label="仅看有学生" value="hasStudents" />
            <el-option label="仅看空组" value="empty" />
            <el-option label="仅看已满额" value="full" />
          </el-select>
        </div>
        <div class="toolbar-field">
          <span class="toolbar-field__label">排序方式</span>
          <el-select v-model="sortMode" class="toolbar__select">
            <el-option label="按学生数从多到少" value="countDesc" />
            <el-option label="按学生数从少到多" value="countAsc" />
            <el-option label="按导师姓名" value="nameAsc" />
          </el-select>
        </div>
      </div>
      <div class="toolbar__actions">
        <span class="toolbar__hint">
          {{ visibleMentorCount }} 组导师 · {{ visibleStudentCount }} 名学生
        </span>
        <el-button plain @click="expandAll">展开全部</el-button>
        <el-button plain @click="collapseAll">收起全部</el-button>
        <el-button type="danger" plain @click="openResetAllDialog">解除全部关系</el-button>
        <el-button type="primary" @click="fetchPageData">刷新数据</el-button>
      </div>
    </section>

    <section v-if="resetBatches.length > 0" class="batch-panel">
      <div class="batch-panel__header">
        <div>
          <span class="batch-panel__title">批量重置记录</span>
          <span class="batch-panel__subtitle">可从任意未失效批次恢复</span>
        </div>
        <el-tag size="small" effect="plain">{{ resetBatches.length }} 个批次</el-tag>
      </div>

      <div class="batch-list">
        <article
          v-for="batch in resetBatches"
          :key="batch.id"
          class="batch-item"
        >
          <div class="batch-item__main">
            <div class="batch-item__top">
              <span class="batch-item__id">批次 #{{ batch.id }}</span>
              <span class="batch-item__time">{{ formatDateTime(batch.createdAt) }}</span>
              <el-tag :type="batch.restorable ? 'success' : 'info'" effect="dark" size="small">
                {{ batch.statusCN }}
              </el-tag>
            </div>
            <div class="batch-item__reason">{{ batch.reason }}</div>
            <div class="batch-item__meta">
              <span>{{ batch.createdByName || "未知操作人" }}</span>
              <span>{{ batch.affectedStudentCount }} 名学生 · {{ batch.affectedMentorCount }} 位导师</span>
              <span>已恢复 {{ batch.restoreCount }} 次</span>
              <span v-if="batch.lastRestoredAt">
                上次恢复：{{ formatDateTime(batch.lastRestoredAt) }}<template v-if="batch.lastRestoredByName"> · {{ batch.lastRestoredByName }}</template>
              </span>
              <span v-else>尚未恢复</span>
            </div>
          </div>
          <div class="batch-item__actions">
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!batch.restorable"
              :loading="restoringBatchId === batch.id"
              @click="restoreBatch(batch)"
            >
              恢复此批次
            </el-button>
          </div>
        </article>
      </div>
    </section>

    <section v-if="filteredGroups.length > 0" class="group-list">
      <el-collapse v-model="expandedMentorIds">
        <el-collapse-item
          v-for="group in filteredGroups"
          :key="group.mentor.uid"
          :name="String(group.mentor.uid)"
          class="mentor-group"
        >
          <template #title>
            <div class="group-summary">
              <div class="group-summary__lead">
                <div class="mentor-avatar" :class="{ 'mentor-avatar--image': hasMentorImage(group.mentor) }">
                  <img
                    v-if="hasMentorImage(group.mentor)"
                    :src="getMentorImageUrl(group.mentor)"
                    :alt="`${group.mentor.fullName} 照片`"
                    @error="markMentorImageBroken(group.mentor)"
                  />
                  <span v-else>{{ getMentorInitial(group.mentor.fullName) }}</span>
                </div>
                <div class="group-summary__main">
                  <div class="group-summary__name">{{ group.mentor.fullName }}</div>
                  <div class="group-summary__meta">{{ group.mentor.email }}</div>
                </div>
              </div>
              <div class="student-slot-grid">
                <div
                  v-for="slot in getStudentSlots(group.students)"
                  :key="`${group.mentor.uid}-${slot.index}`"
                  class="student-slot"
                  :class="{ 'student-slot--empty': !slot.student }"
                >
                  <template v-if="slot.student">
                    <span class="student-slot__name">{{ slot.student.fullName }}</span>
                    <span class="student-slot__meta">{{ slot.student.username }}</span>
                  </template>
                  <template v-else>
                    <span class="student-slot__empty">空位 {{ slot.index + 1 }}</span>
                  </template>
                </div>
              </div>
              <div class="group-summary__aside">
                <div class="group-summary__tags">
                  <el-tag size="small" effect="plain">
                    {{ group.mentor.studentCount }}/{{ group.mentor.capacity }} 名学生
                  </el-tag>
                  <el-tag
                    v-if="group.mentor.pendingApplicationCount > 0"
                    size="small"
                    type="warning"
                    effect="plain"
                  >
                    {{ group.mentor.pendingApplicationCount }} 条待处理申请
                  </el-tag>
                  <el-tag :type="getStatusType(group.mentor.status)" size="small" effect="dark">
                    {{ getStatusLabel(group.mentor.status) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </template>

          <div class="group-body">
            <div class="group-body__header">
              <div>
                <h5>组内学生</h5>
                <p>勾选后可批量解除该导师与学生的关系。</p>
              </div>
              <div class="group-body__actions">
                <span class="selection-hint">
                  已选 {{ selectedCountFor(group.mentor.uid) }} 名
                </span>
                <el-button
                  type="danger"
                  plain
                  :disabled="selectedCountFor(group.mentor.uid) === 0"
                  @click.stop="handleBatchUnbind(group)"
                >
                  批量解除
                </el-button>
              </div>
            </div>

            <div class="students-table-shell">
              <el-table
                :data="group.students"
                row-key="uid"
                class="students-table"
                empty-text="该导师当前没有学生"
                @selection-change="handleSelectionChange(group.mentor.uid, $event)"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column prop="fullName" label="学生姓名" min-width="140" />
                <el-table-column prop="username" label="学号" min-width="140" />
                <el-table-column prop="grade" label="年级" min-width="100">
                  <template #default="{ row }">
                    {{ row.grade || "未填写" }}
                  </template>
                </el-table-column>
                <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
                <el-table-column label="操作" width="120" align="right">
                  <template #default="{ row }">
                    <el-button
                      link
                      type="danger"
                      @click.stop="handleSingleUnbind(row, group.mentor)"
                    >
                      解除关系
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </section>

    <section v-if="filteredUnassignedStudents.length > 0" class="unassigned-panel">
      <div class="panel-header">
        <div>
          <p class="panel-header__eyebrow">未分配学生</p>
          <h4>当前未绑定导师的学生</h4>
          <p class="panel-header__desc">这部分学生会在解除关系后自动回到这里，方便继续分配。</p>
        </div>
        <el-tag type="warning" effect="dark">
          {{ filteredUnassignedStudents.length }} 人
        </el-tag>
      </div>

      <div class="students-table-shell">
        <el-table :data="filteredUnassignedStudents" row-key="uid" class="students-table">
          <el-table-column prop="fullName" label="学生姓名" min-width="140" />
          <el-table-column prop="username" label="学号" min-width="140" />
          <el-table-column prop="grade" label="年级" min-width="100">
            <template #default="{ row }">
              {{ row.grade || "未填写" }}
            </template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
    </section>

    <section
      v-if="!loading && filteredGroups.length === 0 && filteredUnassignedStudents.length === 0 && resetBatches.length === 0"
      class="empty-page"
    >
      <h4>没有匹配到分组</h4>
      <p>试试缩短搜索词，或者切换筛选条件。</p>
    </section>
  </div>

  <el-dialog
    v-model="unbindDialogVisible"
    :width="unbindDialogWidth"
    :title="`确认解除关系${targetMentor ? ` · ${targetMentor.fullName}` : ''}`"
  >
    <div class="unbind-dialog">
      <p class="unbind-dialog__desc">
        将解除以下 {{ studentsToUnbind.length }} 名学生与当前导师的关系：
      </p>
      <div class="unbind-dialog__chips">
        <span
          v-for="student in studentsToUnbind"
          :key="student.uid"
          class="student-chip"
        >
          {{ student.fullName }}
        </span>
      </div>
      <p class="unbind-dialog__notice">
        解除后，这些学生会进入“未分配学生”分组，导师当前学生数会同步减少。
      </p>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="unbindDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="unbinding" @click="confirmUnbind">
          确认解除
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    v-model="resetAllDialogVisible"
    :width="resetAllDialogWidth"
    title="统一解除全部导师关系"
  >
    <div class="unbind-dialog">
      <p class="unbind-dialog__desc">
        此操作通常用于新学期或新周期开始时统一重置当前师生关系。
      </p>
      <el-input
        v-model="resetAllReason"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="可填写本次统一重置的原因；留空则默认记录为“统一重置导师关系”"
      />
      <p class="unbind-dialog__notice">
        执行后会生成可恢复的批次快照。后续若恢复某批次，将以该批次快照覆盖当前师生关系，但历史记录不会删除。
      </p>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="resetAllDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="resettingAll" @click="confirmResetAll">
          确认统一解除
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="sass">
.all_pending_box
  margin: 40px auto 24px auto
  width: min(1180px, calc(100% - 40px))
  color: var(--app-text-primary)

.overview
  padding: 0 0 16px 0
  background: transparent
  border: none
  border-radius: 0
  box-shadow: none

.stats-grid
  display: grid
  grid-template-columns: repeat(4, minmax(0, 1fr))
  gap: 0
  border-top: 1px solid var(--app-border)
  border-bottom: 1px solid var(--app-border)

.stat-card
  display: flex
  flex-direction: column
  gap: 8px
  min-height: 116px
  padding: 18px 22px
  background: transparent
  border: none
  border-right: 1px solid var(--app-border)
  border-radius: 0
  box-shadow: none

.stat-card:last-child
  border-right: none

.stat-card__label
  display: block
  color: var(--app-text-secondary)
  font-size: 14px

.stat-card__value
  display: block
  font-size: 34px
  line-height: 1

.toolbar
  display: flex
  align-items: center
  justify-content: space-between
  gap: 16px
  margin-top: 14px
  padding: 16px 0 18px 0
  background: transparent
  border: none
  border-bottom: 1px solid var(--app-border)
  border-radius: 0
  box-shadow: none

.toolbar__filters
  display: flex
  flex: 1
  gap: 16px
  align-items: flex-end

.toolbar-field
  display: flex
  flex-direction: column
  gap: 8px
  min-width: 0

.toolbar-field--search
  flex: 1

.toolbar-field__label
  color: var(--app-text-secondary)
  font-size: 12px
  line-height: 1
  letter-spacing: .04em

.toolbar__search
  width: min(420px, 100%)

.toolbar__select
  width: 180px

.toolbar__actions
  display: flex
  align-items: center
  gap: 10px

.toolbar__hint
  color: var(--app-text-secondary)
  font-size: 14px
  white-space: nowrap

.unassigned-panel,
.batch-panel,
.group-list,
.empty-page
  margin-top: 16px

.batch-panel
  padding: 14px 16px
  background: color-mix(in srgb, var(--app-brand) 4%, var(--app-surface) 96%)
  border: 1px solid var(--app-border)
  border-radius: 12px
  box-shadow: none

.batch-panel__header
  display: flex
  align-items: center
  justify-content: space-between
  gap: 12px
  margin-bottom: 10px

.batch-panel__title
  font-size: 15px
  font-weight: 700
  color: var(--app-text-primary)

.batch-panel__subtitle
  margin-left: 10px
  font-size: 13px
  color: var(--app-text-secondary)

.batch-list
  display: flex
  flex-direction: column
  gap: 10px

.batch-item
  display: grid
  grid-template-columns: minmax(0, 1fr) auto
  gap: 16px
  align-items: center
  padding: 12px 14px
  border: 1px solid var(--app-border)
  border-radius: 12px
  background: var(--app-surface)

.batch-item__main
  min-width: 0

.batch-item__top
  display: flex
  align-items: center
  gap: 10px
  flex-wrap: wrap

.batch-item__id
  font-size: 14px
  font-weight: 700
  color: var(--app-text-primary)

.batch-item__time
  font-size: 13px
  color: var(--app-text-secondary)

.batch-item__reason
  margin-top: 6px
  font-size: 14px
  color: var(--app-text-primary)

.batch-item__meta
  display: flex
  flex-wrap: wrap
  gap: 8px 14px
  margin-top: 6px
  font-size: 13px
  color: var(--app-text-secondary)

.batch-item__actions
  display: flex
  align-items: center

.unassigned-panel
  padding: 20px
  background: linear-gradient(180deg, color-mix(in srgb, #c48a1e 9%, var(--app-surface) 91%) 0%, var(--app-surface) 100%)
  border: 1px solid color-mix(in srgb, #c48a1e 28%, var(--app-border) 72%)
  border-radius: 14px
  box-shadow: none

.panel-header
  display: flex
  align-items: center
  justify-content: space-between
  gap: 12px
  margin-bottom: 14px

.panel-header__eyebrow
  margin: 0 0 4px 0
  font-size: 12px
  letter-spacing: .08em
  text-transform: uppercase
  color: var(--app-text-secondary)

.panel-header h4
  margin: 0
  font-size: 22px

.panel-header__desc
  margin: 6px 0 0 0
  color: var(--app-text-secondary)

.mentor-group
  margin-bottom: 14px
  border: 1px solid var(--app-border)
  border-radius: 14px
  overflow: hidden
  background: linear-gradient(180deg, color-mix(in srgb, var(--app-brand) 6%, var(--app-surface) 94%) 0%, var(--app-surface) 100%)
  box-shadow: none

.mentor-group :deep(.el-collapse-item__wrap)
  background: transparent
  border-top: 1px solid var(--app-border)

.mentor-group :deep(.el-collapse-item__header)
  height: auto
  min-height: 0
  padding: 18px 22px 16px 22px
  background: linear-gradient(90deg, color-mix(in srgb, var(--app-brand) 16%, var(--app-surface) 84%) 0%, color-mix(in srgb, var(--app-brand) 6%, var(--app-surface) 94%) 100%)
  border-bottom: none
  align-items: stretch
  line-height: normal

.mentor-group :deep(.el-collapse-item__arrow)
  align-self: center

.mentor-group :deep(.el-collapse-item__content)
  padding-bottom: 0

.group-summary
  display: grid
  grid-template-columns: minmax(220px, 280px) minmax(360px, 1fr) minmax(170px, 220px)
  align-items: center
  width: 100%
  gap: 20px
  padding-right: 8px

.group-summary__lead
  display: flex
  align-items: center
  gap: 14px
  min-width: 0
  flex: 1

.mentor-avatar
  width: 76px
  height: 96px
  border-radius: 16px
  display: inline-flex
  align-items: center
  justify-content: center
  flex-shrink: 0
  background: linear-gradient(150deg, color-mix(in srgb, var(--app-brand) 78%, #ffffff 22%) 0%, var(--app-brand) 100%)
  color: var(--app-text-inverse)
  font-size: 22px
  font-weight: 800
  box-shadow: 0 12px 24px color-mix(in srgb, var(--app-brand) 24%, transparent 76%)
  overflow: hidden
  border: 1px solid color-mix(in srgb, var(--app-brand) 18%, var(--app-border) 82%)

.mentor-avatar span
  display: inline-flex
  align-items: center
  justify-content: center
  width: 100%
  height: 100%

.mentor-avatar--image
  background: color-mix(in srgb, var(--app-brand) 10%, var(--app-surface) 90%)
  box-shadow: none

.mentor-avatar img
  width: 100%
  height: 100%
  object-fit: cover
  display: block

.group-summary__main
  min-width: 0

.group-summary__name
  font-size: 20px
  font-weight: 700
  line-height: 1.2

.group-summary__meta
  margin-top: 4px
  font-size: 14px
  color: var(--app-text-secondary)
  line-height: 1.4
  word-break: break-all

.student-slot-grid
  display: grid
  grid-template-columns: repeat(3, minmax(0, 1fr))
  gap: 10px
  min-width: 0

.student-slot
  min-height: 58px
  padding: 9px 10px
  display: flex
  flex-direction: column
  justify-content: center
  border-radius: 12px
  border: 1px solid color-mix(in srgb, var(--app-brand) 16%, var(--app-border) 84%)
  background: color-mix(in srgb, var(--app-brand) 10%, var(--app-surface) 90%)
  min-width: 0

.student-slot--empty
  background: transparent
  border-style: dashed
  opacity: .72

.student-slot__name
  font-size: 13px
  font-weight: 700
  line-height: 1.2
  white-space: nowrap
  overflow: hidden
  text-overflow: ellipsis

.student-slot__meta,
.student-slot__empty
  margin-top: 4px
  font-size: 12px
  color: var(--app-text-secondary)
  white-space: nowrap
  overflow: hidden
  text-overflow: ellipsis

.group-summary__aside
  display: flex
  flex-direction: row
  align-items: flex-end
  gap: 10px
  min-width: 190px

.group-summary__tags
  display: flex
  align-items: center
  gap: 8px
  flex-wrap: wrap
  justify-content: flex-end

.group-body
  padding: 16px 18px 18px 18px
  background: color-mix(in srgb, var(--app-surface) 94%, var(--app-brand) 6%)

.group-body__header
  display: flex
  align-items: center
  justify-content: space-between
  gap: 16px
  margin-bottom: 14px

.group-body__header h5
  margin: 0 0 4px 0
  font-size: 17px

.group-body__header p
  margin: 0
  color: var(--app-text-secondary)
  font-size: 13px

.group-body__actions
  display: flex
  align-items: center
  gap: 10px

.selection-hint
  color: var(--app-text-secondary)
  font-size: 14px

.students-table
  width: 100%
  min-width: 760px
  border: 1px solid color-mix(in srgb, var(--app-brand) 10%, var(--app-border) 90%)
  border-radius: 16px
  overflow: hidden

.students-table-shell
  width: 100%
  overflow-x: auto

.students-table :deep(.el-table__inner-wrapper::before)
  display: none

.students-table :deep(th.el-table__cell)
  background: color-mix(in srgb, var(--app-brand) 10%, var(--app-surface-elevated) 90%)
  color: var(--app-text-secondary)
  font-weight: 700

.students-table :deep(td.el-table__cell)
  background: transparent

.students-table :deep(.el-table__row:hover > td.el-table__cell)
  background: color-mix(in srgb, var(--app-brand) 10%, var(--app-surface) 90%)

.empty-page
  padding: 46px 20px
  text-align: center
  background: var(--app-surface)
  border: 1px dashed var(--app-border-strong)
  border-radius: 22px

.empty-page h4
  margin: 0 0 10px 0
  font-size: 22px

.empty-page p
  margin: 0
  color: var(--app-text-secondary)

.unbind-dialog__desc,
.unbind-dialog__notice
  margin: 0
  color: var(--app-text-secondary)
  line-height: 1.7

.unbind-dialog__chips
  display: flex
  flex-wrap: wrap
  gap: 10px
  margin: 18px 0

.student-chip
  display: inline-flex
  align-items: center
  justify-content: center
  padding: 8px 12px
  border-radius: 999px
  background: color-mix(in srgb, var(--app-brand) 12%, var(--app-surface) 88%)
  border: 1px solid var(--app-border)
  color: var(--app-text-primary)

.dialog-footer
  display: flex
  justify-content: flex-end
  gap: 10px

@media (max-width: 1100px)
  .stats-grid
    grid-template-columns: repeat(2, minmax(0, 1fr))

  .toolbar
    flex-direction: column
    align-items: stretch

  .toolbar__filters
    flex-wrap: wrap

  .toolbar__actions
    flex-wrap: wrap
    justify-content: flex-end

  .batch-item
    grid-template-columns: 1fr

  .batch-item__actions
    justify-content: flex-end

  .group-summary
    grid-template-columns: minmax(220px, 280px) 1fr

  .group-summary__aside
    grid-column: 1 / -1
    justify-content: space-between
    align-items: center
    width: 100%
    min-width: 0

@media (max-width: 768px)
  .all_pending_box
    width: calc(100% - 20px)

  .stats-grid
    grid-template-columns: repeat(2, minmax(0, 1fr))

  .stat-card
    min-height: 88px
    padding: 14px 16px

  .stat-card:nth-child(2n)
    border-right: none

  .stat-card:nth-child(-n+2)
    border-bottom: 1px solid var(--app-border)

  .overview
    padding: 0 0 14px 0

  .toolbar__filters
    flex-direction: column

  .toolbar-field,
  .toolbar__search,
  .toolbar__select
    max-width: none
    width: 100%

  .batch-panel__header
    flex-direction: column
    align-items: flex-start

  .batch-panel__subtitle
    display: block
    margin: 4px 0 0 0

  .group-summary
    grid-template-columns: 1fr
    align-items: flex-start

  .group-summary__lead
    width: 100%

  .student-slot-grid
    width: 100%
    grid-template-columns: repeat(2, minmax(0, 1fr))

  .group-summary__aside
    width: 100%
    min-width: 0
    align-items: flex-start

  .group-summary__tags
    justify-content: flex-start

  .group-summary__meter
    align-items: flex-start
    justify-content: flex-start
    width: 100%

  .group-summary__meter-track
    width: 140px

  .group-body__header
    flex-direction: column
    align-items: flex-start

  .group-body__actions
    width: 100%
    justify-content: space-between

  .students-table
    min-width: 700px

  .dialog-footer
    width: 100%
    flex-direction: column-reverse
</style>
