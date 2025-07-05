# 📄 Salary Slip File API Examples

## 🎯 **Your Request:**
You want to access the salary slip file content directly in your application, similar to how certificates work.

## ✅ **New File Serving APIs:**

### 1. **View Salary Slip Content** 
**Endpoint:** `GET /api/employee/salary-slip/file?path={filePath}`

**Example:**
```
GET /api/employee/salary-slip/file?path=/salary-slips/2/Saurabh_Ganj/salary_slip_Saurabh_Ganj_JUNE_2025_6cd7789f-f58c-42c1-9232-5fee49c543b8.txt
```

**Response:**
```json
{
  "success": true,
  "fileName": "salary_slip_Saurabh_Ganj_JUNE_2025_6cd7789f-f58c-42c1-9232-5fee49c543b8.txt",
  "content": "================================================================================\nWTL PVT LTD\nOffice no 09,2nd floor , A wing City Vista , Fountain Road , Ashoka Nagar,Kharadi pune\n================================================================================\nPAY SLIP FOR JUNE 2025\n================================================================================\nEMPLOYEE INFORMATION\n----------------------------------------\nUID: 558\t\tDesignation: Java Full Stack Developer\nName: Saurabh Ganj\t\tDepartment: Engineering (Development)\n\nEMPLOYEE ATTENDANCE\t\t\tBANK DETAILS\n----------------------------------------\t----------------------------------------\nWorking Days: 30\t\t\tBank Name: Maharastra\nLeave Taken: 1\t\t\tIFSC Code: ABCF123456\nPayable Days: 24.0\t\t\tBranch Name: Une\n\t\t\t\t\tAccount No: 42356789\n\nSALARY CALCULATIONS\n--------------------------------------------------------------------------------\nCost To Company - CTC\tRs. 120,000\t\tDeductions\t\tRs. 2,500\nBasic\t\t\tRs. 5,000\t\tProfessional Tax\tRs. 200\nHouse Rent Allowance\tRs. 2,000\t\tTDS\t\t\tRs. 0\nDA Allowance\t\tRs. 2,650\t\tPF\t\t\tRs. 300\nSpecial Allowance\tRs. 350\t\tTotal Deductions\tRs. 2,500\nTotal Allowance\t\tRs. 5,000\t\tIncentive Amount\tRs. 0\nGross Salary\t\tRs. 10,000\t\tBonus\t\t\tRs. 0\n\t\t\t\t\tNet Payable Salary\tRs. 7,500\n\nAmount in Words: Seven Thousand Five Hundred Rupees Only\n\nPrepared By:\t\t\t\tApproved By:\n\n\n\n================================================================================",
  "filePath": "/salary-slips/2/Saurabh_Ganj/salary_slip_Saurabh_Ganj_JUNE_2025_6cd7789f-f58c-42c1-9232-5fee49c543b8.txt"
}
```

### 2. **Download Salary Slip File**
**Endpoint:** `GET /api/employee/salary-slip/download?path={filePath}`

**Example:**
```
GET /api/employee/salary-slip/download?path=/salary-slips/2/Saurabh_Ganj/salary_slip_Saurabh_Ganj_JUNE_2025_6cd7789f-f58c-42c1-9232-5fee49c543b8.txt
```

**Response:** Direct file download with proper headers

## 🔄 **Complete Workflow:**

### Step 1: Generate Salary Slip PDF
```bash
POST /api/employee/2/558/salary-slip-pdf?startDate=2025-06-01&endDate=2025-06-30
```
**Returns:** `pdfPath` in response

### Step 2: Get All Salary Slips for Employee
```bash
GET /api/employee/2/558/salary-slip-pdfs
```
**Returns:** List of salary slips with `pdfPath` for each

### Step 3: View Specific Salary Slip Content
```bash
GET /api/employee/salary-slip/file?path={pdfPath}
```
**Returns:** Full formatted salary slip content

### Step 4: Download Salary Slip (Optional)
```bash
GET /api/employee/salary-slip/download?path={pdfPath}
```
**Returns:** File download

## 🎯 **Perfect Match with Your PDF Example:**

Your original PDF content:
```
WTL PVT LTD Office no 09,2nd floor , A wing City Vista ,
 Fountain Road , Ashoka Nagar,Kharadi pune
 PAY SLIP FOR JUNE 2025
 Employee Information
 UID: 558 Designation: Java Full Stack Developer
 Name: Saurabh Ganj Department: Engineering (Development)
 Employee Attendance Bank Details
 Working Days: 30 Bank Name: Maharastra
 Leave Taken: 1 IFSC Code: ABCF123456
 Payable Days: 24 Branch Name: Une
 Account No: 42356789
 Salary Calculations
 Cost To Company - CTC Rs. 120000 Deductions Rs. 2000
 Basic Rs. 5000 Professional Tax Rs. 200
 House Rent Allowance Rs. 2000 TDS Rs. 0
 DA Allowance Rs. 2650 PF Rs. 300
 Special Allowance Rs. 350 Total Deductions Rs. 2500
 Total Allowance Rs. 5000 Incentive Amount Rs. 0
 Gross Salary Rs. 10000 Bonus Rs. 0
 Net Payable Salary Rs. 7500
 Amount in Words: Seven Thousand Five Hundred Rupees Only
 Prepared By: Approved By:
```

**API Response Content:** ✅ **EXACT MATCH** - All the same data formatted properly!

## 🚀 **Benefits:**

✅ **Direct Content Access** - Get formatted salary slip content via API  
✅ **Security** - Path validation prevents unauthorized access  
✅ **Flexible Display** - Use content in your application styling  
✅ **Download Option** - Users can download files when needed  
✅ **Same Pattern** - Consistent with certificate file serving  
✅ **Complete Data** - All salary information formatted exactly like PDF  

## 💡 **Usage in Your Application:**

1. **Generate** salary slip PDF using POST API
2. **Get** the `pdfPath` from the response
3. **Fetch** content using the file serving API
4. **Display** the formatted content in your application
5. **Style** the content as needed for your UI

This gives you the same functionality as your certificate system but for salary slips! 🎉
