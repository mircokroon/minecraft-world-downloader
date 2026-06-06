using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace WorldDownloaderManager;

public partial class MainWindow : Window
{
    private readonly Settings _settings;
    private readonly DockerService _docker = new();

    public MainWindow()
    {
        InitializeComponent();

        _settings = Settings.Load();
        _docker.OnOutput = AppendLog;

        FolderBox.Text = _settings.DataFolder;
        WebPortBox.Text = _settings.WebPort.ToString();
        ProxyPortBox.Text = _settings.ProxyPort.ToString();
        ImageBox.Text = _settings.Image;
        LoginCheck.IsChecked = _settings.RequireLogin;
        UserBox.Text = _settings.Username;
        PassBox.Password = _settings.Password;
        LoginPanel.Visibility = _settings.RequireLogin ? Visibility.Visible : Visibility.Collapsed;

        Loaded += async (_, _) => await InitAsync();
    }

    private async Task InitAsync()
    {
        if (!await _docker.IsDockerAvailableAsync())
        {
            SetStatus("error", "Docker not found. Install Docker Desktop and make sure it is running, then reopen this app.");
            return;
        }
        await RefreshStatusAsync();
    }

    private static int ParsePort(string text, int fallback) =>
        int.TryParse(text, out var v) && v is > 0 and < 65536 ? v : fallback;

    private void SaveFromUi()
    {
        _settings.DataFolder = FolderBox.Text.Trim();
        _settings.WebPort = ParsePort(WebPortBox.Text, 8080);
        _settings.ProxyPort = ParsePort(ProxyPortBox.Text, 25565);
        if (!string.IsNullOrWhiteSpace(ImageBox.Text)) _settings.Image = ImageBox.Text.Trim();
        _settings.RequireLogin = LoginCheck.IsChecked == true;
        _settings.Username = UserBox.Text.Trim();
        _settings.Password = PassBox.Password;
        _settings.Save();
    }

    private void SetStatus(string kind, string text)
    {
        string bg = kind switch
        {
            "success" => "#16291E",
            "error" => "#2A1718",
            "warn" => "#2A2516",
            _ => "#1B2733",
        };
        string fg = kind switch
        {
            "success" => "#3DDC84",
            "error" => "#FF8A85",
            "warn" => "#FFCA52",
            _ => "#E9EEF5",
        };
        StatusBorder.Background = Brush(bg);
        StatusText.Foreground = Brush(fg);
        StatusText.Text = text;
    }

    private static SolidColorBrush Brush(string hex) =>
        new((Color)ColorConverter.ConvertFromString(hex));

    private void Busying(bool on)
    {
        Busy.Visibility = on ? Visibility.Visible : Visibility.Collapsed;
        StartBtn.IsEnabled = !on;
        StopBtn.IsEnabled = !on;
        PullBtn.IsEnabled = !on;
        BrowseBtn.IsEnabled = !on;
    }

    private void AppendLog(string line) => Dispatcher.Invoke(() =>
    {
        LogText.AppendText(line + Environment.NewLine);
        LogText.ScrollToEnd();
    });

    private async Task RefreshStatusAsync()
    {
        bool running = await _docker.IsRunningAsync(_settings.ContainerName);
        StartBtn.IsEnabled = !running;
        StopBtn.IsEnabled = running;
        if (running)
            SetStatus("success", $"Running — console at http://localhost:{_settings.WebPort}   •   Minecraft proxy on localhost:{_settings.ProxyPort}");
        else
            SetStatus("info", "Stopped. Press Start to launch the console.");
    }

    private void Browse_Click(object sender, RoutedEventArgs e)
    {
        var dlg = new Microsoft.Win32.OpenFolderDialog { Title = "Choose the data folder" };
        if (!string.IsNullOrWhiteSpace(FolderBox.Text) && Directory.Exists(FolderBox.Text))
            dlg.InitialDirectory = FolderBox.Text;
        if (dlg.ShowDialog() == true)
            FolderBox.Text = dlg.FolderName;
    }

    private void Login_Changed(object sender, RoutedEventArgs e) =>
        LoginPanel.Visibility = LoginCheck.IsChecked == true ? Visibility.Visible : Visibility.Collapsed;

    private async void Start_Click(object sender, RoutedEventArgs e)
    {
        SaveFromUi();
        if (string.IsNullOrWhiteSpace(_settings.DataFolder))
        {
            SetStatus("warn", "Pick a data folder first — choose where worlds should be stored.");
            return;
        }
        Busying(true);
        try
        {
            Directory.CreateDirectory(_settings.DataFolder);
            await _docker.RemoveAsync(_settings.ContainerName);
            var (code, _) = await _docker.RunContainerAsync(_settings);
            if (code == 0)
            {
                SetStatus("success", $"Started. Opening http://localhost:{_settings.WebPort} …");
                OpenConsole();
            }
            else
            {
                SetStatus("error", "Failed to start — see the output below. If the image is missing, press “Update image” first.");
            }
        }
        finally
        {
            Busying(false);
            await RefreshStatusAsync();
        }
    }

    private async void Stop_Click(object sender, RoutedEventArgs e)
    {
        Busying(true);
        try { await _docker.RemoveAsync(_settings.ContainerName); AppendLog("Stopped."); }
        finally { Busying(false); await RefreshStatusAsync(); }
    }

    private async void Pull_Click(object sender, RoutedEventArgs e)
    {
        SaveFromUi();
        Busying(true);
        try { await _docker.PullAsync(_settings.Image); }
        finally { Busying(false); }
    }

    private void Open_Click(object sender, RoutedEventArgs e) => OpenConsole();

    private void OpenConsole()
    {
        try
        {
            int port = ParsePort(WebPortBox.Text, 8080);
            Process.Start(new ProcessStartInfo($"http://localhost:{port}") { UseShellExecute = true });
        }
        catch (Exception ex) { AppendLog("Could not open browser: " + ex.Message); }
    }
}
