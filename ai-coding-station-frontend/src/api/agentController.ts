// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /agent/chat/stream */
export async function chatStream(
  body: API.AgentChatStreamRequest,
  options?: { [key: string]: any }
) {
  return request<API.ServerSentEventString[]>("/agent/chat/stream", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /agent/history/list */
export async function listHistory1(
  body: API.AgentHistoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAgentChatMessageVO>(
    "/agent/history/list",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /agent/session/create */
export async function createSession1(
  body: API.AgentSessionAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/agent/session/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /agent/session/delete */
export async function deleteSession1(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/agent/session/delete", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /agent/session/list */
export async function listSessions1(
  body: API.AgentSessionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAgentChatSessionVO>(
    "/agent/session/list",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /agent/session/update/title */
export async function updateSessionTitle1(
  body: API.AgentSessionUpdateTitleRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/agent/session/update/title", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 与业务命名对齐的别名（避免页面受 openapi 生成后缀影响） */
export {
  createSession1 as agentCreateSession,
  listSessions1 as agentListSessions,
  listHistory1 as agentListHistory,
  updateSessionTitle1 as agentUpdateSessionTitle,
  deleteSession1 as agentDeleteSession,
};
