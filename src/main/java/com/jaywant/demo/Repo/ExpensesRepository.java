package com.jaywant.demo.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jaywant.demo.Entity.Expenses;
import java.util.List;

@Repository
public interface ExpensesRepository extends JpaRepository<Expenses, Integer> {
  // no extra methods needed for basic CRUD

  List<Expenses> findBySubadminId(Integer subadminId);

}
