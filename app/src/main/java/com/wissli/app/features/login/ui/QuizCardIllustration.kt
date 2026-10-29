package com.wissli.app.features.login.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wissli.app.R
import com.wissli.app.ui.preview.ComponentPreviews
import com.wissli.app.ui.preview.WissliComponentPreview
import com.wissli.app.ui.theme.Blush
import com.wissli.app.ui.theme.Hairline
import com.wissli.app.ui.theme.Ink
import com.wissli.app.ui.theme.InkMuted
import com.wissli.app.ui.theme.LavenderContainer
import com.wissli.app.ui.theme.Leaf
import com.wissli.app.ui.theme.LeafContainer
import com.wissli.app.ui.theme.Mint
import com.wissli.app.ui.theme.OnLavenderContainer
import com.wissli.app.ui.theme.OnLeafContainer
import com.wissli.app.ui.theme.OnPeachContainer
import com.wissli.app.ui.theme.OnSkyContainer
import com.wissli.app.ui.theme.PaperWhite
import com.wissli.app.ui.theme.PeachContainer
import com.wissli.app.ui.theme.SkyContainer
import com.wissli.app.ui.theme.Spacing
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Decorative sample question, treated like an image: fixed colors, hidden from screen readers and
 * scaled down as a whole on narrow displays instead of wrapping the answers.
 */
@Composable
internal fun QuizCardIllustration(modifier: Modifier = Modifier) {
    val fanOut = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fanOut.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = FAN_DAMPING, stiffness = Spring.StiffnessVeryLow),
        )
    }

    Box(
        modifier =
            modifier
                .clearAndSetSemantics {}
                .scaleDownToFitWidth(IllustrationDesignWidth),
    ) {
        Box(
            modifier =
                Modifier
                    .width(IllustrationDesignWidth)
                    .padding(start = Spacing.m, end = Spacing.m, top = Spacing.xxs, bottom = Spacing.xl),
        ) {
            val progress = { fanOut.value }
            BackCard(color = Mint, rotation = 7f, offsetX = 14.dp, offsetY = 26.dp, progress = progress)
            BackCard(color = Blush, rotation = -5f, offsetX = (-10).dp, offsetY = 30.dp, progress = progress)
            FrontCard(
                modifier =
                    Modifier.graphicsLayer {
                        rotationZ = FRONT_CARD_ROTATION * fanOut.value
                    },
            )
        }
    }
}

@Composable
private fun BoxScope.BackCard(
    color: Color,
    rotation: Float,
    offsetX: Dp,
    offsetY: Dp,
    progress: () -> Float,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = progress()
                    rotationZ = rotation * p
                    translationX = offsetX.toPx() * p
                    translationY = offsetY.toPx() * p
                }.background(color, shape),
    )
}

@Composable
private fun FrontCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = PaperWhite,
        shadowElevation = Spacing.xs,
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.login_preview_category),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnLeafContainer,
                    modifier =
                        Modifier
                            .background(LeafContainer, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.login_preview_progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
            }
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.login_preview_question),
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )
            Spacer(Modifier.height(Spacing.m))
            AnswerGrid()
        }
    }
}

@Composable
private fun AnswerGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
        PreviewAnswers.chunked(2).forEachIndexed { rowIndex, rowAnswers ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(TileGap),
            ) {
                rowAnswers.forEachIndexed { columnIndex, answer ->
                    AnswerTile(
                        letter = 'A' + rowIndex * 2 + columnIndex,
                        answer = answer,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerTile(
    letter: Char,
    answer: PreviewAnswer,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val isCorrect = answer.isCorrect
    Row(
        modifier =
            modifier
                .background(if (isCorrect) LeafContainer else PaperWhite, shape)
                .border(
                    width = if (isCorrect) 1.5.dp else 1.dp,
                    color = if (isCorrect) Leaf else Hairline,
                    shape = shape,
                ).padding(horizontal = TilePadding, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(LetterChipSize)
                    .background(if (isCorrect) Leaf else answer.chipColor, MaterialTheme.shapes.extraSmall),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) PaperWhite else answer.onChipColor,
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = stringResource(answer.textRes),
            style = MaterialTheme.typography.labelLarge,
            fontSize = AnswerFontSize,
            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isCorrect) OnLeafContainer else Ink,
        )
    }
}

/** Measures at [designWidth] and scales down (including the reported size) when space is tighter. */
private fun Modifier.scaleDownToFitWidth(designWidth: Dp): Modifier =
    layout { measurable, constraints ->
        val designWidthPx = designWidth.roundToPx()
        val placeable = measurable.measure(Constraints(minWidth = designWidthPx, maxWidth = designWidthPx))
        val availableWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else designWidthPx
        val scale = min(1f, availableWidth.toFloat() / designWidthPx)
        val scaledWidth = (designWidthPx * scale).roundToInt()
        val scaledHeight = (placeable.height * scale).roundToInt()
        val layoutWidth = availableWidth.coerceAtLeast(constraints.minWidth)
        layout(layoutWidth, scaledHeight) {
            placeable.placeWithLayer(x = (layoutWidth - scaledWidth) / 2, y = 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }

private data class PreviewAnswer(
    @param:StringRes val textRes: Int,
    val chipColor: Color,
    val onChipColor: Color,
    val isCorrect: Boolean = false,
)

private val PreviewAnswers =
    listOf(
        PreviewAnswer(R.string.login_preview_answer_a, SkyContainer, OnSkyContainer),
        PreviewAnswer(R.string.login_preview_answer_b, LeafContainer, OnLeafContainer, isCorrect = true),
        PreviewAnswer(R.string.login_preview_answer_c, LavenderContainer, OnLavenderContainer),
        PreviewAnswer(R.string.login_preview_answer_d, PeachContainer, OnPeachContainer),
    )

private val IllustrationDesignWidth = 388.dp
private val CardPadding = 18.dp
private val TileGap = 10.dp
private val TilePadding = 10.dp
private val LetterChipSize = 28.dp
private val AnswerFontSize = 14.sp
private const val FRONT_CARD_ROTATION = -2f
private const val FAN_DAMPING = 0.55f

// The fan-out animation only plays in interactive mode; the static preview shows the stacked start state.
@Preview
@Composable
internal fun QuizCardIllustrationPreview() {
    WissliComponentPreview {
        QuizCardIllustration()
    }
}
