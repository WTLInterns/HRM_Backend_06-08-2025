package com.jaywant.demo.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jaywant.demo.Entity.Certificate;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;

@Repository
public interface CertificateRepo extends JpaRepository<Certificate, Long> {

  List<Certificate> findBySubadmin(Subadmin subadmin);

  List<Certificate> findByEmployee(Employee employee);

  Optional<Certificate> findByEmployeeAndSubadmin(Employee employee, Subadmin subadmin);

  @Query("SELECT c FROM Certificate c WHERE c.subadmin = :subadmin AND " +
      "(c.letterHeadPath IS NOT NULL OR c.appointmentLetterPath IS NOT NULL OR " +
      "c.joiningLetterPath IS NOT NULL OR c.agreementContractPath IS NOT NULL OR " +
      "c.incrementLetterPath IS NOT NULL OR c.experienceLetterPath IS NOT NULL OR " +
      "c.relievingLetterPath IS NOT NULL OR c.exitLetterPath IS NOT NULL OR " +
      "c.terminationLetterPath IS NOT NULL)")
  List<Certificate> findEmployeesWithLetters(@Param("subadmin") Subadmin subadmin);

  @Query("SELECT c FROM Certificate c WHERE c.subadmin = :subadmin AND " +
      "(c.internshipCompletionPath IS NOT NULL OR c.achievementCertificatePath IS NOT NULL OR " +
      "c.performanceCertificatePath IS NOT NULL OR c.postAppraisalPath IS NOT NULL)")
  List<Certificate> findEmployeesWithCertificates(@Param("subadmin") Subadmin subadmin);

  @Query("SELECT c FROM Certificate c WHERE c.subadmin = :subadmin AND " +
      "(:documentType = 'letterHead' AND c.letterHeadPath IS NOT NULL OR " +
      ":documentType = 'appointment' AND c.appointmentLetterPath IS NOT NULL OR " +
      ":documentType = 'joining' AND c.joiningLetterPath IS NOT NULL OR " +
      ":documentType = 'agreement' AND c.agreementContractPath IS NOT NULL OR " +
      ":documentType = 'increment' AND c.incrementLetterPath IS NOT NULL OR " +
      ":documentType = 'experience' AND c.experienceLetterPath IS NOT NULL OR " +
      ":documentType = 'relieving' AND c.relievingLetterPath IS NOT NULL OR " +
      ":documentType = 'exit' AND c.exitLetterPath IS NOT NULL OR " +
      ":documentType = 'termination' AND c.terminationLetterPath IS NOT NULL OR " +
      ":documentType = 'internship' AND c.internshipCompletionPath IS NOT NULL OR " +
      ":documentType = 'achievement' AND c.achievementCertificatePath IS NOT NULL OR " +
      ":documentType = 'performance' AND c.performanceCertificatePath IS NOT NULL OR " +
      ":documentType = 'postAppraisal' AND c.postAppraisalPath IS NOT NULL)")
  List<Certificate> findEmployeesByDocumentType(@Param("subadmin") Subadmin subadmin,
      @Param("documentType") String documentType);
}