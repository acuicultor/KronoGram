package org.telegram.ui.ActionBar;

import android.text.TextUtils;

import org.telegram.messenger.LocaleController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public final class ThemeKeyLocalizer {

    // Supported language column indices:
    // 0:en, 1:es, 2:ru, 3:zh, 4:zh-TW, 5:it, 6:fr, 7:pt, 8:de, 9:ar, 10:uk, 11:tr, 12:ko, 13:ja, 14:nl
    private static final int LANG_EN = 0;
    private static final int LANG_ES = 1;
    private static final int LANG_RU = 2;
    private static final int LANG_ZH = 3;
    private static final int LANG_ZH_TW = 4;
    private static final int LANG_IT = 5;
    private static final int LANG_FR = 6;
    private static final int LANG_PT = 7;
    private static final int LANG_DE = 8;
    private static final int LANG_AR = 9;
    private static final int LANG_UK = 10;
    private static final int LANG_TR = 11;
    private static final int LANG_KO = 12;
    private static final int LANG_JA = 13;
    private static final int LANG_NL = 14;

    private static volatile int cachedLang = -1;
    private static final HashMap<String, String> cache = new HashMap<>();

    private static final HashMap<String, String[]> EXACT = new HashMap<>();
    private static final HashMap<String, String[]> TOKENS = new HashMap<>();

    static {
        // High-frequency & screenshot-visible exact translations
        ex("windowBackgroundWhite",
                "Main window background", "Fondo principal de ventana", "Основной фон окна", "主窗口背景", "主視窗背景",
                "Sfondo principale finestra", "Arrière-plan principal de fenêtre", "Fundo principal da janela",
                "Hauptfenster-Hintergrund", "خلفية النافذة الرئيسية", "Основний фон вікна", "Ana pencere arka planı",
                "기본 창 배경", "メインウィンドウ背景", "Hoofdvenster-achtergrond");
        ex("windowBackgroundGray",
                "Secondary window background", "Fondo secundario gris", "Серый фон окна", "次级窗口背景（灰色）", "次級視窗背景（灰色）",
                "Sfondo secondario grigio", "Arrière-plan secondaire gris", "Fundo secundário cinza",
                "Sekundärer Fensterhintergrund", "خلفية النافذة الرمادية", "Сірий фон вікна", "İkincil gri pencere arka planı",
                "보조 창 배경 (회색)", "サブウィンドウ背景（グレー）", "Grijze vensterachtergrond");
        ex("windowBackgroundGrayShadow",
                "Section divider shadow", "Sombra divisora de secciones", "Тень разделителя секций", "分区阴影", "分區陰影",
                "Ombra divisore sezioni", "Ombre de séparation de section", "Sombra divisória de seções",
                "Abschnittstrenn-Schatten", "ظل فاصل الأقسام", "Тінь роздільника секцій", "Bölüm ayırıcı gölge",
                "섹션 구분 그림자", "セクション区切り影", "Sectiescheiding-schaduw");
        ex("windowBackgroundWhiteBlackText",
                "Primary text on main background", "Texto principal sobre fondo blanco", "Основной текст на белом фоне", "主背景主要文字", "主背景主要文字",
                "Testo principale su sfondo bianco", "Texte principal sur fond blanc", "Texto principal no fundo branco",
                "Haupttext auf weißem Hintergrund", "النص الرئيسي على الخلفية البيضاء", "Основний текст на білому фоні", "Beyaz arka planda ana metin",
                "기본 배경 기본 텍스트", "メイン背景のメインテキスト", "Hoofdtekst op witte achtergrond");
        ex("windowBackgroundWhiteBlueText",
                "Blue text on main background", "Texto azul sobre fondo principal", "Синий текст на белом фоне", "主背景蓝色文字", "主背景藍色文字",
                "Testo blu su sfondo principale", "Texte bleu sur fond principal", "Texto azul no fundo principal",
                "Blauer Text auf Haupthintergrund", "نص أزرق على الخلفية الرئيسية", "Синій текст на білому фоні", "Ana arka planda mavi metin",
                "기본 배경 파란색 텍스트", "メイン背景の青テキスト", "Blauwe tekst op hoofdachtergrond");
        ex("windowBackgroundWhiteBlueText2",
                "Blue text 2 on main background", "Texto azul 2 sobre fondo principal", "Синий текст 2 на белом фоне", "主背景蓝色文字 2", "主背景藍色文字 2",
                "Testo blu 2 su sfondo principale", "Texte bleu 2 sur fond principal", "Texto azul 2 no fundo principal",
                "Blauer Text 2 auf Haupthintergrund", "نص أزرق 2 على الخلفية الرئيسية", "Синій текст 2 на білому фоні", "Ana arka planda mavi metin 2",
                "기본 배경 파란색 텍스트 2", "メイン背景の青テキスト 2", "Blauwe tekst 2 op hoofdachtergrond");
        ex("windowBackgroundWhiteBlueText4",
                "Accent blue text on main background", "Texto azul de acento", "Акцентный синий текст", "主背景强调蓝文字", "主背景強調藍文字",
                "Testo blu di accento", "Texte bleu d'accentuation", "Texto azul de destaque",
                "Akzenttext Blau", "نص أزرق مميز", "Акцентний синій текст", "Vurgulu mavi metin",
                "강조 파란색 텍스트", "アクセント青テキスト", "Blauwe accenttekst");
        ex("windowBackgroundWhiteBlueHeader",
                "Section header text", "Título de sección", "Заголовок секции", "分组标题文字", "分組標題文字",
                "Intestazione sezione", "Titre de section", "Título de seção",
                "Abschnittsüberschrift", "عنوان القسم", "Заголовок секції", "Bölüm başlığı",
                "섹션 제목 텍스트", "セクション見出し", "Sectiekoptekst");
        ex("windowBackgroundWhiteGrayText",
                "Secondary gray text", "Texto secundario gris", "Серый вспомогательный текст", "灰色次要文字", "灰色次要文字",
                "Testo secondario grigio", "Texte secondaire gris", "Texto secundário cinza",
                "Sekundärer grauer Text", "نص رمادي ثانوي", "Сірий допоміжний текст", "İkincil gri metin",
                "보조 회색 텍스트", "サブグレーテキスト", "Grijze secundaire tekst");
        ex("windowBackgroundWhiteGrayText2",
                "Subtitle gray text", "Texto de subtítulo gris", "Серый подзаголовок", "灰色副标题文字", "灰色副標題文字",
                "Sottotitolo grigio", "Sous-titre gris", "Subtítulo cinza",
                "Grauer Untertiteltext", "نص عنوان فرعي رمادي", "Сірий підзаголовок", "Gri alt başlık metni",
                "회색 부제목 텍스트", "グレーのサブタイトル", "Grijze ondertiteltekst");
        ex("windowBackgroundWhiteGrayIcon",
                "List icon on main background", "Icono gris de lista", "Серая иконка списка", "列表灰色图标", "列表灰色圖示",
                "Icona grigia elenco", "Icône grise de liste", "Ícone cinza da lista",
                "Graues Listensymbol", "أيقونة رمادية في القائمة", "Сіра іконка списку", "Liste gri simgesi",
                "목록 회색 아이콘", "リストのグレーアイコン", "Grijs lijstpictogram");
        ex("windowBackgroundWhiteHintText",
                "Input hint text", "Texto de sugerencia", "Текст подсказки ввода", "输入框提示文字", "輸入框提示文字",
                "Testo suggerimento", "Texte indicatif", "Texto de dica",
                "Eingabehinweistext", "نص التلميح", "Текст підказки", "İpucu metni",
                "입력 힌트 텍스트", "入力ヒントテキスト", "Invoerhint-tekst");
        ex("windowBackgroundWhiteValueText",
                "Setting value text", "Texto de valor del ajuste", "Текст значения настройки", "设置值文字", "設定值文字",
                "Testo valore impostazione", "Texte de valeur du paramètre", "Texto do valor da configuração",
                "Einstellungswert-Text", "نص قيمة الإعداد", "Текст значення налаштування", "Ayar değeri metni",
                "설정 값 텍스트", "設定値テキスト", "Instellingswaarde-tekst");
        ex("windowBackgroundWhiteLinkText",
                "Link text", "Texto de enlace", "Текст ссылки", "链接文字", "連結文字",
                "Testo collegamento", "Texte de lien", "Texto de link",
                "Link-Text", "نص الرابط", "Текст посилання", "Bağlantı metni",
                "링크 텍스트", "リンクテキスト", "Linktekst");
        ex("windowBackgroundWhiteRedText",
                "Warning / red text", "Texto rojo de advertencia", "Красный текст предупреждения", "红色警告文字", "紅色警告文字",
                "Testo rosso di avviso", "Texte rouge d'avertissement", "Texto vermelho de aviso",
                "Roter Warntext", "نص أحمر تحذيري", "Червоний текст попередження", "Kırmızı uyarı metni",
                "빨간색 경고 텍스트", "赤色警告テキスト", "Rode waarschuwingstekst");
        ex("windowBackgroundWhiteGreenText",
                "Success / green text", "Texto verde de estado", "Зелёный текст статуса", "绿色状态文字", "綠色狀態文字",
                "Testo verde di stato", "Texte vert d'état", "Texto verde de status",
                "Grüner Statustext", "نص أخضر للحالة", "Зелений текст статусу", "Yeşil durum metni",
                "초록색 상태 텍스트", "緑色ステータステキスト", "Groene statustekst");
        ex("actionBarDefault",
                "Default top bar", "Barra superior predeterminada", "Верхняя панель по умолчанию", "默认顶部栏", "預設頂部列",
                "Barra superiore predefinita", "Barre supérieure par défaut", "Barra superior padrão",
                "Standard-Aktionsleiste", "الشريط العلوي الافتراضي", "Верхня панель за замовчуванням", "Varsayılan üst çubuk",
                "기본 상단 바", "デフォルト上部バー", "Standaard bovenbalk");
        ex("actionBarDefaultIcon",
                "Top bar icons", "Iconos de barra superior", "Иконки верхней панели", "顶部栏图标", "頂部列圖示",
                "Icone barra superiore", "Icônes de barre supérieure", "Ícones da barra superior",
                "Symbole der Aktionsleiste", "أيقونات الشريط العلوي", "Іконки верхньої панелі", "Üst çubuk simgeleri",
                "상단 바 아이콘", "上部バーアイコン", "Bovenbalk-pictogrammen");
        ex("actionBarDefaultTitle",
                "Top bar title", "Título de barra superior", "Заголовок верхней панели", "顶部栏标题", "頂部列標題",
                "Titolo barra superiore", "Titre de barre supérieure", "Título da barra superior",
                "Titel der Aktionsleiste", "عنوان الشريط العلوي", "Заголовок верхньої панелі", "Üst çubuk başlığı",
                "상단 바 제목", "上部バータイトル", "Bovenbalk-titel");
        ex("actionBarDefaultSubtitle",
                "Top bar subtitle", "Subtítulo de barra superior", "Подзаголовок верхней панели", "顶部栏副标题", "頂部列副標題",
                "Sottotitolo barra superiore", "Sous-titre de barre supérieure", "Subtítulo da barra superior",
                "Untertitel der Aktionsleiste", "العنوان الفرعي للشريط العلوي", "Підзаголовок верхньої панелі", "Üst çubuk alt başlığı",
                "상단 바 부제목", "上部バーサブタイトル", "Bovenbalk-ondertitel");
        ex("actionBarDefaultSelector",
                "Top bar tap highlight", "Resaltado al pulsar en barra superior", "Подсветка нажатия верхней панели", "顶部栏点击高亮", "頂部列點擊高亮",
                "Evidenziazione tocco barra superiore", "Surbrillance d'appui barre supérieure", "Destaque ao tocar na barra superior",
                "Tipp-Hervorhebung der Aktionsleiste", "تمييز الضغط في الشريط العلوي", "Підсвітка натискання верхньої панелі", "Üst çubuk dokunma vurgusu",
                "상단 바 터치 강조", "上部バータップ強調", "Bovenbalk-tikaccent");
        ex("actionBarDefaultSearch",
                "Top bar search text", "Texto de búsqueda en barra superior", "Текст поиска в верхней панели", "顶部栏搜索文字", "頂部列搜尋文字",
                "Testo ricerca barra superiore", "Texte de recherche barre supérieure", "Texto de busca na barra superior",
                "Suchtext der Aktionsleiste", "نص البحث في الشريط العلوي", "Текст пошуку у верхній панелі", "Üst çubuk arama metni",
                "상단 바 검색 텍스트", "上部バー検索テキスト", "Bovenbalk-zoektekst");
        ex("actionBarDefaultSearchPlaceholder",
                "Top bar search hint", "Sugerencia de búsqueda en barra superior", "Подсказка поиска в верхней панели", "顶部栏搜索提示", "頂部列搜尋提示",
                "Suggerimento ricerca barra superiore", "Indication de recherche barre supérieure", "Dica de busca na barra superior",
                "Suchhinweis der Aktionsleiste", "تلميح البحث في الشريط العلوي", "Підказка пошуку у верхній панелі", "Üst çubuk arama ipucu",
                "상단 바 검색 힌트", "上部バー検索ヒント", "Bovenbalk-zoekhint");
        ex("switchTrack",
                "Switch track (off)", "Carril del interruptor (apagado)", "Дорожка переключателя (выкл.)", "开关轨道（关闭）", "開關軌道（關閉）",
                "Traccia interruttore (spento)", "Piste de l'interrupteur (désactivé)", "Trilha do interruptor (desligado)",
                "Schalterbahn (aus)", "مسار المفتاح (مطفأ)", "Доріжка перемикача (вимк.)", "Anahtar yolu (kapalı)",
                "스위치 트랙 (꺼짐)", "スイッチトラック（オフ）", "Schakelaarspoor (uit)");
        ex("switchTrackChecked",
                "Switch track (on)", "Carril del interruptor (activado)", "Дорожка переключателя (вкл.)", "开关轨道（开启）", "開關軌道（開啟）",
                "Traccia interruttore (attivo)", "Piste de l'interrupteur (activé)", "Trilha do interruptor (ligado)",
                "Schalterbahn (ein)", "مسار المفتاح (مفعّل)", "Доріжка перемикача (увімк.)", "Anahtar yolu (açık)",
                "스위치 트랙 (켜짐)", "スイッチトラック（オン）", "Schakelaarspoor (aan)");
        ex("switchTrackBlue",
                "Blue switch track (off)", "Carril azul del interruptor (apagado)", "Синяя дорожка переключателя (выкл.)", "蓝色开关轨道（关闭）", "藍色開關軌道（關閉）",
                "Traccia blu interruttore (spento)", "Piste bleue d'interrupteur (désactivé)", "Trilha azul do interruptor (desligado)",
                "Blaue Schalterbahn (aus)", "مسار المفتاح الأزرق (مطفأ)", "Синя доріжка перемикача (вимк.)", "Mavi anahtar yolu (kapalı)",
                "파란 스위치 트랙 (꺼짐)", "青スイッチトラック（オフ）", "Blauw schakelaarspoor (uit)");
        ex("switchTrackBlueChecked",
                "Blue switch track (on)", "Carril azul del interruptor (activado)", "Синяя дорожка переключателя (вкл.)", "蓝色开关轨道（开启）", "藍色開關軌道（開啟）",
                "Traccia blu interruttore (attivo)", "Piste bleue d'interrupteur (activé)", "Trilha azul do interruptor (ligado)",
                "Blaue Schalterbahn (ein)", "مسار المفتاح الأزرق (مفعّل)", "Синя доріжка перемикача (увімк.)", "Mavi anahtar yolu (açık)",
                "파란 스위치 트랙 (켜짐)", "青スイッチトラック（オン）", "Blauw schakelaarspoor (aan)");
        ex("switchTrackBlueThumb",
                "Blue switch knob (off)", "Botón del interruptor azul (apagado)", "Ползунок синего переключателя (выкл.)", "蓝色开关滑块（关闭）", "藍色開關滑塊（關閉）",
                "Cursore interruttore blu (spento)", "Curseur d'interrupteur bleu (désactivé)", "Botão do interruptor azul (desligado)",
                "Blauer Schalterknopf (aus)", "مقبض المفتاح الأزرق (مطفأ)", "Повзунок синього перемикача (вимк.)", "Mavi anahtar düğmesi (kapalı)",
                "파란 스위치 노브 (꺼짐)", "青スイッチつまみ（オフ）", "Blauwe schakelaarknop (uit)");
        ex("switchTrackBlueThumbChecked",
                "Blue switch knob (on)", "Botón del interruptor azul (activado)", "Ползунок синего переключателя (вкл.)", "蓝色开关滑块（开启）", "藍色開關滑塊（開啟）",
                "Cursore interruttore blu (attivo)", "Curseur d'interrupteur bleu (activé)", "Botão do interruptor azul (ligado)",
                "Blauer Schalterknopf (ein)", "مقبض المفتاح الأزرق (مفعّل)", "Повзунок синього перемикача (увімк.)", "Mavi anahtar düğmesi (açık)",
                "파란 스위치 노브 (켜짐)", "青スイッチつまみ（オン）", "Blauwe schakelaarknop (aan)");
        ex("switch2Track",
                "Secondary switch track (off)", "Carril del interruptor 2 (apagado)", "Дорожка переключателя 2 (выкл.)", "次级开关轨道（关闭）", "次級開關軌道（關閉）",
                "Traccia interruttore 2 (spento)", "Piste d'interrupteur 2 (désactivé)", "Trilha do interruptor 2 (desligado)",
                "Schalterbahn 2 (aus)", "مسار المفتاح 2 (مطفأ)", "Доріжка перемикача 2 (вимк.)", "Anahtar yolu 2 (kapalı)",
                "스위치 2 트랙 (꺼짐)", "スイッチ2トラック（オフ）", "Schakelaarspoor 2 (uit)");
        ex("switch2TrackChecked",
                "Secondary switch track (on)", "Carril del interruptor 2 (activado)", "Дорожка переключателя 2 (вкл.)", "次级开关轨道（开启）", "次級開關軌道（開啟）",
                "Traccia interruttore 2 (attivo)", "Piste d'interrupteur 2 (activé)", "Trilha do interruptor 2 (ligado)",
                "Schalterbahn 2 (ein)", "مسار المفتاح 2 (مفعّل)", "Доріжка перемикача 2 (увімк.)", "Anahtar yolu 2 (açık)",
                "스위치 2 트랙 (켜짐)", "スイッチ2トラック（オン）", "Schakelaarspoor 2 (aan)");
        ex("divider",
                "List divider line", "Línea divisora de listas", "Линия-разделитель", "列表分隔线", "列表分隔線",
                "Linea divisoria", "Ligne de séparation", "Linha divisória",
                "Trennlinie", "خط فاصل", "Лінія-роздільник", "Ayırıcı çizgi",
                "구분선", "区切り線", "Scheidingslijn");
        ex("listSelectorSDK21",
                "List item tap ripple", "Efecto al pulsar elemento de lista", "Подсветка нажатия элемента списка", "列表项点击波纹", "列表項點擊波紋",
                "Effetto tocco elemento elenco", "Effet d'appui sur élément de liste", "Efeito ao tocar item da lista",
                "Listenelement-Tippeffekt", "تأثير الضغط على عنصر القائمة", "Підсвітка натискання елемента списку", "Liste öğesi dokunma efekti",
                "목록 항목 터치 효과", "リスト項目のタップ効果", "Tik-effect op lijstitem");
        ex("dialogBackground",
                "Dialog background", "Fondo de cuadro de diálogo", "Фон диалогового окна", "弹窗背景", "彈窗背景",
                "Sfondo finestra di dialogo", "Arrière-plan de boîte de dialogue", "Fundo da caixa de diálogo",
                "Dialog-Hintergrund", "خلفية النافذة المنبثقة", "Фон діалогового вікна", "İletişim kutusu arka planı",
                "대화상자 배경", "ダイアログ背景", "Dialoogachtergrond");
        ex("dialogTextBlack",
                "Dialog primary text", "Texto principal de diálogo", "Основной текст диалога", "弹窗主要文字", "彈窗主要文字",
                "Testo principale dialogo", "Texte principal de dialogue", "Texto principal do diálogo",
                "Dialog-Haupttext", "النص الرئيسي للنافذة المنبثقة", "Основний текст діалогу", "İletişim kutusu ana metni",
                "대화상자 기본 텍스트", "ダイアログのメインテキスト", "Dialoog-hoofdtekst");
        ex("dialogButton",
                "Dialog button text", "Botón de cuadro de diálogo", "Кнопка диалогового окна", "弹窗按钮", "彈窗按鈕",
                "Pulsante finestra di dialogo", "Bouton de dialogue", "Botão do diálogo",
                "Dialog-Schaltfläche", "زر النافذة المنبثقة", "Кнопка діалогу", "İletişim kutusu düğmesi",
                "대화상자 버튼", "ダイアログボタン", "Dialoogknop");
        ex("chat_wallpaper",
                "Chat background wallpaper", "Fondo de pantalla del chat", "Обои фона чата", "聊天背景壁纸", "聊天背景桌布",
                "Sfondo della chat", "Fond d'écran de discussion", "Papel de parede do chat",
                "Chat-Hintergrundbild", "خلفية المحادثة", "Шпалери фону чату", "Sohbet duvar kağıdı",
                "채팅 배경화면", "チャット背景壁紙", "Chat-achtergrond");
        ex("chat_inBubble",
                "Incoming message bubble", "Burbuja de mensaje recibido", "Пузырь входящего сообщения", "接收消息气泡", "接收訊息氣泡",
                "Fumetto messaggio ricevuto", "Bulle de message reçu", "Balão de mensagem recebida",
                "Eingehende Nachrichtenblase", "فقاعة الرسالة الواردة", "Бульбашка вхідного повідомлення", "Gelen mesaj balonu",
                "받은 메시지 말풍선", "受信メッセージ吹き出し", "Inkomende berichtballon");
        ex("chat_outBubble",
                "Outgoing message bubble", "Burbuja de mensaje enviado", "Пузырь исходящего сообщения", "发送消息气泡", "傳送訊息氣泡",
                "Fumetto messaggio inviato", "Bulle de message envoyé", "Balão de mensagem enviada",
                "Ausgehende Nachrichtenblase", "فقاعة الرسالة الصادرة", "Бульбашка вихідного повідомлення", "Giden mesaj balonu",
                "보낸 메시지 말풍선", "送信メッセージ吹き出し", "Uitgaande berichtballon");
        ex("chats_actionBackground",
                "New chat floating button", "Botón flotante de nuevo chat", "Плавающая кнопка нового чата", "新建聊天悬浮按钮", "新增聊天懸浮按鈕",
                "Pulsante fluttuante nuova chat", "Bouton flottant nouveau chat", "Botão flutuante de novo chat",
                "Schwebende Schaltfläche (Neuer Chat)", "الزر العائم لمحادثة جديدة", "Плаваюча кнопка нового чату", "Yeni sohbet yüzen düğmesi",
                "새 채팅 플로팅 버튼", "新規チャット浮遊ボタン", "Zwevende knop nieuwe chat");
        ex("chats_actionIcon",
                "New chat floating button icon", "Icono del botón flotante", "Иконка плавающей кнопки", "悬浮按钮图标", "懸浮按鈕圖示",
                "Icona pulsante fluttuante", "Icône du bouton flottant", "Ícone do botão flutuante",
                "Symbol der schwebenden Schaltfläche", "أيقونة الزر العائم", "Іконка плаваючої кнопки", "Yüzen düğme simgesi",
                "플로팅 버튼 아이콘", "浮遊ボタンアイコン", "Pictogram zwevende knop");
        ex("featuredStickers_addButton",
                "Primary action button", "Botón de acción principal", "Основная кнопка действия", "主要操作按钮", "主要操作按鈕",
                "Pulsante azione principale", "Bouton d'action principal", "Botão de ação principal",
                "Hauptaktions-Schaltfläche", "زر الإجراء الرئيسي", "Основна кнопка дії", "Ana işlem düğmesi",
                "기본 작업 버튼", "メインアクションボタン", "Hoofdactieknop");
        ex("featuredStickers_buttonText",
                "Primary action button text", "Texto del botón principal", "Текст основной кнопки", "主要按钮文字", "主要按鈕文字",
                "Testo pulsante principale", "Texte du bouton principal", "Texto do botão principal",
                "Text der Hauptschaltfläche", "نص الزر الرئيسي", "Текст основної кнопки", "Ana düğme metni",
                "기본 버튼 텍스트", "メインボタンテキスト", "Hoofdknoptekst");

        // Comprehensive token dictionary covering all 341 tokens across ThemeColors.java
        tk("chat", "Chat", "Chat", "Чат", "聊天", "聊天", "Chat", "Chat", "Chat", "Chat", "المحادثة", "Чат", "Sohbet", "채팅", "チャット", "Chat");
        tk("chats", "Chat list", "Lista de chats", "Список чатов", "聊天列表", "聊天列表", "Elenco chat", "Liste des chats", "Lista de chats", "Chatliste", "قائمة المحادثات", "Список чатів", "Sohbet listesi", "채팅 목록", "チャット一覧", "Chatlijst");
        tk("dialog", "Dialog", "Diálogo", "Диалог", "弹窗", "彈窗", "Dialogo", "Dialogue", "Diálogo", "Dialog", "حوار", "Діалог", "İletişim kutusu", "대화상자", "ダイアログ", "Dialoog");
        tk("dialogs", "Dialogs", "Diálogos", "Диалоги", "对话", "對話", "Dialoghi", "Dialogues", "Diálogos", "Dialoge", "محادثات", "Діалоги", "Diyaloglar", "대화", "ダイアログ", "Dialogen");
        tk("window", "Window", "Ventana", "Окно", "窗口", "視窗", "Finestra", "Fenêtre", "Janela", "Fenster", "نافذة", "Вікно", "Pencere", "창", "ウィンドウ", "Venster");
        tk("action", "Action", "Acción", "Действие", "操作", "操作", "Azione", "Action", "Ação", "Aktion", "إجراء", "Дія", "İşlem", "작업", "アクション", "Actie");
        tk("bar", "bar", "barra", "панель", "栏", "列", "barra", "barre", "barra", "Leiste", "شريط", "панель", "çubuk", "바", "バー", "balk");
        tk("background", "background", "fondo", "фон", "背景", "背景", "sfondo", "arrière-plan", "fundo", "Hintergrund", "خلفية", "фон", "arka plan", "배경", "背景", "achtergrond");
        tk("text", "text", "texto", "текст", "文字", "文字", "testo", "texte", "texto", "Text", "نص", "текст", "metin", "텍스트", "テキスト", "tekst");
        tk("button", "button", "botón", "кнопка", "按钮", "按鈕", "pulsante", "bouton", "botão", "Schaltfläche", "زر", "кнопка", "düğme", "버튼", "ボタン", "knop");
        tk("icon", "icon", "icono", "иконка", "图标", "圖示", "icona", "icône", "ícone", "Symbol", "أيقونة", "іконка", "simge", "아이콘", "アイコン", "pictogram");
        tk("icons", "icons", "iconos", "иконки", "图标", "圖示", "icone", "icônes", "ícones", "Symbole", "أيقونات", "іконки", "simgeler", "아이콘", "アイコン", "pictogrammen");
        tk("in", "incoming", "entrante", "входящий", "接收", "接收", "in entrata", "entrant", "recebido", "eingehend", "وارد", "вхідний", "gelen", "수신", "受信", "inkomend");
        tk("out", "outgoing", "saliente", "исходящий", "发送", "傳送", "in uscita", "sortant", "enviado", "ausgehend", "صادر", "вихідний", "giden", "발신", "送信", "uitgaand");
        tk("selected", "selected", "seleccionado", "выбранный", "选中", "選取", "selezionato", "sélectionné", "selecionado", "ausgewählt", "محدد", "вибраний", "seçili", "선택됨", "選択中", "geselecteerd");
        tk("unselected", "unselected", "no seleccionado", "не выбранный", "未选中", "未選取", "non selezionato", "non sélectionné", "não selecionado", "nicht ausgewählt", "غير محدد", "не вибраний", "seçili değil", "미선택", "未選択", "niet geselecteerd");
        tk("voipgroup", "Voice chat", "Chat de voz", "Голосовой чат", "语音聊天", "語音聊天", "Chat vocale", "Chat vocal", "Chat de voz", "Sprachchat", "محادثة صوتية", "Голосовий чат", "Sesli sohbet", "음성 채팅", "ボイスチャット", "Spraakchat");
        tk("panel", "panel", "panel", "панель", "面板", "面板", "pannello", "panneau", "painel", "Panel", "لوحة", "панель", "panel", "패널", "パネル", "paneel");
        tk("message", "message", "mensaje", "сообщение", "消息", "訊息", "messaggio", "message", "mensagem", "Nachricht", "رسالة", "повідомлення", "mesaj", "메시지", "メッセージ", "bericht");
        tk("messages", "messages", "mensajes", "сообщения", "消息", "訊息", "messaggi", "messages", "mensagens", "Nachrichten", "رسائل", "повідомлення", "mesajlar", "메시지", "メッセージ", "berichten");
        tk("msg", "message", "mensaje", "сообщение", "消息", "訊息", "messaggio", "message", "mensagem", "Nachricht", "رسالة", "повідомлення", "mesaj", "메시지", "メッセージ", "bericht");
        tk("compact", "compact", "compacto", "компактный", "紧凑", "精簡", "compatto", "compact", "compacto", "kompakt", "مدمج", "компактний", "kompakt", "컴팩트", "コンパクト", "compact");
        tk("name", "name", "nombre", "имя", "名称", "名稱", "nome", "nom", "nome", "Name", "اسم", "ім'я", "ad", "이름", "名前", "naam");
        tk("default", "default", "predeterminado", "по умолчанию", "默认", "預設", "predefinito", "par défaut", "padrão", "Standard", "افتراضي", "за замовчуванням", "varsayılan", "기본", "デフォルト", "standaard");
        tk("progress", "progress", "progreso", "прогресс", "进度", "進度", "avanzamento", "progression", "progresso", "Fortschritt", "تقدم", "прогрес", "ilerleme", "진행", "進捗", "voortgang");
        tk("gradient", "gradient", "degradado", "градиент", "渐变", "漸層", "sfumatura", "dégradé", "gradiente", "Verlauf", "تدرج", "градієнт", "gradyan", "그라데이션", "グラデーション", "verloop");
        tk("iv", "Instant View", "Vista rápida", "Быстрый просмотр", "即时预览", "即時預覽", "Vista rapida", "Aperçu instantané", "Visualização rápida", "Instant View", "العرض الفوري", "Швидкий перегляд", "Anında Görünüm", "인스턴트 뷰", "インスタントビュー", "Directe weergave");
        tk("blue", "blue", "azul", "синий", "蓝色", "藍色", "blu", "bleu", "azul", "blau", "أزرق", "синій", "mavi", "파란색", "青", "blauw");
        tk("white", "white", "blanco", "белый", "白色", "白色", "bianco", "blanc", "branco", "weiß", "أبيض", "білий", "beyaz", "흰색", "白", "wit");
        tk("gray", "gray", "gris", "серый", "灰色", "灰色", "grigio", "gris", "cinza", "grau", "رمادي", "сірий", "gri", "회색", "グレー", "grijs");
        tk("black", "black", "negro", "чёрный", "黑色", "黑色", "nero", "noir", "preto", "schwarz", "أسود", "чорний", "siyah", "검정", "黒", "zwart");
        tk("red", "red", "rojo", "красный", "红色", "紅色", "rosso", "rouge", "vermelho", "rot", "أحمر", "червоний", "kırmızı", "빨간색", "赤", "rood");
        tk("green", "green", "verde", "зелёный", "绿色", "綠色", "verde", "vert", "verde", "grün", "أخضر", "зелений", "yeşil", "초록색", "緑", "groen");
        tk("orange", "orange", "naranja", "оранжевый", "橙色", "橙色", "arancione", "orange", "laranja", "orange", "برتقالي", "помаранчевий", "turuncu", "주황색", "オレンジ", "oranje");
        tk("yellow", "yellow", "amarillo", "жёлтый", "黄色", "黃色", "giallo", "jaune", "amarelo", "gelb", "أصفر", "жовтий", "sarı", "노란색", "黄", "geel");
        tk("cyan", "cyan", "cian", "голубой", "青色", "青色", "ciano", "cyan", "ciano", "cyan", "سماوي", "блакитний", "camgöbeği", "청록색", "シアン", "cyaan");
        tk("pink", "pink", "rosa", "розовый", "粉色", "粉色", "rosa", "rose", "rosa", "rosa", "وردي", "рожевий", "pembe", "분홍색", "ピンク", "roze");
        tk("violet", "violet", "violeta", "фиолетовый", "紫罗兰色", "紫羅蘭色", "viola", "violet", "violeta", "violett", "بنفسجي", "фіолетовий", "mor", "보라색", "バイオレット", "violet");
        tk("purple", "purple", "púrpura", "пурпурный", "紫色", "紫色", "viola", "pourpre", "roxo", "lila", "أرجواني", "пурпуровий", "eflatun", "자주색", "紫", "paars");
        tk("indigo", "indigo", "índigo", "индиго", "靛蓝", "靛藍", "indaco", "indigo", "índigo", "indigo", "نيلي", "індиго", "çivit", "남색", "インディゴ", "indigo");
        tk("golden", "golden", "dorado", "золотой", "金色", "金色", "dorato", "doré", "dourado", "golden", "ذهبي", "золотий", "altın", "금색", "ゴールド", "goud");
        tk("lightblue", "light blue", "azul claro", "светло-синий", "浅蓝", "淺藍", "azzurro", "bleu clair", "azul claro", "hellblau", "أزرق فاتح", "світло-синій", "açık mavi", "연파랑", "水色", "lichtblauw");
        tk("lightgreen", "light green", "verde claro", "светло-зелёный", "浅绿", "淺綠", "verde chiaro", "vert clair", "verde claro", "hellgrün", "أخضر فاتح", "світло-зелений", "açık yeşil", "연초록", "黄緑", "lichtgroen");
        tk("dark", "dark", "oscuro", "тёмный", "深色", "深色", "scuro", "sombre", "escuro", "dunkel", "داكن", "темний", "koyu", "어두운", "ダーク", "donker");
        tk("darker", "darker", "más oscuro", "темнее", "更深", "更深", "più scuro", "plus sombre", "mais escuro", "dunkler", "أكثر دكنة", "темніший", "daha koyu", "더 어두운", "より暗い", "donkerder");
        tk("light", "light", "claro", "светлый", "浅色", "淺色", "chiaro", "clair", "claro", "hell", "فاتح", "світлий", "açık", "밝은", "ライト", "licht");
        tk("line", "line", "línea", "линия", "线条", "線條", "linea", "ligne", "linha", "Linie", "خط", "лінія", "çizgi", "선", "ライン", "lijn");
        tk("lines", "lines", "líneas", "линии", "线条", "線條", "linee", "lignes", "linhas", "Linien", "خطوط", "лінії", "çizgiler", "선", "ライン", "lijnen");
        tk("avatar", "avatar", "avatar", "аватар", "头像", "頭像", "avatar", "avatar", "avatar", "Avatar", "صورة رمزية", "аватар", "avatar", "아바타", "アバター", "avatar");
        tk("location", "location", "ubicación", "геолокация", "位置", "位置", "posizione", "localisation", "localização", "Standort", "الموقع", "геолокація", "konum", "위치", "位置情報", "locatie");
        tk("media", "media", "multimedia", "медиа", "媒体", "媒體", "media", "média", "mídia", "Medien", "وسائط", "медіа", "medya", "미디어", "メディア", "media");
        tk("audio", "audio", "audio", "аудио", "音频", "音訊", "audio", "audio", "áudio", "Audio", "صوت", "аудіо", "ses", "오디오", "オーディオ", "audio");
        tk("chart", "chart", "gráfico", "график", "图表", "圖表", "grafico", "graphique", "gráfico", "Diagramm", "مخطط", "графік", "grafik", "차트", "グラフ", "grafiek");
        tk("check", "check", "marca", "галочка", "勾选", "勾選", "spunta", "coche", "marca", "Häkchen", "علامة", "галочка", "onay", "체크", "チェック", "vinkje");
        tk("checked", "checked", "marcado", "отмечено", "已选中", "已選取", "selezionato", "coché", "marcado", "aktiviert", "محدد", "позначено", "işaretli", "체크됨", "オン", "aangevinkt");
        tk("unchecked", "unchecked", "sin marcar", "не отмечено", "未选中", "未選取", "non selezionato", "décoché", "desmarcado", "deaktiviert", "غير محدد", "не позначено", "işaretsiz", "미체크", "オフ", "uitgevinkt");
        tk("reply", "reply", "respuesta", "ответ", "回复", "回覆", "risposta", "réponse", "resposta", "Antwort", "رد", "відповідь", "yanıt", "답장", "返信", "antwoord");
        tk("menu", "menu", "menú", "меню", "菜单", "選單", "menu", "menu", "menu", "Menü", "قائمة", "меню", "menü", "메뉴", "メニュー", "menu");
        tk("submenu", "submenu", "submenú", "подменю", "子菜单", "子選單", "sottomenu", "sous-menu", "submenu", "Untermenü", "قائمة فرعية", "підменю", "alt menü", "하위 메뉴", "サブメニュー", "submenu");
        tk("statistic", "Statistics", "Estadísticas", "Статистика", "统计", "統計", "Statistiche", "Statistiques", "Estatísticas", "Statistik", "الإحصائيات", "Статистика", "İstatistikler", "통계", "統計", "Statistieken");
        tk("voice", "voice", "voz", "голос", "语音", "語音", "voce", "voix", "voz", "Sprache", "صوت", "голос", "ses", "음성", "音声", "spraak");
        tk("premium", "Premium", "Premium", "Premium", "Premium", "Premium", "Premium", "Premium", "Premium", "Premium", "بريميوم", "Premium", "Premium", "프리미엄", "プレミアム", "Premium");
        tk("selector", "selector", "selector", "выделение", "选择器", "選擇器", "selettore", "sélecteur", "seletor", "Auswahl", "محدد", "виділення", "seçici", "선택기", "セレクタ", "selector");
        tk("color", "color", "color", "цвет", "颜色", "顏色", "colore", "couleur", "cor", "Farbe", "لون", "колір", "renk", "색상", "色", "kleur");
        tk("attach", "Attachment", "Adjunto", "Вложение", "附件", "附件", "Allegato", "Pièce jointe", "Anexo", "Anhang", "مرفق", "Вкладення", "Ek", "첨부", "添付", "Bijlage");
        tk("emoji", "emoji", "emoji", "эмодзи", "表情", "表情", "emoji", "émoji", "emoji", "Emoji", "إيموجي", "емодзі", "emoji", "이모지", "絵文字", "emoji");
        tk("overlay", "overlay", "superposición", "наложение", "遮罩", "遮罩", "sovrapposizione", "superposition", "sobreposição", "Überlagerung", "تراكب", "накладення", "kaplama", "오버레이", "オーバーレイ", "overlay");
        tk("pressed", "pressed", "pulsado", "нажато", "按下", "按下", "premuto", "appuyé", "pressionado", "gedrückt", "مضغوط", "натиснуто", "basılı", "눌림", "押下時", "ingedrukt");
        tk("reaction", "reaction", "reacción", "реакция", "回应", "回應", "reazione", "réaction", "reação", "Reaktion", "تفاعل", "реакція", "tepki", "반응", "リアクション", "reactie");
        tk("sent", "sent", "enviado", "отправлено", "已发送", "已傳送", "inviato", "envoyé", "enviado", "gesendet", "مرسل", "надіслано", "gönderildi", "전송됨", "送信済み", "verzonden");
        tk("top", "top", "superior", "верхний", "顶部", "頂部", "superiore", "haut", "topo", "oben", "علوي", "верхній", "üst", "상단", "上部", "boven");
        tk("bottom", "bottom", "inferior", "нижний", "底部", "底部", "inferiore", "bas", "inferior", "unten", "سفلي", "нижній", "alt", "하단", "下部", "onder");
        tk("player", "Player", "Reproductor", "Плеер", "播放器", "播放器", "Lettore", "Lecteur", "Player", "Player", "المشغّل", "Плеєр", "Oynatıcı", "플레이어", "プレーヤー", "Speler");
        tk("inapp", "In-app", "En la app", "В приложении", "应用内", "應用內", "Nell'app", "Intégré", "No app", "In-App", "داخل التطبيق", "У застосунку", "Uygulama içi", "앱 내", "アプリ内", "In-app");
        tk("tab", "tab", "pestaña", "вкладка", "标签页", "分頁", "scheda", "onglet", "aba", "Tab", "تبويب", "вкладка", "sekme", "탭", "タブ", "tabblad");
        tk("tabs", "tabs", "pestañas", "вкладки", "标签页", "分頁", "schede", "onglets", "abas", "Tabs", "تبويبات", "вкладки", "sekmeler", "탭", "タブ", "tabbladen");
        tk("active", "active", "activo", "активный", "激活", "啟用", "attivo", "actif", "ativo", "aktiv", "نشط", "активний", "aktif", "활성", "アクティブ", "actief");
        tk("inactive", "inactive", "inactivo", "неактивный", "未激活", "未啟用", "inattivo", "inactif", "inativo", "inaktiv", "غير نشط", "неактивний", "pasif", "비활성", "非アクティブ", "inactief");
        tk("unactive", "inactive", "inactivo", "неактивный", "未激活", "未啟用", "inattivo", "inactif", "inativo", "inaktiv", "غير نشط", "неактивний", "pasif", "비활성", "非アクティブ", "inactief");
        tk("activated", "activated", "activado", "активировано", "已激活", "已啟用", "attivato", "activé", "ativado", "aktiviert", "مفعّل", "активовано", "etkin", "활성화됨", "有効化", "geactiveerd");
        tk("bubble", "bubble", "burbuja", "пузырь", "气泡", "氣泡", "fumetto", "bulle", "balão", "Blase", "فقاعة", "бульбашка", "balon", "말풍선", "吹き出し", "ballon");
        tk("code", "code", "código", "код", "代码", "程式碼", "codice", "code", "código", "Code", "كود", "код", "kod", "코드", "コード", "code");
        tk("file", "file", "archivo", "файл", "文件", "檔案", "file", "fichier", "arquivo", "Datei", "ملف", "файл", "dosya", "파일", "ファイル", "bestand");
        tk("files", "files", "archivos", "файлы", "文件", "檔案", "file", "fichiers", "arquivos", "Dateien", "ملفات", "файли", "dosyalar", "파일", "ファイル", "bestanden");
        tk("muted", "muted", "silenciado", "без звука", "已静音", "已靜音", "silenziato", "en sourdine", "silenciado", "stumm", "مكتوم", "без звуку", "sessiz", "음소거됨", "ミュート", "gedempt");
        tk("mute", "mute", "silenciar", "выкл. звук", "静音", "靜音", "silenzia", "couper le son", "silenciar", "stumm", "كتم", "вимк. звук", "sessize al", "음소거", "ミュート", "dempen");
        tk("unmute", "unmute", "activar sonido", "вкл. звук", "取消静音", "取消靜音", "riattiva audio", "réactiver le son", "ativar som", "Ton ein", "إلغاء الكتم", "увімк. звук", "sesi aç", "음소거 해제", "ミュート解除", "dempen opheffen");
        tk("unmuted", "unmuted", "con sonido", "со звуком", "未静音", "未靜音", "con audio", "son activé", "com som", "mit Ton", "غير مكتوم", "зі звуком", "sesli", "소리 켜짐", "ミュートなし", "niet gedempt");
        tk("search", "search", "búsqueda", "поиск", "搜索", "搜尋", "ricerca", "recherche", "busca", "Suche", "بحث", "пошук", "arama", "검색", "検索", "zoeken");
        tk("bot", "bot", "bot", "бот", "机器人", "機器人", "bot", "bot", "bot", "Bot", "بوت", "бот", "bot", "봇", "ボット", "bot");
        tk("profile", "Profile", "Perfil", "Профиль", "个人资料", "個人資料", "Profilo", "Profil", "Perfil", "Profil", "الملف الشخصي", "Профіль", "Profil", "프로필", "プロフィール", "Profiel");
        tk("seekbar", "seekbar", "barra de progreso", "полоса перемотки", "进度条", "進度條", "barra avanzamento", "barre de lecture", "barra de progresso", "Fortschrittsleiste", "شريط التمرير", "смуга перемотування", "arama çubuğu", "탐색 바", "シークバー", "zoekbalk");
        tk("admin", "admin", "administrador", "админ", "管理员", "管理員", "amministratore", "admin", "admin", "Admin", "مشرف", "адмін", "yönetici", "관리자", "管理者", "beheerder");
        tk("contact", "contact", "contacto", "контакт", "联系人", "聯絡人", "contatto", "contact", "contato", "Kontakt", "جهة اتصال", "контакт", "kişi", "연락처", "連絡先", "contact");
        tk("contacts", "Contacts", "Contactos", "Контакты", "联系人", "聯絡人", "Contatti", "Contacts", "Contatos", "Kontakte", "جهات الاتصال", "Контакти", "Kişiler", "연락처", "連絡先", "Contacten");
        tk("link", "link", "enlace", "ссылка", "链接", "連結", "collegamento", "lien", "link", "Link", "رابط", "посилання", "bağlantı", "링크", "リンク", "link");
        tk("archived", "archived", "archivado", "в архиве", "已归档", "已封存", "archiviato", "archivé", "arquivado", "archiviert", "مؤرشف", "в архіві", "arşivlenmiş", "보관됨", "アーカイブ済み", "gearchiveerd");
        tk("archive", "archive", "archivo", "архив", "归档", "封存", "archivio", "archive", "arquivo", "Archiv", "أرشيف", "архів", "arşiv", "보관함", "アーカイブ", "archief");
        tk("glass", "Glass", "Cristal", "Стекло", "毛玻璃", "毛玻璃", "Vetro", "Verre", "Vidro", "Glas", "زجاج", "Скло", "Cam", "글래스", "ガラス", "Glas");
        tk("shadow", "shadow", "sombra", "тень", "阴影", "陰影", "ombra", "ombre", "sombra", "Schatten", "ظل", "тінь", "gölge", "그림자", "影", "schaduw");
        tk("checkbox", "checkbox", "casilla", "флажок", "复选框", "核取方塊", "casella", "case à cocher", "caixa de seleção", "Kontrollkästchen", "مربع اختيار", "прапорець", "onay kutusu", "체크박스", "チェックボックス", "selectievakje");
        tk("inline", "inline", "integrado", "встроенный", "内联", "內嵌", "in linea", "intégré", "integrado", "Inline", "مضمن", "вбудований", "satır içi", "인라인", "インライン", "inline");
        tk("stickers", "Stickers", "Stickers", "Стикеры", "贴纸", "貼圖", "Sticker", "Autocollants", "Stickers", "Sticker", "ملصقات", "Стікери", "Çıkartmalar", "스티커", "スタンプ", "Stickers");
        tk("sticker", "sticker", "sticker", "стикер", "贴纸", "貼圖", "sticker", "autocollant", "sticker", "Sticker", "ملصق", "стікер", "çıkartma", "스티커", "スタンプ", "sticker");
        tk("switch", "switch", "interruptor", "переключатель", "开关", "開關", "interruttore", "interrupteur", "interruptor", "Schalter", "مفتاح", "перемикач", "anahtar", "스위치", "スイッチ", "schakelaar");
        tk("track", "track", "carril", "дорожка", "轨道", "軌道", "traccia", "piste", "trilha", "Bahn", "مسار", "доріжка", "yol", "트랙", "トラック", "spoor");
        tk("time", "time", "hora", "время", "时间", "時間", "ora", "heure", "hora", "Zeit", "الوقت", "час", "saat", "시간", "時刻", "tijd");
        tk("unread", "unread", "no leído", "непрочитано", "未读", "未讀", "non letto", "non lu", "não lido", "ungelesen", "غير مقروء", "непрочитано", "okunmamış", "읽지 않음", "未読", "ongelezen");
        tk("article", "article", "artículo", "статья", "文章", "文章", "articolo", "article", "artigo", "Artikel", "مقال", "стаття", "makale", "기사", "記事", "artikel");
        tk("circle", "circle", "círculo", "круг", "圆形", "圓形", "cerchio", "cercle", "círculo", "Kreis", "دائرة", "коло", "daire", "원", "円", "cirkel");
        tk("info", "info", "información", "инфо", "信息", "資訊", "info", "info", "info", "Info", "معلومات", "інфо", "bilgi", "정보", "情報", "info");
        tk("hint", "hint", "sugerencia", "подсказка", "提示", "提示", "suggerimento", "indice", "dica", "Hinweis", "تلميح", "підказка", "ipucu", "힌트", "ヒント", "hint");
        tk("key", "key", "tecla", "клавиша", "按键", "按鍵", "tasto", "touche", "tecla", "Taste", "مفتاح", "клавіша", "tuş", "키", "キー", "toets");
        tk("loader", "loader", "cargador", "индикатор загрузки", "加载器", "載入器", "caricamento", "chargeur", "carregador", "Ladeanzeige", "مؤشر تحميل", "індикатор завантаження", "yükleyici", "로더", "ローダー", "lader");
        tk("placeholder", "placeholder", "marcador", "заполнитель", "占位符", "佔位符", "segnaposto", "espace réservé", "marcador", "Platzhalter", "نص مؤقت", "заповнювач", "yer tutucu", "플레이스홀더", "プレースホルダー", "plaatsaanduiding");
        tk("title", "title", "título", "заголовок", "标题", "標題", "titolo", "titre", "título", "Titel", "عنوان", "заголовок", "başlık", "제목", "タイトル", "titel");
        tk("subtitle", "subtitle", "subtítulo", "подзаголовок", "副标题", "副標題", "sottotitolo", "sous-titre", "subtítulo", "Untertitel", "عنوان فرعي", "підзаголовок", "alt başlık", "부제목", "サブタイトル", "ondertitel");
        tk("call", "call", "llamada", "звонок", "通话", "通話", "chiamata", "appel", "chamada", "Anruf", "مكالمة", "дзвінок", "arama", "통화", "通話", "oproep");
        tk("calls", "Calls", "Llamadas", "Звонки", "通话", "通話", "Chiamate", "Appels", "Chamadas", "Anrufe", "المكالمات", "Дзвінки", "Aramalar", "통화", "通話", "Oproepen");
        tk("context", "context", "contextual", "контекст", "上下文", "上下文", "contesto", "contexte", "contexto", "Kontext", "سياق", "контекст", "bağlam", "컨텍스트", "コンテキスト", "context");
        tk("disabled", "disabled", "desactivado", "отключено", "禁用", "停用", "disattivato", "désactivé", "desativado", "deaktiviert", "معطّل", "вимкнено", "devre dışı", "비활성화", "無効", "uitgeschakeld");
        tk("enabled", "enabled", "habilitado", "включено", "启用", "啟用", "abilitato", "activé", "habilitado", "aktiviert", "مفعّل", "увімкнено", "etkin", "활성화", "有効", "ingeschakeld");
        tk("featured", "Featured", "Destacados", "Популярные", "精选", "精選", "In evidenza", "En vedette", "Destaques", "Empfohlen", "مميز", "Популярні", "Öne çıkan", "추천", "注目", "Uitgelicht");
        tk("input", "input", "entrada", "ввод", "输入", "輸入", "inserimento", "saisie", "entrada", "Eingabe", "إدخال", "введення", "giriş", "입력", "入力", "invoer");
        tk("field", "field", "campo", "поле", "栏位", "欄位", "campo", "champ", "campo", "Feld", "حقل", "поле", "alan", "필드", "フィールド", "veld");
        tk("service", "service", "servicio", "служебный", "系统服务", "系統服務", "servizio", "service", "serviço", "Dienst", "خدمة", "службовий", "hizmet", "서비스", "サービス", "service");
        tk("square", "square", "cuadrado", "квадратный", "方形", "方形", "quadrato", "carré", "quadrado", "quadratisch", "مربع", "квадратний", "kare", "사각형", "四角", "vierkant");
        tk("stories", "Stories", "Historias", "Истории", "动态", "限時動態", "Storie", "Stories", "Stories", "Stories", "القصص", "Історії", "Hikayeler", "스토리", "ストーリー", "Verhalen");
        tk("counter", "counter", "contador", "счётчик", "计数器", "計數器", "contatore", "compteur", "contador", "Zähler", "عداد", "лічильник", "sayaç", "카운터", "カウンター", "teller");
        tk("danger", "danger", "peligro", "опасность", "危险", "危險", "pericolo", "danger", "perigo", "Gefahr", "خطر", "небезпека", "tehlike", "위험", "危険", "gevaar");
        tk("down", "down", "abajo", "вниз", "向下", "向下", "giù", "bas", "baixo", "unten", "أسفل", "вниз", "aşağı", "아래", "下", "omlaag");
        tk("up", "up", "arriba", "вверх", "向上", "向上", "su", "haut", "cima", "oben", "أعلى", "вгору", "yukarı", "위", "上", "omhoog");
        tk("duration", "duration", "duración", "длительность", "时长", "時長", "durata", "durée", "duração", "Dauer", "المدة", "тривалість", "süre", "길이", "再生時間", "duur");
        tk("empty", "empty", "vacío", "пустой", "空", "空", "vuoto", "vide", "vazio", "leer", "فارغ", "порожній", "boş", "비어 있음", "空", "leeg");
        tk("fill", "fill", "relleno", "заливка", "填充", "填滿", "riempimento", "remplissage", "preenchimento", "Füllung", "تعبئة", "заливка", "dolgu", "채우기", "塗りつぶし", "vulling");
        tk("groupcreate", "Create group", "Crear grupo", "Создание группы", "创建群组", "建立群組", "Crea gruppo", "Créer un groupe", "Criar grupo", "Gruppe erstellen", "إنشاء مجموعة", "Створення групи", "Grup oluştur", "그룹 만들기", "グループ作成", "Groep maken");
        tk("live", "live", "en vivo", "эфир", "实时", "即時", "live", "en direct", "ao vivo", "live", "مباشر", "ефір", "canlı", "실시간", "ライブ", "live");
        tk("mode", "mode", "modo", "режим", "模式", "模式", "modalità", "mode", "modo", "Modus", "وضع", "режим", "mod", "모드", "モード", "modus");
        tk("photo", "photo", "foto", "фото", "照片", "照片", "foto", "photo", "foto", "Foto", "صورة", "фото", "fotoğraf", "사진", "写真", "foto");
        tk("primary", "primary", "primario", "основной", "主要", "主要", "primario", "principal", "primário", "primär", "رئيسي", "основний", "birincil", "기본", "メイン", "primair");
        tk("secondary", "secondary", "secundario", "вторичный", "次要", "次要", "secondario", "secondaire", "secundário", "sekundär", "ثانوي", "вторинний", "ikincil", "보조", "サブ", "secundair");
        tk("send", "send", "enviar", "отправить", "发送", "傳送", "invia", "envoyer", "enviar", "senden", "إرسال", "надіслати", "gönder", "전송", "送信", "verzenden");
        tk("success", "success", "éxito", "успех", "成功", "成功", "successo", "succès", "sucesso", "Erfolg", "نجاح", "успіх", "başarılı", "성공", "成功", "succes");
        tk("alert", "alert", "alerta", "предупреждение", "警告", "警告", "avviso", "alerte", "alerta", "Warnung", "تنبيه", "попередження", "uyarı", "경고", "警告", "waarschuwing");
        tk("clock", "clock", "reloj", "часы", "时钟", "時鐘", "orologio", "horloge", "relógio", "Uhr", "ساعة", "годинник", "saat", "시계", "時計", "klok");
        tk("inner", "inner", "interior", "внутренний", "内部", "內部", "interno", "interne", "interno", "innen", "داخلي", "внутрішній", "iç", "내부", "内側", "binnen");
        tk("outer", "outer", "exterior", "внешний", "外部", "外部", "esterno", "externe", "externo", "außen", "خارجي", "зовнішній", "dış", "외부", "外側", "buiten");
        tk("instant", "instant", "instantáneo", "мгновенный", "即时", "即時", "istantaneo", "instantané", "instantâneo", "sofortig", "فوري", "миттєвий", "anlık", "즉시", "インスタント", "direct");
        tk("keyboard", "keyboard", "teclado", "клавиатура", "键盘", "鍵盤", "tastiera", "clavier", "teclado", "Tastatur", "لوحة المفاتيح", "клавіатура", "klavye", "키보드", "キーボード", "toetsenbord");
        tk("phone", "phone", "teléfono", "телефон", "电话", "電話", "telefono", "téléphone", "telefone", "Telefon", "هاتف", "телефон", "telefon", "전화", "電話", "telefoon");
        tk("picker", "picker", "selector", "выбор", "选择器", "選擇器", "selettore", "sélecteur", "seletor", "Auswahl", "منتقي", "вибір", "seçici", "선택기", "ピッカー", "kiezer");
        tk("poll", "poll", "encuesta", "опрос", "投票", "投票", "sondaggio", "sondage", "enquete", "Umfrage", "استطلاع", "опитування", "anket", "설문", "アンケート", "peiling");
        tk("preview", "preview", "vista previa", "превью", "预览", "預覽", "anteprima", "aperçu", "prévia", "Vorschau", "معاينة", "перегляд", "önizleme", "미리보기", "プレビュー", "voorbeeld");
        tk("recorded", "recorded", "grabado", "записанный", "已录制", "已錄製", "registrato", "enregistré", "gravado", "aufgenommen", "مسجل", "записаний", "kaydedilmiş", "녹음됨", "録音済み", "opgenomen");
        tk("record", "record", "grabar", "запись", "录制", "錄製", "registra", "enregistrer", "gravar", "aufnehmen", "تسجيل", "запис", "kayıt", "녹음", "録音", "opnemen");
        tk("scroll", "scroll", "desplazamiento", "прокрутка", "滚动", "捲動", "scorrimento", "défilement", "rolagem", "Scrollen", "تمرير", "прокрутка", "kaydırma", "스크롤", "スクロール", "scroll");
        tk("scrollbar", "scrollbar", "barra de desplazamiento", "полоса прокрутки", "滚动条", "捲動軸", "barra scorrimento", "barre de défilement", "barra de rolagem", "Bildlaufleiste", "شريط التمرير", "смуга прокрутки", "kaydırma çubuğu", "스크롤바", "スクロールバー", "schuifbalk");
        tk("scrolled", "scrolled", "desplazado", "при прокрутке", "滚动时", "捲動時", "scorso", "défilé", "rolado", "gescrollt", "عند التمرير", "при прокрутці", "kaydırılmış", "스크롤됨", "スクロール時", "gescrold");
        tk("unscrolled", "unscrolled", "sin desplazar", "без прокрутки", "未滚动", "未捲動", "non scorso", "non défilé", "não rolado", "ungescrollt", "بدون تمرير", "без прокрутки", "kaydırılmamış", "미스크롤", "未スクロール", "niet gescrold");
        tk("selection", "selection", "selección", "выделение", "选择", "選取", "selezione", "sélection", "seleção", "Auswahl", "تحديد", "виділення", "seçim", "선택", "選択", "selectie");
        tk("select", "select", "seleccionar", "выбрать", "选择", "選擇", "seleziona", "sélectionner", "selecionar", "auswählen", "تحديد", "вибрати", "seç", "선택", "選択", "selecteren");
        tk("sound", "sound", "sonido", "звук", "声音", "聲音", "suono", "son", "som", "Ton", "صوت", "звук", "ses", "소리", "サウンド", "geluid");
        tk("start", "start", "inicio", "старт", "开始", "開始", "avvio", "début", "início", "Start", "بدء", "старт", "başlat", "시작", "開始", "start");
        tk("stop", "stop", "detener", "стоп", "停止", "停止", "ferma", "arrêter", "parar", "Stopp", "إيقاف", "стоп", "durdur", "정지", "停止", "stop");
        tk("table", "table", "tabla", "таблица", "表格", "表格", "tabella", "tableau", "tabela", "Tabelle", "جدول", "таблиця", "tablo", "표", "テーブル", "tabel");
        tk("wallpaper", "wallpaper", "fondo de pantalla", "обои", "壁纸", "桌布", "sfondo", "fond d'écran", "papel de parede", "Hintergrundbild", "خلفية", "шпалери", "duvar kağıdı", "배경화면", "壁紙", "achtergrond");
        tk("accent", "accent", "acento", "акцент", "强调色", "強調色", "accento", "accent", "destaque", "Akzent", "تمييز", "акцент", "vurgu", "강조", "アクセント", "accent");
        tk("border", "border", "borde", "рамка", "边框", "邊框", "bordo", "bordure", "borda", "Rahmen", "حد", "рамка", "kenarlık", "테두리", "枠線", "rand");
        tk("close", "close", "cerrar", "закрыть", "关闭", "關閉", "chiudi", "fermer", "fechar", "schließen", "إغلاق", "закрити", "kapat", "닫기", "閉じる", "sluiten");
        tk("image", "image", "imagen", "изображение", "图片", "圖片", "immagine", "image", "imagem", "Bild", "صورة", "зображення", "görsel", "이미지", "画像", "afbeelding");
        tk("item", "item", "elemento", "элемент", "项目", "項目", "elemento", "élément", "item", "Element", "عنصر", "елемент", "öğe", "항목", "項目", "item");
        tk("items", "items", "elementos", "элементы", "项目", "項目", "elementi", "éléments", "itens", "Elemente", "عناصر", "елементи", "öğeler", "항목", "項目", "items");
        tk("list", "list", "lista", "список", "列表", "列表", "elenco", "liste", "lista", "Liste", "قائمة", "список", "liste", "목록", "リスト", "lijst");
        tk("sheet", "sheet", "hoja", "панель", "面板", "面板", "foglio", "feuille", "painel", "Blatt", "ورقة", "панель", "sayfa", "시트", "シート", "blad");
        tk("views", "views", "vistas", "просмотры", "浏览量", "瀏覽數", "visualizzazioni", "vues", "visualizações", "Aufrufe", "المشاهدات", "перегляди", "görüntülenme", "조회수", "閲覧数", "weergaven");
        tk("view", "view", "vista", "просмотр", "视图", "檢視", "vista", "vue", "visualização", "Ansicht", "عرض", "перегляд", "görünüm", "보기", "表示", "weergave");
        tk("add", "add", "añadir", "добавить", "添加", "新增", "aggiungi", "ajouter", "adicionar", "hinzufügen", "إضافة", "додати", "ekle", "추가", "追加", "toevoegen");
        tk("added", "added", "añadido", "добавлено", "已添加", "已新增", "aggiunto", "ajouté", "adicionado", "hinzugefügt", "تمت الإضافة", "додано", "eklendi", "추가됨", "追加済み", "toegevoegd");
        tk("answer", "answer", "respuesta", "ответ", "回答", "回答", "risposta", "réponse", "resposta", "Antwort", "إجابة", "відповідь", "cevap", "답변", "回答", "antwoord");
        tk("box", "box", "cuadro", "блок", "框", "框", "riquadro", "cadre", "caixa", "Feld", "مربع", "блок", "kutu", "상자", "ボックス", "vak");
        tk("cursor", "cursor", "cursor", "курсор", "光标", "游標", "cursore", "curseur", "cursor", "Cursor", "مؤشر", "курсор", "imleç", "커서", "カーソル", "cursor");
        tk("details", "details", "detalles", "подробности", "详情", "詳情", "dettagli", "détails", "detalhes", "Details", "تفاصيل", "деталі", "ayrıntılar", "세부정보", "詳細", "details");
        tk("error", "error", "error", "ошибка", "错误", "錯誤", "errore", "erreur", "erro", "Fehler", "خطأ", "помилка", "hata", "오류", "エラー", "fout");
        tk("lock", "lock", "bloqueo", "замок", "锁定", "鎖定", "blocco", "verrou", "bloqueio", "Sperre", "قفل", "замок", "kilit", "잠금", "ロック", "slot");
        tk("performer", "artist", "artista", "исполнитель", "歌手", "歌手", "artista", "artiste", "artista", "Künstler", "الفنان", "виконавець", "sanatçı", "아티스트", "アーティスト", "artiest");
        tk("perfomer", "artist", "artista", "исполнитель", "歌手", "歌手", "artista", "artiste", "artista", "Künstler", "الفنان", "виконавець", "sanatçı", "아티스트", "アーティスト", "artiest");
        tk("radio", "radio button", "botón de opción", "радиокнопка", "单选按钮", "單選按鈕", "pulsante radio", "bouton radio", "botão de opção", "Optionsfeld", "زر اختيار", "перемикач", "radyo düğmesi", "라디오 버튼", "ラジオボタン", "keuzerondje");
        tk("secret", "secret", "secreto", "секретный", "私密", "私密", "segreto", "secret", "secreto", "geheim", "سري", "секретний", "gizli", "비밀", "シークレット", "geheim");
        tk("section", "section", "sección", "секция", "区块", "區塊", "sezione", "section", "seção", "Abschnitt", "قسم", "секція", "bölüm", "섹션", "セクション", "sectie");
        tk("shared", "shared", "compartido", "общий", "共享", "共享", "condiviso", "partagé", "compartilhado", "geteilt", "مشترك", "спільний", "paylaşılan", "공유됨", "共有", "gedeeld");
        tk("share", "share", "compartir", "поделиться", "分享", "分享", "condividi", "partager", "compartilhar", "teilen", "مشاركة", "поділитися", "paylaş", "공유", "共有", "delen");
        tk("venue", "venue", "lugar", "место", "地点", "地點", "luogo", "lieu", "local", "Ort", "مكان", "місце", "mekan", "장소", "場所", "locatie");
        tk("verified", "verified", "verificado", "подтверждено", "已认证", "已驗證", "verificato", "vérifié", "verificado", "verifiziert", "موثّق", "підтверджено", "doğrulanmış", "인증됨", "認証済み", "geverifieerd");
        tk("alpha", "transparency", "transparencia", "прозрачность", "透明度", "透明度", "trasparenza", "transparence", "transparência", "Transparenz", "الشفافية", "прозорість", "saydamlık", "투명도", "透明度", "transparantie");
        tk("arrow", "arrow", "flecha", "стрелка", "箭头", "箭頭", "freccia", "flèche", "seta", "Pfeil", "سهم", "стрілка", "ok", "화살표", "矢印", "pijl");
        tk("authorize", "authorize", "autorizar", "авторизация", "授权", "授權", "autorizza", "autoriser", "autorizar", "autorisieren", "تفويض", "авторизація", "yetkilendir", "인증", "認証", "autoriseren");
        tk("cancel", "cancel", "cancelar", "отмена", "取消", "取消", "annulla", "annuler", "cancelar", "abbrechen", "إلغاء", "скасувати", "iptal", "취소", "キャンセル", "annuleren");
        tk("cats", "categories", "categorías", "категории", "分类", "分類", "categorie", "catégories", "categorias", "Kategorien", "الفئات", "категорії", "kategoriler", "카테고리", "カテゴリ", "categorieën");
        tk("comment", "comment", "comentario", "комментарий", "评论", "留言", "commento", "commentaire", "comentário", "Kommentar", "تعليق", "коментар", "yorum", "댓글", "コメント", "reactie");
        tk("divider", "divider", "divisor", "разделитель", "分隔线", "分隔線", "divisore", "séparateur", "divisor", "Trennlinie", "فاصل", "роздільник", "ayırıcı", "구분선", "区切り線", "scheidingslijn");
        tk("separator", "separator", "separador", "разделитель", "分隔符", "分隔符", "separatore", "séparateur", "separador", "Trenner", "فاصل", "роздільник", "ayırıcı", "구분자", "区切り", "scheidingsteken");
        tk("fast", "fast", "rápido", "быстрый", "快速", "快速", "veloce", "rapide", "rápido", "schnell", "سريع", "швидкий", "hızlı", "빠른", "高速", "snel");
        tk("slow", "slow", "lento", "медленный", "慢速", "慢速", "lento", "lent", "lento", "langsam", "بطيء", "повільний", "yavaş", "느린", "低速", "traag");
        tk("floating", "floating", "flotante", "плавающий", "悬浮", "懸浮", "fluttuante", "flottant", "flutuante", "schwebend", "عائم", "плаваючий", "yüzen", "플로팅", "浮遊", "zwevend");
        tk("go", "go", "ir", "перейти", "跳转", "跳轉", "vai", "aller", "ir", "gehe", "انتقال", "перейти", "git", "이동", "移動", "ga");
        tk("highlight", "highlight", "resaltado", "подсветка", "高亮", "高亮", "evidenziazione", "surbrillance", "destaque", "Hervorhebung", "تمييز", "підсвітка", "vurgu", "강조", "ハイライト", "markering");
        tk("invite", "invite", "invitar", "приглашение", "邀请", "邀請", "invito", "invitation", "convite", "Einladung", "دعوة", "запрошення", "davet", "초대", "招待", "uitnodiging");
        tk("leave", "leave", "salir", "покинуть", "离开", "離開", "esci", "quitter", "sair", "verlassen", "مغادرة", "покинути", "ayrıl", "나가기", "退出", "verlaten");
        tk("outline", "outline", "contorno", "контур", "轮廓", "輪廓", "contorno", "contour", "contorno", "Umriss", "إطار", "контур", "anahat", "외곽선", "輪郭", "omtrek");
        tk("passport", "Passport", "Pasaporte", "Паспорт", "通行证", "護照", "Passaporto", "Passeport", "Passaporte", "Pass", "جواز السفر", "Паспорт", "Pasaport", "패스포트", "パスポート", "Paspoort");
        tk("permission", "permission", "permiso", "разрешение", "权限", "權限", "permesso", "autorisation", "permissão", "Berechtigung", "إذن", "дозвіл", "izin", "권한", "権限", "machtiging");
        tk("read", "read", "leído", "прочитано", "已读", "已讀", "letto", "lu", "lido", "gelesen", "مقروء", "прочитано", "okundu", "읽음", "既読", "gelezen");
        tk("return", "return", "volver", "возврат", "返回", "返回", "ritorna", "retour", "voltar", "zurück", "رجوع", "повернення", "dön", "돌아가기", "戻る", "terug");
        tk("set", "set", "conjunto", "набор", "合集", "合集", "set", "ensemble", "conjunto", "Set", "مجموعة", "набір", "set", "세트", "セット", "set");
        tk("span", "highlight", "resaltado", "фрагмент", "片段", "片段", "evidenziazione", "segment", "destaque", "Bereich", "نطاق", "фрагмент", "vurgulama", "구간", "スパン", "bereik");
        tk("star", "star", "estrella", "звезда", "星星", "星星", "stella", "étoile", "estrela", "Stern", "نجمة", "зірка", "yıldız", "별", "スター", "ster");
        tk("stars", "Stars", "Estrellas", "Звёзды", "星星", "星星", "Stelle", "Étoiles", "Estrelas", "Sterne", "النجوم", "Зірки", "Yıldızlar", "스타", "スター", "Sterren");
        tk("status", "status", "estado", "статус", "状态", "狀態", "stato", "statut", "status", "Status", "الحالة", "статус", "durum", "상태", "ステータス", "status");
        tk("telegram", "Telegram", "Telegram", "Telegram", "Telegram", "Telegram", "Telegram", "Telegram", "Telegram", "Telegram", "تيليجرام", "Telegram", "Telegram", "텔레그램", "Telegram", "Telegram");
        tk("three", "three", "tres", "три", "三", "三", "tre", "trois", "três", "drei", "ثلاثة", "три", "üç", "세", "3", "drie");
        tk("trending", "trending", "tendencias", "популярные", "热门", "熱門", "di tendenza", "tendances", "em alta", "Trends", "رائج", "популярні", "trend", "인기", "トレンド", "trending");
        tk("undo", "Undo bar", "Barra deshacer", "Панель отмены", "撤销栏", "復原列", "Barra annulla", "Barre annuler", "Barra desfazer", "Rückgängig-Leiste", "شريط التراجع", "Панель скасування", "Geri alma çubuğu", "실행 취소 바", "元に戻すバー", "Ongedaan maken");
        tk("via", "via", "vía", "через", "通过", "透過", "tramite", "via", "via", "über", "عبر", "через", "aracılığıyla", "경유", "経由", "via");
        tk("wallet", "Wallet", "Billetera", "Кошелёк", "钱包", "錢包", "Portafoglio", "Portefeuille", "Carteira", "Wallet", "المحفظة", "Гаманець", "Cüzdan", "지갑", "ウォレット", "Portemonnee");
        tk("badge", "badge", "insignia", "значок", "徽章", "徽章", "badge", "badge", "selo", "Abzeichen", "شارة", "значок", "rozet", "배지", "バッジ", "badge");
        tk("blur", "blur", "desenfoque", "размытие", "模糊", "模糊", "sfocatura", "flou", "desfoque", "Unschärfe", "ضبابية", "розмиття", "bulanıklık", "흐림", "ぼかし", "vervaging");
        tk("bold", "bold", "negrita", "жирный", "粗体", "粗體", "grassetto", "gras", "negrito", "fett", "عريض", "жирний", "kalın", "굵게", "太字", "vet");
        tk("cache", "cache", "caché", "кэш", "缓存", "快取", "cache", "cache", "cache", "Cache", "ذاكرة مؤقتة", "кеш", "önbellek", "캐시", "キャッシュ", "cache");
        tk("coin", "coin", "moneda", "монета", "硬币", "硬幣", "moneta", "pièce", "moeda", "Münze", "عملة", "монета", "madeni para", "코인", "コイン", "munt");
        tk("correct", "correct", "correcto", "правильно", "正确", "正確", "corretto", "correct", "correto", "richtig", "صحيح", "правильно", "doğru", "정답", "正解", "juist");
        tk("wrong", "wrong", "incorrecto", "неверно", "错误", "錯誤", "errato", "incorrect", "incorreto", "falsch", "خطأ", "неправильно", "yanlış", "오답", "不正解", "fout");
        tk("creator", "creator", "creador", "создатель", "创建者", "建立者", "creatore", "créateur", "criador", "Ersteller", "المنشئ", "творець", "kurucu", "생성자", "作成者", "maker");
        tk("date", "date", "fecha", "дата", "日期", "日期", "data", "date", "data", "Datum", "التاريخ", "дата", "tarih", "날짜", "日付", "datum");
        tk("delete", "delete", "eliminar", "удалить", "删除", "刪除", "elimina", "supprimer", "excluir", "löschen", "حذف", "видалити", "sil", "삭제", "削除", "verwijderen");
        tk("dot", "dot", "punto", "точка", "圆点", "圓點", "punto", "point", "ponto", "Punkt", "نقطة", "крапка", "nokta", "점", "ドット", "stip");
        tk("fade", "fade", "desvanecido", "затухание", "渐隐", "漸隱", "dissolvenza", "fondu", "esmaecimento", "Ausblenden", "تلاشي", "згасання", "solma", "페이드", "フェード", "vervagen");
        tk("folder", "folder", "carpeta", "папка", "文件夹", "資料夾", "cartella", "dossier", "pasta", "Ordner", "مجلد", "папка", "klasör", "폴더", "フォルダ", "map");
        tk("forwarded", "forwarded", "reenviado", "переслано", "已转发", "已轉寄", "inoltrato", "transféré", "encaminhado", "weitergeleitet", "محوّل", "переслано", "iletildi", "전달됨", "転送", "doorgestuurd");
        tk("friends", "friends", "amigos", "друзья", "好友", "好友", "amici", "amis", "amigos", "Freunde", "الأصدقاء", "друзі", "arkadaşlar", "친구", "友達", "vrienden");
        tk("gif", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF", "GIF");
        tk("gift", "gift", "regalo", "подарок", "礼物", "禮物", "regalo", "cadeau", "presente", "Geschenk", "هدية", "подарунок", "hediye", "선물", "ギフト", "cadeau");
        tk("gifts", "gifts", "regalos", "подарки", "礼物", "禮物", "regali", "cadeaux", "presentes", "Geschenke", "هدايا", "подарунки", "hediyeler", "선물", "ギフト", "cadeaus");
        tk("last", "last", "último", "последний", "最后", "最後", "ultimo", "dernier", "último", "letzte", "أخير", "останній", "son", "마지막", "最後", "laatste");
        tk("login", "login", "inicio de sesión", "вход", "登录", "登入", "accesso", "connexion", "login", "Anmeldung", "تسجيل الدخول", "вхід", "giriş", "로그인", "ログイン", "inloggen");
        tk("main", "main", "principal", "основной", "主", "主", "principale", "principal", "principal", "Haupt", "رئيسي", "головний", "ana", "메인", "メイン", "hoofd");
        tk("mention", "mention", "mención", "упоминание", "提及", "提及", "menzione", "mention", "menção", "Erwähnung", "إشارة", "згадка", "bahsetme", "멘션", "メンション", "vermelding");
        tk("neutral", "neutral", "neutro", "нейтральный", "中性", "中性", "neutro", "neutre", "neutro", "neutral", "محايد", "нейтральний", "nötr", "중립", "ニュートラル", "neutraal");
        tk("pack", "pack", "paquete", "набор", "包", "包", "pacchetto", "pack", "pacote", "Paket", "حزمة", "пакет", "paket", "팩", "パック", "pakket");
        tk("pause", "pause", "pausa", "пауза", "暂停", "暫停", "pausa", "pause", "pausa", "Pause", "إيقاف مؤقت", "пауза", "duraklat", "일시정지", "一時停止", "pauze");
        tk("play", "play", "reproducir", "воспроизведение", "播放", "播放", "riproduci", "lecture", "reproduzir", "Wiedergabe", "تشغيل", "відтворення", "oynat", "재생", "再生", "afspelen");
        tk("pinned", "pinned", "fijado", "закреплено", "置顶", "釘選", "fissato", "épinglé", "fixado", "angepinnt", "مثبّت", "закріплено", "sabitlenmiş", "고정됨", "ピン留め", "vastgezet");
        tk("pin", "pin", "fijar", "закрепить", "置顶", "釘選", "fissa", "épingler", "fixar", "anpinnen", "تثبيت", "закріпити", "sabitle", "고정", "ピン", "vastzetten");
        tk("psa", "announcement", "anuncio", "объявление", "公告", "公告", "annuncio", "annonce", "anúncio", "Ankündigung", "إعلان", "оголошення", "duyuru", "공지", "お知らせ", "aankondiging");
        tk("pull", "pull", "tirar", "потянуть", "下拉", "下拉", "trascina", "tirer", "puxar", "ziehen", "سحب", "потягнути", "çek", "당기기", "引っ張る", "trekken");
        tk("pulling", "pulling", "tirando", "перетягивание", "拉动中", "拉動中", "trascinando", "traction", "puxando", "ziehend", "سحب", "перетягування", "çekiliyor", "당기는 중", "引っ張り中", "trekken");
        tk("quote", "quote", "cita", "цитата", "引用", "引用", "citazione", "citation", "citação", "Zitat", "اقتباس", "цитата", "alıntı", "인용", "引用", "citaat");
        tk("received", "received", "recibido", "получено", "已收到", "已收到", "ricevuto", "reçu", "recebido", "empfangen", "مستلم", "отримано", "alındı", "수신됨", "受信済み", "ontvangen");
        tk("remove", "remove", "quitar", "удалить", "移除", "移除", "rimuovi", "retirer", "remover", "entfernen", "إزالة", "вилучити", "kaldır", "제거", "削除", "verwijderen");
        tk("ribbon", "ribbon", "cinta", "лента", "丝带", "絲帶", "nastro", "ruban", "faixa", "Band", "شريط", "стрічка", "kurdele", "리본", "リボン", "lint");
        tk("round", "round", "redondo", "круглый", "圆形", "圓形", "rotondo", "rond", "redondo", "rund", "دائري", "круглий", "yuvarlak", "둥근", "丸型", "rond");
        tk("save", "save", "guardar", "сохранить", "保存", "儲存", "salva", "enregistrer", "salvar", "speichern", "حفظ", "зберегти", "kaydet", "저장", "保存", "opslaan");
        tk("saved", "saved", "guardado", "сохранено", "已保存", "已儲存", "salvato", "enregistré", "salvo", "gespeichert", "محفوظ", "збережено", "kaydedildi", "저장됨", "保存済み", "opgeslagen");
        tk("seen", "seen", "visto", "просмотрено", "已看", "已看", "visto", "vu", "visto", "gesehen", "تمت المشاهدة", "переглянуто", "görüldü", "읽음", "既読", "gezien");
        tk("signature", "signature", "firma", "подпись", "签名", "簽名", "firma", "signature", "assinatura", "Signatur", "توقيع", "підпис", "imza", "서명", "署名", "handtekening");
        tk("site", "site", "sitio", "сайт", "网站", "網站", "sito", "site", "site", "Website", "موقع", "сайт", "site", "사이트", "サイト", "website");
        tk("small", "small", "pequeño", "малый", "小", "小", "piccolo", "petit", "pequeno", "klein", "صغير", "малий", "küçük", "작은", "小", "klein");
        tk("tag", "tag", "etiqueta", "тег", "标签", "標籤", "tag", "étiquette", "etiqueta", "Tag", "وسم", "тег", "etiket", "태그", "タグ", "tag");
        tk("target", "target", "objetivo", "цель", "目标", "目標", "destinazione", "cible", "destino", "Ziel", "هدف", "ціль", "hedef", "대상", "ターゲット", "doel");
        tk("thumb", "knob", "perilla", "ползунок", "滑块", "滑塊", "cursore", "curseur", "controle", "Regler", "مقبض", "повзунок", "tutamak", "노브", "つまみ", "knop");
        tk("topic", "topic", "tema", "тема", "话题", "主題", "argomento", "sujet", "tópico", "Thema", "موضوع", "тема", "konu", "주제", "トピック", "onderwerp");
        tk("topics", "topics", "temas", "темы", "话题", "主題", "argomenti", "sujets", "tópicos", "Themen", "المواضيع", "теми", "konular", "주제", "トピック", "onderwerpen");
        tk("waveform", "waveform", "onda de audio", "волна звука", "音频波形", "音訊波形", "forma d'onda", "forme d'onde", "onda sonora", "Wellenform", "موجة الصوت", "хвиля звуку", "ses dalgası", "파형", "波形", "golfvorm");
        tk("ab", "Action bar", "Barra superior", "Верхняя панель", "顶部栏", "頂部列", "Barra azioni", "Barre d'action", "Barra de ações", "Aktionsleiste", "شريط الإجراءات", "Верхня панель", "İşlem çubuğu", "액션 바", "アクションバー", "Actiebalk");
        tk("animated", "animated", "animado", "анимированный", "动态", "動態", "animato", "animé", "animado", "animiert", "متحرك", "анімований", "animasyonlu", "애니메이션", "アニメーション", "geanimeerd");
        tk("back", "back", "atrás", "назад", "返回", "返回", "indietro", "retour", "voltar", "zurück", "رجوع", "назад", "geri", "뒤로", "戻る", "terug");
        tk("backspace", "backspace", "borrar", "удаление", "退格", "退格", "cancella", "retour arrière", "apagar", "Rücktaste", "مسافة للخلف", "видалення", "geri sil", "백스페이스", "バックスペース", "backspace");
        tk("browser", "Browser", "Navegador", "Браузер", "浏览器", "瀏覽器", "Browser", "Navigateur", "Navegador", "Browser", "المتصفح", "Браузер", "Tarayıcı", "브라우저", "ブラウザ", "Browser");
        tk("cached", "cached", "en caché", "в кэше", "已缓存", "已快取", "in cache", "en cache", "em cache", "im Cache", "مخزّن مؤقتاً", "у кеші", "önbellekte", "캐시됨", "キャッシュ済み", "in cache");
        tk("card", "card", "tarjeta", "карточка", "卡片", "卡片", "scheda", "carte", "cartão", "Karte", "بطاقة", "картка", "kart", "카드", "カード", "kaart");
        tk("changephoneinfo", "Change phone", "Cambiar teléfono", "Смена номера", "更换手机号", "更換手機號", "Cambia numero", "Changer de numéro", "Alterar telefone", "Nummer ändern", "تغيير الرقم", "Зміна номера", "Numara değiştir", "번호 변경", "電話番号変更", "Nummer wijzigen");
        tk("chevron", "chevron", "flecha", "шеврон", "箭头", "箭頭", "freccia", "chevron", "seta", "Pfeil", "سهم", "шеврон", "ok", "화살표", "矢印", "pijltje");
        tk("connecting", "connecting", "conectando", "подключение", "连接中", "連線中", "connessione", "connexion", "conectando", "verbinden", "جارٍ الاتصال", "підключення", "bağlanıyor", "연결 중", "接続中", "verbinden");
        tk("constant", "constant", "constante", "константа", "常量", "常數", "costante", "constante", "constante", "Konstante", "ثابت", "константа", "sabit", "상수", "定数", "constante");
        tk("create", "create", "crear", "создать", "创建", "建立", "crea", "créer", "criar", "erstellen", "إنشاء", "створити", "oluştur", "생성", "作成", "maken");
        tk("currency", "currency", "moneda", "валюта", "货币", "貨幣", "valuta", "devise", "moeda", "Währung", "عملة", "валюта", "para birimi", "통화", "通貨", "valuta");
        tk("description", "description", "descripción", "описание", "描述", "描述", "descrizione", "description", "descrição", "Beschreibung", "وصف", "опис", "açıklama", "설명", "説明", "beschrijving");
        tk("devices", "devices", "dispositivos", "устройства", "设备", "裝置", "dispositivi", "appareils", "dispositivos", "Geräte", "الأجهزة", "пристрої", "cihazlar", "기기", "デバイス", "apparaten");
        tk("draft", "draft", "borrador", "черновик", "草稿", "草稿", "bozza", "brouillon", "rascunho", "Entwurf", "مسودة", "чернетка", "taslak", "임시저장", "下書き", "concept");
        tk("edge", "edge", "borde", "край", "边缘", "邊緣", "bordo", "bord", "borda", "Rand", "حافة", "край", "kenar", "가장자리", "端", "rand");
        tk("edit", "edit", "editar", "редактировать", "编辑", "編輯", "modifica", "modifier", "editar", "bearbeiten", "تعديل", "редагувати", "düzenle", "편집", "編集", "bewerken");
        tk("function", "function", "función", "функция", "函数", "函式", "funzione", "fonction", "função", "Funktion", "دالة", "функція", "işlev", "함수", "関数", "functie");
        tk("gallery", "gallery", "galería", "галерея", "相册", "相簿", "galleria", "galerie", "galeria", "Galerie", "المعرض", "галерея", "galeri", "갤러리", "ギャラリー", "galerij");
        tk("game", "game", "juego", "игра", "游戏", "遊戲", "gioco", "jeu", "jogo", "Spiel", "لعبة", "гра", "oyun", "게임", "ゲーム", "spel");
        tk("glow", "glow", "brillo", "свечение", "光晕", "光暈", "bagliore", "lueur", "brilho", "Leuchten", "توهج", "світіння", "parlama", "발광", "グロー", "gloed");
        tk("hairline", "hairline", "línea fina", "тонкая линия", "细线", "細線", "linea sottile", "ligne fine", "linha fina", "Haarlinie", "خط رفيع", "тонка лінія", "ince çizgi", "가는 선", "極細線", "haarlijn");
        tk("header", "header", "cabecera", "заголовок", "标题栏", "標題列", "intestazione", "en-tête", "cabeçalho", "Kopfzeile", "رأس", "заголовок", "başlık", "헤더", "ヘッダー", "kop");
        tk("hidden", "hidden", "oculto", "скрытый", "隐藏", "隱藏", "nascosto", "masqué", "oculto", "versteckt", "مخفي", "прихований", "gizli", "숨김", "非表示", "verborgen");
        tk("keyword", "keyword", "palabra clave", "ключевое слово", "关键词", "關鍵字", "parola chiave", "mot-clé", "palavra-chave", "Schlüsselwort", "كلمة مفتاحية", "ключове слово", "anahtar kelime", "키워드", "キーワード", "trefwoord");
        tk("listening", "listening", "escuchando", "прослушивание", "收听中", "收聽中", "in ascolto", "écoute", "ouvindo", "hört zu", "يستمع", "прослуховування", "dinliyor", "듣는 중", "視聴中", "luisteren");
        tk("load", "load", "cargar", "загрузка", "加载", "載入", "carica", "charger", "carregar", "laden", "تحميل", "завантаження", "yükle", "불러오기", "読み込み", "laden");
        tk("loading", "loading", "cargando", "загрузка", "加载中", "載入中", "caricamento", "chargement", "carregando", "lädt", "جارٍ التحميل", "завантаження", "yükleniyor", "로딩 중", "読み込み中", "laden");
        tk("logo", "logo", "logotipo", "логотип", "徽标", "標誌", "logo", "logo", "logo", "Logo", "شعار", "логотип", "logo", "로고", "ロゴ", "logo");
        tk("mark", "mark", "marca", "метка", "标记", "標記", "segno", "marque", "marca", "Markierung", "علامة", "мітка", "işaret", "표시", "マーク", "markering");
        tk("members", "members", "miembros", "участники", "成员", "成員", "membri", "membres", "membros", "Mitglieder", "الأعضاء", "учасники", "üyeler", "멤버", "メンバー", "leden");
        tk("middle", "middle", "medio", "средний", "中间", "中間", "medio", "milieu", "meio", "Mitte", "وسط", "середній", "orta", "중간", "中央", "midden");
        tk("navigation", "navigation", "navegación", "навигация", "导航", "導覽", "navigazione", "navigation", "navegação", "Navigation", "التنقل", "навігація", "gezinme", "내비게이션", "ナビゲーション", "navigatie");
        tk("new", "new", "nuevo", "новый", "新建", "新增", "nuovo", "nouveau", "novo", "neu", "جديد", "новий", "yeni", "새", "新規", "nieuw");
        tk("normal", "normal", "normal", "обычный", "普通", "一般", "normale", "normal", "normal", "normal", "عادي", "звичайний", "normal", "보통", "標準", "normaal");
        tk("number", "number", "número", "число", "数字", "數字", "numero", "nombre", "número", "Zahl", "رقم", "число", "sayı", "숫자", "数値", "nummer");
        tk("offset", "offset", "desplazamiento", "смещение", "偏移", "偏移", "scostamento", "décalage", "deslocamento", "Versatz", "إزاحة", "зміщення", "kaydırma", "오프셋", "オフセット", "verschuiving");
        tk("online", "online", "en línea", "в сети", "在线", "線上", "online", "en ligne", "online", "online", "متصل", "у мережі", "çevrimiçi", "온라인", "オンライン", "online");
        tk("operator", "operator", "operador", "оператор", "运算符", "運算子", "operatore", "opérateur", "operador", "Operator", "معامل", "оператор", "operatör", "연산자", "演算子", "operator");
        tk("other", "other", "otro", "другой", "其他", "其他", "altro", "autre", "outro", "andere", "آخر", "інший", "diğer", "기타", "その他", "overig");
        tk("place", "place", "lugar", "место", "地点", "地點", "luogo", "lieu", "lugar", "Ort", "مكان", "місце", "yer", "장소", "場所", "plaats");
        tk("regular", "regular", "normal", "обычный", "常规", "一般", "regolare", "normal", "regular", "regulär", "عادي", "звичайний", "normal", "일반", "通常", "normaal");
        tk("result", "result", "resultado", "результат", "结果", "結果", "risultato", "résultat", "resultado", "Ergebnis", "نتيجة", "результат", "sonuç", "결과", "結果", "resultaat");
        tk("ripple", "ripple", "onda al pulsar", "эффект нажатия", "点击波纹", "點擊波紋", "effetto tocco", "ondulation", "ondulação", "Wellen-Effekt", "تموج", "ефект натискання", "dalga efekti", "물결 효과", "リップル", "rimpel");
        tk("rotation", "rotation", "rotación", "поворот", "旋转", "旋轉", "rotazione", "rotation", "rotação", "Drehung", "تدوير", "обертання", "döndürme", "회전", "回転", "rotatie");
        tk("rtmp", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP", "RTMP");
        tk("sessions", "Sessions", "Sesiones", "Сеансы", "会话", "工作階段", "Sessioni", "Sessions", "Sessões", "Sitzungen", "الجلسات", "Сеанси", "Oturumlar", "세션", "セッション", "Sessies");
        tk("side", "side", "lateral", "боковой", "侧边", "側邊", "laterale", "latéral", "lateral", "Seite", "جانبي", "бічний", "yan", "측면", "サイド", "zijkant");
        tk("soldout", "sold out", "agotado", "распродано", "已售罄", "已售罄", "esaurito", "épuisé", "esgotado", "ausverkauft", "نفذت الكمية", "розпродано", "tükendi", "품절", "売り切れ", "uitverkocht");
        tk("speaking", "speaking", "hablando", "говорит", "正在说话", "正在說話", "sta parlando", "parle", "falando", "spricht", "يتحدث", "говорить", "konuşuyor", "말하는 중", "発言中", "spreekt");
        tk("string", "string", "cadena", "строка", "字符串", "字串", "stringa", "chaîne", "texto", "Zeichenkette", "نص", "рядок", "dize", "문자열", "文字列", "tekenreeks");
        tk("swipe", "swipe", "deslizar", "свайп", "滑动", "滑動", "scorrimento", "glissement", "deslizar", "Wischen", "سحب", "свайп", "kaydırma", "스와이프", "スワイプ", "vegen");
        tk("tablet", "tablet", "tableta", "планшет", "平板", "平板", "tablet", "tablette", "tablet", "Tablet", "جهاز لوحي", "планшет", "tablet", "태블릿", "タブレット", "tablet");
        tk("transcribe", "transcribe", "transcribir", "расшифровка", "转文字", "轉文字", "trascrivi", "transcrire", "transcrever", "transkribieren", "تحويل لنص", "розшифровка", "metne çevir", "받아쓰기", "文字起こし", "transcriberen");
        tk("value", "value", "valor", "значение", "数值", "數值", "valore", "valeur", "valor", "Wert", "قيمة", "значення", "değer", "값", "値", "waarde");
        tk("zoom", "zoom", "zoom", "масштаб", "缩放", "縮放", "zoom", "zoom", "zoom", "Zoom", "تكبير", "масштаб", "yakınlaştırma", "확대/축소", "ズーム", "zoom");
        tk("by", "by", "por", "от", "由", "由", "da", "par", "por", "von", "بواسطة", "від", "ile", "기준", "による", "door");
        tk("to", "to", "a", "к", "到", "到", "a", "vers", "para", "zu", "إلى", "до", "için", "대상", "へ", "naar");
        tk("sdk", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK", "SDK");
    }

    private ThemeKeyLocalizer() {
    }

    private static void ex(String key, String... values) {
        EXACT.put(key, values);
    }

    private static void tk(String token, String... values) {
        TOKENS.put(token, values);
    }

    private static int resolveCurrentLang() {
        Locale locale = LocaleController.getInstance().getCurrentLocale();
        if (locale == null) {
            return LANG_EN;
        }
        String lang = locale.getLanguage();
        if (TextUtils.isEmpty(lang)) {
            return LANG_EN;
        }
        lang = lang.toLowerCase(Locale.US);
        if ("es".equals(lang)) return LANG_ES;
        if ("ru".equals(lang)) return LANG_RU;
        if ("zh".equals(lang)) {
            String country = locale.getCountry();
            String script = locale.getScript();
            if ("TW".equalsIgnoreCase(country) || "HK".equalsIgnoreCase(country) || "MO".equalsIgnoreCase(country) || "Hant".equalsIgnoreCase(script)) {
                return LANG_ZH_TW;
            }
            return LANG_ZH;
        }
        if ("it".equals(lang)) return LANG_IT;
        if ("fr".equals(lang)) return LANG_FR;
        if ("pt".equals(lang)) return LANG_PT;
        if ("de".equals(lang)) return LANG_DE;
        if ("ar".equals(lang)) return LANG_AR;
        if ("uk".equals(lang)) return LANG_UK;
        if ("tr".equals(lang)) return LANG_TR;
        if ("ko".equals(lang)) return LANG_KO;
        if ("ja".equals(lang)) return LANG_JA;
        if ("nl".equals(lang)) return LANG_NL;
        return LANG_EN;
    }

    public static String getLocalizedTitle(String rawKey) {
        if (TextUtils.isEmpty(rawKey)) {
            return "";
        }
        int lang = resolveCurrentLang();
        synchronized (cache) {
            if (cachedLang != lang) {
                cache.clear();
                cachedLang = lang;
            }
            String hit = cache.get(rawKey);
            if (hit != null) {
                return hit;
            }
        }
        String translated = translateKey(rawKey, lang);
        synchronized (cache) {
            if (cachedLang == lang) {
                cache.put(rawKey, translated);
            }
        }
        return translated;
    }

    private static String translateKey(String rawKey, int lang) {
        String[] exactArr = EXACT.get(rawKey);
        if (exactArr != null && lang < exactArr.length) {
            return exactArr[lang];
        }

        int underscoreIdx = rawKey.indexOf('_');
        String prefixPart = null;
        String bodyPart = rawKey;
        if (underscoreIdx > 0 && underscoreIdx < rawKey.length() - 1) {
            prefixPart = rawKey.substring(0, underscoreIdx);
            bodyPart = rawKey.substring(underscoreIdx + 1);
        }

        String translatedBody = translateSegment(bodyPart, lang);
        if (prefixPart != null) {
            String translatedPrefix = translateSegment(prefixPart, lang);
            if (!TextUtils.isEmpty(translatedPrefix)) {
                String sep = (lang == LANG_ZH || lang == LANG_ZH_TW || lang == LANG_JA) ? "：" : ": ";
                return capitalize(translatedPrefix) + sep + translatedBody;
            }
        }
        return capitalize(translatedBody);
    }

    private static String translateSegment(String segment, int lang) {
        ArrayList<String> rawTokens = splitTokens(segment);
        boolean noSpaces = (lang == LANG_ZH || lang == LANG_ZH_TW || lang == LANG_JA);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawTokens.size(); i++) {
            String tok = rawTokens.get(i);
            String lower = tok.toLowerCase(Locale.US);
            // Skip technical SDK suffix like "SDK21"
            if ("sdk".equals(lower) && i + 1 < rawTokens.size() && "21".equals(rawTokens.get(i + 1))) {
                i++;
                continue;
            }
            String piece;
            String[] map = TOKENS.get(lower);
            if (map != null && lang < map.length) {
                piece = map[lang];
            } else {
                piece = tok;
            }
            if (TextUtils.isEmpty(piece)) {
                continue;
            }
            if (sb.length() > 0) {
                if (!noSpaces || Character.isDigit(piece.charAt(0))) {
                    sb.append(' ');
                }
            }
            sb.append(piece);
        }
        return sb.toString().trim();
    }

    private static ArrayList<String> splitTokens(String input) {
        ArrayList<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(input)) {
            return out;
        }
        int len = input.length();
        int start = 0;
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c == '_') {
                if (i > start) {
                    out.add(input.substring(start, i));
                }
                start = i + 1;
                continue;
            }
            if (i > start) {
                char prev = input.charAt(i - 1);
                boolean prevLower = Character.isLowerCase(prev);
                boolean currUpper = Character.isUpperCase(c);
                boolean prevDigit = Character.isDigit(prev);
                boolean currDigit = Character.isDigit(c);
                boolean upperBeforeLower = Character.isUpperCase(prev) && currUpper && (i + 1 < len) && Character.isLowerCase(input.charAt(i + 1));
                if ((prevLower && currUpper) || (prevDigit != currDigit) || upperBeforeLower) {
                    out.add(input.substring(start, i));
                    start = i;
                }
            }
        }
        if (start < len) {
            out.add(input.substring(start));
        }
        return out;
    }

    private static String capitalize(String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        }
        char first = s.charAt(0);
        if (Character.isLowerCase(first)) {
            return Character.toUpperCase(first) + s.substring(1);
        }
        return s;
    }
}
