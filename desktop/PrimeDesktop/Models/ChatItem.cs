using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace PrimeDesktop.Models;

public enum DeviceConnectionState
{
    Disconnected,
    Connecting,
    Connected
}

public class ChatItem : INotifyPropertyChanged
{
    private string _displayName = string.Empty;
    private string _username = string.Empty;
    private string _lastMessage = string.Empty;
    private string _lastMessageTime = string.Empty;
    private int _unreadCount;
    private bool _isOnline;
    private bool _isTyping;
    private DeviceConnectionState _connectionState = DeviceConnectionState.Disconnected;

    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string DeviceAddress { get; set; } = string.Empty; // Bluetooth MAC or IP
    public bool IsBluetoothDevice { get; set; } = true;
    public string? AvatarPath { get; set; }

    public ObservableCollection<ChatMessage> Messages { get; } = new();

    public string DisplayName
    {
        get => _displayName;
        set => SetField(ref _displayName, value);
    }

    public string Username
    {
        get => _username;
        set => SetField(ref _username, value);
    }

    public string LastMessage
    {
        get => _lastMessage;
        set => SetField(ref _lastMessage, value);
    }

    public string LastMessageTime
    {
        get => _lastMessageTime;
        set => SetField(ref _lastMessageTime, value);
    }

    public int UnreadCount
    {
        get => _unreadCount;
        set => SetField(ref _unreadCount, value);
    }

    public bool IsOnline
    {
        get => _isOnline;
        set => SetField(ref _isOnline, value);
    }

    public bool IsTyping
    {
        get => _isTyping;
        set => SetField(ref _isTyping, value);
    }

    public DeviceConnectionState ConnectionState
    {
        get => _connectionState;
        set => SetField(ref _connectionState, value);
    }

    public string Initials
    {
        get
        {
            if (string.IsNullOrWhiteSpace(DisplayName)) return "?";
            var parts = DisplayName.Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
            if (parts.Length >= 2)
                return $"{char.ToUpper(parts[0][0])}{char.ToUpper(parts[1][0])}";
            return char.ToUpper(parts[0][0]).ToString();
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string? propertyName = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));

    protected bool SetField<T>(ref T field, T value, [CallerMemberName] string? propertyName = null)
    {
        if (Equals(field, value)) return false;
        field = value;
        OnPropertyChanged(propertyName);
        if (propertyName == nameof(DisplayName)) OnPropertyChanged(nameof(Initials));
        return true;
    }
}
