package group.taczexpands.client.gui

import group.taczexpands.client.network.NetworkManager
import group.taczexpands.common.network.c2s.C2SGetVariable

object VariableManager {
    class VariableResult(val value: String, val lastUpdated: Long) {

    }

    val cachedVariables = mutableMapOf<String, VariableResult>()
    val fetchingVariables = mutableListOf<String>()
    fun add(variable: String, value: String) {
        fetchingVariables.remove(variable)
        cachedVariables[variable] = VariableResult(value, System.currentTimeMillis())
    }

    fun reset() {
        fetchingVariables.clear()
        cachedVariables.clear()
    }

    fun get(variable: String): String {
        val name = variable
        if (!cachedVariables.containsKey(name)) {
            if (!fetchingVariables.contains(name)) {
                fetchingVariables.add(name)
                NetworkManager.sendToServer(C2SGetVariable(name))
            }
            return "获取中"
        } else {
            val result = cachedVariables[name]!!
            if (System.currentTimeMillis() - result.lastUpdated > 5000) {
                if (!fetchingVariables.contains(name)) {
                    fetchingVariables.add(name)
                    NetworkManager.sendToServer(C2SGetVariable(name))
                }
            }
            return result.value
        }
    }

    fun invalidate(variable: String) {
        val name = variable
        if (cachedVariables.containsKey(name) && !fetchingVariables.contains(name)) {
            fetchingVariables.add(name)
            NetworkManager.sendToServer(C2SGetVariable(name))
        }
    }

    fun invalidateAll() {
        cachedVariables.keys.forEach { name ->
            if (!fetchingVariables.contains(name)) {
                fetchingVariables.add(name)
                NetworkManager.sendToServer(C2SGetVariable(name))
            }
        }
    }

    fun processString(input: String): String {
        return replaceVariables(input, ::get)
    }


    private fun replaceVariables(text: String, valueProvider: (variableName: String) -> String): String {
        val regex = Regex("%(Var[a-zA-Z0-9_]+)(?:\\$([0-9]+))?(?:\\$\\((.*?)\\))?%")

        return regex.replace(text) { matchResult ->
            val variableName = matchResult.groupValues[1]
            val decimalPlaces = matchResult.groupValues.getOrNull(2)
            val defaultValue = matchResult.groupValues.getOrNull(3)

            val replacementValue = try {
                val value = valueProvider(variableName)
                if (value == "获取中" || value == "未知") {
                    if(!defaultValue.isNullOrEmpty()) defaultValue else value
                } else {
                    if (!decimalPlaces.isNullOrEmpty()) {
                        val formatString = "%.${decimalPlaces}f"
                        val num = value.toDoubleOrNull()
                        if (num != null) {
                            String.format(formatString, num)
                        } else {
                            value
                        }
                    } else {
                        value
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if(!defaultValue.isNullOrEmpty()) defaultValue else "错误"
            }

            replacementValue
        }
    }
}