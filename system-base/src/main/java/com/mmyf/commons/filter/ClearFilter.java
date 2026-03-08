package com.mmyf.commons.filter;

import com.mmyf.commons.util.mybatis.DynamicUpdateUtils;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * @date 2024/04/15 11:28
 **/
@WebFilter(filterName = "clearFilter", urlPatterns = {"/*"})
public class ClearFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // 在这里清理所有和当前请求线程的状态有关的数据，例如：ThreadLocal
            DynamicUpdateUtils.clearUpdateKeys();
        }
    }

    @Override
    public void destroy() {

    }
}
