import UIKit

class MainViewController: UIViewController {

    private let scrollView = UIScrollView()
    private let contentView = UIView()

    private let logoImageView: UIImageView = {
        let iv = UIImageView()
        iv.image = UIImage(systemName: "desktopcomputer")
        iv.tintColor = UIColor(red: 0.08, green: 0.40, blue: 0.75, alpha: 1.0)
        iv.contentMode = .scaleAspectFit
        iv.translatesAutoresizingMaskIntoConstraints = false
        return iv
    }()

    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = "CBT Exam Browser"
        label.font = .systemFont(ofSize: 28, weight: .bold)
        label.textColor = UIColor(red: 0.08, green: 0.40, blue: 0.75, alpha: 1.0)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let subtitleLabel: UILabel = {
        let label = UILabel()
        label.text = "SMKN 19 Jakarta"
        label.font = .systemFont(ofSize: 16)
        label.textColor = .secondaryLabel
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let urlTextField: UITextField = {
        let tf = UITextField()
        tf.placeholder = "Masukkan URL Test Center"
        tf.borderStyle = .roundedRect
        tf.keyboardType = .URL
        tf.autocapitalizationType = .none
        tf.autocorrectionType = .no
        tf.clearButtonMode = .whileEditing
        tf.font = .systemFont(ofSize: 16)
        tf.translatesAutoresizingMaskIntoConstraints = false
        return tf
    }()

    private let scanQRButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("  Scan QR Code", for: .normal)
        button.setImage(UIImage(systemName: "qrcode.viewfinder"), for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16)
        button.layer.borderWidth = 1
        button.layer.borderColor = UIColor(red: 0.08, green: 0.40, blue: 0.75, alpha: 1.0).cgColor
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    private let userAgentSwitch: UISwitch = {
        let sw = UISwitch()
        sw.isOn = true
        sw.translatesAutoresizingMaskIntoConstraints = false
        return sw
    }()

    private let userAgentLabel: UILabel = {
        let label = UILabel()
        label.text = "Custom User Agent"
        label.font = .systemFont(ofSize: 16, weight: .medium)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let userAgentStatusLabel: UILabel = {
        let label = UILabel()
        label.text = "User Agent: cbt-exam-browser"
        label.font = .systemFont(ofSize: 12)
        label.textColor = .secondaryLabel
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let startButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("MULAI UJIAN", for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 18, weight: .bold)
        button.setTitleColor(.white, for: .normal)
        button.backgroundColor = UIColor(red: 0.08, green: 0.40, blue: 0.75, alpha: 1.0)
        button.layer.cornerRadius = 12
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.systemGroupedBackground
        navigationController?.isNavigationBarHidden = true

        setupUI()
        loadSavedURL()
    }

    private func setupUI() {
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        contentView.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(scrollView)
        scrollView.addSubview(contentView)

        [logoImageView, titleLabel, subtitleLabel, urlTextField,
         scanQRButton, userAgentLabel, userAgentStatusLabel,
         userAgentSwitch, startButton].forEach { contentView.addSubview($0) }

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),

            logoImageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 80),
            logoImageView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            logoImageView.widthAnchor.constraint(equalToConstant: 80),
            logoImageView.heightAnchor.constraint(equalToConstant: 80),

            titleLabel.topAnchor.constraint(equalTo: logoImageView.bottomAnchor, constant: 16),
            titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),

            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4),
            subtitleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),

            urlTextField.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: 40),
            urlTextField.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            urlTextField.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            urlTextField.heightAnchor.constraint(equalToConstant: 48),

            scanQRButton.topAnchor.constraint(equalTo: urlTextField.bottomAnchor, constant: 12),
            scanQRButton.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            scanQRButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            scanQRButton.heightAnchor.constraint(equalToConstant: 48),

            userAgentLabel.topAnchor.constraint(equalTo: scanQRButton.bottomAnchor, constant: 24),
            userAgentLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),

            userAgentSwitch.centerYAnchor.constraint(equalTo: userAgentLabel.centerYAnchor),
            userAgentSwitch.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),

            userAgentStatusLabel.topAnchor.constraint(equalTo: userAgentLabel.bottomAnchor, constant: 4),
            userAgentStatusLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),

            startButton.topAnchor.constraint(equalTo: userAgentStatusLabel.bottomAnchor, constant: 40),
            startButton.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            startButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            startButton.heightAnchor.constraint(equalToConstant: 56),
            startButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -40),
        ])

        startButton.addTarget(self, action: #selector(startExamTapped), for: .touchUpInside)
        scanQRButton.addTarget(self, action: #selector(scanQRTapped), for: .touchUpInside)
        userAgentSwitch.addTarget(self, action: #selector(userAgentToggled), for: .valueChanged)
    }

    private func loadSavedURL() {
        if let savedURL = UserDefaults.standard.string(forKey: "last_url"), !savedURL.isEmpty {
            urlTextField.text = savedURL
        }
    }

    @objc private func startExamTapped() {
        guard var urlString = urlTextField.text?.trimmingCharacters(in: .whitespacesAndNewlines),
              !urlString.isEmpty else {
            showAlert(title: "Error", message: "Masukkan URL test center")
            return
        }

        if !urlString.hasPrefix("http://") && !urlString.hasPrefix("https://") {
            urlString = "http://\(urlString)"
        }

        UserDefaults.standard.set(urlString, forKey: "last_url")

        let alert = UIAlertController(
            title: "Mulai Ujian",
            message: "Anda akan memulai ujian.\n\nPastikan:\n- Koneksi internet stabil\n- Baterai cukup\n- Jangan keluar dari aplikasi\n\nLanjutkan?",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "Batal", style: .cancel))
        alert.addAction(UIAlertAction(title: "Mulai", style: .default) { [weak self] _ in
            let examVC = ExamViewController()
            examVC.examURL = urlString
            examVC.useCustomUserAgent = self?.userAgentSwitch.isOn ?? true
            examVC.modalPresentationStyle = .fullScreen
            self?.present(examVC, animated: true)
        })
        present(alert, animated: true)
    }

    @objc private func scanQRTapped() {
        showAlert(title: "QR Scanner", message: "QR Scanner memerlukan kamera. Fitur ini tersedia saat app di-install di perangkat.")
    }

    @objc private func userAgentToggled() {
        userAgentStatusLabel.text = userAgentSwitch.isOn
            ? "User Agent: cbt-exam-browser"
            : "User Agent: Default"
    }

    private func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
