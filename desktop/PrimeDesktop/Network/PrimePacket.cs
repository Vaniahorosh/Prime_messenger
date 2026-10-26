using System;
using System.Buffers.Binary;
using System.IO;
using System.Text;
using PrimeDesktop.Models;

namespace PrimeDesktop.Network;

public class PrimePacket
{
    public PacketType Type { get; set; }
    public byte[] Payload { get; set; } = Array.Empty<byte>();

    public PrimePacket(PacketType type, byte[] payload)
    {
        Type = type;
        Payload = payload ?? Array.Empty<byte>();
    }

    /// <summary>
    /// Writes packet into stream matching BluetoothConnectionManager.java:
    /// 1 byte Type, 4 bytes Big-Endian Length, then raw Payload bytes.
    /// </summary>
    public static void WriteToStream(Stream stream, PacketType type, byte[] payload)
    {
        stream.WriteByte((byte)type);
        byte[] lenBytes = new byte[4];
        BinaryPrimitives.WriteInt32BigEndian(lenBytes, payload.Length);
        stream.Write(lenBytes, 0, 4);

        if (payload.Length > 0)
        {
            // Chunked write (16 KB chunks) as in Android app for RFCOMM buffer stability
            int offset = 0;
            int chunkSize = 16384;
            while (offset < payload.Length)
            {
                int toWrite = Math.Min(chunkSize, payload.Length - offset);
                stream.Write(payload, offset, toWrite);
                offset += toWrite;
            }
        }
        stream.Flush();
    }

    /// <summary>
    /// Reads one full packet from stream. Returns null on stream disconnect.
    /// </summary>
    public static PrimePacket? ReadFromStream(Stream stream)
    {
        int typeByte = stream.ReadByte();
        if (typeByte == -1) return null; // End of stream

        byte[] lenBytes = new byte[4];
        int read = 0;
        while (read < 4)
        {
            int r = stream.Read(lenBytes, read, 4 - read);
            if (r <= 0) return null;
            read += r;
        }

        int length = BinaryPrimitives.ReadInt32BigEndian(lenBytes);
        if (length < 0 || length > 100 * 1024 * 1024) // 100 MB max protection
        {
            throw new InvalidDataException($"Invalid packet length: {length}");
        }

        byte[] payload = new byte[length];
        read = 0;
        while (read < length)
        {
            int r = stream.Read(payload, read, length - read);
            if (r <= 0) return null;
            read += r;
        }

        return new PrimePacket((PacketType)typeByte, payload);
    }

    // ==========================================
    // Payload Formatters & Parsers
    // ==========================================

    public static byte[] BuildTextPayload(string msgId, string text, string? replyToId = null, string? replyToSender = null, string? replyToText = null)
    {
        var sb = new StringBuilder();
        sb.Append(msgId).Append(":::").Append(text);
        if (!string.IsNullOrEmpty(replyToId))
        {
            sb.Append(":::REPLY:::")
              .Append(replyToId).Append(":::")
              .Append(replyToSender ?? "").Append(":::")
              .Append(replyToText ?? "");
        }
        return Encoding.UTF8.GetBytes(sb.ToString());
    }

    public static (string msgId, string realText, string? replyToId, string? replyToSender, string? replyToText) ParseTextPayload(byte[] payload)
    {
        string raw = Encoding.UTF8.GetString(payload);
        string msgId = Guid.NewGuid().ToString();
        string body = raw;

        int sep = raw.IndexOf(":::");
        if (sep != -1)
        {
            msgId = raw.Substring(0, sep);
            body = raw.Substring(sep + 3);
        }

        string? rId = null, rSender = null, rText = null;
        int replySep = body.IndexOf(":::REPLY:::");
        if (replySep != -1)
        {
            string replyPayload = body.Substring(replySep + 11);
            body = body.Substring(0, replySep);
            string[] parts = replyPayload.Split(":::");
            if (parts.Length >= 1) rId = parts[0];
            if (parts.Length >= 2) rSender = parts[1];
            if (parts.Length >= 3) rText = parts[2];
        }

        return (msgId, body, rId, rSender, rText);
    }

    public static byte[] BuildHandshakePayload(string displayName, string username, bool isAck = false)
    {
        string prefix = isAck ? "HANDSHAKE_ACK:" : "HANDSHAKE:";
        string content = $"{prefix}name={displayName};login={username}";
        return Encoding.UTF8.GetBytes(content);
    }

    public static (string? displayName, string? username, bool isAck) ParseHandshakePayload(byte[] payload)
    {
        string text = Encoding.UTF8.GetString(payload);
        bool isAck = text.StartsWith("HANDSHAKE_ACK:");
        string prefix = isAck ? "HANDSHAKE_ACK:" : "HANDSHAKE:";
        if (!text.StartsWith(prefix)) return (null, null, false);

        string data = text.Substring(prefix.Length).Trim();
        string? name = null;
        string? login = null;

        if (data.Contains("login=") || data.Contains("name="))
        {
            string[] parts = data.Split(';');
            foreach (var p in parts)
            {
                if (p.StartsWith("login=")) login = p.Substring(6);
                else if (p.StartsWith("name=")) name = p.Substring(5);
            }
        }
        else
        {
            name = data;
        }

        return (name, login, isAck);
    }
}
