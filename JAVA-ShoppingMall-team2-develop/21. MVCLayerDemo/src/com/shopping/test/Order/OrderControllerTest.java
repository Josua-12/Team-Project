package com.shopping.test.Order;

import com.shopping.Auth.Session;
import com.shopping.controller.OrderController;
import com.shopping.model.Order;
import com.shopping.model.OrderItem;
import com.shopping.model.OrderStatus;
import com.shopping.model.Role;
import com.shopping.model.User;
import com.shopping.service.OrderService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * OrderController 메뉴/명령 → 서비스 라우팅 & 세션전달 & 유효성/예외 처리 테스트
 * - Scanner에 ByteArrayInputStream을 주입해 콘솔 입력을 흉내냄 (I/O는 통합 테스트로 분리)
 * - 서비스 호출 경로/인자 검증, 잘못된 입력 방어, 서비스 예외 → 사용자 안내 흐름까지 확인
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    OrderService service;

    // ───────────────────────────────────────────────────────────────────────────
    // 헬퍼: 로그인된 세션과, 원하는 입력을 가진 컨트롤러 생성
    // ───────────────────────────────────────────────────────────────────────────
    private Session loginUser(String userId) {
        Session s = new Session();
        // User는 최소한 id만 있는 더미 객체 생성해도 OK
        User dummy = new User();
        dummy.setId(userId);       // 👈 User 클래스에 맞게 필드 세팅 (없으면 생성자 이용)

        s.login(userId, Role.USER, dummy);
        return s;
    }

    private OrderController newControllerWithInput(OrderService svc, Session session, String input) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        Scanner sc = new Scanner(in);
        // 실코드: public OrderController(OrderService svc, Session session, Scanner sc)
        return new OrderController(svc, session, sc);  // 👈 생성자 시그니처 다르면 맞춰 변경
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 1) orderMenu: 확정(4) → confirmOrder(orderId, userId, role, session)
    // ───────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("orderMenu: 4 선택 → confirmOrder(orderId) 호출")
    void orderMenu_confirm_calls_service() {
        // Given: 입력 시퀀스 [4][개행][O1][개행]
        Session u1 = loginUser("U1");
        OrderController c = newControllerWithInput(service, u1, "4\nO1\n");

        doNothing().when(service).confirmOrder(eq("O1"), eq("U1"), eq(Role.USER), same(u1));

        // When
        c.orderMenu();

        // Then
        verify(service, times(1)).confirmOrder("O1", "U1", Role.USER, u1);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 2) orderHistoryMenu: 목록(1) → listOrders(userId, role)
    // ───────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("orderHistoryMenu: 1 선택 → listOrders(userId, role) 호출")
    void orderHistoryMenu_list_calls_service() {
        // Given: 입력 [1][개행]
        Session u1 = loginUser("U1");
        OrderController c = newControllerWithInput(service, u1, "1\n");

        when(service.listOrders("U1", Role.USER)).thenReturn(List.of());

        // When
        c.orderHistoryMenu();

        // Then
        verify(service, times(1)).listOrders("U1", Role.USER);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 3) orderHistoryMenu: 상세(2) → getOrder(orderId, userId, role, session)
    // ───────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("orderHistoryMenu: 2 선택 → getOrder(orderId) 호출")
    void orderHistoryMenu_detail_calls_service() {
        // Given: 입력 [2][개행][O9][개행]
        Session u1 = loginUser("U1");
        OrderController c = newControllerWithInput(service, u1, "2\nO9\n");

        when(service.getOrder(eq("O9"), eq("U1"), eq(Role.USER), same(u1)))
                .thenReturn(new Order()); // 👈 필요 시 빌더/생성자 맞춤

        // When
        c.orderHistoryMenu();

        // Then
        verify(service, times(1)).getOrder("O9", "U1", Role.USER, u1);
    }

    @Test
    @DisplayName("orderHistoryMenu: 2(상세)에서 인자 캡처로 orderId 정확성 확인")
    void orderHistoryMenu_detail_argument_capture() {
        // Given: 입력 [2][개행][O777][개행]
        Session u1 = loginUser("U1");
        OrderController c = newControllerWithInput(service, u1, "2\nO777\n");

        when(service.getOrder(anyString(), anyString(), any(), any())).thenReturn(new Order());

        // When
        c.orderHistoryMenu();

        // Then
        ArgumentCaptor<String> idCap = ArgumentCaptor.forClass(String.class);
        verify(service).getOrder(idCap.capture(), eq("U1"), eq(Role.USER), same(u1));
        assertEquals("O777", idCap.getValue());
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 4) orderHistoryMenu: 취소(3) → cancelOrder(orderId, userId, role)
    // ───────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("orderHistoryMenu: 3 선택 → cancelOrder(orderId) 호출")
    void orderHistoryMenu_cancel_calls_service() {
        // Given: 입력 [3][개행][O5][개행]
        Session u1 = loginUser("U1");
        OrderController c = newControllerWithInput(service, u1, "3\nO5\n");

        doNothing().when(service).cancelOrder("O5", "U1", Role.USER);

        // When
        c.orderHistoryMenu();

        // Then
        verify(service, times(1)).cancelOrder("O5", "U1", Role.USER);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 5) 유효성/방어: 잘못된 메뉴 입력 → 어떤 서비스도 호출하지 않음
    // ───────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("잘못된 메뉴 입력 시 서비스 미호출 (방어적 분기)")
    void invalid_menu_input_does_not_call_service() {
        // Given: 숫자 아님, 범위 밖 등. 예: "x\n99\n-1\n"
        Session u1 = loginUser("U1");
        OrderController c = newControllerWithInput(service, u1, "x\n");

        // When
        assertDoesNotThrow(c::orderHistoryMenu);

        // Then
        verifyNoInteractions(service);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // 6) 서비스 예외: 서비스에서 던진 예외를 컨트롤러가 잡아 사용자 안내 후 계속
    // ───────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("서비스 예외 발생 시 컨트롤러가 잡아 안내 (예: confirm 실패)")
    void service_exception_is_caught_and_reported() {
        // Given: [4][개행][OX][개행] → confirm 경로에서 예외
        Session u1 = loginUser("U1");
        OrderController c = newControllerWithInput(service, u1, "4\nOX\n");

        doThrow(new IllegalStateException("재고부족"))
                .when(service).confirmOrder(eq("OX"), eq("U1"), eq(Role.USER), same(u1));

        // When/Then: 컨트롤러 내에서 try/catch로 안내만 하고, 테스트에는 예외 비전파
        assertDoesNotThrow(c::orderMenu);
        verify(service).confirmOrder("OX", "U1", Role.USER, u1);
    }
}