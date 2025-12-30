package me.m0dii.nbteditor.multiversion;

import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

public class IdentifierInst {

    private IdentifierInst() {
    }

    public static Identifier of(String id) throws InvalidIdentifierException {
        return Identifier.of(id);
    }

    public static Identifier of(String namespace, String path) throws InvalidIdentifierException {
        return Identifier.of(namespace, path);
    }

    public static boolean isValid(String id) {
        try {
            of(id);
            return true;
        } catch (InvalidIdentifierException e) {
            return false;
        }
    }

}
