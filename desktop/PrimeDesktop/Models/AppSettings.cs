namespace PrimeDesktop.Models;

public class AppSettings
{
    public string MyUsername { get; set; } = "prime_user";
    public string MyDisplayName { get; set; } = Environment.UserName;
    public string AccentColorHex { get; set; } = "#4D9FFF";
    public bool DarkTheme { get; set; } = true;
    public bool EnableAcrylic { get; set; } = true;
    public bool PlayNotificationSounds { get; set; } = true;
    public bool MinimizeToTray { get; set; } = true;
    public int CompanionPort { get; set; } = 8989;
    public bool AutoConnectBluetooth { get; set; } = true;
}
