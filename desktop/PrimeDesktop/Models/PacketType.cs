namespace PrimeDesktop.Models;

/// <summary>
/// Packet types matching Prime Messenger Android protocol
/// </summary>
public enum PacketType : byte
{
    Text = 0x01,
    Photo = 0x02,
    Typing = 0x04,
    Edit = 0x05,
    ChatDeleted = 0x08,
    ReadReceipt = 0x0A,
    DeleteMessage = 0x0B,
    Presence = 0x0C,
    File = 0x0E,
    Voice = 0x0F,
    KeepAlive = 0xFE
}
