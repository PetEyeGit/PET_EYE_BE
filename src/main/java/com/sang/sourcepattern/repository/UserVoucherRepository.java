package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Integer> {
    List<UserVoucher> findByUserId(Integer userId);
    List<UserVoucher> findByUserIdAndIsUsedFalse(Integer userId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT DATE(v.createdAt), COUNT(v)
        FROM UserVoucher v
        WHERE YEAR(v.createdAt) = :year AND MONTH(v.createdAt) = :month
        GROUP BY DATE(v.createdAt)
    """)
    List<Object[]> voucherCountByDateRange(@org.springframework.data.repository.query.Param("year") int year, @org.springframework.data.repository.query.Param("month") int month);
}
