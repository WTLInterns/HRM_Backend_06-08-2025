package com.jaywant.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jaywant.demo.Entity.Resume;
import com.jaywant.demo.Service.NotificationService;
import com.jaywant.demo.Service.ResumeService;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/upload/{empId}/{usertoken}/{subadmintoken}")
    public ResponseEntity<?> uploadResume(
            @PathVariable int empId,
            @PathVariable String usertoken,
            @PathVariable String subadmintoken,
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobRole") String jobRole) {
        try {
            System.out.println("🔄 Resume upload started for employee ID: " + empId);
            System.out.println("📄 Job role: " + jobRole);
            System.out.println("📁 File: " + file.getOriginalFilename());

            Resume resume = resumeService.uploadResume(file, empId, jobRole);
            System.out.println("✅ Resume uploaded successfully with ID: " + resume.getResumeId());

            // Send notification to subadmin about new resume submission
            System.out.println("🔔 Sending notification to subadmin...");
            try {
                notificationService.sendResumeSubmissionNotification(empId, resume, usertoken, subadmintoken);
                System.out.println("✅ Notification service called successfully");
            } catch (Exception notificationError) {
                System.err.println("❌ Notification service failed: " + notificationError.getMessage());
                notificationError.printStackTrace();
                // Continue with resume upload even if notification fails
            }

            return ResponseEntity.ok(resume);
        } catch (Exception e) {
            System.err.println("❌ Resume upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error uploading resume: " + e.getMessage());
        }
    }

    @GetMapping("/employee/{empId}")
    public ResponseEntity<?> getEmployeeResumes(@PathVariable int empId) {
        try {
            List<Resume> resumes = resumeService.getEmployeeResumes(empId);
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching resumes: " + e.getMessage());
        }
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<?> getResumeById(@PathVariable int resumeId) {
        try {
            Resume resume = resumeService.getResumeById(resumeId);
            return ResponseEntity.ok(resume);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching resume: " + e.getMessage());
        }
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<?> deleteResume(@PathVariable int resumeId) {
        try {
            resumeService.deleteResume(resumeId);
            return ResponseEntity.ok().body("Resume deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting resume: " + e.getMessage());
        }
    }

}
