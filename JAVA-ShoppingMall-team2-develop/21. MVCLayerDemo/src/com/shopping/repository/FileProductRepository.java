package com.shopping.repository;

import com.shopping.model.Product;
import com.shopping.model.ProductCategory;
import com.shopping.persistence.FileManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.shopping.model.Product;
import com.shopping.persistence.FileManager;

/**
 * 상품 데이터의 영속성을 관리하는 저장소 클래스.
 * FileManager를 사용하여 파일에서 데이터를 읽고 쓰는 역할을 담당합니다.
 * ProductRepository 인터페이스의 파일 기반 구현체입니다.
 */

public class FileProductRepository implements ProductRepository {
	// 2025.08.23 17:12 JHE 수정
	private final Map<String, Product> productStore = new HashMap<>();
    private static final String DATA_FILE_NAME = "products.dat";
    private long sequence = 0L;

    public FileProductRepository() {
        loadDataFromFile();       // 시작 시 파일에서 읽어오기
        ensureSampleData();
    }
    
    

   
    // 2025.08.23 18:23 JSA 추가
	private void ensureSampleData() {
		if (productStore.isEmpty()) {
			// 전자제품
	        addSampleProduct("아이폰 16", 1250000, 50, ProductCategory.ELECTRONICS, 
	        		"아이폰 16은 향상된 성능의 A18 칩 그리고 새로운 AI 기능(Apple Intelligence) 을 탑재하여 전반적인 성능과 사용 경험을 향상시켰습니다");
	        addSampleProduct("갤럭시 Z 폴드7", 2250000, 30, ProductCategory.ELECTRONICS, 
	        		"갤럭시 Z 폴드7은 초슬림·초경량 디자인에 2억 화소 카메라 그리고 강력한 갤럭시 AI 기능을 모두 갖춘 최신 폴더블 스마트폰입니다. ");

	        // 의류
	        addSampleProduct("남성 프리미엄 가죽 재킷", 350000, 40, ProductCategory.CLOTHING,
	                "고급 소가죽으로 제작된 프리미엄 가죽 재킷으로 세련된 디자인과 뛰어난 내구성을 자랑합니다.");
	        addSampleProduct("여성용 캐시미어 스웨터", 180000, 60, ProductCategory.CLOTHING,
	                "부드러운 캐시미어 소재로 제작되어 보온성과 착용감이 뛰어난 여성용 스웨터입니다.");

	        // 식품
	        addSampleProduct("제주 햇감귤 5kg", 25000, 100, ProductCategory.FOOD,
	                "신선하고 달콤한 제주산 햇감귤 5kg 세트로 겨울철 건강 간식으로 제격입니다.");
	        addSampleProduct("유기농 퀴노아 1kg", 15000, 70, ProductCategory.FOOD,
	                "남미산 유기농 퀴노아로 영양가 풍부하며 다양한 요리에 활용 가능한 건강 식재료입니다.");

	        // 도서
	        addSampleProduct("자바 프로그래밍 마스터", 35000, 80, ProductCategory.BOOKS,
	                "초보부터 고급까지 아우르는 자바 프로그래밍 완벽 입문서로, 실전 예제와 함께 구성되어 있습니다.");
	        addSampleProduct("클린 코드", 28000, 50, ProductCategory.BOOKS,
	                "소프트웨어 개발의 품질과 유지보수를 극대화하기 위한 명탐정 개발자의 실천 지침서입니다.");

	        // 기타
	        addSampleProduct("레고 클래식 블록 세트", 90000, 20, ProductCategory.OTHER,
	                "창의력과 상상력을 키워주는 다양한 색상과 모양의 레고 클래식 블록 세트입니다.");
	        addSampleProduct("무선 블루투스 이어폰", 45000, 35, ProductCategory.OTHER,
	                "컴팩트한 사이즈와 뛰어난 음질, 긴 배터리 수명을 자랑하는 최신 무선 블루투스 이어폰입니다.");

	        saveDataToFile();
	    }
	}
	
	private void addSampleProduct(String name, int price, int stock,
            ProductCategory category, String description) {
		Product product = new Product();
		product.setId(generateId());
		product.setName(name);
		product.setPrice(price);
		product.setStock(stock);
		product.setCategory(category);
		product.setDescription(description);
		product.setRegistrationDateTime(LocalDateTime.now());
		productStore.put(product.getId(), product);
	}



	// 2025.08.23 17:12 JHE 추가 
    @Override
    public boolean hasStock(String productId, int qty) {
        Product p = productStore.get(productId);
        return p != null && qty > 0 && p.getStock() >= qty;
    }

    @Override
    public void decreaseStock(String productId, int qty) {
        Product p = productStore.get(productId);
        if (p == null) {
			throw new IllegalArgumentException("상품 없음: " + productId);
		}
        if (qty <= 0 || p.getStock() < qty) {
			throw new IllegalArgumentException("재고 부족(요청 " + qty + ", 보유 " + (p.getStock()) + ")");
		}
        p.setStock(p.getStock() - qty);
        saveDataToFile();
    }

    @Override
    public void increaseStock(String productId, int qty) {
        Product p = productStore.get(productId);
        if (p == null) {
			throw new IllegalArgumentException("상품 없음: " + productId);
		}
        if (qty <= 0) {
			throw new IllegalArgumentException("증가 수량은 0보다 커야 함");
		}
        p.setStock(p.getStock() + qty);
        saveDataToFile();
    }


    private void loadDataFromFile() {
        List<Product> products = FileManager.readFromFile(DATA_FILE_NAME);
        for (Product product : products) {
            productStore.put(product.getId(), product);
        }
        this.sequence = productStore.keySet().stream()
                .map(id -> id.substring(1))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);
    }
    
    //

    private void saveDataToFile() {
    	// 2025.08.23 JSA 추가
    	File dataDir = new File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("data 폴더가 생성되었습니다.");
            } else {
                System.out.println("data 폴더 생성 실패 - 권한 문제 확인 필요");
            }
        }
        
        FileManager.writeToFile(DATA_FILE_NAME, new ArrayList<>(productStore.values()));
    }

    /*
     * [수정] 신규 상품(ID가 null)인 경우 ID와 등록일시를 자동 생성하여 저장하도록 로직을 수정했습니다.
     */
    @Override
    public Product save(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            String newId = generateId();
            product.setId(newId);
            product.setRegistrationDateTime(LocalDateTime.now());
        }
        productStore.put(product.getId(), product);
        saveDataToFile();
        return product;
    }

    @Override
    public void saveAll(Product product) {
        if (product == null) {
			return;
		}
        save(product);
    }

    @Override
    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(productStore.get(productId));
    }

    @Override
    public List<Product> findBynameContains(String name) {
        if (name == null || name.isBlank()) {
            return new ArrayList<>();
        }
        String lowerCaseName = name.toLowerCase();
        return productStore.values().stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerCaseName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByCategory(String category) {
        if (category == null || category.isBlank()) {
            return new ArrayList<>();
        }
        return productStore.values().stream()
                .filter(p -> p.getCategory().name().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findAll() {
        return productStore.values().stream()
                .sorted(Comparator.comparing(Product::getId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(String productId) {
        if (productStore.remove(productId) != null) {
            saveDataToFile();
            return true;
        }
        return false;
    }

    /**
     * "P001" 형식의 새로운 상품 ID를 생성하여 반환합니다.
     */
    public String generateId() {
        this.sequence++;
        return String.format("P%03d", this.sequence);
    }

    // =================================================================
    // [추가] 인터페이스에 새로 추가된 메소드들의 실제 구현
    // =================================================================

    @Override
    public List<Product> findAll(int page, int pageSize) {
        return productStore.values().stream()
                .sorted(Comparator.comparing(Product::getId))
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return productStore.size();
    }

    @Override
    public List<Product> findBestSellers(int limit) {
        return productStore.values().stream()
                .sorted(Comparator.comparingInt(Product::getSalesCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findNewArrivals(int limit) {
        return productStore.values().stream()
                .sorted(Comparator.comparing(Product::getRegistrationDateTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByPriceRange(int minPrice, int maxPrice) {
        return productStore.values().stream()
                .filter(p -> p.getPrice() >= minPrice && (maxPrice == -1 || p.getPrice() < maxPrice))
                .sorted(Comparator.comparing(Product::getPrice))
                .collect(Collectors.toList());
    }
}