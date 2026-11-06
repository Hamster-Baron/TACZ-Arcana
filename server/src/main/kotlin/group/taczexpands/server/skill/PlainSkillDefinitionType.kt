package group.taczexpands.server.skill

internal enum class PlainSkillDefinitionType(val dataType: Int, val directory: String) {
    SKILL(0, "skills"),
    CONDITION(1, "conditions"),
    ACTION(2, "actions");

    val nativeType: Int
        get() = dataType
}
