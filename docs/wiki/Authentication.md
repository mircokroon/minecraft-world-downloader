# Authentication

To download from an **online-mode** server (the default for most servers) the downloader must
authenticate with your Minecraft account, exactly like the real client does. **Offline-mode** servers
need only a username.

## With the jar / GUI
- **GUI:** use the **Authentication** tab to sign in with Microsoft, or enter a username + access token.
- **Automatic:** if the official launcher/game is running, the downloader can read the session from it.
- **Manual (CLI):** pass `--username` and `--token` (a Minecraft access token). See
  [Command-Line Options](Command-Line-Options).

```bash
java -jar world-downloader.jar --no-gui -s server.example -u YourName -t <access-token>
```

## With the Docker web console
Open the **Minecraft account** panel and pick a method:

### 1. Microsoft (recommended)
Uses the OAuth **device-code** flow — ideal for headless/remote use, no browser redirect needed.
1. Click **Sign in with Microsoft**.
2. A code appears and `https://www.microsoft.com/link` opens.
3. Enter the code and complete the Microsoft sign-in.
4. The console finishes the Xbox Live → XSTS → Minecraft token exchange and stores your session.

The Minecraft token is **refreshed automatically** using the stored refresh token, so you normally sign
in only once. The session persists in `./data/auth.json`.

### 2. Access token
Paste an existing Minecraft access token (e.g. from `launcher_accounts.json` or another auth tool). The
console looks up your username/UUID from the profile API automatically. Tokens expire after a while; if
downloads start failing with an auth error, sign in again.

### 3. Offline username
For **offline-mode / cracked** servers only. Enter a username; no token is used.

## Notes & troubleshooting
- The account must **own Minecraft: Java Edition** and have an **Xbox profile**. Common errors:
  - *"does not own Minecraft"* — the account has no Java Edition license.
  - *"no Xbox profile / region / child account"* — finish Xbox setup, or the account is restricted.
- **Custom Azure app:** the Microsoft flow defaults to the public Minecraft launcher client id. To use
  your own Azure AD app, set `MS_CLIENT_ID` (and, if needed, `MS_DEVICE_CODE_URL`, `MS_TOKEN_URL`,
  `MS_SCOPE`).
- The downloader only uses your token to join the server on your behalf (the same request the vanilla
  client makes). Your token is stored locally in the mounted `./data` volume and never sent anywhere
  else.
