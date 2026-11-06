package group.taczexpands.server.skill

data class SkillReloadResult(
    val serverSkills: Int,
    val packSkills: Int,
    val serverStatus: SkillReloadSourceStatus,
    val hybridStatus: SkillReloadSourceStatus
) {
    constructor(serverSkills: Int, packSkills: Int, success: Boolean) : this(
        serverSkills, packSkills,
        if (success) SkillReloadSourceStatus.SUCCESS else SkillReloadSourceStatus.FAILED,
        SkillReloadSourceStatus.UNSUPPORTED
    )

    val success: Boolean
        get() = serverStatus != SkillReloadSourceStatus.FAILED && hybridStatus != SkillReloadSourceStatus.FAILED
}
