package com.shopping.util;

import com.shopping.model.Cart;
import com.shopping.model.Product;
import com.shopping.model.ProductCategory;
import com.shopping.persistence.FileManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 테스트에 사용될 products.dat와 carts.dat 파일을 생성하는 유틸리티 클래스입니다.
 * 이 클래스를 한 번 실행하면 src/test/resources/data/ 경로에 테스트용 데이터 파일이 생성됩니다.
 */
public class TestDataGenerator {

    public static void main(String[] args) {
        System.out.println("테스트 데이터 파일 생성을 시작합니다...");

        // 1. 테스트용 상품 데이터 생성
        createTestProducts();

        // 2. 테스트용 장바구니 데이터 생성
        createTestCarts();

        System.out.println("테스트 데이터 파일 생성이 완료되었습니다.");
        System.out.println("위치: src/test/resources/data/");
    }

    /**
     * 테스트용 상품 목록을 생성하고 파일에 저장합니다.
     */
    private static void createTestProducts() {
        List<Product> products = new ArrayList<>();

        // 상품 1
        Product p1 = new Product("P001", "고성능 노트북", ProductCategory.ELECTRONICS, 1500000, 10, "최신 CPU가 탑재된 고성능 노트북입니다.");
        p1.setRegistrationDateTime(LocalDateTime.now().minusDays(10));
        p1.setSalesCount(25);
        products.add(p1);

        // 상품 2
        Product p2 = new Product("P002", "편안한 면 티셔츠", ProductCategory.CLOTHING, 25000, 50, "100% 순면으로 제작된 부드러운 티셔츠입니다.");
        p2.setRegistrationDateTime(LocalDateTime.now().minusDays(5));
        p2.setSalesCount(80);
        products.add(p2);

        // 상품 3
        Product p3 = new Product("P003", "유기농 사과 1kg", ProductCategory.FOOD, 12000, 100, "신선하고 맛있는 친환경 유기농 사과입니다.");
        p3.setRegistrationDateTime(LocalDateTime.now().minusDays(1));
        p3.setSalesCount(120);
        products.add(p3);
        
        // 상품 4
        Product p4 = new Product("P004", "자바 프로그래밍 입문", ProductCategory.BOOKS, 30000, 30, "초보자를 위한 최고의 자바 입문서입니다.");
        p4.setRegistrationDateTime(LocalDateTime.now().minusMonths(2));
        p4.setSalesCount(45);
        products.add(p4);

        // 파일에 저장
        FileManager.writeToFile("src/test/resources/data/products.dat", products);
    }

    /**
     * 테스트용 장바구니 목록을 생성하고 파일에 저장합니다.
     */
    private static void createTestCarts() {
        List<Cart> carts = new ArrayList<>();
        
        // 'testuser'의 장바구니
        Cart userCart = new Cart("testuser");
        // 위에서 생성한 상품 객체를 그대로 사용해야 합니다.
        Product p1 = new Product("P001", "고성능 노트북", ProductCategory.ELECTRONICS, 1500000, 10, "최신 CPU가 탑재된 고성능 노트북입니다.");
        Product p3 = new Product("P003", "유기농 사과 1kg", ProductCategory.FOOD, 12000, 100, "신선하고 맛있는 친환경 유기농 사과입니다.");
        
        userCart.addProduct(p1, 1); // 노트북 1개 추가
        userCart.addProduct(p3, 2); // 사과 2kg 추가
        carts.add(userCart);

        // 'admin'의 장바구니
        Cart adminCart = new Cart("admin");
        Product p2 = new Product("P002", "편안한 면 티셔츠", ProductCategory.CLOTHING, 25000, 50, "100% 순면으로 제작된 부드러운 티셔츠입니다.");
        
        adminCart.addProduct(p2, 5); // 티셔츠 5개 추가
        carts.add(adminCart);
        
        // 파일에 저장
        FileManager.writeToFile("src/test/resources/data/carts.dat", carts);
    }
}