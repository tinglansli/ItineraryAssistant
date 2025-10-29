package com.tinglans.backend.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 开销类别枚举
 */
@Getter
public enum ExpenseCategory {
    TRANSPORT(0, "transport", "交通"),
    HOTEL(1, "hotel", "住宿"),
    SIGHT(2, "sight", "景点"),
    FOOD(3, "food", "餐饮"),
    OTHER(4, "other", "其他");

    private final int code;
    private final String value;
    private final String description;

    ExpenseCategory(int code, String value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    /**
     * 序列化为 JSON 时使用 value
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * 根据字符串值获取枚举
     */
    public static ExpenseCategory fromValue(String value) {
        for (ExpenseCategory category : ExpenseCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return OTHER;
    }

    /**
     * 根据代码获取枚举
     */
    public static ExpenseCategory fromCode(int code) {
        for (ExpenseCategory category : ExpenseCategory.values()) {
            if (category.code == code) {
                return category;
            }
        }
        return OTHER;
    }
}
