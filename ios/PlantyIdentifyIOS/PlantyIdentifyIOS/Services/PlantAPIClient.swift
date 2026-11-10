import Foundation
import UIKit

final class PlantAPIClient {
    private let baseURL = URL(string: "https://identifierapp-server-hmvz.onrender.com/")!
    private let apiKey = "lamma.scan.plant"

    func scanPlant(image: UIImage) async throws -> PlantScanData {
        let response: PlantScanAPIResponse = try await upload(
            endpoint: "api/v1/plant/scan",
            image: image
        )
        guard response.status?.lowercased() == "success",
              let data = response.data,
              let scientificName = data.scientificName?.trimmingCharacters(in: .whitespacesAndNewlines),
              !scientificName.isEmpty,
              scientificName.lowercased() != "unknown" else {
            throw PlantAPIError.notIdentified
        }
        return await localizedPlant(data)
    }

    func diagnosePlant(image: UIImage) async throws -> PlantDiseaseData {
        let response: PlantDiseaseAPIResponse = try await upload(
            endpoint: "api/v1/plant/disease",
            image: image
        )
        guard response.status?.lowercased() == "success" else {
            throw PlantAPIError.server
        }
        let data = response.data ?? PlantDiseaseData(
            analysisNotes: nil,
            isPlantImage: true,
            isDiseasedLeafImage: false,
            isHealthy: true,
            diseaseId: nil,
            diseaseName: nil,
            matchedDiseaseName: nil,
            isKnownDisease: nil,
            diseaseDetails: nil,
            confidence: 100
        )
        return await localizedDisease(data)
    }

    private func upload<T: Decodable>(endpoint: String, image: UIImage) async throws -> T {
        guard let jpeg = image.jpegData(compressionQuality: 0.88) else {
            throw PlantAPIError.invalidImage
        }

        let boundary = "Boundary-\(UUID().uuidString)"
        var request = URLRequest(url: baseURL.appendingPathComponent(endpoint))
        request.httpMethod = "POST"
        request.timeoutInterval = 120
        request.setValue(apiKey, forHTTPHeaderField: "X-API-Key")
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.httpBody = multipartBody(
            boundary: boundary,
            imageData: jpeg,
            fields: ["language": "vi"]
        )

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode) else {
            throw PlantAPIError.server
        }
        return try JSONDecoder().decode(T.self, from: data)
    }

    private func localizedPlant(_ data: PlantScanData) async -> PlantScanData {
        let displayNames = await translateList(data.commonNames)
        return PlantScanData(
            scientificName: data.scientificName,
            commonNames: displayNames.isEmpty ? data.commonNames : displayNames,
            confidence: data.confidence,
            description: await translate(data.description),
            plantId: data.plantId,
            qrCode: data.qrCode,
            isMedicinal: data.isMedicinal,
            medicinalLabel: await translate(data.medicinalLabel),
            uses: await translateList(data.uses),
            toxicity: data.toxicity,
            care: await localizedCare(data.care),
            commonDiseases: await localizedCommonDiseases(data.commonDiseases),
            folkRemedies: data.folkRemedies,
            medicalDisclaimer: await translate(data.medicalDisclaimer),
            detailAvailable: data.detailAvailable,
            imageUrl: data.imageUrl
        )
    }

    private func localizedDisease(_ data: PlantDiseaseData) async -> PlantDiseaseData {
        PlantDiseaseData(
            analysisNotes: await translate(data.analysisNotes),
            isPlantImage: data.isPlantImage,
            isDiseasedLeafImage: data.isDiseasedLeafImage,
            isHealthy: data.isHealthy,
            diseaseId: data.diseaseId,
            diseaseName: await translate(data.diseaseName),
            matchedDiseaseName: await translate(data.matchedDiseaseName),
            isKnownDisease: data.isKnownDisease,
            diseaseDetails: await localizedFlexibleText(data.diseaseDetails),
            confidence: data.confidence
        )
    }

    private func localizedFlexibleText(_ value: FlexibleText?) async -> FlexibleText? {
        guard let displayText = value?.displayText else { return value }
        return .string(await translate(displayText) ?? displayText)
    }

    private func localizedCare(_ care: CareData?) async -> CareData? {
        guard let care else { return nil }
        return CareData(
            planting: await translate(care.planting),
            care: await translate(care.care),
            bestSeason: await translate(care.bestSeason),
            citations: care.citations,
            citationNote: await translate(care.citationNote)
        )
    }

    private func localizedCommonDiseases(_ diseases: [CommonDiseaseData]?) async -> [CommonDiseaseData]? {
        guard let diseases else { return nil }
        var result: [CommonDiseaseData] = []
        for disease in diseases {
            result.append(
                CommonDiseaseData(
                    name: await translate(disease.name),
                    description: await translate(disease.description),
                    ecoTreatment: await translate(disease.ecoTreatment),
                    imageUrl: disease.imageUrl
                )
            )
        }
        return result
    }

    private func translateList(_ values: [String]?) async -> [String] {
        var result: [String] = []
        for value in values ?? [] {
            if let translated = await translate(value), !translated.isEmpty {
                result.append(translated)
            }
        }
        return Array(Set(result)).sorted()
    }

    private func translate(_ value: String?) async -> String? {
        guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else {
            return nil
        }
        if let fallback = VietnameseTextNormalizer.text(trimmed), fallback != trimmed {
            return fallback
        }

        if !needsTranslation(trimmed) {
            return VietnameseTextNormalizer.text(trimmed) ?? trimmed
        }

        var request = URLRequest(url: baseURL.appendingPathComponent("api/v1/plant/chat"))
        request.httpMethod = "POST"
        request.timeoutInterval = 60
        request.setValue(apiKey, forHTTPHeaderField: "X-API-Key")
        request.setValue("application/x-www-form-urlencoded; charset=utf-8", forHTTPHeaderField: "Content-Type")
        let prompt = """
        Dịch nội dung sau sang tiếng Việt tự nhiên để hiển thị trong app nhận diện cây.
        Chỉ trả về bản dịch, không giải thích. Nếu là tên khoa học Latin thì giữ nguyên.
        Nội dung: \(trimmed)
        """
        let body = "message=\(urlEncode(prompt))&language=vi"
        request.httpBody = Data(body.utf8)

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse,
                  (200..<300).contains(http.statusCode) else {
                return trimmed
            }
            let decoded = try JSONDecoder().decode(PlantChatAPIResponse.self, from: data)
            let answer = decoded.data?.answer?
                .trimmingCharacters(in: CharacterSet.whitespacesAndNewlines.union(CharacterSet(charactersIn: "\"'")))
            return answer.nilIfBlank ?? trimmed
        } catch {
            return trimmed
        }
    }

    private func needsTranslation(_ value: String) -> Bool {
        if VietnameseTextNormalizer.containsEnglishDisplayTerms(value) {
            return true
        }
        if value.range(of: "[ăâêôơưđáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]", options: [.regularExpression, .caseInsensitive]) != nil {
            return false
        }
        return value.range(of: "[A-Za-z]{3,}", options: .regularExpression) != nil
    }

    private func urlEncode(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? value
    }

    private func multipartBody(
        boundary: String,
        imageData: Data,
        fields: [String: String]
    ) -> Data {
        var body = Data()
        fields.forEach { key, value in
            body.append("--\(boundary)\r\n")
            body.append("Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n")
            body.append("\(value)\r\n")
        }
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"scan.jpg\"\r\n")
        body.append("Content-Type: image/jpeg\r\n\r\n")
        body.append(imageData)
        body.append("\r\n--\(boundary)--\r\n")
        return body
    }
}

enum PlantAPIError: LocalizedError {
    case invalidImage
    case notIdentified
    case server

    var errorDescription: String? {
        switch self {
        case .invalidImage:
            return "Ảnh không hợp lệ."
        case .notIdentified:
            return "Không nhận diện được cây. Vui lòng chụp lại ảnh rõ hơn."
        case .server:
            return "Máy chủ đang bận. Vui lòng thử lại."
        }
    }
}

private extension Data {
    mutating func append(_ string: String) {
        append(Data(string.utf8))
    }
}

private extension Optional where Wrapped == String {
    var nilIfBlank: String? {
        guard let trimmed = self?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else {
            return nil
        }
        return trimmed
    }
}
