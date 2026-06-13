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

        XCTAssertEqual(dto.flag, "EVACUATION_REQUIRED")
        XCTAssertEqual(dto.message, "Big Emergency")

        // mapStatus is now `internal`, so we can verify the decoded flag maps correctly.
        XCTAssertEqual(APIService.shared.mapStatus(dto.flag), .evacuation)
    }

    // (1b) Status string → TrackStatus mapping, including the priority ordering.
    func testMapStatus_CoreFlags() {
        let api = APIService.shared
        XCTAssertEqual(api.mapStatus("GREEN"), .green)
        XCTAssertEqual(api.mapStatus("RED_FLAG"), .red)
        XCTAssertEqual(api.mapStatus("EVACUATION_REQUIRED"), .evacuation)
        XCTAssertEqual(api.mapStatus("SAFETY_CAR"), .sc)
        XCTAssertEqual(api.mapStatus("VSC"), .sc)
    }

    // (1c) Regression: YELLOW / CAUTION must map to .yellow, NOT be swallowed by the
    // Safety-Car matcher (which also matches "CAUTION"). The yellow check runs first.
    func testMapStatus_YellowAndCautionBeatSafetyCar() {
        let api = APIService.shared
        XCTAssertEqual(api.mapStatus("YELLOW"), .yellow)
        XCTAssertEqual(api.mapStatus("YELLOW_FLAG"), .yellow)
        XCTAssertEqual(api.mapStatus("CAUTION"), .yellow,
            "CAUTION must resolve to .yellow even though the SC group historically matched it")
        // Distinct from a genuine safety-car state.
        XCTAssertEqual(api.mapStatus("SAFETY_CAR"), .sc)
        XCTAssertNotEqual(api.mapStatus("YELLOW"), api.mapStatus("SAFETY_CAR"))
    }

    // (1d) Mapping is case-insensitive and whitespace-tolerant; unknown stays .unknown.
    func testMapStatus_NormalizationAndUnknown() {
        let api = APIService.shared
        XCTAssertEqual(api.mapStatus("  green  "), .green)
        XCTAssertEqual(api.mapStatus("yellow"), .yellow)
        XCTAssertEqual(api.mapStatus("something_we_dont_know"), .unknown)
        XCTAssertEqual(api.mapStatus(""), .unknown)
    }

    // (1e) RED has priority over EVACUATION? No — EVACUATION is highest. Verify ordering.
    func testMapStatus_EvacuationHasHighestPriority() {
        let api = APIService.shared
        // A string containing both EVACUATION and RED must resolve to .evacuation.
        XCTAssertEqual(api.mapStatus("RED_EVACUATION"), .evacuation)
    }
    
    // (2) Test Product Decoding with Missing Fields (The Fix)
    //
    // The payload deliberately omits product_id, description, stock and category.
    // (Foundation's JSONDecoder is strict RFC-8259 JSON and rejects `//` comments,
    // so the note about the missing fields lives here in Swift, not inside the literal.)
    func testProductDecoding_Relaxed() throws {
        let json = """
        [
            {
                "id": "item1",
                "name": "Minimal Item",
                "price": 10.0,
                "is_active": 1
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
