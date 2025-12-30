package me.m0dii.nbteditor.multiversion;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MVShaders {

    public static final VertexFormatElement POSITION_ELEMENT = getElement(() -> VertexFormatElement.POSITION);
    public static final VertexFormatElement TEXTURE_ELEMENT = getElement(() -> VertexFormatElement.UV_0);
    public static final VertexFormatElement LIGHT_ELEMENT = getElement(() -> VertexFormatElement.UV_2);

    private static VertexFormatElement getElement(Supplier<VertexFormatElement> newElement) {
        return newElement.get();
    }

    public static VertexFormat createFormat(Consumer<ImmutableMap.Builder<String, VertexFormatElement>> builderConsumer) {
        ImmutableMap.Builder<String, VertexFormatElement> mapBuilder = ImmutableMap.builder();
        builderConsumer.accept(mapBuilder);
        ImmutableMap<String, VertexFormatElement> map = mapBuilder.build();

        VertexFormat.Builder vertexBuilder = VertexFormat.builder();
        map.forEach(vertexBuilder::add);
        return vertexBuilder.build();
    }

    public static RenderPhase.ShaderProgram newRenderPhaseShaderProgram(MVShaderProgram shader) {
        return new RenderPhase.ShaderProgram((ShaderProgramKey) shader.key.mcKey());
    }

    public record MVShaderProgramKey(String name, VertexFormat vertexFormat, Object mcKey) {
        public MVShaderProgramKey(String name, VertexFormat vertexFormat) {
            this(name, vertexFormat, new ShaderProgramKey(IdentifierInst.of("minecraft", "core/" + name), vertexFormat, Defines.EMPTY));
        }
    }

    public static class MVShaderProgram {
        public final MVShaderProgramKey key;
        public ShaderProgram shader;

        public MVShaderProgram(MVShaderProgramKey key) {
            this.key = key;
        }
    }

    public record MVShaderAndLayer(MVShaderProgram shader, RenderLayer layer) {
    }

}
