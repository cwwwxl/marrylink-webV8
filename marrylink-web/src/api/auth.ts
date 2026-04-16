import request from "@/utils/request";

class AuthAPI {
  /** 登录 接口*/
  static login(data: LoginData) {
    const formData = new FormData();
    formData.append("username", data.username);
    formData.append("password", data.password);
    formData.append("userType", data.userType || "ADMIN");
    return request<any, LoginResult>({
      url: "/auth/admin/login",
      method: "post",
      data: formData,
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  }

  /** 注销 接口*/
  static logout() {
    return request({
      url: "/auth/logout",
      method: "post",
    });
  }

  /** 修改密码 接口*/
  static changePassword(data: ChangePasswordData) {
    return request({
      url: "/auth/change-password",
      method: "post",
      data,
    });
  }
}

export default AuthAPI;

/** 登录请求参数 */
export interface LoginData {
  /** 用户名 */
  username: string;
  /** 密码 */
  password: string;
  /** 用户类型 */
  userType?: string;
}

/** 登录响应 */
export interface LoginResult {
  /** 访问token */
  token?: string;
  /** token 类型 */
  tokenType?: string;
  /** 账号ID */
  accountId?: number;
  /** 业务表ID */
  refId?: number;
  /** 用户类型 */
  userType?: string;
  /** 真实姓名 */
  realName?: string;
  /** 手机号 */
  phone?: string;
  /** 邮箱 */
  email?: string;
  /** 角色列表 */
  roles?: string[];
  /** 权限列表 */
  permissions?: string[];
}

/** 修改密码请求参数 */
export interface ChangePasswordData {
  /** 旧密码 */
  oldPassword: string;
  /** 新密码 */
  newPassword: string;
  /** 确认新密码 */
  confirmPassword: string;
}
