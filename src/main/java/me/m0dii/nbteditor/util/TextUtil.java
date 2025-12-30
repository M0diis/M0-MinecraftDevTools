package me.m0dii.nbteditor.util;

import com.google.gson.JsonParseException;
import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.fancytext.FancyText;
import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.EditableText;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.screens.util.FancyConfirmScreen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.text.StringVisitable.StyledVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TextUtil {

    private TextUtil() {
    }

    public static List<Text> getLongTranslatableTextLines(String key) {
        List<Text> lines = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Text line = TextInst.translatable(key + "_" + i);
            String str = line.getString();
            if (str.equals(key + "_" + i)) {
                break;
            }

            if (str.startsWith("[LINK] ")) {
                String url = str.substring("[LINK] ".length());
                line = TextInst.literal(url).styled(style -> style.withClickEvent(new ClickEvent(Action.OPEN_URL, url))
                        .withUnderline(true).withItalic(true).withColor(Formatting.GOLD));
            }
            if (str.startsWith("[FORMAT] ")) {
                String toFormat = str.substring("[FORMAT] ".length());
                line = FancyText.parse(toFormat);
            }
            lines.add(line);
        }
        return lines;
    }

    public static Text getLongTranslatableText(String key) {
        List<Text> lines = getLongTranslatableTextLines(key);
        if (lines.isEmpty()) {
            return TextInst.of(key);
        }
        EditableText output = TextInst.copy(lines.getFirst());
        for (int i = 1; i < lines.size(); i++) {
            output.append("\n").append(lines.get(i));
        }
        return output;
    }

    public static Text parseTranslatableFormatted(String key, Object... args) {
        return FancyText.parse(TextInst.translatable(key, args).getString());
    }

    public static Text substring(Text text, int start, int end) {
        EditableText output = TextInst.literal("");
        text.visit(new StyledVisitor<Boolean>() {
            private int i;

            @Override
            public Optional<Boolean> accept(Style style, String str) {
                if (i + str.length() <= start) {
                    i += str.length();
                    return Optional.empty();
                }
                if (i >= start) {
                    if (end >= 0 && i + str.length() > end) {
                        return accept(style, str.substring(0, end - i));
                    }
                    output.append(TextInst.literal(str).fillStyle(style));
                    i += str.length();
                    if (end >= 0 && i == end) {
                        return Optional.of(true);
                    }
                } else {
                    str = str.substring(start - i);
                    i = start;
                    accept(style, str);
                }
                return Optional.empty();
            }
        }, Style.EMPTY);
        return output;
    }

    public static Text substring(Text text, int start) {
        return substring(text, start, -1);
    }

    public static Text deleteCharAt(Text text, int index) {
        EditableText output = TextInst.literal("");
        AtomicInteger pos = new AtomicInteger(0);
        text.visit((style, str) -> {
            int strLen = str.length();
            if (pos.getPlain() <= index && index < pos.getPlain() + strLen) {
                str = new StringBuilder(str).deleteCharAt(index - pos.getPlain()).toString();
            }
            if (!str.isEmpty()) {
                output.append(TextInst.literal(str).setStyle(style));
            }
            pos.setPlain(pos.getPlain() + strLen);
            return Optional.empty();
        }, Style.EMPTY);
        return output;
    }

    public static Text joinLines(List<Text> lines) {
        EditableText output = TextInst.literal("");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                output.append("\n");
            }
            output.append(lines.get(i));
        }
        return output;
    }

    public static List<Text> splitText(Text text) {
        List<Text> output = new ArrayList<>();
        int i;
        while ((i = text.getString().indexOf('\n')) != -1) {
            output.add(substring(text, 0, i));
            text = substring(text, i + 1);
        }
        output.add(text);
        return output;
    }

    public static Text stripInvalidChars(Text text, boolean allowLineBreaks) {
        EditableText output = TextInst.literal("");
        text.visit((style, str) -> {
            output.append(TextInst.literal(MVMisc.stripInvalidChars(str, allowLineBreaks)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return output;
    }

    public static Text attachFileTextOptions(EditableText link, File file) {
        return link.append(" ").append(TextInst.translatable("nbteditor.file_options.show").styled(style ->
                        style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                                file.getAbsoluteFile().getParentFile().getAbsolutePath()))))
                .append(" ").append(TextInst.translatable("nbteditor.file_options.delete").styled(style ->
                        MixinLink.withRunClickEvent(style, () -> MiscUtil.client.setScreen(
                                new FancyConfirmScreen(confirmed -> {
                                    if (confirmed) {
                                        if (file.exists()) {
                                            try {
                                                Files.deleteIfExists(file.toPath());
                                                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.file_options.delete.success", "§6" + file.getName()), false);
                                            } catch (IOException e) {
                                                M0DevTools.LOGGER.error("Error deleting file", e);
                                                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.file_options.delete.error", "§6" + file.getName()), false);
                                            }
                                        } else {
                                            MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.file_options.delete.missing", "§6" + file.getName()), false);
                                        }
                                    }
                                    MiscUtil.client.setScreen(null);
                                }, TextInst.translatable("nbteditor.file_options.delete.title", file.getName()),
                                        TextInst.translatable("nbteditor.file_options.delete.desc", file.getName()))))));
    }

    public static int lastIndexOf(Text text, int ch) {
        AtomicInteger output = new AtomicInteger(-1);
        AtomicInteger pos = new AtomicInteger(0);
        text.visit(str -> {
            int i = str.lastIndexOf(ch);
            if (i != -1) {
                output.setPlain(pos.getPlain() + i);
            }
            pos.setPlain(pos.getPlain() + str.length());
            return Optional.empty();
        });
        return output.getPlain();
    }

    public static Text fromJsonSafely(String json) {
        try {
            Text output = TextInst.fromJson(json);
            if (output != null) {
                return output;
            }
        } catch (JsonParseException ignored) {
        }
        return TextInst.of(json);
    }

}
