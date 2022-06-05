package proxy.auth;

import config.Config;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Retrieve authentication details from the Minecraft process.
 *
 * When Minecraft is launched, the launcher passes the username, user ID and access token to the process as arguments.
 * We can retrieve these to use for authentication, as long as the game is already running.
 */
public class AuthDetailsFromProcess {
    private String username;

    public AuthDetailsFromProcess() { }

    public AuthDetailsFromProcess(String username) {
        this.username = username;
        if (username == null || username.equals("")) {
            this.username = Config.getUsername();
        }
    }

    public AuthDetails getDetails() throws IOException {
        List<String> candidates = findCandidateProcesses();
        if (candidates.isEmpty()) {
            return AuthDetails.INVALID;
        }

        Stream<AuthDetails> details = candidates.stream().map(this::parseDetails);

        // filter for the specific username - only really useful if there's multiple instances open
//        if (username != null && !username.equals("")) {
//            details = details.filter(d -> d.name.equalsIgnoreCase(username));
//        }

        return details.findAny().orElse(AuthDetails.INVALID);
    }

    /**
     * Get the relevant information from the arguments.
     */
    private AuthDetails parseDetails(String input) {
        String[] parts = input.split(" ");

        String username = "";
        String uuid = "";
        String accessToken = "";

        for (int i = 0; i < parts.length; i++) {
            switch (parts[i]) {
                case "--username": username = parts[++i]; break;
                case "--uuid": uuid = parts[++i]; break;
                case "--accessToken": accessToken = parts[++i]; break;
            }
        }

        return new AuthDetails(username, uuid, accessToken);
    }

    private List<String> findCandidateProcesses() throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            return findJavawProcessWindows().stream()
                    .map(this::getLaunchParametersWindows)
                    .filter(line -> line.contains("--accessToken"))
                    .collect(Collectors.toList());
        } else {
            return findCandidateProcessUnix().stream()
                    .map(a -> "" + a).collect(Collectors.toList());
        }
    }

    /**
     * On windows, we use tasklist.exe to get all javaw processes. We return the PID of each as it's a few more steps
     * to retrieve the launch parameters.
     */
    private List<Long> findJavawProcessWindows() throws IOException {
        Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\tasklist.exe");

        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<Long> res = new ArrayList<>();
        String line;
        while ((line = input.readLine()) != null) {
            if (line.startsWith("javaw.exe") || line.startsWith("java.exe")) {
                String[] parts = line.split(" +");
                res.add(Long.valueOf(parts[1]));
            }
        }
        input.close();

        return res;
    }

    /**
     * For unix systems we can use one command to get all java processes and their arguments. The first line will be a
     * header that we can read out to get the required index for the arguments.
     */
    private List<String> findCandidateProcessUnix() throws IOException {
        Process p;
        if (SystemUtils.IS_OS_MAC_OSX) {
            p = Runtime.getRuntime().exec("ps ax");
        }
        else {
            p = Runtime.getRuntime().exec("ps -fC java");
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<String> res = new ArrayList<>();

        String line;
        while ((line = input.readLine()) != null) {
            if (line.contains("java") && line.contains("--accessToken")) {
                res.add(line);
            }
        }
        input.close();

        return res;
    }

    /**
     * Retrieve launch parameters from a given PID on Windows. Uses PowerShell since WMIC is removed in windows 11.
     */
    private String getLaunchParametersWindows(long pid) {
        String query = "SELECT CommandLine FROM Win32_Process WHERE ProcessID = \"" + pid + "\"";
        String formatter = "| Select-Object -Property CommandLine | Out-String -width 9999";
        String cmd = "\"Get-WmiObject -Query \\\"" + query + "\\\" " + formatter + "\"";
        try {
            Process process = new ProcessBuilder(
                    "Powershell.exe", "-Command", cmd
            ).redirectErrorStream(true).start();

            String res = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().filter(line -> line.contains("accessToken")).collect(Collectors.joining());

            return res;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
