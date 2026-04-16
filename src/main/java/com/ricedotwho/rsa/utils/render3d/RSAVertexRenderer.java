package com.ricedotwho.rsa.utils.render3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.utils.render.render3d.VertexRenderer;
import lombok.experimental.UtilityClass;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class RSAVertexRenderer {
    public void renderRing(PoseStack.Pose pose, VertexConsumer buffer, Vec3 pos, float radius, Colour colour, int slices, int layers) {
        if (slices >= 3) {
            pose.translate((float)pos.x(), (float)pos.y(), (float)pos.z());

            double h = (radius * 2) / 3.0;
            float oneOverLayers = 1.0f / layers;

            float red = colour.getRedFloat();
            float green = colour.getGreenFloat();
            float blue = colour.getBlueFloat();

            for (int i = 0; i < layers; i++) {
                float yOffset = (float) ((h * i) / (float) layers);

                float t = 1.0f - (i * oneOverLayers);
                float alpha = t * t * t;
                if (alpha < 0.01f) continue;
                VertexRenderer.circle(pose, buffer, radius, yOffset, alpha, red, green, blue, slices);
            }

            pose.translate((float)(-pos.x()), (float)(-pos.y()), (float)(-pos.z()));
        }
    }
}
