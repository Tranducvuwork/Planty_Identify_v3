import SwiftUI
import UIKit

struct MainView: View {
    @StateObject private var viewModel = ScanViewModel()
    @State private var screen: PlantyScreen = .home
    @State private var selectedTab: HomeTab = .home
    @State private var pickerSource: UIImagePickerController.SourceType?

    var body: some View {
        ZStack {
            switch screen {
            case .home:
                HomeScreen(
                    selectedTab: $selectedTab,
                    onCamera: { openCamera(mode: .identify) },
                    onIdentify: { openCamera(mode: .identify) },
                    onDiagnose: { openCamera(mode: .diagnose) }
                )
            case .camera:
                CameraFlowScreen(
                    mode: $viewModel.mode,
                    selectedImage: viewModel.selectedImage,
                    onClose: { screen = .home },
                    onCapture: { openPicker(.camera) },
                    onGallery: { openPicker(.photoLibrary) }
                )
            case .processing:
                ProcessingScreen(
                    mode: viewModel.mode,
                    image: viewModel.selectedImage,
                    errorMessage: viewModel.errorMessage,
                    onClose: { screen = .camera },
                    onRetry: { startProcessing() }
                )
            case .result:
                ResultHostScreen(
                    mode: viewModel.mode,
                    image: viewModel.selectedImage,
                    result: viewModel.result,
                    onClose: { screen = .home },
                    onScanAgain: { screen = .camera }
                )
            }
        }
        .preferredColorScheme(screen == .camera || screen == .processing ? .dark : .light)
        .sheet(item: Binding(
            get: { pickerSource.map { PickerSource(source: $0) } },
            set: { pickerSource = $0?.source }
        )) { source in
            ImagePicker(sourceType: source.source) { image in
                viewModel.selectedImage = image
                startProcessing()
            }
        }
    }

    private func openCamera(mode: ScanMode) {
        viewModel.mode = mode
        screen = .camera
    }

    private func openPicker(_ source: UIImagePickerController.SourceType) {
        let resolvedSource: UIImagePickerController.SourceType =
            UIImagePickerController.isSourceTypeAvailable(source) ? source : .photoLibrary
        pickerSource = resolvedSource
    }

    private func startProcessing() {
        screen = .processing
        Task {
            await viewModel.runScan()
            await MainActor.run {
                if viewModel.errorMessage == nil, viewModel.result != nil {
                    screen = .result
                }
            }
        }
    }
}

private enum PlantyScreen {
    case home
    case camera
    case processing
    case result
}

private enum HomeTab: String, CaseIterable, Identifiable {
    case home = "Home"
    case explore = "Explore"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: return "Trang chủ"
        case .explore: return "Khám phá"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .explore: return "safari.fill"
        }
    }
}

private struct HomeScreen: View {
    @Binding var selectedTab: HomeTab
    let onCamera: () -> Void
    let onIdentify: () -> Void
    let onDiagnose: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color(hex: 0xF4F4F4).ignoresSafeArea()
            ScrollView(showsIndicators: false) {
                if selectedTab == .home {
                    VStack(alignment: .leading, spacing: 0) {
                        HomeHeader()
                        ScanBanner(onTap: onCamera)
                            .padding(.horizontal, 16)
                            .padding(.top, 20)
                        ThesisExtras()
                            .padding(.top, 16)
                        FeatureSection(onIdentify: onIdentify, onDiagnose: onDiagnose)
                            .padding(.bottom, 120)
                    }
                } else {
                    ExploreScreen(
                        onIdentify: onIdentify,
                        onDiagnose: onDiagnose
                    )
                        .padding(.bottom, 120)
                }
            }
            .ignoresSafeArea(edges: selectedTab == .home ? Edge.Set.top : Edge.Set())
            ChatButton()
                .padding(.trailing, 12)
                .padding(.bottom, 92)
                .frame(maxWidth: .infinity, alignment: .trailing)
            BottomNav(selectedTab: $selectedTab, onCamera: onCamera)
        }
    }
}

private struct FeatureSection: View {
    let onIdentify: () -> Void
    let onDiagnose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Tất cả tính năng")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color(hex: 0x1B1B1B))
                .padding(.horizontal, 18)
            HStack(spacing: 16) {
                FeatureCard(
                    title: "Nhận dạng",
                    subtitle: "Tìm hiểu cây của bạn",
                    tint: Color(hex: 0xEEF5E6),
                    assetName: "IdentifyCard",
                    onTap: onIdentify
                )
                FeatureCard(
                    title: "Chẩn đoán",
                    subtitle: "Tìm hiểu bệnh cây",
                    tint: Color(hex: 0xFFF4E5),
                    assetName: "DiagnoseCard",
                    onTap: onDiagnose
                )
            }
            .padding(.horizontal, 18)
        }
        .padding(.top, 10)
    }
}

private struct ExploreScreen: View {
    let onIdentify: () -> Void
    let onDiagnose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Khám phá")
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(Color(hex: 0x1B1B1B))
                .padding(.horizontal, 18)
                .padding(.top, 22)

            SearchPill()
                .padding(.horizontal, 18)

            FeatureSection(onIdentify: onIdentify, onDiagnose: onDiagnose)

            Text("Danh mục phổ biến")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color(hex: 0x1B1B1B))
                .padding(.horizontal, 18)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 14) {
                ExploreCategory(title: "Hoa", icon: "camera.macro")
                ExploreCategory(title: "Cây trong nhà", icon: "house.lodge.fill")
                ExploreCategory(title: "Thảo mộc", icon: "leaf.fill")
                ExploreCategory(title: "Cây thuốc", icon: "cross.vial.fill")
                ExploreCategory(title: "Sâu bệnh", icon: "bandage.fill")
                ExploreCategory(title: "Chăm sóc", icon: "drop.fill")
            }
            .padding(.horizontal, 18)
        }
    }
}

private struct ExploreCategory: View {
    let title: String
    let icon: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color(hex: 0x19A451))
                .frame(width: 36, height: 36)
                .background(Color(hex: 0xEAF7EE))
                .clipShape(Circle())
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Color(hex: 0x1B1B1B))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Spacer()
        }
        .padding(12)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct HomeHeader: View {
    var body: some View {
        ZStack(alignment: .bottom) {
            Image("HomeHeader")
                .resizable()
                .scaledToFill()
            .frame(height: 200)
            .clipped()

            VStack(spacing: 20) {
                WeatherCard()
                    .padding(.horizontal, 16)
                    .padding(.top, 44)
                SearchPill()
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
            }
        }
        .frame(height: 220)
    }
}

private struct WeatherCard: View {
    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Chạm để bật vị trí và nhận cập nhật thời tiết")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                Text("Theo dõi điều kiện chăm cây")
                    .font(.system(size: 12))
                    .foregroundStyle(.white.opacity(0.86))
            }
            Spacer()
        }
    }
}

private struct SearchPill: View {
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Color(hex: 0x9A9A9A))
            Text("Tìm cây & Hoa")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Color(hex: 0x9A9A9A))
            Spacer()
        }
        .frame(height: 51)
        .padding(.horizontal, 16)
        .background(.white)
        .clipShape(Capsule())
        .shadow(color: .black.opacity(0.12), radius: 7, x: 0, y: 3)
        .overlay(Capsule().stroke(.black.opacity(0.08), lineWidth: 1))
    }
}

private struct ScanBanner: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .leading) {
                Image("ScanBanner")
                    .resizable()
                    .scaledToFill()
                Image("TryProPlant")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 210, height: 80)
                    .padding(.leading, 4)
            }
            .frame(minHeight: 140)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct ThesisExtras: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Nhật ký & cây thuốc")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(Color(hex: 0x1B1B1B))
            ThesisButton(title: "Nhật ký cây trồng", icon: "book.closed.fill")
            ThesisButton(title: "Danh mục cây thuốc", icon: "cross.vial.fill")
            ThesisButton(title: "Bài thuốc dân gian", icon: "text.book.closed.fill")
        }
        .padding(.horizontal, 18)
    }
}

private struct ThesisButton: View {
    let title: String
    let icon: String

    var body: some View {
        HStack {
            Image(systemName: icon)
            Text(title)
                .font(.system(size: 15, weight: .semibold))
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 15)
        .background(Color(hex: 0x19A451))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct FeatureCard: View {
    let title: String
    let subtitle: String
    let tint: Color
    let assetName: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .topLeading) {
                tint
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color(hex: 0x1B1B1B))
                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(Color(hex: 0x777777))
                    Spacer()
                    HStack {
                        Spacer()
                        Image(assetName)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 150, height: 150)
                            .clipped()
                    }
                }
                .padding(14)
            }
            .frame(height: 220)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .shadow(color: .black.opacity(0.08), radius: 3, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

private struct ChatButton: View {
    var body: some View {
        ZStack {
            Circle()
                .fill(LinearGradient(colors: [Color(hex: 0x20C463), Color(hex: 0x0D9F49)], startPoint: .top, endPoint: .bottom))
            Image(systemName: "message.fill")
                .font(.system(size: 25, weight: .bold))
                .foregroundStyle(.white)
        }
        .frame(width: 60, height: 60)
        .shadow(color: .black.opacity(0.2), radius: 10, y: 4)
    }
}

private struct BottomNav: View {
    @Binding var selectedTab: HomeTab
    let onCamera: () -> Void

    var body: some View {
        ZStack(alignment: .top) {
            HStack {
                navItem(.home)
                Spacer().frame(width: 96)
                navItem(.explore)
            }
            .padding(.horizontal, 44)
            .padding(.top, 12)
            .padding(.bottom, 18)
            .frame(maxWidth: .infinity)
            .background(.white)
            .shadow(color: .black.opacity(0.12), radius: 10, x: 0, y: -2)

            Button(action: onCamera) {
                ZStack {
                    Circle().fill(Color(hex: 0x18B65B))
                    Circle().stroke(.white, lineWidth: 5)
                    Image(systemName: "camera.fill")
                        .font(.system(size: 29, weight: .bold))
                        .foregroundStyle(.white)
                }
                .frame(width: 66, height: 66)
                .offset(y: -24)
                .shadow(color: Color(hex: 0x18B65B).opacity(0.35), radius: 8, y: 5)
            }
            .buttonStyle(.plain)
        }
    }

    private func navItem(_ tab: HomeTab) -> some View {
        Button {
            selectedTab = tab
        } label: {
            VStack(spacing: 5) {
                Image(systemName: tab.icon)
                    .font(.system(size: 21, weight: .medium))
                Text(tab.title)
                    .font(.system(size: 10, weight: .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .foregroundStyle(selectedTab == tab ? Color(hex: 0x19A451) : Color(hex: 0x9E9E9E))
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

private struct CameraFlowScreen: View {
    @Binding var mode: ScanMode
    let selectedImage: UIImage?
    let onClose: () -> Void
    let onCapture: () -> Void
    let onGallery: () -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    IconButton(systemName: "xmark", action: onClose)
                    Spacer()
                    Text("Trực tuyến")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(Color(hex: 0x19A451))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(.white)
                        .clipShape(Capsule())
                    Spacer()
                    IconButton(systemName: "bolt.slash.fill", action: {})
                }
                .padding(.horizontal, 16)
                .padding(.top, 14)

                ZStack {
                    if let selectedImage {
                        Image(uiImage: selectedImage)
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        Rectangle()
                            .fill(Color(hex: 0x101010))
                            .overlay(
                                VStack(spacing: 12) {
                                    Image(systemName: "camera.viewfinder")
                                        .font(.system(size: 68))
                                    Text("Đưa cây vào khung hình")
                                        .font(.system(size: 16, weight: .medium))
                                }
                                .foregroundStyle(.white.opacity(0.68))
                            )
                    }
                    FocusFrame()
                }
                .padding(.top, 18)
                .padding(.bottom, 28)

                ModeTabs(mode: $mode)
                    .padding(.horizontal, 74)
                    .padding(.bottom, 24)

                HStack(alignment: .center) {
                    Button(action: onGallery) {
                        Image(systemName: "photo")
                            .font(.system(size: 28, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(width: 56, height: 56)
                    }
                    Spacer()
                    Button(action: onCapture) {
                        ZStack {
                            Circle().fill(.white)
                            Circle().stroke(Color(hex: 0x19A451), lineWidth: 6)
                            Image(systemName: mode == .identify ? "camera.fill" : "cross.case.fill")
                                .font(.system(size: 26, weight: .bold))
                                .foregroundStyle(Color(hex: 0x19A451))
                        }
                        .frame(width: 65, height: 65)
                    }
                    Spacer()
                    VStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 38))
                        Text("Trực tuyến")
                            .font(.system(size: 13, weight: .medium))
                    }
                    .foregroundStyle(Color(hex: 0x19A451))
                    .frame(width: 56)
                }
                .padding(.horizontal, 60)
                .padding(.bottom, 32)
            }
        }
    }
}

private struct ModeTabs: View {
    @Binding var mode: ScanMode

    var body: some View {
        HStack(spacing: 0) {
            modeButton(.identify)
            modeButton(.diagnose)
        }
        .padding(4)
        .background(Color(hex: 0x222222))
        .clipShape(Capsule())
    }

    private func modeButton(_ value: ScanMode) -> some View {
        Button {
            mode = value
        } label: {
            Text(value == .identify ? "NHẬN DẠNG" : "CHẨN ĐOÁN")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(mode == value ? Color(hex: 0x19A451) : .white.opacity(0.65))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(mode == value ? .white : .clear)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct FocusFrame: View {
    var body: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width * 0.68, proxy.size.height * 0.55)
            RoundedRectangle(cornerRadius: 24)
                .stroke(.white.opacity(0.78), style: StrokeStyle(lineWidth: 3, dash: [28, 16]))
                .frame(width: size, height: size)
                .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
        }
        .allowsHitTesting(false)
    }
}

private struct ProcessingScreen: View {
    let mode: ScanMode
    let image: UIImage?
    let errorMessage: String?
    let onClose: () -> Void
    let onRetry: () -> Void

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack {
                HStack {
                    IconButton(systemName: "xmark", action: onClose)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 14)

                Spacer()

                ZStack {
                    RoundedRectangle(cornerRadius: 22)
                        .stroke(Color(hex: 0x19A451), lineWidth: 3)
                        .frame(width: 250, height: 250)
                    if let image {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 220, height: 220)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    } else {
                        Image(systemName: "leaf.fill")
                            .font(.system(size: 88))
                            .foregroundStyle(Color(hex: 0x19A451))
                    }
                    Rectangle()
                        .fill(Color(hex: 0x19A451).opacity(0.8))
                        .frame(width: 230, height: 3)
                        .offset(y: -70)
                }

                VStack(spacing: 12) {
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.white)
                            .multilineTextAlignment(.center)
                        Button("Thử lại", action: onRetry)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 28)
                            .padding(.vertical, 12)
                            .background(Color(hex: 0x19A451))
                            .clipShape(Capsule())
                    } else {
                        ProgressView()
                            .tint(Color(hex: 0x19A451))
                        Text(mode == .identify ? "Đang nhận dạng đặc điểm" : "Đang chẩn đoán bệnh")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(.white)
                        Text(mode == .identify ? "Nhận dạng cây hoàn tất." : "Chẩn đoán bệnh hoàn tất.")
                            .font(.system(size: 14))
                            .foregroundStyle(.white.opacity(0.68))
                    }
                }
                .padding(.top, 40)

                Spacer()
            }
        }
    }
}

private struct ResultHostScreen: View {
    let mode: ScanMode
    let image: UIImage?
    let result: ScanResult?
    let onClose: () -> Void
    let onScanAgain: () -> Void

    var body: some View {
        switch result {
        case .plant(let plant):
            PlantResultView(plant: plant, image: image, onClose: onClose, onScanAgain: onScanAgain)
        case .disease(let disease):
            DiseaseResultView(disease: disease, image: image, onClose: onClose, onScanAgain: onScanAgain)
        case .none:
            ProcessingScreen(mode: mode, image: image, errorMessage: "Không có dữ liệu kết quả.", onClose: onClose, onRetry: onScanAgain)
        }
    }
}

private struct IconButton: View {
    let systemName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(.white.opacity(0.18))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
    }
}

final class ScanViewModel: ObservableObject {
    @Published var mode: ScanMode = .identify
    @Published var selectedImage: UIImage?
    @Published var result: ScanResult?
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let apiClient = PlantAPIClient()

    @MainActor
    func runScan() async {
        guard let selectedImage else { return }
        isLoading = true
        errorMessage = nil
        result = nil
        defer { isLoading = false }

        do {
            switch mode {
            case .identify:
                result = .plant(try await apiClient.scanPlant(image: selectedImage))
            case .diagnose:
                result = .disease(try await apiClient.diagnosePlant(image: selectedImage))
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private struct PickerSource: Identifiable {
    let source: UIImagePickerController.SourceType
    var id: Int { source.rawValue }
}

extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}
