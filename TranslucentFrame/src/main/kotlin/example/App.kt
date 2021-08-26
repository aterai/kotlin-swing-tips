package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.synth.ColorType
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthGraphicsUtils
import javax.swing.plaf.synth.SynthLookAndFeel
import javax.swing.plaf.synth.SynthPainter
import javax.swing.plaf.synth.SynthStyle
import javax.swing.plaf.synth.SynthStyleFactory

fun makeUI(): Component {
  val p1 = JPanel()
  p1.isOpaque = false
  val p2 = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      // super.paintComponent(g);
      g.color = Color(100, 50, 50, 100)
      g.fillRect(0, 0, width, height)
    }
  }
  p2.isOpaque = false

  val desktop = JDesktopPane()
  desktop.add(createFrame(initContainer(p1), 0))
  desktop.add(createFrame(initContainer(p2), 1))

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(panel: Container, idx: Int): JInternalFrame {
  val frame = JInternalFrame("title", true, true, true, true)
  // frame.putClientProperty("Nimbus.Overrides", d)
  // // frame.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
  frame.contentPane = panel
  frame.rootPane.isOpaque = false
  frame.isOpaque = false
  frame.setSize(160, 100)
  frame.setLocation(10 + 60 * idx, 10 + 40 * idx)
  EventQueue.invokeLater { frame.isVisible = true }
  return frame
}

private fun initContainer(p: Container): Container {
  p.add(JLabel("label"))
  p.add(JButton("button"))
  return p
}

private class MySynthStyleFactory(private val wrappedFactory: SynthStyleFactory) : SynthStyleFactory() {
  override fun getStyle(c: JComponent, id: Region): SynthStyle {
    var s = wrappedFactory.getStyle(c, id)
    // if (id == Region.INTERNAL_FRAME_TITLE_PANE || id == Region.INTERNAL_FRAME) {
    if (id === Region.INTERNAL_FRAME) {
      s = TranslucentSynthStyle(s)
    }
    return s
  }
}

@Suppress("TooManyFunctions")
private class TranslucentSynthStyle(private val style: SynthStyle) : SynthStyle() {
  override operator fun get(context: SynthContext?, key: Any): Any? = style.get(context, key)

  override fun getBoolean(context: SynthContext?, key: Any, defaultValue: Boolean) =
    style.getBoolean(context, key, defaultValue)

  override fun getColor(context: SynthContext?, type: ColorType): Color? = style.getColor(context, type)

  override fun getFont(context: SynthContext?): Font? = style.getFont(context)

  override fun getGraphicsUtils(context: SynthContext?): SynthGraphicsUtils? = style.getGraphicsUtils(context)

  override fun getIcon(context: SynthContext?, key: Any): Icon? = style.getIcon(context, key)

  override fun getInsets(context: SynthContext?, insets: Insets?): Insets? = style.getInsets(context, insets)

  override fun getInt(context: SynthContext?, key: Any, defaultValue: Int) =
    style.getInt(context, key, defaultValue)

  override fun getPainter(context: SynthContext?) =
    object : SynthPainter() {
      override fun paintInternalFrameBackground(
        context: SynthContext?,
        g: Graphics,
        x: Int,
        y: Int,
        w: Int,
        h: Int
      ) {
        g.color = Color(100, 200, 100, 100)
        g.fillRoundRect(x, y, w - 1, h - 1, 15, 15)
      }
    }

  override fun getString(context: SynthContext, key: Any, defaultValue: String): String? =
    style.getString(context, key, defaultValue)

  override fun installDefaults(context: SynthContext) = style.installDefaults(context)

  override fun uninstallDefaults(context: SynthContext) {
    style.uninstallDefaults(context)
  }

  override fun isOpaque(context: SynthContext) =
    context.region !== Region.INTERNAL_FRAME && style.isOpaque(context)

  override fun getColorForState(context: SynthContext, type: ColorType) = null // Color.RED

  override fun getFontForState(context: SynthContext) = null // Font(Font.MONOSPACED, Font.ITALIC, 24)
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
      SynthLookAndFeel.setStyleFactory(MySynthStyleFactory(SynthLookAndFeel.getStyleFactory()))
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
