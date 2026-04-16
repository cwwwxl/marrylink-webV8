import AuthAPI, { type LoginData } from "@/api/auth";
import UserAPI, { type UserInfo } from "@/api/user";
import { resetRouter } from "@/router";
import { store } from "@/store";
import { usePermissionStore } from "@/store/modules/permission";
import { TOKEN_KEY } from "@/enums/CacheEnum";

export const useUserStore = defineStore("user", () => {
  const user = ref<UserInfo>({
    accountId: undefined,
    refId: undefined,
    userType: undefined,
    realName: undefined,
    phone: undefined,
    email: undefined,
    avatar: undefined,
    roles: [],
    perms: [],
  });

  /**
   * 登录
   *
   * @param {LoginData}
   * @returns
   */
  function login(loginData: LoginData) {
    return new Promise<void>((resolve, reject) => {
      AuthAPI.login(loginData)
        .then((data) => {
          const {
            tokenType,
            token,
            accountId,
            refId,
            userType,
            realName,
            phone,
            email,
            roles,
            permissions,
          } = data;
          localStorage.setItem(TOKEN_KEY, tokenType + " " + token); // Bearer eyJhbGciOiJIUzI1NiJ9.xxx.xxx
          // 保存完整的用户信息
          Object.assign(user.value, {
            accountId,
            refId,
            userType,
            realName,
            phone,
            email,
            roles: roles || [],
            perms: permissions || [],
          });
          resolve();
        })
        .catch((error) => {
          reject(error);
        });
    });
  }

  // 获取信息(用户昵称、头像、角色集合、权限集合)
  // 页面刷新时 Pinia 状态丢失，需要通过此接口重新获取
  function getUserInfo() {
    return new Promise<UserInfo>((resolve, reject) => {
      UserAPI.getInfo()
        .then((data) => {
          if (!data) {
            reject("Verification failed, please Login again.");
            return;
          }
          if (!data.roles || data.roles.length <= 0) {
            reject("getUserInfo: roles must be a non-null array!");
            return;
          }
          Object.assign(user.value, { ...data });
          resolve(data);
        })
        .catch((error) => {
          reject(error);
        });
    });
  }

  // user logout
  function logout() {
    return new Promise<void>((resolve, reject) => {
      AuthAPI.logout()
        .then(() => {
          localStorage.setItem(TOKEN_KEY, "");
          // 清空用户信息
          Object.assign(user.value, {
            roles: [],
            perms: [],
            accountId: undefined,
            refId: undefined,
            userType: undefined,
            realName: undefined,
            phone: undefined,
            email: undefined,
            avatar: undefined,
          });
          // 重置动态路由状态
          const permissionStore = usePermissionStore();
          permissionStore.resetRoutes();
          location.reload(); // 清空路由
          resolve();
        })
        .catch((error) => {
          reject(error);
        });
    });
  }

  // remove token
  function resetToken() {
    return new Promise<void>((resolve) => {
      localStorage.setItem(TOKEN_KEY, "");
      // 清空用户信息
      Object.assign(user.value, {
        roles: [],
        perms: [],
        accountId: undefined,
        refId: undefined,
        userType: undefined,
        realName: undefined,
        phone: undefined,
        email: undefined,
        avatar: undefined,
      });
      // 重置动态路由状态
      const permissionStore = usePermissionStore();
      permissionStore.resetRoutes();
      resetRouter();
      resolve();
    });
  }

  return {
    user,
    login,
    getUserInfo,
    logout,
    resetToken,
  };
});

/**
 * 用于在组件外部（如在Pinia Store 中）使用 Pinia 提供的 store 实例。
 * 官方文档解释了如何在组件外部使用 Pinia Store：
 * https://pinia.vuejs.org/core-concepts/outside-component-usage.html#using-a-store-outside-of-a-component
 */
export function useUserStoreHook() {
  return useUserStore(store);
}
