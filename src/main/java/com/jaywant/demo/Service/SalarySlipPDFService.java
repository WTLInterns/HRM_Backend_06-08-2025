package com.jaywant.demo.Service;

import com.jaywant.demo.DTO.SalaryDTO;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.SalarySlip;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SalarySlipRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SalarySlipPDFService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private SalarySlipRepository salarySlipRepository;

    @Autowired
    private SalaryService salaryService;

    @Autowired
    private EmployeeRepo employeeRepo;

    // Create directory if it doesn't exist
    private void createDirectoryIfNotExists(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    // Generate unique filename for PDF
    private String generateUniqueFileName(String employeeName, String month) {
        String sanitizedName = employeeName.replaceAll("\\s+", "_");
        return "salary_slip_" + sanitizedName + "_" + month + "_" + UUID.randomUUID().toString() + ".txt";
    }

    // Get employee by subadmin and employee ID
    private Employee getEmployee(int subadminId, int empId) {
        return employeeRepo.findBySubadminIdAndEmpId(subadminId, empId);
    }

    // Convert number to words (simplified version)
    private String numberToWords(double amount) {
        if (amount == 0) return "Zero Rupees Only";
        
        long num = Math.round(amount);
        if (num == 0) return "Zero Rupees Only";
        
        // Simplified conversion
        if (num < 1000) {
            return num + " Rupees Only";
        } else if (num < 100000) {
            long thousands = num / 1000;
            long remainder = num % 1000;
            return thousands + " Thousand " + (remainder > 0 ? remainder + " " : "") + "Rupees Only";
        } else {
            long lakhs = num / 100000;
            long remainder = num % 100000;
            long thousands = remainder / 1000;
            long units = remainder % 1000;
            
            String result = lakhs + " Lakh ";
            if (thousands > 0) result += thousands + " Thousand ";
            if (units > 0) result += units + " ";
            return result + "Rupees Only";
        }
    }

    // Generate salary slip content (as text for now, can be enhanced to PDF later)
    public String generateSalarySlipPDF(int subadminId, int empId, String startDate, String endDate) throws IOException {
        // Get employee
        Employee employee = getEmployee(subadminId, empId);
        if (employee == null) {
            throw new RuntimeException("Employee not found");
        }

        // Get salary data
        SalaryDTO salaryData = salaryService.generateSalaryReport(employee, startDate, endDate);
        
        if (salaryData == null) {
            throw new RuntimeException("Unable to generate salary data");
        }

        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        
        // Create pay slip month
        LocalDate startLocalDate = LocalDate.parse(startDate);
        String paySlipMonth = startLocalDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();
        
        // Create directory structure
        String salarySlipDir = uploadDir + "/salary-slips/" + subadminId + "/" + employeeName.replaceAll("\\s+", "_");
        createDirectoryIfNotExists(salarySlipDir);
        
        // Generate filename and path
        String fileName = generateUniqueFileName(employeeName, paySlipMonth.replace(" ", "_"));
        Path filePath = Paths.get(salarySlipDir, fileName);
        
        // Generate salary slip content
        StringBuilder content = new StringBuilder();
        content.append("=".repeat(80)).append("\n");
        content.append(String.format("%s\n", salaryData.getCompanyName() != null ? salaryData.getCompanyName().toUpperCase() : "COMPANY NAME"));
        content.append(String.format("%s\n", salaryData.getCompanyAddress() != null ? salaryData.getCompanyAddress() : "Company Address"));
        content.append("=".repeat(80)).append("\n");
        content.append(String.format("PAY SLIP FOR %s\n", paySlipMonth));
        content.append("=".repeat(80)).append("\n");
        
        // Employee Information
        content.append("EMPLOYEE INFORMATION\n");
        content.append("-".repeat(40)).append("\n");
        content.append(String.format("UID: %s\t\tDesignation: %s\n", 
            salaryData.getUid() != null ? salaryData.getUid() : "N/A",
            salaryData.getJobRole() != null ? salaryData.getJobRole() : "N/A"));
        content.append(String.format("Name: %s %s\t\tDepartment: %s\n",
            salaryData.getFirstName() != null ? salaryData.getFirstName() : "",
            salaryData.getLastName() != null ? salaryData.getLastName() : "",
            salaryData.getDepartment() != null ? salaryData.getDepartment() : "N/A"));
        content.append("\n");
        
        // Attendance and Bank Details
        content.append("EMPLOYEE ATTENDANCE\t\t\tBANK DETAILS\n");
        content.append("-".repeat(40)).append("\t").append("-".repeat(40)).append("\n");
        content.append(String.format("Working Days: %d\t\t\tBank Name: %s\n",
            salaryData.getWorkingDays(),
            salaryData.getBankName() != null ? salaryData.getBankName() : "N/A"));
        content.append(String.format("Leave Taken: %d\t\t\tIFSC Code: %s\n",
            salaryData.getLeaveTaken(),
            salaryData.getIfscCode() != null ? salaryData.getIfscCode() : "N/A"));
        content.append(String.format("Payable Days: %.1f\t\t\tBranch Name: %s\n",
            salaryData.getPayableDays(),
            salaryData.getBranchName() != null ? salaryData.getBranchName() : "N/A"));
        content.append(String.format("\t\t\t\t\tAccount No: %s\n",
            salaryData.getBankAccountNo() != null ? salaryData.getBankAccountNo() : "N/A"));
        content.append("\n");
        
        // Salary Calculations
        content.append("SALARY CALCULATIONS\n");
        content.append("-".repeat(80)).append("\n");
        content.append(String.format("Cost To Company - CTC\tRs. %,.0f\t\tDeductions\t\tRs. %,.0f\n",
            salaryData.getSalary(), salaryData.getTotalDeductions()));
        content.append(String.format("Basic\t\t\tRs. %,.0f\t\tProfessional Tax\tRs. %,.0f\n",
            salaryData.getBasic(), salaryData.getProfessionalTax()));
        content.append(String.format("House Rent Allowance\tRs. %,.0f\t\tTDS\t\t\tRs. %,.0f\n",
            salaryData.getHra(), salaryData.getTds()));
        content.append(String.format("DA Allowance\t\tRs. %,.0f\t\tPF\t\t\tRs. %,.0f\n",
            salaryData.getDaAllowance(), salaryData.getPf()));
        content.append(String.format("Special Allowance\tRs. %,.0f\t\tTotal Deductions\tRs. %,.0f\n",
            salaryData.getSpecialAllowance(), salaryData.getTotalDeductions()));
        content.append(String.format("Total Allowance\t\tRs. %,.0f\t\tIncentive Amount\tRs. 0\n",
            salaryData.getTotalAllowance()));
        content.append(String.format("Gross Salary\t\tRs. %,.0f\t\tBonus\t\t\tRs. %,.0f\n",
            salaryData.getGrossSalary(), salaryData.getBonus()));
        content.append(String.format("\t\t\t\t\tNet Payable Salary\tRs. %,.0f\n",
            salaryData.getNetPayable()));
        content.append("\n");
        
        // Amount in Words
        content.append(String.format("Amount in Words: %s\n", numberToWords(salaryData.getNetPayable())));
        content.append("\n");
        
        // Signature section
        content.append("Prepared By:\t\t\t\tApproved By:\n");
        content.append("\n\n\n");
        content.append("=".repeat(80)).append("\n");
        
        // Write to file
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content.toString());
        }
        
        // Return relative path
        return "/salary-slips/" + subadminId + "/" + employeeName.replaceAll("\\s+", "_") + "/" + fileName;
    }

    // Save salary slip record to database
    public SalarySlip saveSalarySlipRecord(Employee employee, Subadmin subadmin, String startDate, 
                                          String endDate, String pdfPath, SalaryDTO salaryData) {
        
        LocalDate startLocalDate = LocalDate.parse(startDate);
        String paySlipMonth = startLocalDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();
        
        // Check if salary slip already exists for this period
        Optional<SalarySlip> existingSlip = salarySlipRepository.findByEmployeeAndDateRange(employee, startDate, endDate);
        
        SalarySlip salarySlip;
        if (existingSlip.isPresent()) {
            salarySlip = existingSlip.get();
            salarySlip.setPdfPath(pdfPath); // Update with new PDF path
        } else {
            salarySlip = new SalarySlip(employee, subadmin, startDate, endDate, paySlipMonth, pdfPath);
        }
        
        // Set salary details
        salarySlip.setNetPayable(salaryData.getNetPayable());
        salarySlip.setGrossSalary(salaryData.getGrossSalary());
        salarySlip.setTotalDeductions(salaryData.getTotalDeductions());
        salarySlip.setWorkingDays(salaryData.getWorkingDays());
        salarySlip.setPayableDays(salaryData.getPayableDays());
        
        return salarySlipRepository.save(salarySlip);
    }

    // Get salary slips by employee
    public List<SalarySlip> getSalarySlipsByEmployee(Employee employee) {
        return salarySlipRepository.findByEmployee(employee);
    }

    // Get salary slips by subadmin
    public List<SalarySlip> getSalarySlipsBySubadmin(Subadmin subadmin) {
        return salarySlipRepository.findBySubadmin(subadmin);
    }
}
