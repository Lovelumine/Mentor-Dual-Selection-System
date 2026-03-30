export type StudentListKey = "1" | "2" | "3" | "4" | "graduated";

export interface StudentListMenuItem {
  key: StudentListKey;
  path: string;
  label: string;
  grade: string | null;
}

function getAcademicBaseYear(now = new Date()) {
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1;
  return currentMonth >= 9 ? currentYear : currentYear - 1;
}

export function getCohortGradeByKey(key: Exclude<StudentListKey, "graduated">, now = new Date()) {
  const academicBaseYear = getAcademicBaseYear(now);
  const offset = Number(key) - 1;
  return String(academicBaseYear - offset);
}

export function getGraduatedThresholdGrade(now = new Date()) {
  return Number(getCohortGradeByKey("4", now));
}

export function getStudentListMenuItems(now = new Date()): StudentListMenuItem[] {
  return [
    { key: "1", path: "/stu_list1", label: `${getCohortGradeByKey("1", now)}级`, grade: getCohortGradeByKey("1", now) },
    { key: "2", path: "/stu_list2", label: `${getCohortGradeByKey("2", now)}级`, grade: getCohortGradeByKey("2", now) },
    { key: "3", path: "/stu_list3", label: `${getCohortGradeByKey("3", now)}级`, grade: getCohortGradeByKey("3", now) },
    { key: "4", path: "/stu_list4", label: `${getCohortGradeByKey("4", now)}级`, grade: getCohortGradeByKey("4", now) },
    { key: "graduated", path: "/stu_list_graduated", label: "已毕业", grade: null },
  ];
}

export function getStudentListPageTitle(key: StudentListKey, now = new Date()) {
  return key === "graduated" ? "学生列表（已毕业）" : `学生列表（${getCohortGradeByKey(key, now)}级）`;
}

export function getStudentListSectionTitle(key: StudentListKey, now = new Date()) {
  return key === "graduated" ? "列表内容（已毕业）" : `列表内容（${getCohortGradeByKey(key, now)}）级`;
}

export function matchesStudentListKey(gradeValue: string | number | null | undefined, key: StudentListKey, now = new Date()) {
  if (gradeValue === null || gradeValue === undefined) {
    return false;
  }

  const normalizedGrade = String(gradeValue).trim();
  if (!/^\d{4}$/.test(normalizedGrade)) {
    return false;
  }

  if (key === "graduated") {
    return Number(normalizedGrade) < getGraduatedThresholdGrade(now);
  }

  return normalizedGrade === getCohortGradeByKey(key, now);
}
