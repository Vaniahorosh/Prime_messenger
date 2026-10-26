using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using PrimeDesktop.Models;

namespace PrimeDesktop.Network;

/// <summary>
/// Companion Bridge for local Wi-Fi / LAN connection between Windows PC and Phone.
/// Enables seamless sync when phone and PC are on the same Wi-Fi network.
/// </summary>
public class CompanionBridgeService : IPrimeNetworkService
{
    public event Action<string, DeviceConnectionState, string>? ConnectionStateChanged;
    public event Action<string, PacketType, byte[]>? PacketReceived;
    public event Action<string, string>? ErrorOccurred;

    private TcpListener? _listener;
    private TcpClient? _client;
    private NetworkStream? _stream;
    private CancellationTokenSource? _readCts;
    private readonly int _port;

    public string MyDisplayName { get; set; } = Environment.MachineName;
    public string MyUsername { get; set; } = "prime_desktop";

    public CompanionBridgeService(int port = 8989)
    {
        _port = port;
    }

    public Task StartServerAsync()
    {
        try
        {
            _listener = new TcpListener(IPAddress.Any, _port);
            _listener.Start();

            _ = Task.Run(async () =>
            {
                while (_listener != null)
                {
                    try
                    {
                        var tcpClient = await _listener.AcceptTcpClientAsync();
                        string remoteIp = ((IPEndPoint)tcpClient.Client.RemoteEndPoint!).Address.ToString();

                        _client?.Dispose();
                        _client = tcpClient;
                        _stream = tcpClient.GetStream();

                        ConnectionStateChanged?.Invoke(remoteIp, DeviceConnectionState.Connected, "Prime Phone (Wi-Fi)");

                        // Send handshake
                        byte[] handshake = PrimePacket.BuildHandshakePayload(MyDisplayName, MyUsername, isAck: false);
                        await SendPacketAsync(remoteIp, PacketType.Text, handshake);

                        StartReading(remoteIp, _stream);
                    }
                    catch { break; }
                }
            });
        }
        catch (Exception ex)
        {
            ErrorOccurred?.Invoke("Wi-Fi Bridge", $"Failed to start bridge server: {ex.Message}");
        }

        return Task.CompletedTask;
    }

    public Task StopServerAsync()
    {
        try
        {
            _listener?.Stop();
            _listener = null;
            _readCts?.Cancel();
            _stream?.Dispose();
            _client?.Dispose();
        }
        catch { }
        return Task.CompletedTask;
    }

    public async Task<bool> ConnectAsync(string phoneIp)
    {
        try
        {
            ConnectionStateChanged?.Invoke(phoneIp, DeviceConnectionState.Connecting, "Prime Phone");

            _client?.Dispose();
            _client = new TcpClient();
            await _client.ConnectAsync(phoneIp, _port);
            _stream = _client.GetStream();

            ConnectionStateChanged?.Invoke(phoneIp, DeviceConnectionState.Connected, "Prime Phone (Wi-Fi)");

            byte[] handshake = PrimePacket.BuildHandshakePayload(MyDisplayName, MyUsername, isAck: false);
            await SendPacketAsync(phoneIp, PacketType.Text, handshake);

            StartReading(phoneIp, _stream);
            return true;
        }
        catch (Exception ex)
        {
            ConnectionStateChanged?.Invoke(phoneIp, DeviceConnectionState.Disconnected, phoneIp);
            ErrorOccurred?.Invoke(phoneIp, $"Bridge connection failed: {ex.Message}");
            return false;
        }
    }

    public Task DisconnectAsync(string targetAddress)
    {
        try
        {
            _readCts?.Cancel();
            _stream?.Dispose();
            _client?.Dispose();
            _stream = null;
            _client = null;
            ConnectionStateChanged?.Invoke(targetAddress, DeviceConnectionState.Disconnected, targetAddress);
        }
        catch { }
        return Task.CompletedTask;
    }

    public async Task SendPacketAsync(string targetAddress, PacketType type, byte[] payload)
    {
        if (_stream == null) throw new InvalidOperationException("Not connected to phone companion.");

        await Task.Run(() =>
        {
            lock (_stream)
            {
                PrimePacket.WriteToStream(_stream, type, payload);
            }
        });
    }

    public Task<List<ChatItem>> DiscoverDevicesAsync()
    {
        return Task.FromResult(new List<ChatItem>());
    }

    private void StartReading(string address, NetworkStream stream)
    {
        _readCts?.Cancel();
        _readCts = new CancellationTokenSource();

        Task.Run(() =>
        {
            try
            {
                while (!_readCts.IsCancellationRequested)
                {
                    var packet = PrimePacket.ReadFromStream(stream);
                    if (packet == null) break;

                    if (packet.Type == PacketType.Text)
                    {
                        var (name, login, isAck) = PrimePacket.ParseHandshakePayload(packet.Payload);
                        if (name != null)
                        {
                            if (!isAck)
                            {
                                byte[] ack = PrimePacket.BuildHandshakePayload(MyDisplayName, MyUsername, isAck: true);
                                _ = SendPacketAsync(address, PacketType.Text, ack);
                            }
                            ConnectionStateChanged?.Invoke(address, DeviceConnectionState.Connected, name);
                            continue;
                        }
                    }

                    PacketReceived?.Invoke(address, packet.Type, packet.Payload);
                }
            }
            catch { }
            finally
            {
                DisconnectAsync(address).Wait(300);
            }
        }, _readCts.Token);
    }
}
