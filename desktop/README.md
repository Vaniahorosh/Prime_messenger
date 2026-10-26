# Prime Desktop (Windows)

Официальная десктопная версия мессенджера **«Прайм»** для Windows 10/11, созданная на **C# (.NET 8 WPF)** — основном нативном стеке для операционной системы Windows.

## Ключевые возможности:
* **Связка с телефоном (Dual-Mode):**
  1. **Direct Bluetooth RFCOMM:** Подключение напрямую к смартфонам с Prime Messenger по протоколу Bluetooth RFCOMM (UUID: `fa87c0d0-afac-11de-8a39-0800200c9a66`). Полная совместимость пакетов (0x01–0x0F).
  2. **Wi-Fi Companion Bridge:** Связка по локальной сети Wi-Fi для мгновенной синхронизации истории переписки, статусов и отправки сообщений.
* **Десктопный дизайн (Fluent / Dark Mode):**
  * 2-панельный интерфейс: список диалогов с поиском слева и переписка по центру.
  * Поддержка цитирования сообщений (`:::REPLY:::`).
  * Передача изображений (`📷`) и файлов (`📎`).
  * Индикаторы набора текста («Печатает...») и статусы доставки.
  * Управление мышью и хоткеи: `Enter` — отправка, `Shift + Enter` — новая строка.

## Структура проекта:
```text
desktop/
├── PrimeDesktop.sln            # Решение для Visual Studio
├── build.bat                   # Скрипт быстрой сборки
└── PrimeDesktop/
    ├── PrimeDesktop.csproj     # Конфигурация проекта (WPF, .NET 8, WinRT)
    ├── MainWindow.xaml         # Разметка окна в стиле Windows 11
    ├── MainWindow.xaml.cs      # Логика окна и горячие клавиши
    ├── Models/                 # Модели ChatItem, ChatMessage, PacketType
    ├── Network/                # Bluetooth RFCOMM и Wi-Fi Companion службы
    ├── Services/               # Хранилище SQLite/JSON и менеджер тем
    ├── ViewModels/             # MVVM архитектура (MainViewModel)
    └── Resources/              # Цветовая палитра и стили компонентов
```

## Как собрать и запустить:
1. **Через Visual Studio 2022 / 2024:**
   * Откройте файл `desktop/PrimeDesktop.sln` и нажмите **F5** (или `Ctrl + F5`).
2. **Через командную строку (.NET 8 SDK):**
   * Запустите `desktop/build.bat` или выполните:
   ```cmd
   cd desktop/PrimeDesktop
   dotnet run
   ```
