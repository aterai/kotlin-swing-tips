package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicInternalFrameTitlePane
import javax.swing.plaf.basic.BasicInternalFrameUI
import javax.swing.plaf.metal.MetalLookAndFeel

fun makeUI(): Component {
  val f0 = JInternalFrame("metal(default)", true, true, true, true)
  f0.setSize(240, 100)
  f0.setLocation(20, 10)
  f0.isVisible = true

  val f1 = object : JInternalFrame("basic", true, true, true, true) {
    override fun updateUI() {
      super.updateUI()
      val tmp = object : BasicInternalFrameUI(this) {
        override fun createNorthPane(w: JInternalFrame) = BumpsFreeInternalFrameTitlePane(w)
      }
      setUI(tmp)
    }
  }
  f1.setSize(240, 100)
  f1.setLocation(40, 120)
  f1.isVisible = true

  return JDesktopPane().also {
    it.add(f0)
    it.add(f1)
    it.preferredSize = Dimension(320, 240)
  }
}

private class BumpsFreeInternalFrameTitlePane(w: JInternalFrame) : BasicInternalFrameTitlePane(w) {
  init {
    border = BorderFactory.createMatteBorder(0, 0, 1, 0, MetalLookAndFeel.getPrimaryControlDarkShadow())
  }

  override fun getPreferredSize(): Dimension? = super.getPreferredSize().also {
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
