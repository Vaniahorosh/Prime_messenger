using System.Windows;
using System.Windows.Input;
using PrimeDesktop.ViewModels;

namespace PrimeDesktop;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
    }

    private void TitleBar_MouseDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ChangedButton == MouseButton.Left)
        {
            DragMove();
        }
    }

    private void BtnMinimize_Click(object sender, RoutedEventArgs e)
    {
        WindowState = WindowState.Minimized;
    }

    private void BtnMaximize_Click(object sender, RoutedEventArgs e)
    {
        WindowState = (WindowState == WindowState.Maximized) ? WindowState.Normal : WindowState.Maximized;
    }

    private void BtnClose_Click(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private void MsgInputTextBox_PreviewKeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter)
        {
            if (Keyboard.Modifiers.HasFlag(ModifierKeys.Shift))
            {
                // Shift+Enter creates a new line
                int caret = MsgInputTextBox.CaretIndex;
                MsgInputTextBox.Text = MsgInputTextBox.Text.Insert(caret, Environment.NewLine);
                MsgInputTextBox.CaretIndex = caret + Environment.NewLine.Length;
                e.Handled = true;
            }
            else
            {
                // Enter sends message
                if (DataContext is MainViewModel vm && vm.SendMessageCommand.CanExecute(null))
                {
                    vm.SendMessageCommand.Execute(null);
                }
                e.Handled = true;
            }
        }
    }
}
