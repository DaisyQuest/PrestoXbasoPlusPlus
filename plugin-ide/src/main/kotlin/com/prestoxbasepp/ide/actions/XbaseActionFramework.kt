package com.prestoxbasepp.ide.actions

import kotlin.reflect.KClass

@JvmInline
value class ActionId private constructor(val value: String) {
    companion object {
        private val IdPattern = Regex("[A-Za-z0-9_.-]+")

        fun of(raw: String): ActionId {
            val trimmed = raw.trim()
            require(trimmed.isNotEmpty()) { "Action id cannot be blank." }
            require(IdPattern.matches(trimmed)) { "Action id must match ${IdPattern.pattern}." }
            return ActionId(trimmed)
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class ActionKey<T : Any> private constructor(val name: String) {
    companion object {
        fun <T : Any> of(name: String): ActionKey<T> {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Action key name cannot be blank." }
            return ActionKey(trimmed)
        }
    }
}

data class ActionContext internal constructor(
    private val values: Map<ActionKey<*>, Any>
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: ActionKey<T>): T? = values[key] as? T

    fun <T : Any> require(key: ActionKey<T>): T =
        get(key) ?: error("Missing required action context key: ${key.name}")

    fun toMap(): Map<ActionKey<*>, Any> = values.toMap()

    companion object {
        fun empty(): ActionContext = ActionContext(emptyMap())

        fun build(builder: ActionContextBuilder.() -> Unit): ActionContext =
            ActionContextBuilder().apply(builder).build()
    }
}

class ActionContextBuilder {
    private val values = linkedMapOf<ActionKey<*>, Any>()

    fun <T : Any> put(key: ActionKey<T>, value: T): ActionContextBuilder = apply {
        values[key] = value
    }

    fun build(): ActionContext = ActionContext(values.toMap())
}

fun interface ActionCondition {
    fun isSatisfied(context: ActionContext): Boolean

    companion object {
        fun always(): ActionCondition = ActionCondition { true }

        fun never(): ActionCondition = ActionCondition { false }

        fun allOf(conditions: Iterable<ActionCondition>): ActionCondition = ActionCondition { context ->
            val iterator = conditions.iterator()
            if (!iterator.hasNext()) {
                true
            } else {
                conditions.all { it.isSatisfied(context) }
            }
        }

        fun anyOf(conditions: Iterable<ActionCondition>): ActionCondition = ActionCondition { context ->
            val iterator = conditions.iterator()
            if (!iterator.hasNext()) {
                false
            } else {
                conditions.any { it.isSatisfied(context) }
            }
        }
    }
}

fun interface ActionHandler {
    fun perform(context: ActionContext): ActionResult
}

sealed interface ActionResult {
    val message: String?

    data class Success(override val message: String? = null) : ActionResult

    data class Failure(override val message: String? = null, val cause: Throwable? = null) : ActionResult
}

data class ActionPresentation(
    val text: String,
    val description: String? = null,
    val category: String? = null,
    val iconPath: String? = null,
    val tags: Set<String> = emptySet()
) {
    init {
        require(text.isNotBlank()) { "Action presentation text cannot be blank." }
    }
}

interface ActionDefinition {
    val id: ActionId
    val presentation: ActionPresentation
    val visibility: ActionCondition
    val enablement: ActionCondition
    val handler: ActionHandler

    fun isVisible(context: ActionContext): Boolean = visibility.isSatisfied(context)

    fun isEnabled(context: ActionContext): Boolean = enablement.isSatisfied(context)

    fun execute(context: ActionContext): ActionResult = handler.perform(context)
}

data class ActionSpec(
    override val id: ActionId,
    override val presentation: ActionPresentation,
    override val visibility: ActionCondition,
    override val enablement: ActionCondition,
    override val handler: ActionHandler
) : ActionDefinition

class ActionPresentationBuilder(private val id: ActionId) {
    var text: String = id.value
    var description: String? = null
    var category: String? = null
    var iconPath: String? = null
    private val tags: MutableSet<String> = linkedSetOf()

    fun tag(tag: String) = apply { tags.add(tag) }

    fun build(): ActionPresentation = ActionPresentation(
        text = text,
        description = description,
        category = category,
        iconPath = iconPath,
        tags = tags.toSet()
    )
}

class ActionSpecBuilder(private val id: ActionId) {
    private var visibility: ActionCondition = ActionCondition.always()
    private var enablement: ActionCondition = ActionCondition.always()
    private var handler: ActionHandler? = null
    private var presentationBuilder: ActionPresentationBuilder? = null

    fun presentation(builder: ActionPresentationBuilder.() -> Unit) = apply {
        val presentation = presentationBuilder ?: ActionPresentationBuilder(id).also { presentationBuilder = it }
        presentation.apply(builder)
    }

    fun visibleWhen(condition: ActionCondition) = apply { visibility = condition }

    fun enabledWhen(condition: ActionCondition) = apply { enablement = condition }

    fun handler(handler: ActionHandler) = apply { this.handler = handler }

    fun build(): ActionSpec {
        val resolvedHandler = handler ?: error("Action '${id.value}' is missing a handler.")
        val presentation = presentationBuilder?.build() ?: ActionPresentationBuilder(id).build()
        return ActionSpec(
            id = id,
            presentation = presentation,
            visibility = visibility,
            enablement = enablement,
            handler = resolvedHandler
        )
    }
}

fun xbaseAction(id: String, configure: ActionSpecBuilder.() -> Unit): ActionSpec =
    ActionSpecBuilder(ActionId.of(id)).apply(configure).build()

interface ActionRegistry {
    fun register(action: ActionDefinition)
    fun get(id: ActionId): ActionDefinition?
    fun all(): List<ActionDefinition>
    fun available(context: ActionContext): List<ActionDefinition>
}

class InMemoryActionRegistry : ActionRegistry {
    private val actions = linkedMapOf<ActionId, ActionDefinition>()

    override fun register(action: ActionDefinition) {
        require(actions.putIfAbsent(action.id, action) == null) {
            "Action with id '${action.id.value}' is already registered."
        }
    }

    override fun get(id: ActionId): ActionDefinition? = actions[id]

    override fun all(): List<ActionDefinition> = actions.values.toList()

    override fun available(context: ActionContext): List<ActionDefinition> =
        actions.values.filter { it.isVisible(context) && it.isEnabled(context) }
}

interface ActionIntention {
    val key: ActionKey<out Any>
    val requiredType: KClass<out Any>
}

class TypedActionIntention<T : Any>(
    override val key: ActionKey<T>,
    override val requiredType: KClass<T>
) : ActionIntention
