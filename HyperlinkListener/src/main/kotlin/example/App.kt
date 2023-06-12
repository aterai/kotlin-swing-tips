package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.HyperlinkEvent
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.StyleConstants
import javax.swing.text.View
import javax.swing.text.html.HTMLDocument

private const val LINK = "https://ateraimemo.com/"
private const val HTML = """
  <html>
    <body>
      html tag: <br /><a href='$LINK'>$LINK</a>
    </body>
  </html>
"""
private var tooltip: String? = null

fun makeUI() = JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
  it.resizeWeight = .5
  it.topComponent = JScrollPane(makeEditorPane(false))
  it.bottomComponent = JScrollPane(makeEditorPane(true))
  it.preferredSize = Dimension(320, 240)
}

private fun makeEditorPane(editable: Boolean) = JEditorPane().also {
  it.isEditable = editable
  it.contentType = "text/html"
  it.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  it.text = HTML
  it.addHyperlinkListener { e ->
    when (e.eventType) {
      HyperlinkEvent.EventType.ACTIVATED -> {
        val msg = "Clicked on the link " + e.url
        JOptionPane.showMessageDialog(it, msg)
      }
      HyperlinkEvent.EventType.ENTERED -> {
        tooltip = it.toolTipText
        it.toolTipText = e.url?.toExternalForm()
      }
      HyperlinkEvent.EventType.EXITED -> {
        it.toolTipText = tooltip
        tooltip = null
      }
    }
  }

  val doc = it.document
  if (doc is HTMLDocument) {
    val s = doc.addStyle("button", null)
    StyleConstants.setAlignment(s, StyleConstants.ALIGN_CENTER)
    val button = HyperlinkButton(LINK)
    button.addActionListener { _ ->
      JOptionPane.showMessageDialog(it, "Clicked on the link $LINK")
    }
    button.toolTipText = "button: $LINK"
    button.isOpaque = false
    StyleConstants.setComponent(s, button)
    runCatching {
      val txt = """
        
        ----
        JButton:

      """.trimIndent()
      doc.insertString(doc.getLength(), txt, null)
      doc.insertString(doc.getLength(), LINK + "\n", doc.getStyle("button"))
    }
  }
}

private class HyperlinkButton : JButton {
  constructor(text: String?, icon: Icon?) : super(text, icon)

  constructor(text: String?) : this(text, null)

  // constructor(icon: Icon) : this(null, icon)

  // constructor(a: Action) : this(null, null) {
  //   super.setAction(a)
  // }

  override fun updateUI() {
    super.updateUI()
    setUI(BasicLinkViewButtonUI())
    foreground = Color.BLUE
    border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
}

private open class LinkViewButtonUI : BasicButtonUI()

private class BasicLinkViewButtonUI : LinkViewButtonUI() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun paint(g: Graphics, c: JComponent) {
    val b = c as? AbstractButton ?: return
    val f = c.font
    g.font = f

    SwingUtilities.calculateInnerArea(c, viewRect)
    iconRect.setBounds(0, 0, 0, 0)
    textRect.setBounds(0, 0, 0, 0)

    val text = SwingUtilities.layoutCompoundLabel(
      c, c.getFontMetrics(f), b.text, null, // altIcon != null ? altIcon : getDefaultIcon(),
      b.verticalAlignment, b.horizontalAlignment,
      b.verticalTextPosition, b.horizontalTextPosition,
      viewRect, iconRect, textRect, 0 // b.getText() == null ? 0 : b.getIconTextGap()
    )

    if (c.isOpaque()) {
      g.color = b.background
      g.fillRect(0, 0, c.width, c.height)
    }

    if (b.isRolloverEnabled && b.model.isRollover) {
      g.color = Color.BLUE
      g.drawLine(
        viewRect.x,
        viewRect.y + viewRect.height,
        viewRect.x + viewRect.width,
        viewRect.y + viewRect.height
      )
    }

    (c.getClientProperty(BasicHTML.propertyKey) as? View)?.paint(g, textRect)
      ?: paintText(g, c, textRect, text)
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
