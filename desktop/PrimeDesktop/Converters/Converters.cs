using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;
using PrimeDesktop.Models;

namespace PrimeDesktop.Converters;

public class BoolToVisibilityConverter : IValueConverter
{
    public bool Invert { get; set; }

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        bool b = value is bool flag && flag;
        if (Invert) b = !b;
        return b ? Visibility.Visible : Visibility.Collapsed;
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value is Visibility vis && vis == Visibility.Visible;
    }
}

public class ConnectionStateColorConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        if (value is DeviceConnectionState state)
        {
            return state switch
            {
                DeviceConnectionState.Connected => "#2EA043", // Green
                DeviceConnectionState.Connecting => "#D29922", // Yellow/Amber
                _ => "#6E7681" // Gray
            };
        }
        return "#6E7681";
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
}

public class MessageAlignmentConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        bool isFromMe = value is bool b && b;
        return isFromMe ? HorizontalAlignment.Right : HorizontalAlignment.Left;
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
}

public class MessageBubbleBrushConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        bool isFromMe = value is bool b && b;
        return Application.Current.Resources[isFromMe ? "SentBubbleBrush" : "ReceivedBubbleBrush"];
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
}
