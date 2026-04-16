import request from "@/utils/request";

export function getMessages(status?: number) {
  return request({
    url: "/message/list",
    method: "get",
    params: { status }
  });
}

export function getUnreadCount() {
  return request({
    url: "/message/unread/count",
    method: "get",
  });
}

export function markAllRead() {
  return request({
    url: "/message/mark-read",
    method: "post",
  });
}