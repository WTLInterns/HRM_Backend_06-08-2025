package com.jaywant.demo.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jaywant.demo.Entity.Subadmin;

public interface SubAdminRepo extends JpaRepository<Subadmin, Integer> {
  List<Subadmin> findByEmail(String email);

  List<Subadmin> findByRegistercompanyname(String registercompanyname);

  List<Subadmin> findByStatus(String status);

  List<Subadmin> findFirstByRoll(String roll);

}
