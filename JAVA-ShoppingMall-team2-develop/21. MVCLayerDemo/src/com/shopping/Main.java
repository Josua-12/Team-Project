package com.shopping;

import com.shopping.Auth.Session;
import com.shopping.controller.*;
import com.shopping.model.Role;
import com.shopping.repository.*;
import com.shopping.service.*;

import java.util.Scanner;
/*
 * == 메인 메뉴 ==
 * 로그인되지 않음
 * 
 * 1. 사용자관리
 * 2. 상품 보기
 * 3. 주문 관리
 * 0. 종료
 * 
 * 선택 : 1
 * 
 * == 사용자관리 메뉴 ==
 * 1. 회원가입
 * 2. 로그인
 * 3. 로그아웃
 * 4. 내 정보 보기
 * 0. 돌아가기
 * 선택: 1
 * 
 * == 회원가입 ==
 * 아이디 (3자 이상, 영문/숫자): 
 * 패스워드 (4자 이상): 
 * 이름: 
 * 
 * 새로 생성됩니다.
 * 새 사용자 등록!
 * 회원가입 성공!
 * 
 *  환영합니다. [사용자]님!
 *  초기 잔액: 10000.0원
 *  
 * 
 * 
 */

/*
 * 	* 구현 완료 순서
 * 		1) Model Layer
 * 			- User.java 
 * 			- Serializable 구현
 * 		2) Pesistance Layer
 * 			- FileManager.java
 * 		3) Repository Layer
 * 			- UserReposiotry.java
 * 			- CRUD 메서드 구현
 * 		4) Service Layer
 * 			- 비즈니스 로직 적용
 * 			- 예외 처리 구현 
 * 		5) Util Layer
 * 			- Constant.java
 * 		6) Controller Layer
 * 			- UserController.java 		
 * 											- 회원가입 테스트
 * 		7) Main Application
 * 			- Main.java			
 */
public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // 영속성 계층 구현 인스턴스 생성
        FileUserRepository userRepo = new FileUserRepository();
        FileAdminRepository adminRepo = new FileAdminRepository();
        FileCartRepository cartRepo = new FileCartRepository();
        OrderRepository orderRepo = new DefaultFileOrderRepository("data/orders.dat");
        ProductRepository productRepo = new FileProductRepository();
        FileOrderRepository fileOrderRepo = (FileOrderRepository) orderRepo;

        // 서비스 계층 생성
        AuthService authService = new AuthService(userRepo, adminRepo);
        UserService userService = new UserService(userRepo);
        ProductService productService = new ProductService(productRepository);
        OrderService orderService = new OrderService(orderRepo, productRepo, fileOrderRepo);
        AdminService adminService = new AdminService(userRepo);
        ReportService reportService = new ReportService(orderRepo);

        // 세션 생성 (로그인 상태 공유)
        Session session = new Session();

        // 컨트롤러 생성
        UserController userController = new UserController(session);
        ProductController productController = new ProductController(productService, scanner);
        OrderController orderController = new OrderController(orderService, session, scanner);
        CartController cartController = new CartController(productRepo, cartRepo);
        AdminController adminController = new AdminController(userService, orderService, productService, reportService);
        ReportController reportController = new ReportController(reportService);

        // 메인 컨트롤러 역할 수행
        MainController mainController = new MainController(session, orderController, userController, productController, cartController, adminController);

        // 애플리케이션 시작
        mainController.start();

        scanner.close();
    }
}