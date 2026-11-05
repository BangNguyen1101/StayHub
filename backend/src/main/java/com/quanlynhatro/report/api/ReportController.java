package com.quanlynhatro.report.api;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.report.service.ReportAccessDeniedException;
import com.quanlynhatro.report.service.ReportService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {
    private final ReportService reports;
    private final AppUserRepository users;

    public ReportController(ReportService reports, AppUserRepository users) {
        this.reports = reports;
        this.users = users;
    }

    @GetMapping("/dashboard")
    public ReportDtos.Dashboard dashboard(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate asOf) {
        admin(authentication);
        return reports.dashboard(asOf);
    }

    @GetMapping("/revenue")
    public ReportDtos.ReportBundle revenue(
            Authentication authentication,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        admin(authentication);
        return reports.revenue(from, to);
    }

    @GetMapping("/revenue-by-room")
    public java.util.List<ReportDtos.RoomRevenue> revenueByRoom(
            Authentication authentication,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        admin(authentication);
        return reports.revenueByRoom(from, to);
    }

    @GetMapping("/invoiced")
    public ReportDtos.ReportBundle invoiced(
            Authentication authentication,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        admin(authentication);
        return reports.invoiced(from, to);
    }

    @GetMapping("/debts")
    public ReportDtos.Debt debts(
            Authentication authentication, @RequestParam(required = false) LocalDate asOf) {
        admin(authentication);
        return reports.debt(asOf);
    }

    @GetMapping("/invoice-items")
    public ReportDtos.ReportBundle invoiceItems(
            Authentication authentication,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        admin(authentication);
        return reports.invoiceItems(from, to);
    }

    @GetMapping("/rooms")
    public java.util.List<ReportDtos.Count> rooms(Authentication a) {
        admin(a);
        return reports.rooms();
    }

    @GetMapping("/contracts")
    public java.util.List<ReportDtos.Count> contracts(Authentication a) {
        admin(a);
        return reports.contracts();
    }

    @GetMapping("/maintenance")
    public java.util.List<ReportDtos.Count> maintenance(Authentication a) {
        admin(a);
        return reports.maintenance();
    }

    @GetMapping("/utilities")
    public ReportDtos.ReportBundle utilities(
            Authentication a, @RequestParam String fromPeriod, @RequestParam String toPeriod) {
        admin(a);
        return reports.utilities(fromPeriod, toPeriod);
    }

    private AppUserEntity admin(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ReportAccessDeniedException();
        }
        AppUserEntity user =
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                authentication.getName(), authentication.getName())
                        .orElseThrow(ReportAccessDeniedException::new);
        if (user.getRole() == null
                || !"ADMIN".equals(user.getRole().getCode())
                || !user.isActive()) {
            throw new ReportAccessDeniedException();
        }
        return user;
    }
}
