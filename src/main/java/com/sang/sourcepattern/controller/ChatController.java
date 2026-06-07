package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.config.WebSocketAuthInterceptor;
import com.sang.sourcepattern.config.WebSocketAuthInterceptor.WsPrincipal;
import com.sang.sourcepattern.dto.request.ChatMessageRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ChatMessageResponse;
import com.sang.sourcepattern.dto.response.UserResponse;
import com.sang.sourcepattern.entity.Message;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.entity.Staff;
import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.repository.MessageRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.StaffRepository;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ChatController {

    MessageRepository messageRepository;
    UserRepository userRepository;
    ShopRepository shopRepository;
    StaffRepository staffRepository;
    BookingRepository bookingRepository;
    SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket — FE gửi tới /app/chat
     * Principal được set bởi WebSocketAuthInterceptor khi CONNECT
     */
    @MessageMapping("/chat")
    public void handleMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (!(principal instanceof WebSocketAuthInterceptor.WsPrincipal wsPrincipal)) {
            log.error("Invalid principal type: {}", principal);
            return;
        }

        String senderEmail = wsPrincipal.email();
        List<String> roles = wsPrincipal.roles();
        
        String senderRole = roles.contains("ADMIN") ? "ADMIN" : 
                           roles.contains("SHOP_OWNER") ? "SHOP_OWNER" : 
                           roles.contains("STAFF") ? "STAFF" : "USER";

        String finalRecipient = request.getRecipientEmail();
        // If USER is sending to CUSTOMER_CHAT, the recipient is themselves (channel identifier)
        if ("USER".equals(senderRole) && "CUSTOMER_CHAT".equals(request.getChannelType())) {
            finalRecipient = senderEmail;
        }

        // BA Logic: STAFF cannot message ADMIN_SUPPORT
        if ("STAFF".equals(senderRole) && "ADMIN_SUPPORT".equals(request.getChannelType())) {
            log.warn("Security Alert: STAFF {} tried to message ADMIN_SUPPORT", senderEmail);
            return;
        }

        // BA Logic: STAFF can only message CUSTOMER_CHAT if they are assigned via a Booking
        if ("STAFF".equals(senderRole) && "CUSTOMER_CHAT".equals(request.getChannelType())) {
            boolean isAuthorized = false;
            Staff staff = staffRepository.findByUserEmail(senderEmail).orElse(null);
            if (staff != null) {
                isAuthorized = bookingRepository.findByShopId(request.getShopId()).stream()
                    .anyMatch(b -> b.getStaff() != null && b.getStaff().getId() == staff.getId()
                                   && b.getUser() != null && b.getUser().getEmail().equals(request.getRecipientEmail()));
            }
            if (!isAuthorized) {
                log.warn("Security Alert: STAFF {} tried to message CUSTOMER {} without assignment", senderEmail, request.getRecipientEmail());
                return;
            }
        }

        log.info("Chat [{}] : {} (Role: {}) sent message to shop {}: {}", 
                request.getChannelType(), senderEmail, senderRole, request.getShopId(), request.getContent());

        Message message = messageRepository.save(Message.builder()
                .shopId(request.getShopId())
                .channelType(request.getChannelType() != null ? request.getChannelType() : "ADMIN_SUPPORT")
                .senderEmail(senderEmail)
                .recipientEmail(finalRecipient)
                .targetId(request.getTargetId())
                .senderRole(senderRole)
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentType(request.getAttachmentType())
                .attachmentName(request.getAttachmentName())
                .build());

        // Broadcast destination
        String destination;
        if ("CUSTOMER_CHAT".equals(message.getChannelType())) {
            // Broadcast to a specific customer sub-topic
            destination = "/topic/chat/" + request.getShopId() + "/customer/" + message.getRecipientEmail();
        } else if ("DIRECT".equals(message.getChannelType())) {
            // 1-1 chat between Owner and Staff. We use the staff's email as the channel identifier.
            String staffEmail = roles.contains("SHOP_OWNER") ? message.getRecipientEmail() : senderEmail;
            destination = "/topic/chat/" + request.getShopId() + "/direct/" + staffEmail;
        } else {
            destination = "/topic/chat/" + request.getShopId() + "/" + message.getChannelType();
        }
        
        messagingTemplate.convertAndSend(destination, toResponse(message));
    }

    @MessageMapping("/chat/typing")
    public void handleTyping(@Payload ChatMessageRequest request, Principal principal) {
        if (!(principal instanceof WebSocketAuthInterceptor.WsPrincipal wsPrincipal)) return;
        String senderEmail = wsPrincipal.email();
        List<String> roles = wsPrincipal.roles();
        
        String senderRole = roles.contains("ADMIN") ? "ADMIN" : 
                           roles.contains("SHOP_OWNER") ? "SHOP_OWNER" : 
                           roles.contains("STAFF") ? "STAFF" : "USER";

        String finalRecipient = request.getRecipientEmail();
        if ("USER".equals(senderRole) && "CUSTOMER_CHAT".equals(request.getChannelType())) {
            finalRecipient = senderEmail;
        }

        String destination;
        if ("CUSTOMER_CHAT".equals(request.getChannelType())) {
            destination = "/topic/chat/" + request.getShopId() + "/typing/customer/" + finalRecipient;
        } else if ("DIRECT".equals(request.getChannelType())) {
            String staffEmail = roles.contains("SHOP_OWNER") ? request.getRecipientEmail() : senderEmail;
            destination = "/topic/chat/" + request.getShopId() + "/typing/direct/" + staffEmail;
        } else {
            destination = "/topic/chat/" + request.getShopId() + "/typing/" + request.getChannelType();
        }

        messagingTemplate.convertAndSend(destination, java.util.Map.of(
            "senderEmail", senderEmail,
            "senderRole", senderRole,
            "isTyping", "true".equals(request.getContent())
        ));
    }

    /** REST — lấy lịch sử chat */
    @GetMapping("/chat/{shopId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOP_OWNER') or hasRole('STAFF') or hasRole('USER')")
    public ApiResponse<List<ChatMessageResponse>> getHistory(
            @PathVariable int shopId,
            @RequestParam(defaultValue = "ADMIN_SUPPORT") String channelType,
            @RequestParam(required = false) String recipientEmail,
            @AuthenticationPrincipal Jwt jwt) {
        
        List<String> roles = jwt.getClaimAsStringList("roles");
        final String myEmail = jwt.getClaimAsString("email") != null 
                ? jwt.getClaimAsString("email") 
                : jwt.getSubject();

        // Security check for USER role
        if (roles.contains("USER")) {
            if (!"CUSTOMER_CHAT".equals(channelType) && !(shopId == 0 && "ADMIN_SUPPORT".equals(channelType))) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            // User can only see their own chat
            recipientEmail = myEmail;
        }

        if ("ADMIN_SUPPORT".equals(channelType)) {
            if (roles.contains("STAFF")) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
        
        List<Message> messages;
        if ("DIRECT".equals(channelType)) {
            // Get messages between current user and staff (one-on-one)
            // (existing logic)
            messages = messageRepository.findByShopIdAndChannelTypeAndSenderEmailAndRecipientEmailOrderByCreatedAtAsc(
                shopId, channelType, myEmail, recipientEmail);
            List<Message> received = messageRepository.findByShopIdAndChannelTypeAndSenderEmailAndRecipientEmailOrderByCreatedAtAsc(
                shopId, channelType, recipientEmail, myEmail);
            messages.addAll(received);
            messages.sort((a,b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
        } else if ("CUSTOMER_CHAT".equals(channelType)) {
            messages = messageRepository.findByShopIdAndChannelTypeAndRecipientEmailOrderByCreatedAtAsc(
                    shopId, channelType, recipientEmail);
        } else {
            messages = messageRepository.findByShopIdAndChannelTypeOrderByCreatedAtAsc(shopId, channelType);
        }

        return ApiResponse.<List<ChatMessageResponse>>builder()
                .result(messages.stream().map(this::toResponse).toList())
                .build();
    }

    @GetMapping("/chat/my-conversations")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<com.sang.sourcepattern.dto.response.ConversationResponse>> getMyConversations(@AuthenticationPrincipal Jwt jwt) {
        final String myEmail = jwt.getClaimAsString("email") != null 
                ? jwt.getClaimAsString("email") 
                : jwt.getSubject();

        // Find unique shop IDs where user sent or received messages
        java.util.Set<Integer> shopIds = new java.util.HashSet<>();
        List<Integer> msgShopIds = messageRepository.findShopIdsByParticipantEmail(myEmail);
        List<Integer> bookingShopIds = bookingRepository.findShopIdsByUserEmail(myEmail);
        
        if (msgShopIds != null) shopIds.addAll(msgShopIds);
        if (bookingShopIds != null) shopIds.addAll(bookingShopIds);

        List<com.sang.sourcepattern.dto.response.ConversationResponse> shops = shopIds.stream()
                .map(id -> {
                    com.sang.sourcepattern.entity.Shop shop = shopRepository.findById(id).orElse(null);
                    if (shop == null) return null;
                    
                    Message lastMsg = messageRepository.findFirstByShopIdAndChannelTypeAndRecipientEmailOrderByCreatedAtDesc(id, "CUSTOMER_CHAT", myEmail);
                    long unreadCount = messageRepository.countUnreadForCustomer(id, "CUSTOMER_CHAT", myEmail);
                    
                    return com.sang.sourcepattern.dto.response.ConversationResponse.builder()
                            .id(shop.getId())
                            .shopName(shop.getShopName())
                            .logoUrl(shop.getLogoUrl())
                            .lastMessage(lastMsg != null ? lastMsg.getContent() : null)
                            .lastMessageAt(lastMsg != null ? lastMsg.getCreatedAt() : null)
                            .unreadCount((int) unreadCount)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> {
                    if (a.getLastMessageAt() == null && b.getLastMessageAt() == null) return 0;
                    if (a.getLastMessageAt() == null) return 1;
                    if (b.getLastMessageAt() == null) return -1;
                    return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                })
                .toList();

        return ApiResponse.<List<com.sang.sourcepattern.dto.response.ConversationResponse>>builder().result(shops).build();
    }

    /** REST — đánh dấu đã đọc */
    @PatchMapping("/chat/{shopId}/read")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOP_OWNER') or hasRole('STAFF') or hasRole('USER')")
    public ApiResponse<Void> markRead(
            @PathVariable int shopId,
            @RequestParam(defaultValue = "ADMIN_SUPPORT") String channelType,
            @RequestParam(required = false) String recipientEmail,
            @AuthenticationPrincipal Jwt jwt) {
        
        List<String> roles = jwt.getClaimAsStringList("roles");
        final String myEmail = jwt.getClaimAsString("email") != null 
                ? jwt.getClaimAsString("email") 
                : jwt.getSubject();

        if (roles.contains("STAFF") && "ADMIN_SUPPORT".equals(channelType)) {
            return ApiResponse.<Void>builder().message("Access denied").build();
        }

        String readerRole = roles.contains("ADMIN") ? "ADMIN" : 
                           roles.contains("STAFF") ? "STAFF" : 
                           roles.contains("USER") ? "USER" : "SHOP_OWNER";

        if ("CUSTOMER_CHAT".equals(channelType) || (shopId == 0 && "ADMIN_SUPPORT".equals(channelType))) {
            String targetCustomer = roles.contains("USER") ? myEmail : recipientEmail;
            messageRepository.markRecipientAllAsRead(shopId, channelType, targetCustomer, readerRole);
        } else {
            messageRepository.markAllAsRead(shopId, channelType, readerRole);
        }
        return ApiResponse.<Void>builder().message("Marked as read").build();
    }

    @GetMapping("/chat/{shopId}/customers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOP_OWNER') or hasRole('STAFF')")
    public ApiResponse<List<UserResponse>> getShopCustomers(
            @PathVariable int shopId, 
            @AuthenticationPrincipal Jwt jwt) {
        
        final List<String> roles = jwt.getClaimAsStringList("roles");
        final String emailIdentifier = jwt.getClaimAsString("email") != null 
                ? jwt.getClaimAsString("email") 
                : jwt.getSubject();
        
        boolean isStaff = roles != null && roles.contains("STAFF") && !roles.contains("SHOP_OWNER");
        java.util.Set<User> allowedUsers = new java.util.HashSet<>();

        if (isStaff) {
            java.util.Optional<Staff> staffOpt;
            if (emailIdentifier.contains("@")) {
                staffOpt = staffRepository.findByUserEmail(emailIdentifier);
            } else {
                staffOpt = staffRepository.findByUserId(Integer.parseInt(emailIdentifier));
            }

            staffOpt.ifPresent(staff -> {
                List<Booking> staffBookings = bookingRepository.findByStaffId(staff.getId());
                staffBookings.forEach(b -> {
                    // Only show customers from non-cancelled bookings
                    if (b.getUser() != null && !"CANCELLED".equals(b.getStatus())) {
                        allowedUsers.add(b.getUser());
                    }
                });
            });
        } else {
            // Owner/Admin sees all customers
            allowedUsers.addAll(userRepository.findUsersByShopId(shopId));
            allowedUsers.addAll(userRepository.findUsersByChatHistory(shopId));
        }

        List<UserResponse> response = allowedUsers.stream()
                .map(u -> {
                    Message lastMsg = messageRepository.findFirstByShopIdAndChannelTypeAndRecipientEmailOrderByCreatedAtDesc(shopId, "CUSTOMER_CHAT", u.getEmail());
                    long unreadCount = messageRepository.countUnreadForShopFromCustomer(shopId, "CUSTOMER_CHAT", u.getEmail());
                    return UserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .avatar(u.getAvatar())
                        .lastMessage(lastMsg != null ? lastMsg.getContent() : null)
                        .lastMessageAt(lastMsg != null ? lastMsg.getCreatedAt() : null)
                        .unreadCount((int) unreadCount)
                        .build();
                })
                .sorted((a, b) -> {
                    if (a.getLastMessageAt() == null && b.getLastMessageAt() == null) return 0;
                    if (a.getLastMessageAt() == null) return 1;
                    if (b.getLastMessageAt() == null) return -1;
                    return b.getLastMessageAt().compareTo(a.getLastMessageAt());
                })
                .toList();
        return ApiResponse.<List<UserResponse>>builder().result(response).build();
    }

    private ChatMessageResponse toResponse(Message m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .shopId(m.getShopId())
                .channelType(m.getChannelType())
                .senderEmail(m.getSenderEmail())
                .recipientEmail(m.getRecipientEmail())
                .targetId(m.getTargetId())
                .senderRole(m.getSenderRole())
                .content(m.getContent())
                .attachmentUrl(m.getAttachmentUrl())
                .attachmentType(m.getAttachmentType())
                .attachmentName(m.getAttachmentName())
                .createdAt(m.getCreatedAt())
                .isRead(m.isRead())
                .build();
    }
}
