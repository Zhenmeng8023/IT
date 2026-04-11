package com.alikeyou.itmodulelogin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    @Value("${app.security.session.cookie-name:ITSESSIONID}")
    private String cookieName;

    @Value("${app.security.session.cookie-secure:false}")
    private boolean secure;

    @Value("${app.security.session.same-site:Lax}")
    private String sameSite;

    @Value("${app.security.session.cookie-max-age-seconds:604800}")
    private int cookieMaxAgeSeconds;

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookieName);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite(sameSite);
        serializer.setCookiePath("/");
        if (cookieMaxAgeSeconds > 0) {
            serializer.setCookieMaxAge(cookieMaxAgeSeconds);
        }
        return serializer;
    }
}
