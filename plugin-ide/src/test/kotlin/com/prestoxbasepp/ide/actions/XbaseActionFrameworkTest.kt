package com.prestoxbasepp.ide.actions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class XbaseActionFrameworkTest {
    @Test
    fun `action id trims and validates`() {
        val id = ActionId.of("  build.run ")

        assertThat(id.value).isEqualTo("build.run")
        assertThat(id.toString()).isEqualTo("build.run")

        assertThatThrownBy { ActionId.of(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)

        assertThatThrownBy { ActionId.of("bad id") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `action key requires non-blank name`() {
        val key = ActionKey.of<String>("project")
        val context = ActionContext.build { put(key, "demo") }

        assertThat(context.get(key)).isEqualTo("demo")
        assertThat(context.require(key)).isEqualTo("demo")
        assertThat(context.toMap()).containsEntry(key, "demo")

        assertThatThrownBy { ActionKey.of<String>(" ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `context require throws for missing keys`() {
        val key = ActionKey.of<String>("file")
        val context = ActionContext.empty()

        assertThat(context.get(key)).isNull()
        assertThatThrownBy { context.require(key) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Missing required action context key")
    }

    @Test
    fun `action conditions combine as expected`() {
        val key = ActionKey.of<Boolean>("flag")
        val condition = ActionCondition.allOf(
            listOf(
                ActionCondition { it.require(key) },
                ActionCondition { true }
            )
        )
        val anyCondition = ActionCondition.anyOf(listOf(ActionCondition.never(), ActionCondition.always()))
        val emptyAll = ActionCondition.allOf(emptyList())
        val emptyAny = ActionCondition.anyOf(emptyList())

        val context = ActionContext.build { put(key, true) }
        val negativeContext = ActionContext.build { put(key, false) }

        assertThat(condition.isSatisfied(context)).isTrue()
        assertThat(condition.isSatisfied(negativeContext)).isFalse()
        assertThat(anyCondition.isSatisfied(context)).isTrue()
        assertThat(emptyAll.isSatisfied(context)).isTrue()
        assertThat(emptyAny.isSatisfied(context)).isFalse()
    }

    @Test
    fun `action spec builder enforces handler and defaults presentation`() {
        val action = xbaseAction("build.run") {
            handler(ActionHandler { ActionResult.Success("ok") })
        }

        assertThat(action.presentation.text).isEqualTo("build.run")
        assertThat(action.execute(ActionContext.empty())).isEqualTo(ActionResult.Success("ok"))

        assertThatThrownBy {
            xbaseAction("build.fail") {
                presentation { text = "Fail" }
            }
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("missing a handler")
    }

    @Test
    fun `action spec supports presentation overrides`() {
        val action = xbaseAction("inspect.file") {
            presentation {
                text = "Inspect File"
                description = "Inspect the active file"
                category = "Inspection"
                iconPath = "icons/inspect.svg"
                tag("analysis")
                tag("quick")
            }
            handler(ActionHandler { ActionResult.Success() })
        }

        assertThat(action.presentation.text).isEqualTo("Inspect File")
        assertThat(action.presentation.description).isEqualTo("Inspect the active file")
        assertThat(action.presentation.category).isEqualTo("Inspection")
        assertThat(action.presentation.iconPath).isEqualTo("icons/inspect.svg")
        assertThat(action.presentation.tags).containsExactly("analysis", "quick")
    }

    @Test
    fun `registry tracks availability based on conditions`() {
        val key = ActionKey.of<Boolean>("enabled")
        val registry = InMemoryActionRegistry()
        val visibleAction = xbaseAction("action.visible") {
            visibleWhen(ActionCondition.always())
            enabledWhen(ActionCondition { it.require(key) })
            handler(ActionHandler { ActionResult.Success() })
        }
        val hiddenAction = xbaseAction("action.hidden") {
            visibleWhen(ActionCondition.never())
            handler(ActionHandler { ActionResult.Success("hidden") })
        }

        registry.register(visibleAction)
        registry.register(hiddenAction)

        assertThat(registry.all()).containsExactly(visibleAction, hiddenAction)
        assertThat(registry.get(ActionId.of("action.visible"))).isSameAs(visibleAction)
        assertThat(registry.get(ActionId.of("missing"))).isNull()

        val enabledContext = ActionContext.build { put(key, true) }
        val disabledContext = ActionContext.build { put(key, false) }

        assertThat(registry.available(enabledContext)).containsExactly(visibleAction)
        assertThat(registry.available(disabledContext)).isEmpty()
    }

    @Test
    fun `registry rejects duplicate ids`() {
        val registry = InMemoryActionRegistry()
        val action = xbaseAction("action.dup") {
            handler(ActionHandler { ActionResult.Success() })
        }

        registry.register(action)

        assertThatThrownBy { registry.register(action) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("already registered")
    }

    @Test
    fun `action presentation requires text`() {
        assertThatThrownBy {
            ActionPresentation(text = " ")
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("cannot be blank")
    }

    @Test
    fun `typed action intention exposes key and type`() {
        val key = ActionKey.of<String>("project")
        val intention = TypedActionIntention(key, String::class)

        assertThat(intention.key).isEqualTo(key)
        assertThat(intention.requiredType).isEqualTo(String::class)
    }
}
