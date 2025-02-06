package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.invoke.MethodHandles
import java.nio.charset.StandardCharsets
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler
import javax.swing.*
import javax.swing.event.InternalFrameEvent
import javax.swing.event.InternalFrameListener

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)
private var openFrameCount = 0
private var row = 0
private var col = 0

fun makeUI(): Component {
  val textArea = JTextArea(2, 0)
  textArea.isEditable = false
  logger.useParentHandlers = false
  logger.addHandler(TextAreaHandler(TextAreaOutputStream(textArea)))

  val scroll = JScrollPane(textArea)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val desktop = JDesktopPane()
  val im = desktop.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape")

  val action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      desktop.selectedFrame?.also {
        desktop.desktopManager.closeFrame(it)
      }
    }
  }
  val am = desktop.actionMap
  am.put("escape", action)

  return JPanel(BorderLayout()).also {
    it.add(createToolBar(desktop), BorderLayout.NORTH)
    it.add(desktop)
    it.add(scroll, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBar(desktop: JDesktopPane): JToolBar {
  val toolBar = JToolBar()
  toolBar.isFloatable = false
  var b = JButton(UIManager.getIcon("FileView.fileIcon"))
  b.addActionListener {
    val frame = makeInternalFrame(desktop)
    desktop.add(frame)
    runCatching {
      frame.isSelected = true
      if (openFrameCount % 2 == 0) {
        frame.isIcon = true
      }
    }
  }
  b.toolTipText = "create new InternalFrame"
  toolBar.add(b)
  toolBar.add(Box.createGlue())
  b = JButton(CloseIcon(Color.RED))
  b.addActionListener {
    desktop.selectedFrame?.dispose()
  }
  b.toolTipText = "f.dispose();"
  toolBar.add(b)
  b = JButton(CloseIcon(Color.GREEN))
  b.addActionListener {
    desktop.selectedFrame?.also {
      desktop.desktopManager.closeFrame(it)
    }
  }
  b.toolTipText = "desktop.getDesktopManager().closeFrame(f);"
  toolBar.add(b)
  b = JButton(CloseIcon(Color.BLUE))
  b.addActionListener {
    desktop.selectedFrame?.doDefaultCloseAction()
  }
  b.toolTipText = "f.doDefaultCloseAction();"
  toolBar.add(b)
  b = JButton(CloseIcon(Color.YELLOW))
  b.addActionListener {
    desktop.selectedFrame?.also {
      runCatching {
        it.isClosed = true
      }
    }
  }
  b.toolTipText = "f.setClosed(true);"
  toolBar.add(b)
  return toolBar
}

private fun makeInternalFrame(desktop: JDesktopPane): JInternalFrame {
  val f = JInternalFrame(
    "Document #${++openFrameCount}",
    true,
    true,
    true,
    true,
  )
  row += 1
  f.setSize(240, 120)
  f.setLocation(20 * row + 20 * col, 20 * row)
  f.isVisible = true
  EventQueue.invokeLater {
    val rect = desktop.bounds
    rect.setLocation(0, 0)
    if (!rect.contains(f.bounds)) {
      row = 0
      col += 1
    }
  }
  f.addInternalFrameListener(object : InternalFrameListener {
    override fun internalFrameClosing(e: InternalFrameEvent) {
      logger.info { "internalFrameClosing: " + e.internalFrame.title }
    }

    override fun internalFrameClosed(e: InternalFrameEvent) {
      logger.info { "internalFrameClosed: " + e.internalFrame.title }
    }

    override fun internalFrameOpened(e: InternalFrameEvent) {
      logger.info { "internalFrameOpened: " + e.internalFrame.title }
    }

    override fun internalFrameIconified(e: InternalFrameEvent) {
      logger.info { "internalFrameIconified: " + e.internalFrame.title }
    }

    override fun internalFrameDeiconified(e: InternalFrameEvent) {
      logger.info { "internalFrameDeiconified: " + e.internalFrame.title }
    }

    override fun internalFrameActivated(e: InternalFrameEvent) {
      logger.info { "internalFrameActivated: " + e.getInternalFrame().getTitle() }
    }

    override fun internalFrameDeactivated(e: InternalFrameEvent) {
      logger.info { "internalFrameDeactivated: " + e.internalFrame.title }
    }
  })
  return f
}

private class CloseIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.drawLine(4, 4, 11, 11)
    g2.drawLine(4, 5, 10, 11)
    g2.drawLine(5, 4, 11, 10)
    g2.drawLine(11, 4, 4, 11)
    g2.drawLine(11, 5, 5, 11)
    g2.drawLine(10, 4, 4, 10)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private class TextAreaOutputStream(
  private val textArea: JTextArea,
) : OutputStream() {
  private val buffer = ByteArrayOutputStream()

  @Throws(IOException::class)
  override fun flush() {
    textArea.append(buffer.toString("UTF-8"))
    buffer.reset()
  }

  override fun write(b: Int) {
    buffer.write(b)
  }
}

private class TextAreaHandler(
  os: OutputStream,
) : StreamHandler(os, SimpleFormatter()) {
  override fun getEncoding() = StandardCharsets.UTF_8.name()

  @Synchronized
  override fun publish(logRecord: LogRecord) {
    super.publish(logRecord)
    flush()
  }

  @Synchronized
  override fun close() {
    flush()
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
