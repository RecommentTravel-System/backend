package org.example.wayveesystem.common.exception;

import lombok.Getter;

@Getter
public class ExternalMapServiceException extends AppException {

    public ExternalMapServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExternalMapServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode);
        if (cause != null) {
            initCause(cause);
        }
    }
}
