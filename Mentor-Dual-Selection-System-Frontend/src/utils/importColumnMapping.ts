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

export function inferImportColumnMapping<FieldKey extends string>(
  options: ImportColumnOption[],
  rules: ImportColumnRule<FieldKey>[],
) {
  const usedIndexes = new Set<number>();
  const mapping = {} as Record<FieldKey, number | null>;

  for (const rule of rules) {
    const matchedOption = options.find((option) => {
      if (usedIndexes.has(option.index)) {
        return false;
      }

      return rule.aliases.some((alias) => {
        const normalizedAlias = normalizeHeaderLabel(alias);
        return option.normalized === normalizedAlias
          || option.normalized.includes(normalizedAlias)
          || normalizedAlias.includes(option.normalized);
      });
    });

    if (matchedOption) {
      mapping[rule.key] = matchedOption.index;
      usedIndexes.add(matchedOption.index);
      continue;
    }

    if (
      typeof rule.fallbackIndex === "number"
      && options[rule.fallbackIndex]
      && !usedIndexes.has(rule.fallbackIndex)
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
