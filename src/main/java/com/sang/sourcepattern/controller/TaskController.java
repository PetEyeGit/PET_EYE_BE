package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.StaffChangeRequestCreate;
import com.sang.sourcepattern.dto.request.StaffChangeResponseDto;
import com.sang.sourcepattern.dto.request.TaskStatusUpdateRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.TaskResponse;
import com.sang.sourcepattern.entity.StaffChangeRequest;
import com.sang.sourcepattern.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Task Management", description = "APIs for Staff to view/update tasks, Owner to assign/manage")
public class TaskController {

    TaskService taskService;

    // ─── Staff endpoints ──────────────────────────────────────────────────────

    /**
     * GET /tasks/my-tasks
     * Staff: Retrieve all bookings assigned to the authenticated staff member.
     */
    @GetMapping("/my-tasks")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Staff: get all tasks assigned to me")
    public ApiResponse<List<TaskResponse>> getMyTasks(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<TaskResponse>>builder()
                .result(taskService.getMyTasks(jwt.getClaim("email")))
                .build();
    }

    /**
     * GET /tasks/unassigned
     * Staff/Owner: Get bookings in the shop that have no staff assigned (Open Pool).
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('STAFF', 'SHOP_OWNER')")
    @Operation(summary = "Staff/Owner: get all unassigned bookings in the shop")
    public ApiResponse<List<TaskResponse>> getUnassignedTasks(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<TaskResponse>>builder()
                .result(taskService.getUnassignedTasks(jwt.getClaim("email")))
                .build();
    }

    /**
     * PUT /tasks/{bookingId}/claim
     * Staff: Self-claim an unassigned booking (OPEN_POOL mode).
     */
    @PutMapping("/{bookingId}/claim")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Staff: claim an unassigned booking")
    public ApiResponse<TaskResponse> claimTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.claimTask(bookingId, jwt.getClaim("email")))
                .message("Task claimed successfully")
                .build();
    }

    /**
     * PUT /tasks/{bookingId}/status
     * Staff: Update the task status (CONFIRMED → IN_PROGRESS → COMPLETED).
     */
    @PutMapping("/{bookingId}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'SHOP_OWNER')")
    @Operation(summary = "Staff: update status of an assigned task")
    public ApiResponse<TaskResponse> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId,
            @RequestBody @Valid TaskStatusUpdateRequest request) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.updateTaskStatus(bookingId, request, jwt.getClaim("email")))
                .message("Task status updated")
                .build();
    }

    // ─── Owner endpoints ──────────────────────────────────────────────────────

    /**
     * GET /tasks/all
     * Owner: get all bookings for the shop (for the assignment board).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Owner: get all bookings for the shop")
    public ApiResponse<List<TaskResponse>> getAllShopTasks(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<TaskResponse>>builder()
                .result(taskService.getAllShopTasks(jwt.getClaim("email")))
                .build();
    }

    /**
     * PUT /tasks/{bookingId}/assign/{staffId}
     * Owner: Manually assign a staff member to a booking.
     */
    @PutMapping("/{bookingId}/assign/{staffId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Owner: manually assign a staff to a booking")
    public ApiResponse<TaskResponse> assignTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId,
            @PathVariable int staffId) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.assignTask(bookingId, staffId, jwt.getClaim("email")))
                .message("Staff assigned to booking")
                .build();
    }

    /**
     * PUT /tasks/{bookingId}/unassign
     * Owner: Remove staff assignment from a booking.
     */
    @PutMapping("/{bookingId}/unassign")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Owner: unassign staff from a booking")
    public ApiResponse<TaskResponse> unassignTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.unassignTask(bookingId, jwt.getClaim("email")))
                .message("Staff unassigned from booking")
                .build();
    }

    /**
     * GET /tasks/{bookingId}/staff-change-request
     * Customer/Owner: Get pending staff change request for a booking.
     */
    @GetMapping("/{bookingId}/staff-change-request")
    @PreAuthorize("hasAnyRole('USER', 'SHOP_OWNER')")
    @Operation(summary = "Get pending staff change request for a booking")
    public ApiResponse<com.sang.sourcepattern.dto.response.StaffChangeRequestResponse> getPendingRequest(@PathVariable int bookingId) {
        List<StaffChangeRequest> requests = taskService.getPendingStaffChangeRequest(bookingId);
        
        if (requests.isEmpty()) {
            return ApiResponse.<com.sang.sourcepattern.dto.response.StaffChangeRequestResponse>builder()
                    .result(null)
                    .build();
        }

        StaffChangeRequest req = requests.get(0);
        com.sang.sourcepattern.dto.response.StaffChangeRequestResponse dto = com.sang.sourcepattern.dto.response.StaffChangeRequestResponse.builder()
                .id(req.getId())
                .bookingId(req.getBooking().getId())
                .reason(req.getReason())
                .status(req.getStatus())
                .oldStaff(req.getOldStaff() != null ? 
                        com.sang.sourcepattern.dto.response.StaffChangeRequestResponse.StaffDto.builder()
                            .id(req.getOldStaff().getId())
                            .fullName(req.getOldStaff().getFullName())
                            .build() : null)
                .proposedStaff(req.getProposedStaff() != null ? 
                        com.sang.sourcepattern.dto.response.StaffChangeRequestResponse.StaffDto.builder()
                            .id(req.getProposedStaff().getId())
                            .fullName(req.getProposedStaff().getFullName())
                            .build() : null)
                .build();

        return ApiResponse.<com.sang.sourcepattern.dto.response.StaffChangeRequestResponse>builder()
                .result(dto)
                .build();
    }

    /**
     * POST /tasks/{bookingId}/request-change/{staffId}
     * Owner: Request to change staff for a booking (requires customer approval).
     */
    @PostMapping("/{bookingId}/request-change/{staffId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Owner: request to change staff for a booking")
    public ApiResponse<Void> requestStaffChange(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId,
            @PathVariable int staffId,
            @RequestBody @Valid StaffChangeRequestCreate request) {
        taskService.requestStaffChange(bookingId, staffId, request.getReason(), jwt.getClaim("email"));
        return ApiResponse.<Void>builder()
                .message("Staff change request created successfully")
                .build();
    }

    @GetMapping("/{bookingId}/staff-change-history")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'USER')")
    @Operation(summary = "Get staff change request history for a booking")
    public ApiResponse<List<com.sang.sourcepattern.dto.response.StaffChangeRequestResponse>> getStaffChangeHistory(@PathVariable int bookingId) {
        List<StaffChangeRequest> requests = taskService.getStaffChangeHistory(bookingId);
        List<com.sang.sourcepattern.dto.response.StaffChangeRequestResponse> dtos = requests.stream()
                .map(req -> com.sang.sourcepattern.dto.response.StaffChangeRequestResponse.builder()
                        .id(req.getId())
                        .bookingId(req.getBooking().getId())
                        .reason(req.getReason())
                        .status(req.getStatus())
                        .oldStaff(req.getOldStaff() != null ? 
                                com.sang.sourcepattern.dto.response.StaffChangeRequestResponse.StaffDto.builder()
                                    .id(req.getOldStaff().getId())
                                    .fullName(req.getOldStaff().getFullName())
                                    .build() : null)
                        .proposedStaff(req.getProposedStaff() != null ? 
                                com.sang.sourcepattern.dto.response.StaffChangeRequestResponse.StaffDto.builder()
                                    .id(req.getProposedStaff().getId())
                                    .fullName(req.getProposedStaff().getFullName())
                                    .build() : null)
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ApiResponse.<List<com.sang.sourcepattern.dto.response.StaffChangeRequestResponse>>builder()
                .result(dtos)
                .build();
    }

    /**
     * PUT /tasks/staff-change/{requestId}/respond
     * Customer: Respond to a staff change request.
     */
    @PutMapping("/staff-change/{requestId}/respond")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Customer: respond to a staff change request")
    public ApiResponse<TaskResponse> respondToStaffChange(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int requestId,
            @RequestBody @Valid StaffChangeResponseDto request) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.respondToStaffChange(requestId, request.getStatus(), jwt.getClaim("email")))
                .message("Responded to staff change request")
                .build();
    }

    /**
     * PUT /tasks/{bookingId}/no-show
     * Staff/Owner: Cancel a booking due to customer no-show (late arrival beyond grace period).
     * The deposit (10%) is retained by Admin as a no-show penalty.
     */
    @PutMapping("/{bookingId}/no-show")
    @PreAuthorize("hasAnyRole('STAFF', 'SHOP_OWNER')")
    @Operation(summary = "Staff/Owner: cancel booking due to customer no-show")
    public ApiResponse<TaskResponse> cancelNoShow(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.cancelNoShow(bookingId, jwt.getClaim("email")))
                .message("Booking cancelled due to customer no-show")
                .build();
    }

    @PutMapping("/{bookingId}/complete-service/{serviceId}")
    @PreAuthorize("hasAnyRole('STAFF', 'SHOP_OWNER')")
    @Operation(summary = "Staff/Owner: mark an individual sub-service item as completed for a multi-service booking")
    public ApiResponse<TaskResponse> completeServiceItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId,
            @PathVariable int serviceId) {
        return ApiResponse.<TaskResponse>builder()
                .result(taskService.completeServiceItem(bookingId, serviceId, jwt.getClaim("email")))
                .message("Service item marked completed")
                .build();
    }
}
