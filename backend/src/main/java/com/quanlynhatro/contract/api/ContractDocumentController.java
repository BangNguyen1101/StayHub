package com.quanlynhatro.contract.api;

import com.quanlynhatro.auth.domain.AppUserEntity;
import com.quanlynhatro.auth.repository.AppUserRepository;
import com.quanlynhatro.contract.service.ContractAccessDeniedException;
import com.quanlynhatro.contract.service.ContractBusinessException;
import com.quanlynhatro.contract.service.ContractDocumentService;
import com.quanlynhatro.contract.service.ContractDocumentService.DocumentPdf;
import com.quanlynhatro.contract.service.ContractDocumentService.Viewer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Electronic contract documents: PDF preview/download, online signing and integrity checks. */
@RestController
@RequestMapping("/api")
public class ContractDocumentController {
    private static final String ADMIN = "ADMIN";
    private static final String TENANT = "TENANT";

    private final ContractDocumentService documents;
    private final AppUserRepository users;

    public ContractDocumentController(ContractDocumentService documents, AppUserRepository users) {
        this.documents = documents;
        this.users = users;
    }

    @GetMapping("/admin/contracts/{id}/documents")
    public List<ContractDocumentResponse> adminList(
            Authentication authentication, @PathVariable @Positive Long id) {
        return documents.list(id, admin(authentication));
    }

    @PostMapping("/admin/contracts/{id}/documents/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractDocumentResponse issue(
            Authentication authentication, @PathVariable @Positive Long id) {
        return documents.issueForAdmin(id, admin(authentication).userId());
    }

    @GetMapping("/admin/contracts/documents/{documentId}/pdf")
    public ResponseEntity<byte[]> adminPdf(
            Authentication authentication, @PathVariable @Positive Long documentId) {
        return pdfResponse(documents.pdf(documentId, admin(authentication)));
    }

    @GetMapping("/admin/contracts/documents/{documentId}/verify")
    public ContractDocumentVerifyResponse adminVerify(
            Authentication authentication, @PathVariable @Positive Long documentId) {
        return documents.verifyStored(documentId, admin(authentication));
    }

    @PostMapping("/admin/contracts/documents/{documentId}/verify-file")
    public ContractDocumentVerifyResponse adminVerifyFile(
            Authentication authentication,
            @PathVariable @Positive Long documentId,
            @RequestParam("file") MultipartFile file) {
        return documents.verifyUpload(documentId, bytes(file), admin(authentication));
    }

    @GetMapping("/tenant/contracts/{id}/documents")
    public List<ContractDocumentResponse> tenantList(
            Authentication authentication, @PathVariable @Positive Long id) {
        return documents.list(id, tenant(authentication));
    }

    @PostMapping("/tenant/contracts/{id}/renewals")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractDocumentResponse requestRenewal(
            Authentication authentication,
            @PathVariable @Positive Long id,
            @Valid @RequestBody ContractRenewalCreateRequest request) {
        return documents.requestRenewal(id, request.months(), tenant(authentication).userId());
    }

    @PostMapping("/tenant/contracts/documents/{documentId}/sign")
    public ContractDocumentResponse sign(
            Authentication authentication,
            @PathVariable @Positive Long documentId,
            @Valid @RequestBody ContractSignRequest request,
            HttpServletRequest http) {
        return documents.sign(
                documentId,
                request,
                tenant(authentication).userId(),
                http.getRemoteAddr(),
                http.getHeader(HttpHeaders.USER_AGENT));
    }

    @GetMapping("/tenant/contracts/documents/{documentId}/pdf")
    public ResponseEntity<byte[]> tenantPdf(
            Authentication authentication, @PathVariable @Positive Long documentId) {
        return pdfResponse(documents.pdf(documentId, tenant(authentication)));
    }

    @GetMapping("/tenant/contracts/documents/{documentId}/verify")
    public ContractDocumentVerifyResponse tenantVerify(
            Authentication authentication, @PathVariable @Positive Long documentId) {
        return documents.verifyStored(documentId, tenant(authentication));
    }

    @PostMapping("/tenant/contracts/documents/{documentId}/verify-file")
    public ContractDocumentVerifyResponse tenantVerifyFile(
            Authentication authentication,
            @PathVariable @Positive Long documentId,
            @RequestParam("file") MultipartFile file) {
        return documents.verifyUpload(documentId, bytes(file), tenant(authentication));
    }

    private static ResponseEntity<byte[]> pdfResponse(DocumentPdf pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.fileName()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.content());
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ContractBusinessException("Không đọc được file tải lên");
        }
    }

    private Viewer admin(Authentication authentication) {
        return new Viewer(requireRole(authentication, ADMIN).getId(), true);
    }

    private Viewer tenant(Authentication authentication) {
        return new Viewer(requireRole(authentication, TENANT).getId(), false);
    }

    private AppUserEntity requireRole(Authentication authentication, String role) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ContractAccessDeniedException();
        }
        AppUserEntity user =
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                authentication.getName(), authentication.getName())
                        .orElseThrow(ContractAccessDeniedException::new);
        if (user.getRole() == null || !role.equals(user.getRole().getCode())) {
            throw new ContractAccessDeniedException();
        }
        return user;
    }
}
