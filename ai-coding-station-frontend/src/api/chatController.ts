// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /chat/history/add */
export async function addMessage(
  body: API.ChatHistoryAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/chat/history/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /chat/history/admin/list */
export async function listAllHistory(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listAllHistoryParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListChatHistoryVO>(
    "/chat/history/admin/list",
    {
      method: "GET",
      params: {
        // pageSize has a default value: 10
        pageSize: "10",
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /chat/history/list */
export async function listHistory(
  body: API.ChatHistoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListChatHistoryVO>("/chat/history/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /chat/session/create */
export async function createSession(
  body: API.ChatSessionAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/chat/session/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /chat/session/delete */
export async function deleteSession(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/chat/session/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /chat/session/list */
export async function listSessions(
  body: API.ChatSessionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatSessionVO>("/chat/session/list", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /chat/session/update/title */
export async function updateSessionTitle(
  body: API.ChatSessionUpdateTitleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/chat/session/update/title", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
