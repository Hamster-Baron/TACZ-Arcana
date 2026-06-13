package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;
import group.taczexpands.common.util.BlockResistanceData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BulletExtraHolder {
    @SerializedName("penetration")
    public int penetration = 0;

    @SerializedName("penetration_decay")
    public boolean penetrationDecay = true;

    @SerializedName("missile")
    public MissileData missileData = new MissileData();

    @SerializedName("hook")
    public HookData hookData = new HookData();

    @SerializedName("block_table")
    public ResistanceTable resistanceTable = new ResistanceTable();

    @SerializedName("proximity_fuze_range")
    public float proximityFuzeRange = 0.0f;

    @SerializedName("proximity_fuze_min_speed")
    public float proximityFuzeMinSpeed = 0.0f;

    @SerializedName("start_explosion_delay_on_impact")
    public boolean startExplosionDelayOnImpact = false;

    @SerializedName("deflection")
    public boolean deflection = false;

    @SerializedName("max_deflection_count")
    public int maxDeflectionCount = 1;

    @SerializedName("deflection_speed_factor")
    public float deflectionSpeedFactor = 0.5f;

    @SerializedName("deflection_damage_factor")
    public float deflectionDamageFactor = 0.5f;

    @SerializedName("min_incidence_angle")
    public float minIncidenceAngle = 0.0f;

    @SerializedName("render_penetration_bullet_hole")
    public boolean renderPenetrationBulletHole = true;

    @SerializedName("render_deflection_bullet_hole")
    public boolean renderDeflectionBulletHole = true;

    @SerializedName("render_normal_bullet_hole")
    public boolean renderNormalBulletHole = true;

}
