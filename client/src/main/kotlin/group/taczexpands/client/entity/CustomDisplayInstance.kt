package group.taczexpands.client.entity

import com.tacz.guns.api.client.animation.AnimationController
import com.tacz.guns.api.client.animation.Animations
import com.tacz.guns.api.client.animation.ObjectAnimation
import com.tacz.guns.client.model.BedrockAnimatedModel
import com.tacz.guns.client.resource.ClientAssetsManager
import com.tacz.guns.client.resource.pojo.model.BedrockVersion
import group.taczexpands.common.entity.CustomDisplayEntity
import net.minecraft.resources.ResourceLocation

class CustomDisplayInstance(val entity: CustomDisplayEntity, val modelID: ResourceLocation, val textureID: ResourceLocation, val animationID: ResourceLocation?, val animationName: String?, val animationDelay: Int) {
    var model: BedrockAnimatedModel
    var controller: AnimationController?
    var animationPlayed = false
    var deathAnimationPlayed = false

    init {
        val modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(modelID) ?: throw RuntimeException("custom modelID not found")
        val model = if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.geometryModelLegacy != null) {
            BedrockAnimatedModel(modelPOJO, BedrockVersion.LEGACY)
        } else if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.geometryModelNew != null) {
            BedrockAnimatedModel(modelPOJO, BedrockVersion.NEW)
        } else throw RuntimeException("custom model version mismatched")

        val controller = if (animationID == null) {
            AnimationController(mutableListOf(), model)
        } else {
            val gltfAnimations = ClientAssetsManager.INSTANCE.getGltfAnimation(animationID)
            val bedrockAnimationFile = ClientAssetsManager.INSTANCE.getBedrockAnimations(animationID)
            if (bedrockAnimationFile != null) {
                Animations.createControllerFromBedrock(bedrockAnimationFile, model)
            } else if (gltfAnimations != null) {
                Animations.createControllerFromGltf(gltfAnimations, model)
            } else null
        }

        this.model = model
        this.controller = controller
    }

    fun tick() {
        if (deathAnimationPlayed) return
        if (!animationPlayed && entity.tickCount >= animationDelay) {
            animationPlayed = true
            if (animationName != null && animationName != "") {
                controller?.runAnimation(0, animationName, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0f)
            }
        }
    }

    fun onDeath() {
        if (!deathAnimationPlayed) {
            if (entity.renderData.contains("deathAnimationName")) {
                val animationName = entity.renderData.getString("deathAnimationName")
                deathAnimationPlayed = true
                if (animationName != null && animationName != "") {
                    controller?.runAnimation(0, animationName, ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0f)
                }
            }
        }
    }
}