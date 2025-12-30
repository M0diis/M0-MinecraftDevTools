package me.m0dii.nbteditor.multiversion;

import lombok.Getter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class MVMatrix4f {

    @Getter
    private final Matrix4f value;

    public MVMatrix4f(Matrix4f value) {
        this.value = value;
    }

    public MVMatrix4f(MVQuaternionf quat) {
        this(toMatrix4f(quat));
    }

    public static MVMatrix4f getPositionMatrix(MatrixStack.Entry matrix) {
        return new MVMatrix4f(matrix.getPositionMatrix());
    }

    public static float[] getTranslation(MatrixStack matrices) {
        Object matrix = getPositionMatrix(matrices.peek()).getValue();
        Vector3f output = ((Matrix4f) matrix).getColumn(3, new Vector3f());
        return new float[]{output.x, output.y, output.z};
    }

    public static MVMatrix4f ofScale(float x, float y, float z) {
        Matrix4f matrix = new Matrix4f();
        matrix.scale(x, y, z);
        return new MVMatrix4f(matrix);
    }


    private static Matrix4f toMatrix4f(MVQuaternionf quat) {
        Matrix4f matrix = new Matrix4f();
        matrix.set(quat.getValue());
        return matrix;
    }

    public MVMatrix4f multiply(MVMatrix4f right) {
        value.mul((Matrix4f) right.getValue());
        return this;
    }

    public MVMatrix4f multiply(MVQuaternionf right) {
        return multiply(new MVMatrix4f(right));
    }

    public MVMatrix4f scale(float x, float y, float z) {
        return multiply(ofScale(x, y, z));
    }

    public MVMatrix4f copy() {
        Matrix4f newMatrix = new Matrix4f();
        value.get(newMatrix);
        return new MVMatrix4f(newMatrix);
    }

    public void applyToPositionMatrix(MatrixStack matrices) {
        matrices.multiplyPositionMatrix(value);
    }

    public VertexConsumer applyToVertex(VertexConsumer buffer, float x, float y, float z) {
        return buffer.vertex(value, x, y, z);
    }

}
