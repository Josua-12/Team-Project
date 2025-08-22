package com.shopping.controller;

import com.shopping.Auth.Session;
import com.shopping.model.Role;

import java.util.Scanner;

/**
 * 메인 컨트롤러 (콘솔)
 * - 비로그인 / 고객 / 관리자 상태에 따라 서로 다른 메인 메뉴 출력
 * - 4. 주문하기 -> OrderController.showCreateMenu()
 * - 5. 주문 내역 -> OrderController.showManageMenu()
 */
public class MainController {

    private final Session session;               // 로그인/권한 상태 공유
    private final Scanner sc = new Scanner(System.in);

    // 다른 컨트롤러들(필요한 것만 우선)
    private final OrderController orderController;
    private final UserController userController;
    private final ProductController productController;
    private final CartController cartController;
    private final AdminController adminController;

    public MainController(Session session,
                          OrderController orderController, 
                          UserController userController, 
                          ProductController productController,
                          CartController cartController,
                          AdminController adminController ) {
        this.session = session;
        this.orderController = orderController;
        this.userController = userController;
        this.productController = productController;
        this.cartController = cartController;
        this.adminController = adminController;
    }

    public void start() {
        while (true) {
        	try {
	            if (!session.isLoggedIn()) {
	                showGuestMenu();
	                String sel = sc.nextLine().trim();
	                switch (sel) {
	                    case "1" -> safeRun(() -> userController.register(), "회원가입");
	                    case "2" -> safeRun(() -> userController.login(), "로그인");
	                    // case "3" -> safeRun(() -> browseProducts(), "상품 둘러보기");
	                    case "4" -> { // 프로그램 종료
	                        System.out.println("프로그램을 종료합니다.");
	                        return;
	                    }
	                    default -> warn();
	                }
	            } else if (session.getRole() == Role.USER) {
	                showUserMenu(session.getUserId());
	                String sel = sc.nextLine().trim();
	                switch (sel) {
	                    // case "1" -> safeRun(() -> browseProducts(), "상품 둘러보기");
	                    // case "2" -> safeRun(() -> searchProducts(), "상품 검색");
	                    case "3" -> safeRun(() -> cartController.runCartMenu(session.getUserId()), "장바구니 관리");
	                    case "4" -> safeRun(() -> orderController.orderMenu(), "주문 메뉴 처리");
	                    case "5" -> safeRun(() -> orderController.orderHistoryMenu(), "주문 내역 처리");
	                    case "6" -> safeRun(() -> userController.myPage(session.getUser()), "마이페이지");
	                    case "7" -> safeRun(() -> userController.logout(), "로그아웃");
	                    default -> warn();
	                }
	            } else { // ADMIN
	                showAdminMenu();
	                String sel = sc.nextLine().trim();
	                switch (sel) {
	                    // case "1" -> safeRun(() -> browseProducts(), "상품 둘러보기");
	                    // case "2" -> safeRun(() -> searchProducts(), "상품 검색");
	                    case "3" -> safeRun(() -> cartController.runCartMenu(session.getUserId()), "장바구니 관리");
	                    case "4" -> safeRun(() -> orderController.orderMenu(), "주문 메뉴 처리");
	                    case "5" -> safeRun(() -> orderController.orderHistoryMenu(), "주문 내역 처리");
	                    case "6" -> safeRun(() -> userController.myPage(session.getUser()), "마이페이지");
	                    case "7" -> safeRun(() -> userController.logout(), "로그아웃");
	                    case "8" -> safeRun(() -> adminController.handleProductManagement(), "관리/상품 관리");
	                    case "9" -> safeRun(() -> adminController.handleUserManagement(), "관리/사용자 관리");
	                    default -> warn();
	                }
	            } 
        	} catch (java.util.NoSuchElementException e) {
                return; // ✅ 안전 탈출
            }
        }
    }

    // ───────────────────── 화면 출력 ─────────────────────

    private void showGuestMenu() {
        System.out.println();
        System.out.println("┌───────────────────────────────────┐");
        System.out.println("│   🔒  Java Shopping Mall          │");
        System.out.println("└───────────────────────────────────┘");
        System.out.println(" 1. 회원가입");
        System.out.println(" 2. 로그인");
        System.out.println(" 3. 상품 둘러보기");
        System.out.println(" 4. 프로그램 종료");
        System.out.print("\n메뉴를 선택하세요: ");
    }

    private void showUserMenu(String usernameOrId) {
        System.out.println();
        System.out.println("┌───────────────────────────────────┐");
        System.out.println("│   🔓  Java Shopping Mall          │");
        System.out.println("│   환영합니다, [" + usernameOrId + "]님!   │");
        System.out.println("└───────────────────────────────────┘");
        System.out.println(" 1. 상품 둘러보기");
        System.out.println(" 2. 상품 검색");
        System.out.println(" 3. 장바구니 관리");
        System.out.println(" 4. 주문하기");      // ← 2.4.1
        System.out.println(" 5. 주문 내역");    // ← 2.4.2
        System.out.println(" 6. 마이페이지");
        System.out.println(" 7. 로그아웃");
        System.out.print("\n메뉴를 선택하세요: ");
    }

    private void showAdminMenu() {
        System.out.println();
        System.out.println("┌───────────────────────────────────┐");
        System.out.println("│   🔧  Java Shopping Mall          │");
        System.out.println("│   [관리자 모드] 환영합니다!         │");
        System.out.println("└───────────────────────────────────┘");
        System.out.println(" 1. 상품 둘러보기");
        System.out.println(" 2. 상품 검색");
        System.out.println(" 3. 장바구니 관리");
        System.out.println(" 4. 주문하기");      // ← 2.4.1
        System.out.println(" 5. 주문 내역");    // ← 2.4.2
        System.out.println(" 6. 마이페이지");
        System.out.println(" 7. 로그아웃");
        System.out.println(" 8. [관리] 상품 관리");
        System.out.println(" 9. [관리] 사용자 관리");
        System.out.print("\n메뉴를 선택하세요: ");
    }

    // ───────────────────── 연결 포인트 ─────────────────────
//    // 회원가입
//    private void signUp() {
//        userController.register();  
//    }
//
//    // 로그인
//    private void login() {
//        userController.login();  
//    }
//
//    // 상품 둘러보기
//    private void browseProducts() {
//        productController.list();
//    }
//
//    // 상품 검색
//    private void searchProducts() {
//        productController.search();
//    }
//
//    // 장바구니 관리
//    private void manageCart() {
//        cartController.menu(session);
//    }
//
//      //마이페이지
//      private void myPage() {
//    	  userController.myPage(session.getUser());
//      }
//
//    // 로그아웃
//    private void logout() {
//        session.logout();
//        System.out.println("로그아웃 되었습니다.");
//    }
//
//    // 관리자 기능
//    private void adminManageProducts() {
//        adminController.manageProducts();
//    }
//
//    private void adminManageUsers() {
//        adminController.manageUsers();
//    }

    private void warn() { System.out.println("잘못된 선택입니다."); }
    
    private void safeRun(Runnable r, String label) {
        try { r.run(); }
        catch (Exception e) { System.out.println("[경고] " + label + " 중 오류: " + e.getMessage()); }
    }
}