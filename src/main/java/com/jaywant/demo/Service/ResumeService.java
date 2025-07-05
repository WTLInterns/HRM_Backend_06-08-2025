package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Resume;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Repo.ResumeRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResumeService {
    
    @Autowired
    private ResumeRepo resumeRepo;
    
    @Autowired
    private EmployeeRepo employeeRepo;

    private final String UPLOAD_DIR = "src/main/resources/static/uploads/resumes/";

    public Resume uploadResume(MultipartFile file, int empId, String jobRole) throws IOException {
        Employee employee = employeeRepo.findById(empId)
            .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file
        String fileName = empId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // Create resume entry
        Resume resume = new Resume();
        resume.setEmployee(employee);
        resume.setJobRole(jobRole);
        resume.setResumeFileName(fileName);
        resume.setResumeFilePath(filePath.toString());
        resume.setUploadDateTime(LocalDateTime.now());

        return resumeRepo.save(resume);
    }

    public List<Resume> getEmployeeResumes(int empId) {
        return resumeRepo.findByEmployeeEmpId(empId);
    }

    public Resume getResumeById(int resumeId) {
        return resumeRepo.findById(resumeId)
            .orElseThrow(() -> new RuntimeException("Resume not found"));
    }  

    public void deleteResume(int resumeId) throws IOException {
        Resume resume = resumeRepo.findById(resumeId)
            .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Delete the physical file
        Path filePath = Paths.get(resume.getResumeFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete the database entry
        resumeRepo.deleteById(resumeId);
    }

} 
