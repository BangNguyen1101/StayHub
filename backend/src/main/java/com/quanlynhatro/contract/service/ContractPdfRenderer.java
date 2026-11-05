package com.quanlynhatro.contract.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders contract documents to PDF with the bundled Noto Sans font (Vietnamese glyphs). */
@Component
public class ContractPdfRenderer {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String DEFAULT_TERMS =
            """
            1. Bên B sử dụng phòng đúng mục đích để ở, giữ gìn vệ sinh, an ninh trật tự và \
            tuân thủ nội quy nhà trọ.
            2. Bên B thanh toán tiền thuê và các chi phí dịch vụ đúng hạn theo hoá đơn do Bên A \
            phát hành trên hệ thống.
            3. Bên A bảo đảm quyền sử dụng phòng của Bên B trong thời hạn thuê và sửa chữa các \
            hư hỏng không do lỗi của Bên B.
            4. Bên nào muốn chấm dứt hợp đồng trước hạn phải báo trước cho bên kia ít nhất 30 \
            ngày.""";

    private final BaseFont regular;
    private final BaseFont bold;

    public ContractPdfRenderer() {
        this.regular = loadFont("fonts/NotoSans-Regular.ttf");
        this.bold = loadFont("fonts/NotoSans-Bold.ttf");
    }

    /** Signature placed on a signed document; {@code signedAtUtc} is stored in UTC. */
    public record SignatureStamp(
            byte[] signaturePng, String signerName, LocalDateTime signedAtUtc, String signerIp) {}

    /**
     * @param stamp the tenant's signature, or {@code null} for an unsigned preview (watermarked)
     */
    public byte[] render(
            Long documentId, ContractDocumentContent content, String contentHash,
            SignatureStamp stamp) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 56, 56, 48, 48);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            if (stamp == null) {
                writer.setPageEvent(new PreviewWatermark());
            }
            document.addTitle(title(content) + " " + content.contractCode());
            document.open();
            writeHeader(document, content);
            writeParties(document, content);
            if ("RENEWAL".equals(content.documentType())) {
                writeRenewalBody(document, content);
            } else {
                writeContractBody(document, content);
            }
            writeSignatures(document, content, stamp);
            writeVerification(document, documentId, contentHash, stamp);
            document.close();
        } catch (DocumentException | IOException ex) {
            throw new ContractBusinessException("Không thể tạo file PDF hợp đồng");
        }
        return out.toByteArray();
    }

    private void writeHeader(Document document, ContractDocumentContent content) {
        document.add(centered("CỘNG HOÀ XÃ HỘI CHỦ NGHĨA VIỆT NAM", font(bold, 12)));
        document.add(centered("Độc lập - Tự do - Hạnh phúc", font(bold, 11)));
        document.add(centered("---------------", font(regular, 10)));
        Paragraph title = centered(title(content), font(bold, 16));
        title.setSpacingBefore(14);
        document.add(title);
        document.add(centered("Số: " + text(content.contractCode()), font(regular, 11)));
        Paragraph issued =
                centered("Ngày lập: " + date(content.issuedDate()), font(regular, 10));
        issued.setSpacingAfter(12);
        document.add(issued);
    }

    private void writeParties(Document document, ContractDocumentContent content) {
        writeParty(document, "BÊN CHO THUÊ (Bên A)", content.landlord());
        writeParty(document, "BÊN THUÊ (Bên B)", content.tenant());
    }

    private void writeParty(
            Document document, String heading, ContractDocumentContent.Party party) {
        document.add(section(heading));
        document.add(line("Họ và tên", party.fullName()));
        document.add(line("Số điện thoại", party.phoneNumber()));
        document.add(line("Email", party.email()));
        if (party.identityDocumentNumber() != null) {
            String identity =
                    party.identityDocumentNumber()
                            + (party.identityIssuedDate() == null
                                    ? ""
                                    : ", cấp ngày " + date(party.identityIssuedDate()))
                            + (party.identityIssuedPlace() == null
                                    ? ""
                                    : ", tại " + party.identityIssuedPlace());
            document.add(
                    line(
                            party.identityDocumentType() == null
                                    ? "Giấy tờ tuỳ thân"
                                    : party.identityDocumentType(),
                            identity));
        }
        if (party.address() != null) {
            document.add(line("Địa chỉ liên hệ", party.address()));
        }
    }

    private void writeContractBody(Document document, ContractDocumentContent content) {
        Paragraph intro =
                paragraph(
                        "Hai bên thống nhất ký kết hợp đồng thuê phòng trọ với các điều khoản"
                                + " sau:",
                        font(regular, 11));
        intro.setSpacingBefore(8);
        document.add(intro);
        document.add(section("ĐIỀU 1. PHÒNG CHO THUÊ"));
        writeRoom(document, content.room());
        document.add(section("ĐIỀU 2. THỜI HẠN THUÊ"));
        document.add(
                line(
                        "Thời hạn",
                        "từ ngày " + date(content.startDate()) + " đến ngày "
                                + date(content.endDate())));
        document.add(section("ĐIỀU 3. GIÁ THUÊ VÀ THANH TOÁN"));
        document.add(line("Tiền thuê", money(content.monthlyRent()) + " / tháng"));
        document.add(line("Tiền đặt cọc", money(content.depositAmount())));
        document.add(
                line("Chu kỳ thanh toán", content.billingCycleMonths() + " tháng một lần"));
        document.add(line("Hạn thanh toán", "ngày " + content.dueDay() + " của kỳ thanh toán"));
        document.add(section("ĐIỀU 4. QUYỀN, NGHĨA VỤ VÀ ĐIỀU KHOẢN KHÁC"));
        String terms =
                content.terms() == null || content.terms().isBlank()
                        ? DEFAULT_TERMS
                        : content.terms();
        document.add(paragraph(terms, font(regular, 11)));
    }

    private void writeRenewalBody(Document document, ContractDocumentContent content) {
        Paragraph intro =
                paragraph(
                        "Căn cứ hợp đồng thuê phòng số " + text(content.contractCode())
                                + ", hai bên thống nhất gia hạn hợp đồng như sau:",
                        font(regular, 11));
        intro.setSpacingBefore(8);
        document.add(intro);
        document.add(section("ĐIỀU 1. PHÒNG THUÊ"));
        writeRoom(document, content.room());
        document.add(section("ĐIỀU 2. GIA HẠN THỜI HẠN THUÊ"));
        document.add(line("Ngày hết hạn cũ", date(content.previousEndDate())));
        document.add(line("Gia hạn thêm", content.renewalMonths() + " tháng"));
        document.add(line("Ngày hết hạn mới", date(content.newEndDate())));
        document.add(section("ĐIỀU 3. GIÁ THUÊ"));
        document.add(line("Tiền thuê", money(content.monthlyRent()) + " / tháng (giữ nguyên)"));
        document.add(section("ĐIỀU 4. ĐIỀU KHOẢN KHÁC"));
        document.add(
                paragraph(
                        "Các điều khoản khác của hợp đồng số " + text(content.contractCode())
                                + " giữ nguyên hiệu lực. Phụ lục này là một phần không tách"
                                + " rời của hợp đồng.",
                        font(regular, 11)));
    }

    private void writeRoom(Document document, ContractDocumentContent.RoomInfo room) {
        document.add(
                line(
                        "Phòng",
                        text(room.roomCode())
                                + (room.roomName() == null ? "" : " - " + room.roomName())));
        if (room.floorNumber() != null) {
            document.add(line("Tầng", String.valueOf(room.floorNumber())));
        }
        if (room.areaM2() != null) {
            document.add(line("Diện tích", room.areaM2().stripTrailingZeros().toPlainString() + " m²"));
        }
    }

    private void writeSignatures(
            Document document, ContractDocumentContent content, SignatureStamp stamp)
            throws IOException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(24);
        table.setKeepTogether(true);
        table.addCell(signatureCell("BÊN A", content.landlord().fullName(), null, null));
        table.addCell(
                signatureCell(
                        "BÊN B",
                        content.tenant().fullName(),
                        stamp == null ? null : stamp.signaturePng(),
                        stamp == null
                                ? "(Chưa ký)"
                                : "Đã ký điện tử lúc " + dateTime(stamp.signedAtUtc())));
        document.add(table);
    }

    private PdfPCell signatureCell(String party, String name, byte[] png, String note)
            throws IOException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.addElement(centered(party, font(bold, 11)));
        cell.addElement(centered("(Ký và ghi rõ họ tên)", font(regular, 9)));
        if (png != null) {
            Image signature = Image.getInstance(png);
            signature.scaleToFit(170, 70);
            signature.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(signature);
        } else {
            Paragraph blank = new Paragraph(" ");
            blank.setSpacingBefore(56);
            cell.addElement(blank);
        }
        cell.addElement(centered(text(name), font(bold, 11)));
        if (note != null) {
            cell.addElement(centered(note, font(regular, 9)));
        }
        return cell;
    }

    private void writeVerification(
            Document document, Long documentId, String contentHash, SignatureStamp stamp) {
        Font small = font(regular, 8);
        Paragraph heading = paragraph("THÔNG TIN XÁC THỰC ĐIỆN TỬ", font(bold, 9));
        heading.setSpacingBefore(24);
        document.add(heading);
        document.add(paragraph("Mã tài liệu: #" + documentId, small));
        document.add(paragraph("Mã băm nội dung (SHA-256): " + contentHash, small));
        if (stamp != null) {
            document.add(
                    paragraph(
                            "Ký bởi " + text(stamp.signerName()) + " lúc "
                                    + dateTime(stamp.signedAtUtc()) + " (giờ Việt Nam), địa chỉ IP: "
                                    + text(stamp.signerIp()),
                            small));
            document.add(
                    paragraph(
                            "Mã băm SHA-256 của file PDF này được lưu trên hệ thống quản lý nhà trọ."
                                    + " Mọi chỉnh sửa file sau khi ký sẽ làm mã băm không còn khớp.",
                            small));
        } else {
            document.add(paragraph("Bản xem trước, chưa có giá trị khi chưa được ký.", small));
        }
    }

    private static String title(ContractDocumentContent content) {
        return "RENEWAL".equals(content.documentType())
                ? "PHỤ LỤC GIA HẠN HỢP ĐỒNG THUÊ PHÒNG"
                : "HỢP ĐỒNG THUÊ PHÒNG TRỌ";
    }

    private Paragraph section(String value) {
        Paragraph paragraph = paragraph(value, font(bold, 11));
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(2);
        return paragraph;
    }

    private Paragraph line(String label, String value) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk("- " + nfc(label) + ": ", font(regular, 11)));
        paragraph.add(new Chunk(text(value), font(bold, 11)));
        return paragraph;
    }

    private static Paragraph centered(String value, Font font) {
        Paragraph paragraph = new Paragraph(nfc(value), font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private static Paragraph paragraph(String value, Font font) {
        Paragraph paragraph = new Paragraph(nfc(value), font);
        paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
        return paragraph;
    }

    private static Font font(BaseFont base, float size) {
        return new Font(base, size);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "-" : nfc(value.trim());
    }

    /** Precomposed Vietnamese characters exist in the font; decomposed marks may not render. */
    private static String nfc(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }

    private static String date(LocalDate value) {
        return value == null ? "-" : DATE.format(value);
    }

    private static String dateTime(LocalDateTime utc) {
        return DATE_TIME.format(utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(DISPLAY_ZONE));
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(value) + " đ";
    }

    private static BaseFont loadFont(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            byte[] bytes = input.readAllBytes();
            return BaseFont.createFont(
                    path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (IOException ex) {
            throw new UncheckedIOException("Missing contract PDF font " + path, ex);
        }
    }

    /** Diagonal grey watermark on every page of an unsigned preview. */
    private final class PreviewWatermark extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState state = new PdfGState();
            state.setFillOpacity(0.12f);
            canvas.saveState();
            canvas.setGState(state);
            canvas.setColorFill(Color.GRAY);
            canvas.beginText();
            canvas.setFontAndSize(bold, 44);
            canvas.showTextAligned(
                    Element.ALIGN_CENTER,
                    nfc("BẢN XEM TRƯỚC - CHƯA KÝ"),
                    PageSize.A4.getWidth() / 2,
                    PageSize.A4.getHeight() / 2,
                    45);
            canvas.endText();
            canvas.restoreState();
        }
    }
}
