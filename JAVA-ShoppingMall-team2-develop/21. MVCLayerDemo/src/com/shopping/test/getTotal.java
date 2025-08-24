package com.shopping.test;

import com.shopping.model.Order;
import com.shopping.model.OrderItem;
import com.shopping.repository.FileOrderRepository;
import com.shopping.repository.OrderRepository;

public class getTotal {
	
	public static void main(String[] args) {
		Order o = new Order();
		o.setOrderId("O1"); 
		o.setUserId("U1");
	
		// 1) 직접 생성
		o.addItem(new OrderItem("P1", "키보드", 30000, 2));
		o.addItem(new OrderItem("P2", "마우스", 15000, 1));
		System.out.println("after add, total = " + o.getTotalPrice());
	
		// 2) 수량 변경
		o.updateItemQuantity("P2", 3);
		System.out.println("after qty change, total = " + o.getTotalPrice());
	
		// 3) 상세 출력
		for (OrderItem it : o.getItems()) {
		    System.out.printf("  %s %s qty=%d unit=%d line=%d%n",
		        it.getProductId(), it.getProductName(), it.getQuantity(), it.getUnitPrice(), it.getLineTotal());
		}
	    // 4) 호출부 이슈
		    OrderRepository repo = new FileOrderRepository("data/orders.dat"); // ★ 인스턴스 생성

//	        Order o = new Order();
//	        o.setOrderId("O1");    // 수동 세팅하거나, save 시 nextId()로 자동 부여해도 됨
//	        o.setUserId("U1");

	        // 아이템 추가
	        o.addItem(new OrderItem("P1", "키보드", 30000, 2));
	        o.addItem(new OrderItem("P2", "마우스", 15000, 1));
	        System.out.println("after add, total = " + o.getTotalPrice());

	        // 수량 변경
	        o.updateItemQuantity("P2", 3);
	        System.out.println("after qty change, total = " + o.getTotalPrice());

	        // 저장 (여기서 실제 저장됨)
	        repo.save(o);

	        // 조회 (static 아님! 인스턴스 사용)
	        Order re = repo.findById("O1").orElseThrow();
	        System.out.println("re.getTotal = " + re.getTotalPrice());
		}
	
}
