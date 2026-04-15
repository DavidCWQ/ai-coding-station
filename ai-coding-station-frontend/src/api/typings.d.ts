declare namespace API {
  type AgentChatMessageVO = {
    id?: number;
    userId?: number;
    sessionId?: number;
    messageType?: string;
    message?: string;
    createTime?: string;
  };

  type AgentChatSessionVO = {
    id?: number;
    userId?: number;
    agentCode?: string;
    title?: string;
    lastMsgTime?: string;
    createTime?: string;
    updateTime?: string;
  };

  type AgentChatStreamRequest = {
    sessionId: number;
    agentCode: string;
    message: string;
  };

  type AgentHistoryQueryRequest = {
    agentCode: string;
    sessionId: number;
    beforeMessageId?: number;
    beforeCreateTime?: string;
    pageSize?: number;
  };

  type AgentSessionAddRequest = {
    agentCode: string;
    title?: string;
  };

  type AgentSessionQueryRequest = {
    agentCode: string;
    pageNum?: number;
    pageSize?: number;
  };

  type AgentSessionUpdateTitleRequest = {
    sessionId: number;
    title: string;
  };

  type AppAddRequest = {
    appName?: string;
    cover?: string;
    initPrompt?: string;
    codeGenType?: string;
  };

  type AppAdminUpdateRequest = {
    id: number;
    appName?: string;
    cover?: string;
    priority?: number;
    userId?: number;
  };

  type AppChatGenCodeRequest = {
    appId: number;
    sessionId: number;
    message: string;
  };

  type AppDeployRequest = {
    appId: number;
  };

  type AppReplaceImageRequest = {
    appId: number;
    newImagePath: string;
    targetXpath?: string;
    targetTag?: string;
    targetId?: string;
    targetClassList?: string[];
    targetTagPath?: string[];
  };

  type AppQueryRequest = {
    pageNum?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id: number;
    appName?: string;
    cover?: string;
    initPrompt?: string;
    codeGenType?: string;
    deployKey?: string;
    priority?: number;
    userId?: number;
  };

  type AppUpdateRequest = {
    id: number;
    appName?: string;
    codeGenType?: string;
  };

  type AppVO = {
    id?: number;
    appName?: string;
    cover?: string;
    initPrompt?: string;
    codeGenType?: string;
    deployKey?: string;
    deployedTime?: string;
    priority?: number;
    userId?: number;
    createTime?: string;
    updateTime?: string;
    user?: UserVO;
  };

  type BaseResponseAppVO = {
    code?: number;
    data?: AppVO;
    message?: string;
  };

  type BaseResponseBoolean = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseListAgentChatMessageVO = {
    code?: number;
    data?: AgentChatMessageVO[];
    message?: string;
  };

  type BaseResponseListChatHistoryVO = {
    code?: number;
    data?: ChatHistoryVO[];
    message?: string;
  };

  type BaseResponseLong = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponsePageAgentChatSessionVO = {
    code?: number;
    data?: PageAgentChatSessionVO;
    message?: string;
  };

  type BaseResponsePageAppVO = {
    code?: number;
    data?: PageAppVO;
    message?: string;
  };

  type BaseResponsePageChatSessionVO = {
    code?: number;
    data?: PageChatSessionVO;
    message?: string;
  };

  type BaseResponsePageUserVO = {
    code?: number;
    data?: PageUserVO;
    message?: string;
  };

  type BaseResponseString = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseSysUser = {
    code?: number;
    data?: SysUser;
    message?: string;
  };

  type BaseResponseUserLoginVO = {
    code?: number;
    data?: UserLoginVO;
    message?: string;
  };

  type BaseResponseUserVO = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type ChatHistoryAddRequest = {
    appId: number;
    sessionId: number;
    message: string;
    messageType: string;
  };

  type ChatHistoryQueryRequest = {
    appId: number;
    sessionId: number;
    beforeMessageId?: number;
    beforeCreateTime?: string;
    pageSize?: number;
  };

  type ChatHistoryVO = {
    id?: number;
    message?: string;
    messageType?: string;
    fileList?: string;
    appId?: number;
    sessionId?: number;
    parentId?: number;
    createTime?: string;
    updateTime?: string;
  };

  type ChatSessionAddRequest = {
    appId: number;
    title?: string;
  };

  type ChatSessionQueryRequest = {
    pageNum?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    appId: number;
  };

  type ChatSessionUpdateTitleRequest = {
    sessionId: number;
    title: string;
  };

  type ChatSessionVO = {
    id?: number;
    appId?: number;
    title?: string;
    lastMsgTime?: string;
    createTime?: string;
    updateTime?: string;
  };

  type chatToGenCode1Params = {
    appId: number;
    sessionId: number;
    message: string;
  };

  type DeleteRequest = {
    id?: number;
  };

  type downloadAppCodeParams = {
    appId: number;
  };

  type getAppParams = {
    id: number;
  };

  type getAppVOParams = {
    id: number;
  };

  type getUserParams = {
    id: number;
  };

  type getUserVOParams = {
    id: number;
  };

  type listAllHistoryParams = {
    pageSize?: number;
  };

  type PageAgentChatSessionVO = {
    records?: AgentChatSessionVO[];
    pageNumber?: number;
    pageSize?: number;
    totalPage?: number;
    totalRow?: number;
    optimizeCountQuery?: boolean;
  };

  type PageAppVO = {
    records?: AppVO[];
    pageNumber?: number;
    pageSize?: number;
    totalPage?: number;
    totalRow?: number;
    optimizeCountQuery?: boolean;
  };

  type PageChatSessionVO = {
    records?: ChatSessionVO[];
    pageNumber?: number;
    pageSize?: number;
    totalPage?: number;
    totalRow?: number;
    optimizeCountQuery?: boolean;
  };

  type PageUserVO = {
    records?: UserVO[];
    pageNumber?: number;
    pageSize?: number;
    totalPage?: number;
    totalRow?: number;
    optimizeCountQuery?: boolean;
  };

  type ServerSentEventString = true;

  type serveStaticResourceParams = {
    codeDir: string;
  };

  type SysUser = {
    id?: number;
    userAccount?: string;
    userPassword?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    editTime?: string;
    createTime?: string;
    updateTime?: string;
    isDeleted?: number;
    vipCode?: string;
    vipNumber?: number;
    vipExpireTime?: string;
  };

  type UserAddRequest = {
    userName?: string;
    userAccount?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserLoginRequest = {
    userAccount?: string;
    userPassword?: string;
  };

  type UserLoginVO = {
    id?: number;
    userAccount?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    createTime?: string;
    updateTime?: string;
  };

  /** Current session user (same shape as {@link UserLoginVO} from login / getLoginUser). */
  type LoginUserVO = UserLoginVO;

  type UserQueryRequest = {
    pageNum?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    userName?: string;
    userAccount?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserRegisterRequest = {
    userAccount?: string;
    userPassword?: string;
    checkPassword?: string;
  };

  type UserUpdateRequest = {
    id?: number;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserVO = {
    id?: number;
    userAccount?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    createTime?: string;
  };
}
