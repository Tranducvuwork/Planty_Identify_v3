import Foundation

struct PlantScanAPIResponse: Decodable {
    let status: String?
    let data: PlantScanData?
}

struct PlantScanData: Decodable {
    let scientificName: String?
    let commonNames: [String]?
    let confidence: Int?
    let description: String?
    let plantId: String?
    let qrCode: String?
    let isMedicinal: Bool?
    let medicinalLabel: String?
    let uses: [String]?
    let toxicity: ToxicityData?
    let care: CareData?
    let commonDiseases: [CommonDiseaseData]?
    let folkRemedies: [FolkRemedyData]?
    let medicalDisclaimer: String?
    let detailAvailable: Bool?
    let imageUrl: String?
}

struct ToxicityData: Decodable {
    let isToxic: Bool?
    let isFungus: Bool?
    let toxicSap: Bool?
    let warningLevel: String?
    let warningMessage: String?
    let firstAid: String?

    enum CodingKeys: String, CodingKey {
        case isToxic = "is_toxic"
        case isFungus = "is_fungus"
        case toxicSap = "toxic_sap"
        case warningLevel = "warning_level"
        case warningMessage = "warning_message"
        case firstAid = "first_aid"
    }
}

struct CareData: Decodable {
    let planting: String?
    let care: String?
    let bestSeason: String?
    let citations: [CitationData]?
    let citationNote: String?

    enum CodingKeys: String, CodingKey {
        case planting
        case care
        case bestSeason = "best_season"
        case citations
        case citationNote = "citation_note"
    }
}

struct CitationData: Decodable, Identifiable {
    var id: String { url ?? title ?? UUID().uuidString }
    let type: String?
    let title: String?
    let url: String?
}

struct CommonDiseaseData: Decodable, Identifiable {
    var id: String { name ?? UUID().uuidString }
    let name: String?
    let description: String?
    let ecoTreatment: String?
    let imageUrl: String?

    enum CodingKeys: String, CodingKey {
        case name
        case description
        case ecoTreatment = "eco_treatment"
        case imageUrl = "image_url"
    }
}

struct FolkRemedyData: Decodable, Identifiable {
    let id: String?
    let title: String?
    let ailment: String?
    let ingredients: [String]?
    let method: String?
    let region: String?
    let plants: [String]?
    let sourceTitle: String?
    let caution: String?

    enum CodingKeys: String, CodingKey {
        case id
        case title
        case ailment
        case ingredients
        case method
        case region
        case plants
        case sourceTitle = "source_title"
        case caution
    }
}

struct PlantDiseaseAPIResponse: Decodable {
    let status: String?
    let data: PlantDiseaseData?
}

struct PlantChatAPIResponse: Decodable {
    let status: String?
    let message: String?
    let data: PlantChatData?
}

struct PlantChatData: Decodable {
    let answer: String?
}

struct PlantDiseaseData: Decodable {
    let analysisNotes: String?
    let isPlantImage: Bool?
    let isDiseasedLeafImage: Bool?
    let isHealthy: Bool?
    let diseaseId: Int?
    let diseaseName: String?
    let matchedDiseaseName: String?
    let isKnownDisease: Bool?
    let diseaseDetails: FlexibleText?
    let confidence: Int?

    enum CodingKeys: String, CodingKey {
        case analysisNotes = "analysis_notes"
        case isPlantImage
        case isDiseasedLeafImage
        case isHealthy
        case diseaseId
        case diseaseName
        case matchedDiseaseName
        case isKnownDisease
        case diseaseDetails
        case confidence
    }
}

enum FlexibleText: Decodable {
    case string(String)
    case object([String: String])

    var displayText: String? {
        switch self {
        case .string(let value):
            return value
        case .object(let values):
            return values["description"] ?? values["summary"] ?? values["details"] ?? values["title"]
        }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let value = try? container.decode(String.self) {
            self = .string(value)
            return
        }
        if let values = try? container.decode([String: String].self) {
            self = .object(values)
            return
        }
        self = .string("")
    }
}

enum ScanMode: String, CaseIterable, Identifiable {
    case identify = "Nhận diện"
    case diagnose = "Chẩn đoán"

    var id: String { rawValue }
}

enum ScanResult {
    case plant(PlantScanData)
    case disease(PlantDiseaseData)
}
