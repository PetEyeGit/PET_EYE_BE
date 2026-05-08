package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.CareLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CareLogRepository extends JpaRepository<CareLog, Integer> {
    List<CareLog> findByBookingIdOrderByTimestampDesc(int bookingId);
}
