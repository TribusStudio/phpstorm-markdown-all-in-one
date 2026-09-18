package com.tribus.markdown.editor

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.tribus.markdown.actions.MarkdownAction

/**
 * Opening a markdown editor binds our shortcuts to the editor component.
 *
 * Those bindings must never touch the action instances [ActionManager] hands
 * out: those are application-wide singletons, so mutating one rebinds the
 * shortcut for the whole IDE and makes the platform log a stack trace for every
 * call — 21 of them per markdown editor opened.
 */
class MarkdownFileEditorListenerTest : BasePlatformTestCase() {

    private val boundActionIds = listOf(
        "com.tribus.markdown.actions.ToggleBold",
        "com.tribus.markdown.actions.ToggleItalic",
        "com.tribus.markdown.actions.MoveLineUp",
        "com.tribus.markdown.actions.SoftBreak",
    )

    fun testOpeningMarkdownDoesNotMutateSharedActions() {
        val actionManager = ActionManager.getInstance()
        val before: Map<String, ShortcutSet> = boundActionIds.associateWith {
            actionManager.getAction(it).shortcutSet
        }

        // Creates an editor, which fires MarkdownFileEditorListener.editorCreated.
        myFixture.configureByText("test.md", "# hello<caret>")

        for (id in boundActionIds) {
            assertSame(
                "$id: shortcut set of the shared ActionManager instance was replaced — " +
                    "registerCustomShortcutSet must be called on a per-editor wrapper",
                before.getValue(id),
                actionManager.getAction(id).shortcutSet
            )
        }
    }

    fun testOpeningMarkdownRepeatedlyLeavesSharedActionsAlone() {
        val actionManager = ActionManager.getInstance()
        val boldBefore = actionManager.getAction(boundActionIds[0]).shortcutSet

        repeat(3) { i ->
            myFixture.configureByText("doc$i.md", "- item<caret>")
        }

        assertSame(
            "repeated markdown editor creation must not accumulate global shortcut mutations",
            boldBefore,
            actionManager.getAction(boundActionIds[0]).shortcutSet
        )
    }

    fun testNonMarkdownFileRegistersNothing() {
        val actionManager = ActionManager.getInstance()
        val before = actionManager.getAction(boundActionIds[0]).shortcutSet
        myFixture.configureByText("test.txt", "plain<caret>")
        assertSame(before, actionManager.getAction(boundActionIds[0]).shortcutSet)
    }

    /**
     * The promoter identifies our actions by the [MarkdownAction] marker, so the
     * per-editor wrapper has to carry it too — otherwise Cmd+B silently loses to
     * the IDE's Go To Declaration.
     */
    fun testBoundActionsAreMarkdownActions() {
        val actionManager = ActionManager.getInstance()
        for (id in boundActionIds) {
            assertTrue(
                "$id should implement MarkdownAction so the promoter recognises it",
                actionManager.getAction(id) is MarkdownAction
            )
        }
    }
}
