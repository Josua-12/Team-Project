package com.shopping.test.Order;

import com.shopping.model.Order;
import com.shopping.model.OrderItem;
import com.shopping.model.OrderStatus;
import com.shopping.repository.FileOrderRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 파일 기반 구현(FileOrderRepository)의 단위/통합 경계 테스트
 * - @TempDir 를 사용하여 빠른 파일 라운드트립 검증
 * - 원자성(임시파일→move), 손상 파일 로딩 정책, 상태전이/삭제/필터 등을 확인
 *
 * 주의: FileOrderRepository는 추상 클래스로 제공되므로, 테스트 내에서 익명 클래스로 인스턴스화.
 */
public class FileOrderRepositoryTest {

    @TempDir
    Path tempDir;
    Path store;

    private FileOrderRepository repo() {
        return new FileOrderRepository(store.toString()) {};
    }

    private static OrderItem it(String id, String name, int price, int qty) {
        return new OrderItem(id, name, price, qty);
    }
    private static Order newOrder(String userId, OrderItem... items) {
        Order o = new Order();
        o.setUserId(userId);
        for (OrderItem i : items) o.addItem(i);
        return o;
    }

    @BeforeEach
    void setUp() {
        store = tempDir.resolve("orders.dat");
    }

    @Test
    @DisplayName("라운드트립: save → 새 인스턴스로 로드 → findById 동일 데이터")
    void roundTrip_save_reload_ok() {
        FileOrderRepository r1 = repo();
        Order o = newOrder("u1", it("P1","n1",300,2));
        o.setOrderDate(LocalDateTime.of(2025,8,3,9,30));
        r1.save(o);
        String savedId = o.getOrderId();
        assertNotNull(savedId);

        // 새 인스턴스(프로세스 재기동 효과)로 파일 재로딩
        FileOrderRepository r2 = repo();
        Order loaded = r2.findById(savedId).orElseThrow();

        assertEquals("u1", loaded.getUserId());
        assertEquals(1, loaded.getItems().size());
        assertEquals(600, loaded.getTotalPrice());
        assertEquals(LocalDate.of(2025,8,3), loaded.getOrderDate().toLocalDate());
        assertEquals(OrderStatus.PENDING, loaded.getStatus());
    }

    @Test
    @DisplayName("여러 건 저장/조회: 사용자별 조회 정확")
    void findByUser_ok() {
        FileOrderRepository r = repo();
        r.save(newOrder("u1", it("A","a",100,1)));
        r.save(newOrder("u2", it("B","b",100,1)));
        r.save(newOrder("u1", it("C","c",100,1)));

        List<Order> u1 = r.findByUserId("u1");
        assertEquals(2, u1.size());
        List<Order> u2 = r.findByUserId("u2");
        assertEquals(1, u2.size());
    }

    @Test
    @DisplayName("기간별 조회: from/to 경계 포함")
    void findByDateRange_ok() {
        FileOrderRepository r = repo();
        Order a = newOrder("u", it("P","n",100,1)); a.setOrderDate(LocalDateTime.of(2025,8,1,0,0));
        Order b = newOrder("u", it("P","n",100,1)); b.setOrderDate(LocalDateTime.of(2025,8,10,0,0));
        Order c = newOrder("u", it("P","n",100,1)); c.setOrderDate(LocalDateTime.of(2025,8,20,0,0));
        r.save(a); r.save(b); r.save(c);

        List<Order> mid = r.findByDateRange(LocalDate.of(2025,8,10), LocalDate.of(2025,8,20));
        // 경계 포함 → b, c
        assertEquals(2, mid.size());
    }

    @Test
    @DisplayName("상태 전이: 합법 전이만 OK, 불법 전이는 예외")
    void updateStatus_transitions() {
        FileOrderRepository r = repo();
        Order o = newOrder("u1", it("P","n",100,1));
        r.save(o);

        assertTrue(r.updateStatus(o.getOrderId(), OrderStatus.CONFIRMED)); // PENDING → CONFIRMED
        assertThrows(IllegalStateException.class,
                () -> r.updateStatus(o.getOrderId(), OrderStatus.PENDING)); // 되돌리기 불가
    }

    @Test
    @DisplayName("삭제: delete 후 새 인스턴스 로드해도 없음")
    void delete_persists() {
        FileOrderRepository r1 = repo();
        Order o = newOrder("u1", it("P","n",100,1));
        r1.save(o);
        String id = o.getOrderId();
        assertTrue(r1.delete(id));

        // 새 인스턴스에서 파일 다시 읽어도 삭제 상태 유지
        FileOrderRepository r2 = repo();
        Optional<Order> after = r2.findById(id);
        assertTrue(after.isEmpty());
    }

    @Test
    @DisplayName("ID 생성기: O1 → O2 → O3... 증가/유일")
    void nextId_increasing() {
        FileOrderRepository r = repo();
        Order a = newOrder("u", it("P","n",10,1)); r.save(a);
        Order b = newOrder("u", it("P","n",10,1)); r.save(b);
        assertEquals("O1", a.getOrderId());
        assertEquals("O2", b.getOrderId());
    }

    @Test
    @DisplayName("원자성/무결성: 저장 후 임시파일(.tmp)이 남지 않음")
    void atomic_save_tempCleared() throws IOException {
        FileOrderRepository r = repo();
        r.save(newOrder("u", it("P","n",10,1)));

        Path tmp = Paths.get(store.toString() + ".tmp");
        assertFalse(Files.exists(tmp), "임시 파일이 남아있으면 안 됨");
        assertTrue(Files.exists(store), "본 파일은 존재해야 함");
    }

    @Test
    @DisplayName("파일 손상 대응: 손상된 파일이면 로딩 시 RuntimeException 발생")
    void corrupted_file_throws() throws Exception {
        // 손상 파일 작성
        Files.write(store, "NOT_A_SERIALIZED_MAP".getBytes());
        assertThrows(RuntimeException.class, () -> repo(), "손상 파일은 로드 실패해야 함");
    }

    @Test
    @DisplayName("방어적 복사 정책(참고): save 직후 원본 객체를 수정해도, '이미 파일에 기록된 스냅샷'은 새 인스턴스에서 유지")
    void snapshot_persisted_on_save() {
        FileOrderRepository r1 = repo();
        Order o = newOrder("u", it("P","n",100,1));
        r1.save(o); // 이 시점의 상태가 파일에 기록됨
        String id = o.getOrderId();

        // 메모리상의 동일 객체를 수정
        o.addItem(it("Q","qq",50,1));
        // r1.persist()를 다시 부르는 동작이 없다면 파일은 이전 스냅샷을 유지

        FileOrderRepository r2 = repo(); // 파일 재로딩
        Order loaded = r2.findById(id).orElseThrow();
        assertEquals(1, loaded.getItems().size(), "save 직후 기록된 스냅샷(1라인) 유지");
        assertEquals(100, loaded.getTotalPrice());
    }
}