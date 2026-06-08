package me.m0dii.modules.getdata;

public interface GetDataTargetView {
    boolean matchesTarget(String token);

    void applySyncedPayload(String payload);
}
