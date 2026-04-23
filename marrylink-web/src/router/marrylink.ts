import type { RouteRecordRaw } from 'vue-router'

export const Layout = () => import('@/layout/index.vue')

// 婚礼智配路由
export const marrylinkRoutes: RouteRecordRaw[] = [
  {
    path: '/marrylink',
    component: Layout,
    redirect: '/marrylink/questionnaire',
    name: 'Marrylink',
    meta: {
      title: '婚礼智配',
      icon: 'homepage',
      alwaysShow: true
    },
    children: [
      {
        path: 'chat',
        name: 'ChatManage',
        component: () => import('@/views/marrylink/chat/index.vue'),
        meta: {
          title: '实时对话',
        }
      },
      {
        path: 'host',
        name: 'HostManage',
        component: () => import('@/views/marrylink/host/index.vue'),
        meta: {
          title: '主持人管理',
        }
      },
      {
        path: 'video',
        name: 'VideoManage',
        component: () => import('@/views/marrylink/video/index.vue'),
        meta: {
          title: '案例管理',
        }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/marrylink/user/index.vue'),
        meta: {
          title: '用户管理',
        }
      },
      {
        path: 'order',
        name: 'OrderManage',
        component: () => import('@/views/marrylink/order/index.vue'),
        meta: {
          title: '订单管理',
        }
      },
      {
        path: 'order-log',
        name: 'OrderLogManage',
        component: () => import('@/views/marrylink/order-log/index.vue'),
        meta: {
          title: '日志管理',
        }
      },
      {
        path: 'schedule',
        name: 'ScheduleManage',
        component: () => import('@/views/marrylink/schedule/index.vue'),
        meta: {
          title: '档期管理',
        }
      },
      {
        path: 'tag',
        name: 'TagManage',
        component: () => import('@/views/marrylink/tag/index.vue'),
        meta: {
          title: '标签管理',
        }
      },
      {
        path: 'questionnaire',
        name: 'QuestionnaireManage',
        component: () => import('@/views/marrylink/questionnaire/index.vue'),
        meta: {
          title: '问卷管理',
        }
      },
      {
        path: 'user-questionnaire',
        name: 'UserQuestionnaire',
        component: () => import('@/views/marrylink/user-questionnaire/index.vue'),
        meta: {
          title: '我的问卷',
        }
      },
      {
        path: 'user-questionnaire/fill',
        name: 'UserQuestionnaireFill',
        component: () => import('@/views/marrylink/user-questionnaire/fill.vue'),
        meta: {
          title: '填写问卷',
          hidden: true
        }
      },
      {
        path: 'settlement',
        name: 'SettlementManage',
        component: () => import('@/views/marrylink/settlement/index.vue'),
        meta: {
          title: '结算管理',
        }
      },
      {
        path: 'commission',
        name: 'CommissionManage',
        component: () => import('@/views/marrylink/commission/index.vue'),
        meta: {
          title: '佣金管理',
        }
      },
      {
        path: 'host-wallet',
        name: 'HostWalletManage',
        component: () => import('@/views/marrylink/host-wallet/index.vue'),
        meta: {
          title: '主持人钱包',
        }
      },
      {
        path: 'platform-settings',
        name: 'PlatformSettings',
        component: () => import('@/views/marrylink/platform-settings/index.vue'),
        meta: {
          title: '平台设置',
        }
      },
      {
        path: 'platform-finance',
        name: 'PlatformFinance',
        component: () => import('@/views/marrylink/platform-finance/index.vue'),
        meta: {
          title: '平台财务',
        }
      }
    ]
  }
]