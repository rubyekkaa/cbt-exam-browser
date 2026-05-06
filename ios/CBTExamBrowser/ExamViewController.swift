import UIKit
import WebKit

class ExamViewController: UIViewController {

    var examURL: String = ""
    var useCustomUserAgent: Bool = true

    private var webView: WKWebView!
    private let toolbarView = UIView()
    private let timeLabel = UILabel()
    private let progressView = UIProgressView()
    private var timer: Timer?
    private var currentZoom: CGFloat = 1.0

    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge { .all }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        // Prevent screenshots
        setupScreenshotPrevention()

        setupToolbar()
        setupWebView()
        setupProgressView()
        startTimer()

        if let url = URL(string: examURL) {
            webView.load(URLRequest(url: url))
        }
    }

    private func setupScreenshotPrevention() {
        let field = UITextField()
        field.isSecureTextEntry = true
        view.addSubview(field)
        field.centerYAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
        field.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        view.layer.superlayer?.addSublayer(field.layer)
        field.layer.sublayers?.first?.addSublayer(view.layer)
    }

    private func setupToolbar() {
        toolbarView.backgroundColor = UIColor(red: 0.10, green: 0.10, blue: 0.18, alpha: 1.0)
        toolbarView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(toolbarView)

        NSLayoutConstraint.activate([
            toolbarView.topAnchor.constraint(equalTo: view.topAnchor),
            toolbarView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            toolbarView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            toolbarView.heightAnchor.constraint(equalToConstant: 44),
        ])

        let stackView = UIStackView()
        stackView.axis = .horizontal
        stackView.spacing = 8
        stackView.alignment = .center
        stackView.translatesAutoresizingMaskIntoConstraints = false
        toolbarView.addSubview(stackView)

        NSLayoutConstraint.activate([
            stackView.leadingAnchor.constraint(equalTo: toolbarView.leadingAnchor, constant: 8),
            stackView.trailingAnchor.constraint(equalTo: toolbarView.trailingAnchor, constant: -8),
            stackView.centerYAnchor.constraint(equalTo: toolbarView.centerYAnchor),
        ])

        let backBtn = makeToolbarButton(systemName: "chevron.left", action: #selector(goBack))
        let forwardBtn = makeToolbarButton(systemName: "chevron.right", action: #selector(goForward))
        let refreshBtn = makeToolbarButton(systemName: "arrow.clockwise", action: #selector(refresh))
        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        let zoomOutBtn = makeToolbarButton(systemName: "minus.magnifyingglass", action: #selector(zoomOut))
        let zoomInBtn = makeToolbarButton(systemName: "plus.magnifyingglass", action: #selector(zoomIn))

        timeLabel.font = .monospacedDigitSystemFont(ofSize: 14, weight: .bold)
        timeLabel.textColor = UIColor(red: 0.31, green: 0.76, blue: 0.97, alpha: 1.0)
        timeLabel.text = "00:00:00"

        let exitBtn = makeToolbarButton(systemName: "xmark.circle.fill", action: #selector(exitTapped))
        exitBtn.tintColor = UIColor(red: 0.94, green: 0.33, blue: 0.31, alpha: 1.0)

        [backBtn, forwardBtn, refreshBtn, spacer, zoomOutBtn, zoomInBtn, timeLabel, exitBtn].forEach {
            stackView.addArrangedSubview($0)
        }
    }

    private func makeToolbarButton(systemName: String, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        button.setImage(UIImage(systemName: systemName), for: .normal)
        button.tintColor = .lightGray
        button.widthAnchor.constraint(equalToConstant: 36).isActive = true
        button.heightAnchor.constraint(equalToConstant: 36).isActive = true
        button.addTarget(self, action: action, for: .touchUpInside)
        return button
    }

    private func setupWebView() {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.preferences.javaScriptEnabled = true

        webView = WKWebView(frame: .zero, configuration: config)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.navigationDelegate = self
        webView.allowsBackForwardNavigationGestures = false

        if useCustomUserAgent {
            webView.customUserAgent = "cbt-exam-browser"
        }

        view.addSubview(webView)
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: toolbarView.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    private func setupProgressView() {
        progressView.translatesAutoresizingMaskIntoConstraints = false
        progressView.tintColor = UIColor(red: 0.08, green: 0.40, blue: 0.75, alpha: 1.0)
        view.addSubview(progressView)

        NSLayoutConstraint.activate([
            progressView.topAnchor.constraint(equalTo: toolbarView.bottomAnchor),
            progressView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            progressView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            progressView.heightAnchor.constraint(equalToConstant: 3),
        ])

        webView.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
    }

    override func observeValue(
        forKeyPath keyPath: String?,
        of object: Any?,
        change: [NSKeyValueChangeKey: Any]?,
        context: UnsafeMutableRawPointer?
    ) {
        if keyPath == "estimatedProgress" {
            progressView.progress = Float(webView.estimatedProgress)
            progressView.isHidden = webView.estimatedProgress >= 1.0
        }
    }

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            let formatter = DateFormatter()
            formatter.dateFormat = "HH:mm:ss"
            self?.timeLabel.text = formatter.string(from: Date())
        }
    }

    @objc private func goBack() {
        if webView.canGoBack { webView.goBack() }
    }

    @objc private func goForward() {
        if webView.canGoForward { webView.goForward() }
    }

    @objc private func refresh() {
        webView.reload()
    }

    @objc private func zoomIn() {
        currentZoom = min(currentZoom + 0.1, 2.0)
        applyZoom()
    }

    @objc private func zoomOut() {
        currentZoom = max(currentZoom - 0.1, 0.5)
        applyZoom()
    }

    private func applyZoom() {
        let js = "document.body.style.zoom = '\(currentZoom)'"
        webView.evaluateJavaScript(js, completionHandler: nil)
    }

    @objc private func exitTapped() {
        let alert = UIAlertController(
            title: "Keluar Ujian",
            message: "Apakah Anda yakin ingin keluar dari ujian?\n\nPeringatan: Keluar saat ujian berlangsung dapat mempengaruhi nilai Anda.",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "Lanjut Ujian", style: .cancel))
        alert.addAction(UIAlertAction(title: "Keluar", style: .destructive) { [weak self] _ in
            self?.dismiss(animated: true)
        })
        present(alert, animated: true)
    }

    deinit {
        timer?.invalidate()
        webView?.removeObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress))
    }
}

extension ExamViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        // Only allow http/https URLs
        if let url = navigationAction.request.url,
           (url.scheme == "http" || url.scheme == "https") {
            decisionHandler(.allow)
        } else {
            decisionHandler(.cancel)
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        let alert = UIAlertController(
            title: "Error",
            message: error.localizedDescription,
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
