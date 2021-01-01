package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ButtonUI
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View

fun makeUI(): Component {
  val label = JButton("https://ateraimemo.com/")
  label.ui = LinkViewButtonUI.createUI(label, label.text)

  val p = JPanel(FlowLayout(FlowLayout.LEADING, 5, 5))
  p.border = BorderFactory.createTitledBorder("Draggable Hyperlink")
  p.add(JLabel("D&D->Browser:"))
  p.add(label)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea("JTextArea")))
    it.preferredSize = Dimension(320, 240)
  }
}

private class LinkViewButtonUI : BasicButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun paint(g: Graphics, c: JComponent) {
    if (c !is AbstractButton) {
      return
    }
    g.font = c.font
    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)
    val text = SwingUtilities.layoutCompoundLabel(
      c, c.getFontMetrics(c.font), c.text, null,
      c.verticalAlignment, c.horizontalAlignment,
      c.verticalTextPosition, c.horizontalTextPosition,
      viewRect, iconRect, textRect,
      0
    )
    if (c.isOpaque()) {
      g.color = c.background
      g.fillRect(0, 0, c.getWidth(), c.getHeight())
    }
    val model = c.model
    if (c.isRolloverEnabled && model.isRollover) {
      g.color = Color.BLUE
      g.drawLine(viewRect.x, viewRect.y + viewRect.height, viewRect.x + viewRect.width, viewRect.y + viewRect.height)
    }
    (c.getClientProperty(BasicHTML.propertyKey) as? View)?.paint(g, textRect) ?: paintText(g, c, textRect, text)
  }

  companion object {
    private val URI_FLAVOR = DataFlavor.stringFlavor

    fun createUI(b: JButton, href: String): ButtonUI {
      b.foreground = Color.BLUE
      b.border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
      b.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      b.transferHandler = object : TransferHandler("text") {
        override fun canImport(c: JComponent, flavors: Array<DataFlavor>) =
          flavors.isNotEmpty() && flavors[0].equals(URI_FLAVOR)

        override fun createTransferable(c: JComponent) = object : Transferable {
          override fun getTransferData(flavor: DataFlavor) = href

          override fun getTransferDataFlavors() = arrayOf(URI_FLAVOR)

          override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.equals(URI_FLAVOR)
        }
      }
      val ml = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          (e.component as? JButton)?.also {
            it.transferHandler.exportAsDrag(it, e, TransferHandler.COPY)
          }
        }
      }
      b.addMouseListener(ml)
      return LinkViewButtonUI()
    }
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
