package group.taczexpands.common.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import group.taczexpands.common.accessor.IAccessorCommonDataManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class GunExtraAmmo {
    @SerializedName("reference")
    private ResourceLocation reference = null;

    @SerializedName("inherit")
    private boolean inherit = false;

    @SerializedName("override_gun_base_ammo")
    private boolean overrideGunBaseAmmo = false;

    @SerializedName("trigger_base_ammo_reload_event")
    private boolean triggerBaseAmmoReloadEvent = false;

    @SerializedName("ammo")
    private ResourceLocation ammoId = null;

    @SerializedName("ammo_amount")
    private int ammoAmount = 0;

    @SerializedName("extended_mag_ammo_amount")
    private int @Nullable [] extendedMagAmmoAmount = null;

    @SerializedName("bullet")
    private BulletData bulletData = null;

    @SerializedName("hide")
    private boolean hide = false;

    @SerializedName("auto_model_control")
    private boolean autoModelControl = false;

    @SerializedName("change_model_delay")
    private float changeModelDelay = 0.0f;

    @SerializedName("hidden_attachment")
    private JsonElement hiddenAttachmentDataRaw = null;

    private transient AttachmentData hiddenAttachment = null;

    private transient boolean attachmentInited = false;

    private GunExtraAmmo getReference() {
        if (reference != null)
            return IAccessorCommonDataManager.getAmmoData(CommonAssetsManager.get(), reference);
        return this;
    }

    public boolean isInherit() {
        return inherit;
    }

    public boolean isOverrideGunBaseAmmo() {
        return overrideGunBaseAmmo;
    }

    public boolean isTriggerBaseAmmoReloadEvent() {
        return triggerBaseAmmoReloadEvent;
    }

    public ResourceLocation getAmmoId() {
        if (ammoId != null)
            return ammoId;
        return getReference().ammoId;
    }

    public int getAmmoAmount() {
        if (ammoAmount != 0)
            return ammoAmount;
        return getReference().ammoAmount;
    }

    public int[] getExtendedMagAmmoAmount() {
        if (extendedMagAmmoAmount != null)
            return extendedMagAmmoAmount;
        return getReference().extendedMagAmmoAmount;
    }

    public BulletData getBulletData() {
        if (bulletData != null)
            return bulletData;
        return getReference().bulletData;
    }

    @Nullable
    public AttachmentData getHiddenAttachment() {
        if (hiddenAttachmentDataRaw != null) {
            if (hiddenAttachment == null) {
                initAttachment();
            }
            return hiddenAttachment;
        } else {
            if (hiddenAttachment != null) {
                return hiddenAttachment;
            }
        }
        var reference = getReference();
        if (reference.hiddenAttachmentDataRaw != null) {
            if (reference.hiddenAttachment == null) {
                reference.initAttachment();
            }
            return reference.hiddenAttachment;
        } else {
            if (reference.hiddenAttachment != null) {
                return reference.hiddenAttachment;
            }
        }
        return null;
    }

    public void initAttachment() {
        var element = hiddenAttachmentDataRaw;
        hiddenAttachmentDataRaw = null;
        AttachmentData data = CommonAssetsManager.GSON.fromJson(element, AttachmentData.class);
        if (data != null) {
            String json = element.toString();
            AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject jsonObject = element.getAsJsonObject();
                if (jsonObject.has(key)) {
                    JsonProperty<?> property = value.readJson(json);
                    property.initComponents();
                    data.addModifier(key, property);
                } else if (jsonObject.has(value.getOptionalFields())) {
                    JsonProperty<?> property = value.readJson(json);
                    property.initComponents();
                    data.addModifier(key, property);
                }
            });
        }
        hiddenAttachment = data;
    }

    public boolean isHide() {
        return hide || overrideGunBaseAmmo;
    }

    public boolean isAutoModelControl() {
        return autoModelControl;
    }

    public float getChangeModelDelay() {
        return changeModelDelay;
    }
}
