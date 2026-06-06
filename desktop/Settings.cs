using System;
using System.IO;
using System.Text.Json;

namespace WorldDownloaderManager;

public class Settings
{
    public string DataFolder { get; set; } = "";
    public int WebPort { get; set; } = 8080;
    public int ProxyPort { get; set; } = 25565;
    public string Image { get; set; } = "ghcr.io/cafepromenade/minecraft-world-downloader:latest";
    public string ContainerName { get; set; } = "minecraft-world-downloader";
    public bool RequireLogin { get; set; } = false;
    public string Username { get; set; } = "admin";
    public string Password { get; set; } = "";

    private static string FilePath
    {
        get
        {
            var dir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "WorldDownloaderManager");
            Directory.CreateDirectory(dir);
            return Path.Combine(dir, "settings.json");
        }
    }

    public static Settings Load()
    {
        Settings s = new();
        try
        {
            if (File.Exists(FilePath))
                s = JsonSerializer.Deserialize<Settings>(File.ReadAllText(FilePath)) ?? new Settings();
        }
        catch { /* fall back to defaults */ }

        if (string.IsNullOrWhiteSpace(s.DataFolder))
        {
            s.DataFolder = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                "WorldDownloader");
        }
        return s;
    }

    public void Save()
    {
        try
        {
            File.WriteAllText(FilePath,
                JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch { /* ignore */ }
    }
}
