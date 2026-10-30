import { computed, onMounted, onUnmounted, ref } from "vue";

type DeviceType = "mobile" | "tablet" | "desktop";
type SidebarMode = "drawer" | "collapsed" | "fixed";

const MOBILE_MAX = 767;
const TABLET_MAX = 1023;

const viewportWidth = ref(typeof window !== "undefined" ? window.innerWidth : 1440);
const device = ref<DeviceType>("desktop");
const sidebarOpen = ref(true);

let initialized = false;
let activeConsumers = 0;

function resolveDevice(width: number): DeviceType {
  if (width <= MOBILE_MAX) {
    return "mobile";
  }

  if (width <= TABLET_MAX) {
    return "tablet";
  }

  return "desktop";
}

function applyViewport(width: number, forceReset = false) {
  const previousDevice = device.value;
  const nextDevice = resolveDevice(width);

  viewportWidth.value = width;
  device.value = nextDevice;

  if (forceReset || previousDevice !== nextDevice) {
    if (nextDevice === "desktop") {
      sidebarOpen.value = true;
      return;
    }

    if (forceReset || previousDevice === "desktop" || nextDevice === "mobile") {
      sidebarOpen.value = false;
    }
  }
}

function handleResize() {
  applyViewport(window.innerWidth);
}

function ensureInitialized() {
  if (initialized || typeof window === "undefined") {
    return;
  }

  initialized = true;
  applyViewport(window.innerWidth, true);
  window.addEventListener("resize", handleResize, { passive: true });
}

function cleanupListener() {
  if (!initialized || typeof window === "undefined" || activeConsumers > 0) {
    return;
  }

  window.removeEventListener("resize", handleResize);
  initialized = false;
}

export function useResponsiveLayout() {
  onMounted(() => {
    activeConsumers += 1;
    ensureInitialized();
  });

  onUnmounted(() => {
    activeConsumers = Math.max(0, activeConsumers - 1);
    cleanupListener();
  });

  const isMobile = computed(() => device.value === "mobile");
  const isTablet = computed(() => device.value === "tablet");
  const isDesktop = computed(() => device.value === "desktop");
  const sidebarMode = computed<SidebarMode>(() => {
    if (isDesktop.value) {
      return "fixed";
    }

    if (isTablet.value && !sidebarOpen.value) {
      return "collapsed";
    }

    return "drawer";
  });

  const contentOffset = computed(() => {
    if (isDesktop.value) {
      return 200;
    }

    if (isTablet.value) {
      return 72;
    }

    return 0;
  });

  function toggleSidebar() {
    if (isDesktop.value) {
      sidebarOpen.value = true;
      return;
    }

    sidebarOpen.value = !sidebarOpen.value;
  }

  function openSidebar() {
    if (!isDesktop.value) {
      sidebarOpen.value = true;
    }
  }

  function closeSidebar() {
    if (!isDesktop.value) {
      sidebarOpen.value = false;
    }
  }

  return {
    viewportWidth,
    device,
    isMobile,
    isTablet,
    isDesktop,
    sidebarMode,
    sidebarOpen,
    contentOffset,
    toggleSidebar,
    openSidebar,
    closeSidebar,
  };
}
