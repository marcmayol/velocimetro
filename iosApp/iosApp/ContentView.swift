import SwiftUI
import UIKit
import ComposeApp

/// Toda la pantalla es la de Compose (app/src/commonMain); aquí solo se incrusta.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // El teclado lo gestiona Compose (imePadding), no SwiftUI.
        ComposeView().ignoresSafeArea(.keyboard)
    }
}
