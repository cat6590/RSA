package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class MovementPredictor {

    public static double getTickVelocityFromInput(int tickIndex, double walkSpeed) {
        // I plotted some velocities and solved for the exponential function
        // https://www.desmos.com/calculator/tpikdildj1
        return Math.pow(0.546000082, tickIndex) * 0.098 * walkSpeed; // Don't mind the constants
    }

    // Walkspeed should be 1 when normal, may need to mult by 10
    // this function assumes it is normalized
    public static double getDisplacementFromInput(double walkSpeed, boolean sneaking) {
        if (sneaking) walkSpeed = walkSpeed * 0.3;
        int movementTicks = getInputMovementTicks(walkSpeed);

        return 0.098 * walkSpeed * (1.0 - Math.pow(0.546000082, movementTicks)) / (1.0 - 0.546000082); // Don't mind the constants
    }

    public static double squaredAfterTick(double fwd, double right, double dFwd, double dRight) {
        double nf = (fwd + dFwd) * 0.546000082;
        double nr = (right + dRight) * 0.546000082;
        return nf * nf + nr * nr;
    }

    public static int getMovementTicks(float dx, float dy) {
        return (int) Math.ceil(Math.log(0.003 / Mth.sqrt(dx * dx + dy + dy)) / Math.log(0.546000082));
    }

    public static double getDisplacementMagnitude(Vec2 velocity) {
        double magnitude = velocity.length();
        int movementTicks = (int) Math.ceil(Math.log(0.003 / magnitude) / Math.log(0.546000082));

        if (movementTicks <= 0) return magnitude;
        return magnitude * (1.0 - Math.pow(0.546000082, movementTicks)) / (1.0 - 0.546000082);
    }

    public static Vec2 getDisplacementVector(Vec2 velocity) {
        float magnitude = velocity.length();
        if (magnitude < 1e-6) return Vec2.ZERO;

        float displacement = (float) getDisplacementMagnitude(velocity);
        float scale = displacement / magnitude;

        return velocity.scale(scale);
    }


    private static int getInputMovementTicks(double velocity) {
        // 0.003 is epsilon, check LivingEnntity.aiStep()
        return (int) Math.ceil(Math.log(0.003 / (0.098 * velocity)) / Math.log(0.546000082));
    }

    public static Vec2 rotateVec(Vec2 vec, float yaw) {
        double yawRad = Math.toRadians(yaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        float newX = (float) (vec.x * cos - vec.y * sin);
        float newY = (float) (vec.x * sin + vec.y * cos);

        return new Vec2(newX, newY);
    }

    public static Vec3 rotateVec(Vec3 vec, float yaw) {
        double yawRad = Math.toRadians(yaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        double newX = vec.x * cos - vec.z * sin;
        double newZ = vec.x * sin + vec.z * cos;

        return new Vec3(newX, vec.y, newZ);
    }
}
