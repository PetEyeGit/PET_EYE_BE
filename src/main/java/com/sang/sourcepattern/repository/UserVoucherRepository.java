package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Integer> {
    List<UserVoucher> findByUserId(Integer userId);
    List<UserVoucher> findByUserIdAndIsUsedFalse(Integer userId);
}
