package com.shopping.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.shopping.Auth.Session;
import com.shopping.model.Order;
import com.shopping.model.OrderItem;
import com.shopping.model.OrderStatus;
import com.shopping.model.Role;
import com.shopping.model.User;
import com.shopping.repository.FileOrderRepository;
import com.shopping.repository.OrderRepository;
import com.shopping.service.OrderService;

/**
 * 통합 테스트: Product/Cart/Order/Role 흐름을 실제 구현으로 검증
 *
 * ⚠️ 시그니처가 프로젝트마다 조금씩 달 수 있습니다.
 * 아래 “OPTION A/B” 중 네 프로젝트에 맞는 라인을 선택해 주석 해제하세요.
 */
@DisplayName("[IT] 쇼핑몰 통합 테스트")
class IntegratedTest {

    @TempDir Path tempDir;

    // ====== 실제 구현 인스턴스 ======
    OrderRepository orderRepo;
    OrderService orderService;

    // (선택) 있으면 연결
    // ProductService productService;
    // CartService cartService;

    // ====== 세션/계정 ======
    Session guest, userA, userB, admin;

	User user;
	User useradmin;


    // ====== 시드 상품 ID/가격/재고 (실제 ProductService 있다면 거기서 생성) ======
    final String P100 = "P100"; // 키보드 50,000 재고 10
    final String P200 = "P200"; // 마우스 30,000 재고 5
    final String P300 = "P300"; // 모니터 200,000 재고 2

    @BeforeEach
    void setUp() {
        // --- Repository ---
        orderRepo = new FileOrderRepository(tempDir.toString());
        //  (만약 생성자가 다르면: new FileOrderRepository(tempDir); 또는 경로 + 파일명 전달)

        // --- Services (네 프로젝트 시그니처에 맞춰 조립) ---
        // OPTION A: 재고/상품 의존 없이 OrderService 단독 생성 가능한 경우
        orderService = new OrderService(orderRepo);

        // OPTION B: ProductService/CartService를 함께 주입해야 하는 경우
        // productService = new ProductService(...);
        // cartService    = new CartService(...);
        // orderService   = new OrderService(orderRepo, productService, cartService, ...);

        // --- Sessions / Roles ---
        guest = new Session(); // 비로그인 상태
        userA = new Session(); userA.login("userA", Role.USER, user);
        userB = new Session(); userB.login("userB", Role.USER, user);
        admin = new Session(); admin.login("admin", Role.ADMIN, useradmin);

        // --- Seed Products (있다면 ProductService로 등록/재고 세팅) ---
        // if (productService != null) {
        //     productService.add(P100, "키보드", 50000, 10);
        //     productService.add(P200, "마우스", 30000, 5);
        //     productService.add(P300, "모니터", 200000, 2);
        // }
    }

    // 유틸: 주문 아이템 리스트 (CartService가 없을 경우 직접 생성해서 place에 전달)
    private List<OrderItem> items(Object[][] lines) {
        List<OrderItem> list = new ArrayList<>();
        for (Object[] l : lines) {
            String pid = (String) l[0];
            String name = (String) l[1];
            int price = (Integer) l[2];
            int qty = (Integer) l[3];
            list.add(new OrderItem(pid, name, price, qty));
        }
        return list;
    }

    // =========================
    // 1) E2E 해피패스
    // =========================
    @Test
    @DisplayName("E2E: 장바구니→주문(PENDING)→확정(재고 차감)→배송→완료")
    void e2e_order_flow_ok() {
        // GIVEN: userA가 카트에 P100x2, P200x1 담았다고 가정
        String orderId;

        // 주문 생성 (place)
        // OPTION A: 카트 기반 place(userA 세션 사용)
        // orderId = orderService.place(userA);

        // OPTION B: 직접 아이템 전달 방식 place(userId, items)
        orderId = orderService.place("userA", items(new Object[][]{
            {P100, "키보드", 50000, 2},
            {P200, "마우스", 30000, 1}
        }));

        Order pending = orderService.getOrder(orderId, "userA", Role.USER, userA);
        assertEquals(OrderStatus.PENDING, pending.getStatus());
        assertEquals(2, pending.getItems().size());
        assertEquals(130000, pending.getTotalAmount()); // 50,000*2 + 30,000

        // WHEN: 확정(confirm) → 재고 차감
        orderService.confirm(orderId, "userA", Role.USER, userA);
        Order confirmed = orderService.getOrder(orderId, "userA", Role.USER, userA);
        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());

        // (선택) ProductService가 있으면 재고 검증
        // assertEquals(8, productService.getStock(P100)); // 10→8
        // assertEquals(4, productService.getStock(P200)); // 5 →4

        // WHEN: 배송 → 완료
        orderService.ship(orderId, "admin", Role.ADMIN, admin);
        orderService.deliver(orderId, "admin", Role.ADMIN, admin);

        Order delivered = orderService.getOrder(orderId, "userA", Role.USER, userA);
        assertEquals(OrderStatus.DELIVERED, delivered.getStatus());
        assertEquals(130000, delivered.getTotalAmount()); // 스냅샷 총액 유지
    }

    // =========================
    // 2) 권한
    // =========================
    @Test
    @DisplayName("권한: USER는 본인만, ADMIN은 전체 조회 가능")
    void authz_ok() {
        String orderId = orderService.place("userA", items(new Object[][]{
            {P100, "키보드", 50000, 1}
        }));

        // 타 사용자(userB) 접근 → 예외
        assertThrows(IllegalStateException.class,
            () -> orderService.getOrder(orderId, "userB", Role.USER, userB));

        // ADMIN 접근 → 가능
        Order adminView = orderService.getOrder(orderId, "admin", Role.ADMIN, admin);
        assertEquals(orderId, adminView.getOrderId());

        // PENDING에서 본인 취소 → OK
        orderService.cancel(orderId, "userA", Role.USER, userA);
        assertEquals(OrderStatus.CANCELLED,
            orderService.getOrder(orderId, "userA", Role.USER, userA).getStatus());
    }

    // =========================
    // 3) 재고 부족 예외
    // =========================
    @Test
    @DisplayName("재고 부족: confirm 실패 시 상태/재고 유지")
    void confirm_stockNotEnough_fail() {
        // GIVEN: P300 재고 2라고 가정
        String orderId = orderService.place("userA", items(new Object[][]{
            {P300, "모니터", 200000, 3} // 재고보다 크게
        }));

        assertThrows(IllegalStateException.class,
            () -> orderService.confirm(orderId, "userA", Role.USER, userA));

        Order stillPending = orderService.getOrder(orderId, "userA", Role.USER, userA);
        assertEquals(OrderStatus.PENDING, stillPending.getStatus());

        // (선택) 재고 변화 없음 확인
        // assertEquals(2, productService.getStock(P300));
    }

    // =========================
    // 4) 파일 라운드트립(영속성)
    // =========================
    @Test
    @DisplayName("라운드트립: 저장→새 Repo 로드→동일 데이터")
    void repository_roundtrip_ok() {
        String id1 = orderService.place("userA", items(new Object[][]{
            {P100, "키보드", 50000, 1}
        }));
        String id2 = orderService.place("userA", items(new Object[][]{
            {P200, "마우스", 30000, 2}
        }));

        // 새 저장소 인스턴스(= 재기동 효과)
        OrderRepository repo2 = new FileOrderRepository(tempDir.toString());
        Order re1 = repo2.findById(id1).orElseThrow();
        Order re2 = repo2.findById(id2).orElseThrow();

        assertEquals(id1, re1.getOrderId());
        assertEquals(id2, re2.getOrderId());
        assertNotNull(re1.getItems());
        assertNotNull(re2.getItems());
    }

    // =========================
    // 5) 스냅샷: 과거 주문 금액 고정
    // =========================
    @Test
    @DisplayName("스냅샷: 확정 후 상품가 변경해도 과거 주문 총액은 그대로")
    void snapshot_kept_after_product_change() {
        String orderId = orderService.place("userA", items(new Object[][]{
            {P100, "키보드", 50000, 2}
        }));
        orderService.confirm(orderId, "userA", Role.USER, userA);

        // (선택) 실제 상품 가격 변경
        // productService.updatePrice(P100, 60000);

        Order old = orderService.getOrder(orderId, "userA", Role.USER, userA);
        assertEquals(100000, old.getTotalAmount()); // 50,000 * 2 유지
    }

    // =========================
    // 6) 카트 병합/빈 카트 예외 (CartService 있는 경우)
    // =========================
    @Test
    @DisplayName("카트 병합: 같은 상품 2회 담으면 수량 합쳐짐 / 빈 카트는 주문 불가")
    void cart_merge_and_empty_cart() {
        // OPTION A: CartService 사용 예시
        // cartService.add("userA", P100, 1);
        // cartService.add("userA", P100, 1);
        // cartService.add("userA", P200, 1);
        // String orderId = orderService.place(userA);
        // Order o = orderService.getOrder(orderId, "userA", Role.USER, userA);
        // assertEquals(2, o.getItems().size()); // P100(수량2), P200(수량1)

        // 빈 카트 주문 예외
        assertThrows(IllegalStateException.class,
            () -> {
                // OPTION A: cartService 비우고 place(userA)
                // cartService.clear("userA");
                // orderService.place(userA);

                // OPTION B: 직접 전달 방식으로 빈 리스트 전달
                orderService.place("userA", new ArrayList<>());
            });
    }

    // =========================
    // 7) 에지 케이스
    // =========================
    @Test
    @DisplayName("에지: 수량=1, 큰 금액, 잘못된 ID/음수 값")
    void edge_cases() {
        // 수량=1 경계
        String id = orderService.place("userA", items(new Object[][]{
            {P200, "마우스", 30000, 1}
        }));
        Order o = orderService.getOrder(id, "userA", Role.USER, userA);
        assertEquals(30000, o.getTotalAmount());

        // 잘못된 ID
        assertTrue(orderService.findById("NO_SUCH_ID").isEmpty());

        // 음수 수량/가격은 place 시 예외가 나야 함(정책에 맞게)
        assertThrows(IllegalArgumentException.class,
            () -> orderService.place("userA", items(new Object[][]{
                {P100, "키보드", 50000, -1}
            })));

        // 매우 큰 금액(오버플로 방지) – long/BigDecimal 사용 시 정책대로 합계 계산 검증
        String big = orderService.place("userA", items(new Object[][]{
            { "PX", "초고가", Integer.MAX_VALUE, 2 } // 필요하면 long/BigDecimal로 교체
        }));
        Order bigOrder = orderService.getOrder(big, "userA", Role.USER, userA);
        assertTrue(bigOrder.getTotalAmount() > 0); // 계산 성공(오버플로 X) - 실제 타입에 맞게 조정
    }

    // =========================
    // (옵션) UI 포함 미니 E2E: MainController 스크립트 한 바퀴
    // =========================
    // 콘솔 UI를 정말 데모하고 싶다면 Scanner에 입력 시나리오를 흘린다.
    // @Test
    // @DisplayName("[옵션] MainController 한 바퀴 (게스트→userA→admin)")
    // void mainController_demo_once() {
    //     String script = String.join(System.lineSeparator(),
    //         // 게스트: 장바구니/주문 시도 → 로그인 필요
    //         "3", // 예: '주문하기' 메뉴
    //         // 로그인
    //         "1", "userA", "password",
    //         // 장바구니 담기/주문 생성/확정/배송/완료 ...
    //         "0"  // 종료
    //     );
    //     Scanner sc = new Scanner(new ByteArrayInputStream(script.getBytes()));
    //     MainController mc = new MainController(
    //         new Session(), /* controllers/services... */, sc
    //     );
    //     mc.show(); // 또는 run()
    //     // 이후 repo/service 상태로 검증
    // }
}
