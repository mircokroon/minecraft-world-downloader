using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Threading.Tasks;

namespace WorldDownloaderManager;

/// <summary>Thin wrapper around the docker CLI.</summary>
public class DockerService
{
    public Action<string>? OnOutput;

    private void Log(string text) => OnOutput?.Invoke(text);

    public async Task<(int code, string output)> RunAsync(IReadOnlyList<string> args, bool log = true)
    {
        var psi = new ProcessStartInfo
        {
            FileName = "docker",
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true,
            StandardOutputEncoding = Encoding.UTF8,
            StandardErrorEncoding = Encoding.UTF8,
        };
        foreach (var a in args) psi.ArgumentList.Add(a);

        var sb = new StringBuilder();
        try
        {
            if (log) Log("$ docker " + string.Join(" ", args));
            using var p = new Process { StartInfo = psi, EnableRaisingEvents = true };
            p.OutputDataReceived += (_, e) => { if (e.Data != null) { sb.AppendLine(e.Data); if (log) Log(e.Data); } };
            p.ErrorDataReceived += (_, e) => { if (e.Data != null) { sb.AppendLine(e.Data); if (log) Log(e.Data); } };
            p.Start();
            p.BeginOutputReadLine();
            p.BeginErrorReadLine();
            await p.WaitForExitAsync();
            return (p.ExitCode, sb.ToString());
        }
        catch (Exception ex)
        {
            Log("Error: " + ex.Message + "  (Is Docker Desktop installed and running?)");
            return (-1, ex.Message);
        }
    }

    public async Task<bool> IsDockerAvailableAsync()
    {
        var (code, _) = await RunAsync(new[] { "version", "--format", "{{.Server.Version}}" }, log: false);
        return code == 0;
    }

    public async Task<bool> IsRunningAsync(string container)
    {
        var (code, output) = await RunAsync(
            new[] { "ps", "--filter", $"name=^/{container}$", "--filter", "status=running", "--format", "{{.Names}}" },
            log: false);
        return code == 0 && output.Trim().Length > 0;
    }

    public Task RemoveAsync(string container) =>
        RunAsync(new[] { "rm", "-f", container }, log: false);

    public Task<(int, string)> PullAsync(string image) =>
        RunAsync(new[] { "pull", image });

    public Task<(int, string)> RunContainerAsync(Settings s)
    {
        var args = new List<string>
        {
            "run", "-d",
            "--name", s.ContainerName,
            "--restart", "unless-stopped",
            "-p", $"{s.WebPort}:8080",
            "-p", $"{s.ProxyPort}:25565",
            "-v", $"{s.DataFolder}:/data",
        };
        if (s.RequireLogin && !string.IsNullOrWhiteSpace(s.Password))
        {
            args.Add("-e"); args.Add($"WEB_USERNAME={s.Username}");
            args.Add("-e"); args.Add($"WEB_PASSWORD={s.Password}");
        }
        args.Add(s.Image);
        return RunAsync(args);
    }
}
