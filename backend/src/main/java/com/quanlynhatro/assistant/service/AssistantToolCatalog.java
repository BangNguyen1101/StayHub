package com.quanlynhatro.assistant.service;

import com.quanlynhatro.assistant.api.AssistantChatResponse.Action;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The data lookups the assistant may run, one per {@link AssistantIntent}. The catalog is filtered
 * by role on the server, so a tenant's model never even sees the admin tools; each lookup is also
 * re-checked when it executes. Actions and follow-up suggestions are fixed per lookup, so the model
 * cannot invent links.
 */
final class AssistantToolCatalog {
    record Param(String name, String type, String description, boolean required) {}

    record Tool(
            AssistantIntent intent,
            String description,
            List<Param> params,
            List<Action> actions,
            List<String> suggestions) {

        String name() {
            return intent.name().toLowerCase(Locale.ROOT);
        }

        boolean adminOnly() {
            return intent.name().startsWith("ADMIN_");
        }
    }

    private static final Param PERIOD =
            new Param("period", "string", "Kỳ tháng dạng yyyy-MM; bỏ trống = tháng hiện tại", false);

    private static final List<Tool> TOOLS =
            List.of(
                    new Tool(
                            AssistantIntent.ADMIN_OVERVIEW,
                            "Tổng quan nhà trọ: số phòng, hợp đồng, công nợ, yêu cầu thuê chờ xử"
                                    + " lý.",
                            List.of(),
                            List.of(new Action("Mở báo cáo", "/admin/reports")),
                            List.of(
                                    "Tháng này còn bao nhiêu tiền chưa thu?",
                                    "Phòng nào sắp hết hợp đồng?",
                                    "Doanh thu tháng này bao nhiêu?")),
                    new Tool(
                            AssistantIntent.ADMIN_MONTHLY_DEBT,
                            "Danh sách hoá đơn còn nợ (chưa thu) của một kỳ tháng, theo phòng và"
                                    + " người trả.",
                            List.of(PERIOD),
                            List.of(new Action("Xem hoá đơn & công nợ", "/admin/invoices")),
                            List.of(
                                    "Công nợ tháng trước thế nào?",
                                    "Doanh thu tháng này bao nhiêu?",
                                    "Tổng quan tình hình nhà trọ")),
                    new Tool(
                            AssistantIntent.ADMIN_EXPIRING_CONTRACTS,
                            "Hợp đồng đang hiệu lực sẽ hết hạn trong N ngày tới.",
                            List.of(
                                    new Param(
                                            "days",
                                            "integer",
                                            "Số ngày tới (1-365); bỏ trống = 30",
                                            false)),
                            List.of(new Action("Quản lý hợp đồng", "/admin/contracts")),
                            List.of(
                                    "Hợp đồng nào hết hạn trong 60 ngày tới?",
                                    "Tình trạng các phòng hiện nay?",
                                    "Tổng quan tình hình nhà trọ")),
                    new Tool(
                            AssistantIntent.ADMIN_ROOMS,
                            "Số phòng theo từng trạng thái (trống, đang ở, đủ người, bảo trì...).",
                            List.of(),
                            List.of(new Action("Quản lý phòng", "/admin/rooms")),
                            List.of(
                                    "Phòng A101 đang có ai ở?",
                                    "Phòng nào sắp hết hợp đồng?",
                                    "Tổng quan tình hình nhà trọ")),
                    new Tool(
                            AssistantIntent.ADMIN_ROOM_DETAIL,
                            "Chi tiết một phòng theo mã phòng: người đang ở, hợp đồng, công nợ"
                                    + " còn lại.",
                            List.of(
                                    new Param(
                                            "roomCode",
                                            "string",
                                            "Mã phòng, ví dụ A102 hoặc P01",
                                            true)),
                            List.of(new Action("Quản lý phòng", "/admin/rooms")),
                            List.of(
                                    "Tình trạng các phòng hiện nay?",
                                    "Tháng này còn bao nhiêu tiền chưa thu?")),
                    new Tool(
                            AssistantIntent.ADMIN_REVENUE,
                            "Doanh thu thực thu (tiền đã xác nhận) trong một khoảng ngày.",
                            List.of(
                                    new Param(
                                            "fromDate",
                                            "string",
                                            "Từ ngày yyyy-MM-dd; bỏ trống = đầu tháng này",
                                            false),
                                    new Param(
                                            "toDate",
                                            "string",
                                            "Đến ngày yyyy-MM-dd; bỏ trống = hôm nay",
                                            false)),
                            List.of(
                                    new Action("Mở báo cáo doanh thu", "/admin/reports"),
                                    new Action("Thanh toán", "/admin/payments")),
                            List.of(
                                    "Doanh thu tháng trước bao nhiêu?",
                                    "Tháng này còn bao nhiêu tiền chưa thu?")),
                    new Tool(
                            AssistantIntent.ADMIN_MAINTENANCE,
                            "Số yêu cầu bảo trì theo trạng thái.",
                            List.of(),
                            List.of(new Action("Xử lý bảo trì", "/admin/maintenance")),
                            List.of("Tổng quan tình hình nhà trọ", "Tình trạng các phòng hiện nay?")),
                    new Tool(
                            AssistantIntent.ADMIN_UTILITIES,
                            "Tổng lượng điện (kWh) và nước (m3) tiêu thụ của toàn nhà trong một kỳ"
                                    + " tháng.",
                            List.of(PERIOD),
                            List.of(new Action("Nhập chỉ số điện nước", "/admin/utilities")),
                            List.of(
                                    "Tiêu thụ điện nước tháng trước?",
                                    "Doanh thu tháng này bao nhiêu?")),
                    new Tool(
                            AssistantIntent.TENANT_INVOICES,
                            "Các hoá đơn còn nợ của chính người dùng.",
                            List.of(),
                            List.of(new Action("Thanh toán ngay", "/tenant/invoices")),
                            List.of(
                                    "Hạn thanh toán hóa đơn của tôi?",
                                    "Tiền điện nước gần đây của tôi?")),
                    new Tool(
                            AssistantIntent.TENANT_UTILITIES,
                            "Tiền điện, nước trên các hoá đơn gần đây của chính người dùng.",
                            List.of(),
                            List.of(new Action("Xem hoá đơn", "/tenant/invoices")),
                            List.of(
                                    "Tôi còn hóa đơn nào chưa trả?",
                                    "Lịch sử thanh toán của tôi?")),
                    new Tool(
                            AssistantIntent.TENANT_CONTRACT,
                            "Hợp đồng đang hiệu lực của chính người dùng (phòng, thời hạn, giá"
                                    + " thuê).",
                            List.of(),
                            List.of(new Action("Xem / gia hạn hợp đồng", "/tenant/contracts")),
                            List.of(
                                    "Tôi còn hóa đơn nào chưa trả?",
                                    "Yêu cầu bảo trì của tôi đang thế nào?")),
                    new Tool(
                            AssistantIntent.TENANT_PAYMENT_DUE,
                            "Hạn thanh toán các hoá đơn còn nợ của chính người dùng, đánh dấu quá"
                                    + " hạn.",
                            List.of(),
                            List.of(new Action("Thanh toán ngay", "/tenant/invoices")),
                            List.of(
                                    "Lịch sử thanh toán của tôi?",
                                    "Tiền điện nước gần đây của tôi?")),
                    new Tool(
                            AssistantIntent.TENANT_PAYMENTS,
                            "Các giao dịch thanh toán gần đây của chính người dùng.",
                            List.of(),
                            List.of(new Action("Lịch sử thanh toán", "/tenant/payments")),
                            List.of(
                                    "Tôi còn hóa đơn nào chưa trả?",
                                    "Hợp đồng của tôi còn hạn đến bao giờ?")),
                    new Tool(
                            AssistantIntent.TENANT_MAINTENANCE,
                            "Các yêu cầu bảo trì gần đây do chính người dùng gửi.",
                            List.of(),
                            List.of(new Action("Gửi / xem yêu cầu bảo trì", "/tenant/maintenance")),
                            List.of(
                                    "Hợp đồng của tôi còn hạn đến bao giờ?",
                                    "Tôi còn hóa đơn nào chưa trả?")));

    private AssistantToolCatalog() {}

    static List<Tool> forRole(String role) {
        boolean admin = "ADMIN".equals(role);
        return TOOLS.stream().filter(tool -> tool.adminOnly() == admin).toList();
    }

    static Optional<Tool> find(String role, String name) {
        return forRole(role).stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    static Optional<Tool> of(AssistantIntent intent) {
        return TOOLS.stream().filter(tool -> tool.intent() == intent).findFirst();
    }
}
