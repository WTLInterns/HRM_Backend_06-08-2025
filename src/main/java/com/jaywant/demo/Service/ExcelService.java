package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    public byte[] generateAttendanceExcel(List<Attendance> attendanceList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Data");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Full Name", "Date", "Status", "Punch In", "Lunch In", "Lunch Out", "Punch Out", "Work Duration"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIdx = 1;
            List<Map.Entry<Employee, List<Attendance>>> attendanceByEmployee = attendanceList.stream()
                    .collect(Collectors.groupingBy(Attendance::getEmployee))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey((e1, e2) -> Integer.compare(e1.getEmpId(), e2.getEmpId())))
                    .collect(Collectors.toList());

            for (Map.Entry<Employee, List<Attendance>> entry : attendanceByEmployee) {
                Employee employee = entry.getKey();
                List<Attendance> employeeAttendance = entry.getValue().stream()
                        .sorted((a1, a2) -> a1.getDate().compareTo(a2.getDate()))
                        .collect(Collectors.toList());

                for (Attendance attendance : employeeAttendance) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(employee.getEmpId());
                    row.createCell(1).setCellValue(employee.getFullName());
                    row.createCell(2).setCellValue(attendance.getDate());
                    row.createCell(3).setCellValue(attendance.getStatus());
                    row.createCell(4).setCellValue(attendance.getPunchInTime() != null ? attendance.getPunchInTime().toString() : "");
                    row.createCell(5).setCellValue(attendance.getLunchInTime() != null ? attendance.getLunchInTime().toString() : "");
                    row.createCell(6).setCellValue(attendance.getLunchOutTime() != null ? attendance.getLunchOutTime().toString() : "");
                    row.createCell(7).setCellValue(attendance.getPunchOutTime() != null ? attendance.getPunchOutTime().toString() : "");
                    row.createCell(8).setCellValue(attendance.getWorkDuration() != null ? attendance.getWorkDuration().toString() : "");
                }
                rowIdx += 2; // Add two blank rows between employees
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public ByteArrayInputStream generateAttendanceExcel(List<Employee> employees, String month) throws IOException {
        List<Attendance> attendanceList = employees.stream()
                .flatMap(employee -> employee.getAttendances().stream())
                .filter(a -> a.getDate().substring(0, 7).equals(month))
                .collect(Collectors.toList());
        byte[] excelData = generateAttendanceExcel(attendanceList);
        return new ByteArrayInputStream(excelData);
    }
}