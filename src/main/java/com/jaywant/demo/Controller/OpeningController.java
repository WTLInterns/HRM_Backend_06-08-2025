package com.jaywant.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaywant.demo.Entity.Opening;
import com.jaywant.demo.Service.NotificationService;
import com.jaywant.demo.Service.OpeningService;

@RestController
@RequestMapping("/api/openings/{subadminid}")
public class OpeningController {
    private final OpeningService openingService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    public OpeningController(OpeningService openingService) {
        this.openingService = openingService;
    }

    // Get all openings for a subadmin
    @GetMapping
    public List<Opening> getOpeningsBySubadmin(@PathVariable("subadminid") int subadminId) {
        return openingService.getOpeningsBySubadminId(subadminId);
    }

    // Create Opening for a subadmin with FCM notification
    @PostMapping("/{subadmintoken}")
    public Opening createOpening(
            @PathVariable("subadminid") int subadminId,
            @PathVariable("subadmintoken") String subadminToken,
            @RequestBody Opening opening) {

        System.out.println("🔄 Creating job opening for subadmin ID: " + subadminId);
        System.out.println("📝 Opening data: " + opening.getRole() + " at " + opening.getLocation());

        Opening createdOpening = openingService.createOpeningForSubadmin(subadminId, opening);

        if (createdOpening != null) {
            System.out.println("✅ Job opening created with ID: " + createdOpening.getId());

            // Send notification to all employees of this subadmin
            // Backend will fetch all employee FCM tokens from database
            notificationService.sendJobOpeningNotification(subadminId, createdOpening, subadminToken);
        } else {
            System.err.println("❌ Failed to create job opening");
        }

        return createdOpening;
    }

    // Get Opening by ID for a subadmin
    @GetMapping("/{id}")
    public Opening getOpeningById(@PathVariable("subadminid") int subadminId, @PathVariable("id") int id) {
        return openingService.getOpeningByIdForSubadmin(subadminId, id);
    }

    // Update Opening for a subadmin
    @PutMapping("/{id}")
    public Opening updateOpening(@PathVariable("subadminid") int subadminId, @PathVariable("id") int id,
            @RequestBody Opening opening) {
        return openingService.updateOpeningForSubadmin(subadminId, id, opening);
    }

    // Delete Opening for a subadmin
    @DeleteMapping("/{id}")
    public void deleteOpening(@PathVariable("subadminid") int subadminId, @PathVariable("id") int id) {
        openingService.deleteOpeningForSubadmin(subadminId, id);
    }
}
