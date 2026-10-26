using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Rfcomm;
using Windows.Devices.Enumeration;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;
using PrimeDesktop.Models;

namespace PrimeDesktop.Network;

/// <summary>
/// Native Windows Bluetooth Classic RFCOMM service.
/// Interoperates 100% with Prime Messenger on Android.
/// </summary>
public class BluetoothRfcommService : IPrimeNetworkService
{
    public static readonly Guid PrimeServiceUuid = Guid.Parse("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public event Action<string, DeviceConnectionState, string>? ConnectionStateChanged;
    public event Action<string, PacketType, byte[]>? PacketReceived;
    public event Action<string, string>? ErrorOccurred;

    private StreamSocketListener? _serverListener;
    private RfcommServiceProvider? _rfcommProvider;

    private readonly ConcurrentDictionary<string, StreamSocket> _activeSockets = new();
    private readonly ConcurrentDictionary<string, Stream> _activeStreams = new();
    private readonly ConcurrentDictionary<string, CancellationTokenSource> _readCancellation = new();

    public string MyDisplayName { get; set; } = Environment.MachineName;
    public string MyUsername { get; set; } = "prime_desktop";

    public async Task StartServerAsync()
    {
        try
        {
            _rfcommProvider = await RfcommServiceProvider.CreateAsync(RfcommServiceId.FromUuid(PrimeServiceUuid));
            _serverListener = new StreamSocketListener();
            _serverListener.ConnectionReceived += OnConnectionReceived;

            await _serverListener.BindServiceNameAsync(
                _rfcommProvider.ServiceId.AsString(),
                SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);

            // Populate SDP Service Record
            using (var writer = new DataWriter())
            {
                writer.WriteByte(0x0C); // SDP String type
                writer.WriteByte((byte)"PrimeChat".Length);
                writer.WriteString("PrimeChat");
                _rfcommProvider.SdpRawAttributes.Add(0x0100, writer.DetachBuffer());
            }

            _rfcommProvider.StartAdvertising(_serverListener, true);
        }
        catch (Exception ex)
        {
            ErrorOccurred?.Invoke("LocalServer", $"Failed to start Bluetooth RFCOMM server: {ex.Message}");
        }
    }

    public Task StopServerAsync()
    {
        try
        {
            _rfcommProvider?.StopAdvertising();
            _rfcommProvider = null;

            _serverListener?.Dispose();
            _serverListener = null;

            foreach (var kvp in _activeSockets)
            {
                DisconnectAsync(kvp.Key).Wait(500);
            }
        }
        catch { }
        return Task.CompletedTask;
    }

    private void OnConnectionReceived(StreamSocketListener sender, StreamSocketListenerConnectionReceivedEventArgs args)
    {
        var socket = args.Socket;
        string remoteAddress = socket.Information.RemoteAddress.CanonicalName;
        string remoteName = socket.Information.RemoteHostName?.DisplayName ?? remoteAddress;

        _activeSockets[remoteAddress] = socket;
        var stream = socket.InputStream.AsStreamForRead();
        var outStream = socket.OutputStream.AsStreamForWrite();
        _activeStreams[remoteAddress] = new BidirectionalStream(stream, outStream);

        ConnectionStateChanged?.Invoke(remoteAddress, DeviceConnectionState.Connected, remoteName);

        // Send Initial Handshake
        _ = Task.Run(async () =>
        {
            try
            {
                byte[] handshake = PrimePacket.BuildHandshakePayload(MyDisplayName, MyUsername, isAck: false);
                await SendPacketAsync(remoteAddress, PacketType.Text, handshake);
            }
            catch { }
        });

        StartReading(remoteAddress, stream);
    }

    public async Task<bool> ConnectAsync(string deviceAddressOrId)
    {
        try
        {
            ConnectionStateChanged?.Invoke(deviceAddressOrId, DeviceConnectionState.Connecting, deviceAddressOrId);

            BluetoothDevice? device = null;
            if (ulong.TryParse(deviceAddressOrId.Replace(":", "").Replace("-", ""), System.Globalization.NumberStyles.HexNumber, null, out ulong mac))
            {
                device = await BluetoothDevice.FromBluetoothAddressAsync(mac);
            }
            else
            {
                device = await BluetoothDevice.FromIdAsync(deviceAddressOrId);
            }

            if (device == null)
            {
                ConnectionStateChanged?.Invoke(deviceAddressOrId, DeviceConnectionState.Disconnected, deviceAddressOrId);
                ErrorOccurred?.Invoke(deviceAddressOrId, "Bluetooth device not found.");
                return false;
            }

            var rfcommServices = await device.GetRfcommServicesForIdAsync(RfcommServiceId.FromUuid(PrimeServiceUuid));
            if (rfcommServices.Services.Count == 0)
            {
                ConnectionStateChanged?.Invoke(deviceAddressOrId, DeviceConnectionState.Disconnected, device.Name);
                ErrorOccurred?.Invoke(deviceAddressOrId, "Prime Messenger service not available on device.");
                return false;
            }

            var service = rfcommServices.Services[0];
            var socket = new StreamSocket();
            await socket.ConnectAsync(service.ConnectionHostName, service.ConnectionServiceName, SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);

            string address = device.DeviceId;
            _activeSockets[address] = socket;
            var inStream = socket.InputStream.AsStreamForRead();
            var outStream = socket.OutputStream.AsStreamForWrite();
            _activeStreams[address] = new BidirectionalStream(inStream, outStream);

            ConnectionStateChanged?.Invoke(address, DeviceConnectionState.Connected, device.Name);

            // Send handshake
            byte[] handshake = PrimePacket.BuildHandshakePayload(MyDisplayName, MyUsername, isAck: false);
            await SendPacketAsync(address, PacketType.Text, handshake);

            StartReading(address, inStream);
            return true;
        }
        catch (Exception ex)
        {
            ConnectionStateChanged?.Invoke(deviceAddressOrId, DeviceConnectionState.Disconnected, deviceAddressOrId);
            ErrorOccurred?.Invoke(deviceAddressOrId, $"Bluetooth connection error: {ex.Message}");
            return false;
        }
    }

    public Task DisconnectAsync(string address)
    {
        if (_readCancellation.TryRemove(address, out var cts))
        {
            cts.Cancel();
            cts.Dispose();
        }

        if (_activeStreams.TryRemove(address, out var stream))
        {
            stream.Dispose();
        }

        if (_activeSockets.TryRemove(address, out var socket))
        {
            socket.Dispose();
        }

        ConnectionStateChanged?.Invoke(address, DeviceConnectionState.Disconnected, address);
        return Task.CompletedTask;
    }

    public async Task SendPacketAsync(string targetAddress, PacketType type, byte[] payload)
    {
        if (!_activeStreams.TryGetValue(targetAddress, out var stream))
        {
            throw new InvalidOperationException($"Not connected to device: {targetAddress}");
        }

        await Task.Run(() =>
        {
            lock (stream)
            {
                PrimePacket.WriteToStream(stream, type, payload);
            }
        });
    }

    public async Task<List<ChatItem>> DiscoverDevicesAsync()
    {
        var result = new List<ChatItem>();
        try
        {
            string selector = BluetoothDevice.GetDeviceSelectorFromPairingState(true);
            var devices = await DeviceInformation.FindAllAsync(selector);

            foreach (var dev in devices)
            {
                result.Add(new ChatItem
                {
                    Id = dev.Id,
                    DeviceAddress = dev.Id,
                    DisplayName = dev.Name,
                    IsBluetoothDevice = true,
                    ConnectionState = DeviceConnectionState.Disconnected
                });
            }
        }
        catch (Exception ex)
        {
            ErrorOccurred?.Invoke("Discovery", $"Device scan failed: {ex.Message}");
        }
        return result;
    }

    private void StartReading(string address, Stream inStream)
    {
        var cts = new CancellationTokenSource();
        _readCancellation[address] = cts;

        Task.Run(() =>
        {
            try
            {
                while (!cts.IsCancellationRequested)
                {
                    var packet = PrimePacket.ReadFromStream(inStream);
                    if (packet == null) break;

                    if (packet.Type == PacketType.Text)
                    {
                        var (name, login, isAck) = PrimePacket.ParseHandshakePayload(packet.Payload);
                        if (name != null)
                        {
                            if (!isAck)
                            {
                                // Reply with ACK
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
        }, cts.Token);
    }

    /// <summary>
    /// Helper stream for combined reading and writing.
    /// </summary>
    private class BidirectionalStream : Stream
    {
        private readonly Stream _inStream;
        private readonly Stream _outStream;

        public BidirectionalStream(Stream inStream, Stream outStream)
        {
            _inStream = inStream;
            _outStream = outStream;
        }

        public override bool CanRead => _inStream.CanRead;
        public override bool CanSeek => false;
        public override bool CanWrite => _outStream.CanWrite;
        public override long Length => throw new NotSupportedException();
        public override long Position { get => throw new NotSupportedException(); set => throw new NotSupportedException(); }

        public override void Flush() => _outStream.Flush();
        public override int Read(byte[] buffer, int offset, int count) => _inStream.Read(buffer, offset, count);
        public override void Write(byte[] buffer, int offset, int count) => _outStream.Write(buffer, offset, count);
        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();
        public override void SetLength(long value) => throw new NotSupportedException();

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _inStream.Dispose();
                _outStream.Dispose();
            }
            base.Dispose(disposing);
        }
    }
}
