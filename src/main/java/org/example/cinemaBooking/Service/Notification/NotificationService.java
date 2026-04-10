// NotificationService.java
package org.example.cinemaBooking.Service.Notification;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.DTO.Response.Notification.NotificationResponse;
import org.example.cinemaBooking.Entity.Booking;
import org.example.cinemaBooking.Entity.Notification;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.NotificationMapper;
import org.example.cinemaBooking.Repository.NotificationRepository;
import org.example.cinemaBooking.Repository.UserRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.Shared.enums.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class NotificationService {

    NotificationRepository notificationRepository;
    NotificationMapper     notificationMapper;
    UserRepository         userRepository;

    // ── Tạo notification — gọi async từ PaymentService ───────────────

    @Async
    @Transactional
    public void notifyBookingSuccess(Booking booking) {
        create(
            booking.getUser(),
            "Đặt vé thành công 🎬",
            String.format("Vé xem phim \"%s\" lúc %s đã được xác nhận. Mã đặt vé: %s",
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getStartTime(),
                booking.getBookingCode()),
            Type.BOOKING
        );
    }

    @Async
    @Transactional
    public void notifyBookingCancelled(Booking booking) {
        create(
            booking.getUser(),
            "Đặt vé đã bị huỷ",
            String.format("Đặt vé \"%s\" (mã: %s) đã bị huỷ.",
                booking.getShowtime().getMovie().getTitle(),
                booking.getBookingCode()),
            Type.CANCELLATION
        );
    }

    @Async
    @Transactional
    public void notifyPaymentSuccess(Booking booking) {
        create(
            booking.getUser(),
            "Thanh toán thành công 💳",
            String.format("Thanh toán %,.0f đ cho đặt vé %s thành công.",
                booking.getFinalPrice().doubleValue(),
                booking.getBookingCode()),
            Type.PAYMENT
        );
    }

    // ── API cho user ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(int page, int size) {
        String userId = getCurrentUser().getId();
        Page<Notification> notifPage = notificationRepository.findAllByUserId(
            userId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return PageResponse.<NotificationResponse>builder()
            .page(page)
            .size(size)
            .totalElements(notifPage.getTotalElements())
            .totalPages(notifPage.getTotalPages())
            .items(notifPage.getContent().stream()
                .map(notificationMapper::toResponse).toList())
            .build();
    }

    @Transactional(readOnly = true)
    public int countUnread() {
        return notificationRepository.countUnreadByUserId(getCurrentUser().getId());
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsReadByUserId(getCurrentUser().getId());
    }

    @Transactional
    public void markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public NotificationResponse getNotificationById(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(getCurrentUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return notificationMapper.toResponse(notification);
    }

    // ── Private ──────────────────────────────────────────────────────

    private void create(UserEntity user, String title, String body, Type type) {
        notificationRepository.save(
            Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .build()
        );
    }

    private UserEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findUserEntityByUsername(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
