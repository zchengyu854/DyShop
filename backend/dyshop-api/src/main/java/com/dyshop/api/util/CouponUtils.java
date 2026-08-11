package com.dyshop.api.util;

import com.dyshop.common.entity.CouponTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 优惠券公共工具（ch11）。
 */
public final class CouponUtils {

    private CouponUtils() {
    }

    /**
     * 有效期计算（spec §5.6）：
     * FIXED → 复制 end_at（可空=长期）；AFTER_DAYS → receivedAt + valid_days（0=长期）。
     */
    public static LocalDateTime computeExpireAt(CouponTemplate tpl, LocalDateTime receivedAt) {
        if (tpl == null) {
            return null;
        }
        if ("FIXED".equals(tpl.getValidType())) {
            return tpl.getEndAt();
        }
        if ("AFTER_DAYS".equals(tpl.getValidType())) {
            Integer days = tpl.getValidDays();
            if (days != null && days > 0 && receivedAt != null) {
                return receivedAt.plusDays(days);
            }
        }
        return null;
    }

    /**
     * 解析 JSON 数组文本（"[1,2,3]"）为 Long 列表；空/非法返回空列表。
     */
    public static List<Long> parseLongs(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return List.of();
        }
        String trimmed = jsonArray.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of();
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String part : body.split(",")) {
            try {
                result.add(Long.valueOf(part.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法项
            }
        }
        return result;
    }

    /**
     * Long 列表序列化为 JSON 数组文本；空列表返回 null（落库为 NULL）。
     */
    public static String writeLongs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ids.get(i));
        }
        return sb.append(']').toString();
    }
}
