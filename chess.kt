// import statements

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

// credit to https://greenchess.net/info.php?item=downloads for the chess piece images

// terminal commands for launch:
// #1:: Ensure you are routed into the proper directory. Utilize 'cd' along with the pathing.
// #2:: Run the program with the command :: java -jar chess.jar
// #3:: Have fun moving pieces (the 'active' player swaps after each move, so this is only single player, no AI)

fun main() {
    SwingUtilities.invokeLater { ChessFrame() }
}

class ChessFrame : JFrame("Kotlin Chess") {
    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        add(ChessBoardPanel())
        pack()
        isVisible = true
    }
}

class ChessBoardPanel : JPanel() {
    private val boardSize = 8
    private val squareSize = 128
    private var selected: Pair<Int, Int>? = null
    private var highlighted: Pair<Int, Int>? = null
    private var hover: Pair<Int, Int>? = null
    private var turn = true  // true = white, false = black

    private val pieces = Array(boardSize) { Array<Piece?>(boardSize) { null } }

    private val pieceImages = mutableMapOf<String, Image>()

    init {
        preferredSize = Dimension(squareSize * boardSize, squareSize * boardSize)
        val pieceTypes = mapOf("k" to "king", "q" to "queen", "r" to "rook", "b" to "bishop", "n" to "knight", "p" to "pawn")
        for (color in listOf("white", "black")) {
            val prefix = if (color == "white") "w" else "b"
            for ((type, name) in pieceTypes) {
                try {
                    val img = ImageIO.read(java.io.File("$color-$name.png"))
                    pieceImages["$prefix$type"] = img.getScaledInstance(squareSize, squareSize, Image.SCALE_SMOOTH)
                } catch (e: Exception) {
                    println("Missing $color-$name.png - using letters")
                }
            }
        }

        placePieces() //gotta place them somewhere

        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val col = e.x / squareSize
                val row = e.y / squareSize
                if (col in 0..7 && row in 0..7) {
                    if (e.button == MouseEvent.BUTTON1) { //clicking logic
                        handleClick(row, col)
                    } else if (e.button == MouseEvent.BUTTON3) {
                        highlighted = if (highlighted == row to col) null else row to col
                        repaint()
                    }
                }
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val col = e.x / squareSize
                val row = e.y / squareSize
                hover = if (col in 0..7 && row in 0..7) row to col else null
                repaint()
            }
        })
    }

    private fun placePieces() {
        val backRow = arrayOf("r", "n", "b", "q", "k", "b", "n", "r")

        //fun fact, K is reserved for the king in notation
        //while the Knight must utilize N

        for (i in 0..7) {
            pieces[1][i] = Piece("p", false)
            pieces[6][i] = Piece("p", true)
            pieces[0][i] = Piece(backRow[i], false)
            pieces[7][i] = Piece(backRow[i], true)
        }
    }

    private fun handleClick(row: Int, col: Int) {
        val piece = pieces[row][col]
        if (selected == null) {
            if (piece != null && piece.white == turn) {
                selected = row to col
            }
        } else {
            val (sr, sc) = selected!!
            if (sr == row && sc == col) {
                selected = null  // deselect
            } else if (isValidMove(sr, sc, row, col)) {
                pieces[row][col] = pieces[sr][sc]
                pieces[sr][sc] = null
                pieces[row][col]!!.moved = true

                // Handle castling
                if (pieces[row][col]!!.type == "k" && kotlin.math.abs(col - sc) == 2) {
                    val rookCol = if (col > sc) 7 else 0
                    val newRookCol = if (col > sc) col - 1 else col + 1
                    pieces[row][newRookCol] = pieces[row][rookCol]
                    pieces[row][rookCol] = null
                    pieces[row][newRookCol]!!.moved = true
                }
                turn = !turn
                selected = null
                highlighted = null
            } else if (piece != null && piece.white == turn) {
                selected = row to col
            }
        }
        repaint()
    }

    private fun isValidMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val piece = pieces[fromRow][fromCol] ?: return false
        val target = pieces[toRow][toCol]
        if (target != null && target.white == piece.white) return false

        val dr = toRow - fromRow
        val dc = toCol - fromCol
        val absDr = kotlin.math.abs(dr)
        val absDc = kotlin.math.abs(dc)

        when (piece.type) {
            //I shall dub it, 'the beautiful headache'
            //Oh wait, It's called chess
            "p" -> {
                val dir = if (piece.white) -1 else 1
                if (dc == 0 && target == null) {
                    if (dr == dir) return true
                    if (dr == 2 * dir && ((piece.white && fromRow == 6) || (!piece.white && fromRow == 1))) {
                        if (pieces[fromRow + dir][fromCol] == null) return true
                    }
                } else if (absDc == 1 && dr == dir && target != null) return true
            }
            "r" -> if (dr == 0 || dc == 0) return clearPath(fromRow, fromCol, toRow, toCol)
            "n" -> if ((absDr == 2 && absDc == 1) || (absDr == 1 && absDc == 2)) return true
            "b" -> if (absDr == absDc) return clearPath(fromRow, fromCol, toRow, toCol)
            "q" -> if (dr == 0 || dc == 0 || absDr == absDc) return clearPath(fromRow, fromCol, toRow, toCol)
            "k" -> {
                if (absDr <= 1 && absDc <= 1) return true
                else if (absDc == 2 && dr == 0 && !piece.moved) {
                    val rookCol = if (dc > 0) 7 else 0
                    val rook = pieces[fromRow][rookCol]
                    if (rook != null && rook.type == "r" && rook.white == piece.white && !rook.moved) {
                        val start = minOf(fromCol, rookCol) + 1
                        val end = maxOf(fromCol, rookCol) - 1
                        for (c in start..end) {
                            if (pieces[fromRow][c] != null) return false
                        }
                        return true
                    }
                }
                return false
            }
        }
        return false
    }

    private fun clearPath(fr: Int, fc: Int, tr: Int, tc: Int): Boolean {
        val dr = Integer.signum(tr - fr)
        val dc = Integer.signum(tc - fc)
        var r = fr + dr
        var c = fc + dc
        while (r != tr || c != tc) {
            if (pieces[r][c] != null) return false
            r += dr
            c += dc
        }
        return true
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                g2.color = if ((r + c) % 2 == 0) Color(240, 217, 181) else Color(139, 69, 19)
                g2.fillRect(c * squareSize, r * squareSize, squareSize, squareSize)
            }
        }

        // Highlight Code
        selected?.let { (r, c) ->
            g2.color = Color(0, 255, 0, 100)
            g2.fillRect(c * squareSize, r * squareSize, squareSize, squareSize)
        }

        highlighted?.let { (r, c) ->
            g2.color = Color(255, 0, 0, 100)
            g2.fillRect(c * squareSize, r * squareSize, squareSize, squareSize)
        }
        hover?.let { (r, c) ->
            if (selected == null || selected != r to c) {
                g2.color = Color(255, 255, 0, 50)
                g2.fillRect(c * squareSize, r * squareSize, squareSize, squareSize)
            }
        }

        // Draw pieces
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                pieces[r][c]?.let { p ->
                    val key = if (p.white) "w${p.type}" else "b${p.type}"
                    pieceImages[key]?.let {
                        g2.drawImage(it, c * squareSize, r * squareSize, null)
                    } ?: run {
                        g2.color = if (p.white) Color.WHITE else Color.BLACK
                        g2.font = Font("Serif", Font.BOLD, squareSize / 2)
                        g2.drawString(if (p.white) p.type.uppercase() else p.type.lowercase(), c * squareSize + 20, r * squareSize + squareSize / 2 + 20)
                    }
                }
            }
        }
    }
}

data class Piece(val type: String, val white: Boolean, var moved: Boolean = false)  // type: p,r,n,b,q,k