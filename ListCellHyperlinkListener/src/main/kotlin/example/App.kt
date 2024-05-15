package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

fun makeUI(): Component {
  val m = DefaultListModel<SiteItem>()
  m.addElement(
    SiteItem("aterai", listOf("https://ateraimemo.com", "https://github.com/aterai")),
  )
  m.addElement(
    SiteItem("example", listOf("http://www.example.com", "https://www.example.com")),
  )
  val list = object : JList<SiteItem>(m) {
    override fun updateUI() {
      super.updateUI()
      fixedCellHeight = 120
      cellRenderer = SiteListItemRenderer()
    }
  }
  val ml = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val pt = e.point
      val index = list.locationToIndex(pt)
      if (index >= 0) {
        val item = list.model.getElementAt(index)
        val c = list.cellRenderer.getListCellRendererComponent(
          list,
          item,
          index,
          false,
          false,
        )
        if (c is JEditorPane) {
          val r = list.getCellBounds(index, index)
          c.setBounds(r)
          val me = SwingUtilities.convertMouseEvent(list, e, c)
          me.translatePoint(pt.x - r.x - me.x, pt.y - r.y - me.y)
          c.dispatchEvent(me)
        }
      }
    }
  }
  list.addMouseListener(ml)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private data class SiteItem(val name: String, val link: List<String>)

private class SiteListItemRenderer : ListCellRenderer<SiteItem> {
  private val renderer = object : JEditorPane("text/html", "") {
    private var listener: HyperlinkListener? = null

    override fun updateUI() {
      removeHyperlinkListener(listener)
      super.updateUI()
      this.contentType = "text/html"
      this.isEditable = false
      listener = HyperlinkListener { e ->
        if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
          val c = (e.source as? JComponent)?.rootPane
          JOptionPane.showMessageDialog(c, "You click the link with the URL " + e.url)
        }
      }
      addHyperlinkListener(listener)
    }
  }

  override fun getListCellRendererComponent(
    list: JList<out SiteItem>,
    item: SiteItem,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val buf = StringBuilder(100)
    buf.append("<html><h1>${item.name}</h1><table>")
    for (url in item.link) {
      buf.append("<tr><td><a href='$url'>$url</a></td></tr>")
    }
    buf.append("</table></html>")
    renderer.text = buf.toString()
    renderer.background = if (isSelected) Color.LIGHT_GRAY else Color.WHITE
    return renderer
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
