package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val icon = UIManager.getIcon("OptionPane.informationIcon")
  val label = object : JLabel("OptionPane.informationIcon", icon, SwingConstants.LEADING) {
    private val viewRect = Rectangle()
    private val iconRect = Rectangle()
    private val textRect = Rectangle()

    override fun getToolTipText(e: MouseEvent): String? {
      SwingUtilities.calculateInnerArea(this, viewRect)
      SwingUtilities.layoutCompoundLabel(
        this,
        this.getFontMetrics(this.font),
        this.text,
        this.icon,
        this.verticalAlignment,
        this.horizontalAlignment,
        this.verticalTextPosition,
        this.horizontalTextPosition,
        viewRect,
        iconRect,
        textRect,
        this.iconTextGap
      )
      val tip = super.getToolTipText(e)
      return when {
        tip == null -> null
        iconRect.contains(e.point) -> "Icon: $tip"
        textRect.contains(e.point) -> "Text: $tip"
        else -> "Border: $tip"
      }
    }
  }
  label.isOpaque = true
  label.background = Color.GREEN
  label.border = BorderFactory.createMatteBorder(20, 10, 50, 30, Color.RED)
  label.toolTipText = "ToolTipText ToolTipText"

  val item = IconTooltipItem("Information", icon)
  item.toolTipText = "Information item"
  val menu = JMenu("Menu")
  menu.add(item)
  val menuBar = JMenuBar()
  menuBar.add(menu)

  return JPanel().also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

private class IconTooltipItem(text: String?, icon: Icon?) : JMenuItem(text, icon) {
  override fun getToolTipText(e: MouseEvent): String? {
    SwingUtilities.calculateInnerArea(this, VIEW_RECT)
    SwingUtilities.layoutCompoundLabel(
      this,
      getFontMetrics(font),
      text,
      this.icon,
      verticalAlignment,
      horizontalAlignment,
      verticalTextPosition,
      horizontalTextPosition,
      VIEW_RECT,
      ICON_RECT,
      TEXT_RECT,
      iconTextGap
    )
    val tip = super.getToolTipText(e)
    return if (tip == null) {
      null
    } else {
      (if (ICON_RECT.contains(e.point)) "Icon: " else "Text: ") + tip
    }
  }

  companion object {
    private val VIEW_RECT = Rectangle()
    private val ICON_RECT = Rectangle()
    private val TEXT_RECT = Rectangle()
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
