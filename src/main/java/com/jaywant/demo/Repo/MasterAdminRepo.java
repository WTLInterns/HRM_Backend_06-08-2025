package com.jaywant.demo.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jaywant.demo.Entity.MasterAdmin;

public interface MasterAdminRepo extends JpaRepository<MasterAdmin, Long> {

  MasterAdmin findByEmail(String email);

  MasterAdmin findFirstByRoll(String Roll);

}
