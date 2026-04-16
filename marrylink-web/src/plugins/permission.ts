import {
  NavigationGuardNext,
  RouteLocationNormalized,
  RouteRecordRaw,
} from "vue-router";

import NProgress from "@/utils/nprogress";
import { TOKEN_KEY } from "@/enums/CacheEnum";
import router from "@/router";
import { usePermissionStore, useUserStore } from "@/store";

export function setupPermission() {
  // 白名单路由
  const whiteList = ["/login"];

  router.beforeEach(async (to, from, next) => {
    NProgress.start();
    const hasToken = localStorage.getItem(TOKEN_KEY);

    if (hasToken) {
      if (to.path === "/login") {
        // 如果已登录，跳转到首页
        next({ path: "/" });
        NProgress.done();
      } else {
        const userStore = useUserStore();
        const permissionStore = usePermissionStore();

        // 判断动态路由是否已加载（使用独立标志位，而非 roles）
        if (permissionStore.isRoutesLoaded) {
          // 已加载过动态路由
          if (to.matched.length === 0) {
            // 未匹配到路由，跳转404
            next(from.name ? { name: from.name } : "/404");
          } else {
            // 如果路由参数中有 title，覆盖路由元信息中的 title
            const title =
              (to.params.title as string) || (to.query.title as string);
            if (title) {
              to.meta.title = title;
            }
            next();
          }
        } else {
          try {
            // 1. 获取用户信息（含 roles、perms）
            //    登录后 Pinia 中已有 roles，无需再调 getUserInfo
            //    页面刷新后 Pinia 丢失，需要重新获取
            const hasRoles =
              userStore.user.roles && userStore.user.roles.length > 0;
            if (!hasRoles) {
              console.log("[Permission] Fetching user info...");
              await userStore.getUserInfo();
              console.log(
                "[Permission] User info fetched, roles:",
                userStore.user.roles
              );
            }

            // 2. 根据角色从后端获取动态路由菜单
            console.log("[Permission] Generating dynamic routes...");
            const dynamicRoutes = await permissionStore.generateRoutes();
            console.log(
              "[Permission] Dynamic routes generated:",
              dynamicRoutes.length
            );

            // 3. 动态注册路由
            dynamicRoutes.forEach((route: RouteRecordRaw) =>
              router.addRoute(route)
            );

            // 4. 重新导航到目标路由（确保新注册的路由生效）
            next({ ...to, replace: true });
          } catch (error) {
            console.error("[Permission] Error:", error);
            // 获取用户信息或路由失败，清除 token 并重定向到登录页
            await userStore.resetToken();
            redirectToLogin(to, next);
            NProgress.done();
          }
        }
      }
    } else {
      // 未登录
      if (whiteList.includes(to.path)) {
        next(); // 在白名单，直接进入
      } else {
        // 不在白名单，重定向到登录页
        redirectToLogin(to, next);
        NProgress.done();
      }
    }
  });

  router.afterEach(() => {
    NProgress.done();
  });
}

/** 重定向到登录页 */
function redirectToLogin(
  to: RouteLocationNormalized,
  next: NavigationGuardNext
) {
  const params = new URLSearchParams(to.query as Record<string, string>);
  const queryString = params.toString();
  const redirect = queryString ? `${to.path}?${queryString}` : to.path;
  next(`/login?redirect=${encodeURIComponent(redirect)}`);
}

/** 判断是否有权限 */
export function hasAuth(
  value: string | string[],
  type: "button" | "role" = "button"
) {
  const { roles, perms } = useUserStore().user;

  // 超级管理员 拥有所有权限
  if (type === "button" && roles.includes("ROOT")) {
    return true;
  }

  const auths = type === "button" ? perms : roles;
  return typeof value === "string"
    ? auths.includes(value)
    : value.some((perm) => auths.includes(perm));
}
