import Foundation

enum VietnameseTextNormalizer {
    static func displayName(commonNames: [String]?, scientificName: String?) -> String {
        let translated = commonNames.orEmpty
            .compactMap { fallbackName($0) ?? vietnameseOnly($0) }
        if let first = translated.first {
            return first
        }
        return scientificName.nilIfBlank ?? "Cây chưa rõ tên Việt"
    }

    static func text(_ value: String?) -> String? {
        guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else {
            return nil
        }
        if looksScientificName(trimmed) {
            return trimmed
        }
        if let fallback = fallbackName(trimmed) {
            return fallback
        }
        let localized = replaceKnownEnglishNames(in: trimmed)
        if containsVietnamese(localized) {
            return localized
        }
        return localized
    }

    static func diseaseName(_ value: String?) -> String {
        let raw = value?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
        if raw.isEmpty {
            return "Bệnh cây"
        }
        if raw.contains("anthracnose") { return "Bệnh thán thư" }
        if raw.contains("powdery") { return "Bệnh phấn trắng" }
        if raw.contains("downy") { return "Bệnh sương mai" }
        if raw.contains("blight") { return "Bệnh cháy lá" }
        if raw.contains("leaf spot") { return "Bệnh đốm lá" }
        if raw.contains("root rot") { return "Bệnh thối rễ" }
        if raw.contains("rust") { return "Bệnh gỉ sắt" }
        if raw.contains("mosaic") { return "Bệnh khảm lá" }
        if raw.contains("healthy") { return "Cây khỏe" }
        return text(value) ?? "Bệnh cây"
    }

    static func careVideoCitation(displayName: String, scientificName: String?) -> CitationData {
        let nameParts = [displayName, scientificName.nilIfBlank]
            .compactMap { $0 }
            .uniquedByFolded()
            .joined(separator: " ")
        let query = "cách trồng chăm sóc \(nameParts)"
            .addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return CitationData(
            type: "youtube",
            title: "Video: cách trồng và chăm sóc \(displayName)",
            url: "https://www.youtube.com/results?search_query=\(query)"
        )
    }

    private static func vietnameseOnly(_ value: String) -> String? {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty || looksEnglishCommonName(trimmed) {
            return nil
        }
        return trimmed
    }

    private static func fallbackName(_ value: String) -> String? {
        let key = value.folded()
        if key == "snake plant" || key.contains("snake plant") || key.contains("mother in law") {
            return "Cây lưỡi hổ"
        }
        if key == "aloe vera" || key.contains("aloe vera") {
            return "Nha đam"
        }
        if key.contains("golden pothos") || key.contains("pothos") || key == "money plant" {
            return "Cây trầu bà"
        }
        if key.contains("peace lily") {
            return "Cây lan ý"
        }
        if key.contains("spider plant") {
            return "Cây dây nhện"
        }
        if key.contains("zz plant") {
            return "Cây kim tiền"
        }
        if key.contains("rubber plant") {
            return "Cây đa búp đỏ"
        }
        if key.contains("fiddle leaf fig") {
            return "Cây bàng Singapore"
        }
        if key.contains("jade plant") {
            return "Cây ngọc bích"
        }
        if key.contains("swiss cheese plant") || key.contains("monstera") {
            return "Cây trầu bà lá xẻ"
        }
        if key.contains("corn plant") {
            return "Cây phát tài"
        }
        if key.contains("lucky bamboo") {
            return "Cây phát lộc"
        }
        if key.contains("areca palm") {
            return "Cây cau vàng"
        }
        if key.contains("chinese evergreen") {
            return "Cây vạn niên thanh"
        }
        if key.contains("dieffenbachia") || key.contains("dumb cane") {
            return "Cây vạn niên thanh"
        }
        return nil
    }

    static func containsEnglishDisplayTerms(_ value: String) -> Bool {
        let key = value.folded()
        let terms = [
            "snake plant", "mother in law", "aloe vera", "golden pothos", "pothos",
            "money plant", "peace lily", "spider plant", "zz plant", "rubber plant",
            "fiddle leaf fig", "jade plant", "swiss cheese plant", "monstera",
            "corn plant", "lucky bamboo", "areca palm", "chinese evergreen",
            "leaf spot", "root rot", "powdery mildew", "anthracnose", "blight", "rust"
        ]
        return terms.contains { key.contains($0) }
    }

    private static func replaceKnownEnglishNames(in value: String) -> String {
        var result = value
        let replacements: [(String, String)] = [
            ("Snake Plant", "Cây lưỡi hổ"),
            ("Mother-in-law's Tongue", "Cây lưỡi hổ"),
            ("Aloe Vera", "Nha đam"),
            ("Golden Pothos", "Cây trầu bà"),
            ("Pothos", "Cây trầu bà"),
            ("Money Plant", "Cây trầu bà"),
            ("Peace Lily", "Cây lan ý"),
            ("Spider Plant", "Cây dây nhện"),
            ("ZZ Plant", "Cây kim tiền"),
            ("Rubber Plant", "Cây đa búp đỏ"),
            ("Fiddle Leaf Fig", "Cây bàng Singapore"),
            ("Jade Plant", "Cây ngọc bích"),
            ("Swiss Cheese Plant", "Cây trầu bà lá xẻ"),
            ("Monstera", "Cây trầu bà lá xẻ"),
            ("Corn Plant", "Cây phát tài"),
            ("Lucky Bamboo", "Cây phát lộc"),
            ("Areca Palm", "Cây cau vàng"),
            ("Chinese Evergreen", "Cây vạn niên thanh"),
            ("Leaf Spot", "Bệnh đốm lá"),
            ("Root Rot", "Bệnh thối rễ"),
            ("Powdery Mildew", "Bệnh phấn trắng"),
            ("Anthracnose", "Bệnh thán thư"),
            ("Blight", "Bệnh cháy lá"),
            ("Rust", "Bệnh gỉ sắt")
        ]
        for (english, vietnamese) in replacements {
            result = result.replacingOccurrences(of: english, with: vietnamese, options: [.caseInsensitive])
        }
        return result
    }

    private static func containsVietnamese(_ value: String) -> Bool {
        value.range(of: "[ăâêôơưđáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]", options: [.regularExpression, .caseInsensitive]) != nil
    }

    private static func looksScientificName(_ value: String) -> Bool {
        let words = value.split(separator: " ")
        guard words.count >= 2, words.count <= 4 else { return false }
        guard let first = words.first, first.first?.isUppercase == true else { return false }
        let commonWords = ["plant", "tree", "leaf", "flower", "vine", "grass", "palm", "cactus", "fern"]
        return !words.contains { commonWords.contains($0.lowercased()) }
    }

    private static func looksEnglishCommonName(_ value: String) -> Bool {
        let key = value.folded()
        return key.contains("plant") ||
            key.contains("tree") ||
            key.contains("leaf") ||
            key.contains("pothos") ||
            key.contains("dracaena")
    }
}

private extension Optional where Wrapped == [String] {
    var orEmpty: [String] { self ?? [] }
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

private extension String {
    func folded() -> String {
        folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "vi_VN"))
            .replacingOccurrences(of: "đ", with: "d")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

private extension Array where Element == String {
    func uniquedByFolded() -> [String] {
        var seen = Set<String>()
        return filter { value in
            let key = value.folded()
            return seen.insert(key).inserted
        }
    }
}
