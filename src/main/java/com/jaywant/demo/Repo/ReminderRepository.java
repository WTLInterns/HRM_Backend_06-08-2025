package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findBySubadminId(Long subadminId);
}
