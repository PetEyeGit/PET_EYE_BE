package com.sang.sourcepattern.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.sang.sourcepattern.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
<<<<<<< HEAD

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
=======
    Optional<User> findByFacebookId(String facebookId);
    Optional<User> findByZaloId(String zaloId);
    Optional<User> findByGoogleId(String googleId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b.user FROM Booking b WHERE b.shop.id = :shopId")
    List<User> findUsersByShopId(int shopId);
>>>>>>> 2bce3625c4c9432a8ab2789a1318ecb0783728e3
}
