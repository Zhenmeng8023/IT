package com.alikeyou.itmoduleproject.repository;

import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoLiteRepository extends JpaRepository<UserInfoLite, Long> {

    List<UserInfoLite> findByIdIn(Collection<Long> ids);

    Optional<UserInfoLite> findByUsername(String username);

    @Query("""
            select u from ProjectUserInfoLite u
            where :keyword = ''
               or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(u.username, '')) like lower(concat('%', :keyword, '%'))
            order by
              case
                when lower(coalesce(u.nickname, '')) = lower(:keyword) then 0
                when lower(coalesce(u.username, '')) = lower(:keyword) then 1
                when lower(coalesce(u.nickname, '')) like lower(concat(:keyword, '%')) then 2
                when lower(coalesce(u.username, '')) like lower(concat(:keyword, '%')) then 3
                else 4
              end,
              u.id desc
            """)
    List<UserInfoLite> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
