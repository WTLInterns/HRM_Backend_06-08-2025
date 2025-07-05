# 📋 HRM API Documentation - Salary Slips & Certificates

## 🚀 **ENHANCED SALARY SLIP API** - Now includes ALL PDF data!

The salary slip GET API has been enhanced to include **ALL** the data that appears in the PDF, including:

✅ **Company Information:**
- Company Name, Address, GST No, CIN No, Company URL
- Signature Image, Stamp Image, Company Logo paths

✅ **Complete Employee Details:**
- Full employee information including department, joining date
- Bank details (Bank Name, Account No, IFSC Code, Branch Name)

✅ **Comprehensive Salary Breakdown:**
- All allowances (Basic, HRA, DA, Special, Transport, Medical, Food)
- All deductions (Professional Tax, TDS, PF, ESI, Advance)
- Bonus, Incentive Amount, Net Payable
- Amount in Words conversion

✅ **Attendance Details:**
- Working Days, Payable Days, Leave Taken, Half Days
- Per Day Salary calculations

✅ **Additional Features:**
- Pay Slip Month formatting (e.g., "JUNE 2025")
- Generated timestamp
- All data needed for PDF generation

## 💰 Salary Slip APIs

### 1. Get Salary Slip for Specific Employee
**Endpoint:** `GET /api/employee/{subadminId}/{empId}/salary-slip`
**Parameters:** 
- `subadminId` (path): ID of the subadmin
- `empId` (path): ID of the employee
- `startDate` (query): Start date in format `yyyy-MM-dd`
- `endDate` (query): End date in format `yyyy-MM-dd`

**Example:**
```
GET /api/employee/2/558/salary-slip?startDate=2025-01-01&endDate=2025-01-31
```

**Response:** ✨ **ENHANCED WITH ALL PDF DATA**
```json
{
  "success": true,
  "employee": {
    "empId": 558,
    "fullName": "Saurabh Ganj",
    "email": "saurabh@example.com",
    "department": "Engineering (Development)",
    "jobRole": "Java Full Stack Developer"
  },
  "salarySlip": {
    "uid": "558",
    "firstName": "Saurabh",
    "lastName": "Ganj",
    "email": "saurabh@example.com",
    "jobRole": "Java Full Stack Developer",
    "department": "Engineering (Development)",
    "joiningDate": "2024-01-15",

    "companyName": "WTL PVT LTD",
    "companyAddress": "Office no 09,2nd floor , A wing City Vista , Fountain Road , Ashoka Nagar,Kharadi pune",
    "companyGstNo": "27AABCW1234F1Z5",
    "companyCinNo": "U72900MH2020PTC123456",
    "companyUrl": "https://wtlinterns.com",
    "signatureImage": "signature_wtl.jpg",
    "stampImage": "stamp_wtl.jpg",
    "companyLogo": "logo_wtl.jpg",

    "bankName": "Maharastra",
    "bankAccountNo": "42356789",
    "branchName": "Une",
    "ifscCode": "ABCF123456",

    "workingDays": 30,
    "payableDays": 24.0,
    "leaveTaken": 1,
    "presentDays": 24.0,
    "halfDay": 0,

    "basic": 5000.0,
    "hra": 2000.0,
    "daAllowance": 2650.0,
    "specialAllowance": 350.0,
    "totalAllowance": 5000.0,
    "grossSalary": 10000.0,

    "professionalTax": 200.0,
    "tds": 0.0,
    "pf": 300.0,
    "esi": 0.0,
    "advance": 0.0,
    "totalDeductions": 2500.0,

    "bonus": 0.0,
    "incentiveAmount": 0.0,
    "netPayable": 7500.0,
    "amountInWords": "Seven Thousand Five Hundred Rupees Only",

    "transportAllowance": 0.0,
    "medicalAllowance": 0.0,
    "foodAllowance": 0.0,
    "additionalPerks": null,

    "perDaySalary": 333.33,
    "salary": 120000.0
  },
  "period": {
    "startDate": "2025-06-01",
    "endDate": "2025-06-30"
  },
  "paySlipMonth": "JUNE 2025",
  "generatedAt": "2025-07-05T13:30:00.000+00:00"
}
```

### 2. Get All Salary Slips for Subadmin
**Endpoint:** `GET /api/employee/{subadminId}/salary-slips`
**Parameters:**
- `subadminId` (path): ID of the subadmin
- `startDate` (query): Start date in format `yyyy-MM-dd`
- `endDate` (query): End date in format `yyyy-MM-dd`

**Example:**
```
GET /api/employee/2/salary-slips?startDate=2025-01-01&endDate=2025-01-31
```

**Response:**
```json
{
  "success": true,
  "subadminId": 2,
  "period": {
    "startDate": "2025-01-01",
    "endDate": "2025-01-31"
  },
  "totalEmployees": 5,
  "processedSlips": 5,
  "salarySlips": [
    {
      "empId": 558,
      "fullName": "John Doe",
      "email": "john@example.com",
      "department": "IT",
      "jobRole": "Developer",
      "salarySlip": { /* salary details */ }
    }
    // ... more employees
  ]
}
```

### 3. Generate Salary Slip PDF ✨ **NEW!**
**Endpoint:** `POST /api/employee/{subadminId}/{empId}/salary-slip-pdf`
**Parameters:**
- `subadminId` (path): ID of the subadmin
- `empId` (path): ID of the employee
- `startDate` (query): Start date in format `yyyy-MM-dd`
- `endDate` (query): End date in format `yyyy-MM-dd`

**Example:**
```
POST /api/employee/2/558/salary-slip-pdf?startDate=2025-06-01&endDate=2025-06-30
```

**Response:**
```json
{
  "success": true,
  "message": "Salary slip PDF generated successfully",
  "employee": {
    "empId": 558,
    "fullName": "Saurabh Ganj",
    "email": "saurabh@example.com",
    "department": "Engineering (Development)",
    "jobRole": "Java Full Stack Developer"
  },
  "salarySlip": {
    "id": 1,
    "pdfPath": "/salary-slips/2/Saurabh_Ganj/salary_slip_Saurabh_Ganj_JUNE_2025_abc123.pdf",
    "paySlipMonth": "JUNE 2025",
    "netPayable": 7500.0,
    "grossSalary": 10000.0,
    "createdAt": "2025-07-05T13:30:00.000"
  },
  "period": {
    "startDate": "2025-06-01",
    "endDate": "2025-06-30"
  }
}
```

### 4. Get Salary Slip PDFs for Employee ✨ **NEW!**
**Endpoint:** `GET /api/employee/{subadminId}/{empId}/salary-slip-pdfs`
**Parameters:**
- `subadminId` (path): ID of the subadmin
- `empId` (path): ID of the employee

**Example:**
```
GET /api/employee/2/558/salary-slip-pdfs
```

**Response:**
```json
{
  "success": true,
  "employee": {
    "empId": 558,
    "fullName": "Saurabh Ganj",
    "email": "saurabh@example.com",
    "department": "Engineering (Development)",
    "jobRole": "Java Full Stack Developer"
  },
  "totalSalarySlips": 3,
  "salarySlips": [
    {
      "id": 1,
      "pdfPath": "/salary-slips/2/Saurabh_Ganj/salary_slip_Saurabh_Ganj_JUNE_2025_abc123.pdf",
      "paySlipMonth": "JUNE 2025",
      "startDate": "2025-06-01",
      "endDate": "2025-06-30",
      "netPayable": 7500.0,
      "grossSalary": 10000.0,
      "totalDeductions": 2500.0,
      "workingDays": 30,
      "payableDays": 24.0,
      "createdAt": "2025-07-05T13:30:00.000"
    }
    // ... more salary slips
  ]
}
```

### 5. Legacy Salary Report API (Company-based)
**Endpoint:** `GET /api/employee/company/{companyName}/employee/{empId}/attendance/report`
**Parameters:**
- `companyName` (path): Name of the company
- `empId` (path): ID of the employee
- `startDate` (query): Start date in format `yyyy-MM-dd`
- `endDate` (query): End date in format `yyyy-MM-dd`

## 📜 Certificate APIs

### 1. Get Certificates for Specific Employee
**Endpoint:** `GET /api/certificate/get/{subadminId}/{empId}`
**Parameters:**
- `subadminId` (path): ID of the subadmin
- `empId` (path): ID of the employee

**Example:**
```
GET /api/certificate/get/2/558
```

**Response:**
```json
{
  "success": true,
  "employee": {
    "empId": 558,
    "fullName": "John Doe",
    "email": "john@example.com",
    "department": "IT",
    "jobRole": "Developer"
  },
  "totalCertificates": 3,
  "certificates": [
    {
      "id": 1,
      "joiningLetterPath": "/certificates/2/John_Doe/joining_abc123.pdf",
      "appointmentLetterPath": "/certificates/2/John_Doe/appointment_def456.pdf",
      "experienceLetterPath": "/certificates/2/John_Doe/experience_ghi789.pdf",
      "createdAt": "2025-01-15T10:30:00"
    }
  ],
  "message": "No certificates found for this employee" // Only if empty
}
```

### 2. Get All Certificates for Subadmin
**Endpoint:** `GET /api/certificate/get/{subadminId}/all`
**Parameters:**
- `subadminId` (path): ID of the subadmin

**Example:**
```
GET /api/certificate/get/2/all
```

**Response:**
```json
{
  "success": true,
  "subadminId": 2,
  "totalEmployees": 5,
  "totalCertificates": 12,
  "employeeCertificates": [
    {
      "empId": 558,
      "fullName": "John Doe",
      "email": "john@example.com",
      "department": "IT",
      "jobRole": "Developer",
      "certificateCount": 3,
      "certificates": [ /* certificate objects */ ]
    }
    // ... more employees
  ]
}
```

### 3. Get Certificate Summary for Subadmin
**Endpoint:** `GET /api/certificate/summary/{subadminId}`
**Parameters:**
- `subadminId` (path): ID of the subadmin

**Example:**
```
GET /api/certificate/summary/2
```

**Response:**
```json
{
  "success": true,
  "subadminId": 2,
  "totalEmployees": 5,
  "employeesWithCertificates": 3,
  "totalCertificates": 12,
  "documentTypeCounts": {
    "joining": 3,
    "appointment": 3,
    "experience": 2,
    "relieving": 1,
    "performance": 2,
    "achievement": 1
  }
}
```

### 4. Send/Upload Certificate
**Endpoint:** `POST /api/certificate/send/{subadminId}/{empId}/{documentType}`
**Parameters:**
- `subadminId` (path): ID of the subadmin
- `empId` (path): ID of the employee
- `documentType` (path): Type of document (joining, appointment, experience, etc.)
- `file` (form-data): The certificate file to upload

**Supported Document Types:**
- `joining` - Joining Letter
- `appointment` - Appointment Letter
- `experience` - Experience Certificate
- `relieving` - Relieving Letter
- `termination` - Termination Letter
- `increment` - Increment Letter
- `agreement` - Employment Agreement
- `performance` - Performance Certificate
- `achievement` - Achievement Certificate
- `internship` - Internship Completion Certificate
- `postAppraisal` - Post Appraisal Certificate

### 5. Delete Certificate
**Endpoint:** `DELETE /api/certificate/delete/{subadminId}/{empId}/{certificateId}`
**Parameters:**
- `subadminId` (path): ID of the subadmin
- `empId` (path): ID of the employee
- `certificateId` (path): ID of the certificate to delete

## 🔧 Additional Configuration APIs

### Get Salary Configuration
**Endpoint:** `GET /api/salary-config/{subadminId}`
**Description:** Get salary configuration settings for a subadmin

### Update Salary Configuration
**Endpoint:** `POST /api/salary-config/{subadminId}`
**Description:** Create or update salary configuration for a subadmin

## 📝 Notes

1. **Authentication:** All APIs require proper authentication (implementation depends on your auth system)
2. **File Storage:** Certificate files are stored in `/src/main/resources/static/uploads/certificates/`
3. **Email Integration:** When uploading certificates, emails are automatically sent if employee has email
4. **Error Handling:** All APIs return proper HTTP status codes and error messages
5. **Date Format:** Always use `yyyy-MM-dd` format for dates
6. **File Types:** Certificates support PDF, DOC, DOCX, and image formats

## 🚀 Usage Examples

### Get salary slip for employee 558 under subadmin 2:
```bash
curl -X GET "http://localhost:8282/api/employee/2/558/salary-slip?startDate=2025-01-01&endDate=2025-01-31"
```

### Get all certificates for employee 558:
```bash
curl -X GET "http://localhost:8282/api/certificate/get/2/558"
```

### Upload experience certificate:
```bash
curl -X POST "http://localhost:8282/api/certificate/send/2/558/experience" \
  -F "file=@experience_certificate.pdf"
```

## 🔄 **BEFORE vs AFTER Enhancement**

### ❌ **BEFORE** (Limited Data):
The GET API only returned basic salary calculations without company details, signatures, or complete employee information.

### ✅ **AFTER** (Complete PDF Data):
The GET API now returns **EVERYTHING** that appears in the PDF:

| **Category** | **Fields Included** |
|--------------|-------------------|
| **Company Info** | Company Name, Address, GST No, CIN No, URL, Signature, Stamp, Logo |
| **Employee Details** | Full Name, Department, Job Role, Joining Date, UID |
| **Bank Details** | Bank Name, Account No, IFSC Code, Branch Name |
| **Salary Components** | Basic, HRA, DA, Special, Transport, Medical, Food Allowances |
| **Deductions** | Professional Tax, TDS, PF, ESI, Advance |
| **Attendance** | Working Days, Payable Days, Leave Taken, Half Days |
| **Calculations** | Gross Salary, Total Deductions, Net Payable, Amount in Words |
| **Additional** | Bonus, Incentive, Per Day Salary, Pay Slip Month |

### 🎯 **Perfect Match with PDF**
The API response now contains **exactly the same data** that appears in your PDF:
- "WTL PVT LTD Office no 09,2nd floor , A wing City Vista , Fountain Road , Ashoka Nagar,Kharadi pune"
- "PAY SLIP FOR JUNE 2025"
- "UID: 558 Designation: Java Full Stack Developer"
- "Name: Saurabh Ganj Department: Engineering (Development)"
- All salary calculations, bank details, and company information
- Signature and stamp image paths for PDF generation
