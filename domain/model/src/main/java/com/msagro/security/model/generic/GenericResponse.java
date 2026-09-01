package com.msagro.security.model.generic;

import com.msagro.security.model.enums.ResponseStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Unified API response wrapper. Every endpoint of ms-security answers with this envelope,
 * so clients parse one shape for both successes and errors.
 *
 * @param <T> payload type
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponse<T> {

    /** SUCCESS or ERROR. */
    private ResponseStatusEnum status;

    /** Human-readable message. */
    private String message;

    /** HTTP status code (200, 400, 401, 403, 404, 409, 500). */
    private Integer httpStatus;

    /** Main response payload. */
    private T answer;

    /** Microservice that produced the response. */
    private String applicationProvider = "ms-security";

    /** Optional metadata (paging, correlation id, …). */
    private Object metadata;

    /** Server timestamp in ISO-8601 with offset. */
    private String serverDateTime;

    public GenericResponse(ResponseStatusEnum status, String message, Integer httpStatus, T answer) {
        this.status = status;
        this.message = message;
        this.httpStatus = httpStatus;
        this.answer = answer;
        this.applicationProvider = "ms-security";
        this.serverDateTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static <T> GenericResponse<T> success(T answer, String message, Integer httpStatus) {
        return new GenericResponse<>(ResponseStatusEnum.SUCCESS, message, httpStatus, answer);
    }

    public static <T> GenericResponse<T> error(String message, Integer httpStatus) {
        return new GenericResponse<>(ResponseStatusEnum.ERROR, message, httpStatus, null);
    }
}
