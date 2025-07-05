package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Reminder;
import com.jaywant.demo.Repo.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReminderService {

  @Autowired
  private ReminderRepository reminderRepository;

  public List<Reminder> getRemindersBySubadminId(Long subadminId) {
    return reminderRepository.findBySubadminId(subadminId);
  }

  public Reminder createReminder(Reminder reminder) {
    return reminderRepository.save(reminder);
  }

  public Reminder updateReminder(Long id, Reminder reminderDetails) {
    Reminder reminder = reminderRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Reminder not found with id: " + id));

    reminder.setFunctionName(reminderDetails.getFunctionName());
    reminder.setReminderDate(reminderDetails.getReminderDate());

    return reminderRepository.save(reminder);
  }

  public void deleteReminder(Long id) {
    Reminder reminder = reminderRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Reminder not found with id: " + id));

    reminderRepository.delete(reminder);
  }
}
