using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Threading.Tasks;
using PrimeDesktop.Models;

namespace PrimeDesktop.Services;

public class StorageService
{
    private static readonly string BaseDirectory = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "PrimeDesktop");

    private static readonly string SettingsFile = Path.Combine(BaseDirectory, "settings.json");
    private static readonly string ChatsFile = Path.Combine(BaseDirectory, "chats.json");
    public static readonly string MediaDirectory = Path.Combine(BaseDirectory, "Media");

    public StorageService()
    {
        Directory.CreateDirectory(BaseDirectory);
        Directory.CreateDirectory(MediaDirectory);
    }

    public async Task<AppSettings> LoadSettingsAsync()
    {
        try
        {
            if (File.Exists(SettingsFile))
            {
                string json = await File.ReadAllTextAsync(SettingsFile);
                return JsonSerializer.Deserialize<AppSettings>(json) ?? new AppSettings();
            }
        }
        catch { }
        return new AppSettings();
    }

    public async Task SaveSettingsAsync(AppSettings settings)
    {
        try
        {
            string json = JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true });
            await File.WriteAllTextAsync(SettingsFile, json);
        }
        catch { }
    }

    public async Task<List<ChatItem>> LoadChatsAsync()
    {
        try
        {
            if (File.Exists(ChatsFile))
            {
                string json = await File.ReadAllTextAsync(ChatsFile);
                return JsonSerializer.Deserialize<List<ChatItem>>(json) ?? new List<ChatItem>();
            }
        }
        catch { }
        return new List<ChatItem>();
    }

    public async Task SaveChatsAsync(IEnumerable<ChatItem> chats)
    {
        try
        {
            string json = JsonSerializer.Serialize(chats, new JsonSerializerOptions { WriteIndented = true });
            await File.WriteAllTextAsync(ChatsFile, json);
        }
        catch { }
    }
}
