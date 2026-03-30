export function getSingleQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return typeof value[0] === "string" ? value[0] : "";
  }

  return typeof value === "string" ? value : "";
}

export function parsePositiveIntQuery(value: unknown, fallback = 1) {
  const parsed = Number.parseInt(getSingleQueryValue(value), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function parseEnumQuery<T extends string>(value: unknown, allowedValues: readonly T[], fallback: T) {
  const normalized = getSingleQueryValue(value) as T;
  return allowedValues.includes(normalized) ? normalized : fallback;
}
