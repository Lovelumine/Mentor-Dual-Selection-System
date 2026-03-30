export interface ExistingImportUser {
  uid: number;
  username: string;
  fullName: string;
  email: string;
  role: string;
  active: boolean;
}

export interface ImportRequiredField<Row> {
  label: string;
  value: (row: Row) => string;
}

export interface ImportPreviewAnalysis<Row> {
  row: Row;
  rowIndex: number;
  action: "create" | "overwrite" | "conflict";
  message: string;
  existingUser: ExistingImportUser | null;
}

interface AnalyzeImportRowsOptions<Row> {
  rows: Row[];
  existingUsers: ExistingImportUser[];
  expectedRole: string;
  uniqueFieldLabel: string;
  uniqueValue: (row: Row) => string;
  requiredFields: Array<ImportRequiredField<Row>>;
}

const ROLE_LABEL_MAP: Record<string, string> = {
  ADMIN: "管理员",
  TEACHER: "导师",
  STUDENT: "学生",
};

function normalizeText(value: unknown) {
  return value == null ? "" : String(value).trim();
}

function roleLabel(role: string) {
  return ROLE_LABEL_MAP[role] ?? (role || "其他账号");
}

export function analyzeImportRows<Row>(options: AnalyzeImportRowsOptions<Row>): Array<ImportPreviewAnalysis<Row>> {
  const existingByUsername = new Map<string, ExistingImportUser>();
  for (const user of options.existingUsers) {
    const normalizedUsername = normalizeText(user.username);
    if (normalizedUsername) {
      existingByUsername.set(normalizedUsername, user);
    }
  }

  const uploadCounts = new Map<string, number>();
  for (const row of options.rows) {
    const key = normalizeText(options.uniqueValue(row));
    if (!key) continue;
    uploadCounts.set(key, (uploadCounts.get(key) ?? 0) + 1);
  }

  return options.rows.map((row, index) => {
    const username = normalizeText(options.uniqueValue(row));
    const missingFields = options.requiredFields
      .filter((field) => !normalizeText(field.value(row)))
      .map((field) => field.label);

    if (missingFields.length > 0) {
      return {
        row,
        rowIndex: index + 1,
        action: "conflict" as const,
        message: `缺少必填字段：${missingFields.join("、")}`,
        existingUser: null,
      };
    }

    if ((uploadCounts.get(username) ?? 0) > 1) {
      return {
        row,
        rowIndex: index + 1,
        action: "conflict" as const,
        message: `导入文件内存在重复${options.uniqueFieldLabel}`,
        existingUser: null,
      };
    }

    const existingUser = existingByUsername.get(username) ?? null;
    if (!existingUser) {
      return {
        row,
        rowIndex: index + 1,
        action: "create" as const,
        message: `系统中不存在相同${options.uniqueFieldLabel}，将新建账号`,
        existingUser: null,
      };
    }

    if (existingUser.role !== options.expectedRole) {
      return {
        row,
        rowIndex: index + 1,
        action: "conflict" as const,
        message: `该${options.uniqueFieldLabel}已被${roleLabel(existingUser.role)}账号占用`,
        existingUser,
      };
    }

    if (!existingUser.active) {
      return {
        row,
        rowIndex: index + 1,
        action: "conflict" as const,
        message: `该${options.uniqueFieldLabel}对应账号已停用，请先人工处理`,
        existingUser,
      };
    }

    return {
      row,
      rowIndex: index + 1,
      action: "overwrite" as const,
      message: `将覆盖更新现有${roleLabel(existingUser.role)}账号：${existingUser.fullName || existingUser.username}`,
      existingUser,
    };
  });
}
