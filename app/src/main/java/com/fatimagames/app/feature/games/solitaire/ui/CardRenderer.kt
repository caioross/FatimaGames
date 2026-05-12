package com.fatimagames.app.feature.games.solitaire.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatimagames.app.feature.games.solitaire.domain.Card

private val Back1 = Color(0xFF4F6A53)
private val Back2 = Color(0xFF2C3E33)
private val FaceBg = Color(0xFFFFFEF7)
private val RedInk = Color(0xFFB8351F)
private val BlackInk = Color(0xFF1F1B17)

@Composable
fun PlayingCard(
    card: Card?,
    width: Dp = 56.dp,
    height: Dp = 80.dp,
    selected: Boolean = false,
    placeholder: Boolean = false,
) {
    val cornerShape = RoundedCornerShape(6.dp)
    if (card == null) {
        // Placeholder (slot vazio)
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .clip(cornerShape)
                .background(Color.Black.copy(alpha = 0.10f))
                .border(1.dp, Color.Black.copy(alpha = 0.25f), cornerShape),
        )
        return
    }
    if (!card.faceUp) {
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .clip(cornerShape)
                .background(Back1)
                .border(1.dp, Color.Black.copy(alpha = 0.4f), cornerShape),
        ) {
            // Padrão de diamantes simples
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Back2)
                    .border(1.dp, Color(0xFFD4A24A).copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("♣", color = Color(0xFFD4A24A).copy(alpha = 0.7f), fontWeight = FontWeight.W700)
            }
        }
        return
    }
    val ink = if (card.suit.isRed) RedInk else BlackInk
    val borderColor = if (selected) Color(0xFFD4A24A) else Color.Black.copy(alpha = 0.4f)
    val borderW = if (selected) 2.5.dp else 1.dp
    Column(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(cornerShape)
            .background(FaceBg)
            .border(borderW, borderColor, cornerShape)
            .padding(4.dp),
    ) {
        // Topo esquerda: rank + suit pequenos
        Row {
            Text(
                card.rank.symbol,
                color = ink,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W700),
            )
            Text(
                card.suit.symbol,
                color = ink,
                style = TextStyle(fontSize = 12.sp),
            )
        }
        Spacer(Modifier.height(2.dp))
        // Centro: naipe grande
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                card.suit.symbol,
                color = ink,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.W600),
            )
        }
    }
}
