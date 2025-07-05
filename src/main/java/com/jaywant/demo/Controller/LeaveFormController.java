package com.jaywant.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaywant.demo.Entity.LeaveForm;
import com.jaywant.demo.Service.LeaveFormService;

@RestController
@RequestMapping("/api/leaveform")
public class LeaveFormController {

    @Autowired
    private LeaveFormService leaveFormService;

    /** Create a new leave form for an employee */
    @PostMapping("/{subadminId}/{empId}/{usertoken}/{subadmintoken}")
    public ResponseEntity<LeaveForm> createLeave(
            @PathVariable int subadminId,
            @PathVariable int empId,
            @PathVariable String usertoken,
            @PathVariable String subadmintoken,
            @RequestBody LeaveForm leaveForm) {

        LeaveForm created = leaveFormService
                .createLeaveForm(subadminId, empId, leaveForm, usertoken, subadmintoken);
        return ResponseEntity.ok(created);
    }

    /** Retrieve all leave forms for an employee under a subadmin */
    @GetMapping("/{subadminId}/{empId}")
    public ResponseEntity<List<LeaveForm>> getLeavesByEmployee(
            @PathVariable int subadminId,
            @PathVariable int empId) {

        List<LeaveForm> leaves = leaveFormService
                .getBySubadminAndEmployeeId(subadminId, empId);
        return ResponseEntity.ok(leaves);
    }

    /** Retrieve a specific leave form by leaveId */
    @GetMapping("/{subadminId}/{empId}/{leaveId}")
    public ResponseEntity<LeaveForm> getLeave(
            @PathVariable int subadminId,
            @PathVariable int empId,
            @PathVariable int leaveId) {

        LeaveForm leave = leaveFormService
                .getLeaveFormById(subadminId, empId, leaveId);
        return ResponseEntity.ok(leave);
    }

    /** Update a specific leave form */
    @PutMapping("/{subadminId}/{empId}/{leaveId}/{usertoken}/{subadmintoken}")
    public ResponseEntity<LeaveForm> updateLeave(
            @PathVariable int subadminId,
            @PathVariable int empId,
            @PathVariable int leaveId,
            @PathVariable String usertoken,
            @PathVariable String subadmintoken,
            @RequestBody LeaveForm leaveForm) {

        LeaveForm updated = leaveFormService
                .updateLeaveForm(subadminId, empId, leaveId, leaveForm, usertoken, subadmintoken);
        return ResponseEntity.ok(updated);
    }

    /** Delete a specific leave form */
    @DeleteMapping("/{subadminId}/{empId}/{leaveId}")
    public ResponseEntity<Void> deleteLeave(
            @PathVariable int subadminId,
            @PathVariable int empId,
            @PathVariable int leaveId) {

        leaveFormService.deleteLeaveForm(subadminId, empId, leaveId);
        return ResponseEntity.noContent().build();
    }

    /** Retrieve all leave forms for all employees under a specific subadmin */
    @GetMapping("/{subadminId}/all")
    public ResponseEntity<List<LeaveForm>> getLeavesBySubadmin(
            @PathVariable int subadminId) {
        List<LeaveForm> all = leaveFormService.getAllLeavesBySubadmin(subadminId);
        return ResponseEntity.ok(all);
    }
}
