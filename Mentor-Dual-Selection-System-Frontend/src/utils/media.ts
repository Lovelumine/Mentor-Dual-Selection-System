const MINIO_PUBLIC_PREFIX = "https://minio.lumoxuan.cn/mentor-dual-selection-system/";
const MINIO_LOCAL_PREFIX = "http://127.0.0.1:9000/mentor-dual-selection-system/";
const THUMBNAIL_SUFFIX = ".thumb.jpg";
const LEGACY_DEFAULT_AVATAR_FILENAMES = ["defaultavater.webp", "defaultavatar.webp"];
const DEFAULT_CARD_BACKGROUND_FILENAME = "纪行．捕风.png";
const DEFAULT_CARD_BACKGROUND_URL =
  "https://minio.lumoxuan.cn/mentor-dual-selection-system/admin/%E7%BA%AA%E8%A1%8C%EF%BC%8E%E6%8D%95%E9%A3%8E.png";

function isLocalBrowserRuntime() {
  return typeof window !== "undefined" && ["127.0.0.1", "localhost"].includes(window.location.hostname);
}

function isManagedMinioUrl(url?: string | null): url is string {
  return typeof url === "string" && (url.startsWith(MINIO_PUBLIC_PREFIX) || url.startsWith(MINIO_LOCAL_PREFIX));
}

function rewriteMinioUrlForRuntime(url?: string | null) {
  if (!url) {
    return "";
  }

  if ((import.meta.env.DEV || isLocalBrowserRuntime()) && url.startsWith(MINIO_PUBLIC_PREFIX)) {
    return `${MINIO_LOCAL_PREFIX}${url.slice(MINIO_PUBLIC_PREFIX.length)}`;
  }

  return url;
}

function normalizeImageUrl(url?: string | null) {
  if (!url) {
    return "";
  }

  try {
    return new URL(url).toString();
  } catch {
    return url;
  }
}

function safeDecodeUrl(url: string) {
  try {
    return decodeURIComponent(url);
  } catch {
    return url;
  }
}

export function isMinioPublicUrl(url?: string | null): url is string {
  return isManagedMinioUrl(url);
}

export function isLikelyImageUrl(url?: string | null): url is string {
  if (!url) {
    return false;
  }

  const normalized = url.split("?")[0].toLowerCase();
  return [".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp"].some((suffix) => normalized.endsWith(suffix));
}

export function toThumbnailUrl(url?: string | null) {
  const rewrittenUrl = rewriteMinioUrlForRuntime(url);

  if (!rewrittenUrl || !isManagedMinioUrl(rewrittenUrl) || !isLikelyImageUrl(rewrittenUrl)) {
    return normalizeImageUrl(rewrittenUrl);
  }

  if (rewrittenUrl.includes(THUMBNAIL_SUFFIX)) {
    return normalizeImageUrl(rewrittenUrl);
  }

  const [base, query] = rewrittenUrl.split("?", 2);
  const thumbnailUrl = `${base}${THUMBNAIL_SUFFIX}`;
  return normalizeImageUrl(query ? `${thumbnailUrl}?${query}` : thumbnailUrl);
}

export function getDisplayImageUrl(url?: string | null, preferThumbnail = false) {
  if (!url) {
    return "";
  }

  const rewrittenUrl = rewriteMinioUrlForRuntime(url);
  return preferThumbnail ? toThumbnailUrl(rewrittenUrl) : normalizeImageUrl(rewrittenUrl);
}

export function isLegacyDefaultAvatarUrl(url?: string | null) {
  if (!url) {
    return false;
  }

  const normalized = url.split("?")[0].toLowerCase();
  return LEGACY_DEFAULT_AVATAR_FILENAMES.some((filename) => normalized.endsWith(`/${filename}`) || normalized.endsWith(filename));
}

export function isDefaultCardBackgroundUrl(url?: string | null) {
  if (!url) {
    return false;
  }

  const normalized = safeDecodeUrl(url.split("?")[0]).toLowerCase();
  return normalized.endsWith(`/admin/${DEFAULT_CARD_BACKGROUND_FILENAME}`.toLowerCase())
    || normalized.endsWith(DEFAULT_CARD_BACKGROUND_FILENAME.toLowerCase());
}

export function getPortraitImageCandidates(url?: string | null) {
  if (!url || isLegacyDefaultAvatarUrl(url) || isDefaultCardBackgroundUrl(url)) {
    return [] as string[];
  }

  const displayUrl = getDisplayImageUrl(url, false);
  const thumbnailUrl = toThumbnailUrl(url);
  return Array.from(new Set([thumbnailUrl, displayUrl].filter(Boolean)));
}

export function getPortraitImageUrl(url?: string | null) {
  return getPortraitImageCandidates(url)[0] ?? "";
}

export function getCardBackgroundImageUrl(url?: string | null) {
  if (!url || isLegacyDefaultAvatarUrl(url)) {
    return DEFAULT_CARD_BACKGROUND_URL;
  }

  if (url === DEFAULT_CARD_BACKGROUND_URL) {
    return url;
  }

  return getDisplayImageUrl(url, true);
}
