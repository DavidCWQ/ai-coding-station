/* 注意`common`是无业务层，避免放入`UserService`架构污染，后续按业务域分包，按业务模块纵向拆 */
package com.cwq.project_aicodingstation.common.handler;

import com.cwq.project_aicodingstation.common.error.ErrorCode;
import com.cwq.project_aicodingstation.common.error.ErrorType;
import com.cwq.project_aicodingstation.common.exception.BaseException;
import com.cwq.project_aicodingstation.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public BaseResponse<?> handleBaseException(BaseException e) {
        if (e.getType() == ErrorType.SYSTEM) {
            log.error("System exception", e);
        } else {
            log.warn("Business exception: {}", e.getMessage());
        }
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return BaseResponse.error(
                ErrorCode.SYSTEM_ERROR.getCode(),
                ErrorCode.SYSTEM_ERROR.getMessage()
        );
    }
}
