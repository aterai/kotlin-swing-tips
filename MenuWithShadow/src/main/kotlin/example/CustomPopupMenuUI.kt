package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.plaf.basic.BasicPopupMenuUI

class CustomPopupMenuUI : BasicPopupMenuUI() {
  override fun getPopup(
    popup: JPopupMenu?,
    x: Int,
    y: Int,
  ): Popup? {
    val pp = super.getPopup(popup, x, y)
    if (pp != null) {
      EventQueue.invokeLater {
        (SwingUtilities.getWindowAncestor(popup) as? JWindow)
          ?.takeIf { it.type == Window.Type.POPUP }
          ?.background = Color(0x0, true)
      }
      (SwingUtilities.getUnwrappedParent(popup) as? JComponent)?.also {
        it.border = ShadowBorderInPanel()
        it.isOpaque = false
      }
    }
    return pp
  }

  private class ShadowBorderInPanel : AbstractBorder() {
    override fun getBorderInsets(c: Component?) = Insets(0, 0, OFF, OFF)

    override fun paintBorder(
      c: Component?,
      g: Graphics,
      x: Int,
      y: Int,
      w: Int,
      h: Int,
    ) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.drawImage(makeShadowImage(x, y, w, h), x, y, c)
      g2.dispose()
    }

    fun makeShadowImage(
      x: Int,
      y: Int,
      w: Int,
      h: Int,
    ): BufferedImage {
      val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
      val g2 = image.createGraphics()
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALPHA)
      g2.paint = Color.BLACK
      g2.translate(x, y)
      for (i in 0 until OFF) {
        g2.fillRoundRect(OFF, OFF, w - OFF - OFF + i, h - OFF - OFF + i, ARC, ARC)
      }
      g2.dispose()
      return image
    }

    companion object {
      private const val OFF = 4
      private const val ARC = 2
      private const val ALPHA = .12f
      // https://youtrack.jetbrains.com/issue/KT-12993
      // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
      //  the same signature as a static method in a Java base class : KT-12993
      // fun createUI(c: JComponent?): ComponentUI {
      //   return CustomPopupMenuUI()
      // }
    }
  }
}
