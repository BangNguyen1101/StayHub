package com.quanlynhatro.auth.api;

import com.quanlynhatro.billing.service.BillingBusinessException;
import com.quanlynhatro.billing.service.InvoiceAccessDeniedException;
import com.quanlynhatro.billing.service.InvoiceNotFoundException;
import com.quanlynhatro.billing.service.PaymentConflictException;
import com.quanlynhatro.billing.service.PaymentNotFoundException;
import com.quanlynhatro.contract.service.ContractAccessDeniedException;
import com.quanlynhatro.contract.service.ContractBusinessException;
import com.quanlynhatro.contract.service.ContractNotFoundException;
import com.quanlynhatro.maintenance.service.MaintenanceAccessDeniedException;
import com.quanlynhatro.maintenance.service.MaintenanceBusinessException;
import com.quanlynhatro.maintenance.service.MaintenanceConflictException;
import com.quanlynhatro.maintenance.service.MaintenanceNotFoundException;
import com.quanlynhatro.notification.service.NotificationAccessDeniedException;
import com.quanlynhatro.notification.service.NotificationBusinessException;
import com.quanlynhatro.notification.service.NotificationConflictException;
import com.quanlynhatro.notification.service.NotificationNotFoundException;
import com.quanlynhatro.rental.service.RentalRequestAccessDeniedException;
import com.quanlynhatro.rental.service.RentalRequestBusinessException;
import com.quanlynhatro.rental.service.RentalRequestNotFoundException;
import com.quanlynhatro.report.service.ReportAccessDeniedException;
import com.quanlynhatro.report.service.ReportBusinessException;
import com.quanlynhatro.room.service.DuplicateRoomCodeException;
import com.quanlynhatro.room.service.RoomNotFoundException;
import com.quanlynhatro.room.service.RoomStatusChangeNotAllowedException;
import com.quanlynhatro.tenant.service.DuplicateTenantEmailException;
import com.quanlynhatro.tenant.service.TenantAccountConflictException;
import com.quanlynhatro.tenant.service.TenantDeletionConflictException;
import com.quanlynhatro.tenant.service.TenantNotFoundException;
import com.quanlynhatro.tenant.service.TenantProfileNotFoundException;
import com.quanlynhatro.utility.service.DuplicateServiceCodeException;
import com.quanlynhatro.utility.service.DuplicateUtilityReadingException;
import com.quanlynhatro.utility.service.FeeConfigNotFoundException;
import com.quanlynhatro.utility.service.FeeConfigPeriodOverlapException;
import com.quanlynhatro.utility.service.InvalidFeeConfigPeriodException;
import com.quanlynhatro.utility.service.InvalidFeePriceException;
import com.quanlynhatro.utility.service.InvalidUtilityReadingException;
import com.quanlynhatro.utility.service.LockedUtilityReadingException;
import com.quanlynhatro.utility.service.RoomForUtilityNotFoundException;
import com.quanlynhatro.utility.service.ServiceNotFoundException;
import com.quanlynhatro.utility.service.UtilityBusinessException;
import com.quanlynhatro.utility.service.UtilityReadingNotFoundException;
import com.quanlynhatro.utility.service.UtilityReadingSequenceException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<AuthDtos.ErrorResponse> authenticationFailed() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthDtos.ErrorResponse("AUTH_FAILED", "Invalid credentials"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<AuthDtos.ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<AuthDtos.ErrorResponse> validation() {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("VALIDATION_FAILED", "Invalid request"));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<AuthDtos.ErrorResponse> methodValidation() {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("VALIDATION_FAILED", "Invalid request"));
    }

    @ExceptionHandler(RoomNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> roomNotFound(RoomNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("ROOM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateRoomCodeException.class)
    ResponseEntity<AuthDtos.ErrorResponse> duplicateRoomCode(DuplicateRoomCodeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("ROOM_CODE_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(RoomStatusChangeNotAllowedException.class)
    ResponseEntity<AuthDtos.ErrorResponse> invalidRoomStatus(
            RoomStatusChangeNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        new AuthDtos.ErrorResponse(
                                "ROOM_STATUS_CHANGE_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(RentalRequestNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> rentalRequestNotFound(
            RentalRequestNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("RENTAL_REQUEST_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RentalRequestAccessDeniedException.class)
    ResponseEntity<AuthDtos.ErrorResponse> rentalRequestAccessDenied(
            RentalRequestAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthDtos.ErrorResponse("RENTAL_REQUEST_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(RentalRequestBusinessException.class)
    ResponseEntity<AuthDtos.ErrorResponse> rentalRequestConflict(
            RentalRequestBusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("RENTAL_REQUEST_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> tenantNotFound(TenantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("TENANT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(TenantProfileNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> tenantProfileNotFound(
            TenantProfileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("TENANT_PROFILE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateTenantEmailException.class)
    ResponseEntity<AuthDtos.ErrorResponse> duplicateTenantEmail(DuplicateTenantEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("TENANT_EMAIL_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(TenantAccountConflictException.class)
    ResponseEntity<AuthDtos.ErrorResponse> tenantAccountConflict(
            TenantAccountConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("TENANT_ACCOUNT_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(TenantDeletionConflictException.class)
    ResponseEntity<AuthDtos.ErrorResponse> tenantDeletionConflict(
            TenantDeletionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("TENANT_HAS_ACTIVE_CONTRACT", ex.getMessage()));
    }

    @ExceptionHandler(ContractNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> contractNotFound(ContractNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("CONTRACT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ContractAccessDeniedException.class)
    ResponseEntity<AuthDtos.ErrorResponse> contractAccessDenied(ContractAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthDtos.ErrorResponse("CONTRACT_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(ContractBusinessException.class)
    ResponseEntity<AuthDtos.ErrorResponse> contractConflict(ContractBusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("CONTRACT_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler({
        FeeConfigNotFoundException.class,
        ServiceNotFoundException.class,
        UtilityReadingNotFoundException.class,
        RoomForUtilityNotFoundException.class
    })
    ResponseEntity<AuthDtos.ErrorResponse> utilityNotFound(UtilityBusinessException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("UTILITY_RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler({
        FeeConfigPeriodOverlapException.class,
        DuplicateServiceCodeException.class,
        DuplicateUtilityReadingException.class,
        LockedUtilityReadingException.class,
        UtilityReadingSequenceException.class
    })
    ResponseEntity<AuthDtos.ErrorResponse> utilityConflict(UtilityBusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("UTILITY_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler({
        InvalidFeeConfigPeriodException.class,
        InvalidFeePriceException.class,
        InvalidUtilityReadingException.class
    })
    ResponseEntity<AuthDtos.ErrorResponse> utilityBadRequest(UtilityBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("UTILITY_VALIDATION_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(UtilityBusinessException.class)
    ResponseEntity<AuthDtos.ErrorResponse> utilityBusinessError(UtilityBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("UTILITY_BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> invoiceNotFound(InvoiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("INVOICE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvoiceAccessDeniedException.class)
    ResponseEntity<AuthDtos.ErrorResponse> invoiceAccessDenied(InvoiceAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthDtos.ErrorResponse("INVOICE_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> paymentNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("PAYMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(PaymentConflictException.class)
    ResponseEntity<AuthDtos.ErrorResponse> paymentConflict(PaymentConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("PAYMENT_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(BillingBusinessException.class)
    ResponseEntity<AuthDtos.ErrorResponse> billingConflict(BillingBusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("INVOICE_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> notificationNotFound(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("NOTIFICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(NotificationAccessDeniedException.class)
    ResponseEntity<AuthDtos.ErrorResponse> notificationAccessDenied(
            NotificationAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthDtos.ErrorResponse("NOTIFICATION_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(NotificationConflictException.class)
    ResponseEntity<AuthDtos.ErrorResponse> notificationConflict(NotificationConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("NOTIFICATION_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(NotificationBusinessException.class)
    ResponseEntity<AuthDtos.ErrorResponse> notificationBusinessError(
            NotificationBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("NOTIFICATION_BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MaintenanceNotFoundException.class)
    ResponseEntity<AuthDtos.ErrorResponse> maintenanceNotFound(MaintenanceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ErrorResponse("MAINTENANCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MaintenanceAccessDeniedException.class)
    ResponseEntity<AuthDtos.ErrorResponse> maintenanceAccessDenied(
            MaintenanceAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthDtos.ErrorResponse("MAINTENANCE_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(MaintenanceConflictException.class)
    ResponseEntity<AuthDtos.ErrorResponse> maintenanceConflict(MaintenanceConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthDtos.ErrorResponse("MAINTENANCE_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(MaintenanceBusinessException.class)
    ResponseEntity<AuthDtos.ErrorResponse> maintenanceBusinessError(
            MaintenanceBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("MAINTENANCE_BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(ReportBusinessException.class)
    ResponseEntity<AuthDtos.ErrorResponse> reportBusinessError(ReportBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(new AuthDtos.ErrorResponse("REPORT_BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(ReportAccessDeniedException.class)
    ResponseEntity<AuthDtos.ErrorResponse> reportAccessDenied(ReportAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AuthDtos.ErrorResponse("REPORT_ACCESS_DENIED", ex.getMessage()));
    }
}
