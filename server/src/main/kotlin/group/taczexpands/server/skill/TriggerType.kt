package group.taczexpands.server.skill

enum class TriggerType(val reversible: Boolean, val subTypes: List<TriggerType>? = null) {
    ON_MAIN_HAND(true),
    ON_SHOOT(false),
    ON_HIT_ENTITY(false),
    ON_HIT_BLOCK(false),
    ON_CROUCH(true),
    ON_STAND(true),
    ON_CRAWL(true),
    ON_HIT_BY_TARGET(false),

    ON_BULLET_EXPLOSION_HIT_ENTITY(false),
    ON_BULLET_EXPLOSION_KILL_ENTITY(false),
    ON_KILL_ENTITY(false),
    ON_HOT_BAR(true),
    ON_OFF_HAND(true),
    ON_STOP_MOVE(true),
    ON_STOP_SPRINT(true),
    ON_MOVE(true),
    ON_SPRINT(true),
    ON_PRE_RELOAD(false),
    ON_POST_RELOAD(false),
    ON_AIM(true),

    ON_SWITCH_FIREMODE(false),
    ON_MELEE_ATTACK(false),
    ON_MELEE_HIT(false),
    ON_ACTION_1(false),
    ON_ACTION_2(false),
    ON_ACTION_3(false),
    ON_ACTION_4(false),

    ON_BULLET_DISCARD(false),

    ON_SWITCH_TO_GUN(true),
    ON_SWITCH_TO_UNDER_BARREL(true),
    ON_SWITCH_AMMO(false),
    ON_JUMP(false),
    ON_BULLET_SPAWN(false),

    ON_SIGNAL(false),

    ON_ZOOM_CHANGE(false),
    ON_AUTO_SHOOT(true),
    ON_BURST_SHOOT(true),
    ON_LOCKING_TARGET(true),
    ON_COMPLETE_LOCKING_TARGET(false),
    ON_PLAYER_TICK(false),

    ON_CLICK(false),
    ON_CHANGE_ATTACHMENT(false),

    ON_HURT(false),
    ON_DEATH(false),

    ON_INSPECT(false),

    ON_SWITCH_FLASHLIGHT(false),
    ON_SWITCH_LASER(false),
    ON_MELEE_KILL(false),

    ON_HOOK_ATTACH_BLOCK(false),
    ON_HOOK_ATTACH_ENTITY(false),
    ON_HOOK_DETACH_BLOCK(false),
    ON_HOOK_DETACH_ENTITY(false),


    ON_BULLET_PENETRATE(false),
    ON_BULLET_DEFLECT(false),
    ON_WHEEL_SCROLL(false),

    ON_BULLET_HIT(false, listOf(ON_HIT_ENTITY, ON_HIT_BLOCK, ON_BULLET_EXPLOSION_HIT_ENTITY)),

    ON_SHIELD_BLOCK_BULLET(false),

    ON_SHIELD_BLOCK_VANILLA(false),

    ON_SHIELD_BLOCK(false, listOf(ON_SHIELD_BLOCK_BULLET, ON_SHIELD_BLOCK_VANILLA)),

    ON_SHIELD_DISABLED(false),


    ON_CHARGE(true)



    ;



    val superTypes: List<TriggerType> by lazy {
        TriggerType.entries.filter { it.subTypes?.contains(this) ?: false }
    }
}
