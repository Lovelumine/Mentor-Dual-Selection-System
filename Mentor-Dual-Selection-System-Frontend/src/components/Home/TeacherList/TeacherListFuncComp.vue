<script setup lang="ts">
import { computed, ref } from "vue";
import * as XLSX from "xlsx";
import { ElMessage, ElMessageBox } from "element-plus";
import { http, httpAdmin } from "@/utils/http";
import { useTeacherListStore } from "@/stores/TeacherListStore";
import type { TeachDetailsFullImpl } from "@/interfaces/TeachDetailsFullImpl";
import { downloadBlob, downloadWorkbook, parseContentDispositionFilename } from "@/utils/fileDownload";
import {
  analyzeImportRows,
  type ExistingImportUser,
} from "@/utils/importUserPreview";
import {
  createImportColumnOptions,
  getImportCellValue,
  inferImportColumnMapping,
  type ImportColumnRule,
} from "@/utils/importColumnMapping";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";

type TeacherUploadField =
  | "role"
  | "fullName"
  | "email"
  | "username"
  | "teacherPosition"
  | "researchDirection";

interface TeacherUploadPreviewRow {
  role: string;
  fullName: string;
  email: string;
  username: string;
  teacherPosition: string;
  researchDirection: string;
}

interface TeacherImportPlanRow extends TeacherUploadPreviewRow {
  rowIndex: number;
  action: "create" | "overwrite" | "conflict";
  actionLabel: string;
  message: string;
  existingUid: number | null;
}

interface SingleTeacherForm {
  role: string;
  fullName: string;
  email: string;
  username: string;
  teacherPosition: string;
  researchDirection: string;
  resume: string;
}

const teacherImportRules: ImportColumnRule<TeacherUploadField>[] = [
  { key: "role", label: "权限", aliases: ["权限", "角色", "role"], fallbackIndex: 0 },
  { key: "fullName", label: "姓名", aliases: ["姓名", "导师姓名", "名称", "fullname", "name"], fallbackIndex: 1, required: true },
  { key: "email", label: "电子邮箱", aliases: ["电子邮箱", "邮箱", "email"], fallbackIndex: 2 },
  { key: "username", label: "工号", aliases: ["工号", "教师工号", "账号", "username"], fallbackIndex: 3, required: true },
  { key: "teacherPosition", label: "职称", aliases: ["职称", "职位", "教师职称", "teacherposition", "position"], fallbackIndex: 4, required: true },
  { key: "researchDirection", label: "研究方向", aliases: ["研究方向", "方向", "研究领域", "researchdirection"], fallbackIndex: 5 },
];

const teacherListStore = useTeacherListStore();
const { isMobile, isTablet } = useResponsiveLayout();

const fileInput = ref<HTMLInputElement | null>(null);
const excelName = ref("");
const headerRow = ref<unknown[]>([]);
const rawRows = ref<unknown[][]>([]);
const existingUsers = ref<ExistingImportUser[]>([]);
const currentTeachers = ref<TeachDetailsFullImpl[]>([]);
const columnMapping = ref<Record<TeacherUploadField, number | null>>({
  role: null,
  fullName: null,
  email: null,
  username: null,
  teacherPosition: null,
  researchDirection: null,
});
const isUploadBoxShow = ref(false);
const isUploading = ref(false);
const isDownloadingTemplate = ref(false);
const isExporting = ref(false);
const isAddTeacherDialogVisible = ref(false);
const isSubmittingSingleTeacher = ref(false);
const progressPercentage = ref(0);
const progressStatus = ref("");
const total = ref(0);
const falCount = ref(0);
const singleTeacherForm = ref<SingleTeacherForm>({
  role: "TEACHER",
  fullName: "",
  email: "",
  username: "",
  teacherPosition: "",
  researchDirection: "",
  resume: "",
});

const previewLimit = 8;
const addTeacherDialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "86vw" : "560px"));
const columnOptions = computed(() => createImportColumnOptions(headerRow.value));
const missingRequiredLabels = computed(() =>
  teacherImportRules
    .filter((rule) => rule.required && columnMapping.value[rule.key] == null)
    .map((rule) => rule.label),
);

const mappedRows = computed<TeacherUploadPreviewRow[]>(() =>
  rawRows.value
    .map((row) => ({
      role: getImportCellValue(row, columnMapping.value.role) || "TEACHER",
      fullName: getImportCellValue(row, columnMapping.value.fullName),
      email: getImportCellValue(row, columnMapping.value.email),
      username: getImportCellValue(row, columnMapping.value.username),
      teacherPosition: getImportCellValue(row, columnMapping.value.teacherPosition),
      researchDirection: getImportCellValue(row, columnMapping.value.researchDirection),
    }))
    .filter((row) => [row.fullName, row.email, row.username, row.teacherPosition, row.researchDirection].some(Boolean)),
);

const importPlans = computed<TeacherImportPlanRow[]>(() =>
  analyzeImportRows({
    rows: mappedRows.value,
    existingUsers: existingUsers.value,
    expectedRole: "TEACHER",
    uniqueFieldLabel: "工号",
    uniqueValue: (row) => row.username,
    requiredFields: [
      { label: "姓名", value: (row) => row.fullName },
      { label: "工号", value: (row) => row.username },
      { label: "职称", value: (row) => row.teacherPosition },
    ],
  }).map((plan) => ({
    ...plan.row,
    rowIndex: plan.rowIndex,
    action: plan.action,
    actionLabel: getActionLabel(plan.action),
    message: plan.message,
    existingUid: plan.existingUser?.uid ?? null,
  })),
);

const previewRows = computed(() => importPlans.value.slice(0, previewLimit));
const hasMorePreview = computed(() => importPlans.value.length > previewLimit);
const uploadSummary = computed(() =>
  importPlans.value.reduce(
    (summary, row) => {
      summary[row.action] += 1;
      return summary;
    },
    { create: 0, overwrite: 0, conflict: 0 },
  ),
);
const executableRows = computed(() => importPlans.value.filter((row) => row.action !== "conflict"));
const canUpload = computed(() =>
  !isUploading.value
  && importPlans.value.length > 0
  && missingRequiredLabels.value.length === 0
  && uploadSummary.value.conflict === 0,
);

function authHeaders() {
  return {
    Accept: "*/*",
    Authorization: `Bearer ${localStorage.getItem("token")}`,
  };
}

function getActionLabel(action: TeacherImportPlanRow["action"]) {
  if (action === "overwrite") return "覆盖更新";
  if (action === "conflict") return "存在冲突";
  return "新增账号";
}

function getActionTagType(action: TeacherImportPlanRow["action"]) {
  if (action === "overwrite") return "warning";
  if (action === "conflict") return "danger";
  return "success";
}

function getTimestamp() {
  const now = new Date();
  const year = now.getFullYear();
  const month = `${now.getMonth() + 1}`.padStart(2, "0");
  const day = `${now.getDate()}`.padStart(2, "0");
  const hour = `${now.getHours()}`.padStart(2, "0");
  const minute = `${now.getMinutes()}`.padStart(2, "0");
  return `${year}${month}${day}-${hour}${minute}`;
}

async function fetchExistingUsers() {
  const res = await http({
    url: "/user/all",
    method: "POST",
    headers: authHeaders(),
  });

  if (res.data.code !== 200 || !Array.isArray(res.data.data)) {
    throw new Error("现有账号信息获取失败");
  }

  existingUsers.value = res.data.data;
}

async function fetchTeachersForExport(forceRefresh = false) {
  const cachedRows = Array.isArray(teacherListStore.teacherListSt) ? teacherListStore.teacherListSt : null;
  if (!forceRefresh && cachedRows) {
    currentTeachers.value = cachedRows;
    return cachedRows;
  }

  const res = await http({
    url: "/search/teachers",
    method: "GET",
    headers: authHeaders(),
  });

  if (res.data.code !== 200 || !Array.isArray(res.data.data)) {
    throw new Error("导师列表获取失败");
  }

  teacherListStore.updateTeacherList(res.data.data);
  currentTeachers.value = res.data.data;
  return res.data.data;
}

function resetUploadProgress() {
  isUploading.value = false;
  progressPercentage.value = 0;
  progressStatus.value = "";
  total.value = 0;
  falCount.value = 0;
}

function resetUploadPreview() {
  excelName.value = "";
  headerRow.value = [];
  rawRows.value = [];
  existingUsers.value = [];
  isUploadBoxShow.value = false;
  resetUploadProgress();
  if (fileInput.value) {
    fileInput.value.value = "";
  }
}

function addSingleTeacherClicked() {
  resetSingleTeacherForm();
  isAddTeacherDialogVisible.value = true;
}

function resetSingleTeacherForm() {
  singleTeacherForm.value = {
    role: "TEACHER",
    fullName: "",
    email: "",
    username: "",
    teacherPosition: "",
    researchDirection: "",
    resume: "",
  };
}

async function submitSingleTeacher() {
  if (isSubmittingSingleTeacher.value) return;

  const draft = {
    role: "TEACHER",
    fullName: singleTeacherForm.value.fullName.trim(),
    email: singleTeacherForm.value.email.trim(),
    username: singleTeacherForm.value.username.trim(),
    teacherPosition: singleTeacherForm.value.teacherPosition.trim(),
    researchDirection: singleTeacherForm.value.researchDirection.trim(),
    resume: singleTeacherForm.value.resume.trim(),
  };

  if (!draft.fullName) {
    ElMessage.warning("请填写姓名");
    return;
  }

  if (!draft.username) {
    ElMessage.warning("请填写工号");
    return;
  }

  if (!draft.teacherPosition) {
    ElMessage.warning("请填写职称");
    return;
  }

  isSubmittingSingleTeacher.value = true;

  try {
    const basicRes = await http({
      url: "/user/update",
      method: "POST",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      data: {
        role: draft.role,
        fullName: draft.fullName,
        email: draft.email || null,
        username: draft.username,
      },
    });

    if (basicRes.data.code !== 200) {
      throw new Error(basicRes.data?.data?.error || basicRes.data?.message || "导师基础信息添加失败");
    }

    const uid = Number(basicRes.data?.data?.uid);
    if (!Number.isFinite(uid) || uid <= 0) {
      throw new Error("导师基础信息返回的 UID 无效");
    }

    const detailRes = await httpAdmin({
      url: `/update/${uid}`,
      method: "PUT",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      data: {
        teacherPosition: draft.teacherPosition,
        researchDirection: draft.researchDirection,
        professionalDirection: "",
        resume: draft.resume,
        studentGrade: "",
      },
    });

    if (detailRes.data.code !== 200) {
      throw new Error(detailRes.data?.data?.error || detailRes.data?.message || "导师详情添加失败");
    }

    const latestTeachers = await fetchTeachersForExport(true);
    teacherListStore.updateTeacherList(latestTeachers);
    isAddTeacherDialogVisible.value = false;
    resetSingleTeacherForm();
    ElMessage.success("导师信息已添加");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.data?.error || error?.message || "导师信息添加失败");
  } finally {
    isSubmittingSingleTeacher.value = false;
  }
}

async function downloadTeacherTemplate() {
  if (isDownloadingTemplate.value) return;

  isDownloadingTemplate.value = true;
  try {
    const response = await http({
      url: "/import/template/teacher",
      method: "GET",
      responseType: "blob",
      headers: authHeaders(),
    });

    const filename = parseContentDispositionFilename(response.headers["content-disposition"]) || "导师信息导入模板.xlsx";
    downloadBlob(response.data, filename);
    ElMessage.success("导师导入模板已开始下载");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.data?.error || "导师模板下载失败");
  } finally {
    isDownloadingTemplate.value = false;
  }
}

async function exportCurrentTeachers() {
  if (isExporting.value) return;

  isExporting.value = true;
  try {
    const teachers = await fetchTeachersForExport();
    const rows = teachers.map((teacher: any) => ({
      姓名: teacher.fullName ?? "",
      工号: teacher.username ?? "",
      电子邮箱: teacher.email ?? "",
      职称: teacher.teacherposition ?? "",
      研究方向: teacher.research_direction ?? "",
      介绍: teacher.resume ?? "",
    }));

    const workbook = XLSX.utils.book_new();
    const worksheet = XLSX.utils.json_to_sheet(rows);
    XLSX.utils.book_append_sheet(workbook, worksheet, "导师信息");
    downloadWorkbook(workbook, `导师信息导出-${getTimestamp()}.xlsx`);
    ElMessage.success("导师信息已导出");
  } catch (error: any) {
    ElMessage.error(error?.message || "导师信息导出失败");
  } finally {
    isExporting.value = false;
  }
}

function inferTeacherMapping() {
  columnMapping.value = inferImportColumnMapping(columnOptions.value, teacherImportRules);
}

function buildHeaderRow(rows: unknown[][]) {
  const widestColumnCount = rows.reduce((max, row) => Math.max(max, row.length), 0);
  const sourceHeader = rows[0] ?? [];
  return Array.from({ length: widestColumnCount }, (_, index) => sourceHeader[index] ?? "");
}

async function checkUpload() {
  if (missingRequiredLabels.value.length > 0) {
    ElMessage.warning(`请先完成必填列映射：${missingRequiredLabels.value.join("、")}`);
    return;
  }

  if (importPlans.value.length === 0) {
    ElMessage.warning("没有可上传的数据");
    return;
  }

  if (uploadSummary.value.conflict > 0) {
    ElMessage.warning(`当前有 ${uploadSummary.value.conflict} 条冲突数据，请先修正后再上传`);
    return;
  }

  if (uploadSummary.value.overwrite > 0) {
    try {
      await ElMessageBox.confirm(
        `检测到 ${uploadSummary.value.overwrite} 条工号已存在，确认上传后会覆盖更新原导师信息，不会新建重复账号。是否继续？`,
        "确认覆盖更新",
        {
          confirmButtonText: "继续上传",
          cancelButtonText: "取消",
          type: "warning",
          closeOnClickModal: false,
        },
      );
    } catch {
      return;
    }
  }

  try {
    await fetchTeachersForExport();
  } catch (error: any) {
    ElMessage.error(error?.message || "当前导师信息获取失败，无法执行覆盖更新");
    return;
  }

  isUploading.value = true;
  falCount.value = 0;
  total.value = executableRows.value.length;

  for (let i = 0; i < executableRows.value.length; i += 1) {
    const row = executableRows.value[i];

    try {
      const currentName = row.fullName || row.username || `第 ${i + 1} 条`;
      progressStatus.value = `正在处理第 ${i + 1} 条数据：${currentName}`;

      const uid = await uploadBasicInfo(row, i + 1);
      await uploadAdditionalInfo(uid, row, i + 1);
    } catch (error) {
      console.error(`第 ${i + 1} 条数据上传失败`, error);
      falCount.value += 1;
    } finally {
      progressPercentage.value = Math.round(((i + 1) / Math.max(total.value, 1)) * 100);
    }
  }

  isUploading.value = false;
  progressStatus.value = `上传完成，成功 ${total.value - falCount.value} 条，失败 ${falCount.value} 条`;

  try {
    const latestTeachers = await fetchTeachersForExport();
    teacherListStore.updateTeacherList(latestTeachers);
  } catch (error) {
    console.error("导师列表刷新失败", error);
  }

  if (falCount.value === 0) {
    ElMessage.success("导师信息已全部上传完成");
    resetUploadPreview();
  } else {
    ElMessage.warning(`导师信息上传完成，但有 ${falCount.value} 条失败`);
  }
}

async function uploadBasicInfo(rowData: TeacherImportPlanRow, index: number) {
  const existingUser = rowData.existingUid
    ? existingUsers.value.find((user) => user.uid === rowData.existingUid)
    : null;
  const mergedEmail = rowData.email || existingUser?.email || null;

  const res = await http({
    url: "/user/update",
    method: "POST",
    headers: {
      ...authHeaders(),
      "Content-Type": "application/json",
    },
    data: {
      uid: rowData.existingUid ?? undefined,
      role: rowData.role || "TEACHER",
      fullName: rowData.fullName,
      email: mergedEmail,
      username: rowData.username,
    },
  });

  if (res.data.code !== 200) {
    throw new Error(`第 ${index} 条基础信息上传失败`);
  }

  const uid = Number(res.data?.data?.uid);
  if (!Number.isFinite(uid) || uid <= 0) {
    throw new Error(`第 ${index} 条基础信息返回的 UID 无效`);
  }

  return uid;
}

async function uploadAdditionalInfo(uid: number, rowData: TeacherUploadPreviewRow, index: number) {
  const currentTeacher = currentTeachers.value.find(
    (teacher) => teacher.uid === uid || teacher.username === rowData.username,
  );

  const res = await httpAdmin({
    url: `/update/${uid}`,
    method: "PUT",
    headers: authHeaders(),
    data: {
      photoUrl: currentTeacher?.photourl ?? null,
      teacherPosition: rowData.teacherPosition || currentTeacher?.teacherposition || "",
      researchDirection: rowData.researchDirection || currentTeacher?.research_direction || "",
      professionalDirection: currentTeacher?.professionalDirection || "",
      resume: currentTeacher?.resume || "",
      netid: currentTeacher?.netid || "",
      studentClass: null,
      studentGrade: "",
    },
  });

  if (res.data.code !== 200) {
    throw new Error(`第 ${index} 条详细信息上传失败`);
  }
}

const triggerFileInput = () => {
  fileInput.value?.click();
};

function handleFileChange(event: Event) {
  const inputElement = event.target as HTMLInputElement | null;
  const file = inputElement?.files?.[0];

  if (!file) return;

  const fileExtension = file.name.toLowerCase().split(".").pop();
  if (!fileExtension || !["xlsx", "xls"].includes(fileExtension)) {
    ElMessage.warning("您选择的不是 Excel 表格文档");
    resetUploadPreview();
    return;
  }

  excelName.value = file.name;
  resetUploadProgress();

  const reader = new FileReader();
  reader.onload = async (loadEvent) => {
    const arrayBuffer = loadEvent.target?.result as ArrayBuffer | null;
    if (!arrayBuffer) return;

    const workbook = XLSX.read(arrayBuffer, { type: "array" });
    const firstSheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[firstSheetName];
    const parsedRows = XLSX.utils.sheet_to_json<unknown[]>(worksheet, { header: 1, raw: false })
      .map((row) => (Array.isArray(row) ? row : []));

    if (parsedRows.length <= 1) {
      ElMessage.warning("Excel 中没有可预览的数据");
      resetUploadPreview();
      return;
    }

    headerRow.value = buildHeaderRow(parsedRows);
    rawRows.value = parsedRows.slice(1);
    inferTeacherMapping();

    try {
      await fetchExistingUsers();
    } catch (error: any) {
      ElMessage.error(error?.message || "现有账号信息获取失败");
      resetUploadPreview();
      return;
    }

    isUploadBoxShow.value = true;

    ElMessage.success(`已读取 ${mappedRows.value.length} 条导师数据，请核对预览后再确认上传`);
  };

  reader.readAsArrayBuffer(file);
}
</script>

<template>
  <div class="button_box">
    <button @click="addSingleTeacherClicked">添加单条导师信息</button>
    <button :disabled="isDownloadingTemplate" @click="downloadTeacherTemplate">
      {{ isDownloadingTemplate ? "下载中..." : "下载导师表格模板" }}
    </button>
    <button :disabled="isExporting" @click="exportCurrentTeachers">
      {{ isExporting ? "导出中..." : "导出当前导师信息" }}
    </button>
    <input ref="fileInput" type="file" style="display: none" accept=".xlsx, .xls" @change="handleFileChange" />
    <button @click="triggerFileInput">批量上传导师信息</button>
  </div>

  <el-dialog
    v-model="isAddTeacherDialogVisible"
    title="添加单条导师信息"
    :width="addTeacherDialogWidth"
    destroy-on-close
    :close-on-click-modal="!isSubmittingSingleTeacher"
  >
    <el-form label-width="92px" @submit.prevent="submitSingleTeacher">
      <el-form-item label="姓名">
        <el-input v-model="singleTeacherForm.fullName" placeholder="请输入导师姓名" />
      </el-form-item>
      <el-form-item label="工号">
        <el-input v-model="singleTeacherForm.username" placeholder="请输入导师工号" />
      </el-form-item>
      <el-form-item label="电子邮箱">
        <el-input v-model="singleTeacherForm.email" placeholder="可选，推荐填写学校邮箱" />
      </el-form-item>
      <el-form-item label="职称">
        <el-input v-model="singleTeacherForm.teacherPosition" placeholder="例如：教授 / 副教授" />
      </el-form-item>
      <el-form-item label="研究方向">
        <el-input
          v-model="singleTeacherForm.researchDirection"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 6 }"
          placeholder="可选，填写主要研究方向"
        />
      </el-form-item>
      <el-form-item label="简历">
        <el-input
          v-model="singleTeacherForm.resume"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 6 }"
          placeholder="可选，填写导师简介"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="single_dialog_actions">
        <button class="dialog_button dialog_button--ghost" :disabled="isSubmittingSingleTeacher" @click="isAddTeacherDialogVisible = false">取消</button>
        <button class="dialog_button" :disabled="isSubmittingSingleTeacher" @click="submitSingleTeacher">
          {{ isSubmittingSingleTeacher ? "提交中..." : "确认添加" }}
        </button>
      </div>
    </template>
  </el-dialog>

  <div v-if="isUploadBoxShow" class="excel_status">
    <div class="excel_status__summary">
      <p>已读取文件：{{ excelName }}，当前映射后共 {{ importPlans.length }} 条导师数据</p>
      <span>系统会自动匹配列，但你可以在下面手动修正；点击“确认上传”后才会正式写入系统</span>
      <span>新增 {{ uploadSummary.create }} 条，覆盖 {{ uploadSummary.overwrite }} 条，冲突 {{ uploadSummary.conflict }} 条</span>
      <span v-if="missingRequiredLabels.length" class="excel_status__warning">待补充映射：{{ missingRequiredLabels.join("、") }}</span>
      <span v-else-if="uploadSummary.conflict > 0" class="excel_status__warning">存在冲突数据，需先修正原表格再上传</span>
    </div>
    <div class="excel_status__actions">
      <button :disabled="!canUpload" @click="checkUpload">确认上传</button>
      <button :disabled="isUploading" @click="triggerFileInput">重新上传</button>
      <button v-if="!isUploading" class="button_danger" @click="resetUploadPreview">取消</button>
    </div>
  </div>

  <div v-if="isUploadBoxShow" class="mapping_panel">
    <div class="mapping_panel__header">
      <span>列映射</span>
      <span>如果系统自动识别不准确，可以在这里纠正</span>
    </div>

    <div class="mapping_grid">
      <div v-for="rule in teacherImportRules" :key="rule.key" class="mapping_item">
        <label :for="`teacher-mapping-${rule.key}`">
          {{ rule.label }}
          <span v-if="rule.required">*</span>
        </label>
        <el-select :id="`teacher-mapping-${rule.key}`" v-model="columnMapping[rule.key]" clearable placeholder="请选择列">
          <el-option
            v-for="option in columnOptions"
            :key="option.index"
            :label="`${option.label}（第 ${option.index + 1} 列）`"
            :value="option.index"
          />
        </el-select>
      </div>
    </div>
  </div>

  <div v-if="isUploadBoxShow" class="preview_panel">
    <div class="preview_panel__header">
      <span>上传预览</span>
      <span v-if="hasMorePreview">仅显示前 {{ previewRows.length }} 条，剩余 {{ importPlans.length - previewRows.length }} 条将在上传时一并处理</span>
    </div>

    <div class="preview_table_shell">
      <el-table :data="previewRows" border size="small" class="preview_table">
        <el-table-column prop="rowIndex" label="序号" width="70" />
        <el-table-column label="导入动作" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.action)" effect="light">{{ row.actionLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fullName" label="姓名" min-width="120" />
        <el-table-column prop="email" label="电子邮箱" min-width="220" />
        <el-table-column prop="username" label="工号" min-width="120" />
        <el-table-column prop="teacherPosition" label="职称" min-width="120" />
        <el-table-column prop="researchDirection" label="研究方向" min-width="260" show-overflow-tooltip />
        <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
      </el-table>
    </div>
  </div>

  <div v-if="progressPercentage > 0" class="progress_container">
    <el-progress
      :percentage="progressPercentage"
      :text-inside="true"
      :stroke-width="30"
      color="#7ED321"
    />
    <p>{{ progressStatus }}</p>
  </div>
</template>

<style scoped lang="sass">
.button_box
  width: 100%
  min-height: 80px
  display: flex
  align-items: center
  flex-wrap: wrap
  gap: 18px
  button
    padding: 0 20px
    height: 40px
    min-width: 160px
    font-size: 14px
    border-radius: 8px
    border: none
    color: white
    margin-left: 32px
    background-color: var(--app-brand)
    transition: .3s ease
  button:hover
    background-color: var(--app-brand-hover)
  button:disabled
    cursor: not-allowed
    opacity: 0.7

.excel_status
  margin-top: 8px
  padding: 18px 24px
  width: 100%
  display: flex
  align-items: center
  justify-content: space-between
  gap: 20px
  background: linear-gradient(135deg, var(--app-brand) 0%, var(--app-brand-hover) 100%)
  border-radius: 14px
  color: white

.excel_status__summary
  display: flex
  flex-direction: column
  gap: 4px
  p
    font-size: 15px
    font-weight: 600
  span
    font-size: 13px
    color: rgba(255, 255, 255, 0.86)

.excel_status__warning
  color: #fff3d1 !important

.excel_status__actions
  display: flex
  align-items: center
  gap: 12px
  button
    height: 38px
    min-width: 112px
    padding: 0 18px
    border-radius: 10px
    border: 1px solid rgba(255, 255, 255, 0.35)
    background-color: rgba(255, 255, 255, 0.14)
    color: white
    transition: .2s ease
  button:hover
    background-color: rgba(255, 255, 255, 0.22)
  button:disabled
    cursor: not-allowed
    opacity: 0.6
  .button_danger
    border-color: rgba(255, 214, 214, 0.68)
    color: #ffe4e4
  .button_danger:hover
    background-color: rgba(189, 0, 0, 0.28)

.mapping_panel,
.preview_panel
  margin-top: 18px
  padding: 18px
  border: 1px solid var(--app-border)
  border-radius: 14px
  background: var(--app-surface)
  box-shadow: 0 16px 32px var(--app-shadow)

.mapping_panel__header,
.preview_panel__header
  display: flex
  align-items: center
  justify-content: space-between
  gap: 12px
  margin-bottom: 14px
  font-size: 15px
  font-weight: 600
  color: var(--app-text-primary)
  span:last-child
    font-size: 13px
    font-weight: 400
    color: var(--app-text-secondary)

.mapping_grid
  display: grid
  grid-template-columns: repeat(3, minmax(0, 1fr))
  gap: 16px

.mapping_item
  display: flex
  flex-direction: column
  gap: 8px
  label
    font-size: 13px
    font-weight: 600
    color: var(--app-text-primary)
  span
    color: var(--app-danger)

.preview_table
  width: 100%
  min-width: 1040px

.preview_table_shell
  width: 100%
  overflow-x: auto

.progress_container
  margin-top: 18px
  display: flex
  flex-direction: column
  gap: 10px
  p
    color: var(--app-text-secondary)

.single_dialog_actions
  display: flex
  justify-content: flex-end
  gap: 12px

.dialog_button
  height: 36px
  min-width: 104px
  padding: 0 18px
  border: 1px solid var(--app-brand)
  border-radius: 8px
  background: var(--app-brand)
  color: #fff
  transition: .2s ease
  cursor: pointer

.dialog_button:hover:not(:disabled)
  background: var(--app-brand-hover)
  border-color: var(--app-brand-hover)

.dialog_button:disabled
  opacity: .6
  cursor: not-allowed

.dialog_button--ghost
  background: var(--app-surface)
  border-color: var(--app-border)
  color: var(--app-text-primary)

.dialog_button--ghost:hover:not(:disabled)
  background: var(--app-surface-muted)

@media (max-width: 980px)
  .button_box
    gap: 12px
    button
      margin-left: 0
      width: 100%

  .excel_status
    flex-direction: column
    align-items: flex-start

  .excel_status__actions
    flex-wrap: wrap
    width: 100%

  .mapping_panel__header,
  .preview_panel__header
    flex-direction: column
    align-items: flex-start

  .mapping_grid
    grid-template-columns: 1fr

  .single_dialog_actions
    width: 100%
    flex-direction: column-reverse

  .dialog_button
    width: 100%

  .preview_table
    min-width: 920px
</style>
