package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Reminder;
import com.jaywant.demo.Service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "*")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @GetMapping("/{subadminId}")
    public ResponseEntity<List<Reminder>> getRemindersBySubadminId(@PathVariable Long subadminId) {
        List<Reminder> reminders = reminderService.getRemindersBySubadminId(subadminId);
        return ResponseEntity.ok(reminders);
    }

    @PostMapping
    public ResponseEntity<Reminder> createReminder(@RequestBody Reminder reminder) {
        Reminder newReminder = reminderService.createReminder(reminder);
        return ResponseEntity.ok(newReminder);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reminder> updateReminder(
            @PathVariable Long id,
            @RequestBody Reminder reminderDetails) {
        Reminder updatedReminder = reminderService.updateReminder(id, reminderDetails);
        return ResponseEntity.ok(updatedReminder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long id) {
        reminderService.deleteReminder(id);
        return ResponseEntity.ok().build();
    }
}
