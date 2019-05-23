// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

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

class MainPanel : JPanel(BorderLayout()) {
  private val desktop = JDesktopPane()

  init {
    val p1 = JPanel()
    p1.setOpaque(false)
    val p2 = object : JPanel() {
      protected override fun paintComponent(g: Graphics) {
        // super.paintComponent(g);
        g.setColor(Color(100, 50, 50, 100))
        g.fillRect(0, 0, getWidth(), getHeight())
      }
    }
    p2.setOpaque(false)

//    d.put("InternalFrame[Enabled].backgroundPainter", object : Painter<JComponent> {
//      override fun paint(g: Graphics2D, c: JComponent, w: Int, h: Int) {
//        g.setColor(Color(100, 200, 100, 100))
//        g.fillRoundRect(0, 0, w - 1, h - 1, 15, 15)
//      }
//    })
//    d.put("InternalFrame[Enabled+WindowFocused].backgroundPainter", object : Painter {
//      fun paint(g: Graphics2D, c: JComponent, w: Int, h: Int) {
//        g.setColor(Color(100, 250, 120, 100))
//        g.fillRoundRect(0, 0, w - 1, h - 1, 15, 15)
//      }
//    })

    createFrame(initContainer(p1), 0)
    createFrame(initContainer(p2), 1)
    add(desktop)
    setPreferredSize(Dimension(320, 240))
  }

  protected fun createFrame(panel: Container, idx: Int): JInternalFrame {
    val frame = MyInternalFrame()
    // frame.putClientProperty("Nimbus.Overrides", d)
    // // frame.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
    frame.setContentPane(panel)
    frame.getRootPane().setOpaque(false)
    frame.setOpaque(false)
    frame.setVisible(true)
    frame.setLocation(10 + 60 * idx, 10 + 40 * idx)
    desktop.add(frame)
    desktop.getDesktopManager().activateFrame(frame)
    return frame
  }

  private fun initContainer(p: Container): Container {
    p.add(JLabel("label"))
    p.add(JButton("button"))
    return p
  }
}

internal class MyInternalFrame : JInternalFrame("title", true, true, true, true) {
  init {
    setSize(160, 100)
  }
}

internal class MySynthStyleFactory(private val wrappedFactory: SynthStyleFactory) : SynthStyleFactory() {
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
internal class TranslucentSynthStyle(private val style: SynthStyle) : SynthStyle() {
  override operator fun get(context: SynthContext?, key: Any): Any? {
    return style.get(context, key)
  }

  override fun getBoolean(context: SynthContext, key: Any, defaultValue: Boolean): Boolean {
    return style.getBoolean(context, key, defaultValue)
  }

  override fun getColor(context: SynthContext, type: ColorType): Color {
    return style.getColor(context, type)
  }

  override fun getFont(context: SynthContext): Font {
    return style.getFont(context)
  }

  override fun getGraphicsUtils(context: SynthContext?): SynthGraphicsUtils {
    return style.getGraphicsUtils(context)
  }

  override fun getIcon(context: SynthContext, key: Any): Icon? {
    return style.getIcon(context, key)
  }

  override fun getInsets(context: SynthContext?, insets: Insets?): Insets {
    return style.getInsets(context, insets)
  }

  override fun getInt(context: SynthContext, key: Any, defaultValue: Int): Int {
    return style.getInt(context, key, defaultValue)
  }

  override fun getPainter(context: SynthContext?): SynthPainter {
    return object : SynthPainter() {
      override fun paintInternalFrameBackground(
        context: SynthContext?,
        g: Graphics,
        x: Int,
        y: Int,
        w: Int,
        h: Int
      ) {
        g.setColor(Color(100, 200, 100, 100))
        g.fillRoundRect(x, y, w - 1, h - 1, 15, 15)
      }
    }
  }

  override fun getString(context: SynthContext, key: Any, defaultValue: String): String {
    return style.getString(context, key, defaultValue)
  }

  override fun installDefaults(context: SynthContext) {
    style.installDefaults(context)
  }

  override fun uninstallDefaults(context: SynthContext) {
    style.uninstallDefaults(context)
  }

  override fun isOpaque(context: SynthContext): Boolean {
    return context.getRegion() !== Region.INTERNAL_FRAME && style.isOpaque(context)
    // if (context.getRegion() == Region.INTERNAL_FRAME) {
    //   return false
    // } else {
    //   return style.isOpaque(context)
    // }
  }

  override fun getColorForState(context: SynthContext, type: ColorType): Color? {
    return null // Color.RED;
  }

  override fun getFontForState(context: SynthContext): Font? {
    return null // new Font(Font.MONOSPACED, Font.ITALIC, 24);
  }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
