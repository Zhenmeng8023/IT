/**
 * 分页结果封装类
 * 用于封装分页查询的结果
 * @param <T> 数据类型
 */
package com.alikeyou.itmoduleproject.vo;

import lombok.Data;
import java.util.List;

/**
 * 分页结果封装类
 * 用于封装分页查询的结果
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> {
    /**
     * 数据列表
     */
    private List<T> list;
    /**
     * 总记录数
     */
    private long total;
    /**
     * 当前页码
     */
    private int page;
    /**
     * 每页大小
     */
    private int size;

    /**
     * 构造分页结果
     * @param list 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页大小
     */
    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}