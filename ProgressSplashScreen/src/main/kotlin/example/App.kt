package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  object : SwingWorker<Void, Void>() {
    @Throws(InterruptedException::class)
    override fun doInBackground(): Void? {
      Thread.sleep(3_000)
      return null
    }
  }.execute()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMissingImage(): Image {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.paint = Color.LIGHT_GRAY
    g2.fillRect(x, y, w, h)
    g2.paint = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 240

  override fun getIconHeight() = 160
}

private open class BackgroundTask : SwingWorker<Void, Void>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): Void? {
    var current = 0
    val lengthOfTask = 120
    while (current < lengthOfTask && !isCancelled) {
      doSomething(100 * current++ / lengthOfTask)
    }
    return null
  }

  @Throws(InterruptedException::class)
  protected fun doSomething(progress: Int) {
    Thread.sleep(50)
    setProgress(progress)
  }
}

fun main() {
  runCatching {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  }.onFailure {
    it.printStackTrace()
    Toolkit.getDefaultToolkit().beep()
  }

  val frame = JFrame()
  val splashScreen = JDialog(frame, Dialog.ModalityType.DOCUMENT_MODAL)
  val progress = JProgressBar()
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/splash.png")
  EventQueue.invokeLater {
    val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
    splashScreen.isUndecorated = true
    splashScreen.contentPane.add(JLabel(ImageIcon(img)))
    splashScreen.contentPane.add(progress, BorderLayout.SOUTH)
    splashScreen.pack()
    splashScreen.setLocationRelativeTo(null)
    splashScreen.isVisible = true
  }
  val worker = object : BackgroundTask() {
    override fun done() {
      splashScreen.dispose()
    }
  }
  worker.addPropertyChangeListener { e ->
    if ("progress" == e.propertyName) {
      progress.value = e.newValue as? Int ?: 0
    }
  }
  worker.execute()

  frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  frame.contentPane.add(makeUI())
  frame.pack()
  frame.setLocationRelativeTo(null)
  EventQueue.invokeLater { frame.isVisible = true }
}
