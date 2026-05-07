package beauty.beauty.reservation.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CursorResponse<T> {

    private final List<T> content;
    private final boolean hasMore;
    private final Long nextCursorId;  // 다음 요청 시 lastId로 사용 (마지막 항목 id)

    private CursorResponse(List<T> content, boolean hasMore, Long nextCursorId) {
        this.content = content;
        this.hasMore = hasMore;
        this.nextCursorId = nextCursorId;
    }

    public static <T> CursorResponse<T> of(List<T> content, boolean hasMore, Long nextCursorId) {
        return new CursorResponse<>(content, hasMore, nextCursorId);
    }
}
