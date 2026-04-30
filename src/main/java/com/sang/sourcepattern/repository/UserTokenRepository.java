package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {
    Optional<UserToken> findByTokenAndType(String token, String type);
    void deleteByUserIdAndType(int userId, String type);
}
