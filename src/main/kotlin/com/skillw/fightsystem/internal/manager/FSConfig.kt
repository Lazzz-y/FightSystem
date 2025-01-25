package com.skillw.fightsystem.internal.manager

import com.skillw.attsystem.AttributeSystem.attributeManager
import com.skillw.fightsystem.FightSystem
import com.skillw.pouvoir.Pouvoir
import com.skillw.pouvoir.Pouvoir.triggerHandlerManager
import com.skillw.pouvoir.api.manager.ConfigManager
import com.skillw.pouvoir.api.plugin.map.BaseMap
import com.skillw.pouvoir.util.legacy.Mirror
import com.skillw.pouvoir.util.static
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Zombie
import org.spigotmc.AsyncCatcher
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.submit
import taboolib.module.metrics.charts.SingleLineChart
import java.io.File

object FSConfig : ConfigManager(FightSystem) {
    override val priority = 0

    val defaultRegainHolo: Boolean
        get() = this["message"].getBoolean("options.default.health-regain-holo")


    override fun onLoad() {
        AsyncCatcher.enabled = false
        createIfNotExists("fight_group", "default.yml", "skapi.yml", "mythic_skill.yml", "damage_event.yml")
        createIfNotExists("damage_type", "magic.yml", "physical.yml", "real.yml")
        createIfNotExists(
            "scripts",
            "mechanics/basic.js",
            "mechanics/mechanics.js",
            "mechanics/mythicskill.js",
            "mechanics/shield.js",
        )
        createIfNotExists(
            "attributes",
            "其他.yml",
            "readme.txt",
            "吸血.yml",
            "机制.yml",
            "物理.yml",
            "特殊.yml",
            "盾牌.yml",
            "魔法.yml"
        )
        createIfNotExists(
            "dispatchers", "custom-trigger.yml"
        )
//        createIfNotExists(
//            "handlers", "on-attack.yml"
//        )
        //兼容1.4.3及之前的脚本
        mapOf(
            "com.skillw.fightsystem.internal.fight" to "com.skillw.fightsystem.internal.core.fight",
        ).forEach(Pouvoir.scriptEngineManager::relocate)

        Pouvoir.scriptEngineManager.globalVariables.let {
            it["FightSystem"] = FightSystem::class.java.static()
        }
    }


    override fun onEnable() {
        onReload()
        val metrics =
            taboolib.module.metrics.Metrics(
                14766,
                FightSystem.plugin.description.version,
                Platform.BUKKIT
            )
        metrics.addCustomChart(SingleLineChart("FightGroups") {
            FightSystem.fightGroupManager.size
        })
        metrics.addCustomChart(SingleLineChart("mechanics") {
            FightSystem.mechanicManager.size
        })
        attributeManager.addSubPouvoir(FightSystem)
        triggerHandlerManager.addSubPouvoir(FightSystem)
    }


    val attackFightKeyMap = BaseMap<String, String>()
    override fun subReload() {
        Pouvoir.scriptManager.addScriptDir(scripts)
        attackFightKeyMap.clear()
        val a = this["config"].getStringList("options.fight.attack-fight")?.onEach {
            val array = it.split("::")
            if (array.size != 2) return@onEach
            attackFightKeyMap[array[0]] = array[1]
        }
        completeYaml("config.yml")
        completeYaml("message.yml")
    }

    val skillAPI by lazy {
        Bukkit.getPluginManager().isPluginEnabled("SkillAPI") || Bukkit.getPluginManager()
            .isPluginEnabled("ProSkillAPI")
    }

    private val scripts = File(getDataFolder(), "scripts")

    val isFightEnable
        get() = this["message"].getBoolean("options.fight.enable", true)
    val isPersonalEnable
        get() = this["message"].getBoolean("options.personal")

    val defaultAttackMessageType: String
        get() = (this["message"].getString("options.default.attack") ?: "HOLO").uppercase()

    val defaultDefendMessageType: String
        get() = (this["message"].getString("options.default.defend") ?: "CHAT").uppercase()

    val isVanillaArmor: Boolean
        get() = this["config"].getBoolean("options.fight.vanilla-armor")

    var isDebug = false

    val defaultFightGroup: String
        get() = this["config"].getString("options.fight.default","attack-damage")

    val debug: Boolean
        get() = this["config"].getBoolean("options.debug") || isDebug

    private var forceCalEve = false
    val eveFightCal
        get() = forceCalEve || this["config"].getBoolean("options.fight.eve-fight-cal")

    val defaultAttackerName: String
        get() = this["config"].getString("fight-message.default-name.attacker") ?: "大自然"
    val defaultDefenderName: String
        get() = this["config"].getString("fight-message.default-name.defender") ?: "未知"

    val projectileCache
        get() = this["config"].getBoolean("options.fight.projectile-cache-data", true)

    val mmDamageCal
        get() = this["config"].getBoolean("options.fight.mm-damage-cal", false)

    val skapiDamageCal
        get() = this["config"].getBoolean("options.fight.skill-api-damage-cal", false)


    @JvmStatic
    fun debug(debug: () -> Unit) {
        if (this.debug) {
            debug.invoke()
        }
    }


    @Awake(LifeCycle.ACTIVE)
    fun initSystem() {
        submit(delay = 20) {
            val world = Bukkit.getWorlds().first()
            val entityA = world.spawnEntity(Location(world, 0.0, 255.0, 0.0), EntityType.ZOMBIE) as Zombie
            val entityB = world.spawnEntity(Location(world, 0.0, 255.0, 0.0), EntityType.ZOMBIE) as Zombie
            entityA.setGravity(false)
            entityB.setGravity(false)
            forceCalEve = true
            entityA.damage(1.0, entityB)
            forceCalEve = false
            entityA.remove()
            entityB.remove()
            Mirror.mirrorData.clear()
        }
    }
}
