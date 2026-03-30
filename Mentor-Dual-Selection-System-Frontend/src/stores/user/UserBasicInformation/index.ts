import { defineStore } from "pinia";
import { http } from "@/utils/http";

const createEmptyUserInfo = () => ({
  uid: -1 as undefined | number,
  role: null as null | string,
  avatarUrl: null as null | string,
  fullName: null as null | string,
  email: null as null | string,
  username: null as null | string,
});

export const useUserInfoStore = defineStore("userInfoStore", {
  state: () => ({
    userInfo: createEmptyUserInfo(),
    isAuthenticated: false,
  }),
  actions: {
    clearSession() {
      localStorage.removeItem("token");
      this.userInfo = createEmptyUserInfo();
      this.isAuthenticated = false;
    },
    async fetchUserInfo() {
      const token = localStorage.getItem("token");
      if (!token) {
        this.clearSession();
        return false;
      }

      try {
        const res = await http({
          url: "/user/me",
          method: "GET",
          headers: {
            Accept: "*/*",
            Authorization: `Bearer ${token}`,
          },
        });

        if (res.data.code === 200) {
          this.userInfo = res.data.data;
          this.isAuthenticated = true;
          return true;
        }

        this.clearSession();
        return false;
      } catch (err) {
        console.error(err);
        this.clearSession();
        return false;
      }
    },
  },
});
