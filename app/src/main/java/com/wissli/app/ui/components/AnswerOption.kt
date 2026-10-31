package com.wissli.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.CoralContainer
import com.wissli.app.ui.theme.LavenderContainer
import com.wissli.app.ui.theme.Leaf
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.OnCoralContainer
import com.wissli.app.ui.theme.OnLavenderContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnPeachContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.PeachContainer
import com.wissli.app.ui.theme.SkyContainer
import com.wissli.app.ui.theme.Spacing

/** One multiple-choice answer with its letter (A, B, …), shared by levels and the daily question. */
@Composable
fun AnswerOption(
    index: Int,
    text: String,
    look: AnswerLook,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (letterContainer, letterContent) = LetterColors[index % LetterColors.size]
    val (container, content, border) =
        when (look) {
            AnswerLook.CORRECT -> Triple(LeafContainer, OnLeafContainer, Leaf)
            AnswerLook.WRONG -> Triple(CoralContainer, OnCoralContainer, OnCoralContainer)
            AnswerLook.IDLE, AnswerLook.DIMMED ->
                Triple(
                    MaterialTheme.colorScheme.surfaceContainerLowest,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.outlineVariant,
                )
        }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = AnswerMinHeight)
                .alpha(if (look == AnswerLook.DIMMED) DIMMED_ALPHA else 1f),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        border = BorderStroke(if (look == AnswerLook.IDLE) 1.dp else 2.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(LetterSize)
                        .background(letterContainer, MaterialTheme.shapes.small)
                        .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ('A' + index).toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = letterContent,
                )
            }
            Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

/** Shown once a question is answered: right or wrong, an optional [hint] and the explanation. */
@Composable
fun AnswerFeedbackCard(
    isCorrect: Boolean,
    title: String,
    explanation: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isCorrect) LeafContainer else CoralContainer,
        contentColor = if (isCorrect) OnLeafContainer else OnCoralContainer,
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            if (hint != null) {
                Text(text = hint, style = MaterialTheme.typography.labelLarge)
            }
            Text(text = explanation, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val LetterColors: List<Pair<Color, Color>> =
    listOf(
        SkyContainer to OnSkyContainer,
        LeafContainer to OnLeafContainer,
        LavenderContainer to OnLavenderContainer,
        PeachContainer to OnPeachContainer,
    )

private val AnswerMinHeight = 64.dp
private val LetterSize = 40.dp
private const val DIMMED_ALPHA = 0.5f

@ComponentPreviews
@Composable
internal fun AnswerOptionPreview() {
    WissliComponentPreview {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AnswerLook.entries.forEachIndexed { index, look ->
                AnswerOption(index = index, text = look.name, look = look, enabled = false, onClick = {})
            }
        }
    }
}
