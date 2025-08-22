package com.shopping.test.Order;

import com.shopping.model.Order;
import com.shopping.model.OrderItem;
import com.shopping.model.OrderStatus;
import com.shopping.repository.OrderRepository;

import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderRepository 인터페이스의 "공통 규약"을 검증하는 컨트랙트 테스트.
 * - 임시 InMemory 구현에 대해 규약을 테스트하여, 다른 구현(File, DB...)에도 동일 규약을 요구할 수 있게 함.
 *
 * 방어적 복사 정책은 구현별로 다를 수 있으므로, 여기서는
 * "저장 직후 재조회한 데이터가 일관됨"과 "ID는 유일/증가"에 초점을 둔다.
 */
public class OrderRepositoryTest {

    /** 테스트 전용 InMemory 구현 */
	static class InMemoryOrderRepository implements OrderRepository {
	    private final Map<String, Order> map = new ConcurrentHashMap<>();
	    private long seq = 0L;

	    @Override
	    public synchronized void save(Order order) {
	        String id = order.getOrderId();
	        if (id == null || id.isBlank()) {
	            id = nextId();
	            order.setOrderId(id);
	        }
	        map.put(id, order); // 방어적 복사가 필요하면 여기서 복사 정책 적용
	    }

	    @Override
	    public synchronized Optional<Order> findById(String orderId) {
	        return Optional.ofNullable(map.get(orderId));
	    }

	    @Override
	    public synchronized List<Order> findAll() {
	        return new ArrayList<>(map.values());
	    }

	    @Override
	    public synchronized List<Order> findByUserId(String userId) {
	        List<Order> result = new ArrayList<>();
	        for (Order o : map.values()) {
	            if (Objects.equals(o.getUserId(), userId)) result.add(o);
	        }
	        return result;
	    }

	    @Override
	    public synchronized List<Order> findByStatus(OrderStatus status) {
	        List<Order> result = new ArrayList<>();
	        for (Order o : map.values()) {
	            if (o.getStatus() == status) result.add(o); // enum 비교는 ==
	        }
	        return result;
	    }

	    @Override
	    public synchronized List<Order> findByDateRange(LocalDate from, LocalDate to) {
	        List<Order> result = new ArrayList<>();
	        for (Order o : map.values()) {
	            LocalDate d = o.getOrderDate().toLocalDate();
	            boolean gteFrom = (from == null) || !d.isBefore(from); // from 이상
	            boolean lteTo   = (to   == null) || !d.isAfter(to);    // to 이하
	            if (gteFrom && lteTo) result.add(o);
	        }
	        return result;
	    }

	    @Override
	    public synchronized boolean updateStatus(String orderId, OrderStatus newStatus) {
	        Order o = map.get(orderId);
	        if (o == null) return false;
	        o.changeStatus(newStatus); // 전이 검증은 도메인에 위임
	        return true;
	    }

	    @Override
	    public synchronized boolean delete(String orderId) {
	        return map.remove(orderId) != null;
	    }

	    @Override
	    public synchronized String nextId() {
	        return "O" + (++seq); // O1, O2, ...
	    }
	}

    private OrderRepository repo;

    @BeforeEach
    void setUp() { repo = new InMemoryOrderRepository(); }

    private static OrderItem it(String id, String name, int price, int qty) {
        return new OrderItem(id, name, price, qty);
    }
    private static Order newOrder(String userId, OrderItem... items) {
        Order o = new Order();
        o.setUserId(userId);
        for (OrderItem i : items) o.addItem(i);
        return o;
    }

    @Test
    @DisplayName("ID 할당: null/blank면 O1, O2... 유일/증가")
    void idAssignment_uniqueIncreasing() {
        Order o1 = newOrder("u1", it("P1","n1",100,1));
        Order o2 = newOrder("u1", it("P2","n2",200,1));

        repo.save(o1);
        repo.save(o2);

        assertEquals("O1", o1.getOrderId());
        assertEquals("O2", o2.getOrderId());
        assertNotEquals(o1.getOrderId(), o2.getOrderId());
    }

    @Test
    @DisplayName("save 후 findById: 필드 동등성 (userId/items/total/status/date)")
    void save_then_findById_equals() {
        Order o = newOrder("uA", it("P1","n1",300,2));
        o.setOrderDate(LocalDateTime.of(2025,8,1,10,0));
        repo.save(o);

        Order loaded = repo.findById(o.getOrderId()).orElseThrow();
        assertEquals("uA", loaded.getUserId());
        assertEquals(1, loaded.getItems().size());
        assertEquals(600, loaded.getTotalPrice());
        assertEquals(OrderStatus.PENDING, loaded.getStatus());
        assertEquals(LocalDate.of(2025,8,1), loaded.getOrderDate().toLocalDate());
    }

    @Test
    @DisplayName("findByUserId: 사용자별 필터 정확")
    void findByUser() {
        repo.save(newOrder("u1", it("P1","n1",100,1)));
        repo.save(newOrder("u2", it("P2","n2",100,1)));
        repo.save(newOrder("u1", it("P3","n3",100,1)));

        List<Order> u1 = repo.findByUserId("u1");
        assertEquals(2, u1.size());
        List<Order> u2 = repo.findByUserId("u2");
        assertEquals(1, u2.size());
    }

    @Test
    @DisplayName("findByDateRange: 날짜 범위 필터 정확")
    void findByDateRange_ok() {
        Order a = newOrder("u", it("P","n",100,1)); a.setOrderDate(LocalDateTime.of(2025,8,1,0,0));
        Order b = newOrder("u", it("P","n",100,1)); b.setOrderDate(LocalDateTime.of(2025,8,10,0,0));
        Order c = newOrder("u", it("P","n",100,1)); c.setOrderDate(LocalDateTime.of(2025,8,20,0,0));
        repo.save(a); repo.save(b); repo.save(c);

        List<Order> mid = repo.findByDateRange(LocalDate.of(2025,8,5), LocalDate.of(2025,8,15));
        assertEquals(1, mid.size());
        assertEquals(b.getOrderId(), mid.get(0).getOrderId());
    }

    @Test
    @DisplayName("updateStatus: 합법 전이만 허용, 불법 전이는 예외")
    void updateStatus_transitions() {
        Order o = newOrder("u1", it("P","n",100,1));
        repo.save(o);

        assertTrue(repo.updateStatus(o.getOrderId(), OrderStatus.CONFIRMED)); // PENDING→CONFIRMED OK
        assertThrows(IllegalStateException.class, () -> repo.updateStatus(o.getOrderId(), OrderStatus.PENDING)); // 되돌리기 불가
    }

    @Test
    @DisplayName("delete: 삭제 후 재조회 불가")
    void delete_ok() {
        Order o = newOrder("u1", it("P","n",100,1));
        repo.save(o);
        assertTrue(repo.delete(o.getOrderId()));
        assertTrue(repo.findById(o.getOrderId()).isEmpty());
    }
}
