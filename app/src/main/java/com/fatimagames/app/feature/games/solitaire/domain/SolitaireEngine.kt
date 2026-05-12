package com.fatimagames.app.feature.games.solitaire.domain

import kotlin.random.Random

/**
 * Engine de Klondike (Paciência clássica) — single draw, foundations por naipe.
 *
 * Layout:
 *  - 7 colunas de tableau (col i tem i+1 cartas, a do topo virada)
 *  - 4 foundations (uma por naipe, monta do Ás ao Rei)
 *  - Stock + Waste (vira 1 carta por vez do stock pro waste)
 *
 * Regras de movimento no tableau:
 *  - Só Rei pode ir para coluna vazia
 *  - Carta segue sequência DESCENDENTE com naipe ALTERNADO (vermelho/preto)
 *
 * Regras de foundation:
 *  - Começa no Ás, sobe um a um (2, 3, ..., K) do MESMO naipe
 *
 * Vitória: todas as 52 cartas nas foundations.
 */
class SolitaireEngine(seed: Long = System.currentTimeMillis()) {

    val tableau: Array<MutableList<Card>> = Array(7) { mutableListOf() }
    val foundations: Array<MutableList<Card>> = Array(4) { mutableListOf() }
    val stock: MutableList<Card> = mutableListOf()
    val waste: MutableList<Card> = mutableListOf()

    var moves: Int = 0; private set
    var stockResets: Int = 0; private set

    init { deal(seed) }

    private fun deal(seed: Long) {
        val deck = mutableListOf<Card>()
        for (suit in Suit.entries) for (rank in Rank.entries) {
            deck.add(Card(suit, rank, faceUp = false))
        }
        deck.shuffle(Random(seed))
        for (col in 0 until 7) {
            for (j in 0..col) {
                val card = deck.removeAt(0)
                tableau[col].add(card.flip(j == col))
            }
        }
        stock.addAll(deck)
    }

    /** Vira uma carta do stock para o waste (ou reseta se stock vazio). */
    fun drawFromStock(): Boolean {
        if (stock.isEmpty()) {
            if (waste.isEmpty()) return false
            stock.addAll(waste.reversed().map { it.flip(false) })
            waste.clear()
            stockResets++
            return true
        }
        val card = stock.removeAt(stock.size - 1)
        waste.add(card.flip(true))
        moves++
        return true
    }

    /**
     * Tenta mover cartas de [from] (com `fromIndex` no caso do tableau,
     * indicando a primeira carta selecionada da pilha) para [to].
     * Retorna `true` se moveu.
     */
    fun tryMove(from: Pile, fromIndex: Int, to: Pile): Boolean {
        val cards = collectMovableCards(from, fromIndex) ?: return false
        if (cards.isEmpty()) return false
        if (!canPlaceOn(cards, to)) return false

        // Executa a movimentação
        removeFromSource(from, fromIndex)
        placeOnDest(cards, to)
        flipTopIfNeeded(from)
        moves++
        return true
    }

    /**
     * Tenta enviar a carta do topo desta pilha para a foundation correspondente
     * (atalho de auto-move via duplo-clique etc.). Retorna true se moveu.
     */
    fun autoToFoundation(from: Pile): Boolean {
        val topCard = topOf(from) ?: return false
        if (!topCard.faceUp) return false
        val fIdx = topCard.suit.foundationIndex
        return tryMove(from, lastIndexOf(from), Pile.Foundation(fIdx))
    }

    fun isWin(): Boolean = foundations.sumOf { it.size } == 52

    // ----- Internals -----

    private fun collectMovableCards(from: Pile, fromIndex: Int): List<Card>? = when (from) {
        is Pile.Tableau -> {
            val list = tableau[from.index]
            if (fromIndex !in list.indices) null
            else {
                val candidates = list.subList(fromIndex, list.size).toList()
                if (candidates.any { !it.faceUp }) null else candidates
            }
        }
        is Pile.Foundation -> {
            val list = foundations[from.index]
            if (list.isEmpty()) null else listOf(list.last())
        }
        Pile.Waste -> if (waste.isEmpty()) null else listOf(waste.last())
    }

    private fun canPlaceOn(cards: List<Card>, to: Pile): Boolean {
        if (cards.isEmpty()) return false
        return when (to) {
            is Pile.Tableau -> {
                val list = tableau[to.index]
                val first = cards.first()
                if (list.isEmpty()) first.rank == Rank.KING
                else {
                    val top = list.last()
                    if (!top.faceUp) false
                    else top.rank.value == first.rank.value + 1 && top.suit.isRed != first.suit.isRed
                }
            }
            is Pile.Foundation -> {
                if (cards.size != 1) return false
                val card = cards.first()
                val list = foundations[to.index]
                if (list.isEmpty()) card.rank == Rank.ACE && card.suit.foundationIndex == to.index
                else {
                    val top = list.last()
                    top.suit == card.suit && card.rank.value == top.rank.value + 1
                }
            }
            Pile.Waste -> false
        }
    }

    private fun removeFromSource(from: Pile, fromIndex: Int) {
        when (from) {
            is Pile.Tableau -> {
                val list = tableau[from.index]
                while (list.size > fromIndex) list.removeAt(list.size - 1)
            }
            is Pile.Foundation -> foundations[from.index].removeAt(foundations[from.index].size - 1)
            Pile.Waste -> waste.removeAt(waste.size - 1)
        }
    }

    private fun placeOnDest(cards: List<Card>, to: Pile) {
        when (to) {
            is Pile.Tableau -> tableau[to.index].addAll(cards)
            is Pile.Foundation -> foundations[to.index].addAll(cards)
            Pile.Waste -> {}
        }
    }

    private fun flipTopIfNeeded(from: Pile) {
        if (from is Pile.Tableau) {
            val list = tableau[from.index]
            if (list.isNotEmpty() && !list.last().faceUp) {
                list[list.size - 1] = list.last().flip(true)
            }
        }
    }

    private fun topOf(pile: Pile): Card? = when (pile) {
        is Pile.Tableau -> tableau[pile.index].lastOrNull()
        is Pile.Foundation -> foundations[pile.index].lastOrNull()
        Pile.Waste -> waste.lastOrNull()
    }

    private fun lastIndexOf(pile: Pile): Int = when (pile) {
        is Pile.Tableau -> (tableau[pile.index].size - 1).coerceAtLeast(0)
        is Pile.Foundation -> (foundations[pile.index].size - 1).coerceAtLeast(0)
        Pile.Waste -> 0
    }
}
