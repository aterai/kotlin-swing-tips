package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.HyperlinkEvent
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.StyleConstants
import javax.swing.text.View
import javax.swing.text.html.HTMLDocument

class MainPanel : JPanel(BorderLayout()) {
  private val link = "https://ateraimemo.com/"
  private val htmlText = """
      <html><body>
        html tag: <br /><a href='$link'>$link</a>
      </body></html>
    """
  private var tooltip: String? = null

  init {
    add(JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
      it.setResizeWeight(.5)
      it.setTopComponent(JScrollPane(makeEditorPane(false)))
      it.setBottomComponent(JScrollPane(makeEditorPane(true)))
    })
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeEditorPane(editable: Boolean) = JEditorPane().also {
    it.setEditable(editable)
    it.setContentType("text/html")
    it.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    it.setText(htmlText)
    it.addHyperlinkListener { e ->
      when (e.getEventType()) {
        HyperlinkEvent.EventType.ACTIVATED ->
          JOptionPane.showMessageDialog(it, "You click the link with the URL " + e.getURL())
        HyperlinkEvent.EventType.ENTERED -> {
          tooltip = it.getToolTipText()
          it.setToolTipText(e.getURL()?.toExternalForm())
        }
        HyperlinkEvent.EventType.EXITED -> it.setToolTipText(tooltip)
      }
    }

    val doc = it.getDocument()
    if (doc is HTMLDocument) {
      val s = doc.addStyle("button", null)
      StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER)
      val button = HyperlinkButton(link)
      button.addActionListener { e ->
        val b = (e.getSource() as? AbstractButton)?.isSelected() ?: false
        it.setBackground(if (b) Color.RED else Color.WHITE)
        JOptionPane.showMessageDialog(it, "You click the link with the URL $link")
      }
      button.setToolTipText("button: $link")
      button.setOpaque(false)
      StyleConstants.setComponent(s, button)
      runCatching {
        doc.insertString(doc.getLength(), "\n----\nJButton:\n", null)
        doc.insertString(doc.getLength(), link + "\n", doc.getStyle("button"))
      }
    }
  }
}

class HyperlinkButton : JButton {
  override fun updateUI() {
    super.updateUI()
    setUI(BasicLinkViewButtonUI())
    setForeground(Color.BLUE)
    setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0))
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  }

  constructor(text: String?, icon: Icon?) : super(text, icon)

  constructor(text: String?) : this(text, null)

  constructor(icon: Icon) : this(null, icon)

  constructor(a: Action) : this(null, null) {
    super.setAction(a)
  }
}

open class LinkViewButtonUI : BasicButtonUI() /* ButtonUI */

class BasicLinkViewButtonUI : LinkViewButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun paint(g: Graphics, c: JComponent) {
    val b = c as? AbstractButton ?: return
    val f = c.getFont()
    g.setFont(f)

    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)

    val text = SwingUtilities.layoutCompoundLabel(
      c, c.getFontMetrics(f), b.getText(), null, // altIcon != null ? altIcon : getDefaultIcon(),
      b.getVerticalAlignment(), b.getHorizontalAlignment(),
      b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
      viewRect, iconRect, textRect, 0) // b.getText() == null ? 0 : b.getIconTextGap())

    if (c.isOpaque()) {
      g.setColor(b.getBackground())
      g.fillRect(0, 0, c.getWidth(), c.getHeight())
    }

    if (b.isRolloverEnabled() && b.getModel().isRollover()) {
      g.setColor(Color.BLUE)
      g.drawLine(
        viewRect.x, viewRect.y + viewRect.height,
        viewRect.x + viewRect.width, viewRect.y + viewRect.height)
    }

    (c.getClientProperty(BasicHTML.propertyKey) as? View)?.also {
      it.paint(g, textRect)
    } ?: paintText(g, b, textRect, text)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
