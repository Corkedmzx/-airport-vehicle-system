package com.airport.dto;

import com.airport.entity.SysUser;
import lombok.Data;

import java.util.List;

/**
 * 用户DTO，包含角色信息
 * 
 * @author Corkedmzx
 */
@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private java.time.LocalDateTime lastLoginTime;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private List<String> roles; // 角色代码列表

    /**
     * 从SysUser实体转换为UserDTO
     */
    public static UserDTO fromEntity(SysUser user, List<String> roles) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setLastLoginTime(user.getLastLoginTime());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        dto.setRoles(roles);
        return dto;
    }
}

