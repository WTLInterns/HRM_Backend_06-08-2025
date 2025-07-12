# Attendance Images Directory

This directory stores images uploaded for "work from field" attendance records.

## File Naming Convention
Images are automatically named using the following pattern:
```
attendance_{empId}_{date}_{timestamp}_{uuid}.{extension}
```

Example: `attendance_123_20250703_143022_a1b2c3d4.jpg`

## Supported Formats
- JPG/JPEG
- PNG
- GIF

## File Size Limit
Maximum file size: 5MB

## Security Notes
- Images are stored locally in the application resources
- File paths are stored in the database for reference
- Only employees with "work from field" work type can upload images
