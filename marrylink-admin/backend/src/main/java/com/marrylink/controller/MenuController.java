package com.marrylink.controller;

import com.marrylink.common.Result;
import com.marrylink.dto.RouteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单控制器
 * 根据当前登录用户的角色/用户类型，返回对应的前端路由菜单
 */
@Slf4j
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    /**
     * 获取当前用户的路由菜单
     *
     * 菜单分配矩阵：
     * | 菜单         | 管理员(ADMIN) | 主持人(HOST) |
     * |-------------|:---:|:---:|
     * | 控制台       | ✅  | ✅  |
     * | 用户管理     | ✅  | ❌  |
     * | 主持人管理   | ✅  | ❌  |
     * | 订单管理     | ✅  | ✅（仅自己的） |
     * | 日志管理     | ✅  | ❌  |
     * | 档期管理     | ❌  | ✅  |
     * | 标签管理     | ✅  | ❌  |
     * | 问卷管理     | ✅  | ❌  |
     * | 我的问卷     | ❌  | ✅  |
     */
    @GetMapping("/routes")
    public Result<List<RouteVO>> getRoutes(HttpServletRequest request) {
        // 从请求属性中获取用户信息（由 JwtAuthenticationFilter 注入）
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) request.getAttribute("roles");
        String userType = (String) request.getAttribute("userType");

        log.info("Get routes for userType: {}, roles: {}", userType, roles);

        // 判断用户角色
        boolean isAdmin = isAdminUser(userType, roles);
        boolean isHost = isHostUser(userType, roles);

        List<RouteVO> routes = new ArrayList<>();

        // 构建婚礼智配主菜单
        RouteVO marrylink = new RouteVO();
        marrylink.setPath("/marrylink");
        marrylink.setComponent("Layout");
        // 根据用户类型设置默认重定向：主持人->档期管理，管理员->问卷管理
        marrylink.setRedirect(isHost ? "/marrylink/schedule" : "/marrylink/questionnaire");
        marrylink.setName("Marrylink");

        RouteVO.Meta marrylinkMeta = new RouteVO.Meta();
        marrylinkMeta.setTitle("婚礼智配");
        marrylinkMeta.setIcon("homepage");
        marrylinkMeta.setHidden(false);
        marrylinkMeta.setAlwaysShow(true);
        marrylink.setMeta(marrylinkMeta);

        List<RouteVO> children = new ArrayList<>();

        // ========== 共享菜单（所有角色可见）==========


        // ========== 管理员(ADMIN)专属菜单 ==========
        if (isAdmin) {
            children.add(createRoute("dashboard", "marrylink/dashboard/index",
                    "MarrylinkDashboard", "控制台", "monitor"));
            children.add(createRoute("user", "marrylink/user/index",
                    "UserManage", "用户管理", "user"));
            children.add(createRoute("host", "marrylink/host/index",
                    "HostManage", "主持人管理", "user"));
            children.add(createRoute("order", "marrylink/order/index",
                    "OrderManage", "订单管理", "document"));
            children.add(createRoute("order-log", "marrylink/order-log/index",
                    "OrderLogManage", "日志管理", "document"));
            children.add(createRoute("tag", "marrylink/tag/index",
                    "TagManage", "标签管理", "setting"));
            children.add(createRoute("questionnaire", "marrylink/questionnaire/index",
                    "QuestionnaireManage", "问卷管理", "document"));
        }

        // ========== 主持人(HOST)专属菜单 ==========
        if (isHost) {
            children.add(createRoute("schedule", "marrylink/schedule/index",
                    "ScheduleManage", "我的档期", "calendar"));
            children.add(createRoute("order", "marrylink/order/index",
                    "OrderManage", "我的订单", "document"));
            children.add(createRoute("questionnaire", "marrylink/questionnaire/index",
                    "QuestionnaireManage", "问卷管理", "document"));
        }

        marrylink.setChildren(children);
        routes.add(marrylink);

        log.info("Returning {} menu items for userType: {}", children.size(), userType);
        return Result.ok(routes);
    }

    /**
     * 判断是否为管理员用户
     */
    private boolean isAdminUser(String userType, List<String> roles) {
        if ("ADMIN".equals(userType)) {
            return true;
        }
        if (roles != null) {
            return roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");
        }
        return false;
    }

    /**
     * 判断是否为主持人用户
     */
    private boolean isHostUser(String userType, List<String> roles) {
        if ("HOST".equals(userType)) {
            return true;
        }
        if (roles != null) {
            return roles.contains("ROLE_HOST") || roles.contains("HOST");
        }
        return false;
    }

    /**
     * 创建路由对象
     *
     * @param path      路由路径
     * @param component 组件路径（相对于 views 目录）
     * @param name      路由名称
     * @param title     菜单标题
     * @param icon      菜单图标
     * @return RouteVO
     */
    private RouteVO createRoute(String path, String component, String name, String title, String icon) {
        RouteVO route = new RouteVO();
        route.setPath(path);
        route.setComponent(component);
        route.setName(name);

        RouteVO.Meta meta = new RouteVO.Meta();
        meta.setTitle(title);
        meta.setIcon(icon);
        meta.setHidden(false);
        meta.setAlwaysShow(false);
        route.setMeta(meta);

        return route;
    }
}
