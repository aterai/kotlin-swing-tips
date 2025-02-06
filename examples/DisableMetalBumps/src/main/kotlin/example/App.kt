package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicInternalFrameTitlePane
import javax.swing.plaf.basic.BasicInternalFrameUI
import javax.swing.plaf.metal.MetalLookAndFeel

fun makeUI(): Component {
  val f0 = JInternalFrame("metal(default)", true, true, true, true)
  f0.setSize(240, 100)
  f0.setLocation(20, 10)

  val f1 = object : JInternalFrame("basic", true, true, true, true) {
    override fun updateUI() {
      super.updateUI()
      val ui1 = object : BasicInternalFrameUI(this) {
        override fun createNorthPane(
          w: JInternalFrame,
        ) = BumpsFreeInternalFrameTitlePane(w)
      }
      setUI(ui1)
    }
  }
  f1.setSize(240, 100)
  f1.setLocation(40, 120)

  return JDesktopPane().also {
    it.add(f0)
    it.add(f1)
    EventQueue.invokeLater {
      it.allFrames.forEach { f -> f.isVisible = true }
    }
    it.preferredSize = Dimension(320, 240)
  }
}

private class BumpsFreeInternalFrameTitlePane(
  frame: JInternalFrame,
) : BasicInternalFrameTitlePane(frame) {
  override fun updateUI() {
    super.updateUI()
  }

  override fun paintTitleBackground(g: Graphics) {
    super.paintTitleBackground(g)
    val shadow = if (frame.isSelected) {
      MetalLookAndFeel.getPrimaryControlDarkShadow()
    } else {
      MetalLookAndFeel.getControlDarkShadow()
    }
    g.color = shadow
    g.drawLine(0, height - 1, width, height - 1)
    g.drawLine(0, 0, 0, 0)
    g.drawLine(width - 1, 0, width - 1, 0)
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    it.height = 24
  }

  public override fun createButtons() {
    super.createButtons()
    listOf(closeButton, maxButton, iconButton).forEach {
      it.isContentAreaFilled = false
      it.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
