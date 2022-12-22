package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.util.Objects
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultCaret
import javax.swing.text.JTextComponent
import javax.swing.text.Position.Bias
import javax.swing.text.Utilities

fun makeUI(): Component {
  val text = "The quick brown fox jumps over the lazy dog.\n".repeat(3)
  val textArea = object : JTextArea("setCaret\n$text") {
    override fun updateUI() {
      caret = null
      super.updateUI()
      val oldCaret = caret
      val blinkRate = oldCaret.blinkRate
      val caret = SelectWordCaret()
      caret.blinkRate = blinkRate
      setCaret(caret)
    }
  }
  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(JTextArea("default\n$text")))
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class SelectingMode {
  CHAR, WORD, ROW
}

private class SelectWordCaret : DefaultCaret() {
  private var selectingMode = SelectingMode.CHAR
  private var p0 = 0
  private var p1 = 0

  override fun mousePressed(e: MouseEvent) {
    super.mousePressed(e)
    val clickCount = e.clickCount
    if (SwingUtilities.isLeftMouseButton(e) && !e.isConsumed && clickCount > 1) {
      val isDoubleClicked = clickCount == 2
      if (isDoubleClicked) {
        selectingMode = SelectingMode.WORD
        p0 = minOf(dot, mark)
        p1 = maxOf(dot, mark)
      } else {
        selectingMode = SelectingMode.ROW
        val target = component
        val offs = target.caretPosition
        runCatching {
          p0 = Utilities.getRowStart(target, offs)
          p1 = Utilities.getRowEnd(target, offs)
          dot = p0
          moveDot(p1)
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(target)
        }
      }
    } else {
      selectingMode = SelectingMode.CHAR
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (!e.isConsumed && SwingUtilities.isLeftMouseButton(e)) {
      when (selectingMode) {
        SelectingMode.WORD -> continuouslySelectWords(e)
        SelectingMode.ROW -> continuouslySelectRows(e)
        SelectingMode.CHAR -> super.mouseDragged(e)
      }
    } else {
      super.mouseDragged(e)
    }
  }

  private fun getCaretPositionByLocation(c: JTextComponent, pt: Point, biasRet: Array<Bias?>): Int {
    val pos = c.ui.viewToModel(c, pt, biasRet)
    if (biasRet[0] == null) {
      biasRet[0] = Bias.Forward
    }
    return pos
  }

  private fun continuouslySelectWords(e: MouseEvent) {
    val biasRet = arrayOfNulls<Bias>(1)
    val c = component
    val pos = getCaretPositionByLocation(c, e.point, biasRet)
    runCatching {
      when {
        pos in p0 until p1 -> {
          dot = p0
          moveDot(p1, biasRet[0])
        }
        p1 < pos -> {
          dot = p0
          moveDot(Utilities.getWordEnd(c, pos), biasRet[0])
        }
        p0 > pos -> {
          dot = p1
          moveDot(Utilities.getWordStart(c, pos), biasRet[0])
        }
      }
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(c)
    }
  }

  private fun continuouslySelectRows(e: MouseEvent) {
    val biasRet = arrayOfNulls<Bias>(1)
    val c = component
    val pos = getCaretPositionByLocation(c, e.point, biasRet)
    runCatching {
      when {
        pos in p0 until p1 -> {
          dot = p0
          moveDot(p1, biasRet[0])
        }
        p1 < pos -> {
          dot = p0
          moveDot(Utilities.getRowEnd(c, pos), biasRet[0])
        }
        p0 > pos -> {
          dot = p1
          moveDot(Utilities.getRowStart(c, pos), biasRet[0])
        }
      }
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(c)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is SelectWordCaret || !super.equals(other)) {
      return false
    }
    return p0 == other.p0 && p1 == other.p1
  }

  override fun hashCode() = Objects.hash(super.hashCode(), p0, p1)
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
