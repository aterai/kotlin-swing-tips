package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View

open class BasicTabViewButtonUI : TabViewButtonUI() {
  override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.preferredSize = Dimension(0, 24)
    b.isRolloverEnabled = true
    b.isOpaque = true
    val outer = BorderFactory.createMatteBorder(2, 0, 0, 0, b.background)
    val inner = BorderFactory.createMatteBorder(1, 1, 0, 1, Color.RED)
    b.border = BorderFactory.createCompoundBorder(outer, inner)
    (b as? TabButton)?.also {
      it.textColor = Color(100, 100, 100)
      it.pressedTc = Color.GRAY
      it.rolloverTc = Color.BLACK
      it.rolloverSelTc = Color.GRAY
      it.selectedTc = Color.BLACK
    }
  }

  // @Override public void uninstallUI(JComponent c) {
  //   super.uninstallUI(c)
  //   this.tabViewButton = null
  // }

  // @Override public void installDefaults() {}

  override fun paint(g: Graphics, c: JComponent) {
    val b = c as? AbstractButton ?: return
    g.font = c.font

    // val i = c.getInsets()
    // b.getSize(size)
    // viewRect.x = i.left
    // viewRect.y = i.top
    // viewRect.width = size.width - i.right - viewRect.x
    // viewRect.height = size.height - i.bottom - viewRect.y
    SwingUtilities.calculateInnerArea(b, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)

    val text = SwingUtilities.layoutCompoundLabel(
      c,
      c.getFontMetrics(c.font),
      b.text, null,
      b.verticalAlignment,
      b.horizontalAlignment,
      b.verticalTextPosition,
      b.horizontalTextPosition,
      viewRect,
      iconRect,
      textRect,
      0
    )

    g.color = b.background
    g.fillRect(0, 0, size.width, size.height)

    val model = b.model
    g.color = if (model.isSelected || model.isArmed) Color.WHITE else Color(220, 220, 220)
    g.fillRect(
      viewRect.x,
      viewRect.y,
      viewRect.x + viewRect.width,
      viewRect.y + viewRect.height
    )

    val color = Color(255, 120, 40)
    if (model.isSelected) {
      g.color = color
      g.drawLine(viewRect.x + 1, viewRect.y - 2, viewRect.x + viewRect.width - 1, viewRect.y - 2)
      g.color = color.brighter()
      g.drawLine(viewRect.x + 0, viewRect.y - 1, viewRect.x + viewRect.width - 0, viewRect.y - 1)
      g.color = color
      g.drawLine(viewRect.x + 0, viewRect.y - 0, viewRect.x + viewRect.width - 0, viewRect.y - 0)
    } else if (model.isRollover) {
      g.color = color
      g.drawLine(viewRect.x + 1, viewRect.y + 0, viewRect.x + viewRect.width - 1, viewRect.y + 0)
      g.color = color.brighter()
      g.drawLine(viewRect.x + 0, viewRect.y + 1, viewRect.x + viewRect.width - 0, viewRect.y + 1)
      g.color = color
      g.drawLine(viewRect.x + 0, viewRect.y + 2, viewRect.x + viewRect.width - 0, viewRect.y + 2)
    }
    val v = c.getClientProperty(BasicHTML.propertyKey)
    if (v is View) {
      v.paint(g, textRect)
    } else {
      if (model.isSelected) {
        textRect.y -= 2
        textRect.x -= 1
      }
      textRect.x += 4
      paintText(g, b, textRect, text)
    }
  }

  companion object {
    private val size = Dimension()
    private val viewRect = Rectangle()
    private val iconRect = Rectangle()
    private val textRect = Rectangle()

    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //   the same signature as a static method in a Java base class : KT-12993
    // https://youtrack.jetbrains.com/issue/KT-12993
    // @JvmStatic
    // fun createUI(c: JComponent) = BasicTabViewButtonUI()
  }
}
