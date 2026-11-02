export interface ImportColumnRule<FieldKey extends string> {
  key: FieldKey;
  label: string;
  aliases: string[];
  fallbackIndex?: number;
  required?: boolean;
}

export interface ImportColumnOption {
  index: number;
  label: string;
  normalized: string;
}

export function normalizeImportText(value: unknown) {
  return value == null ? "" : String(value).trim();
}

function normalizeHeaderLabel(value: unknown) {
  return normalizeImportText(value).toLowerCase().replace(/[\s:_：\-()（）【】[\]]+/g, "");
}

export function createImportColumnOptions(headerRow: unknown[]) {
  return headerRow.map((value, index) => ({
    index,
    label: normalizeImportText(value) || `第 ${index + 1} 列`,
    normalized: normalizeHeaderLabel(value),
  }));
}

function matchesAlias(option: ImportColumnOption, alias: string, exactOnly: boolean) {
  const normalizedAlias = normalizeHeaderLabel(alias);
  if (!normalizedAlias || !option.normalized) {
    return false;
  }

  if (option.normalized === normalizedAlias) {
    return true;
  }

  if (exactOnly) {
    return false;
  }

  return option.normalized.includes(normalizedAlias) || normalizedAlias.includes(option.normalized);
}

/**
 * 推断 Excel 表头与导入字段的对应关系。
 *
 * 分三轮，避免“先到先得”把列占错位置：
 * 1. 所有规则先做表头精确匹配；
 * 2. 剩下的规则再做包含式模糊匹配；
 * 3. 最后才按 fallbackIndex 兜底，保证可选字段（如“权限”）不会抢走别的列。
 */
export function inferImportColumnMapping<FieldKey extends string>(
  options: ImportColumnOption[],
  rules: ImportColumnRule<FieldKey>[],
) {
  const usedIndexes = new Set<number>();
  const mapping = {} as Record<FieldKey, number | null>;

  const assignByAlias = (exactOnly: boolean) => {
    for (const rule of rules) {
      if (mapping[rule.key] != null) {
        continue;
      }

      const matchedOption = options.find(
        (option) => !usedIndexes.has(option.index)
          && rule.aliases.some((alias) => matchesAlias(option, alias, exactOnly)),
      );

      if (matchedOption) {
        mapping[rule.key] = matchedOption.index;
        usedIndexes.add(matchedOption.index);
      }
    }
  };

  assignByAlias(true);
  assignByAlias(false);

  for (const rule of rules) {
    if (mapping[rule.key] != null) {
      continue;
    }

    if (
      typeof rule.fallbackIndex === "number"
      && options[rule.fallbackIndex]
      && !usedIndexes.has(options[rule.fallbackIndex].index)
    ) {
      mapping[rule.key] = rule.fallbackIndex;
      usedIndexes.add(rule.fallbackIndex);
      continue;
    }

    mapping[rule.key] = null;
  }

  return mapping;
}

export function getImportCellValue(row: unknown[], columnIndex: number | null | undefined) {
  if (columnIndex == null || columnIndex < 0) {
    return "";
  }

  return normalizeImportText(row[columnIndex]);
}
