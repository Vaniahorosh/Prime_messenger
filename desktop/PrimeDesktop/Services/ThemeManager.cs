using System;
using System.Windows;
using System.Windows.Media;

namespace PrimeDesktop.Services;

public static class ThemeManager
{
    public static void ApplyAccentColor(string hexColor)
    {
        try
        {
            var color = (Color)ColorConverter.ConvertFromString(hexColor);
            Application.Current.Resources["AccentColor"] = color;
            Application.Current.Resources["AccentBrush"] = new SolidColorBrush(color);

            // Light hover / translucent shade
            var hoverColor = Color.FromArgb(40, color.R, color.G, color.B);
            Application.Current.Resources["AccentHoverBrush"] = new SolidColorBrush(hoverColor);
        }
        catch { }
    }
}
