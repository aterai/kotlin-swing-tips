package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View

class OperaTabViewButtonUI : BasicTabViewButtonUI() {
  private val size = Dimension()
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.border = BorderFactory.createEmptyBorder()
    b.foreground = Color.WHITE
    (b as? TabButton)?.also {
      it.textColor = Color(230, 245, 255)
      it.pressedTextColor = Color.WHITE.darker()
      it.rolloverTextColor = Color.WHITE
      it.rolloverSelectedTextColor = Color.WHITE
      it.selectedTextColor = Color.WHITE
    }
  }

  // @Override public void installUI(JComponent c) {
  //   super.installUI(c);
  // }

  override fun paint(g: Graphics, c: JComponent) {
    val b = c as? AbstractButton ?: return
    val f = c.getFont()
    g.font = f

    val i = c.getInsets()
    b.getSize(size)
    viewRect.x = i.left
    viewRect.y = i.top
    viewRect.width = size.width - i.right - viewRect.x
    viewRect.height = size.height - i.bottom - viewRect.y
    iconRect.setBounds(0, 0, 0, 0) // .x = iconRect.y = iconRect.width = iconRect.height = 0;
    textRect.setBounds(0, 0, 0, 0) // .x = textRect.y = textRect.width = textRect.height = 0;

    val g2 = g.create() as? Graphics2D ?: return
    // g2.setPaint(Color.CYAN); // c.getBackground());
    // g2.fillRect(0, 0, size.width - 1, size.height);
    // g2.fill(viewRect);
    tabPainter(g2, viewRect)

    val icon = b.icon
    viewRect.width = size.width - i.right - viewRect.x - CLOSE_ICON_WIDTH
    val text = SwingUtilities.layoutCompoundLabel(
      c, c.getFontMetrics(f), b.text, icon, // altIcon != null ? altIcon : getDefaultIcon(),
      b.verticalAlignment, b.horizontalAlignment,
      b.verticalTextPosition, b.horizontalTextPosition,
      viewRect, iconRect, textRect,
      if (b.text != null) b.iconTextGap else 0
    )

    val v = c.getClientProperty(BasicHTML.propertyKey) as? View
    if (v != null) {
      v.paint(g, textRect)
    } else {
      textRect.x += 4
      paintText(g, b, textRect, text)
    }
    icon?.paintIcon(c, g, iconRect.x + 4, iconRect.y + 2)

    val model = b.model
    if (!model.isSelected && !model.isArmed && !model.isRollover) {
      g2.paint = Color(0x64_00_00_00, true)
      g2.fillRect(0, 0, size.width, size.height)
      // g2.fill(viewRect);
    }
    g2.dispose()
  }

  companion object {
    private const val CLOSE_ICON_WIDTH = 12

    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //   the same signature as a static method in a Java base class : KT-12993
    // https://youtrack.jetbrains.com/issue/KT-12993
    // @JvmStatic
    // fun createUI(c: JComponent): ComponentUI {
    //   return OperaTabViewButtonUI()
    // }

    fun tabPainter(g2: Graphics2D, r: Rectangle) {
      val r1 = Rectangle(r.x, r.y, r.width, r.height / 2)
      val r2 = Rectangle(r.x, r.y + r.height / 2, r.width, r.height / 2)
      val r3 = Rectangle(r.x, r.y + r.height / 2 - 2, r.width, r.height / 4)

      g2.paint = GradientPaint(
        0f,
        r1.y.toFloat(),
        Color(0x84_A2_B4),
        0f,
        (r1.y + r1.height).toFloat(),
        Color(0x67_85_98),
        true
      )
      g2.fill(r1)
      g2.paint = GradientPaint(
        0f,
        r2.y.toFloat(),
        Color(0x32_49_54),
        0f,
        (r2.y + r2.height).toFloat(),
        Color(0x3C_56_65),
        true
      )
      g2.fill(r2)
      g2.paint = GradientPaint(
        0f,
        r3.y.toFloat(),
        Color(0, 0, 0, 30),
        0f,
        (r3.y + r3.height).toFloat(),
        Color(0, 0, 0, 5),
        true
      )
      g2.fill(r3)

      g2.paint = Color(39, 56, 67) // g2.setPaint(Color.GREEN);
      g2.drawLine(r.x, r.y, r.x + r.width, r.y)

      g2.paint = Color(255, 255, 255, 30) // g2.setPaint(Color.RED);
      g2.drawLine(r.x + 1, r.y + 1, r.x + r.width, r.y + 1)

      g2.paint = Color(255, 255, 255, 60) // g2.setPaint(Color.BLUE);
      g2.drawLine(r.x, r.y, r.x, r.y + r.height)

      g2.paint = Color(39, 56, 67, 250) // g2.setPaint(Color.YELLOW);
      g2.drawLine(r.x + r.width - 1, r.y, r.x + r.width - 1, r.y + r.height)

      // g2.setPaint(Color.PINK);
      g2.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1)
    }
  }
}
