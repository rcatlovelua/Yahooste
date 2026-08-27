package com.yahoostee.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class YahoosteePanel {
    private static final Logger LOGGER = Logger.getLogger("YahoosteePanel");

    // Размеры и отступы
    private static final int PANEL_WIDTH = 230;
    private static final int ELEMENT_HEIGHT = 20;
    private static final int SPACING = 4;
    private static final int MARGIN_RIGHT = 6;
    private static final int MARGIN_BOTTOM = 90;
    private static final int MAX_MACROS = 5;

    // Количество фиксированных строк интерфейса (не считая кастомных макросов):
    // 1) поле кастомного текста, 2) поле координат, 3) "Записать позицию",
    // 4) строка "Вставить коорд." / "Вставить позицию", 5) "Вставить текст из поля",
    // 6) разделитель, 7) строка из 3 кнопок создания макросов
    private static final int FIXED_ROWS = 7;

    // Внутреннее состояние панели
    private static String savedCoords = "";
    private static String currentCustomText = "";
    private static final List<CustomBtn> customButtons = new ArrayList<>();

    // Все виджеты, принадлежащие панели (кнопки, поля панели).
    // По ним отличаем "фокус на нашем интерфейсе" от "фокус на chatField".
    private static final java.util.Set<ClickableWidget> panelWidgets =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /**
     * true, если сейчас в фокусе именно виджет нашей панели
     * (кнопка, поле координат, поле кастомного текста и т.п.),
     * а не обычное поле ввода сообщения (chatField).
     */
    public static boolean isPanelWidgetFocused(net.minecraft.client.gui.screen.Screen screen) {
        var focused = screen.getFocused();
        return focused instanceof ClickableWidget widget && panelWidgets.contains(widget);
    }

    // --- ФАЙЛОВЫЕ ПУТИ ---
    private static Path getSavePath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("yahoostee_nether_coords.txt");
    }

    private static Path getMacrosPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("yahoostee_macros.txt");
    }

    // --- ЗАГРУЗКА И СОХРАНЕНИЕ ---
    private static void loadFiles() {
        try {
            Path coordsPath = getSavePath();
            if (Files.exists(coordsPath)) {
                savedCoords = Files.readString(coordsPath).trim();
            }

            customButtons.clear();
            Path macrosPath = getMacrosPath();
            if (Files.exists(macrosPath)) {
                List<String> lines = Files.readAllLines(macrosPath);
                for (String line : lines) {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        try {
                            customButtons.add(new CustomBtn(Action.valueOf(parts[0]), parts[1]));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("YahoosteePanel: ошибка загрузки файлов: " + e.getMessage());
        }
    }

    private static void saveCoords(String coords) {
        savedCoords = coords;
        try {
            Files.writeString(getSavePath(), coords);
        } catch (Exception e) {
            LOGGER.warning("YahoosteePanel: не удалось сохранить координаты: " + e.getMessage());
        }
    }

    private static void saveMacros() {
        try {
            List<String> lines = new ArrayList<>();
            for (CustomBtn btn : customButtons) {
                lines.add(btn.action.name() + "|" + btn.payload);
            }
            Files.write(getMacrosPath(), lines);
        } catch (Exception e) {
            LOGGER.warning("YahoosteePanel: не удалось сохранить макросы: " + e.getMessage());
        }
    }

    // --- ДИНАМИЧЕСКАЯ ВЫСОТА ---
    private static int getPanelHeight() {
        int totalElements = FIXED_ROWS + customButtons.size();
        return totalElements * ELEMENT_HEIGHT + (totalElements + 1) * SPACING;
    }

    // --- ПЕРЕЗАГРУЗКА ИНТЕРФЕЙСА ---
    private static void reloadScreen(TextFieldWidget chatField) {
        String currentChat = chatField.getText();
        MinecraftClient.getInstance().setScreen(new ChatScreen(currentChat, false));
    }

    // --- ОБЩАЯ ЛОГИКА "ВЫКЛЮЧЕННЫХ" КЛАВИШ ---
    // Tab / Enter / стрелки не должны утекать в остальной интерфейс чата
    // (автодополнение ников, отправка сообщения, история сообщений и т.д.)
    private static boolean isNavigationKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_TAB
                || keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP
                || keyCode == GLFW.GLFW_KEY_DOWN;
    }

    // --- СОЗДАНИЕ КНОПОК С ОТКЛЮЧЕНИЕМ ФОКУСА ---
    // Используем стандартный builder ButtonWidget — субклассирование через
    // protected-конструктор не переносимо между версиями маппингов и ломает сборку.
    private static ButtonWidget createButton(String text, int x, int y, int width, Runnable action) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(text), btn -> action.run())
                .dimensions(x, y, width, ELEMENT_HEIGHT)
                .build();
        button.setFocused(false);
        return button;
    }

    // --- УПРОЩЕННЫЙ КАСТОМНЫЙ TextFieldWidget ---
    private static class NoFocusTextField extends TextFieldWidget {
        public NoFocusTextField(net.minecraft.client.font.TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
            super(textRenderer, x, y, width, height, text);
            this.setFocused(false);
        }

        // Всегда сбрасываем фокус
        @Override
        public void setFocused(boolean focused) {
            super.setFocused(false);
        }

        // Просто возвращаем false и сбрасываем фокус.
        // Без @Override: в некоторых версиях Minecraft сигнатура обработки клика мыши
        // у TextFieldWidget отличается от (double, double, int) — как и в исходном файле,
        // оставляем это как есть, чтобы не ломать сборку под конкретную версию маппингов.
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.setFocused(false);
            return false;
        }

        // Tab/Enter/стрелки не должны переключать фокус экрана
        // или дёргать отправку сообщения/автодополнение.
        // С 1.21.9+ Minecraft передаёт клавишу единым объектом KeyInput вместо трёх int.
        @Override
        public boolean keyPressed(KeyInput input) {
            if (isNavigationKey(input.key())) {
                return true; // клавиша "выключена" для этого поля
            }
            return super.keyPressed(input);
        }
    }

    // --- ИНИЦИАЛИЗАЦИЯ GUI ---
    public static void init(ChatScreen screen, int screenWidth, int screenHeight,
                            Consumer<ClickableWidget> widgetAdder,
                            TextFieldWidget chatField) {

        loadFiles();

        int panelHeight = getPanelHeight();
        int x = screenWidth - PANEL_WIDTH - MARGIN_RIGHT;
        int y = screenHeight - MARGIN_BOTTOM - panelHeight;
        int currentY = y + SPACING;
        int elemWidth = PANEL_WIDTH - SPACING * 2;
        int buttonWidth = (elemWidth - SPACING) / 2;

        // 1. Поле для кастомного текста
        NoFocusTextField customTextField = new NoFocusTextField(
                MinecraftClient.getInstance().textRenderer,
                x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT, Text.empty()
        );
        customTextField.setMaxLength(256);
        customTextField.setPlaceholder(Text.literal("Кастомный текст..."));
        customTextField.setText(currentCustomText);
        customTextField.setChangedListener(text -> currentCustomText = text);
        widgetAdder.accept(customTextField);
        currentY += ELEMENT_HEIGHT + SPACING;

        // 2. Поле для координат
        NoFocusTextField coordsTextField = new NoFocusTextField(
                MinecraftClient.getInstance().textRenderer,
                x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT, Text.empty()
        );
        coordsTextField.setMaxLength(100);
        coordsTextField.setPlaceholder(Text.literal("Координаты для отправки..."));
        coordsTextField.setText(savedCoords);
        coordsTextField.setChangedListener(YahoosteePanel::saveCoords);
        widgetAdder.accept(coordsTextField);
        currentY += ELEMENT_HEIGHT + SPACING;

        // 3. Базовые кнопки
        widgetAdder.accept(createButton("Записать позицию", x + SPACING, currentY, elemWidth, () -> {
            coordsTextField.setText(getCoords(true));
        }));
        currentY += ELEMENT_HEIGHT + SPACING;

        widgetAdder.accept(createButton("Вставить коорд.", x + SPACING, currentY, buttonWidth, () -> {
            if (!coordsTextField.getText().isEmpty()) chatField.write(coordsTextField.getText());
        }));

        widgetAdder.accept(createButton("Вставить позицию", x + SPACING + buttonWidth + SPACING, currentY, buttonWidth, () -> {
            chatField.write(getCoords(false));
        }));
        currentY += ELEMENT_HEIGHT + SPACING;

        widgetAdder.accept(createButton("Вставить текст из поля", x + SPACING, currentY, elemWidth, () -> {
            if (!customTextField.getText().isEmpty()) chatField.write(customTextField.getText());
        }));
        currentY += ELEMENT_HEIGHT + SPACING;

        // 4. Разделитель (декоративный, без действия)
        ButtonWidget divider = ButtonWidget.builder(Text.literal("═══════ Макросы ═══════"), btn -> {})
                .dimensions(x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT)
                .build();
        divider.setFocused(false);
        widgetAdder.accept(divider);
        currentY += ELEMENT_HEIGHT + SPACING;

        // 5. Кнопки создания макросов
        int macroBtnWidth = (elemWidth - SPACING * 2) / 3;

        widgetAdder.accept(createButton("📝 Текст", x + SPACING, currentY, macroBtnWidth, () -> {
            if (customButtons.size() >= MAX_MACROS) return;
            String payload = customTextField.getText();
            if (payload.isEmpty()) payload = "Пусто";
            customButtons.add(new CustomBtn(Action.PASTE_TEXT, payload));
            saveMacros();
            reloadScreen(chatField);
        }));

        widgetAdder.accept(createButton("📌 Коорд.", x + SPACING + macroBtnWidth + SPACING, currentY, macroBtnWidth, () -> {
            if (customButtons.size() >= MAX_MACROS) return;
            customButtons.add(new CustomBtn(Action.PASTE_COORDS, ""));
            saveMacros();
            reloadScreen(chatField);
        }));

        widgetAdder.accept(createButton("📍 Позиция", x + SPACING + (macroBtnWidth + SPACING) * 2, currentY, macroBtnWidth, () -> {
            if (customButtons.size() >= MAX_MACROS) return;
            customButtons.add(new CustomBtn(Action.PASTE_POS, ""));
            saveMacros();
            reloadScreen(chatField);
        }));
        currentY += ELEMENT_HEIGHT + SPACING;

        // 6. Кастомные кнопки
        for (int i = 0; i < customButtons.size(); i++) {
            CustomBtn cb = customButtons.get(i);
            int finalI = i;

            ButtonWidget actionBtn = createButton(cb.getLabel(), x + SPACING, currentY, elemWidth - 24, () -> cb.execute(chatField));
            widgetAdder.accept(actionBtn);

            ButtonWidget deleteBtn = createButton("✕", x + SPACING + elemWidth - 24, currentY, 20, () -> {
                customButtons.remove(finalI);
                saveMacros();
                reloadScreen(chatField);
            });
            widgetAdder.accept(deleteBtn);

            currentY += ELEMENT_HEIGHT + SPACING;
        }
    }

    public static void drawBackground(DrawContext context, int screenWidth, int screenHeight) {
        int panelHeight = getPanelHeight();
        int x = screenWidth - PANEL_WIDTH - MARGIN_RIGHT;
        int y = screenHeight - MARGIN_BOTTOM - panelHeight;

        context.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xCC0A0A0A);

        int borderColor = 0xFF555555;
        context.fill(x, y, x + PANEL_WIDTH, y + 1, borderColor);
        context.fill(x, y + panelHeight - 1, x + PANEL_WIDTH, y + panelHeight, borderColor);
        context.fill(x, y, x + 1, y + panelHeight, borderColor);
        context.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + panelHeight, borderColor);
        context.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + 2, 0x33222222);
    }

    private static String getCoords(boolean divideIfOverworld) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return "0 0 0";

        int x = client.player.getBlockX();
        int y = client.player.getBlockY();
        int z = client.player.getBlockZ();

        boolean isNether = client.world.getRegistryKey() == World.NETHER;
        if (divideIfOverworld && !isNether) {
            x /= 8;
            z /= 8;
        }
        return x + " " + y + " " + z;
    }

    // --- КЛАССЫ ДЛЯ СИСТЕМЫ МАКРОСОВ ---
    private enum Action {
        PASTE_TEXT("Вставить текст"),
        PASTE_COORDS("Вставить коорд."),
        PASTE_POS("Вставить позицию");

        final String displayName;

        Action(String displayName) {
            this.displayName = displayName;
        }
    }

    private static class CustomBtn {
        Action action;
        String payload;

        CustomBtn(Action action, String payload) {
            this.action = action;
            this.payload = payload;
        }

        String getLabel() {
            if (action == Action.PASTE_TEXT) {
                String t = payload;
                if (t.length() > 14) t = t.substring(0, 14) + "...";
                return "📝 " + t;
            } else if (action == Action.PASTE_COORDS) {
                return "📌 Координаты";
            } else {
                return "📍 Позиция";
            }
        }

        void execute(TextFieldWidget chatField) {
            switch (action) {
                case PASTE_TEXT:
                    chatField.write(payload);
                    break;
                case PASTE_COORDS:
                    if (!savedCoords.isEmpty()) chatField.write(savedCoords);
                    break;
                case PASTE_POS:
                    chatField.write(getCoords(false));
                    break;
            }
        }
    }
}