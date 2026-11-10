import SwiftUI

struct PlantResultView: View {
    let plant: PlantScanData
    let image: UIImage?
    let onClose: () -> Void
    let onScanAgain: () -> Void

    private var displayName: String {
        VietnameseTextNormalizer.displayName(
            commonNames: plant.commonNames,
            scientificName: plant.scientificName
        )
    }

    private var allCitations: [CitationData] {
        let dynamicVideo = VietnameseTextNormalizer.careVideoCitation(
            displayName: displayName,
            scientificName: plant.scientificName
        )
        let other = plant.care?.citations?.filter {
            ($0.type ?? "").lowercased() != "youtube"
        } ?? []
        return [dynamicVideo] + other
    }

    var body: some View {
        DetailScaffold(title: displayName, onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                PlantHeroImage(image: image, imageUrl: plant.imageUrl)

                Text(displayName)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(Color(hex: 0x1B1B1B))
                    .padding(.top, 12)

                Text("Tên khoa học: \(plant.scientificName ?? "")")
                    .font(.system(size: 15))
                    .italic()
                    .foregroundStyle(Color(hex: 0x666666))
                    .padding(.top, 4)

                if plant.isMedicinal == true || plant.medicinalLabel?.isEmpty == false {
                    Text(plant.medicinalLabel?.isEmpty == false ? plant.medicinalLabel! : "Cây thuốc")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background(Color(hex: 0x19A451))
                        .padding(.top, 8)
                }

                Text("Độ tin cậy: \(plant.confidence ?? 0)%")
                    .font(.system(size: 14))
                    .foregroundStyle(Color(hex: 0x444444))
                    .padding(.top, 6)

                ToxicWarning(toxicity: plant.toxicity)

                DetailSection(title: "Mô tả") {
                    Text(VietnameseTextNormalizer.text(plant.description) ??
                         "\(displayName) được nhận diện. Dưới đây là hướng dẫn trồng, chăm sóc và nguồn tham khảo bằng tiếng Việt.")
                }

                DetailSection(title: "Công dụng") {
                    BulletText(items: plant.uses?.compactMap(VietnameseTextNormalizer.text) ?? ["Cây cảnh trong nhà, phù hợp trang trí và quan sát sinh trưởng."])
                }

                DetailSection(title: "Cách trồng & chăm sóc") {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Cách trồng: \(VietnameseTextNormalizer.text(plant.care?.planting) ?? "Trồng trên đất tơi xốp, thoát nước tốt.")")
                        Text("Chăm sóc: \(VietnameseTextNormalizer.text(plant.care?.care) ?? "Tưới vừa phải, tránh úng và đặt nơi có ánh sáng phù hợp.")")
                        Text("Mùa phù hợp: \(VietnameseTextNormalizer.text(plant.care?.bestSeason) ?? "Theo dõi lá, rễ và độ ẩm đất định kỳ.")")
                    }
                }

                DetailSection(title: "Video / tài liệu tham khảo") {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(allCitations) { citation in
                            if let urlText = citation.url, let url = URL(string: urlText) {
                                Link(destination: url) {
                                    Text(ThesisCitationLabel.label(citation))
                                        .foregroundStyle(Color(hex: 0x1565C0))
                                }
                            } else {
                                Text(ThesisCitationLabel.label(citation))
                            }
                        }
                        if let note = plant.care?.citationNote, !note.isEmpty {
                            Text(note)
                                .font(.system(size: 12))
                                .foregroundStyle(Color(hex: 0x666666))
                        }
                    }
                }

                DetailSection(title: "Sâu bệnh thường gặp (xử lý thân thiện môi trường)") {
                    DiseaseBlocks(diseases: plant.commonDiseases ?? [])
                }

                DetailSection(title: "Bài thuốc dân gian liên quan") {
                    FolkBlocks(items: plant.folkRemedies ?? [])
                }

                if let disclaimer = plant.medicalDisclaimer, !disclaimer.isEmpty {
                    Text(disclaimer)
                        .font(.system(size: 11))
                        .foregroundStyle(Color(hex: 0x888888))
                        .padding(.top, 16)
                }

                Button(action: onScanAgain) {
                    Text("Quét cây khác")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color(hex: 0x19A451))
                }
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .padding(.top, 18)
                .padding(.bottom, 24)
            }
            .padding(16)
        }
    }
}

struct DiseaseResultView: View {
    let disease: PlantDiseaseData
    let image: UIImage?
    let onClose: () -> Void
    let onScanAgain: () -> Void

    var body: some View {
        DetailScaffold(title: "Kết quả chẩn đoán", onClose: onClose) {
            VStack(alignment: .leading, spacing: 0) {
                PlantHeroImage(image: image, imageUrl: nil)

                if disease.isPlantImage == false {
                    diagnosisTitle("Không thể nhận dạng cây này!")
                    Text("Hình ảnh không rõ ràng. Vui lòng lau camera và chụp lại.")
                        .foregroundStyle(Color(hex: 0x333333))
                        .padding(.top, 6)
                } else if disease.isHealthy == true {
                    diagnosisTitle("Tuyệt vời!")
                    Text("Các cây của bạn dường như đang phát triển rất tốt. Tiếp tục chăm sóc nhé!")
                        .foregroundStyle(Color(hex: 0x333333))
                        .padding(.top, 6)
                } else {
                    diagnosisTitle(VietnameseTextNormalizer.diseaseName(disease.matchedDiseaseName ?? disease.diseaseName))
                    Text("Độ tin cậy: \(disease.confidence ?? 0)%")
                        .foregroundStyle(Color(hex: 0x444444))
                        .padding(.top, 6)

                    DetailSection(title: "Ghi chú") {
                        Text(VietnameseTextNormalizer.text(disease.diseaseDetails?.displayText ?? disease.analysisNotes) ??
                             "Cắt bỏ phần bệnh, tăng thông gió, tránh tưới ướt lá vào buổi tối và theo dõi thêm.")
                    }
                }

                Button(action: onScanAgain) {
                    Text("Chẩn đoán cây khác")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color(hex: 0x19A451))
                }
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .padding(.top, 18)
                .padding(.bottom, 24)
            }
            .padding(16)
        }
    }

    private func diagnosisTitle(_ value: String) -> some View {
        Text(value)
            .font(.system(size: 22, weight: .semibold))
            .foregroundStyle(Color(hex: 0x1B1B1B))
            .padding(.top, 12)
    }
}

private struct DetailScaffold<Content: View>: View {
    let title: String
    let onClose: () -> Void
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                }
                Text(title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Spacer()
            }
            .padding(.horizontal, 4)
            .frame(height: 56)
            .background(Color(hex: 0x19A451))

            ScrollView(showsIndicators: false) {
                content
            }
            .background(Color(hex: 0xF4F4F4))
        }
        .background(Color(hex: 0xF4F4F4))
        .ignoresSafeArea(edges: .bottom)
    }
}

private struct PlantHeroImage: View {
    let image: UIImage?
    let imageUrl: String?

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else if let imageUrl, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let remote):
                        remote.resizable().scaledToFill()
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(height: 200)
        .frame(maxWidth: .infinity)
        .clipped()
        .background(Color(hex: 0xE0E0E0))
    }

    private var placeholder: some View {
        ZStack {
            Color(hex: 0xE0E0E0)
            Image(systemName: "camera.fill")
                .font(.system(size: 56))
                .foregroundStyle(Color(hex: 0x19A451))
        }
    }
}

private struct ToxicWarning: View {
    let toxicity: ToxicityData?

    var body: some View {
        if toxicity?.isToxic == true || toxicity?.warningMessage?.isEmpty == false {
            VStack(alignment: .leading, spacing: 6) {
                Text(warningTitle)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(Color(hex: 0xB71C1C))
                if let message = toxicity?.warningMessage, !message.isEmpty {
                    Text(message)
                        .foregroundStyle(Color(hex: 0x5D4037))
                }
                if let firstAid = toxicity?.firstAid, !firstAid.isEmpty {
                    Text("Sơ cứu: \(firstAid)")
                        .foregroundStyle(Color(hex: 0x3E2723))
                }
            }
            .font(.system(size: 14))
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(hex: 0xFFEBEE))
            .padding(.top, 12)
        }
    }

    private var warningTitle: String {
        if toxicity?.isFungus == true { return "Cảnh báo nấm độc" }
        if toxicity?.toxicSap == true { return "Cảnh báo nhựa độc" }
        return "Cảnh báo độc"
    }
}

private struct DetailSection<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color(hex: 0x1B1B1B))
            content
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: 0x333333))
                .lineSpacing(2)
        }
        .padding(.top, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct BulletText: View {
    let items: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(items.filter { !$0.isEmpty }, id: \.self) { item in
                Text("• \(item)")
            }
        }
    }
}

private struct DiseaseBlocks: View {
    let diseases: [CommonDiseaseData]

    var body: some View {
        if diseases.isEmpty {
            Text("Chưa có dữ liệu sâu bệnh thường gặp.")
        } else {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(diseases) { disease in
                    VStack(alignment: .leading, spacing: 4) {
                        if let imageUrl = disease.imageUrl, let url = URL(string: imageUrl) {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let image):
                                    image.resizable().scaledToFill()
                                default:
                                    Color(hex: 0xE0E0E0)
                                }
                            }
                            .frame(height: 140)
                            .clipped()
                        }
                        Text(disease.name ?? "")
                            .fontWeight(.bold)
                        Text(disease.description ?? "")
                        Text("Xử lý sinh thái: \(disease.ecoTreatment ?? "")")
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }
}

private struct FolkBlocks: View {
    let items: [FolkRemedyData]

    var body: some View {
        if items.isEmpty {
            Text("Chưa có bài thuốc dân gian liên quan.")
        } else {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(items) { item in
                    Text("• \(item.title ?? "")\n  \(item.ailment ?? "")\n  \(item.method ?? "")")
                }
            }
        }
    }
}

private enum ThesisCitationLabel {
    static func label(_ citation: CitationData) -> String {
        let title = citation.title?.isEmpty == false ? citation.title! : "Nguồn tham khảo"
        if (citation.type ?? "").lowercased() == "youtube" {
            return title
        }
        return "• \(title)"
    }
}
