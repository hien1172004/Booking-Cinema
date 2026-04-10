package org.example.cinemaBooking.Shared.utils;

import org.example.cinemaBooking.Entity.Booking;
import org.example.cinemaBooking.Entity.Ticket;

public class EmailTemplateUtil {

    public static String buildBookingSuccessEmail(Booking booking) {

        // ── Danh sách ghế ────────────────────────────────────────────────
        StringBuilder seatRows = new StringBuilder();
        for (Ticket t : booking.getTickets()) {
            seatRows.append(String.format("""
                <tr>
                  <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0">
                    %s%s
                  </td>
                  <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;color:#666">
                    %s
                  </td>
                  <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;
                             text-align:right;color:#e8353e;font-weight:600">
                    %,.0f đ
                  </td>
                </tr>
                """,
                    t.getSeat().getSeatRow(),
                    t.getSeat().getSeatNumber(),
                    t.getSeat().getSeatType().getName(),
                    t.getPrice()
            ));
        }

        // ── Danh sách sản phẩm / combo ───────────────────────────────────
        StringBuilder productRows = new StringBuilder();
        if (booking.getBookingProducts() != null && !booking.getBookingProducts().isEmpty()) {
            for (var bp : booking.getBookingProducts()) {
                productRows.append(String.format("""
                    <tr>
                      <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0">
                        %s
                      </td>
                      <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;
                                 color:#666;text-align:center">
                        x%d
                      </td>
                      <td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;
                                 text-align:right;color:#e8353e;font-weight:600">
                        %,.0f đ
                      </td>
                    </tr>
                    """,
                        bp.getItemName(),
                        bp.getQuantity(),
                        bp.getItemPrice().multiply(java.math.BigDecimal.valueOf(bp.getQuantity()))
                ));
            }
        }

        // Block sản phẩm — ẩn hoàn toàn nếu không có
        String productBlock = productRows.isEmpty() ? "" : String.format("""
            <!-- Đồ ăn / Combo -->
            <div style="padding:16px 32px;border-bottom:1px dashed #eee">
              <p style="margin:0 0 10px;font-size:14px;font-weight:600;color:#444">
                🍿 Đồ ăn &amp; Combo
              </p>
              <table style="width:100%%;border-collapse:collapse;font-size:14px">
                <thead>
                  <tr style="background:#f5f5f5">
                    <th style="padding:8px 12px;text-align:left;
                               color:#666;font-weight:500">Sản phẩm</th>
                    <th style="padding:8px 12px;text-align:center;
                               color:#666;font-weight:500">SL</th>
                    <th style="padding:8px 12px;text-align:right;
                               color:#666;font-weight:500">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>%s</tbody>
              </table>
            </div>
            """, productRows);

        // ── Dòng giảm giá — chỉ hiện nếu có ────────────────────────────
        String discountRow = booking.getDiscountAmount()
                .compareTo(java.math.BigDecimal.ZERO) > 0
                ? String.format("""
            <tr>
              <td style="padding:4px 0;color:#27ae60">
                Giảm giá (%s)
              </td>
              <td style="text-align:right;color:#27ae60">-%,.0f đ</td>
            </tr>
            """,
                booking.getPromotion() != null
                        ? booking.getPromotion().getCode() : "",
                booking.getDiscountAmount())
                : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="font-family:Arial,sans-serif;background:#f9f9f9;
                         padding:24px;margin:0">
              <div style="max-width:560px;margin:0 auto;background:#fff;
                          border-radius:12px;overflow:hidden;
                          box-shadow:0 2px 8px rgba(0,0,0,0.08)">

                <!-- Header -->
                <div style="background:#e8353e;padding:28px 32px;text-align:center">
                  <h1 style="color:#fff;margin:0;font-size:22px;font-weight:700">
                    🎬 Đặt vé thành công!
                  </h1>
                </div>

                <!-- Movie Info -->
                <div style="padding:24px 32px 16px;text-align:center;
                            border-bottom:1px dashed #eee">
                  <h2 style="margin:0 0 6px;font-size:20px;color:#1a1a1a;
                             font-weight:700">%s</h2>
                  <p style="margin:0;color:#666;font-size:14px">
                    %s — %s
                  </p>
                </div>

                <!-- Mã vé + QR -->
                <div style="padding:24px 32px;text-align:center;
                            border-bottom:1px dashed #eee">
                  <p style="margin:0 0 4px;font-size:12px;color:#999;
                            text-transform:uppercase;letter-spacing:1px">
                    Mã vé (Reservation Code)
                  </p>
                  <p style="margin:0 0 16px;font-size:28px;font-weight:700;
                            color:#1a1a1a;letter-spacing:2px">
                    %s
                  </p>
                  <img src="cid:qrImage"
                       width="180" height="180"
                       alt="QR Code"
                       style="display:block;margin:0 auto;border-radius:8px"/>
                  <p style="margin:12px 0 0;font-size:13px;color:#888">
                    Xuất trình mã này tại quầy để nhận vé vật lý
                  </p>
                </div>

                <!-- Suất chiếu -->
                <div style="padding:16px 32px;border-bottom:1px dashed #eee">
                  <table style="width:100%%;font-size:14px">
                    <tr>
                      <td style="padding:4px 0;color:#888">Suất chiếu</td>
                      <td style="text-align:right;color:#1a1a1a;font-weight:600">
                        %s %s
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:4px 0;color:#888">Phòng chiếu</td>
                      <td style="text-align:right;color:#1a1a1a">%s</td>
                    </tr>
                  </table>
                </div>

                <!-- Danh sách ghế -->
                <div style="padding:16px 32px;border-bottom:1px dashed #eee">
                  <p style="margin:0 0 10px;font-size:14px;font-weight:600;color:#444">
                    🎟️ Danh sách ghế
                  </p>
                  <table style="width:100%%;border-collapse:collapse;font-size:14px">
                    <thead>
                      <tr style="background:#f5f5f5">
                        <th style="padding:8px 12px;text-align:left;
                                   color:#666;font-weight:500">Ghế</th>
                        <th style="padding:8px 12px;text-align:left;
                                   color:#666;font-weight:500">Loại</th>
                        <th style="padding:8px 12px;text-align:right;
                                   color:#666;font-weight:500">Giá</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                </div>

                %s

                <!-- Tổng tiền -->
                <div style="padding:16px 32px;background:#fafafa;
                            border-bottom:1px dashed #eee">
                  <table style="width:100%%;font-size:14px">
                    <tr>
                      <td style="padding:4px 0;color:#666">Tổng tiền</td>
                      <td style="text-align:right;color:#333">%,.0f đ</td>
                    </tr>
                    %s
                    <tr>
                      <td style="padding:8px 0 4px;font-weight:700;
                                 font-size:16px;color:#1a1a1a">Thành tiền</td>
                      <td style="text-align:right;font-weight:700;
                                 font-size:16px;color:#e8353e">%,.0f đ</td>
                    </tr>
                  </table>
                </div>

                <!-- Lưu ý -->
                <div style="padding:20px 32px;font-size:13px;color:#666;
                            line-height:1.6">
                  <p style="margin:0 0 6px;font-weight:600;color:#444">Lưu ý:</p>
                  <p style="margin:0">
                    Vé đã mua không thể huỷ, đổi trả. Vui lòng đến trước giờ chiếu
                    ít nhất <strong>15 phút</strong>.
                    Cảm ơn bạn đã lựa chọn dịch vụ của chúng tôi.
                    Chúc bạn xem phim vui vẻ! 🍿
                  </p>
                </div>

              </div>
            </body>
            </html>
            """,
                // Movie Info
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getRoom().getCinema().getName(),
                booking.getShowtime().getRoom().getName(),
                // Mã vé
                booking.getBookingCode(),
                // Suất chiếu
                booking.getShowtime().getStartTime().toLocalDate(),
                booking.getShowtime().getStartTime().toLocalTime(),
                booking.getShowtime().getRoom().getName(),
                // Ghế
                seatRows,
                // Sản phẩm / combo
                productBlock,
                // Giá
                booking.getTotalPrice(),
                discountRow,
                booking.getFinalPrice()
        );
    }


    public static String buildCancelledEmail(Booking booking) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="font-family:Arial,sans-serif;background:#f9f9f9;padding:24px">
              <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px;
                          overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08)">
                <div style="background:#666;padding:32px;text-align:center">
                  <h1 style="color:#fff;margin:0;font-size:22px">Đặt vé đã bị huỷ</h1>
                  <p style="color:rgba(255,255,255,0.8);margin:8px 0 0">
                    Mã đặt vé: <strong>%s</strong>
                  </p>
                </div>
                <div style="padding:32px;text-align:center;color:#555;font-size:15px">
                  <p>Đặt vé <strong>%s</strong> của bạn đã bị huỷ.</p>
                  <p>Nếu bạn đã thanh toán, hoàn tiền sẽ được xử lý trong 3-5 ngày làm việc.</p>
                </div>
              </div>
            </body>
            </html>
            """,
                booking.getBookingCode(),
                booking.getShowtime().getMovie().getTitle()
        );
    }
}