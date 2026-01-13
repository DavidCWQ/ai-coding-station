package com.cwq.project_aicodingstation.user.service;

import com.cwq.project_aicodingstation.common.request.DeleteRequest;
import com.cwq.project_aicodingstation.user.dto.UserAddRequest;
import com.cwq.project_aicodingstation.user.dto.UserQueryRequest;
import com.cwq.project_aicodingstation.user.dto.UserUpdateRequest;
import com.cwq.project_aicodingstation.user.entity.SysUser;
import com.cwq.project_aicodingstation.user.vo.UserVO;
import com.mybatisflex.core.paginate.Page;

public interface AdminUserService {

    Long save(UserAddRequest request);

    boolean remove(DeleteRequest request);

    boolean update(UserUpdateRequest request);

    SysUser getById(Long id);

    UserVO getVOById(Long id);

    Page<UserVO> listUserVOByPage(UserQueryRequest request);
}

