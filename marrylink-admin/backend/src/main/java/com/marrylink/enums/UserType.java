package com.marrylink.enums;

import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
public enum UserType {
    
    CUSTOMER("CUSTOMER", "新人"),
    HOST("HOST", "主持人"),
    ADMIN("ADMIN", "管理员");
    
    private final String code;
    private final String desc;
    
    UserType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static UserType fromCode(String code) {
        for (UserType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown user type: " + code);
    }
}