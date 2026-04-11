package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.support.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class CurrentUserProvider {

    public Long getCurrentUserIdRequired(HttpServletRequest request) {
        Long a = getCurrentUserIdOrNull(request);
        if (a == null) {
            throw new BusinessException("请先登录");
        }
        return a;
    }

    public Long getCurrentUserIdOrNull(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Long a = b(request.getAttribute("userId"));
        if (a != null) {
            return a;
        }

        a = b(request.getAttribute("user_id"));
        if (a != null) {
            return a;
        }

        a = b(request.getAttribute("currentUserId"));
        if (a != null) {
            return a;
        }

        a = b(request.getAttribute("loginUserId"));
        if (a != null) {
            return a;
        }

        a = c(request.getAttribute("user"));
        if (a != null) {
            return a;
        }

        a = c(request.getAttribute("loginUser"));
        if (a != null) {
            return a;
        }

        a = c(request.getAttribute("currentUser"));
        if (a != null) {
            return a;
        }

        String d = request.getHeader("X-User-Id");
        a = b(d);
        if (a != null) {
            return a;
        }

        d = request.getHeader("x-user-id");
        a = b(d);
        if (a != null) {
            return a;
        }

        d = request.getParameter("userId");
        a = b(d);
        if (a != null) {
            return a;
        }

        d = request.getParameter("user_id");
        a = b(d);
        if (a != null) {
            return a;
        }

        HttpSession e = request.getSession(false);
        if (e != null) {
            a = b(e.getAttribute("userId"));
            if (a != null) {
                return a;
            }

            a = b(e.getAttribute("user_id"));
            if (a != null) {
                return a;
            }

            a = b(e.getAttribute("currentUserId"));
            if (a != null) {
                return a;
            }

            a = b(e.getAttribute("loginUserId"));
            if (a != null) {
                return a;
            }

            a = c(e.getAttribute("user"));
            if (a != null) {
                return a;
            }

            a = c(e.getAttribute("loginUser"));
            if (a != null) {
                return a;
            }

            a = c(e.getAttribute("currentUser"));
            if (a != null) {
                return a;
            }
        }

        return null;
    }

    public Long getCurrentUserId(HttpServletRequest request) {
        return getCurrentUserIdRequired(request);
    }

    private Long b(Object a) {
        if (a == null) {
            return null;
        }
        if (a instanceof Long) {
            return (Long) a;
        }
        if (a instanceof Integer) {
            return ((Integer) a).longValue();
        }
        if (a instanceof Number) {
            return ((Number) a).longValue();
        }
        if (a instanceof String) {
            String b = ((String) a).trim();
            if (b.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(b);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Long c(Object a) {
        if (a == null) {
            return null;
        }

        Long b = d(a, "getId");
        if (b != null) {
            return b;
        }

        b = d(a, "getUserId");
        if (b != null) {
            return b;
        }

        b = d(a, "getUid");
        if (b != null) {
            return b;
        }

        return null;
    }

    private Long d(Object a, String b) {
        try {
            Method c = a.getClass().getMethod(b);
            Object d = c.invoke(a);
            return this.b(d);
        } catch (Exception e) {
            return null;
        }
    }
}