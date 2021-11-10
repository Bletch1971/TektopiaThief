package bletch.tektopiathief.utils;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class TextUtils {

    public static String translate(String translateKey, Object... translationArgs) {
        if (StringUtils.isNullOrWhitespace(translateKey)) {
            return null;
        }

        ITextComponent itextcomponent = new TextComponentTranslation(translateKey, translationArgs);
        String translate = itextcomponent.getUnformattedText();

        return StringUtils.isNullOrWhitespace(translate) || translate.equalsIgnoreCase(translateKey)
                ? null
                : translate;
    }

}
