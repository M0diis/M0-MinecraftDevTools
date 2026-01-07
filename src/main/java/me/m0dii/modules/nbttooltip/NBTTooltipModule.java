package me.m0dii.modules.nbttooltip;

import me.m0dii.modules.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBTTooltipModule extends Module {

    public static final NBTTooltipModule INSTANCE = new NBTTooltipModule();

    private NBTTooltipModule() {
        super("nbt_tooltip", "NBT Tooltip", false);
    }

    @Override
    public void register() {
        // Enabled via ClickGUI
    }

    public static List<Text> getNbtTooltipText(ItemStack itemStack, List<Text> list) {
        List<Text> temp = new ArrayList<>();
        temp.add(Text.translatable("item.nbt_tags", itemStack.manager$getNbt().getKeys().size()).formatted(Formatting.DARK_GRAY));
        int index = list.indexOf(temp.getFirst());
        int indexInsertLocation = index;
        if (index >= 0) {
            list.remove(index);
        }

        String nbtList = String.valueOf(itemStack.manager$getNbt());
        Pattern p = Pattern.compile("[{}:\"\\[\\],']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(nbtList);

        int lineStep = 70;
        int currentLineLength = 0;
        MutableText mutableText = Text.empty();
        mutableText.append(Text.translatable("m0-dev-tools.nbt_tooltip").formatted(Formatting.DARK_GRAY));
        list.add(mutableText);
        mutableText = Text.literal("");
        currentLineLength += mutableText.getString().length();

        Formatting stringColor = Formatting.GREEN;
        Formatting quotationColor = Formatting.WHITE;
        Formatting separationColor = Formatting.WHITE;
        Formatting integerColor = Formatting.GOLD;
        Formatting typeColor = Formatting.RED;
        Formatting fieldColor = Formatting.AQUA;
        Formatting lstringColor = Formatting.YELLOW;

        int lastIndex = 0;
        Boolean singleQuotationMark = Boolean.FALSE;
        String lastString = "";

        while (m.find()) {
            int segmentLength = m.start() - lastIndex;
            if (currentLineLength + segmentLength > lineStep) {
                if (indexInsertLocation >= 0 && indexInsertLocation <= list.size()) {
                    list.add(indexInsertLocation, mutableText);
                    indexInsertLocation += 1;
                } else {
                    list.add(mutableText);
                }
                mutableText = Text.literal(" ");
                currentLineLength = 0;
            }
            if (nbtList.charAt(m.start()) == '\'') {
                if (singleQuotationMark.equals(Boolean.FALSE)) {
                    mutableText.append(Text.literal(String.valueOf(nbtList.charAt(m.start()))).formatted(quotationColor));
                    currentLineLength += 1;
                    singleQuotationMark = Boolean.TRUE;
                } else {
                    mutableText.append(Text.literal(nbtList.substring(lastIndex + 1, m.start())).formatted(stringColor));
                    currentLineLength += segmentLength;
                    mutableText.append(Text.literal(String.valueOf(nbtList.charAt(m.start()))).formatted(quotationColor));
                    currentLineLength += 1;
                    singleQuotationMark = Boolean.FALSE;
                }
                lastString = String.valueOf(nbtList.charAt(m.start()));
                lastIndex = m.start();
            }

            if (!singleQuotationMark) {
                if (nbtList.charAt(m.start()) == '{' || nbtList.charAt(m.start()) == '[') {
                    mutableText.append(Text.literal(String.valueOf(nbtList.charAt(m.start()))).formatted(separationColor));
                    currentLineLength += 1;
                    lastString = String.valueOf(nbtList.charAt(m.start()));
                    lastIndex = m.start();
                }

                if (nbtList.charAt(m.start()) == '}' || nbtList.charAt(m.start()) == ']' || nbtList.charAt(m.start()) == ',') {
                    if (nbtList.charAt(m.start() - 1) == 's' || nbtList.charAt(m.start() - 1) == 'S' ||
                            nbtList.charAt(m.start() - 1) == 'b' || nbtList.charAt(m.start() - 1) == 'B' ||
                            nbtList.charAt(m.start() - 1) == 'l' || nbtList.charAt(m.start() - 1) == 'L' ||
                            nbtList.charAt(m.start() - 1) == 'f' || nbtList.charAt(m.start() - 1) == 'F') {
                        mutableText.append(Text.literal(nbtList.substring(lastIndex + 1, m.start() - 1)).formatted(integerColor));
                        currentLineLength += (m.start() - 1 - (lastIndex + 1));
                        mutableText.append(Text.literal(nbtList.substring(m.start() - 1, m.start())).formatted(typeColor));
                        currentLineLength += 1;
                    } else {
                        mutableText.append(Text.literal(nbtList.substring(lastIndex + 1, m.start())).formatted(integerColor));
                        currentLineLength += segmentLength;
                    }
                    mutableText.append(Text.literal(String.valueOf(nbtList.charAt(m.start())))).formatted(separationColor);
                    currentLineLength += 1;
                    if (nbtList.charAt(m.start()) == ',') {
                        mutableText.append(Text.literal(" ").formatted(separationColor));
                        currentLineLength += 1;
                    }
                    lastString = String.valueOf(nbtList.charAt(m.start()));
                    lastIndex = m.start();
                }

                if (nbtList.charAt(m.start()) == ':') {
                    if (!lastString.equals("\"")) {
                        mutableText.append(Text.literal(nbtList.substring(lastIndex + 1, m.start())).formatted(fieldColor));
                        currentLineLength += segmentLength;
                        mutableText.append((Text.literal(String.valueOf(nbtList.charAt(m.start())))).formatted(separationColor));
                        currentLineLength += 1;
                        mutableText.append(Text.literal(" ").formatted(separationColor));
                        currentLineLength += 1;
                        lastString = String.valueOf(nbtList.charAt(m.start()));
                        lastIndex = m.start();
                    }
                }

                if (nbtList.charAt(m.start()) == '"') {
                    if (lastString.equals("\"")) {
                        if (currentLineLength + (m.start() - lineStep) > lineStep) {
                            mutableText.append(Text.literal("....").formatted(lstringColor));
                            currentLineLength += 4;
                        } else {
                            mutableText.append(Text.literal(nbtList.substring(lastIndex + 1, m.start())).formatted(stringColor));
                            currentLineLength += segmentLength;
                        }

                        mutableText.append(Text.literal(String.valueOf(nbtList.charAt(m.start()))).formatted(quotationColor));
                        currentLineLength += 1;
                    } else {
                        mutableText.append(Text.literal(String.valueOf(nbtList.charAt(m.start()))).formatted(quotationColor));
                        currentLineLength += 1;
                    }
                    lastString = String.valueOf(nbtList.charAt(m.start()));
                    lastIndex = m.start();

                }
            }
        }
        if (!mutableText.getString().isEmpty()) {
            if (indexInsertLocation >= 0 && indexInsertLocation <= list.size()) {
                list.add(indexInsertLocation, mutableText);
            } else {
                list.add(mutableText);
            }
        }

        return list;
    }
}
