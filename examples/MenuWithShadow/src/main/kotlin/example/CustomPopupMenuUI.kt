package example

import java.awt.*
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
      // Make the heavyweight popup window translucent after the popup has been shown
      EventQueue.invokeLater {
        val w = SwingUtilities.getWindowAncestor(popup)
        if (isTranslucentPopupWindow(w)) {
          w.background = Color(0x0, true)
        }
      }
      (SwingUtilities.getUnwrappedParent(popup) as? JComponent)?.also {
        it.border = ShadowBorder()
        it.isOpaque = false
      }
    }
    return pp
  }

  private class ShadowBorder : AbstractBorder() {
    override fun getBorderInsets(c: Component?) = Insets(0, 0, SIZE, SIZE)

    override fun getBorderInsets(
      c: Component?,
      insets: Insets,
    ): Insets {
      insets.set(0, 0, SIZE, SIZE)
      return insets
    }

    override fun paintBorder(
      c: Component?,
      g: Graphics,
      x: Int,
      y: Int,
      w: Int,
      h: Int,
    ) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.composite = AlphaComposite.SrcOver.derive(ALPHA)
      g2.paint = Color.BLACK
      g2.translate(x, y)
      // Overlap translucent rectangles while expanding them one pixel at a time,
      // so that the shadow gets lighter toward the bottom right edge
      for (i in 0..<SIZE) {
        g2.fillRoundRect(SIZE, SIZE, w - SIZE - SIZE + i, h - SIZE - SIZE + i, ARC, ARC)
      }
      g2.dispose()
    }

    companion object {
      private const val SIZE = 4
      private const val ARC = 2
      private const val ALPHA = .12f
    }
  }

  companion object {
    // https://youtrack.jetbrains.com/issue/KT-12993
    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //  the same signature as a static method in a Java base class : KT-12993
    // fun createUI(c: JComponent?): ComponentUI {
    //   return CustomPopupMenuUI()
    // }

    // A lightweight popup returns the JFrame here, which must remain opaque
    private fun isTranslucentPopupWindow(w: Window?): Boolean {
      val isHeavyWeight = w?.type == Window.Type.POPUP
      val gc = if (isHeavyWeight) w?.graphicsConfiguration else null
      return gc?.isTranslucencyCapable == true
    }
  }
}
