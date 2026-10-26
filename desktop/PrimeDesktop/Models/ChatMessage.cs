using System;
using System.Text.Json.Serialization;

namespace PrimeDesktop.Models;

public class ChatMessage
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string Sender { get; set; } = string.Empty;
    public string Text { get; set; } = string.Empty;
    public long Timestamp { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    public string FormattedTime { get; set; } = DateTime.Now.ToString("HH:mm");
    public bool IsFromMe { get; set; }
    public bool IsRead { get; set; }
    public bool IsDelivered { get; set; }
    public bool IsEdited { get; set; }
    public PacketType Type { get; set; } = PacketType.Text;

    // Attachments
    public string? FilePath { get; set; }
    public string? FileName { get; set; }
    public long FileSizeBytes { get; set; }

    // Reply context
    public string? ReplyToId { get; set; }
    public string? ReplyToSender { get; set; }
    public string? ReplyToText { get; set; }

    [JsonIgnore]
    public bool HasReply => !string.IsNullOrEmpty(ReplyToId);

    [JsonIgnore]
    public bool HasAttachment => !string.IsNullOrEmpty(FilePath);

    [JsonIgnore]
    public bool IsPhoto => Type == PacketType.Photo;

    [JsonIgnore]
    public bool IsFile => Type == PacketType.File;
}
