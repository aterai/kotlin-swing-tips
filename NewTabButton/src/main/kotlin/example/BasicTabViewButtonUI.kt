package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View

open class BasicTabViewButtonUI : TabViewButtonUI() {

  protected override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.setPreferredSize(Dimension(0, 24))
    b.setRolloverEnabled(true)
    b.setOpaque(true)
    val outer = BorderFactory.createMatteBorder(2, 0, 0, 0, b.getBackground())
    val inner = BorderFactory.createMatteBorder(1, 1, 0, 1, Color.RED)
    b.setBorder(BorderFactory.createCompoundBorder(outer, inner))
    // b.setForeground(Color.GREEN);

    (b as? TabButton)?.also {
      it.textColor = Color(100, 100, 100)
      it.pressedTextColor = Color.GRAY
      it.rolloverTextColor = Color.BLACK
      it.rolloverSelectedTextColor = Color.GRAY
      it.selectedTextColor = Color.BLACK
    }
  }

  // @Override public void uninstallUI(JComponent c) {
  //   super.uninstallUI(c)
  //   this.tabViewButton = null
  // }

  // @Override public void installDefaults() {}

  override fun paint(g: Graphics, c: JComponent) {
    val b = c as? AbstractButton ?: return
    val f = c.getFont()
    g.setFont(f)

    val i = c.getInsets()
    b.getSize(size)
    viewRect.x = i.left
    viewRect.y = i.top
    viewRect.width = size.width - i.right - viewRect.x
    viewRect.height = size.height - i.bottom - viewRect.y
    iconRect.setBounds(0, 0, 0, 0) // .x = iconRect.y = iconRect.width = iconRect.height = 0;
    textRect.setBounds(0, 0, 0, 0) // .x = textRect.y = textRect.width = textRect.height = 0;

    val text = SwingUtilities.layoutCompoundLabel(
      c, c.getFontMetrics(f), b.getText(), null, // altIcon != null ? altIcon : getDefaultIcon(),
      b.getVerticalAlignment(), b.getHorizontalAlignment(),
      b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
      viewRect, iconRect, textRect,
      0
    ) // b.getText() == null ? 0 : b.getIconTextGap());

    g.setColor(b.getBackground())
    g.fillRect(0, 0, size.width, size.height)

    val model = b.getModel()
    if (model.isSelected() || model.isArmed()) {
      g.setColor(Color.WHITE)
    } else {
      g.setColor(Color(220, 220, 220))
    }
    g.fillRect(
      viewRect.x, viewRect.y,
      viewRect.x + viewRect.width, viewRect.y + viewRect.height
    )

    val color = Color(255, 120, 40)
    if (model.isSelected()) {
      g.setColor(color)
      g.drawLine(viewRect.x + 1, viewRect.y - 2, viewRect.x + viewRect.width - 1, viewRect.y - 2)
      g.setColor(color.brighter())
      g.drawLine(viewRect.x + 0, viewRect.y - 1, viewRect.x + viewRect.width - 0, viewRect.y - 1)
      g.setColor(color)
      g.drawLine(viewRect.x + 0, viewRect.y - 0, viewRect.x + viewRect.width - 0, viewRect.y - 0)
    } else if (model.isRollover()) {
      g.setColor(color)
      g.drawLine(viewRect.x + 1, viewRect.y + 0, viewRect.x + viewRect.width - 1, viewRect.y + 0)
      g.setColor(color.brighter())
      g.drawLine(viewRect.x + 0, viewRect.y + 1, viewRect.x + viewRect.width - 0, viewRect.y + 1)
      g.setColor(color)
      g.drawLine(viewRect.x + 0, viewRect.y + 2, viewRect.x + viewRect.width - 0, viewRect.y + 2)
    }
    val v = c.getClientProperty(BasicHTML.propertyKey) as? View
    if (v != null) {
      v.paint(g, textRect)
    } else {
      if (model.isSelected()) {
        textRect.y -= 2
        textRect.x -= 1
      }
      textRect.x += 4
      paintText(g, b, textRect, text)
    }
  }

  companion object {
    // private static final TabViewButtonUI tabViewButtonUI = new BasicTabViewButtonUI();
    private val size = Dimension()
    private val viewRect = Rectangle()
    private val iconRect = Rectangle()
    private val textRect = Rectangle()

    // protected TabButton tabViewButton;

    // "Accidental override" reported when a @JvmStatic method in a Kotlin class has
    //   the same signature as a static method in a Java base class : KT-12993
    // https://youtrack.jetbrains.com/issue/KT-12993
    // @JvmStatic
    // fun createUI(c: JComponent) = BasicTabViewButtonUI()
  }
}
