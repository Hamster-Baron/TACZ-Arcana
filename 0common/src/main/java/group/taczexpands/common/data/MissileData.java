package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class MissileData {
    @SerializedName("is_missile")
    public boolean isMissile = false;

    @SerializedName("guidance_type")
    public GuidanceType guidanceType = GuidanceType.ACTIVE_RADAR;

    @SerializedName("guidance_stability")
    public float guidanceStability = 0.0f;

    @SerializedName("guidance_delay")
    public int guidanceDelay = 20;

    @SerializedName("tv_overlay_texture")
    public String tvOverlayTexture = "";

    @SerializedName("tv_rotation_lock")
    public boolean tvRotationLock = false;

    @SerializedName("tv_rotation_clamp")
    public boolean tvRotationClamp = false;

    @SerializedName("tv_rotation_clamp_modifier")
    public float tvRotationClampModifier = 4.0f;

    @SerializedName("flight_profile_type")
    public FlightProfileType flightProfileType = FlightProfileType.PURE_PURSUIT;

    @SerializedName("height_offset")
    public float heightOffset = 0.0f;

    @SerializedName("climb_distance")
    public float climbDistance = 32.0f;

    @SerializedName("ascent_angle")
    public int ascentAngle = 75;

    @SerializedName("strike_angle")
    public int strikeAngle = 25;

    @SerializedName("navigation_gain")
    public float navigationGain = 1.0f;

    @SerializedName("max_speed")
    public float maxSpeedPerTick = 10.0f;

    @SerializedName("min_input")
    public float minInput = 0.1f;

    @SerializedName("thrust")
    public float thrustPerTick = 1.0f;

    @SerializedName("fuel")
    public int fuel = 200;

    @SerializedName("acceleration_limit")
    public float accelerationLimitPerTick = 2.0f;

    @SerializedName("proximity_fuze_range")
    public float proximityFuzeRange = 0.0f;

    @SerializedName("targetable_entity_types")
    public List<String> targetableEntityTypes = new ArrayList<>();

    @SerializedName("is_simple_locking")
    public boolean isSimpleLocking = true;

    @SerializedName("must_locking")
    public boolean mustLocking = false;

    @SerializedName("locking_time")
    public int lockingTime = 20;

    @SerializedName("locking_remaining_time")
    public int lockingRemainingTime = 0;

    @SerializedName("max_locking_distance")
    public float maxLockingDistance = 256.0f;

    @SerializedName("jammable_angle")
    public int jammableAngle = 30;

    @SerializedName("max_jammable_distance")
    public float maxJammableDistance = 128.0f;

    @SerializedName("max_off_axis_angle_on_locking")
    public int maxOffAxisAngleOnLocking = 15;

    @SerializedName("max_off_axis_angle_on_guidance")
    public int maxOffAxisAngleOnGuidance = 90;

    @Nullable
    public transient BiFunction<ServerPlayer, Entity, Boolean> targetableEntityPredicatorCache = null;
}
