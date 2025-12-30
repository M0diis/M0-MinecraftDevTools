package me.m0dii.nbteditor.multiversion;

import lombok.Getter;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

public class MVQuaternionf {

    @Getter
    private final Quaternionf value;

    public MVQuaternionf(Quaternionf value) {
        this.value = value;
    }

    public static MVQuaternionf ofAxisRotation(float angle, float x, float y, float z) {
        Quaternionf quat = new Quaternionf();
        quat.rotationAxis(angle, x, y, z);
        return new MVQuaternionf(quat);
    }

    public static MVQuaternionf ofXRotation(float angle) {
        return ofAxisRotation(angle, 1, 0, 0);
    }

    public static MVQuaternionf ofYRotation(float angle) {
        return ofAxisRotation(angle, 0, 1, 0);
    }

    public static MVQuaternionf ofZRotation(float angle) {
        return ofAxisRotation(angle, 0, 0, 1);
    }

    public MVQuaternionf multiply(MVQuaternionf right) {
        value.mul(right.getValue());

        return this;
    }

    public MVQuaternionf rotateAxis(float angle, float x, float y, float z) {
        return multiply(ofAxisRotation(angle, x, y, z));
    }

    public MVQuaternionf rotateX(float angle) {
        return multiply(ofXRotation(angle));
    }

    public MVQuaternionf rotateY(float angle) {
        return multiply(ofYRotation(angle));
    }

    public MVQuaternionf rotateZ(float angle) {
        return multiply(ofZRotation(angle));
    }

    public MVQuaternionf conjugate() {
        value.conjugate();
        return this;
    }

    public MVQuaternionf copy() {
        return new MVQuaternionf(new Quaternionf(value));
    }

    public void applyToMatrixStack(MatrixStack matrices) {
        matrices.multiply(value);
    }

    public void applyToEntityRenderDispatcher(EntityRenderDispatcher dispatcher) {
        dispatcher.setRotation(value);
    }

}
