package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Opening;
import com.jaywant.demo.Repo.OpeningRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Entity.Subadmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OpeningService {
    private final OpeningRepo openingRepo;
private final SubAdminRepo subAdminRepo;

    @Autowired
    public OpeningService(OpeningRepo openingRepo, SubAdminRepo subAdminRepo) {
        this.openingRepo = openingRepo;
        this.subAdminRepo = subAdminRepo;
    }

    public List<Opening> getOpeningsBySubadminId(int subadminId) {
        return openingRepo.findBySubadminId(subadminId);
    }

    // Subadmin-scoped CRUD
    public Opening createOpeningForSubadmin(int subadminId, Opening opening) {
        Subadmin subadmin = subAdminRepo.findById(subadminId).orElse(null);
        if (subadmin == null) return null;
        opening.setSubadmin(subadmin);
        return openingRepo.save(opening);
    }

    public Opening getOpeningByIdForSubadmin(int subadminId, int id) {
        return openingRepo.findById(id)
            .filter(opening -> opening.getSubadmin() != null && opening.getSubadmin().getId() == subadminId)
            .orElse(null);
    }

    public Opening updateOpeningForSubadmin(int subadminId, int id, Opening updatedOpening) {
        return openingRepo.findById(id).map(opening -> {
            if (opening.getSubadmin() == null || opening.getSubadmin().getId() != subadminId) return null;
            opening.setRole(updatedOpening.getRole());
            opening.setLocation(updatedOpening.getLocation());
            opening.setSiteMode(updatedOpening.getSiteMode());
            opening.setPositions(updatedOpening.getPositions());
            opening.setExprience(updatedOpening.getExprience());
            opening.setDescription(updatedOpening.getDescription());
            opening.setWorkType(updatedOpening.getWorkType());
            // subadmin stays the same
            return openingRepo.save(opening);
        }).orElse(null);
    }

    public void deleteOpeningForSubadmin(int subadminId, int id) {
        openingRepo.findById(id).ifPresent(opening -> {
            if (opening.getSubadmin() != null && opening.getSubadmin().getId() == subadminId) {
                openingRepo.deleteById(id);
            }
        });
    }
}
