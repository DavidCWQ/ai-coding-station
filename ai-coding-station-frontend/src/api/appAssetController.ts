// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /app/assets/upload-image */
export async function uploadAppImage(
  appId: number,
  file: File,
  sessionId?: number,
  options?: { [key: string]: any }
) {
  const formData = new FormData();
  formData.append("appId", String(appId));
  if (sessionId != null) {
    formData.append("sessionId", String(sessionId));
  }
  formData.append("file", file);
  return request<API.BaseResponseString>("/app/assets/upload-image", {
    method: "POST",
    data: formData,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /app/assets/replace-image */
export async function replaceAppImage(
  body: API.AppReplaceImageRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/app/assets/replace-image", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
