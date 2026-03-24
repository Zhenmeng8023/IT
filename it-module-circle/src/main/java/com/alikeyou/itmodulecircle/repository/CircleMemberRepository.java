package com.alikeyou.itmodulecircle.repository;

import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CircleMemberRepository extends JpaRepository<CircleMember, Long> {

    /**
     * 根据圈子查询成员列表
     */
    @EntityGraph(attributePaths = {"user"})
    List<CircleMember> findByCircleOrderByJoinTimeDesc(Circle circle);

    /**
     * 查询用户在某个圈子的成员信息
     */
    Optional<CircleMember> findByCircleAndUser(Circle circle, UserInfo user);

    /**
     * 检查用户是否是圈子成员
     */
    boolean existsByCircleAndUser(Circle circle, UserInfo user);

    /**
     * 根据角色查询成员列表
     */
    @EntityGraph(attributePaths = {"user"})
    List<CircleMember> findByCircleAndRoleOrderByJoinTimeDesc(Circle circle, String role);

    /**
     * 统计圈子成员数
     */
    long countByCircle(Circle circle);

    /**
     * 统计所有圈子的总成员数
     */
    long count();

    /**
     * 统计活跃成员数（状态为 active）
     */
    long countByStatus(String status);

    /**
     * 统计指定圈子中特定状态的成员数
     */
    long countByCircleAndStatus(Circle circle, String status);


}
