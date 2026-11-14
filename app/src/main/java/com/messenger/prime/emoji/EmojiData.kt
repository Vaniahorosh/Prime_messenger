package com.messenger.prime.emoji

import android.content.Context

enum class EmojiCategory(val title: String, val icon: String) {
    RECENT("Частые", "🕒"),
    SMILEYS("Смайлы", "😀"),
    GESTURES("Люди", "👋"),
    ANIMALS("Природа", "🐱"),
    FOOD("Еда", "🍕"),
    ACTIVITIES("Спорт", "⚽"),
    TRAVEL("Поездки", "🚀"),
    OBJECTS("Предметы", "💡"),
    SYMBOLS("Символы", "❤️")
}

data class EmojiItem(
    val char: String,
    val category: EmojiCategory,
    val keywords: List<String> = emptyList()
)

object EmojiData {

    private val allEmojis: List<EmojiItem> = listOf(
        // SMILEYS & EMOTIONS
        EmojiItem("😀", EmojiCategory.SMILEYS, listOf("улыбка", "радость", "smile", "happy")),
        EmojiItem("😃", EmojiCategory.SMILEYS, listOf("улыбка", "радость", "smile", "happy")),
        EmojiItem("😄", EmojiCategory.SMILEYS, listOf("смех", "улыбка", "laugh", "happy")),
        EmojiItem("😁", EmojiCategory.SMILEYS, listOf("зубы", "grin", "smile")),
        EmojiItem("😆", EmojiCategory.SMILEYS, listOf("хаха", "смех", "laugh", "lol")),
        EmojiItem("😅", EmojiCategory.SMILEYS, listOf("пот", "неловко", "sweat", "relief")),
        EmojiItem("😂", EmojiCategory.SMILEYS, listOf("смех", "слезы", "ржу", "joy", "laugh", "cry", "lol", "rofl")),
        EmojiItem("🤣", EmojiCategory.SMILEYS, listOf("ржунимагу", "rofl", "lol", "laugh")),
        EmojiItem("🥲", EmojiCategory.SMILEYS, listOf("слеза", "улыбка сквозь слезы", "tear", "smile")),
        EmojiItem("🥹", EmojiCategory.SMILEYS, listOf("мило", "слезы", "растроган", "pleading", "cute")),
        EmojiItem("😊", EmojiCategory.SMILEYS, listOf("румянец", "доброта", "blush", "kind")),
        EmojiItem("😇", EmojiCategory.SMILEYS, listOf("ангел", "святой", "angel", "halo")),
        EmojiItem("🙂", EmojiCategory.SMILEYS, listOf("улыбка", "нормально", "slightly smile")),
        EmojiItem("🙃", EmojiCategory.SMILEYS, listOf("вверх ногами", "сарказм", "upside down", "irony")),
        EmojiItem("😉", EmojiCategory.SMILEYS, listOf("подмигивание", "wink")),
        EmojiItem("😌", EmojiCategory.SMILEYS, listOf("спокойствие", "облегчение", "relieved", "peace")),
        EmojiItem("😍", EmojiCategory.SMILEYS, listOf("любовь", "сердечки", "влюблен", "heart eyes", "love")),
        EmojiItem("🥰", EmojiCategory.SMILEYS, listOf("нежность", "любовь", "сердца", "hearts", "love")),
        EmojiItem("😘", EmojiCategory.SMILEYS, listOf("поцелуй", "kiss", "love")),
        EmojiItem("😗", EmojiCategory.SMILEYS, listOf("поцелуй", "kiss")),
        EmojiItem("😙", EmojiCategory.SMILEYS, listOf("поцелуй с улыбкой", "kiss")),
        EmojiItem("😚", EmojiCategory.SMILEYS, listOf("поцелуй румянец", "kiss")),
        EmojiItem("😋", EmojiCategory.SMILEYS, listOf("вкусно", "язык", "yummy", "delicious")),
        EmojiItem("😛", EmojiCategory.SMILEYS, listOf("дразнит", "язык", "tongue")),
        EmojiItem("😜", EmojiCategory.SMILEYS, listOf("подмигивает язык", "wink tongue", "crazy")),
        EmojiItem("🤪", EmojiCategory.SMILEYS, listOf("безумие", "сумасшедший", "zany", "crazy")),
        EmojiItem("😝", EmojiCategory.SMILEYS, listOf("язык", "squint tongue")),
        EmojiItem("🤑", EmojiCategory.SMILEYS, listOf("деньги", "богатство", "money")),
        EmojiItem("🤗", EmojiCategory.SMILEYS, listOf("обнимашки", "hug")),
        EmojiItem("🫣", EmojiCategory.SMILEYS, listOf("подглядывает", "стеснение", "peeking")),
        EmojiItem("🤫", EmojiCategory.SMILEYS, listOf("тихо", "секрет", "shh", "quiet")),
        EmojiItem("🤔", EmojiCategory.SMILEYS, listOf("думаю", "мысли", "задумался", "think", "hmm")),
        EmojiItem("🫡", EmojiCategory.SMILEYS, listOf("так точно", "честь", "salute")),
        EmojiItem("🤐", EmojiCategory.SMILEYS, listOf("молчу", "замок", "zipper", "silent")),
        EmojiItem("🤨", EmojiCategory.SMILEYS, listOf("бровь", "сомнение", "skeptical")),
        EmojiItem("😐", EmojiCategory.SMILEYS, listOf("нейтрально", "покерфейс", "neutral")),
        EmojiItem("😑", EmojiCategory.SMILEYS, listOf("без эмоций", "expressionless")),
        EmojiItem("😶", EmojiCategory.SMILEYS, listOf("без слов", "no mouth")),
        EmojiItem("🫥", EmojiCategory.SMILEYS, listOf("невидимка", "исчезаю", "dotted line")),
        EmojiItem("😏", EmojiCategory.SMILEYS, listOf("ухмылка", "хитрость", "smirk")),
        EmojiItem("😒", EmojiCategory.SMILEYS, listOf("недоволен", "скука", "unamused")),
        EmojiItem("🙄", EmojiCategory.SMILEYS, listOf("закатил глаза", "eye roll")),
        EmojiItem("😬", EmojiCategory.SMILEYS, listOf("кривит зубы", "grimace")),
        EmojiItem("🤥", EmojiCategory.SMILEYS, listOf("врать", "пиноккио", "lying")),
        EmojiItem("😴", EmojiCategory.SMILEYS, listOf("сон", "спит", "zzz", "sleep")),
        EmojiItem("🤤", EmojiCategory.SMILEYS, listOf("слюни", "вкусно", "drool")),
        EmojiItem("😷", EmojiCategory.SMILEYS, listOf("маска", "болеет", "mask", "sick")),
        EmojiItem("🤒", EmojiCategory.SMILEYS, listOf("температура", "градусник", "fever")),
        EmojiItem("🤕", EmojiCategory.SMILEYS, listOf("бинт", "травма", "bandage", "hurt")),
        EmojiItem("🤢", EmojiCategory.SMILEYS, listOf("тошнота", "nauseated")),
        EmojiItem("🤮", EmojiCategory.SMILEYS, listOf("рвота", "тошнит", "vomit")),
        EmojiItem("🤧", EmojiCategory.SMILEYS, listOf("чихает", "простуда", "sneeze")),
        EmojiItem("🥵", EmojiCategory.SMILEYS, listOf("жара", "горячо", "hot")),
        EmojiItem("🥶", EmojiCategory.SMILEYS, listOf("холод", "мороз", "cold", "freeze")),
        EmojiItem("🥴", EmojiCategory.SMILEYS, listOf("пьяный", "мутно", "woozy")),
        EmojiItem("😵", EmojiCategory.SMILEYS, listOf("головокружение", "dizzy")),
        EmojiItem("🤯", EmojiCategory.SMILEYS, listOf("взрыв мозга", "шок", "mind blown", "exploding")),
        EmojiItem("🤠", EmojiCategory.SMILEYS, listOf("ковбой", "шляпа", "cowboy")),
        EmojiItem("🥳", EmojiCategory.SMILEYS, listOf("праздник", "пати", "день рождения", "party", "celebrate")),
        EmojiItem("😎", EmojiCategory.SMILEYS, listOf("крутой", "очки", "cool", "sunglasses")),
        EmojiItem("🤓", EmojiCategory.SMILEYS, listOf("ботан", "умник", "nerd", "geek")),
        EmojiItem("🧐", EmojiCategory.SMILEYS, listOf("монокль", "детектив", "monocle")),
        EmojiItem("😕", EmojiCategory.SMILEYS, listOf("запутался", "confused")),
        EmojiItem("😟", EmojiCategory.SMILEYS, listOf("беспокойство", "worried")),
        EmojiItem("🙁", EmojiCategory.SMILEYS, listOf("грусть", "sad")),
        EmojiItem("😮", EmojiCategory.SMILEYS, listOf("ого", "удивление", "surprise")),
        EmojiItem("😯", EmojiCategory.SMILEYS, listOf("удивлен", "wow")),
        EmojiItem("😲", EmojiCategory.SMILEYS, listOf("шок", "поражен", "shocked")),
        EmojiItem("😳", EmojiCategory.SMILEYS, listOf("смущение", "румянец", "flushed")),
        EmojiItem("🥺", EmojiCategory.SMILEYS, listOf("пожалуйста", "щенок", "мило", "pleading")),
        EmojiItem("😦", EmojiCategory.SMILEYS, listOf("испуг", "frown")),
        EmojiItem("😨", EmojiCategory.SMILEYS, listOf("страх", "fear")),
        EmojiItem("😰", EmojiCategory.SMILEYS, listOf("тревога", "холодный пот", "anxious")),
        EmojiItem("😥", EmojiCategory.SMILEYS, listOf("печаль", "грусть", "sad")),
        EmojiItem("😢", EmojiCategory.SMILEYS, listOf("плач", "слеза", "cry")),
        EmojiItem("😭", EmojiCategory.SMILEYS, listOf("рыдание", "плачу", "слезы", "sob", "cry")),
        EmojiItem("😱", EmojiCategory.SMILEYS, listOf("крик", "ужас", "мунк", "scream")),
        EmojiItem("😖", EmojiCategory.SMILEYS, listOf("досада", "confounded")),
        EmojiItem("😣", EmojiCategory.SMILEYS, listOf("терпение", "persevere")),
        EmojiItem("😞", EmojiCategory.SMILEYS, listOf("разочарование", "disappointed")),
        EmojiItem("😓", EmojiCategory.SMILEYS, listOf("усталость", "sweat")),
        EmojiItem("😩", EmojiCategory.SMILEYS, listOf("устал", "weary")),
        EmojiItem("😫", EmojiCategory.SMILEYS, listOf("вымотан", "tired")),
        EmojiItem("🥱", EmojiCategory.SMILEYS, listOf("зевота", "скучно", "yawn")),
        EmojiItem("😤", EmojiCategory.SMILEYS, listOf("пар из носа", "гордость", "triumph")),
        EmojiItem("😡", EmojiCategory.SMILEYS, listOf("злость", "красный", "angry", "rage")),
        EmojiItem("😠", EmojiCategory.SMILEYS, listOf("сердитый", "angry")),
        EmojiItem("🤬", EmojiCategory.SMILEYS, listOf("мат", "ругательства", "злость", "censored", "curse")),
        EmojiItem("😈", EmojiCategory.SMILEYS, listOf("чертик", "дьявол", "улыбка", "devil", "evil")),
        EmojiItem("👿", EmojiCategory.SMILEYS, listOf("злой дьявол", "devil")),
        EmojiItem("💀", EmojiCategory.SMILEYS, listOf("череп", "смерть", "умер от смеха", "skull", "dead")),
        EmojiItem("☠️", EmojiCategory.SMILEYS, listOf("пират", "опасность", "crossbones")),
        EmojiItem("💩", EmojiCategory.SMILEYS, listOf("какашка", "poop")),
        EmojiItem("🤡", EmojiCategory.SMILEYS, listOf("клоун", "цирк", "кринж", "clown", "cringe")),
        EmojiItem("👻", EmojiCategory.SMILEYS, listOf("призрак", "хэллоуин", "ghost")),
        EmojiItem("👽", EmojiCategory.SMILEYS, listOf("инопланетянин", "пришелец", "alien")),
        EmojiItem("🤖", EmojiCategory.SMILEYS, listOf("робот", "бот", "ai", "robot")),

        // GESTURES & PEOPLE
        EmojiItem("👋", EmojiCategory.GESTURES, listOf("привет", "пока", "машет", "wave", "hello", "bye")),
        EmojiItem("🤚", EmojiCategory.GESTURES, listOf("ладонь", "стоп", "raised hand")),
        EmojiItem("🖐️", EmojiCategory.GESTURES, listOf("пять пальцев", "hand")),
        EmojiItem("✋", EmojiCategory.GESTURES, listOf("рука", "стоп", "hand", "stop")),
        EmojiItem("🖖", EmojiCategory.GESTURES, listOf("вулкан", "спок", "vulcan")),
        EmojiItem("🫱", EmojiCategory.GESTURES, listOf("рука направо", "rightwards hand")),
        EmojiItem("🫲", EmojiCategory.GESTURES, listOf("рука налево", "leftwards hand")),
        EmojiItem("🫳", EmojiCategory.GESTURES, listOf("ладонь вниз", "palm down")),
        EmojiItem("🫴", EmojiCategory.GESTURES, listOf("ладонь вверх", "дай", "palm up")),
        EmojiItem("👌", EmojiCategory.GESTURES, listOf("ок", "хорошо", "идеально", "ok", "perfect")),
        EmojiItem("🤌", EmojiCategory.GESTURES, listOf("итальянский жест", "что ты хочешь", "pinched")),
        EmojiItem("🤏", EmojiCategory.GESTURES, listOf("чуть-чуть", "мало", "pinching")),
        EmojiItem("✌️", EmojiCategory.GESTURES, listOf("мир", "победа", "два", "peace", "victory")),
        EmojiItem("🤞", EmojiCategory.GESTURES, listOf("удача", "скрестил пальцы", "luck", "crossed fingers")),
        EmojiItem("🫰", EmojiCategory.GESTURES, listOf("корейское сердечко", "деньги", "hand heart")),
        EmojiItem("🤟", EmojiCategory.GESTURES, listOf("люблю тебя", "рок", "love you", "rock")),
        EmojiItem("🤘", EmojiCategory.GESTURES, listOf("рок", "метал", "rock on", "horns")),
        EmojiItem("🤙", EmojiCategory.GESTURES, listOf("позвони мне", "шака", "call me", "shaka")),
        EmojiItem("👈", EmojiCategory.GESTURES, listOf("влево", "туда", "point left")),
        EmojiItem("👉", EmojiCategory.GESTURES, listOf("вправо", "туда", "point right")),
        EmojiItem("👆", EmojiCategory.GESTURES, listOf("вверх", "point up")),
        EmojiItem("👇", EmojiCategory.GESTURES, listOf("вниз", "point down")),
        EmojiItem("☝️", EmojiCategory.GESTURES, listOf("внимание", "палец вверх", "index up")),
        EmojiItem("👍", EmojiCategory.GESTURES, listOf("класс", "лайк", "хорошо", "супер", "like", "thumbs up", "cool")),
        EmojiItem("👎", EmojiCategory.GESTURES, listOf("дизлайк", "плохо", "dislike", "thumbs down")),
        EmojiItem("✊", EmojiCategory.GESTURES, listOf("кулак", "сила", "fist")),
        EmojiItem("👊", EmojiCategory.GESTURES, listOf("удар кулаком", "бро", "punch", "fist bump")),
        EmojiItem("🤛", EmojiCategory.GESTURES, listOf("кулак влево", "left fist")),
        EmojiItem("🤜", EmojiCategory.GESTURES, listOf("кулак вправо", "right fist")),
        EmojiItem("👏", EmojiCategory.GESTURES, listOf("аплодисменты", "браво", "хлопки", "clap", "applause")),
        EmojiItem("🙌", EmojiCategory.GESTURES, listOf("ура", "руки вверх", "celebration", "hooray")),
        EmojiItem("🫶", EmojiCategory.GESTURES, listOf("сердце руками", "любовь", "heart hands")),
        EmojiItem("👐", EmojiCategory.GESTURES, listOf("открытые ладони", "open hands")),
        EmojiItem("🤲", EmojiCategory.GESTURES, listOf("ладони вместе", "palms together")),
        EmojiItem("🤝", EmojiCategory.GESTURES, listOf("рукопожатие", "сделка", "договорились", "handshake", "deal")),
        EmojiItem("🙏", EmojiCategory.GESTURES, listOf("спасибо", "молитва", "пожалуйста", "pray", "thanks", "please")),
        EmojiItem("✍️", EmojiCategory.GESTURES, listOf("пишу", "запись", "write")),
        EmojiItem("💅", EmojiCategory.GESTURES, listOf("маникюр", "ногти", "лак", "nails")),
        EmojiItem("🤳", EmojiCategory.GESTURES, listOf("селфи", "selfie")),
        EmojiItem("💪", EmojiCategory.GESTURES, listOf("мышцы", "сила", "бицепс", "strong", "muscle", "power")),
        EmojiItem("👀", EmojiCategory.GESTURES, listOf("глаза", "смотрю", "вижу", "eyes", "look")),
        EmojiItem("👁️", EmojiCategory.GESTURES, listOf("глаз", "eye")),
        EmojiItem("🧠", EmojiCategory.GESTURES, listOf("мозг", "ум", "интеллект", "brain", "smart")),
        EmojiItem("🫀", EmojiCategory.GESTURES, listOf("сердце орган", "anatomical heart")),
        EmojiItem("🔥", EmojiCategory.GESTURES, listOf("огонь", "пламя", "топ", "жара", "fire", "hot", "lit")),

        // ANIMALS & NATURE
        EmojiItem("🐶", EmojiCategory.ANIMALS, listOf("собака", "пес", "щенок", "dog", "puppy")),
        EmojiItem("🐱", EmojiCategory.ANIMALS, listOf("кот", "кошка", "котенок", "cat", "kitty")),
        EmojiItem("🐭", EmojiCategory.ANIMALS, listOf("мышь", "mouse")),
        EmojiItem("🐹", EmojiCategory.ANIMALS, listOf("хомяк", "hamster")),
        EmojiItem("🐰", EmojiCategory.ANIMALS, listOf("кролик", "заяц", "rabbit", "bunny")),
        EmojiItem("🦊", EmojiCategory.ANIMALS, listOf("лиса", "fox")),
        EmojiItem("🐻", EmojiCategory.ANIMALS, listOf("медведь", "bear")),
        EmojiItem("🐼", EmojiCategory.ANIMALS, listOf("панда", "panda")),
        EmojiItem("🐻‍❄️", EmojiCategory.ANIMALS, listOf("белый медведь", "polar bear")),
        EmojiItem("🐨", EmojiCategory.ANIMALS, listOf("коала", "koala")),
        EmojiItem("🐯", EmojiCategory.ANIMALS, listOf("тигр", "tiger")),
        EmojiItem("🦁", EmojiCategory.ANIMALS, listOf("лев", "lion")),
        EmojiItem("🐮", EmojiCategory.ANIMALS, listOf("корова", "cow")),
        EmojiItem("🐷", EmojiCategory.ANIMALS, listOf("свинья", "pig")),
        EmojiItem("🐸", EmojiCategory.ANIMALS, listOf("лягушка", "жаба", "frog")),
        EmojiItem("🐵", EmojiCategory.ANIMALS, listOf("обезьяна", "monkey")),
        EmojiItem("🙈", EmojiCategory.ANIMALS, listOf("не вижу", "see no evil")),
        EmojiItem("🙉", EmojiCategory.ANIMALS, listOf("не слышу", "hear no evil")),
        EmojiItem("🙊", EmojiCategory.ANIMALS, listOf("не скажу", "speak no evil")),
        EmojiItem("🐧", EmojiCategory.ANIMALS, listOf("пингвин", "penguin")),
        EmojiItem("🐦", EmojiCategory.ANIMALS, listOf("птица", "bird")),
        EmojiItem("🦅", EmojiCategory.ANIMALS, listOf("орел", "eagle")),
        EmojiItem("🦉", EmojiCategory.ANIMALS, listOf("сова", "owl")),
        EmojiItem("🦇", EmojiCategory.ANIMALS, listOf("летучая мышь", "bat")),
        EmojiItem("🐺", EmojiCategory.ANIMALS, listOf("волк", "wolf")),
        EmojiItem("🦄", EmojiCategory.ANIMALS, listOf("единорог", "магия", "unicorn")),
        EmojiItem("🐝", EmojiCategory.ANIMALS, listOf("пчела", "bee")),
        EmojiItem("🦋", EmojiCategory.ANIMALS, listOf("бабочка", "butterfly")),
        EmojiItem("🐢", EmojiCategory.ANIMALS, listOf("черепаха", "turtle")),
        EmojiItem("🐍", EmojiCategory.ANIMALS, listOf("змея", "snake")),
        EmojiItem("🐙", EmojiCategory.ANIMALS, listOf("осьминог", "octopus")),
        EmojiItem("🐬", EmojiCategory.ANIMALS, listOf("дельфин", "dolphin")),
        EmojiItem("🐳", EmojiCategory.ANIMALS, listOf("кит", "whale")),
        EmojiItem("🦈", EmojiCategory.ANIMALS, listOf("акула", "shark")),
        EmojiItem("🌸", EmojiCategory.ANIMALS, listOf("цветок", "сакура", "flower", "cherry")),
        EmojiItem("🌹", EmojiCategory.ANIMALS, listOf("роза", "цветок", "rose")),
        EmojiItem("🌻", EmojiCategory.ANIMALS, listOf("подсолнух", "sunflower")),
        EmojiItem("🌲", EmojiCategory.ANIMALS, listOf("дерево", "елка", "tree")),
        EmojiItem("🍀", EmojiCategory.ANIMALS, listOf("клевер", "удача", "clover", "luck")),
        EmojiItem("🍄", EmojiCategory.ANIMALS, listOf("гриб", "mushroom")),
        EmojiItem("⭐", EmojiCategory.ANIMALS, listOf("звезда", "star")),
        EmojiItem("✨", EmojiCategory.ANIMALS, listOf("искры", "блеск", "магия", "sparkles", "shine")),
        EmojiItem("☀️", EmojiCategory.ANIMALS, listOf("солнце", "тепло", "sun")),
        EmojiItem("🌙", EmojiCategory.ANIMALS, listOf("луна", "месяц", "ночь", "moon", "night")),
        EmojiItem("⚡", EmojiCategory.ANIMALS, listOf("молния", "ток", "lightning", "zap")),
        EmojiItem("🌈", EmojiCategory.ANIMALS, listOf("радуга", "rainbow")),
        EmojiItem("❄️", EmojiCategory.ANIMALS, listOf("снежинка", "зима", "snow")),

        // FOOD & DRINK
        EmojiItem("🍏", EmojiCategory.FOOD, listOf("яблоко", "зеленое яблоко", "apple")),
        EmojiItem("🍎", EmojiCategory.FOOD, listOf("яблоко", "красное яблоко", "apple")),
        EmojiItem("🍌", EmojiCategory.FOOD, listOf("банан", "banana")),
        EmojiItem("🍉", EmojiCategory.FOOD, listOf("арбуз", "watermelon")),
        EmojiItem("🍇", EmojiCategory.FOOD, listOf("виноград", "grape")),
        EmojiItem("🍓", EmojiCategory.FOOD, listOf("клубника", "strawberry")),
        EmojiItem("🍒", EmojiCategory.FOOD, listOf("вишня", "cherry")),
        EmojiItem("🍑", EmojiCategory.FOOD, listOf("персик", "peach")),
        EmojiItem("🍍", EmojiCategory.FOOD, listOf("ананас", "pineapple")),
        EmojiItem("🥑", EmojiCategory.FOOD, listOf("авокадо", "avocado")),
        EmojiItem("🍔", EmojiCategory.FOOD, listOf("бургер", "бургеры", "гамбургер", "burger", "hamburger")),
        EmojiItem("🍟", EmojiCategory.FOOD, listOf("картошка фри", "fries")),
        EmojiItem("🍕", EmojiCategory.FOOD, listOf("пицца", "pizza")),
        EmojiItem("🌭", EmojiCategory.FOOD, listOf("хотдог", "hot dog")),
        EmojiItem("🥪", EmojiCategory.FOOD, listOf("сэндвич", "бутерброд", "sandwich")),
        EmojiItem("🌮", EmojiCategory.FOOD, listOf("тако", "taco")),
        EmojiItem("🍣", EmojiCategory.FOOD, listOf("суши", "роллы", "sushi")),
        EmojiItem("🍜", EmojiCategory.FOOD, listOf("рамен", "лапша", "ramen", "noodles")),
        EmojiItem("🍝", EmojiCategory.FOOD, listOf("паста", "спагетти", "pasta")),
        EmojiItem("🍰", EmojiCategory.FOOD, listOf("торт", "пирожное", "cake")),
        EmojiItem("🍩", EmojiCategory.FOOD, listOf("пончик", "donut")),
        EmojiItem("🍪", EmojiCategory.FOOD, listOf("печенье", "cookie")),
        EmojiItem("🍫", EmojiCategory.FOOD, listOf("шоколад", "chocolate")),
        EmojiItem("🍦", EmojiCategory.FOOD, listOf("мороженое", "ice cream")),
        EmojiItem("☕", EmojiCategory.FOOD, listOf("кофе", "чай", "чашка", "coffee", "tea", "cafe")),
        EmojiItem("🧋", EmojiCategory.FOOD, listOf("бабл ти", "bubble tea")),
        EmojiItem("🍺", EmojiCategory.FOOD, listOf("пиво", "бокал", "beer")),
        EmojiItem("🍻", EmojiCategory.FOOD, listOf("пиво", "тост", "cheers", "beers")),
        EmojiItem("🍷", EmojiCategory.FOOD, listOf("вино", "бокал вина", "wine")),
        EmojiItem("🍾", EmojiCategory.FOOD, listOf("шампанское", "праздник", "champagne")),

        // ACTIVITIES & SPORT
        EmojiItem("⚽", EmojiCategory.ACTIVITIES, listOf("футбол", "мяч", "soccer", "football")),
        EmojiItem("🏀", EmojiCategory.ACTIVITIES, listOf("баскетбол", "basketball")),
        EmojiItem("🏈", EmojiCategory.ACTIVITIES, listOf("американский футбол", "american football")),
        EmojiItem("🎾", EmojiCategory.ACTIVITIES, listOf("теннис", "tennis")),
        EmojiItem("🏐", EmojiCategory.ACTIVITIES, listOf("волейбол", "volleyball")),
        EmojiItem("🥊", EmojiCategory.ACTIVITIES, listOf("бокс", "перчатки", "boxing")),
        EmojiItem("🏆", EmojiCategory.ACTIVITIES, listOf("кубок", "победа", "награда", "trophy", "win")),
        EmojiItem("🥇", EmojiCategory.ACTIVITIES, listOf("золотая медаль", "первое место", "gold medal")),
        EmojiItem("🥈", EmojiCategory.ACTIVITIES, listOf("серебро", "silver medal")),
        EmojiItem("🥉", EmojiCategory.ACTIVITIES, listOf("бронза", "bronze medal")),
        EmojiItem("🎮", EmojiCategory.ACTIVITIES, listOf("игры", "гейминг", "джойстик", "playstation", "game", "gaming")),
        EmojiItem("🎲", EmojiCategory.ACTIVITIES, listOf("кости", "игра", "dice")),
        EmojiItem("🎯", EmojiCategory.ACTIVITIES, listOf("дартс", "мишень", "в цель", "target", "bullseye")),
        EmojiItem("🎧", EmojiCategory.ACTIVITIES, listOf("наушники", "музыка", "headphones", "music")),
        EmojiItem("🎤", EmojiCategory.ACTIVITIES, listOf("микрофон", "петь", "караоке", "mic", "sing")),
        EmojiItem("🎬", EmojiCategory.ACTIVITIES, listOf("кино", "фильм", "хлопушка", "cinema", "movie")),
        EmojiItem("🎨", EmojiCategory.ACTIVITIES, listOf("рисование", "палитра", "искусство", "art", "paint")),

        // TRAVEL & PLACES
        EmojiItem("🚗", EmojiCategory.TRAVEL, listOf("машина", "авто", "car", "auto")),
        EmojiItem("🚕", EmojiCategory.TRAVEL, listOf("такси", "taxi")),
        EmojiItem("🏎️", EmojiCategory.TRAVEL, listOf("гонки", "болид", "race car")),
        EmojiItem("🚌", EmojiCategory.TRAVEL, listOf("автобус", "bus")),
        EmojiItem("🚓", EmojiCategory.TRAVEL, listOf("полиция", "police")),
        EmojiItem("🚑", EmojiCategory.TRAVEL, listOf("скорая", "ambulance")),
        EmojiItem("🚒", EmojiCategory.TRAVEL, listOf("пожарные", "fire truck")),
        EmojiItem("🚲", EmojiCategory.TRAVEL, listOf("велосипед", "bike", "bicycle")),
        EmojiItem("🛵", EmojiCategory.TRAVEL, listOf("мопед", "скутер", "scooter")),
        EmojiItem("🏍️", EmojiCategory.TRAVEL, listOf("мотоцикл", "motorcycle", "bike")),
        EmojiItem("✈️", EmojiCategory.TRAVEL, listOf("самолет", "полет", "plane", "flight")),
        EmojiItem("🚀", EmojiCategory.TRAVEL, listOf("ракета", "старт", "космос", "rocket", "launch")),
        EmojiItem("🛸", EmojiCategory.TRAVEL, listOf("нло", "тарелка", "ufo")),
        EmojiItem("🚢", EmojiCategory.TRAVEL, listOf("корабль", "круиз", "ship")),
        EmojiItem("🗺️", EmojiCategory.TRAVEL, listOf("карта", "путешествие", "map")),
        EmojiItem("🏖️", EmojiCategory.TRAVEL, listOf("пляж", "море", "отпуск", "beach", "vacation")),
        EmojiItem("🏔️", EmojiCategory.TRAVEL, listOf("горы", "снег", "mountains")),

        // OBJECTS
        EmojiItem("📱", EmojiCategory.OBJECTS, listOf("телефон", "смартфон", "phone", "mobile")),
        EmojiItem("💻", EmojiCategory.OBJECTS, listOf("ноутбук", "компьютер", "laptop", "pc")),
        EmojiItem("⌨️", EmojiCategory.OBJECTS, listOf("клавиатура", "keyboard")),
        EmojiItem("🖥️", EmojiCategory.OBJECTS, listOf("монитор", "desktop")),
        EmojiItem("📷", EmojiCategory.OBJECTS, listOf("камера", "фото", "camera", "photo")),
        EmojiItem("📸", EmojiCategory.OBJECTS, listOf("камера со вспышкой", "camera")),
        EmojiItem("📹", EmojiCategory.OBJECTS, listOf("видеокамера", "video")),
        EmojiItem("💡", EmojiCategory.OBJECTS, listOf("лампочка", "идея", "light bulb", "idea")),
        EmojiItem("🔦", EmojiCategory.OBJECTS, listOf("фонарик", "flashlight")),
        EmojiItem("⏰", EmojiCategory.OBJECTS, listOf("будильник", "время", "alarm", "clock")),
        EmojiItem("💰", EmojiCategory.OBJECTS, listOf("деньги", "мешок денег", "money", "cash")),
        EmojiItem("💵", EmojiCategory.OBJECTS, listOf("доллар", "купюра", "dollar", "money")),
        EmojiItem("💳", EmojiCategory.OBJECTS, listOf("карта", "кредитка", "credit card")),
        EmojiItem("💎", EmojiCategory.OBJECTS, listOf("алмаз", "бриллиант", "кристалл", "diamond", "gem")),
        EmojiItem("🎁", EmojiCategory.OBJECTS, listOf("подарок", "сюрприз", "gift", "present")),
        EmojiItem("🎈", EmojiCategory.OBJECTS, listOf("шарик", "праздник", "balloon")),
        EmojiItem("🎉", EmojiCategory.OBJECTS, listOf("конфетти", "праздник", "поздравляю", "party", "tada")),
        EmojiItem("🔒", EmojiCategory.OBJECTS, listOf("замок", "безопасность", "lock", "secure")),
        EmojiItem("🔑", EmojiCategory.OBJECTS, listOf("ключ", "key")),
        EmojiItem("📦", EmojiCategory.OBJECTS, listOf("посылка", "коробка", "box", "package")),
        EmojiItem("✉️", EmojiCategory.OBJECTS, listOf("письмо", "сообщение", "envelope", "mail")),

        // SYMBOLS & HEARTS
        EmojiItem("❤️", EmojiCategory.SYMBOLS, listOf("сердце", "красное сердце", "любовь", "heart", "love", "red heart")),
        EmojiItem("🧡", EmojiCategory.SYMBOLS, listOf("оранжевое сердце", "heart")),
        EmojiItem("💛", EmojiCategory.SYMBOLS, listOf("желтое сердце", "heart")),
        EmojiItem("💚", EmojiCategory.SYMBOLS, listOf("зеленое сердце", "heart")),
        EmojiItem("💙", EmojiCategory.SYMBOLS, listOf("синее сердце", "heart")),
        EmojiItem("💜", EmojiCategory.SYMBOLS, listOf("фиолетовое сердце", "heart")),
        EmojiItem("🖤", EmojiCategory.SYMBOLS, listOf("черное сердце", "black heart")),
        EmojiItem("🤍", EmojiCategory.SYMBOLS, listOf("белое сердце", "white heart")),
        EmojiItem("💔", EmojiCategory.SYMBOLS, listOf("разбитое сердце", "broken heart")),
        EmojiItem("❣️", EmojiCategory.SYMBOLS, listOf("сердце восклицание", "heart exclamation")),
        EmojiItem("💕", EmojiCategory.SYMBOLS, listOf("два сердца", "two hearts")),
        EmojiItem("💞", EmojiCategory.SYMBOLS, listOf("крутящиеся сердца", "revolving hearts")),
        EmojiItem("💓", EmojiCategory.SYMBOLS, listOf("бьющееся сердце", "beating heart")),
        EmojiItem("💗", EmojiCategory.SYMBOLS, listOf("растущее сердце", "growing heart")),
        EmojiItem("💖", EmojiCategory.SYMBOLS, listOf("сияющее сердце", "sparkling heart")),
        EmojiItem("💘", EmojiCategory.SYMBOLS, listOf("сердце со стрелой", "cupid")),
        EmojiItem("💝", EmojiCategory.SYMBOLS, listOf("сердце с лентой", "gift heart")),
        EmojiItem("💯", EmojiCategory.SYMBOLS, listOf("сто", "100", "максимум", "hundred")),
        EmojiItem("💢", EmojiCategory.SYMBOLS, listOf("гнев", "злость", "anger")),
        EmojiItem("💥", EmojiCategory.SYMBOLS, listOf("взрыв", "бум", "boom", "collision")),
        EmojiItem("💬", EmojiCategory.SYMBOLS, listOf("сообщение", "чат", "облако", "speech bubble", "chat")),
        EmojiItem("💭", EmojiCategory.SYMBOLS, listOf("мысли", "thought bubble")),
        EmojiItem("🔔", EmojiCategory.SYMBOLS, listOf("колокольчик", "уведомление", "bell", "notification")),
        EmojiItem("🔕", EmojiCategory.SYMBOLS, listOf("без звука", "bell slash")),
        EmojiItem("✔️", EmojiCategory.SYMBOLS, listOf("галочка", "да", "check")),
        EmojiItem("❌", EmojiCategory.SYMBOLS, listOf("крестик", "нет", "отмена", "cross", "x")),
        EmojiItem("⚠️", EmojiCategory.SYMBOLS, listOf("внимание", "предупреждение", "warning")),
        EmojiItem("⛔", EmojiCategory.SYMBOLS, listOf("вход воспрещен", "no entry")),
        EmojiItem("❓", EmojiCategory.SYMBOLS, listOf("вопрос", "знак вопроса", "question")),
        EmojiItem("❗", EmojiCategory.SYMBOLS, listOf("восклицание", "внимание", "exclamation")),
        EmojiItem("➕", EmojiCategory.SYMBOLS, listOf("плюс", "plus")),
        EmojiItem("➖", EmojiCategory.SYMBOLS, listOf("минус", "minus"))
    )

    private val categoryMap: Map<EmojiCategory, List<String>> by lazy {
        allEmojis.groupBy { it.category }.mapValues { entry -> entry.value.map { it.char }.distinct() }
    }

    fun getByCategory(category: EmojiCategory, context: Context? = null): List<String> {
        if (category == EmojiCategory.RECENT) {
            return getRecentEmojis(context)
        }
        return categoryMap[category] ?: emptyList()
    }

    fun search(query: String): List<String> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        return allEmojis.asSequence()
            .filter { item ->
                (item.char == trimmed) || item.keywords.any { it.contains(trimmed) }
            }
            .map { it.char }
            .distinct()
            .toList()
    }

    fun addRecentEmoji(context: Context, emoji: String) {
        try {
            val prefs = context.getSharedPreferences("PrimeEmojiPrefs", Context.MODE_PRIVATE)
            val existing = prefs.getString("recent_emojis", "")?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            existing.remove(emoji)
            existing.add(0, emoji)
            val trimmed = existing.distinct().take(32)
            prefs.edit().putString("recent_emojis", trimmed.joinToString(",")).apply()
        } catch (_: Exception) {}
    }

    fun clearRecents(context: Context) {
        try {
            val prefs = context.getSharedPreferences("PrimeEmojiPrefs", Context.MODE_PRIVATE)
            prefs.edit().remove("recent_emojis").apply()
        } catch (_: Exception) {}
    }

    private fun getRecentEmojis(context: Context?): List<String> {
        val defaultList = listOf("😀", "😂", "🔥", "❤️", "👍", "😍", "✨", "🎉", "🙏", "😎", "🥺", "💯", "👏", "🥰", "🤔", "👋")
        if (context == null) return defaultList
        try {
            val prefs = context.getSharedPreferences("PrimeEmojiPrefs", Context.MODE_PRIVATE)
            val saved = prefs.getString("recent_emojis", "")?.split(",")?.filter { it.isNotBlank() }?.distinct()
            if (!saved.isNullOrEmpty()) {
                return saved
            }
        } catch (_: Exception) {}
        return defaultList
    }
}
