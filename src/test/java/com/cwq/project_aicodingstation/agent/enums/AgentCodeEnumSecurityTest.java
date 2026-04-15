package com.cwq.project_aicodingstation.agent.enums;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.exception.BusinessException;
import com.cwq.project_aicodingstation.user.vo.UserLoginVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentCodeEnumSecurityTest {

    @Test
    void requireValid_returnsEnumForKnownCode() {
        AgentCodeEnum e = AgentCodeEnum.requireValid("code_assistant");
        assertEquals(AgentCodeEnum.CODE_ASSISTANT, e);
    }

    @Test
    void requireValid_throwsForUnknownCode() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> AgentCodeEnum.requireValid("unknown_agent"));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void requireMayUse_allowsPublicAgentWithoutLogin() {
        assertDoesNotThrow(() -> AgentCodeEnum.requireMayUse(AgentCodeEnum.CODE_ASSISTANT, null));
    }

    @Test
    void requireMayUse_rejectsAdminOnlyAgentWithoutLogin() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> AgentCodeEnum.requireMayUse(AgentCodeEnum.INSPIRATION_ECHO, null));
        assertEquals(ErrorCode.NOT_LOGIN.getCode(), ex.getCode());
    }

    @Test
    void requireMayUse_rejectsNonAdminForAdminOnlyAgent() {
        UserLoginVO user = new UserLoginVO();
        user.setId(1L);
        user.setUserRole("user");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> AgentCodeEnum.requireMayUse(AgentCodeEnum.INSPIRATION_ECHO, user));
        assertEquals(ErrorCode.NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    void requireMayUse_allowsAdminForAdminOnlyAgent() {
        UserLoginVO admin = new UserLoginVO();
        admin.setId(2L);
        admin.setUserRole("admin");

        assertDoesNotThrow(() -> AgentCodeEnum.requireMayUse(AgentCodeEnum.INSPIRATION_ECHO, admin));
    }
}
