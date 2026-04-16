import request from "@/utils/request";

class UserAPI {
  /**
   * 获取当前登录用户信息
   *
   * @returns 登录用户昵称、头像信息，包括角色和权限
   */
  static getInfo() {
    return request<any, UserInfo>({
      url: "/user/me",
      method: "get",
    });
  }

  /**
   * 获取主持人信息
   */
  static getHostInfo() {
    return request<any, any>({
      url: "/host/info",
      method: "get",
    });
  }
}

export default UserAPI;

/** 登录用户信息 */
export interface UserInfo {
  /** 用户ID */
  userId?: number;

  /** 账号ID */
  accountId?: number;

  /** 业务表ID */
  refId?: number;

  /** 用户类型 */
  userType?: string;

  /** 用户名 */
  username?: string;

  /** 真实姓名 */
  realName?: string;

  /** 昵称 */
  nickname?: string;

  /** 手机号 */
  phone?: string;

  /** 邮箱 */
  email?: string;

  /** 头像URL */
  avatar?: string;

  /** 角色 */
  roles: string[];

  /** 权限 */
  perms: string[];

  /** 主持人ID（如果是主持人角色） */
  hostId?: number;
}
