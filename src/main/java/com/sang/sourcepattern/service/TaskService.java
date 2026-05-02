package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.TaskStatusUpdateRequest;
import com.sang.sourcepattern.dto.response.TaskResponse;

import java.util.List;

public interface TaskService {

    /** Staff: get all bookings assigned to the authenticated staff member */
    List<TaskResponse> getMyTasks(String staffEmail);

    /** Staff/Owner: get all bookings in the shop that have NO staff assigned yet */
    List<TaskResponse> getUnassignedTasks(String requesterEmail);

    /** Staff: claim an unassigned booking (OPEN_POOL mode) */
    TaskResponse claimTask(int bookingId, String staffEmail);

    /** Owner: manually assign a specific staff to a booking */
    TaskResponse assignTask(int bookingId, int staffId, String ownerEmail);

    /** Owner: unassign staff from a booking */
    TaskResponse unassignTask(int bookingId, String ownerEmail);

    /** Staff: update task status (CONFIRMED → IN_PROGRESS → COMPLETED) */
    TaskResponse updateTaskStatus(int bookingId, TaskStatusUpdateRequest request, String staffEmail);

    /** Owner: get all bookings for the shop (with and without staff) */
    List<TaskResponse> getAllShopTasks(String ownerEmail);
}
