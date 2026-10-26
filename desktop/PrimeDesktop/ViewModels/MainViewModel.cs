using System;
using System.Collections.ObjectModel;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using Microsoft.Win32;
using PrimeDesktop.Models;
using PrimeDesktop.Network;
using PrimeDesktop.Services;

namespace PrimeDesktop.ViewModels;

public class MainViewModel : ViewModelBase
{
    private readonly StorageService _storageService = new();
    private readonly BluetoothRfcommService _bluetoothService = new();
    private readonly CompanionBridgeService _bridgeService = new();

    private ObservableCollection<ChatItem> _allChats = new();
    private ObservableCollection<ChatItem> _filteredChats = new();
    private ChatItem? _selectedChat;
    private string _searchQuery = string.Empty;
    private string _inputMessageText = string.Empty;
    private ChatMessage? _replyMessage;
    private bool _isSettingsOpen;
    private bool _isMediaDrawerOpen;
    private bool _isScanning;
    private string _statusMessage = "Ready";
    private AppSettings _settings = new();

    public ObservableCollection<ChatItem> FilteredChats
    {
        get => _filteredChats;
        set => SetProperty(ref _filteredChats, value);
    }

    public ChatItem? SelectedChat
    {
        get => _selectedChat;
        set
        {
            if (SetProperty(ref _selectedChat, value))
            {
                if (_selectedChat != null)
                {
                    _selectedChat.UnreadCount = 0;
                    _ = _storageService.SaveChatsAsync(_allChats);
                }
                OnPropertyChanged(nameof(HasSelectedChat));
                CommandManager.InvalidateRequerySuggested();
            }
        }
    }

    public bool HasSelectedChat => SelectedChat != null;

    public string SearchQuery
    {
        get => _searchQuery;
        set
        {
            if (SetProperty(ref _searchQuery, value))
            {
                ApplySearchFilter();
            }
        }
    }

    public string InputMessageText
    {
        get => _inputMessageText;
        set
        {
            if (SetProperty(ref _inputMessageText, value))
            {
                NotifyTyping();
            }
        }
    }

    public ChatMessage? ReplyMessage
    {
        get => _replyMessage;
        set
        {
            if (SetProperty(ref _replyMessage, value))
            {
                OnPropertyChanged(nameof(HasReply));
            }
        }
    }

    public bool HasReply => ReplyMessage != null;

    public bool IsSettingsOpen
    {
        get => _isSettingsOpen;
        set => SetProperty(ref _isSettingsOpen, value);
    }

    public bool IsMediaDrawerOpen
    {
        get => _isMediaDrawerOpen;
        set => SetProperty(ref _isMediaDrawerOpen, value);
    }

    public bool IsScanning
    {
        get => _isScanning;
        set => SetProperty(ref _isScanning, value);
    }

    public string StatusMessage
    {
        get => _statusMessage;
        set => SetProperty(ref _statusMessage, value);
    }

    public AppSettings Settings
    {
        get => _settings;
        set => SetProperty(ref _settings, value);
    }

    // Commands
    public ICommand SendMessageCommand { get; }
    public ICommand AttachFileCommand { get; }
    public ICommand AttachPhotoCommand { get; }
    public ICommand CancelReplyCommand { get; }
    public ICommand ReplyToMessageCommand { get; }
    public ICommand ScanDevicesCommand { get; }
    public ICommand ConnectChatCommand { get; }
    public ICommand DisconnectChatCommand { get; }
    public ICommand ToggleSettingsCommand { get; }
    public ICommand ToggleMediaDrawerCommand { get; }
    public ICommand ConnectCompanionWifiCommand { get; }

    public MainViewModel()
    {
        SendMessageCommand = new RelayCommand(async () => await SendTextMessageAsync(), () => HasSelectedChat && !string.IsNullOrWhiteSpace(InputMessageText));
        AttachFileCommand = new RelayCommand(async () => await SendAttachmentAsync(PacketType.File), () => HasSelectedChat);
        AttachPhotoCommand = new RelayCommand(async () => await SendAttachmentAsync(PacketType.Photo), () => HasSelectedChat);
        CancelReplyCommand = new RelayCommand(() => ReplyMessage = null);
        ReplyToMessageCommand = new RelayCommand(param => { if (param is ChatMessage msg) ReplyMessage = msg; });
        ScanDevicesCommand = new RelayCommand(async () => await ScanDevicesAsync());
        ConnectChatCommand = new RelayCommand(async () => await ConnectSelectedChatAsync(), () => HasSelectedChat);
        DisconnectChatCommand = new RelayCommand(async () => await DisconnectSelectedChatAsync(), () => HasSelectedChat);
        ToggleSettingsCommand = new RelayCommand(() => IsSettingsOpen = !IsSettingsOpen);
        ToggleMediaDrawerCommand = new RelayCommand(() => IsMediaDrawerOpen = !IsMediaDrawerOpen);
        ConnectCompanionWifiCommand = new RelayCommand(async () => await ConnectWifiCompanionAsync());

        // Network wire-up
        _bluetoothService.PacketReceived += OnPacketReceived;
        _bluetoothService.ConnectionStateChanged += OnConnectionStateChanged;
        _bluetoothService.ErrorOccurred += OnNetworkError;

        _bridgeService.PacketReceived += OnPacketReceived;
        _bridgeService.ConnectionStateChanged += OnConnectionStateChanged;
        _bridgeService.ErrorOccurred += OnNetworkError;

        _ = InitializeAsync();
    }

    private async Task InitializeAsync()
    {
        Settings = await _storageService.LoadSettingsAsync();
        ThemeManager.ApplyAccentColor(Settings.AccentColorHex);

        var chats = await _storageService.LoadChatsAsync();
        foreach (var c in chats) _allChats.Add(c);

        if (_allChats.Count == 0)
        {
            // Seed a welcome demo chat so the UI looks active right away
            var welcomeChat = new ChatItem
            {
                DisplayName = "Prime Messenger Info",
                Username = "prime_team",
                DeviceAddress = "00:00:00:00:00:00",
                LastMessage = "Добро пожаловать в Prime Desktop!",
                LastMessageTime = DateTime.Now.ToString("HH:mm"),
                IsOnline = true
            };
            welcomeChat.Messages.Add(new ChatMessage
            {
                Sender = "Prime Messenger Info",
                Text = "Добро пожаловать в компьютерную версию Prime Messenger! Приложение поддерживает связь по прямому Bluetooth RFCOMM и по локальному Wi-Fi мосту с телефоном.",
                IsFromMe = false,
                FormattedTime = DateTime.Now.ToString("HH:mm")
            });
            _allChats.Add(welcomeChat);
        }

        ApplySearchFilter();
        SelectedChat = _filteredChats.FirstOrDefault();

        // Start background servers
        _bluetoothService.MyDisplayName = Settings.MyDisplayName;
        _bluetoothService.MyUsername = Settings.MyUsername;
        await _bluetoothService.StartServerAsync();

        _bridgeService.MyDisplayName = Settings.MyDisplayName;
        _bridgeService.MyUsername = Settings.MyUsername;
        await _bridgeService.StartServerAsync();
    }

    private void ApplySearchFilter()
    {
        if (string.IsNullOrWhiteSpace(SearchQuery))
        {
            FilteredChats = new ObservableCollection<ChatItem>(_allChats);
        }
        else
        {
            var query = SearchQuery.Trim().ToLowerInvariant();
            var matches = _allChats.Where(c =>
                c.DisplayName.ToLowerInvariant().Contains(query) ||
                c.Username.ToLowerInvariant().Contains(query) ||
                c.LastMessage.ToLowerInvariant().Contains(query));
            FilteredChats = new ObservableCollection<ChatItem>(matches);
        }
    }

    public async Task SendTextMessageAsync()
    {
        if (SelectedChat == null || string.IsNullOrWhiteSpace(InputMessageText)) return;

        string text = InputMessageText.Trim();
        InputMessageText = string.Empty;

        string msgId = Guid.NewGuid().ToString();
        var reply = ReplyMessage;
        ReplyMessage = null;

        var message = new ChatMessage
        {
            Id = msgId,
            Sender = Settings.MyDisplayName,
            Text = text,
            IsFromMe = true,
            FormattedTime = DateTime.Now.ToString("HH:mm"),
            ReplyToId = reply?.Id,
            ReplyToSender = reply?.Sender,
            ReplyToText = reply?.Text
        };

        SelectedChat.Messages.Add(message);
        SelectedChat.LastMessage = text;
        SelectedChat.LastMessageTime = message.FormattedTime;

        byte[] payload = PrimePacket.BuildTextPayload(
            msgId,
            text,
            reply?.Id,
            reply?.Sender,
            reply?.Text);

        try
        {
            if (SelectedChat.IsBluetoothDevice)
            {
                await _bluetoothService.SendPacketAsync(SelectedChat.DeviceAddress, PacketType.Text, payload);
            }
            else
            {
                await _bridgeService.SendPacketAsync(SelectedChat.DeviceAddress, PacketType.Text, payload);
            }
            message.IsDelivered = true;
        }
        catch (Exception ex)
        {
            StatusMessage = $"Sending failed: {ex.Message}";
        }

        await _storageService.SaveChatsAsync(_allChats);
    }

    public async Task SendAttachmentAsync(PacketType type)
    {
        if (SelectedChat == null) return;

        var dialog = new OpenFileDialog();
        if (type == PacketType.Photo)
        {
            dialog.Title = "Выберите изображение";
            dialog.Filter = "Изображения|*.png;*.jpg;*.jpeg;*.webp;*.bmp|Все файлы|*.*";
        }
        else
        {
            dialog.Title = "Выберите файл для отправки";
            dialog.Filter = "Все файлы|*.*";
        }

        if (dialog.ShowDialog() == true)
        {
            string filePath = dialog.FileName;
            string fileName = Path.GetFileName(filePath);
            byte[] fileBytes = await File.ReadAllBytesAsync(filePath);

            var message = new ChatMessage
            {
                Sender = Settings.MyDisplayName,
                Text = fileName,
                FilePath = filePath,
                FileName = fileName,
                FileSizeBytes = fileBytes.Length,
                Type = type,
                IsFromMe = true,
                FormattedTime = DateTime.Now.ToString("HH:mm")
            };

            SelectedChat.Messages.Add(message);
            SelectedChat.LastMessage = type == PacketType.Photo ? "📷 Фотография" : $"📎 {fileName}";
            SelectedChat.LastMessageTime = message.FormattedTime;

            try
            {
                if (SelectedChat.IsBluetoothDevice)
                {
                    await _bluetoothService.SendPacketAsync(SelectedChat.DeviceAddress, type, fileBytes);
                }
                else
                {
                    await _bridgeService.SendPacketAsync(SelectedChat.DeviceAddress, type, fileBytes);
                }
                message.IsDelivered = true;
            }
            catch (Exception ex)
            {
                StatusMessage = $"Attachment send failed: {ex.Message}";
            }

            await _storageService.SaveChatsAsync(_allChats);
        }
    }

    private void NotifyTyping()
    {
        if (SelectedChat == null) return;
        _ = Task.Run(async () =>
        {
            try
            {
                byte[] empty = Array.Empty<byte>();
                if (SelectedChat.IsBluetoothDevice)
                    await _bluetoothService.SendPacketAsync(SelectedChat.DeviceAddress, PacketType.Typing, empty);
                else
                    await _bridgeService.SendPacketAsync(SelectedChat.DeviceAddress, PacketType.Typing, empty);
            }
            catch { }
        });
    }

    public async Task ScanDevicesAsync()
    {
        IsScanning = true;
        StatusMessage = "Scanning for paired Bluetooth devices...";
        try
        {
            var found = await _bluetoothService.DiscoverDevicesAsync();
            foreach (var dev in found)
            {
                var existing = _allChats.FirstOrDefault(c => c.DeviceAddress.Equals(dev.DeviceAddress, StringComparison.OrdinalIgnoreCase));
                if (existing == null)
                {
                    _allChats.Add(dev);
                }
            }
            ApplySearchFilter();
            StatusMessage = $"Scan completed. Found {found.Count} devices.";
        }
        catch (Exception ex)
        {
            StatusMessage = $"Scan error: {ex.Message}";
        }
        finally
        {
            IsScanning = false;
        }
    }

    public async Task ConnectSelectedChatAsync()
    {
        if (SelectedChat == null) return;
        StatusMessage = $"Connecting to {SelectedChat.DisplayName}...";
        if (SelectedChat.IsBluetoothDevice)
        {
            await _bluetoothService.ConnectAsync(SelectedChat.DeviceAddress);
        }
        else
        {
            await _bridgeService.ConnectAsync(SelectedChat.DeviceAddress);
        }
    }

    public async Task DisconnectSelectedChatAsync()
    {
        if (SelectedChat == null) return;
        if (SelectedChat.IsBluetoothDevice)
        {
            await _bluetoothService.DisconnectAsync(SelectedChat.DeviceAddress);
        }
        else
        {
            await _bridgeService.DisconnectAsync(SelectedChat.DeviceAddress);
        }
    }

    public async Task ConnectWifiCompanionAsync()
    {
        string inputIp = Microsoft.VisualBasic.Interaction.InputBox("Введите IP-адрес телефона в локальной сети Wi-Fi (например 192.168.1.50):", "Связка с телефоном по Wi-Fi", "192.168.1.");
        if (!string.IsNullOrWhiteSpace(inputIp))
        {
            var existing = _allChats.FirstOrDefault(c => c.DeviceAddress == inputIp);
            if (existing == null)
            {
                existing = new ChatItem
                {
                    DisplayName = "Prime Phone",
                    Username = "phone_link",
                    DeviceAddress = inputIp,
                    IsBluetoothDevice = false,
                    LastMessage = "Связка по Wi-Fi"
                };
                _allChats.Add(existing);
                ApplySearchFilter();
            }
            SelectedChat = existing;
            await _bridgeService.ConnectAsync(inputIp);
        }
    }

    private void OnConnectionStateChanged(string address, DeviceConnectionState state, string name)
    {
        Application.Current.Dispatcher.Invoke(() =>
        {
            var chat = _allChats.FirstOrDefault(c => c.DeviceAddress.Equals(address, StringComparison.OrdinalIgnoreCase));
            if (chat == null)
            {
                chat = new ChatItem
                {
                    DeviceAddress = address,
                    DisplayName = name,
                    IsBluetoothDevice = true
                };
                _allChats.Add(chat);
                ApplySearchFilter();
            }

            chat.ConnectionState = state;
            chat.IsOnline = (state == DeviceConnectionState.Connected);
            if (!string.IsNullOrEmpty(name)) chat.DisplayName = name;

            StatusMessage = $"{chat.DisplayName}: {state}";
        });
    }

    private void OnPacketReceived(string fromAddress, PacketType type, byte[] payload)
    {
        Application.Current.Dispatcher.Invoke(() =>
        {
            var chat = _allChats.FirstOrDefault(c => c.DeviceAddress.Equals(fromAddress, StringComparison.OrdinalIgnoreCase));
            if (chat == null)
            {
                chat = new ChatItem
                {
                    DeviceAddress = fromAddress,
                    DisplayName = fromAddress,
                    IsBluetoothDevice = true,
                    IsOnline = true
                };
                _allChats.Add(chat);
                ApplySearchFilter();
            }

            switch (type)
            {
                case PacketType.Text:
                    var (msgId, text, rId, rSender, rText) = PrimePacket.ParseTextPayload(payload);
                    var msg = new ChatMessage
                    {
                        Id = msgId,
                        Sender = chat.DisplayName,
                        Text = text,
                        IsFromMe = false,
                        FormattedTime = DateTime.Now.ToString("HH:mm"),
                        ReplyToId = rId,
                        ReplyToSender = rSender,
                        ReplyToText = rText
                    };
                    chat.Messages.Add(msg);
                    chat.LastMessage = text;
                    chat.LastMessageTime = msg.FormattedTime;
                    if (SelectedChat != chat) chat.UnreadCount++;
                    break;

                case PacketType.Photo:
                case PacketType.File:
                    string ext = (type == PacketType.Photo) ? ".jpg" : ".bin";
                    string savedName = $"media_{DateTime.Now:yyyyMMdd_HHmmss}_{Guid.NewGuid().ToString().Substring(0, 6)}{ext}";
                    string savedPath = Path.Combine(StorageService.MediaDirectory, savedName);
                    File.WriteAllBytes(savedPath, payload);

                    var fileMsg = new ChatMessage
                    {
                        Sender = chat.DisplayName,
                        Text = savedName,
                        FilePath = savedPath,
                        FileName = savedName,
                        FileSizeBytes = payload.Length,
                        Type = type,
                        IsFromMe = false,
                        FormattedTime = DateTime.Now.ToString("HH:mm")
                    };
                    chat.Messages.Add(fileMsg);
                    chat.LastMessage = (type == PacketType.Photo) ? "📷 Фотография" : "📎 Файл";
                    chat.LastMessageTime = fileMsg.FormattedTime;
                    if (SelectedChat != chat) chat.UnreadCount++;
                    break;

                case PacketType.Typing:
                    chat.IsTyping = true;
                    Task.Delay(3000).ContinueWith(_ => Application.Current.Dispatcher.Invoke(() => chat.IsTyping = false));
                    break;
            }

            _ = _storageService.SaveChatsAsync(_allChats);
        });
    }

    private void OnNetworkError(string source, string message)
    {
        Application.Current.Dispatcher.Invoke(() =>
        {
            StatusMessage = $"{source}: {message}";
        });
    }
}
