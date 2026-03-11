package com.tribus.markdown.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Provides LaTeX command completion inside math environments ($...$ and $$...$$).
 * Triggered when the user types `\` inside a math delimiter.
 */
class MathCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val editor = parameters.editor
        val offset = parameters.offset
        val text = editor.document.text

        // Check if we're inside a math environment
        if (!isInsideMath(text, offset)) return

        // Find the prefix (text after the last \)
        val prefixStart = text.lastIndexOf('\\', offset - 1)
        if (prefixStart < 0) return

        val prefix = text.substring(prefixStart + 1, offset)
        val adjusted = result.withPrefixMatcher(prefix)

        for ((command, description) in MATH_COMMANDS) {
            if (command.startsWith(prefix, ignoreCase = true)) {
                adjusted.addElement(
                    LookupElementBuilder.create(command)
                        .withPresentableText("\\$command")
                        .withTypeText(description, true)
                        .withInsertHandler { ctx, _ ->
                            // Replace the prefix with the full command
                            val startOffset = prefixStart + 1
                            ctx.document.replaceString(startOffset, ctx.tailOffset, command)
                            ctx.editor.caretModel.moveToOffset(startOffset + command.length)
                        }
                )
            }
        }
    }

    companion object {
        fun isInsideMath(text: String, offset: Int): Boolean {
            // Check for display math $$...$$
            val beforeCursor = text.substring(0, offset.coerceAtMost(text.length))

            // Count unmatched $$ pairs
            var displayOpen = false
            var i = 0
            while (i < beforeCursor.length) {
                if (i + 1 < beforeCursor.length && beforeCursor[i] == '$' && beforeCursor[i + 1] == '$') {
                    displayOpen = !displayOpen
                    i += 2
                    continue
                }
                i++
            }
            if (displayOpen) return true

            // Count unmatched $ (excluding $$)
            var inlineOpen = false
            i = 0
            while (i < beforeCursor.length) {
                if (i + 1 < beforeCursor.length && beforeCursor[i] == '$' && beforeCursor[i + 1] == '$') {
                    i += 2
                    continue
                }
                if (beforeCursor[i] == '$') {
                    inlineOpen = !inlineOpen
                }
                i++
            }
            return inlineOpen
        }

        val MATH_COMMANDS: List<Pair<String, String>> = listOf(
            // Greek letters (lowercase)
            "alpha" to "α",
            "beta" to "β",
            "gamma" to "γ",
            "delta" to "δ",
            "epsilon" to "ε",
            "varepsilon" to "ε (variant)",
            "zeta" to "ζ",
            "eta" to "η",
            "theta" to "θ",
            "vartheta" to "θ (variant)",
            "iota" to "ι",
            "kappa" to "κ",
            "lambda" to "λ",
            "mu" to "μ",
            "nu" to "ν",
            "xi" to "ξ",
            "pi" to "π",
            "rho" to "ρ",
            "sigma" to "σ",
            "tau" to "τ",
            "upsilon" to "υ",
            "phi" to "φ",
            "varphi" to "φ (variant)",
            "chi" to "χ",
            "psi" to "ψ",
            "omega" to "ω",

            // Greek letters (uppercase)
            "Gamma" to "Γ",
            "Delta" to "Δ",
            "Theta" to "Θ",
            "Lambda" to "Λ",
            "Xi" to "Ξ",
            "Pi" to "Π",
            "Sigma" to "Σ",
            "Phi" to "Φ",
            "Psi" to "Ψ",
            "Omega" to "Ω",

            // Operators
            "frac{}{}" to "Fraction",
            "sqrt{}" to "Square root",
            "sqrt[]{}" to "Nth root",
            "sum" to "Summation ∑",
            "prod" to "Product ∏",
            "int" to "Integral ∫",
            "iint" to "Double integral",
            "iiint" to "Triple integral",
            "oint" to "Contour integral",
            "lim" to "Limit",
            "inf" to "Infimum",
            "sup" to "Supremum",
            "min" to "Minimum",
            "max" to "Maximum",
            "log" to "Logarithm",
            "ln" to "Natural logarithm",
            "exp" to "Exponential",
            "sin" to "Sine",
            "cos" to "Cosine",
            "tan" to "Tangent",
            "cot" to "Cotangent",
            "sec" to "Secant",
            "csc" to "Cosecant",
            "arcsin" to "Arc sine",
            "arccos" to "Arc cosine",
            "arctan" to "Arc tangent",
            "sinh" to "Hyperbolic sine",
            "cosh" to "Hyperbolic cosine",
            "tanh" to "Hyperbolic tangent",

            // Relations
            "leq" to "≤",
            "geq" to "≥",
            "neq" to "≠",
            "approx" to "≈",
            "equiv" to "≡",
            "sim" to "∼",
            "simeq" to "≃",
            "cong" to "≅",
            "propto" to "∝",
            "ll" to "≪",
            "gg" to "≫",
            "subset" to "⊂",
            "supset" to "⊃",
            "subseteq" to "⊆",
            "supseteq" to "⊇",
            "in" to "∈",
            "notin" to "∉",
            "ni" to "∋",

            // Arrows
            "rightarrow" to "→",
            "leftarrow" to "←",
            "leftrightarrow" to "↔",
            "Rightarrow" to "⇒",
            "Leftarrow" to "⇐",
            "Leftrightarrow" to "⇔",
            "uparrow" to "↑",
            "downarrow" to "↓",
            "mapsto" to "↦",
            "to" to "→",
            "implies" to "⟹",
            "iff" to "⟺",

            // Symbols
            "infty" to "∞",
            "partial" to "∂",
            "nabla" to "∇",
            "forall" to "∀",
            "exists" to "∃",
            "nexists" to "∄",
            "emptyset" to "∅",
            "varnothing" to "∅ (variant)",
            "cdot" to "·",
            "cdots" to "⋯",
            "ldots" to "…",
            "vdots" to "⋮",
            "ddots" to "⋱",
            "times" to "×",
            "div" to "÷",
            "pm" to "±",
            "mp" to "∓",
            "circ" to "∘",
            "star" to "⋆",
            "bullet" to "•",
            "oplus" to "⊕",
            "otimes" to "⊗",
            "cup" to "∪",
            "cap" to "∩",
            "wedge" to "∧",
            "vee" to "∨",
            "neg" to "¬",
            "angle" to "∠",
            "triangle" to "△",
            "perp" to "⊥",
            "parallel" to "∥",

            // Accents and decorations
            "hat{}" to "x̂ accent",
            "bar{}" to "x̄ accent",
            "vec{}" to "x⃗ accent",
            "dot{}" to "ẋ accent",
            "ddot{}" to "ẍ accent",
            "tilde{}" to "x̃ accent",
            "overline{}" to "Overline",
            "underline{}" to "Underline",
            "overbrace{}" to "Overbrace",
            "underbrace{}" to "Underbrace",

            // Delimiters
            "left(" to "Left delimiter (",
            "right)" to "Right delimiter )",
            "left[" to "Left delimiter [",
            "right]" to "Right delimiter ]",
            "left\\{" to "Left delimiter {",
            "right\\}" to "Right delimiter }",
            "langle" to "⟨",
            "rangle" to "⟩",
            "lceil" to "⌈",
            "rceil" to "⌉",
            "lfloor" to "⌊",
            "rfloor" to "⌋",

            // Environments / structures
            "begin{}" to "Begin environment",
            "end{}" to "End environment",
            "text{}" to "Text in math mode",
            "textbf{}" to "Bold text in math",
            "textit{}" to "Italic text in math",
            "mathrm{}" to "Roman font",
            "mathbf{}" to "Bold math",
            "mathit{}" to "Italic math",
            "mathcal{}" to "Calligraphic",
            "mathbb{}" to "Blackboard bold",
            "mathfrak{}" to "Fraktur",

            // Matrices
            "bmatrix" to "Bracketed matrix",
            "pmatrix" to "Parenthesized matrix",
            "vmatrix" to "Determinant matrix",
            "matrix" to "Plain matrix",
        )
    }
}
