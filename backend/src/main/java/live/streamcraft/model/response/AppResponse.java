package live.streamcraft.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppResponse<T> {
    private boolean status;
    private String message;
    private T data;

    public static <T> AppResponse<T> success(String message, T data) {
        return AppResponse.<T>builder()
                .status(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> AppResponse<T> success(String message) {
        return success(message, null);
    }

    public static <T> AppResponse<T> error(String message, T data) {
        return AppResponse.<T>builder()
                .status(false)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> AppResponse<T> error(String message){
    	return error(message, null);
    }
}
