package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicInternalFrameTitlePane
import javax.swing.plaf.basic.BasicInternalFrameUI
import javax.swing.plaf.metal.MetalLookAndFeel

fun createUI(): Component {
  val metalFrame = JInternalFrame("metal(default)", true, true, true, true)
  // TEST: metalFrame.setUI(BasicInternalFrameUI(metalFrame))
  metalFrame.setSize(240, 100)
  metalFrame.setLocation(20, 10)

  val basicFrame = object : JInternalFrame("basic", true, true, true, true) {
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
  basicFrame.setSize(240, 100)
  basicFrame.setLocation(40, 120)

  return JDesktopPane().also {
    it.add(metalFrame)
    it.add(basicFrame)
    EventQueue.invokeLater {
      for (f in it.allFrames) {
        f.isVisible = true
      }
    }
    it.preferredSize = Dimension(320, 240)
  }
}

private class BumpsFreeInternalFrameTitlePane(
  frame: JInternalFrame,
) : BasicInternalFrameTitlePane(frame) {
  override fun paintTitleBackground(g: Graphics) {
    super.paintTitleBackground(g)
    val shadow = if (frame.isSelected) {
      MetalLookAndFeel.getPrimaryControlDarkShadow()
    } else {
      MetalLookAndFeel.getControlDarkShadow()
    }
    g.color = shadow
    // Same edges as MetalInternalFrameTitlePane: bottom line and top corner dots
    g.drawLine(0, height - 1, width, height - 1)
    g.drawLine(0, 0, 0, 0)
    g.drawLine(width - 1, 0, width - 1, 0)
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    it.height = 24
  }

  override fun createButtons() {
    super.createButtons()
    listOf(closeButton, maxButton, iconButton).forEach {
      it.isContentAreaFilled = false
      // Removes the button border; TitlePaneLayout sets fixed 16x14 bounds,
      // so these insets do not enlarge the button
      it.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
