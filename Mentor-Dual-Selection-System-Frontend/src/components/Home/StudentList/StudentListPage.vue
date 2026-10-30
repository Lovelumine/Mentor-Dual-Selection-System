<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import * as XLSX from "xlsx";
import { http, httpAdmin } from "@/utils/http";
import { useRoute, useRouter } from "vue-router";
import { useUploadCoverStore } from "@/stores/UploadCoverStore";
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import { downloadBlob, downloadWorkbook, parseContentDispositionFilename } from "@/utils/fileDownload";
import { getSingleQueryValue, parseEnumQuery } from "@/utils/routeQuery";
import {
  analyzeImportRows,
  type ExistingImportUser,
} from "@/utils/importUserPreview";
import {
  getCohortGradeByKey,
  getStudentListPageTitle,
  type StudentListKey,
  matchesStudentListKey,
} from "@/utils/studentCohort";
import {
  createImportColumnOptions,
  getImportCellValue,
  inferImportColumnMapping,
  type ImportColumnRule,
} from "@/utils/importColumnMapping";
import { useResponsiveLayout } from "@/composables/useResponsiveLayout";

type StudentUploadField = "role" | "fullName" | "email" | "username" | "grade";

interface StudentRecord {
  uid: number;
  fullName: string;
  username: string;
  grade: string | null;
  email: string | null;
  mentor_id: number | null;
  resume: string | null;
}

interface TeacherRecord {
  uid: number;
  fullName: string;
}

interface StudentUploadPreviewRow {
  role: string;
  fullName: string;
  email: string;
  username: string;
  grade: string;
}

interface StudentImportPlanRow extends StudentUploadPreviewRow {
  rowIndex: number;
  action: "create" | "overwrite" | "conflict";
  actionLabel: string;
  message: string;
  existingUid: number | null;
}

interface StudentTableRow extends StudentRecord {
  isMentor: string;
  teacherName: string;
}

interface StudentEditDraft {
  uid: number;
  fullName: string;
  username: string;
  grade: string;
  email: string;
  resume: string;
}

interface SingleStudentForm {
  role: string;
  fullName: string;
  email: string;
  username: string;
  grade: string;
  resume: string;
}

const studentImportRules: ImportColumnRule<StudentUploadField>[] = [
  { key: "role", label: "权限", aliases: ["权限", "角色", "role"], fallbackIndex: 0 },
  { key: "fullName", label: "姓名", aliases: ["姓名", "学生姓名", "名称", "fullname", "name"], fallbackIndex: 1, required: true },
  { key: "email", label: "电子邮箱", aliases: ["电子邮箱", "邮箱", "email"], fallbackIndex: 2 },
  { key: "username", label: "学号", aliases: ["学号", "账号", "username", "studentid"], fallbackIndex: 3, required: true },
  { key: "grade", label: "年级", aliases: ["年级", "学年", "grade"], fallbackIndex: 4, required: true },
];

const props = defineProps<{
  listKey: StudentListKey;
}>();

const router = useRouter();
const route = useRoute();
const uploadCoverStore = useUploadCoverStore();
const userInfoStore = useUserInfoStore();
const mentorStatusQueryOptions = ["", "no", "accepted"] as const;
const { isMobile, isTablet } = useResponsiveLayout();

const students = ref<StudentRecord[]>([]);
const teachers = ref<TeacherRecord[]>([]);
const existingUsers = ref<ExistingImportUser[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const excelName = ref("");
const headerRow = ref<unknown[]>([]);
const rawRows = ref<unknown[][]>([]);
const columnMapping = ref<Record<StudentUploadField, number | null>>({
  role: null,
  fullName: null,
  email: null,
  username: null,
  grade: null,
});
const isUploadBoxShow = ref(false);
const isDownloadingTemplate = ref(false);
const isExporting = ref(false);
const isAddStudentDialogVisible = ref(false);
const isSubmittingSingleStudent = ref(false);
const editingUid = ref<number | null>(null);
const editingDraft = ref<StudentEditDraft | null>(null);
const isSaving = ref(false);
const studentTableRef = ref<{ clearSelection: () => void } | null>(null);
const selectedStudents = ref<StudentTableRow[]>([]);
const isBatchArchiving = ref(false);

const draftStudentName = ref("");
const draftMentorStatus = ref("");
const appliedStudentName = ref("");
const appliedMentorStatus = ref("");
const singleStudentForm = ref<SingleStudentForm>({
  role: "STUDENT",
  fullName: "",
  email: "",
  username: "",
  grade: "",
  resume: "",
});

const isAdmin = computed(() => userInfoStore.userInfo.role === "ADMIN");
const isEditing = computed(() => editingUid.value !== null);
const selectedStudentCount = computed(() => selectedStudents.value.length);
const pageTitle = computed(() => getStudentListPageTitle(props.listKey));
const addStudentDialogWidth = computed(() => (isMobile.value ? "94vw" : isTablet.value ? "86vw" : "560px"));
const columnOptions = computed(() => createImportColumnOptions(headerRow.value));
const missingRequiredLabels = computed(() =>
  studentImportRules
    .filter((rule) => rule.required && columnMapping.value[rule.key] == null)
    .map((rule) => rule.label),
);
const mappedUploadRows = computed<StudentUploadPreviewRow[]>(() =>
  rawRows.value
    .map((row) => ({
      role: getImportCellValue(row, columnMapping.value.role) || "STUDENT",
      fullName: getImportCellValue(row, columnMapping.value.fullName),
      email: getImportCellValue(row, columnMapping.value.email),
      username: getImportCellValue(row, columnMapping.value.username),
      grade: getImportCellValue(row, columnMapping.value.grade),
    }))
    .filter((row) => [row.fullName, row.email, row.username, row.grade].some(Boolean)),
);
const importPlans = computed<StudentImportPlanRow[]>(() =>
  analyzeImportRows({
    rows: mappedUploadRows.value,
    existingUsers: existingUsers.value,
    expectedRole: "STUDENT",
    uniqueFieldLabel: "学号",
    uniqueValue: (row) => row.username,
    requiredFields: [
      { label: "姓名", value: (row) => row.fullName },
      { label: "学号", value: (row) => row.username },
      { label: "年级", value: (row) => row.grade },
    ],
  }).map((plan) => {
    const normalizedGrade = parseGradeInput(plan.row.grade);
    const action = normalizedGrade ? plan.action : "conflict";
    const message = normalizedGrade
      ? plan.message
      : plan.message
        ? `${plan.message}；${getGradeFormatMessage()}`
        : getGradeFormatMessage();

    return {
      ...plan.row,
      grade: normalizedGrade ?? sanitizeGradeInput(plan.row.grade),
      rowIndex: plan.rowIndex,
      action,
      actionLabel: getActionLabel(action),
      message,
      existingUid: plan.existingUser?.uid ?? null,
    };
  }),
);
const previewRows = computed(() => importPlans.value.slice(0, 8));
const hasMorePreview = computed(() => importPlans.value.length > previewRows.value.length);
const uploadSummary = computed(() =>
  importPlans.value.reduce(
    (summary, row) => {
      summary[row.action] += 1;
      return summary;
    },
    { create: 0, overwrite: 0, conflict: 0 },
  ),
);
const canUploadStudents = computed(() =>
  importPlans.value.length > 0
  && missingRequiredLabels.value.length === 0
  && uploadSummary.value.conflict === 0,
);

const teacherNameMap = computed(() => new Map(teachers.value.map((teacher) => [teacher.uid, teacher.fullName])));

const tableData = computed<StudentTableRow[]>(() => {
  const keyword = appliedStudentName.value.trim().toLowerCase();

  return students.value
    .filter((student) => matchesStudentListKey(student.grade, props.listKey))
    .filter((student) => {
      if (!keyword) {
        return true;
      }

      return [
        student.fullName,
        student.username,
        student.email || "",
      ].some((value) => value?.toLowerCase().includes(keyword));
    })
    .filter((student) => {
      if (!appliedMentorStatus.value) {
        return true;
      }

      if (appliedMentorStatus.value === "no") {
        return student.mentor_id === null;
      }

      if (appliedMentorStatus.value === "accepted") {
        return student.mentor_id !== null;
      }

      return true;
    })
    .map((student) => ({
      ...student,
      isMentor: student.mentor_id ? "已通过" : "未选择",
      teacherName: student.mentor_id ? teacherNameMap.value.get(student.mentor_id) || "" : "",
    }));
});

function normalizeText(value: unknown) {
  return value == null ? "" : String(value);
}

function sanitizeGradeInput(value: unknown) {
  return normalizeText(value).replace(/\s+/g, "");
}

function parseGradeInput(value: unknown) {
  const normalized = sanitizeGradeInput(value);
  const match = normalized.match(/^(\d{2}|\d{4})(级)?$/);
  if (!match) {
    return null;
  }

  const digits = match[1];
  return digits.length === 2 ? `20${digits}` : digits;
}

function isValidGradeInput(value: string) {
  return parseGradeInput(value) !== null;
}

function normalizeGradeForSave(value: string) {
  return parseGradeInput(value) ?? sanitizeGradeInput(value);
}

function getGradeFormatMessage() {
  return "年级仅支持 2025、2025级、25 或 25级";
}

function getErrorMessage(error: any, fallback: string) {
  const responseData = error?.response?.data;

  if (typeof responseData?.data === "string") return responseData.data;
  if (typeof responseData?.data?.error === "string") return responseData.data.error;
  if (typeof responseData?.message === "string") return responseData.message;
  if (typeof error?.message === "string") return error.message;
  return fallback;
}

function buildEditDraft(row: StudentTableRow): StudentEditDraft {
  return {
    uid: row.uid,
    fullName: normalizeText(row.fullName),
    username: normalizeText(row.username),
    grade: normalizeText(row.grade),
    email: normalizeText(row.email),
    resume: normalizeText(row.resume),
  };
}

function authHeaders() {
  return {
    Accept: "*/*",
    Authorization: `Bearer ${localStorage.getItem("token")}`,
  };
}

function syncFiltersFromRouteQuery() {
  const nextKeyword = getSingleQueryValue(route.query.keyword);
  const nextMentorStatus = parseEnumQuery(route.query.mentorStatus, mentorStatusQueryOptions, "");

  draftStudentName.value = nextKeyword;
  appliedStudentName.value = nextKeyword;
  draftMentorStatus.value = nextMentorStatus;
  appliedMentorStatus.value = nextMentorStatus;
}

async function updateFilterQuery() {
  const nextKeyword = appliedStudentName.value.trim();
  const nextMentorStatus = appliedMentorStatus.value;
  const currentKeyword = getSingleQueryValue(route.query.keyword);
  const currentMentorStatus = parseEnumQuery(route.query.mentorStatus, mentorStatusQueryOptions, "");

  if (currentKeyword === nextKeyword && currentMentorStatus === nextMentorStatus) {
    return;
  }

  const nextQuery = { ...route.query };
  delete nextQuery.keyword;
  delete nextQuery.mentorStatus;

  if (nextKeyword) {
    nextQuery.keyword = nextKeyword;
  }

  if (nextMentorStatus) {
    nextQuery.mentorStatus = nextMentorStatus;
  }

  await router.replace({
    path: route.path,
    query: nextQuery,
  });
}

function cancelEditing() {
  editingUid.value = null;
  editingDraft.value = null;
}

function isRowEditing(row: StudentTableRow) {
  return isAdmin.value && editingUid.value === row.uid;
}

function getRowClassName({ row }: { row: StudentTableRow }) {
  return row.uid === editingUid.value ? "editing-row" : "";
}

async function startEditing(row: StudentTableRow) {
  if (!isAdmin.value || isSaving.value) return;

  if (editingUid.value !== null && editingUid.value !== row.uid) {
    const saved = await saveEditingRow(true);
    if (!saved) return;
  }

  editingUid.value = row.uid;
  editingDraft.value = buildEditDraft(row);
}

async function handleRowDoubleClick(row: StudentTableRow) {
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
    grade: sanitizeGradeInput(editingDraft.value.grade),
    email: editingDraft.value.email.trim(),
    resume: editingDraft.value.resume.trim(),
  };

  if (!draft.fullName) {
    ElMessage.warning("学生姓名不能为空");
    return false;
  }

  if (!draft.username) {
    ElMessage.warning("学号不能为空");
    return false;
  }

  if (!draft.grade) {
    ElMessage.warning("年级不能为空");
    return false;
  }

  if (!isValidGradeInput(draft.grade)) {
    ElMessage.warning(getGradeFormatMessage());
    return false;
  }

  const normalizedGrade = normalizeGradeForSave(draft.grade);

  isSaving.value = true;

  try {
    const basicRes = await http({
      url: "/user/update",
      method: "POST",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      data: {
        uid: draft.uid,
        role: "STUDENT",
        fullName: draft.fullName,
        email: draft.email || null,
        username: draft.username,
        grade: normalizedGrade,
      },
    });

    if (basicRes.data.code !== 200) {
      throw new Error(basicRes.data?.message || "学生基础信息保存失败");
    }

    const detailRes = await httpAdmin({
      url: `/update/${draft.uid}`,
      method: "PUT",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      data: {
        resume: draft.resume,
      },
    });

    if (detailRes.data.code !== 200) {
      throw new Error(detailRes.data?.message || "学生简介保存失败");
    }

    students.value = students.value.map((student) => (
      student.uid === draft.uid
        ? {
            ...student,
            fullName: draft.fullName,
            grade: normalizedGrade,
            email: draft.email || null,
            resume: draft.resume || null,
          }
        : student
    ));

    cancelEditing();

    if (!silentSuccess) {
      ElMessage.success("学生信息已保存");
    }

    return true;
  } catch (error: any) {
    ElMessage.error(getErrorMessage(error, "保存失败，请稍后重试"));
    return false;
  } finally {
    isSaving.value = false;
  }
}

function getActionLabel(action: StudentImportPlanRow["action"]) {
  if (action === "overwrite") return "覆盖更新";
  if (action === "conflict") return "存在冲突";
  return "新增账号";
}

function getActionTagType(action: StudentImportPlanRow["action"]) {
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

const fetchBaseData = async () => {
  if (!userInfoStore.userInfo.role) {
    userInfoStore.fetchUserInfo();
  }

  const [studentsResponse, teachersResponse] = await Promise.all([
    http({
      url: "/search/students",
      method: "GET",
      headers: authHeaders(),
    }),
    http({
      url: "/search/teachers",
      method: "GET",
      headers: authHeaders(),
    }),
  ]);

  if (studentsResponse.data.code !== 200) {
    throw new Error(studentsResponse.data?.data?.error || "学生列表获取失败");
  }

  if (teachersResponse.data.code !== 200) {
    throw new Error(teachersResponse.data?.data?.error || "导师列表获取失败");
  }

  students.value = studentsResponse.data.data;
  teachers.value = teachersResponse.data.data;
  studentTableRef.value?.clearSelection();
  selectedStudents.value = [];

  if (editingUid.value !== null && !students.value.some((student) => student.uid === editingUid.value)) {
    cancelEditing();
  }
};

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

const applyFilters = () => {
  appliedStudentName.value = draftStudentName.value;
  appliedMentorStatus.value = draftMentorStatus.value;
  void updateFilterQuery();
};

const resetFilters = () => {
  draftStudentName.value = "";
  draftMentorStatus.value = "";
  appliedStudentName.value = "";
  appliedMentorStatus.value = "";
  void updateFilterQuery();
};

const addSingleStudent = () => {
  resetSingleStudentForm();
  isAddStudentDialogVisible.value = true;
};

function resetSingleStudentForm() {
  singleStudentForm.value = {
    role: "STUDENT",
    fullName: "",
    email: "",
    username: "",
    grade: props.listKey === "graduated" ? "" : getCohortGradeByKey(props.listKey),
    resume: "",
  };
}

const submitSingleStudent = async () => {
  if (isSubmittingSingleStudent.value) return;

  const draft = {
    role: "STUDENT",
    fullName: singleStudentForm.value.fullName.trim(),
    email: singleStudentForm.value.email.trim(),
    username: singleStudentForm.value.username.trim(),
    grade: sanitizeGradeInput(singleStudentForm.value.grade),
    resume: singleStudentForm.value.resume.trim(),
  };

  if (!draft.fullName) {
    ElMessage.warning("请填写姓名");
    return;
  }

  if (!draft.username) {
    ElMessage.warning("请填写学号");
    return;
  }

  if (!draft.grade) {
    ElMessage.warning("请填写年级");
    return;
  }

  if (!isValidGradeInput(draft.grade)) {
    ElMessage.warning(getGradeFormatMessage());
    return;
  }

  const normalizedGrade = normalizeGradeForSave(draft.grade);
  isSubmittingSingleStudent.value = true;

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
        grade: normalizedGrade,
      },
    });

    if (basicRes.data.code !== 200) {
      throw new Error(basicRes.data?.data?.error || basicRes.data?.message || "学生基础信息添加失败");
    }

    const uid = Number(basicRes.data?.data?.uid);
    if (!Number.isFinite(uid) || uid <= 0) {
      throw new Error("学生基础信息返回的 UID 无效");
    }

    const detailRes = await httpAdmin({
      url: `/update/${uid}`,
      method: "PUT",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      data: {
        resume: draft.resume,
      },
    });

    if (detailRes.data.code !== 200) {
      throw new Error(detailRes.data?.data?.error || detailRes.data?.message || "学生简介添加失败");
    }

    await fetchBaseData();
    isAddStudentDialogVisible.value = false;
    resetSingleStudentForm();
    ElMessage.success("学生信息已添加");
  } catch (error: any) {
    ElMessage.error(getErrorMessage(error, "学生信息添加失败"));
  } finally {
    isSubmittingSingleStudent.value = false;
  }
};

const downloadStudentTemplate = async () => {
  if (isDownloadingTemplate.value) return;

  isDownloadingTemplate.value = true;
  try {
    const response = await http({
      url: "/import/template/student",
      method: "GET",
      responseType: "blob",
      headers: authHeaders(),
    });

    const filename = parseContentDispositionFilename(response.headers["content-disposition"]) || "学生信息导入模板.xlsx";
    downloadBlob(response.data, filename);
    ElMessage.success("学生导入模板已开始下载");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.data?.error || "学生模板下载失败");
  } finally {
    isDownloadingTemplate.value = false;
  }
};

const exportCurrentStudents = () => {
  if (isExporting.value) return;

  isExporting.value = true;
  try {
    const rows = tableData.value.map((student) => ({
      姓名: student.fullName ?? "",
      学号: student.username ?? "",
      年级: student.grade ?? "",
      邮箱: student.email ?? "",
      选择导师状态: student.isMentor ?? "",
      导师姓名: student.teacherName ?? "",
      简介: student.resume ?? "",
    }));

    const workbook = XLSX.utils.book_new();
    const worksheet = XLSX.utils.json_to_sheet(rows);
    XLSX.utils.book_append_sheet(workbook, worksheet, "学生信息");
    downloadWorkbook(workbook, `${pageTitle.value}-${getTimestamp()}.xlsx`);
    ElMessage.success("学生信息已导出");
  } catch (error: any) {
    ElMessage.error(error?.message || "学生信息导出失败");
  } finally {
    isExporting.value = false;
  }
};

const triggerFileInput = () => {
  fileInput.value?.click();
};

function resetUploadPreview() {
  excelName.value = "";
  headerRow.value = [];
  rawRows.value = [];
  existingUsers.value = [];
  isUploadBoxShow.value = false;
  if (fileInput.value) {
    fileInput.value.value = "";
  }
}

function buildHeaderRow(rows: unknown[][]) {
  const widestColumnCount = rows.reduce((max, row) => Math.max(max, row.length), 0);
  const sourceHeader = rows[0] ?? [];
  return Array.from({ length: widestColumnCount }, (_, index) => sourceHeader[index] ?? "");
}

function inferStudentMapping() {
  columnMapping.value = inferImportColumnMapping(columnOptions.value, studentImportRules);
}

const handleFileChange = (event: Event) => {
  const inputElement = event.target as HTMLInputElement | null;
  const file = inputElement?.files?.[0];
  if (!file) {
    return;
  }

  excelName.value = file.name;
  const extension = file.name.toLowerCase().split(".").pop();
  if (!extension || !["xlsx", "xls"].includes(extension)) {
    ElMessage.error("您选择的不是 Excel 表格文档");
    resetUploadPreview();
    return;
  }

  const reader = new FileReader();
  reader.onload = async (loadEvent: ProgressEvent<FileReader>) => {
    const arrayBuffer = loadEvent.target?.result as ArrayBuffer | null;
    if (!arrayBuffer) {
      return;
    }

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
    inferStudentMapping();

    try {
      await fetchExistingUsers();
    } catch (error: any) {
      ElMessage.error(error?.message || "现有账号信息获取失败");
      resetUploadPreview();
      return;
    }

    isUploadBoxShow.value = true;
    ElMessage.success(`已读取 ${mappedUploadRows.value.length} 条学生数据，请核对预览后再确认上传`);
  };

  reader.readAsArrayBuffer(file);
};

const uploadStudents = async () => {
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
        `检测到 ${uploadSummary.value.overwrite} 条学号已存在，确认上传后会覆盖更新原学生信息，不会新建重复账号。是否继续？`,
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

  uploadCoverStore.triggerCoverShow(true);
  const rowsToUpload = importPlans.value.filter((row) => row.action !== "conflict");
  uploadCoverStore.recordNumberOfPeople(rowsToUpload.length);
  uploadCoverStore.recordNowNumberOfPeople(0);
  uploadCoverStore.recordNowPeopleName("");
  uploadCoverStore.recordUpdateFalStatus(0);
  let failCount = 0;

  for (let index = 0; index < rowsToUpload.length; index += 1) {
    const row = rowsToUpload[index];
    uploadCoverStore.recordNowNumberOfPeople(index + 1);
    uploadCoverStore.recordNowPeopleName(row.fullName || row.username);

    try {
      const existingUser = row.existingUid
        ? existingUsers.value.find((user) => user.uid === row.existingUid)
        : null;
      const mergedEmail = row.email || existingUser?.email || null;

      const response = await http({
        url: "/user/update",
        method: "POST",
        headers: {
          ...authHeaders(),
          "Content-Type": "application/json",
        },
        data: {
          uid: row.existingUid ?? undefined,
          role: row.role || "STUDENT",
          fullName: row.fullName,
          email: mergedEmail,
          username: row.username,
          grade: normalizeGradeForSave(row.grade),
        },
      });

      if (response.data.code !== 200) {
        failCount += 1;
        uploadCoverStore.recordUpdateFalStatus(failCount);
      }
    } catch (error: any) {
      failCount += 1;
      uploadCoverStore.recordUpdateFalStatus(failCount);
      ElMessage.error(error?.response?.data?.data?.error || `第 ${index + 1} 条数据上传失败`);
    }
  }

  await fetchBaseData();

  if (failCount === 0) {
    ElMessage.success("批量上传学生信息已完成");
    resetUploadPreview();
  } else {
    ElMessage.warning(`学生信息上传完成，但有 ${failCount} 条失败`);
  }
};

const archiveStudent = async (student: StudentRecord) => {
  if (isEditing.value) {
    ElMessage.warning("请先保存或取消当前正在编辑的学生信息");
    return;
  }

  let reason = "";

  try {
    const result = await ElMessageBox.prompt(
      `将学生“${student.fullName}”标记为转出后，账号会被停用，当前师生关系会解除，但申请记录和历史关系会保留。可选填写转出原因。`,
      "确认转出学生",
      {
        confirmButtonText: "确认转出",
        cancelButtonText: "取消",
        type: "warning",
        closeOnClickModal: false,
        inputPlaceholder: "选填，例如：转专业转出 / 已毕业离开培养体系",
        inputValue: "",
        inputValidator: () => true,
      },
    );

    reason = result.value?.trim?.() || "";
  } catch {
    return;
  }

  try {
    const response = await http({
      url: `/user/student/${student.uid}/archive`,
      method: "POST",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      data: {
        reason,
      },
    });

    if (response.data.code !== 200) {
      ElMessage.error(response.data?.data?.error || "转出学生失败");
      return;
    }

    students.value = students.value.filter((item) => item.uid !== student.uid);
    selectedStudents.value = selectedStudents.value.filter((item) => item.uid !== student.uid);
    if (editingUid.value === student.uid) {
      cancelEditing();
    }
    ElMessage.success("学生已转出停用，历史记录已保留");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.data?.error || "转出学生失败");
  }
};

function handleStudentSelectionChange(rows: StudentTableRow[]) {
  selectedStudents.value = rows;
}

async function archiveSelectedStudents() {
  if (isEditing.value) {
    ElMessage.warning("请先保存或取消当前正在编辑的学生信息");
    return;
  }

  if (selectedStudents.value.length === 0) {
    ElMessage.warning("请先勾选需要转出的学生");
    return;
  }

  const studentsToArchive = [...selectedStudents.value];
  const previewNames = studentsToArchive
    .slice(0, 5)
    .map((student) => `${student.fullName}（${student.username}）`)
    .join("、");
  const remainingCount = studentsToArchive.length - 5;
  const remainingText = remainingCount > 0 ? `等共 ${studentsToArchive.length} 名学生` : "";

  let reason = "";
  try {
    const result = await ElMessageBox.prompt(
      `即将批量转出 ${studentsToArchive.length} 名学生：${previewNames}${remainingText}。\n\n转出后账号会被停用，当前师生关系会解除，但申请记录和历史关系会保留。可选填写转出原因。`,
      "确认批量转出",
      {
        confirmButtonText: "确认批量转出",
        cancelButtonText: "取消",
        type: "warning",
        closeOnClickModal: false,
        inputPlaceholder: "选填，例如：转专业转出 / 已毕业离开培养体系",
        inputValue: "",
        inputValidator: () => true,
      },
    );
    reason = result.value?.trim?.() || "";
  } catch {
    return;
  }

  isBatchArchiving.value = true;
  try {
    const response = await http({
      url: "/user/students/archive",
      method: "POST",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      data: {
        studentIds: studentsToArchive.map((student) => student.uid),
        reason,
      },
    });

    if (response.data.code !== 200) {
      throw new Error(response.data?.data?.error || response.data?.message || "批量转出学生失败");
    }

    await fetchBaseData();
    const archivedCount = Number(response.data?.data?.archivedStudentCount) || studentsToArchive.length;
    ElMessage.success(`已转出 ${archivedCount} 名学生，历史记录已保留`);
  } catch (error: any) {
    ElMessage.error(getErrorMessage(error, "批量转出学生失败"));
  } finally {
    isBatchArchiving.value = false;
  }
}

watch(
  () => route.query,
  () => {
    syncFiltersFromRouteQuery();
  },
  { immediate: true },
);

watch(
  tableData,
  (rows) => {
    const visibleStudentIds = new Set(rows.map((student) => student.uid));
    selectedStudents.value = selectedStudents.value.filter((student) => visibleStudentIds.has(student.uid));
  },
  { flush: "post" },
);

watch(
  () => isAdmin.value,
  (newVal) => {
    if (!newVal) {
      cancelEditing();
    }
  },
);

onMounted(async () => {
  try {
    await fetchBaseData();
  } catch (error: any) {
    ElMessage.error(error?.message || "学生列表获取失败");
  }
});
</script>

<template>
  <div class="title">
    <span>{{ pageTitle }}</span>
  </div>

  <div class="search_box">
    <span class="input_item">学生姓名：<input v-model="draftStudentName" type="text" placeholder="请输入" /></span>
    <span class="input_item">
      <label for="is_select_teacher">选择导师状态：</label>
      <select id="is_select_teacher" v-model="draftMentorStatus">
        <option value="">全部</option>
        <option value="no">未选择</option>
        <option value="accepted">已通过</option>
      </select>
    </span>
    <button class="button" @click="applyFilters">查询</button>
    <button class="button" @click="resetFilters">重置</button>
  </div>

  <div class="button_box">
    <button @click="addSingleStudent">添加单条学生信息</button>
    <button :disabled="isDownloadingTemplate" @click="downloadStudentTemplate">
      {{ isDownloadingTemplate ? "下载中..." : "下载学生表格模板" }}
    </button>
    <button :disabled="isExporting" @click="exportCurrentStudents">
      {{ isExporting ? "导出中..." : "导出当前学生信息" }}
    </button>
    <input ref="fileInput" type="file" style="display: none" accept=".xlsx, .xls" @change="handleFileChange" />
    <button @click="triggerFileInput">批量上传学生信息</button>
  </div>

  <el-dialog
    v-model="isAddStudentDialogVisible"
    title="添加单条学生信息"
    :width="addStudentDialogWidth"
    destroy-on-close
    :close-on-click-modal="!isSubmittingSingleStudent"
  >
    <el-form label-width="92px" @submit.prevent="submitSingleStudent">
      <el-form-item label="姓名">
        <el-input v-model="singleStudentForm.fullName" placeholder="请输入学生姓名" />
      </el-form-item>
      <el-form-item label="学号">
        <el-input v-model="singleStudentForm.username" placeholder="请输入学生学号" />
      </el-form-item>
      <el-form-item label="电子邮箱">
        <el-input v-model="singleStudentForm.email" placeholder="可选，推荐填写学校邮箱" />
      </el-form-item>
      <el-form-item label="年级">
        <el-input v-model="singleStudentForm.grade" placeholder="例如：2025、2025级、25" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input
          v-model="singleStudentForm.resume"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 6 }"
          placeholder="可选，填写学生简介"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="single_dialog_actions">
        <button class="dialog_button dialog_button--ghost" :disabled="isSubmittingSingleStudent" @click="isAddStudentDialogVisible = false">取消</button>
        <button class="dialog_button" :disabled="isSubmittingSingleStudent" @click="submitSingleStudent">
          {{ isSubmittingSingleStudent ? "提交中..." : "确认添加" }}
        </button>
      </div>
    </template>
  </el-dialog>

  <div v-if="isUploadBoxShow" class="excel_status">
    <div class="excel_status__summary">
      <p>已读取文件：{{ excelName }}，当前映射后共 {{ importPlans.length }} 条学生数据</p>
      <span>系统会自动匹配列，但你可以在下面手动修正；点击“确认上传”后才会正式写入系统</span>
      <span>新增 {{ uploadSummary.create }} 条，覆盖 {{ uploadSummary.overwrite }} 条，冲突 {{ uploadSummary.conflict }} 条</span>
      <span v-if="missingRequiredLabels.length" class="excel_status__warning">待补充映射：{{ missingRequiredLabels.join("、") }}</span>
      <span v-else-if="uploadSummary.conflict > 0" class="excel_status__warning">存在冲突数据，需先修正原表格再上传</span>
    </div>
    <div class="excel_status__actions">
      <button :disabled="!canUploadStudents" @click="uploadStudents">确认上传</button>
      <button @click="triggerFileInput">重新上传</button>
      <button class="button_danger" @click="resetUploadPreview">取消</button>
    </div>
  </div>

  <div v-if="isUploadBoxShow" class="mapping_panel">
    <div class="mapping_panel__header">
      <span>列映射</span>
      <span>如果系统自动识别不准确，可以在这里纠正</span>
    </div>

    <div class="mapping_grid">
      <div v-for="rule in studentImportRules" :key="rule.key" class="mapping_item">
        <label :for="`student-mapping-${rule.key}`">
          {{ rule.label }}
          <span v-if="rule.required">*</span>
        </label>
        <el-select :id="`student-mapping-${rule.key}`" v-model="columnMapping[rule.key]" clearable placeholder="请选择列">
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

    <el-table :data="previewRows" border size="small" class="preview_table">
      <el-table-column prop="rowIndex" label="序号" width="70" />
      <el-table-column label="导入动作" min-width="110">
        <template #default="{ row }">
          <el-tag :type="getActionTagType(row.action)" effect="light">{{ row.actionLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fullName" label="姓名" min-width="120" />
      <el-table-column prop="email" label="电子邮箱" min-width="220" />
      <el-table-column prop="username" label="学号" min-width="120" />
      <el-table-column prop="grade" label="年级" min-width="100" />
      <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
    </el-table>
  </div>

  <div v-if="isAdmin" class="section_title">
    <span class="section_title__hint">
      <template v-if="isEditing">正在编辑当前学生。学号、导师状态和导师姓名保持只读，年级支持 2025、2025级、25 或 25级</template>
      <template v-else>管理员双击学生行可编辑；学号和导师信息不可修改</template>
    </span>
    <div class="section_title__actions">
      <span v-if="selectedStudentCount > 0" class="section_title__selection">
        已选择 {{ selectedStudentCount }} 名
      </span>
      <button
        class="header_button header_button--danger"
        :disabled="selectedStudentCount === 0 || isEditing || isBatchArchiving"
        @click="archiveSelectedStudents"
      >
        {{ isBatchArchiving ? "转出中..." : `批量转出（${selectedStudentCount}）` }}
      </button>
      <template v-if="isEditing">
        <button class="header_button" :disabled="isSaving" @click="saveEditingRow()">
          {{ isSaving ? "保存中..." : "保存" }}
        </button>
        <button class="header_button header_button--ghost" :disabled="isSaving" @click="cancelEditing">取消</button>
      </template>
    </div>
  </div>

  <div class="table_shell">
    <p class="table_scroll_hint">左右滑动表格可查看完整学生信息</p>
    <el-table
      ref="studentTableRef"
      :data="tableData"
      :row-class-name="getRowClassName"
      row-key="uid"
      stripe
      class="table"
      empty-text="暂时没有学生数据"
      @row-dblclick="handleRowDoubleClick"
      @selection-change="handleStudentSelectionChange"
    >
      <el-table-column v-if="isAdmin" type="selection" width="48" fixed="left" />
      <el-table-column label="姓名" min-width="130">
        <template #default="{ row }">
          <el-input v-if="isRowEditing(row) && editingDraft" v-model="editingDraft.fullName" size="small" />
          <span v-else>{{ row.fullName }}</span>
        </template>
      </el-table-column>

      <el-table-column label="学号" min-width="120">
        <template #default="{ row }">
          <span>{{ row.username }}</span>
        </template>
      </el-table-column>

      <el-table-column label="年级" min-width="120">
        <template #default="{ row }">
          <el-input
            v-if="isRowEditing(row) && editingDraft"
            v-model="editingDraft.grade"
            size="small"
            placeholder="例如：2025、2025级、25"
          />
          <span v-else>{{ row.grade }}</span>
        </template>
      </el-table-column>

      <el-table-column label="邮箱" min-width="220">
        <template #default="{ row }">
          <el-input v-if="isRowEditing(row) && editingDraft" v-model="editingDraft.email" size="small" />
          <span v-else class="cell_multiline">{{ row.email }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="isMentor" label="选择导师状态" min-width="140" />
      <el-table-column prop="teacherName" show-overflow-tooltip label="导师姓名" min-width="140" />

      <el-table-column label="简介" min-width="280">
        <template #default="{ row }">
          <el-input
            v-if="isRowEditing(row) && editingDraft"
            v-model="editingDraft.resume"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            resize="none"
          />
          <span v-else class="cell_multiline">{{ row.resume }}</span>
        </template>
      </el-table-column>

      <el-table-column v-if="isAdmin" label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" :disabled="isEditing" @click="archiveStudent(row)">转出</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: var(--app-surface-muted)
  line-height: 60px
  padding-left: 20px
  font-size: 20px
  color: var(--app-text-primary)

.search_box
  margin: 20px auto
  padding: 0 20px
  display: flex
  align-items: flex-end
  flex-wrap: wrap
  gap: 12px 20px

.input_item
  display: inline-flex
  align-items: center
  gap: 10px
  font-size: 16px
  color: var(--app-text-primary)
  input
    width: 220px
    height: 32px
    transition: .3s ease
    border: 1px solid var(--app-border)
    border-radius: 5px
    background-color: var(--app-surface)
    color: var(--app-text-primary)
  input:hover
    border: 1px solid var(--app-brand)
  input:focus
    outline: none
    box-shadow: 0 0 0 3px var(--app-brand-ring)
    border: 1px var(--app-brand) solid
  select
    width: 220px
    height: 32px
    transition: .3s ease
    border: 1px solid var(--app-border)
    border-radius: 5px
    background-color: var(--app-surface)
    color: var(--app-text-primary)
  select:hover
    border: 1px solid var(--app-brand)
  select:focus
    outline: none
    box-shadow: 0 0 0 3px var(--app-brand-ring)
    border: 1px var(--app-brand) solid

.button
  border-radius: 5px
  width: 60px
  height: 32px
  background-color: var(--app-brand)
  color: white
  border: 1px solid var(--app-brand)
  font-size: 15px
  transition: .3s ease

.button:hover
  background-color: var(--app-brand-hover)
  border: 1px solid var(--app-brand-hover)

.button:last-child
  background-color: var(--app-surface)
  border: 1px solid var(--app-danger)
  color: var(--app-danger)

.button:last-child:hover
  background-color: var(--app-danger)
  color: white

.button_box
  background-color: var(--app-surface-muted)
  width: 100%
  min-height: 60px
  display: flex
  align-items: center
  flex-wrap: wrap
  gap: 14px
  padding: 10px 20px
  button
    padding: 0 20px
    height: 36px
    min-width: 160px
    font-size: 14px
    border-radius: 5px
    border: none
    color: white
    background-color: var(--app-brand)
    transition: .3s ease
  button:hover
    background-color: var(--app-brand-hover)
  button:disabled
    cursor: not-allowed
    opacity: 0.7

.excel_status
  margin-top: 14px
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

.section_title
  width: 100%
  min-height: 40px
  background-color: var(--app-surface-muted)
  line-height: 40px
  padding: 0 20px
  font-size: 16px
  margin: 20px 0
  display: flex
  align-items: center
  justify-content: space-between
  gap: 12px
  color: var(--app-text-primary)

.section_title__actions
  display: flex
  align-items: center
  justify-content: flex-end
  gap: 10px
  flex-wrap: wrap

.section_title__selection
  font-size: 13px
  color: var(--app-text-secondary)

.section_title__hint
  font-size: 13px
  color: var(--app-text-secondary)

.header_button
  border: none
  border-radius: 6px
  height: 32px
  padding: 0 16px
  background-color: var(--app-brand)
  color: #ffffff
  font-size: 14px
  cursor: pointer
  transition: .2s ease

.header_button:hover:not(:disabled)
  background-color: var(--app-brand-hover)

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
  background-color: transparent
  border: 1px solid var(--app-danger)
  color: var(--app-danger)

.header_button--danger:hover:not(:disabled)
  background-color: var(--app-danger)
  color: #ffffff

.table_shell
  width: 100%
  overflow-x: auto
  -webkit-overflow-scrolling: touch

.table_scroll_hint
  display: none

.table
  width: 100%
  min-width: 0

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

.cell_multiline
  display: block
  white-space: pre-wrap
  word-break: break-word
  line-height: 1.6

:deep(.editing-row > td)
  background-color: var(--app-brand-soft) !important

:deep(.editing-row .el-input__wrapper),
:deep(.editing-row .el-textarea__inner)
  box-shadow: 0 0 0 1px var(--app-brand-ring) inset

@media (max-width: 980px)
  .search_box
    padding: 0 16px
    flex-direction: column
    align-items: stretch

  .input_item
    width: 100%
    flex-direction: column
    align-items: stretch
    gap: 8px

  .input_item input,
  .input_item select
    width: 100%

  .button
    width: 100%

  .button_box button
    width: 100%
    min-width: 0

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

  .section_title
    align-items: flex-start
    flex-direction: column
    padding: 10px 20px

  .section_title__actions
    width: 100%
    justify-content: flex-start

  .single_dialog_actions
    width: 100%
    flex-direction: column-reverse

  .dialog_button
    width: 100%

  .table
    min-width: 0

  .table_scroll_hint
    display: block
    margin: 0 12px 8px
    color: var(--app-text-secondary)
    font-size: 12px
</style>
