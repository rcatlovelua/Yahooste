package com.yahoostee.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

    private static final Logger LOGGER =
            Logger.getLogger("YahoosteePanel");

    private static final int PANEL_WIDTH = 230;
    private static final int ELEMENT_HEIGHT = 20;
    private static final int SPACING = 4;
    private static final int MARGIN_RIGHT = 6;
    private static final int MARGIN_BOTTOM = 90;
    private static final int MAX_MACROS = 5;
    private static final int FIXED_ROWS = 7;

    private static String savedCoords = "";
    private static String currentCustomText = "";

    private static final List<CustomBtn> customButtons =
            new ArrayList<>();

    /*
     * Все элементы панели.
     * Нужны для определения того, получил ли фокус
     * какой-нибудь элемент панели.
     */
    private static final java.util.Set<ClickableWidget> panelWidgets =
            java.util.Collections.newSetFromMap(
                    new java.util.IdentityHashMap<>()
            );

    public static boolean isPanelWidgetFocused(
            net.minecraft.client.gui.screen.Screen screen
    ) {
        var focused = screen.getFocused();

        return focused instanceof ClickableWidget widget
                && panelWidgets.contains(widget);
    }

    // =========================================================
    // ФАЙЛЫ
    // =========================================================

    private static Path getSavePath() {
        return MinecraftClient.getInstance()
                .runDirectory
                .toPath()
                .resolve("yahoostee_nether_coords.txt");
    }

    private static Path getMacrosPath() {
        return MinecraftClient.getInstance()
                .runDirectory
                .toPath()
                .resolve("yahoostee_macros.txt");
    }

    // =========================================================
    // ЗАГРУЗКА
    // =========================================================

    private static void loadFiles() {
        try {
            Path coordsPath = getSavePath();

            if (Files.exists(coordsPath)) {
                savedCoords =
                        Files.readString(coordsPath).trim();
            }

            customButtons.clear();

            Path macrosPath = getMacrosPath();

            if (Files.exists(macrosPath)) {
                List<String> lines =
                        Files.readAllLines(macrosPath);

                for (String line : lines) {

                    String[] parts =
                            line.split("\\|", 2);

                    if (parts.length == 2) {
                        try {
                            customButtons.add(
                                    new CustomBtn(
                                            Action.valueOf(parts[0]),
                                            parts[1]
                                    )
                            );
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.warning(
                    "YahoosteePanel: ошибка загрузки файлов: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // СОХРАНЕНИЕ
    // =========================================================

    private static void saveCoords(String coords) {

        savedCoords = coords;

        try {
            Files.writeString(
                    getSavePath(),
                    coords
            );
        } catch (Exception e) {
            LOGGER.warning(
                    "YahoosteePanel: не удалось сохранить координаты: "
                            + e.getMessage()
            );
        }
    }

    private static void saveMacros() {

        try {
            List<String> lines =
                    new ArrayList<>();

            for (CustomBtn btn : customButtons) {
                lines.add(
                        btn.action.name()
                                + "|"
                                + btn.payload
                );
            }

            Files.write(
                    getMacrosPath(),
                    lines
            );

        } catch (Exception e) {
            LOGGER.warning(
                    "YahoosteePanel: не удалось сохранить макросы: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // РАЗМЕР ПАНЕЛИ
    // =========================================================

    private static int getPanelHeight() {

        int totalElements =
                FIXED_ROWS + customButtons.size();

        return totalElements * ELEMENT_HEIGHT
                + (totalElements + 1) * SPACING;
    }

    // =========================================================
    // ПЕРЕЗАГРУЗКА
    // =========================================================

    private static void reloadScreen(
            TextFieldWidget chatField
    ) {
        String currentChat =
                chatField.getText();

        MinecraftClient.getInstance()
                .setScreen(
                        new ChatScreen(
                                currentChat,
                                false
                        )
                );
    }

    // =========================================================
    // КНОПКИ
    // =========================================================

    private static ButtonWidget createButton(
            String text,
            int x,
            int y,
            int width,
            Runnable action
    ) {

        ButtonWidget button =
                ButtonWidget.builder(
                        Text.literal(text),
                        btn -> action.run()
                )
                .dimensions(
                        x,
                        y,
                        width,
                        ELEMENT_HEIGHT
                )
                .build();

        /*
         * Кнопка панели никогда не должна
         * становиться фокусом.
         */
        button.setFocused(false);

        panelWidgets.add(button);

        return button;
    }

    // =========================================================
    // GUI
    // =========================================================

    public static void init(
            ChatScreen screen,
            int screenWidth,
            int screenHeight,
            Consumer<ClickableWidget> widgetAdder,
            TextFieldWidget chatField
    ) {

        loadFiles();

        panelWidgets.clear();

        int panelHeight =
                getPanelHeight();

        int x =
                screenWidth
                        - PANEL_WIDTH
                        - MARGIN_RIGHT;

        int y =
                screenHeight
                        - MARGIN_BOTTOM
                        - panelHeight;

        int currentY =
                y + SPACING;

        int elemWidth =
                PANEL_WIDTH
                        - SPACING * 2;

        int buttonWidth =
                (elemWidth - SPACING) / 2;

        // =====================================================
        // КАСТОМНЫЙ ТЕКСТ
        // =====================================================

        NoNavigationTextField customTextField =
                new NoNavigationTextField(
                        MinecraftClient.getInstance()
                                .textRenderer,

                        x + SPACING,
                        currentY,

                        elemWidth,
                        ELEMENT_HEIGHT,

                        Text.empty()
                );

        customTextField.setMaxLength(256);

        customTextField.setPlaceholder(
                Text.literal(
                        "Кастомный текст..."
                )
        );

        customTextField.setText(
                currentCustomText
        );

        widgetAdder.accept(
                customTextField
        );

        panelWidgets.add(
                customTextField
        );

        currentY +=
                ELEMENT_HEIGHT + SPACING;

        // =====================================================
        // КООРДИНАТЫ
        // =====================================================

        NoNavigationTextField coordsTextField =
                new NoNavigationTextField(
                        MinecraftClient.getInstance()
                                .textRenderer,

                        x + SPACING,
                        currentY,

                        elemWidth,
                        ELEMENT_HEIGHT,

                        Text.empty()
                );

        coordsTextField.setMaxLength(100);

        coordsTextField.setPlaceholder(
                Text.literal(
                        "Координаты для отправки..."
                )
        );

        coordsTextField.setText(
                savedCoords
        );

        widgetAdder.accept(
                coordsTextField
        );

        panelWidgets.add(
                coordsTextField
        );

        currentY +=
                ELEMENT_HEIGHT + SPACING;

        // =====================================================
        // ЗАПИСАТЬ ПОЗИЦИЮ
        // =====================================================

        widgetAdder.accept(
                createButton(
                        "Записать позицию",

                        x + SPACING,
                        currentY,

                        elemWidth,

                        () -> {

                            String coords =
                                    getCoords(true);

                            coordsTextField
                                    .setText(coords);

                            saveCoords(coords);
                        }
                )
        );

        currentY +=
                ELEMENT_HEIGHT + SPACING;

        // =====================================================
        // ВСТАВИТЬ КООРДИНАТЫ
        // =====================================================

        widgetAdder.accept(
                createButton(
                        "Вставить коорд.",

                        x + SPACING,
                        currentY,

                        buttonWidth,

                        () -> {

                            if (!coordsTextField
                                    .getText()
                                    .isEmpty()) {

                                chatField.write(
                                        coordsTextField
                                                .getText()
                                );
                            }
                        }
                )
        );

        // =====================================================
        // ВСТАВИТЬ ПОЗИЦИЮ
        // =====================================================

        widgetAdder.accept(
                createButton(
                        "Вставить позицию",

                        x + SPACING
                                + buttonWidth
                                + SPACING,

                        currentY,

                        buttonWidth,

                        () -> {
                            chatField.write(
                                    getCoords(false)
                            );
                        }
                )
        );

        currentY +=
                ELEMENT_HEIGHT + SPACING;

        // =====================================================
        // ВСТАВИТЬ ТЕКСТ
        // =====================================================

        widgetAdder.accept(
                createButton(
                        "Вставить текст из поля",

                        x + SPACING,
                        currentY,

                        elemWidth,

                        () -> {

                            if (!customTextField
                                    .getText()
                                    .isEmpty()) {

                                chatField.write(
                                        customTextField
                                                .getText()
                                );
                            }
                        }
                )
        );

        currentY +=
                ELEMENT_HEIGHT + SPACING;

        // =====================================================
        // РАЗДЕЛИТЕЛЬ
        // =====================================================

        ButtonWidget divider =
                ButtonWidget.builder(
                        Text.literal(
                                "═══════ Макросы ═══════"
                        ),
                        btn -> {}
                )
                .dimensions(
                        x + SPACING,
                        currentY,
                        elemWidth,
                        ELEMENT_HEIGHT
                )
                .build();

        divider.setFocused(false);

        panelWidgets.add(divider);

        widgetAdder.accept(divider);

        currentY +=
                ELEMENT_HEIGHT + SPACING;

        // =====================================================
        // СОЗДАНИЕ МАКРОСОВ
        // =====================================================

        int macroBtnWidth =
                (elemWidth - SPACING * 2) / 3;

        // ТЕКСТ

        widgetAdder.accept(
                createButton(
                        "📝 Текст",

                        x + SPACING,
                        currentY,

                        macroBtnWidth,

                        () -> {

                            if (customButtons.size()
                                    >= MAX_MACROS) {
                                return;
                            }

                            String payload =
                                    customTextField
                                            .getText();

                            if (payload.isEmpty()) {
                                payload = "Пусто";
                            }

                            customButtons.add(
                                    new CustomBtn(
                                            Action.PASTE_TEXT,
                                            payload
                                    )
                            );

                            saveMacros();

                            reloadScreen(
                                    chatField
                            );
                        }
                )
        );

        // КООРДИНАТЫ

        widgetAdder.accept(
                createButton(
                        "📌 Коорд.",

                        x + SPACING
                                + macroBtnWidth
                                + SPACING,

                        currentY,

                        macroBtnWidth,

                        () -> {

                            if (customButtons.size()
                                    >= MAX_MACROS) {
                                return;
                            }

                            customButtons.add(
                                    new CustomBtn(
                                            Action.PASTE_COORDS,
                                            ""
                                    )
                            );

                            saveMacros();

                            reloadScreen(
                                    chatField
                            );
                        }
                )
        );

        // ПОЗИЦИЯ

        widgetAdder.accept(
                createButton(
                        "📍 Позиция",

                        x + SPACING
                                + (macroBtnWidth
                                + SPACING) * 2,

                        currentY,

                        macroBtnWidth,

                        () -> {

                            if (customButtons.size()
                                    >= MAX_MACROS) {
                                return;
                            }

                            customButtons.add(
                                    new CustomBtn(
                                            Action.PASTE_POS,
                                            ""
                                    )
                            );

                            saveMacros();

                            reloadScreen(
                                    chatField
                            );
                        }
                )
        );

        currentY +=
                ELEMENT_HEIGHT + SPACING;

        // =====================================================
        // КАСТОМНЫЕ МАКРОСЫ
        // =====================================================

        for (int i = 0;
             i < customButtons.size();
             i++) {

            CustomBtn cb =
                    customButtons.get(i);

            int finalI = i;

            // Основная кнопка

            ButtonWidget actionBtn =
                    createButton(
                            cb.getLabel(),

                            x + SPACING,
                            currentY,

                            elemWidth - 24,

                            () -> cb.execute(
                                    chatField
                            )
                    );

            widgetAdder.accept(
                    actionBtn
            );

            // Удаление

            ButtonWidget deleteBtn =
                    createButton(
                            "✕",

                            x + SPACING
                                    + elemWidth
                                    - 24,

                            currentY,

                            20,

                            () -> {

                                customButtons
                                        .remove(finalI);

                                saveMacros();

                                reloadScreen(
                                        chatField
                                );
                            }
                    );

            widgetAdder.accept(
                    deleteBtn
            );

            currentY +=
                    ELEMENT_HEIGHT + SPACING;
        }

        /*
         * КРИТИЧНО:
         *
         * После создания всех элементов
         * снова возвращаем фокус в чат.
         *
         * Поэтому стрелки и Enter
         * больше не переключают элементы панели.
         */
        screen.setFocused(chatField);
    }

    // =========================================================
    // ФОН
    // =========================================================

    public static void drawBackground(
            DrawContext context,
            int screenWidth,
            int screenHeight
    ) {

        int panelHeight =
                getPanelHeight();

        int x =
                screenWidth
                        - PANEL_WIDTH
                        - MARGIN_RIGHT;

        int y =
                screenHeight
                        - MARGIN_BOTTOM
                        - panelHeight;

        context.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + panelHeight,
                0xCC0A0A0A
        );

        int borderColor =
                0xFF555555;

        context.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + 1,
                borderColor
        );

        context.fill(
                x,
                y + panelHeight - 1,
                x + PANEL_WIDTH,
                y + panelHeight,
                borderColor
        );

        context.fill(
                x,
                y,
                x + 1,
                y + panelHeight,
                borderColor
        );

        context.fill(
                x + PANEL_WIDTH - 1,
                y,
                x + PANEL_WIDTH,
                y + panelHeight,
                borderColor
        );

        context.fill(
                x + 1,
                y + 1,
                x + PANEL_WIDTH - 1,
                y + 2,
                0x33222222
        );
    }

    // =========================================================
    // КООРДИНАТЫ
    // =========================================================

    private static String getCoords(
            boolean divideIfOverworld
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null
                || client.world == null) {

            return "0 0 0";
        }

        int x =
                client.player.getBlockX();

        int y =
                client.player.getBlockY();

        int z =
                client.player.getBlockZ();

        boolean isNether =
                client.world.getRegistryKey()
                        == World.NETHER;

        if (divideIfOverworld
                && !isNether) {

            x /= 8;
            z /= 8;
        }

        return x + " " + y + " " + z;
    }

    // =========================================================
    // NO NAVIGATION TEXT FIELD
    // =========================================================

    /*
     * Это главное исправление.
     *
     * Стрелки:
     * ← ↑ ↓ →
     *
     * Enter
     *
     * НЕ используются Minecraft-ом
     * для навигации панели.
     *
     * Остальной ввод текста работает нормально.
     */
    private static class NoNavigationTextField extends TextFieldWidget {

    public NoNavigationTextField(
            net.minecraft.client.font.TextRenderer textRenderer,
            int x,
            int y,
            int width,
            int height,
            Text text
    ) {
        super(
                textRenderer,
                x,
                y,
                width,
                height,
                text
        );
    }

    
}

    // =========================================================
    // МАКРОСЫ
    // =========================================================

    private enum Action {

        PASTE_TEXT("Вставить текст"),

        PASTE_COORDS("Вставить коорд."),

        PASTE_POS("Вставить позицию");

        final String displayName;

        Action(String displayName) {
            this.displayName =
                    displayName;
        }
    }

    private static class CustomBtn {

        Action action;
        String payload;

        CustomBtn(
                Action action,
                String payload
        ) {

            this.action = action;
            this.payload = payload;
        }

        String getLabel() {

            if (action
                    == Action.PASTE_TEXT) {

                String t = payload;

                if (t.length() > 14) {
                    t = t.substring(0, 14)
                            + "...";
                }

                return "📝 " + t;

            } else if (
                    action
                            == Action.PASTE_COORDS
            ) {

                return "📌 Координаты";

            } else {

                return "📍 Позиция";
            }
        }

        void execute(
                TextFieldWidget chatField
        ) {

            switch (action) {

                case PASTE_TEXT:

                    chatField.write(
                            payload
                    );

                    break;

                case PASTE_COORDS:

                    if (!savedCoords.isEmpty()) {

                        chatField.write(
                                savedCoords
                        );
                    }

                    break;

                case PASTE_POS:

                    chatField.write(
                            getCoords(false)
                    );

                    break;
            }
        }
    }
}