<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { useTeacherListStore } from "@/stores/TeacherListStore";
import { http, httpAdmin } from "@/utils/http";
import type { TeachDetailsFullImpl } from "@/interfaces/TeachDetailsFullImpl";

interface TeacherTableRow extends TeachDetailsFullImpl {
  avatarUrl: string | null;
  photourl: string | null;
  netid: string | null;
}

interface TeacherEditDraft {
  uid: number;
  fullName: string;
  username: string;
  email: string;
  professionalDirection: string;
  research_direction: string;
  resume: string;
  teacherposition: string;
}

const userStore = useUserInfoStore();
const teacherListStore = useTeacherListStore();

const tableData = ref<TeacherTableRow[]>([]);
const tableRef = ref<any>(null);
const isLoading = ref(false);
const editingUid = ref<number | null>(null);
const editingDraft = ref<TeacherEditDraft | null>(null);
const isSaving = ref(false);
const isDeleteMode = ref(false);
const selectedDeleteIds = ref<number[]>([]);
const isDeleting = ref(false);

const isAdmin = computed(() => userStore.userInfo?.role === "ADMIN");
const isEditing = computed(() => editingUid.value !== null);

const authHeaders = computed(() => ({
  Accept: "*/*",
  Authorization: `Bearer ${localStorage.getItem("token") ?? ""}`,
}));

function normalizeText(value: unknown) {
  return value == null ? "" : String(value);
}

function normalizeTeacherRow(row: Partial<TeacherTableRow> & { uid: number }): TeacherTableRow {
  return {
    uid: row.uid,
    fullName: normalizeText(row.fullName),
    email: normalizeText(row.email),
    resume: normalizeText(row.resume),
    photourl: row.photourl ?? null,
    avatarUrl: row.avatarUrl ?? null,
    netid: row.netid ?? null,
    teacherposition: normalizeText(row.teacherposition),
    research_direction: normalizeText(row.research_direction),
    professionalDirection: normalizeText(row.professionalDirection),
    AcceptedStudents: row.AcceptedStudents ?? 0,
    username: normalizeText(row.username),
  };
}

function buildEditDraft(row: TeacherTableRow): TeacherEditDraft {
  return {
    uid: row.uid,
    fullName: row.fullName,
    username: row.username,
    email: row.email ?? "",
    professionalDirection: row.professionalDirection ?? "",
    research_direction: row.research_direction ?? "",
    resume: row.resume ?? "",
    teacherposition: row.teacherposition ?? "",
  };
}

function applyTableData(rows: TeacherTableRow[] | null | undefined) {
  tableData.value = Array.isArray(rows) ? rows.map((row) => normalizeTeacherRow(row)) : [];

  if (isDeleteMode.value) {
    const visibleTeacherIds = new Set(tableData.value.map((row) => row.uid));
    selectedDeleteIds.value = selectedDeleteIds.value.filter((uid) => visibleTeacherIds.has(uid));
  }

  if (editingUid.value !== null && !tableData.value.some((row) => row.uid === editingUid.value)) {
    cancelEditing();
  }
}

function cancelEditing() {
  editingUid.value = null;
  editingDraft.value = null;
}

function clearTableSelection() {
  tableRef.value?.clearSelection?.();
}

function exitDeleteMode() {
  isDeleteMode.value = false;
  selectedDeleteIds.value = [];
  clearTableSelection();
}

function getErrorMessage(error: any, fallback: string) {
  const responseData = error?.response?.data;

  if (typeof responseData?.data === "string") return responseData.data;
  if (typeof responseData?.data?.error === "string") return responseData.data.error;
  if (typeof responseData?.message === "string") return responseData.message;
  if (typeof error?.message === "string") return error.message;
  return fallback;
}

async function loadTeachers() {
  isLoading.value = true;

  try {
    const res = await http({
      url: "/search/teachers",
      method: "GET",
      headers: authHeaders.value,
    });

    if (res.data.code === 200) {
      applyTableData(res.data.data);
      teacherListStore.updateTeacherList([...res.data.data]);
      return;
    }

    ElMessage.error("导师列表获取失败");
  } catch (error: any) {
    if (error?.response?.status !== 401) {
      ElMessage.error(getErrorMessage(error, "导师列表获取失败"));
    }
  } finally {
    isLoading.value = false;
  }
}

function isRowEditing(row: TeacherTableRow) {
  return isAdmin.value && editingUid.value === row.uid;
}

function getRowClassName({ row }: { row: TeacherTableRow }) {
  return row.uid === editingUid.value ? "editing-row" : "";
}

async function startEditing(row: TeacherTableRow) {
  if (!isAdmin.value || isSaving.value || isDeleteMode.value || isDeleting.value) return;

  if (editingUid.value !== null && editingUid.value !== row.uid) {
    const saved = await saveEditingRow(true);
    if (!saved) return;
  }

  editingUid.value = row.uid;
  editingDraft.value = buildEditDraft(row);
}

async function handleRowDoubleClick(row: TeacherTableRow) {
  await startEditing(row);
}

async function saveEditingRow(silentSuccess = false) {
  if (editingUid.value === null || editingDraft.value === null || isSaving.value) {
    return true;
  }

  const draft = {
    uid: editingDraft.value.uid,
    fullName: editingDraft.value.fullName.trim(),
    username: editingDraft.value.username.trim(),
    email: editingDraft.value.email.trim(),
    professionalDirection: editingDraft.value.professionalDirection.trim(),
    research_direction: editingDraft.value.research_direction.trim(),
    resume: editingDraft.value.resume.trim(),
    teacherposition: editingDraft.value.teacherposition.trim(),
  };

  if (!draft.fullName || !draft.username || !draft.email) {
    ElMessage.warning("姓名、工号、邮箱不能为空");
    return false;
  }

  isSaving.value = true;

  try {
    const basicRes = await http({
      url: "/user/update",
      method: "POST",
      headers: {
        ...authHeaders.value,
        "Content-Type": "application/json",
      },
      data: {
        uid: draft.uid,
        role: "TEACHER",
        fullName: draft.fullName,
        email: draft.email,
        username: draft.username,
      },
    });

    if (basicRes.data.code !== 200) {
      throw new Error(basicRes.data.message || "基础信息保存失败");
    }

    const detailRes = await httpAdmin({
      url: `/update/${draft.uid}`,
      method: "PUT",
      headers: authHeaders.value,
      data: {
        teacherPosition: draft.teacherposition,
        researchDirection: draft.research_direction,
        professionalDirection: draft.professionalDirection,
        resume: draft.resume,
        studentGrade: "",
      },
    });

    if (detailRes.data.code !== 200) {
      throw new Error(detailRes.data.message || "导师详情保存失败");
    }

    const rowIndex = tableData.value.findIndex((row) => row.uid === draft.uid);
    if (rowIndex !== -1) {
      tableData.value[rowIndex] = normalizeTeacherRow({
        ...tableData.value[rowIndex],
        ...draft,
      });
      tableData.value = [...tableData.value];
    }

    if (Array.isArray(teacherListStore.teacherListSt)) {
      teacherListStore.updateTeacherList([...tableData.value]);
    }

    cancelEditing();

    if (!silentSuccess) {
      ElMessage.success("导师信息已保存");
    }

    return true;
  } catch (error: any) {
    ElMessage.error(getErrorMessage(error, "保存失败，请稍后重试"));
    return false;
  } finally {
    isSaving.value = false;
  }
}

function triggerSaveFromBlankArea() {
  if (editingUid.value === null || isDeleteMode.value) return;

  void saveEditingRow();
}

function handleTableAreaDoubleClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  if (!target || target.closest(".el-table__row")) return;

  triggerSaveFromBlankArea();
}

function openDeleteMode() {
  if (!isAdmin.value || isSaving.value || isDeleting.value) return;

  cancelEditing();
  isDeleteMode.value = true;
  selectedDeleteIds.value = [];
  clearTableSelection();
}

function handleDeleteSelectionChange(rows: TeacherTableRow[]) {
  selectedDeleteIds.value = rows.map((row) => row.uid);
}

async function confirmDeleteSelectedTeachers() {
  if (selectedDeleteIds.value.length === 0) {
    ElMessage.warning("请先勾选要删除的导师");
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认删除已勾选的 ${selectedDeleteIds.value.length} 名导师吗？删除后，导师会转为历史导师，当前学生会立即解除关系，历史关系和申请记录会保留。`,
      "确认删除导师",
      {
        confirmButtonText: "确认删除",
        cancelButtonText: "取消",
        type: "warning",
        closeOnClickModal: false,
      },
    );
  } catch {
    return;
  }

  isDeleting.value = true;

  try {
    const res = await http({
      url: "/user/teachers/archive",
      method: "POST",
      headers: {
        ...authHeaders.value,
        "Content-Type": "application/json",
      },
      data: {
        teacherIds: selectedDeleteIds.value,
      },
    });

    if (res.data.code !== 200) {
      throw new Error(res.data?.message || "导师删除失败");
    }

    const archivedTeacherIds = Array.isArray(res.data?.data?.archivedTeacherIds)
      ? new Set<number>(res.data.data.archivedTeacherIds)
      : new Set<number>(selectedDeleteIds.value);

    tableData.value = tableData.value.filter((row) => !archivedTeacherIds.has(row.uid));
    teacherListStore.updateTeacherList([...tableData.value]);
    exitDeleteMode();

    ElMessage.success(
      `已删除 ${res.data?.data?.archivedTeacherCount ?? archivedTeacherIds.size} 名导师，已解除 ${res.data?.data?.affectedStudentCount ?? 0} 名学生关系`,
    );
  } catch (error: any) {
    ElMessage.error(getErrorMessage(error, "导师删除失败，请稍后重试"));
  } finally {
    isDeleting.value = false;
  }
}

watch(
  () => teacherListStore.teacherListSt,
  (newTeacherListVal) => {
    if (Array.isArray(newTeacherListVal)) {
      applyTableData(newTeacherListVal);
    }
  }
);

watch(
  () => isAdmin.value,
  (newVal) => {
    if (!newVal) {
      cancelEditing();
      exitDeleteMode();
    }
  }
);

onMounted(() => {
  void loadTeachers();
});
</script>

<template>
  <div class="title">
    <span>列表内容</span>
    <div v-if="isAdmin" class="title_actions">
      <span class="title_hint">
        <template v-if="isEditing">正在编辑当前导师，可直接保存或取消</template>
        <template v-else-if="isDeleteMode">已进入批量删除模式，请勾选导师后确认删除</template>
        <template v-else>管理员双击行可编辑</template>
      </span>

      <template v-if="isEditing">
        <button class="header_button" :disabled="isSaving" @click="saveEditingRow()">保存</button>
        <button class="header_button header_button--ghost" :disabled="isSaving" @click="cancelEditing">取消</button>
      </template>

      <template v-else-if="isDeleteMode">
        <span class="title_hint">已选 {{ selectedDeleteIds.length }} 名导师</span>
        <button class="header_button header_button--danger" :disabled="selectedDeleteIds.length === 0 || isDeleting" @click="confirmDeleteSelectedTeachers">
          {{ isDeleting ? "删除中..." : "确认删除" }}
        </button>
        <button class="header_button header_button--ghost" :disabled="isDeleting" @click="exitDeleteMode">取消删除</button>
      </template>

      <button v-else class="header_button header_button--danger" @click="openDeleteMode">删除导师</button>
    </div>
  </div>

  <div class="table_shell" @dblclick.capture="handleTableAreaDoubleClick">
    <el-table
      ref="tableRef"
      :data="tableData"
      :row-class-name="getRowClassName"
      row-key="uid"
      stripe
      v-loading="isLoading"
      class="table"
      @row-dblclick="handleRowDoubleClick"
      @selection-change="handleDeleteSelectionChange"
    >
      <el-table-column v-if="isDeleteMode" type="selection" width="54" reserve-selection />

      <el-table-column label="姓名" min-width="120">
        <template #default="{ row }">
          <el-input v-if="isRowEditing(row) && editingDraft" v-model="editingDraft.fullName" size="small" />
          <span v-else>{{ row.fullName }}</span>
        </template>
      </el-table-column>

      <el-table-column label="工号" min-width="120">
        <template #default="{ row }">
          <el-input v-if="isRowEditing(row) && editingDraft" v-model="editingDraft.username" size="small" />
          <span v-else>{{ row.username }}</span>
        </template>
      </el-table-column>

      <el-table-column label="邮箱" min-width="220">
        <template #default="{ row }">
          <el-input v-if="isRowEditing(row) && editingDraft" v-model="editingDraft.email" size="small" />
          <span v-else class="cell_multiline">{{ row.email }}</span>
        </template>
      </el-table-column>

      <el-table-column label="研究方向" min-width="260">
        <template #default="{ row }">
          <el-input
            v-if="isRowEditing(row) && editingDraft"
            v-model="editingDraft.research_direction"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            resize="none"
          />
          <span v-else class="cell_multiline">{{ row.research_direction }}</span>
        </template>
      </el-table-column>

      <el-table-column label="介绍" min-width="220">
        <template #default="{ row }">
          <el-input
            v-if="isRowEditing(row) && editingDraft"
            v-model="editingDraft.resume"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            resize="none"
          />
          <span v-else class="cell_multiline">{{ row.resume }}</span>
        </template>
      </el-table-column>

      <el-table-column label="职称" min-width="120">
        <template #default="{ row }">
          <el-input v-if="isRowEditing(row) && editingDraft" v-model="editingDraft.teacherposition" size="small" />
          <span v-else>{{ row.teacherposition }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="spacer" :class="{ active: editingUid !== null }" @dblclick.stop="triggerSaveFromBlankArea"></div>
  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  min-height: 40px
  background-color: var(--app-surface-muted)
  display: flex
  align-items: center
  justify-content: space-between
  gap: 12px
  padding: 0 20px
  font-size: 16px
  margin-bottom: 20px

.title_hint
  font-size: 13px
  color: var(--app-text-secondary)

.title_actions
  display: flex
  align-items: center
  justify-content: flex-end
  gap: 10px
  flex-wrap: wrap

.header_button
  border: none
  border-radius: 6px
  height: 32px
  padding: 0 16px
  background-color: #005826
  color: #ffffff
  font-size: 14px
  cursor: pointer
  transition: .2s ease

.header_button:hover:not(:disabled)
  background-color: #0f7e3f

.header_button:disabled
  cursor: not-allowed
  opacity: .6

.header_button--ghost
  background-color: var(--app-surface)
  color: var(--app-text-primary)
  border: 1px solid var(--app-border)

.header_button--ghost:hover:not(:disabled)
  background-color: var(--app-surface-muted)

.header_button--danger
  background-color: #c64545

.header_button--danger:hover:not(:disabled)
  background-color: #a93030

.table_shell
  width: 100%

.table
  margin: 0 auto
  width: 98%
  max-height: calc(100vh - 60px - 60px - 150px)
  overflow-y: auto
  border: 1px solid var(--app-brand)
  border-radius: 5px
  transition: .3s ease

.table:hover
  box-shadow: var(--app-brand) 0 0 10px

.cell_multiline
  display: block
  white-space: pre-wrap
  word-break: break-word
  line-height: 1.6

.spacer
  width: 98%
  height: 56px
  margin: 0 auto
  border-radius: 0 0 5px 5px
  transition: .2s ease

.spacer.active
  background-color: var(--app-brand-ring)

:deep(.editing-row > td)
  background-color: var(--app-brand-soft) !important

:deep(.editing-row .el-input__wrapper),
:deep(.editing-row .el-textarea__inner)
  box-shadow: 0 0 0 1px var(--app-brand-ring) inset

@media (max-width: 1200px)
  .title
    flex-direction: column
    align-items: flex-start
    padding: 10px 20px
</style>
