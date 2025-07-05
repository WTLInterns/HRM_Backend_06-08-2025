package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.LocationHistory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    public static String[] HEADERS = {
            "Employee ID",
            "Employee Name",
            "Date",
            "Time",
            "Latitude",
            "Longitude",
            "Location Address",
            "Coordinates"
    };
    public static String SHEET = "Employee Location History (Last 12 Hours)";

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ByteArrayInputStream locationsToExcel(List<LocationHistory> locations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            Sheet sheet = workbook.createSheet(SHEET);

            // Create styles for professional formatting
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle coordinateStyle = createCoordinateStyle(workbook);

            // Create title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("📍 Employee Location Tracking Report - Last 12 Hours");
            CellStyle titleStyle = createTitleStyle(workbook);
            titleCell.setCellStyle(titleStyle);

            // Merge title cells
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // Create header row
            Row headerRow = sheet.createRow(2);
            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 3;
            for (LocationHistory loc : locations) {
                Row row = sheet.createRow(rowIdx++);

                // Employee ID
                Cell empIdCell = row.createCell(0);
                empIdCell.setCellValue(loc.getEmployee().getEmpId());
                empIdCell.setCellStyle(dataStyle);

                // Employee Name
                Cell nameCell = row.createCell(1);
                nameCell.setCellValue(loc.getEmployee().getFullName());
                nameCell.setCellStyle(dataStyle);

                // Date
                Cell dateCell = row.createCell(2);
                dateCell.setCellValue(loc.getTimestamp().format(dateFormatter));
                dateCell.setCellStyle(dataStyle);

                // Time
                Cell timeCell = row.createCell(3);
                timeCell.setCellValue(loc.getTimestamp().format(timeFormatter));
                timeCell.setCellStyle(dataStyle);

                // Latitude
                Cell latCell = row.createCell(4);
                latCell.setCellValue(loc.getLatitude());
                latCell.setCellStyle(coordinateStyle);

                // Longitude
                Cell lngCell = row.createCell(5);
                lngCell.setCellValue(loc.getLongitude());
                lngCell.setCellStyle(coordinateStyle);

                // Address (ensure it's not null or empty)
                Cell addressCell = row.createCell(6);
                String address = loc.getAddress();
                if (address == null || address.trim().isEmpty()) {
                    address = "Address not available";
                }
                addressCell.setCellValue(address);
                addressCell.setCellStyle(dataStyle);

                // Coordinates (formatted)
                Cell coordCell = row.createCell(7);
                coordCell.setCellValue(loc.getLatitude() + ", " + loc.getLongitude());
                coordCell.setCellStyle(coordinateStyle);
            }

            // Set column widths for professional appearance
            setColumnWidths(sheet);

            // Auto-filter for data
            if (!locations.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(2, rowIdx - 1, 0, HEADERS.length - 1));
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCoordinateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Courier New"); // Monospace font for coordinates
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private void setColumnWidths(Sheet sheet) {
        // Set optimal column widths for professional appearance
        sheet.setColumnWidth(0, 3000); // Employee ID
        sheet.setColumnWidth(1, 6000); // Employee Name
        sheet.setColumnWidth(2, 3500); // Date
        sheet.setColumnWidth(3, 3000); // Time
        sheet.setColumnWidth(4, 4000); // Latitude
        sheet.setColumnWidth(5, 4000); // Longitude
        sheet.setColumnWidth(6, 12000); // Location Address (wider for full addresses)
        sheet.setColumnWidth(7, 6000); // Coordinates
    }
}