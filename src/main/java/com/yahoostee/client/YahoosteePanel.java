package com.yahoostee.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class YahoosteePanel {
    // Размеры и отступы
    private static final int PANEL_WIDTH = 210;
    private static final int ELEMENT_HEIGHT = 20; 
    private static final int SPACING = 4;
    private static final int MARGIN_RIGHT = 6;
    private static final int MARGIN_BOTTOM = 90; // Поднято над субтитрами

    // Внутреннее состояние панели
    private static String savedCoords = "";
    private static String currentCustomText = ""; // Сохраняем текст верхнего поля при перезагрузке UI
    private static int currentActionIndex = 0;
    
    private static final List<CustomBtn> customButtons = new ArrayList<>();

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
            // Координаты
            Path coordsPath = getSavePath();
            if (Files.exists(coordsPath)) {
                savedCoords = Files.readString(coordsPath).trim();
            }

            // Макросы (Кастомные кнопки)
            customButtons.clear();
            Path macrosPath = getMacrosPath();
            if (Files.exists(macrosPath)) {
                List<String> lines = Files.readAllLines(macrosPath);
                for (String line : lines) {
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        try {
                            customButtons.add(new CustomBtn(Action.valueOf(parts[0]), parts[1]));
                        } catch (Exception ignored) {} // Игнорируем битые строки
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("YahoosteePanel: Ошибка загрузки файлов: " + e.getMessage());
        }
    }

    private static void saveCoords(String coords) {
        savedCoords = coords;
        try { Files.writeString(getSavePath(), coords); } 
        catch (Exception ignored) {}
    }

    private static void saveMacros() {
        try {
            List<String> lines = new ArrayList<>();
            for (CustomBtn btn : customButtons) {
                lines.add(btn.action.name() + "|" + btn.payload);
            }
            Files.write(getMacrosPath(), lines);
        } catch (Exception ignored) {}
    }

    // --- ДИНАМИЧЕСКАЯ ВЫСОТА ---

    private static int getPanelHeight() {
        // 2 текстовых поля + 4 базовые кнопки + 1 строка создания макроса = 7 базовых элементов
        int totalElements = 7 + customButtons.size();
        return totalElements * ELEMENT_HEIGHT + (totalElements + 1) * SPACING;
    }

    // --- ПЕРЕЗАГРУЗКА ИНТЕРФЕЙСА ---
    // Используется для моментального обновления чата при добавлении/удалении кнопок
    private static void reloadScreen(TextFieldWidget chatField) {
        String currentChat = chatField.getText();
        MinecraftClient.getInstance().setScreen(new ChatScreen(currentChat, false));
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

        // 1. Поле для кастомного текста
        TextFieldWidget customTextField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 
                x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT, Text.empty());
        customTextField.setMaxLength(256);
        customTextField.setPlaceholder(Text.literal("Кастомный текст..."));
        customTextField.setText(currentCustomText);
        customTextField.setChangedListener(text -> currentCustomText = text);
        widgetAdder.accept(customTextField);
        currentY += ELEMENT_HEIGHT + SPACING;

        // 2. Поле для СОХРАНЕННЫХ КООРДИНАТ
        TextFieldWidget coordsTextField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 
                x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT, Text.empty());
        coordsTextField.setMaxLength(100);
        coordsTextField.setPlaceholder(Text.literal("Координаты для отправки..."));
        coordsTextField.setText(savedCoords);
        coordsTextField.setChangedListener(YahoosteePanel::saveCoords); 
        widgetAdder.accept(coordsTextField);
        currentY += ELEMENT_HEIGHT + SPACING;

        // 3. Базовые кнопки
        widgetAdder.accept(ButtonWidget.builder(Text.literal("Записать текущие коорд. в поле"), btn -> {
            coordsTextField.setText(getCoords(true));
        }).dimensions(x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT).build());
        currentY += ELEMENT_HEIGHT + SPACING;

        widgetAdder.accept(ButtonWidget.builder(Text.literal("Вставить коорд. из поля в чат"), btn -> {
            if (!coordsTextField.getText().isEmpty()) chatField.write(coordsTextField.getText());
        }).dimensions(x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT).build());
        currentY += ELEMENT_HEIGHT + SPACING;

        widgetAdder.accept(ButtonWidget.builder(Text.literal("Вставить свою текущую позицию"), btn -> {
            chatField.write(getCoords(false));
        }).dimensions(x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT).build());
        currentY += ELEMENT_HEIGHT + SPACING;

        widgetAdder.accept(ButtonWidget.builder(Text.literal("Текст из верхнего поля в чат"), btn -> {
            if (!customTextField.getText().isEmpty()) chatField.write(customTextField.getText());
        }).dimensions(x + SPACING, currentY, elemWidth, ELEMENT_HEIGHT).build());
        currentY += ELEMENT_HEIGHT + SPACING;

        // 4. СТРОКА СОЗДАНИЯ КАСТОМНОЙ КНОПКИ (Цикличная кнопка выбора + Добавление)
        int actionBtnWidth = elemWidth - 24;
        
        ButtonWidget actionCycler = ButtonWidget.builder(Text.literal("Действие: " + Action.values()[currentActionIndex].displayName), btn -> {
            currentActionIndex = (currentActionIndex + 1) % Action.values().length;
            btn.setMessage(Text.literal("Действие: " + Action.values()[currentActionIndex].displayName));
        }).dimensions(x + SPACING, currentY, actionBtnWidth, ELEMENT_HEIGHT).build();
        widgetAdder.accept(actionCycler);

        widgetAdder.accept(ButtonWidget.builder(Text.literal("+"), btn -> {
            if (customButtons.size() >= 5) return; // Лимит в 5 кнопок
            
            String payload = customTextField.getText();
            Action selectedAction = Action.values()[currentActionIndex];
            if (selectedAction == Action.PASTE_TEXT && payload.isEmpty()) {
                payload = "Пусто";
            }
            
            customButtons.add(new CustomBtn(selectedAction, payload));
            saveMacros();
            reloadScreen(chatField); // Перезагружаем интерфейс чтобы кнопка появилась
        }).dimensions(x + SPACING + actionBtnWidth + 4, currentY, 20, ELEMENT_HEIGHT).build());
        currentY += ELEMENT_HEIGHT + SPACING;

        // 5. РЕНДЕР СОХРАНЕННЫХ КАСТОМНЫХ КНОПОК
        for (int i = 0; i < customButtons.size(); i++) {
            CustomBtn cb = customButtons.get(i);
            int finalI = i;

            // Сама кнопка действия
            widgetAdder.accept(ButtonWidget.builder(Text.literal(cb.getLabel()), btn -> {
                cb.execute(chatField);
            }).dimensions(x + SPACING, currentY, actionBtnWidth, ELEMENT_HEIGHT).build());

            // Кнопка удаления [x]
            widgetAdder.accept(ButtonWidget.builder(Text.literal("x"), btn -> {
                customButtons.remove(finalI);
                saveMacros();
                reloadScreen(chatField); // Обновляем экран для скрытия
            }).dimensions(x + SPACING + actionBtnWidth + 4, currentY, 20, ELEMENT_HEIGHT).build());

            currentY += ELEMENT_HEIGHT + SPACING;
        }
    }

    public static void drawBackground(DrawContext context, int screenWidth, int screenHeight) {
        int panelHeight = getPanelHeight();
        int x = screenWidth - PANEL_WIDTH - MARGIN_RIGHT;
        int y = screenHeight - MARGIN_BOTTOM - panelHeight;

        context.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xAA080808);
        
        int borderColor = 0xFF353535;
        context.fill(x, y, x + PANEL_WIDTH, y + 1, borderColor); 
        context.fill(x, y + panelHeight - 1, x + PANEL_WIDTH, y + panelHeight, borderColor); 
        context.fill(x, y, x + 1, y + panelHeight, borderColor); 
        context.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + panelHeight, borderColor); 
    }

    private static String getCoords(boolean divideIfOverworld) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return "0 0 0";

        int x = client.player.getBlockX();
        int y = client.player.getBlockY();
        int z = client.player.getBlockZ();

        boolean isNether = client.world.getRegistryKey() == World.NETHER;
        if (divideIfOverworld && !isNether) {
            x /= 8; z /= 8;
        }
        return x + " " + y + " " + z;
    }

    // --- КЛАССЫ ДЛЯ СИСТЕМЫ МАКРОСОВ ---

    private enum Action {
        PASTE_TEXT("Вставить текст"),
        PASTE_COORDS("Вставить сохр. коорд."),
        PASTE_POS("Вставить свою позицию");

        final String displayName;
        Action(String displayName) { this.displayName = displayName; }
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
                return "Текст: " + t;
            }
            return action.displayName;
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