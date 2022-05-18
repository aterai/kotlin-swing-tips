package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val i1 = makeImage(cl.getResource("example/test.png"))
  val i2 = makeImage(cl.getResource("example/test.jpg"))
  val randomDissolve = RandomDissolve(i1, i2)

  val button = JButton("change")
  button.addActionListener { randomDissolve.animationStart() }

  return JPanel(BorderLayout()).also {
    it.add(randomDissolve)
    it.add(button, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeImage(url: URL?): BufferedImage {
  val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
    ?: UIManager.getIcon("html.missingImage")
  val w = icon.iconWidth
  val h = icon.iconHeight
  val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
  val g2 = img.createGraphics()
  icon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return img
}

private class RandomDissolve(
  private val image1: BufferedImage,
  private val image2: BufferedImage
) : JComponent() {
  private val animator: Timer
  private var buf: BufferedImage
  private var mode = true
  private var currentStage = 0
  private var src: IntArray? = null
  private var dst: IntArray? = null
  private var step: IntArray? = null

  init {
    buf = copyImage(if (mode) image2 else image1)
    animator = Timer(10) { e ->
      if (nextStage()) {
        repaint()
      } else {
        (e.source as? Timer)?.stop()
      }
    }
  }

  private fun nextStage(): Boolean {
    val s = src
    val d = dst
    return if (currentStage > 0 && s != null && d != null) {
      currentStage -= 1
      step?.also {
        for (i in it.indices) {
          if (it[i] == currentStage) {
            s[i] = d[i]
          }
        }
      }
      true
    } else {
      false
    }
  }

  fun animationStart() {
    currentStage = STAGES
    buf = copyImage(if (mode) image2 else image1)
    val src1 = getData(buf)
    val dst1 = getData(copyImage(if (mode) image1 else image2))
    val step1 = IntArray(src1.size)
    mode = mode xor true
    for (i in step1.indices) {
      step1[i] = (0 until currentStage).random()
    }
    src = src1
    dst = dst1
    step = step1
    animator.start()
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = background
    g2.fillRect(0, 0, width, height)
    g2.drawImage(buf, 0, 0, buf.width, buf.height, this)
    g2.dispose()
  }

  private fun copyImage(image: BufferedImage): BufferedImage {
    val w = image.width
    val h = image.height
    val result = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g2 = result.createGraphics()
    g2.drawRenderedImage(image, null)
    g2.dispose()
    return result
  }

  private fun getData(image: BufferedImage) =
    (image.raster.dataBuffer as? DataBufferInt)?.data ?: IntArray(0)

  companion object {
    private const val STAGES = 16
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
