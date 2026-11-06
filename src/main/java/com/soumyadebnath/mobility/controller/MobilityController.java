package com.soumyadebnath.mobility.controller;

import com.soumyadebnath.mobility.model.ComplianceSummary;
import com.soumyadebnath.mobility.model.Employee;
import com.soumyadebnath.mobility.model.NextAction;
import com.soumyadebnath.mobility.model.Trip;
import com.soumyadebnath.mobility.service.ComplianceEngine;
import com.soumyadebnath.mobility.terminology.TerminologyMap;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MobilityController {

    private final ComplianceEngine engine;

    public MobilityController(ComplianceEngine engine) {
        this.engine = engine;
    }

    public record EmployeeRequest(@NotBlank String name, @NotBlank String originCountry, @NotBlank String email) {}
    public record TripRequest(@NotBlank String employeeId, @NotBlank String originCountry,
                              @NotBlank String destinationCountry, @NotBlank String purpose,
                              @NotNull LocalDate startDate, int durationDays, long annualSalaryCents) {}

    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(new Employee(UUID.randomUUID().toString(), req.name(), req.originCountry(), req.email()));
    }

    @PostMapping("/trips")
    public ResponseEntity<Trip> createTrip(@Valid @RequestBody TripRequest req) {
        return ResponseEntity.ok(new Trip(UUID.randomUUID().toString(), req.employeeId(), req.originCountry(),
            req.destinationCountry(), req.purpose(), req.startDate(), req.durationDays(), req.annualSalaryCents()));
    }

    @GetMapping("/trips/{tripId}/next-actions")
    public ResponseEntity<List<NextAction>> nextActions(@PathVariable String tripId,
                                                         @RequestParam(required = false) String origin,
                                                         @RequestParam(required = false) String destination,
                                                         @RequestParam(required = false) String purpose,
                                                         @RequestParam(required = false) String start,
                                                         @RequestParam(defaultValue = "14") int durationDays) {
        Trip trip = new Trip(tripId, "emp-1", origin == null ? "DE" : origin, destination == null ? "FR" : destination,
            purpose == null ? "Business trip" : purpose,
            start == null ? LocalDate.now().plusDays(10) : LocalDate.parse(start),
            durationDays, 9500000L);
        return ResponseEntity.ok(engine.assess(trip).nextActions());
    }

    @GetMapping("/trips/{tripId}/compliance-summary")
    public ResponseEntity<ComplianceSummary> complianceSummary(@PathVariable String tripId,
                                                                  @RequestParam(required = false) String origin,
                                                                  @RequestParam(required = false) String destination,
                                                                  @RequestParam(defaultValue = "14") int durationDays) {
        Trip trip = new Trip(tripId, "emp-1", origin == null ? "DE" : origin, destination == null ? "FR" : destination,
            "Business trip", LocalDate.now().plusDays(10), durationDays, 9500000L);
        return ResponseEntity.ok(engine.assess(trip));
    }

    @GetMapping("/terminology/{term}")
    public ResponseEntity<Map<String, String>> terminology(@PathVariable String term) {
        return ResponseEntity.ok(Map.of("internal", term, "workflex", TerminologyMap.toWorkflexTerm(term)));
    }

    // Public API-network compatible status endpoint (cf. Postman spec:
    // GET /status after OAuth2 client-credentials token).
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "cross-border-mobility-compliance"));
    }
}
