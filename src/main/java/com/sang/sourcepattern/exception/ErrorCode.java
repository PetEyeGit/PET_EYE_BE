package com.sang.sourcepattern.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999,"Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "User already exists", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1002, "Role not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "Unauthorized", HttpStatus.FORBIDDEN),
    INVALID_KEY(1005, "Invalid key", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(10010, "Email does not exist",HttpStatus.NOT_FOUND),
//   Create user errors
    EMAIL_INVALID(1006, "EMAIL_INVALID", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1009, "EMAIL_REQUIRED", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1007, "PASSWORD_INVALID", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1008, "PASSWORD_REQUIRED", HttpStatus.BAD_REQUEST),

//   Pet errors
    PET_NOT_EXISTED(2001, "Pet not existed", HttpStatus.NOT_FOUND),
    CARE_LOG_NOT_FOUND(2002, "Care log not found", HttpStatus.NOT_FOUND),
    NO_IMAGE_IN_CARE_LOG(2003, "This care log does not contain an image", HttpStatus.BAD_REQUEST),
    CARE_LOG_NOT_BELONG_TO_PET(2004, "Care log does not belong to this pet", HttpStatus.BAD_REQUEST),
    OLD_PASSWORD_INVALID(1011, "Old password is incorrect", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1012, "Incorrect password", HttpStatus.UNAUTHORIZED),
    SHOP_EXISTED(3001, "Shop already exists", HttpStatus.BAD_REQUEST),
    SHOP_NOT_FOUND(3002, "Shop not found", HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_VERIFIED(1013, "Account is pending approval. Please wait for admin verification.", HttpStatus.FORBIDDEN),
    ACCOUNT_DEACTIVATED(1015, "Account is deactivated", HttpStatus.FORBIDDEN),
    SHOP_NAME_REQUIRED(3003, "Shop name is required", HttpStatus.BAD_REQUEST),
    SHOP_TYPE_REQUIRED(3004, "Shop type is required", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1014, "Invalid email format", HttpStatus.BAD_REQUEST),
    PHONE_REQUIRED(3005, "Phone number is required", HttpStatus.BAD_REQUEST),
    ADDRESS_REQUIRED(3006, "Address is required", HttpStatus.BAD_REQUEST),
    CITY_REQUIRED(3007, "City is required", HttpStatus.BAD_REQUEST),
    DESCRIPTION_TOO_SHORT(3008, "Description must be at least 10 characters", HttpStatus.BAD_REQUEST),

//   Service errors
    SERVICE_NOT_FOUND(4001, "Service not found", HttpStatus.NOT_FOUND),
    SERVICE_NAME_REQUIRED(4002, "Service name is required", HttpStatus.BAD_REQUEST),
    SERVICE_CATEGORY_REQUIRED(4003, "Service category is required", HttpStatus.BAD_REQUEST),
    SERVICE_PRICE_REQUIRED(4004, "Service price is required", HttpStatus.BAD_REQUEST),
    SERVICE_PRICE_INVALID(4005, "Service price must be greater than 0", HttpStatus.BAD_REQUEST),
    SERVICE_DURATION_INVALID(4006, "Service duration must be at least 1 minute", HttpStatus.BAD_REQUEST),
    SERVICE_DESCRIPTION_TOO_SHORT(4007, "Service description must be at least 10 characters", HttpStatus.BAD_REQUEST),
    SERVICE_NOT_BELONG_TO_SHOP(4008, "Service does not belong to your shop", HttpStatus.FORBIDDEN),
    SHOP_NOT_VERIFIED(3009, "Shop is not verified yet", HttpStatus.FORBIDDEN),

//   Booking errors
    BOOKING_NOT_FOUND(5001, "Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_NOT_BELONG_TO_USER(5002, "Booking does not belong to you", HttpStatus.FORBIDDEN),
    BOOKING_ALREADY_PAID(5003, "Booking is already paid", HttpStatus.BAD_REQUEST),
    BOOKING_CANCELLED(5004, "Booking has been cancelled", HttpStatus.BAD_REQUEST),
    PET_NOT_BELONG_TO_USER(5005, "Pet does not belong to you", HttpStatus.FORBIDDEN),
    STAFF_NOT_BELONG_TO_SHOP(5006, "Staff does not belong to this shop", HttpStatus.BAD_REQUEST),
    PAYOS_ERROR(5007, "Payment gateway error", HttpStatus.INTERNAL_SERVER_ERROR),
    SHOP_ID_REQUIRED(5008, "Shop ID is required", HttpStatus.BAD_REQUEST),
    SERVICE_ID_REQUIRED(5009, "Service ID is required", HttpStatus.BAD_REQUEST),
    PET_ID_REQUIRED(5010, "Pet ID is required", HttpStatus.BAD_REQUEST),
    APPOINTMENT_DATETIME_REQUIRED(5011, "Appointment datetime is required", HttpStatus.BAD_REQUEST),
    APPOINTMENT_MUST_BE_FUTURE(5012, "Appointment must be in the future", HttpStatus.BAD_REQUEST),

//   Email verification errors
    EMAIL_NOT_VERIFIED(6001, "Email is not verified. Please check your inbox.", HttpStatus.FORBIDDEN),
    INVALID_VERIFICATION_TOKEN(6002, "Invalid or expired verification token", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_VERIFIED(6003, "Email is already verified", HttpStatus.BAD_REQUEST),

//   Password reset errors
    INVALID_RESET_TOKEN(6004, "Invalid or expired password reset token", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_USED(6005, "Password reset token has already been used", HttpStatus.BAD_REQUEST),

//   Staff management errors
    STAFF_NOT_FOUND(7001, "Staff not found", HttpStatus.NOT_FOUND),
    STAFF_ALREADY_INACTIVE(7002, "Staff account is already inactive", HttpStatus.BAD_REQUEST),
    STAFF_EMAIL_EXISTED(7003, "Email already used by another account", HttpStatus.BAD_REQUEST),
    BOOKING_NOT_BELONG_TO_STAFF_SHOP(7004, "Booking does not belong to your shop", HttpStatus.FORBIDDEN),
    BOOKING_ALREADY_ASSIGNED(7005, "Booking is already assigned to a staff member", HttpStatus.BAD_REQUEST),
    BOOKING_STATUS_INVALID(7006, "Invalid booking status transition", HttpStatus.BAD_REQUEST),
    MANUAL_ASSIGNMENT_ONLY(7007, "This shop only allows manual assignment by owner", HttpStatus.FORBIDDEN),


//   Wallet errors
    WALLET_NOT_FOUND(8001, "Wallet not found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE(8002, "Insufficient available balance", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_NOT_FOUND(8003, "Withdrawal request not found", HttpStatus.NOT_FOUND),
    WITHDRAWAL_ALREADY_PROCESSED(8004, "Withdrawal request has already been processed", HttpStatus.BAD_REQUEST),
    PENDING_WITHDRAWAL_EXISTS(8005, "You already have a pending or in-progress withdrawal request", HttpStatus.BAD_REQUEST),

    // Review errors
    REVIEW_NOT_ALLOWED(9001, "You must complete a booking before reviewing this shop", HttpStatus.FORBIDDEN),
    REVIEW_ALREADY_EXISTED(9002, "You have already reviewed this shop", HttpStatus.BAD_REQUEST),


    //   Booking conflict
    PET_BOOKING_CONFLICT(5013, "This pet already has an active booking at the requested time", HttpStatus.CONFLICT),
    STAFF_BOOKING_CONFLICT(5014, "This staff member is already booked at the requested time", HttpStatus.CONFLICT),
    NO_STAFF_AVAILABLE(5015, "No staff available for the selected time slot. Please choose a different time.", HttpStatus.CONFLICT),
    MISSING_MEDICAL_RECORD(5016, "Must fill medical record before completing a clinic booking", HttpStatus.BAD_REQUEST),

    // Request errors
    REQUEST_NOT_FOUND(10001, "Request not found", HttpStatus.NOT_FOUND),
    REQUEST_ALREADY_PROCESSED(10002, "Request has already been processed", HttpStatus.BAD_REQUEST),
    STAFF_CHANGE_REQUEST_ALREADY_EXISTS(10003, "A pending staff change request already exists for this booking", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_STATUS_WHILE_REQUEST_PENDING(10004, "Cannot update status while a staff change request is pending", HttpStatus.BAD_REQUEST),
    CANNOT_CHANGE_STAFF_DIRECTLY(10005, "Cannot change staff directly when a staff is already assigned. Please use the request flow.", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(10006,"Invalid request",HttpStatus.BAD_REQUEST),
    DOCKER_NOT_RUNNING(10007, "Docker daemon is not running or failed to start the camera stream", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_TIME_FORMAT(10008, "Invalid time format. Please use ISO 8601 format (e.g., 2024-12-31T23:59:59)", HttpStatus.BAD_REQUEST),

    // No-Show cancellation errors
    NO_SHOW_TOO_EARLY(10009, "Cannot cancel as no-show yet. The grace period has not elapsed.", HttpStatus.BAD_REQUEST),
    INVALID_GRACE_PERIOD(10010, "Late grace period must be between 5 and 30 minutes", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_FOUND(10011, "Voucher not found", HttpStatus.NOT_FOUND)
    ;



    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
