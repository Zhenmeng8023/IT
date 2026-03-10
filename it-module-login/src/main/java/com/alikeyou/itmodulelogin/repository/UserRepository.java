package com.alikeyou.itmodulelogin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alikeyou.itmodulelogin.entity.LoginUser;

@Repository
public interface UserRepository extends JpaRepository<LoginUser, Long> {
    Optional<LoginUser> findByUsername(String username);
}