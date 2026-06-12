import XCTest
@testable import GeoRacing

final class ParityTests: XCTestCase {

    // (1) Test Circuit State Decoding & Mapping
    func testCircuitStateMapping() throws {
        let json = """
        {
            "id": "1",
            "global_mode": "EVACUATION_REQUIRED",
            "message": "Big Emergency"
        }
        """.data(using: .utf8)!
        
        let decoder = JSONDecoder()
        let dto = try decoder.decode(CircuitStateDto.self, from: json)
        
        // Use APIService private logic via a testable wrapper or check mapping result here manually if private.
        // Since APIService.mapStatus is private, we test the logic behaviorally via CircuitStatusRepo or just replicate expectations.
        // But better: expose mapStatus or assume APIService usage.
        
        // Ideally we test internal mapping, but let's test DTO decoding first
        XCTAssertEqual(dto.flag, "EVACUATION_REQUIRED")
        XCTAssertEqual(dto.message, "Big Emergency")
    }
    
    // (2) Test Product Decoding with Missing Fields (The Fix)
    func testProductDecoding_Relaxed() throws {
        let json = """
        [
            {
                "id": "item1",
                "name": "Minimal Item",
                "price": 10.0,
                "is_active": 1
                // Missing product_id, description, stock, category
            }
        ]
        """.data(using: .utf8)!
        
        let decoder = JSONDecoder()
        let products = try decoder.decode([Product].self, from: json)
        
        XCTAssertEqual(products.count, 1)
        let p = products.first!
        XCTAssertEqual(p.name, "Minimal Item")
        XCTAssertEqual(p.description, "") // Default
        XCTAssertNil(p.productId) // Optional
        XCTAssertEqual(p.category, "General") // Default
        XCTAssertTrue(p.isActive)
    }
    
    // (3) Test Product with product_id (Legacy/Correct)
    func testProductDecoding_Full() throws {
        let json = """
        [
            {
                "id": "item2",
                "product_id": "PID-123",
                "name": "Full Item",
                "description": "Desc",
                "price": 20.0,
                "stock": 5,
                "category": "Food",
                "is_active": 1
            }
        ]
        """.data(using: .utf8)!
        
        let products = try JSONDecoder().decode([Product].self, from: json)
        XCTAssertEqual(products.count, 1)
        XCTAssertEqual(products.first?.productId, "PID-123")
    }
}
