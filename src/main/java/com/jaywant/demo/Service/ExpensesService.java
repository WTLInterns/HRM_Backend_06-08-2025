package com.jaywant.demo.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jaywant.demo.Entity.Expenses;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.ExpensesRepository;
import com.jaywant.demo.Repo.SubAdminRepo;

@Service
public class ExpensesService {

  private final ExpensesRepository repo;
  private final SubAdminRepo subadminRepo;
  private final Path uploadDir = Paths.get("src/main/resources/static/images/profile/");

  @Autowired
  public ExpensesService(ExpensesRepository repo, SubAdminRepo subadminRepo) {
    this.repo = repo;
    this.subadminRepo = subadminRepo;
    try {
      Files.createDirectories(uploadDir);
    } catch (IOException e) {
      throw new RuntimeException("Could not create upload directory", e);
    }
  }

  public Expenses create(Integer subadminId, Expenses data, MultipartFile file) {
    Subadmin sa = subadminRepo.findById(subadminId)
        .orElseThrow(() -> new RuntimeException("Subadmin not found: " + subadminId));
    data.setSubadmin(sa);

    if (file != null && !file.isEmpty()) {
      String fname = System.currentTimeMillis() + "_" + file.getOriginalFilename();
      try {
        Files.copy(file.getInputStream(), uploadDir.resolve(fname), StandardCopyOption.REPLACE_EXISTING);
        data.setBillImage(fname);
      } catch (IOException e) {
        throw new RuntimeException("Failed to save bill image", e);
      }
    }
    return repo.save(data);
  }

  public List<Expenses> findAll(Integer subadminId) {
    return repo.findBySubadminId(subadminId);
  }

  public Optional<Expenses> findOne(Integer subadminId, Integer id) {
    return repo.findById(id)
        .filter(e -> e.getSubadmin() != null
            && Objects.equals(e.getSubadmin().getId(), subadminId));
  }

  public Expenses update(Integer subadminId, Integer id, Expenses updates, MultipartFile file) {
    Expenses existing = findOne(subadminId, id)
        .orElseThrow(() -> new RuntimeException("Expenses not found for this subadmin: " + id));

    existing.setDate(updates.getDate());
    existing.setBillNo(updates.getBillNo());
    existing.setAmount(updates.getAmount());
    existing.setReason(updates.getReason());
    existing.setTransactionId(updates.getTransactionId());

    if (file != null && !file.isEmpty()) {
      String fname = System.currentTimeMillis() + "_" + file.getOriginalFilename();
      try {
        Files.copy(file.getInputStream(), uploadDir.resolve(fname), StandardCopyOption.REPLACE_EXISTING);
        existing.setBillImage(fname);
      } catch (IOException e) {
        throw new RuntimeException("Failed to save bill image", e);
      }
    }

    return repo.save(existing);
  }

  public void delete(Integer subadminId, Integer id) {
    Expenses existing = findOne(subadminId, id)
        .orElseThrow(() -> new RuntimeException("Expenses not found for this subadmin: " + id));
    repo.delete(existing);
  }
}
