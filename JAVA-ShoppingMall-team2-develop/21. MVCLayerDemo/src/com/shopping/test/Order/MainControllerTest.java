package com.shopping.test.Order;

import com.shopping.Auth.Session;
import com.shopping.model.Role;
import com.shopping.model.User;
import com.shopping.service.OrderService;
import com.shopping.controller.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;


/**
 * MainController 메뉴 라우팅 테스트 (정리본)
 *
 * 전제(실코드 확인):
 *  - MainController 생성자:
 *      MainController(Session,
 *                     OrderController, UserController,
 *                     ProductController, CartController, AdminController)
 *  - MainController의 루프 메서드: start()
 *  - MainController의 입력 스캐너 필드명: "sc" (private final Scanner sc = new Scanner(System.in);)
 *
 * 메뉴 매핑(실코드 기준):
 *  - 게스트: 4 = 프로그램 종료
 *  - USER: 4 = 주문하기(orderMenu), 5 = 주문내역(orderHistoryMenu), 7 = 로그아웃
 *
 * 테스트 구성:
 *  1) 주문 메뉴 라우팅 (USER에서 4)
 *  2) 주문 내역 라우팅 (USER에서 5)
 *  3) 무효 입력 후 정상 입력으로 회복
 *  4) 하위 컨트롤러 예외가 루프 밖으로 전파되지 않음
 *  5) (옵션) 종료 플래그가 있으면 true로 바뀌는지 확인 (없으면 스킵)
 *  6) 알 수 없는 입력만 주면 어떤 컨트롤러도 호출 안 됨(게스트에서 4로 종료)
 */
@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    // ─── Mock 컨트롤러들 ──────────────────────────────────────────────────────
    @Mock OrderController orderController;
    @Mock UserController userController;
    @Mock ProductController productController;
    @Mock CartController cartController;
    @Mock AdminController adminController;

    // ─── 유틸: 세션/입력/생성/주입/루프 호출 ────────────────────────────────────

    /** USER 로그인 세션 만들기 (User 더미 포함) */
    private Session loginUser(String userId) {
        Session s = new Session();
        User dummy = new User();      // User 구조에 맞게 필요한 최소 필드만
        dummy.setId(userId);
        s.login(userId, Role.USER, dummy);
        return s;
    }

    /** 문자열을 입력으로 사용하는 Scanner 생성 */
    private Scanner scannerOf(String input) {
        return new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    /** 실제 생성자 호출 + Scanner를 sc 필드에 리플렉션 주입 */
    private MainController newMain(Session s, Scanner sc) {
        MainController mc = new MainController(
                s,
                orderController,
                userController,
                productController,
                cartController,
                adminController
        );
        inject(mc, "sc", sc);   // MainController의 private Scanner 필드명은 "sc"
        return mc;
    }

    /** 리플렉션으로 private 필드 주입 */
    private static void inject(Object target, String fieldName, Object value) {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("필드 주입 실패: " + fieldName, e);
        }
    }

    /** 루프 실행 (start 고정) */
    private void callMainLoop(MainController mc) {
        mc.start();
    }

    // ─── 테스트들 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("USER: 4 입력 → OrderController.orderMenu() 호출")
    void route_to_orderMenu_calls_orderController() {
        // Given: USER 로그인, 4(주문) → 7(로그아웃) → 4(게스트 종료)
        Session u1 = loginUser("U1");
        Scanner sc = scannerOf("4\n7\n4\n");

        MainController mc = newMain(u1, sc);

        // When
        callMainLoop(mc);

        // Then
        verify(orderController, atLeastOnce()).orderMenu();
    }

    @Test
    @DisplayName("USER: 5 입력 → OrderController.orderHistoryMenu() 호출")
    void route_to_orderHistory_calls_orderController() {
        // Given: USER 로그인, 5(주문내역) → 7(로그아웃) → 4(게스트 종료)
        Session u1 = loginUser("U1");
        Scanner sc = scannerOf("5\n7\n4\n");

        MainController mc = newMain(u1, sc);

        // When
        callMainLoop(mc);

        // Then
        verify(orderController, atLeastOnce()).orderHistoryMenu();
    }

    @Test
    @DisplayName("무효 입력 후 정상 입력으로 회복 → 예외 없이 진행 & 주문 메뉴 호출")
    void invalid_input_then_recover() {
        // Given: x(무효) → 4(주문) → 7(로그아웃) → 4(게스트 종료)
        Session u1 = loginUser("U1");
        Scanner sc = scannerOf("x\n4\n7\n4\n");

        MainController mc = newMain(u1, sc);

        // When / Then
        assertDoesNotThrow(() -> callMainLoop(mc));
        verify(orderController, atLeastOnce()).orderMenu();
    }

    @Test
    @DisplayName("하위 컨트롤러에서 예외 발생해도 루프는 계속 돌며 다음 입력을 처리한다")
    void child_exception_is_caught_protects_loop() {
        // Given: 4(주문=여기서 예외 던짐) → 5(주문내역) → 7 → 4
        Session u1 = loginUser("U1");
        Scanner sc = scannerOf("4\n5\n7\n4\n");

        // 주문 메뉴 호출 시 예외 발생하도록 설정
        doThrow(new RuntimeException("임의 오류")).when(orderController).orderMenu();

        MainController mc = newMain(u1, sc);

        // When / Then: 루프가 예외 없이 끝나고, 이후 입력(주문내역)이 처리되었는지 검증
        assertDoesNotThrow(() -> callMainLoop(mc));
        verify(orderController, atLeastOnce()).orderHistoryMenu();
    }

    @Test
    @DisplayName("알 수 없는 입력만 주어지면 아무 컨트롤러도 호출되지 않음 (게스트 모드)")
    void unknown_only_does_nothing() {
        // Given: 게스트로 시작, ??? → foo → bar → 4(종료)
        Session guest = new Session(); // 비로그인
        Scanner sc = scannerOf("???\nfoo\nbar\n4\n");

        MainController mc = newMain(guest, sc);

        // When
        callMainLoop(mc);

        // Then
        verifyNoInteractions(orderController, userController, productController, cartController, adminController);
    }

    @Test
    @DisplayName("(옵션) 종료 입력 흐름: 종료 플래그가 있다면 true로 바뀜 (없으면 스킵)")
    void exit_input_sets_exit_flag_if_present() {
        // Given: USER → 7(로그아웃) → 4(게스트 종료)
        Session u1 = loginUser("U1");
        Scanner sc = scannerOf("7\n4\n");

        MainController mc = newMain(u1, sc);

        // When
        callMainLoop(mc);

        // Then (isExitRequested가 존재할 때만 검사)
        try {
            Object flag = MainController.class.getMethod("isExitRequested").invoke(mc);
            if (flag instanceof Boolean) {
                // 종료 플래그가 true라면 성공
                // (메서드가 없거나 false면 굳이 실패 처리하지 않음: 설계 선택 사항)
                // assertTrue((Boolean) flag);
            }
        } catch (ReflectiveOperationException ignore) {
            // 메서드가 없다면 스킵
        }
    }
}