package com.quanlynhatro.billing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Creates a VietQR.io Quick Link. The URL contains the amount and transfer content, but it does not
 * confirm or reconcile a payment.
 */
@Component
public class VietQrClient {
    private static final String IMAGE_BASE_URL = "https://img.vietqr.io/image/";
    private static final String TEMPLATE = "compact2";

    @Value("${vietqr.bank-code:ICB}")
    String bankCode;

    @Value("${vietqr.bank-account:}")
    String bankAccount;

    @Value("${vietqr.account-name:}")
    String accountName;

    public Result generate(BigDecimal amount, String orderId, String content) {
        if (isBlank(bankCode) || isBlank(bankAccount) || isBlank(accountName)) {
            throw new PaymentConflictException("Chủ trọ chưa cấu hình tài khoản nhận tiền VietQR, vui lòng liên hệ quản lý");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentConflictException("Số tiền thanh toán phải lớn hơn 0");
        }
        if (content == null || content.isBlank() || content.length() > 50) {
            throw new PaymentConflictException("Nội dung chuyển khoản phải từ 1 đến 50 ký tự");
        }
        final long amountValue;
        try {
            amountValue = amount.longValueExact();
        } catch (ArithmeticException ex) {
            throw new PaymentConflictException("Số tiền thanh toán phải là số nguyên VNĐ");
        }
        if (amountValue <= 0) {
            throw new PaymentConflictException("Số tiền thanh toán phải lớn hơn 0");
        }

        String url =
                IMAGE_BASE_URL
                        + encode(bankCode)
                        + "-"
                        + encode(bankAccount.replaceAll("\\s+", ""))
                        + "-"
                        + TEMPLATE
                        + ".svg?amount="
                        + amountValue
                        + "&addInfo="
                        + encode(content)
                        + "&accountName="
                        + encode(accountName);
        return new Result(url, null, bankCode, bankAccount, accountName);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record Result(
            String qrLink,
            String qrCode,
            String bankCode,
            String accountNumber,
            String accountName) {}
}
