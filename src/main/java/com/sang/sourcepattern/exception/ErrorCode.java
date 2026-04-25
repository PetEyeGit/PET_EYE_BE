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
