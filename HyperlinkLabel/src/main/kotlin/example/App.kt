package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.net.URI
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.HyperlinkEvent
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View
import kotlin.collections.LinkedHashMap

fun makeUI(): Component {
  val link = "https://ateraimemo.com/"
  val editor = object : JEditorPane("text/html", "<html><a href='$link'>$link</a>") {
    override fun updateUI() {
      super.updateUI()
      isOpaque = false // editor.setBackground(getBackground())
      isEditable = false // REQUIRED
      background = Color(0x0, true)
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    }
  }
  editor.addHyperlinkListener { e ->
    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
      UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
    }
  }
  val browseAction = object : AbstractAction(link) {
    override fun actionPerformed(e: ActionEvent) {
      runCatching {
        if (Desktop.isDesktopSupported()) {
          Desktop.getDesktop().browse(URI(link))
        }
        Toolkit.getDefaultToolkit().beep()
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
  }
  val map = LinkedHashMap<String, Component>(4)
  map["JLabel+MouseListener: "] = UrlLabel(link)
  map["JButton: "] = JButton(browseAction)
  map["JButton+ButtonUI: "] = HyperlinkButton(browseAction)
  map["JEditorPane+HyperlinkListener: "] = editor

  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  c.insets = Insets(5, 5, 5, 0)
  map.forEach { (k, v) ->
    c.gridx = 0
    c.anchor = GridBagConstraints.LINE_END
    p.add(JLabel(k), c)
    c.gridx = 1
    c.anchor = GridBagConstraints.LINE_START
    p.add(v, c)
  }
  val inside = BorderFactory.createEmptyBorder(2, 5 + 2, 2, 5 + 2)
  val outside = BorderFactory.createTitledBorder("HyperlinkLabel")
  p.border = BorderFactory.createCompoundBorder(outside, inside)
  p.preferredSize = Dimension(320, 240)
  return p
}

private class UrlLabel(h: String?) : JLabel("<html><a href='$h'>$h") {
  private var handler: MouseListener? = null
  override fun updateUI() {
    removeMouseListener(handler)
    super.updateUI()
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    handler = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        UIManager.getLookAndFeel().provideErrorFeedback(e.component)
      }
    }
    addMouseListener(handler)
  }
}

private class HyperlinkButton(text: String? = null, icon: Icon? = null) : JButton(text, icon) {
  constructor(a: Action?) : this() {
    super.setAction(a)
  }

  override fun updateUI() {
    super.updateUI()
    val tmp = if (UIManager.get(UI_CLASS_ID) != null) {
      UIManager.getUI(this) as? LinkViewButtonUI
    } else {
      BasicLinkViewButtonUI()
    }
    setUI(tmp)
    foreground = Color.BLUE
    border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }

  override fun getUI() = BasicLinkViewButtonUI()

  companion object {
    private const val UI_CLASS_ID = "LinkViewButtonUI"
  }
}

private open class LinkViewButtonUI : BasicButtonUI() { /* ButtonUI */ }

private class BasicLinkViewButtonUI : LinkViewButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun paint(g: Graphics, c: JComponent) {
    val b = c as? AbstractButton ?: return
    val f = b.font
    g.font = f
    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)
    val text = SwingUtilities.layoutCompoundLabel(
      b, b.getFontMetrics(f), b.text, null,
      b.verticalAlignment, b.horizontalAlignment,
      b.verticalTextPosition, b.horizontalTextPosition,
      viewRect, iconRect, textRect,
      0
    )
    if (b.isOpaque) {
      g.color = b.background
      g.fillRect(0, 0, b.width, b.height)
    }
    val m = b.model
    if (!m.isSelected && !m.isPressed && m.isRollover) {
      g.color = Color.BLUE
      g.drawLine(
        viewRect.x,
        viewRect.y + viewRect.height,
        viewRect.x + viewRect.width,
        viewRect.y + viewRect.height
      )
    }
    (b.getClientProperty(BasicHTML.propertyKey) as? View)?.paint(g, textRect)
      ?: paintText(g, b, textRect, text)
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
