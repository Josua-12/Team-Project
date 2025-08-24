package com.shopping.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import com.shopping.Auth.Session;
import com.shopping.model.Cart;
import com.shopping.model.Order;
import com.shopping.model.OrderItem;
import com.shopping.model.Product;
import com.shopping.model.Role;
import com.shopping.model.User;
import com.shopping.repository.CartRepository;
import com.shopping.service.OrderService;
import com.shopping.service.ProductService;

public class OrderController {

    private final OrderService orderService;
    private final ProductService productService; // ProductService 추가 2025.08.24 17:27 조수아
    private final CartRepository cartRepository; // CartRepository 추가 2025.08.24 17:27 조수아
    private final Session session;
    private final Scanner sc;
    

    public OrderController(OrderService orderService, Session session, 
    		ProductService productService,CartRepository cartRepository,Scanner sc) {
        this.orderService = orderService;
        this.session = session;
        this.productService = productService;
        this.cartRepository = cartRepository;
        this.sc = sc;
    }

    // 주문 메뉴
    public void orderMenu() {
        System.out.println("--- 주문 생성 ---");
        System.out.println("1. 장바구니 상품 전체 주문");
        System.out.println("2. 개별 상품 즉시 구매");
        System.out.println("3. 배송 주소 입력");
        // 결재 -> 결제 오타 수정 2025.08.24 17:49 조수아
        System.out.println("4. 주문 확인 및 최종 결제");
        // 0. 뒤로 가기 추가 2025.08.24 17:49 조수아
        System.out.println("0. 뒤로 가기");
        // 메뉴를 선택하세요 추가 2025.08.24 17:57 조수아
        System.out.print("\n메뉴를 선택하세요: ");
        

        if (!sc.hasNextInt()) {
            if (sc.hasNext()) {
				sc.next(); // 잘못된 토큰 소비
			}
            System.out.println("숫자를 입력해 주세요.");
            return; // 서비스 호출 없이 종료
        }
        int choice = sc.nextInt();
        sc.nextLine();

        switch (choice) {
            case 1 -> placeOrderFromCart();
            case 2 -> placeOrderSingle();
            case 3 -> inputAddress();
            case 4 -> confirmOrder();
            case 0 -> {
            	System.out.println("상위 메뉴로 돌아갑니다.");
            	return;
            }
            default -> System.out.println("잘못된 선택입니다.");
        }
    }

    // 주문 내역 메뉴
    public void orderHistoryMenu() {
        System.out.println("--- 주문 관리 ---");
        System.out.println("1. 주문 내역 조회");
        System.out.println("2. 주문 상세 정보 조회");
        System.out.println("3. 주문 취소");
        System.out.println("0. 뒤로 가기");
        // 메뉴를 선택하세요 추가 2025.08.24 17:57 조수아
        System.out.print("\n메뉴를 선택하세요: ");

        if (!sc.hasNextInt()) {
            if (sc.hasNext()) {
				sc.next(); // 잘못된 토큰 소비
			}
            System.out.println("숫자를 입력해 주세요.");
            return; // 서비스 호출 없이 종료
        }
        int choice = sc.nextInt();
        sc.nextLine();

        switch (choice) {
            case 1 -> listOrders();
            case 2 -> getOrderDetail();
            case 3 -> cancelOrder();
            case 0 -> {
            	System.out.println("상위 메뉴로 돌아갑니다.");
            	return;
            }
            default -> System.out.println("잘못된 선택입니다.");
        }
    }
    
    // ---- 장바구니 전체 주문하기 구현 2025.08.24 조수아
    private void placeOrderFromCart() {
    	if (!session.isLoggedIn() || session.getRole() != Role.USER) {
            System.out.println("로그인된 사용자만 이용 가능합니다.");
            return;
        }
    	
    	Optional<Cart> cartOpt = cartRepository.findByUserId(session.getUserId()); // cartRepository 필요
        if (cartOpt.isEmpty() || cartOpt.get().getItems().isEmpty()) {
            System.out.println("장바구니가 비어 있습니다.");
            return;
        }

        List<OrderItem> cartItems = new ArrayList<>();
        cartOpt.get().getItems().forEach((productId, cartItem) -> {
            cartItems.add(new OrderItem(productId, cartItem.getQuantity()));
        });

        // 이후 주문 처리 로직 - 8.24 19:04 홍종학 주문완료 후 장바구니 비우기 로직 추가
        try {
            Order order = orderService.placeOrder(session.getUserId(), cartItems, Role.USER);
            
            //  주문 완료 후 장바구니 비우기 로직 추가
            Cart cart = cartOpt.get();
            cart.clear(); // 장바구니의 모든 상품을 제거합니다.
            cartRepository.save(cart); // 변경된 장바구니 상태를 저장소에 반영합니다.
            //  추가 끝

            System.out.println("장바구니 상품 주문 완료. 주문 ID: " + order.getOrderId());
        } catch (Exception e) {
            System.out.println("주문 중 오류 발생: " + e.getMessage());
        }
    }
    // ---------------------------------------------

    // 개별 상품 즉시 구매 구현 2025.08.24 17:21 조수아
    private void placeOrderSingle() {
    	if (!session.isLoggedIn() || session.getRole() != Role.USER) {
            System.out.println("로그인된 사용자만 이용 가능합니다.");
            return;
        }
        try {
            System.out.print("구매할 상품 ID 입력: ");
            String productId = sc.nextLine();
            Product product = productService.findProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            System.out.print("수량 입력: ");
            int qty = Integer.parseInt(sc.nextLine());
            if (qty <= 0) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");

            // 배송 주소 입력 받기
            String address = inputAddress();

            List<OrderItem> items = new ArrayList<>();
            items.add(new OrderItem(product.getId(), qty));
            Order order = orderService.placeOrder(session.getUserId(), items, Role.USER);

            // 배송 주소 처리 로직 필요 (Order에 배송정보 필드 없으면 추가 필요)

            System.out.println("상품 즉시 구매 주문이 완료되었습니다. 주문 ID: " + order.getOrderId() + 
            		"\n배송 주소: " + address);

        } catch (Exception e) {
            System.out.println("주문 처리 중 오류: " + e.getMessage());
        }
    }
    // -----------------------------------------------

    // 배송 주소 입력 구현 2025.08.24 17:24 조수아
    private String inputAddress() {
    	System.out.print("배송 주소를 입력하세요: ");
        String address = sc.nextLine().trim();

        if (address.isEmpty()) {
            // 회원 기본 주소 조회
            if (!session.isLoggedIn()) {
                System.out.println("로그인 상태가 아닙니다. 배송 주소를 직접 입력해주세요.");
                return "";
            }
            // 로그인한 회원 정보에서 주소 필드 가져오기 (예: User.getAddress())
            Object loggedUser = session.getLoggedUser();
            if (loggedUser instanceof User) {
                User user = (User) loggedUser;
                String userAddress = user.getAddress(); // User 클래스에 주소 필드가 있어야 함
                if (userAddress == null || userAddress.isBlank()) {
                    System.out.println("회원 기본 주소가 등록되어 있지 않습니다. 배송 주소를 직접 입력해주세요.");
                    return "";
                }
                System.out.println("회원 기본 주소를 사용합니다: " + userAddress);
                return userAddress;
            } else {
                System.out.println("회원 정보에서 주소를 조회할 수 없습니다. 배송 주소를 직접 입력해주세요.");
                return "";
            }
        }
        return address;
    }
    // -------------------------------------

    private void confirmOrder() {
        System.out.print("주문 ID 입력: ");
        String orderId = sc.nextLine();

        try {
            orderService.confirmOrder(orderId, session.getUserId(), session.getRole(), session );
            System.out.println("주문이 확정되었습니다.");
        } catch (Exception e) {
            System.out.println("에러: " + e.getMessage());
        }
    }

    private void listOrders() {
        List<Order> orders = orderService.listOrders(session.getUserId(), session.getRole());
        orders.forEach(System.out::println);
    }

    private void getOrderDetail() {
        System.out.print("조회할 주문 ID 입력: ");
        String orderId = sc.nextLine();

        try {
        	// ----- before : 주문 상세 정보 조회 order -> orderItem으로 변경 2025.08.24 23:17 장하은
        	//Order order = orderService.getOrder(orderId, session.getUserId(), session.getRole(), session);
        	//System.out.println(order);
        	
        	// ----- after : 주문 상세 정보 조회 order -> orderItem으로 변경 2025.08.24 23:17 장하은
        	var order = orderService.getOrder(orderId, session.getUserId(), session.getRole(), session);

        	// 주문 기본정보 출력
        	System.out.println("주문 ID: " + order.getOrderId());
        	System.out.println("주문자: " + order.getUserId());
        	System.out.println("상태: " + order.getStatus());
        	System.out.println("총액: " + order.getTotalPrice());

        	// 주문 아이템들 출력
        	for (OrderItem item : order.getItems()) {
        	    System.out.printf("상품ID=%s, 상품명=%s, 단가=%d, 수량=%d, 라인합계=%d%n",
        	            item.getProductId(),
        	            item.getProductName(),
        	            item.getUnitPrice(),
        	            item.getQuantity(),
        	            item.getLineTotal()
        	    );
        	// -------------------------------
        	}
        	
        	
        } catch (Exception e) {
            System.out.println("에러: " + e.getMessage());
        }
    }

    private void cancelOrder() {
        System.out.print("취소할 주문 ID 입력: ");
        String orderId = sc.nextLine();

        try {
            orderService.cancelOrder(orderId, session.getUserId(), session.getRole());
            System.out.println("주문이 취소되었습니다.");
        } catch (Exception e) {
            System.out.println("에러: " + e.getMessage());
        }
    }
}