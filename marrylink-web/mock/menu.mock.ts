import { defineMock } from "./base";

export default defineMock([
  {
    url: "menus/routes",
    method: ["GET"],
    body: {
      code: "00000",
      data: [
        {
          path: "/marrylink",
          component: "Layout",
          redirect: "/marrylink/dashboard",
          name: "Marrylink",
          meta: {
            title: "婚礼智配",
            icon: "homepage",
            hidden: false,
            alwaysShow: true,
            params: null,
          },
          children: [
            {
              path: "dashboard",
              component: "marrylink/dashboard/index",
              name: "MarrylinkDashboard",
              meta: {
                title: "控制台",
                icon: "monitor",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
            {
              path: "host",
              component: "marrylink/host/index",
              name: "HostManage",
              meta: {
                title: "主持人管理",
                icon: "user",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
            {
              path: "user",
              component: "marrylink/user/index",
              name: "UserManage",
              meta: {
                title: "用户管理",
                icon: "user",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
            {
              path: "order",
              component: "marrylink/order/index",
              name: "OrderManage",
              meta: {
                title: "订单管理",
                icon: "document",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
            {
              path: 'schedule',
              name: 'ScheduleManage',
              component: "marrylink/schedule/index",
              meta: {
                title: '档期管理',
                icon: 'document',
                hidden: false,
                alwaysShow: false,
                params: null,
              }
            },
            {
              path: "tag",
              component: "marrylink/tag/index",
              name: "TagManage",
              meta: {
                title: "标签管理",
                icon: "menu",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
            {
              path: "questionnaire",
              component: "marrylink/questionnaire/index",
              name: "QuestionnaireManage",
              meta: {
                title: "问卷管理",
                icon: "document",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
            {
              path: 'user-questionnaire',
              name: 'UserQuestionnaire',
              component: "marrylink/user-questionnaire/index",
              meta: {
                title: '我的问卷',
                icon: "document",
                hidden: false,
                alwaysShow: false,
                params: null,
              }
            },
            {
              path: 'user-questionnaire/fill',
              name: 'UserQuestionnaireFill',
              component: "marrylink/user-questionnaire/fill",
              meta: {
                title: '填写问卷',
                icon: 'edit',
                hidden: true,
                alwaysShow: false,
                params: null,
              }
            }
          ],
        },
        {
          path: "/doc",
          component: "Layout",
          redirect: "https://juejin.cn/post/7228990409909108793",
          name: "/doc",
          meta: {
            title: "平台文档",
            icon: "document",
            hidden: false,
            alwaysShow: false,
            params: null,
          },
          children: [
            {
              path: "internal-doc",
              component: "demo/internal-doc",
              name: "InternalDoc",
              meta: {
                title: "平台文档(内嵌)",
                icon: "document",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
            {
              path: "https://juejin.cn/post/7228990409909108793",
              name: "Https://juejin.cn/post/7228990409909108793",
              meta: {
                title: "平台文档(外链)",
                icon: "el-icon-Link",
                hidden: false,
                alwaysShow: false,
                params: null,
              },
            },
          ],
        },
        {
          path: "/multi-level",
          component: "Layout",
          name: "/multiLevel",
          meta: {
            title: "多级菜单",
            icon: "cascader",
            hidden: false,
            alwaysShow: true,
            params: null,
          },
          children: [
            {
              path: "multi-level1",
              component: "demo/multi-level/level1",
              name: "MultiLevel1",
              meta: {
                title: "菜单一级",
                icon: "",
                hidden: false,
                alwaysShow: true,
                params: null,
              },
              children: [
                {
                  path: "multi-level2",
                  component: "demo/multi-level/children/level2",
                  name: "MultiLevel2",
                  meta: {
                    title: "菜单二级",
                    icon: "",
                    hidden: false,
                    alwaysShow: false,
                    params: null,
                  },
                  children: [
                    {
                      path: "multi-level3-1",
                      component: "demo/multi-level/children/children/level3-1",
                      name: "MultiLevel31",
                      meta: {
                        title: "菜单三级-1",
                        icon: "",
                        hidden: false,
                        keepAlive: true,
                        alwaysShow: false,
                        params: null,
                      },
                    },
                    {
                      path: "multi-level3-2",
                      component: "demo/multi-level/children/children/level3-2",
                      name: "MultiLevel32",
                      meta: {
                        title: "菜单三级-2",
                        icon: "",
                        hidden: false,
                        keepAlive: true,
                        alwaysShow: false,
                        params: null,
                      },
                    },
                  ],
                },
              ],
            },
          ],
        },
      ],
      msg: "一切ok",
    },
  },
]);
