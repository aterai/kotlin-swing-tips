package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI

private const val LF = "\n"

fun makeUI(): Component {
  val buf = StringBuilder()
  for (i in 0..100) {
    buf.append(i).append(LF)
  }
  val scroll = JScrollPane(JTextArea(buf.toString()))
  val model = scroll.getVerticalScrollBar().getModel()
  scroll.setColumnHeaderView(ScrollIndicator(model))
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ScrollIndicator(
  model: BoundedRangeModel,
) : JProgressBar(model) {
  override fun updateUI() {
    super.updateUI()
    setUI(ScrollIndicatorUI())
    setBorder(BorderFactory.createEmptyBorder())
  }

  override fun getPercentComplete(): Double {
    val span = (model.maximum - model.minimum).toLong()
    val currentValue = (model.value + model.extent).toDouble()
    return (currentValue - model.minimum) / span
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    if (getOrientation() == HORIZONTAL) {
      d.height = 4
    } else {
      d.width = 4
    }
    return d
  }
}

private class ScrollIndicatorUI : BasicProgressBarUI() {
  override fun paintDeterminate(g: Graphics, c: JComponent?) {
    val b = progressBar.getInsets()
    val r = SwingUtilities.calculateInnerArea(progressBar, null)
    val g2 = g.create()
    if (!r.isEmpty && g2 is Graphics2D) {
      g2.color = UIManager.getColor("ProgressBar.foreground")
      val amountFull = getAmountFull(b, r.width, r.height)
      if (progressBar.getOrientation() == SwingConstants.HORIZONTAL) {
        g2.fillRect(r.x, r.y, amountFull, r.height)
      } else { // VERTICAL
        g2.fillRect(r.x, r.y + r.height - amountFull, r.width, amountFull)
      }
      if (progressBar.isStringPainted) {
        paintString(g2, r.x, r.y, r.width, r.height, amountFull, b)
      }
    }
    g2.dispose()
  }
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
