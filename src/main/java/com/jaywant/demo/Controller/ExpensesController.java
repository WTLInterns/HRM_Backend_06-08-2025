package com.jaywant.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jaywant.demo.Entity.Expenses;
import com.jaywant.demo.Service.ExpensesService;

@RestController
@RequestMapping("/api/expenses/{subadminId}")
public class ExpensesController {

  private final ExpensesService service;

  @Autowired
  public ExpensesController(ExpensesService service) {
    this.service = service;
  }

  /** Create one (with optional billImage). */
  @PostMapping(value = "/postData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Expenses> create(
      @PathVariable Integer subadminId,
      @ModelAttribute Expenses data,
      @RequestPart(value = "billImageFile", required = false) MultipartFile billImageFile) {

    Expenses saved = service.create(subadminId, data, billImageFile);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  /** Read all for this subadmin. */
  @GetMapping("/getAll")
  public List<Expenses> listAll(@PathVariable Integer subadminId) {
    return service.findAll(subadminId);
  }

  /** Read one by ID (only if belongs to this subadmin). */
  @GetMapping("/{id}")
  public ResponseEntity<Expenses> getOne(
      @PathVariable Integer subadminId,
      @PathVariable Integer id) {

    return service.findOne(subadminId, id)
        .map(exp -> ResponseEntity.ok(exp))
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  /** Update one (fields + optionally new image). */
  @PutMapping(value = "/update-expenses/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Expenses> update(
      @PathVariable Integer subadminId,
      @PathVariable Integer id,
      @ModelAttribute Expenses updates,
      @RequestPart(value = "billImageFile", required = false) MultipartFile billImageFile) {

    try {
      Expenses updated = service.update(subadminId, id, updates, billImageFile);
      return ResponseEntity.ok(updated);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }

  /** Delete one. */
  @DeleteMapping("delete-expenses/{id}")
  public ResponseEntity<String> delete(
      @PathVariable Integer subadminId,
      @PathVariable Integer id) {
    try {
      service.delete(subadminId, id);
      return ResponseEntity
          .ok("Expense record deleted successfully");
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }

}
