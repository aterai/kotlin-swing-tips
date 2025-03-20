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

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    val b = c as? AbstractButton ?: return
    g.font = c.font

    // val i = c.getInsets()
    // b.getSize(size)
    // viewRect.x = i.left
    // viewRect.y = i.top
    // viewRect.width = size.width - i.right - viewRect.x
    // viewRect.height = size.height - i.bottom - viewRect.y
    SwingUtilities.calculateInnerArea(b, vr)
    ir.setBounds(0, 0, 0, 0)
    tr.setBounds(0, 0, 0, 0)

    val text = SwingUtilities.layoutCompoundLabel(
      c,
      c.getFontMetrics(c.font),
      b.text,
      null,
      b.verticalAlignment,
      b.horizontalAlignment,
      b.verticalTextPosition,
      b.horizontalTextPosition,
      vr,
      ir,
      tr,
      0,
    )

    g.color = b.background
    g.fillRect(0, 0, size.width, size.height)

    val model = b.model
    val isArmed = model.isSelected || model.isArmed
    g.color = if (isArmed) Color.WHITE else Color(220, 220, 220)
    g.fillRect(vr.x, vr.y, vr.x + vr.width, vr.y + vr.height)

    val color = Color(255, 120, 40)
    if (model.isSelected) {
      g.color = color
      g.drawLine(vr.x + 1, vr.y - 2, vr.x + vr.width - 1, vr.y - 2)
      g.color = color.brighter()
      g.drawLine(vr.x + 0, vr.y - 1, vr.x + vr.width - 0, vr.y - 1)
      g.color = color
      g.drawLine(vr.x + 0, vr.y - 0, vr.x + vr.width - 0, vr.y - 0)
    } else if (model.isRollover) {
      g.color = color
      g.drawLine(vr.x + 1, vr.y + 0, vr.x + vr.width - 1, vr.y + 0)
      g.color = color.brighter()
      g.drawLine(vr.x + 0, vr.y + 1, vr.x + vr.width - 0, vr.y + 1)
      g.color = color
      g.drawLine(vr.x + 0, vr.y + 2, vr.x + vr.width - 0, vr.y + 2)
    }
    val v = c.getClientProperty(BasicHTML.propertyKey)
    if (v is View) {
      v.paint(g, tr)
    } else {
      if (model.isSelected) {
        tr.y -= 2
        tr.x -= 1
      }
      tr.x += 4
      paintText(g, b, tr, text)
    }
  }

  companion object {
    private val size = Dimension()
    private val vr = Rectangle()
    private val ir = Rectangle()
    private val tr = Rectangle()

    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //   the same signature as a static method in a Java base class : KT-12993
    // https://youtrack.jetbrains.com/issue/KT-12993
    // @JvmStatic
    // fun createUI(c: JComponent) = BasicTabViewButtonUI()
  }
}
