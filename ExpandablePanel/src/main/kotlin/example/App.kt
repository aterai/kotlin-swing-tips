package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.EventListener
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports

private fun makeList() = listOf(
  object : AbstractExpansionPanel("Panel1") {
    override fun makePanel(): Container {
      val p = Box.createVerticalBox()
      p.border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
      p.add(JCheckBox("11111"))
      p.add(JCheckBox("2222222222"))
      return p
    }
  },
  object : AbstractExpansionPanel("Panel2") {
    override fun makePanel(): Container {
      val p = Box.createVerticalBox()
      p.border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
      for (i in 0..15) {
        p.add(makeLabel(i))
      }
      return p
    }

    private fun makeLabel(i: Int) = JLabel("%02d".format(i))
  },
  object : AbstractExpansionPanel("Panel3") {
    override fun makePanel(): Container {
      val p = Box.createVerticalBox()
      p.border = BorderFactory.createEmptyBorder(5, 15, 5, 15)
      val bg = ButtonGroup()
      listOf(
        JRadioButton("aa"),
        JRadioButton("bb"),
        JRadioButton("cc", true)
      ).forEach {
        p.add(it)
        bg.add(it)
      }
      return p
    }
  }
)

fun makeUI(): Component {
  val northBox = Box.createVerticalBox()
  val centerBox = Box.createVerticalBox()
  val southBox = Box.createVerticalBox()
  val panelList = makeList()
  val rl = ExpansionListener { e ->
    (e.source as? Component)?.also { s ->
      s.isVisible = false
      centerBox.removeAll()
      northBox.removeAll()
      southBox.removeAll()
      var insertSouth = false
      for (exp in panelList) {
        if (s == exp && exp.isExpanded) {
          centerBox.add(exp)
          insertSouth = true
          continue
        }
        exp.isExpanded = false
        if (insertSouth) {
          southBox.add(exp)
        } else {
          northBox.add(exp)
        }
      }
      s.isVisible = true
    }
  }
  panelList.forEach {
    northBox.add(it)
    it.addExpansionListener(rl)
  }

  val panel = object : JPanel(BorderLayout()) {
    override fun getMinimumSize() = super.getMinimumSize()?.also {
      it.width = 120
    }
  }
  panel.add(northBox, BorderLayout.NORTH)
  panel.add(centerBox)
  panel.add(southBox, BorderLayout.SOUTH)

  return JSplitPane().also {
    it.leftComponent = panel
    it.rightComponent = JScrollPane(JTree())
    it.preferredSize = Dimension(320, 240)
  }
}

private abstract class AbstractExpansionPanel(title: String?) : JPanel(BorderLayout()) {
  private var expansionEvent: ExpansionEvent? = null
  private val scroll = JScrollPane()
  private val button = JButton(title)
  private var openFlag = false
  abstract fun makePanel(): Container?
  var isExpanded: Boolean
    get() = openFlag
    set(flg) {
      openFlag = flg
      if (openFlag) {
        add(scroll)
      } else {
        remove(scroll)
      }
    }

  fun addExpansionListener(l: ExpansionListener) {
    listenerList.add(ExpansionListener::class.java, l)
  }

  // Notify all listeners that have registered interest for
  // notification on this event type.The event instance
  // is lazily created using the parameters passed into
  // the fire method.
  protected fun fireExpansionEvent() {
    // Guaranteed to return a non-null array
    val listeners = listenerList.listenerList
    // Process the listeners last to first, notifying
    // those that are interested in this event
    var i = listeners.size - 2
    while (i >= 0) {
      if (listeners[i] === ExpansionListener::class.java) {
        // Lazily create the event:
        val ee = expansionEvent ?: ExpansionEvent(this)
        (listeners[i + 1] as? ExpansionListener)?.expansionStateChanged(ee)
      }
      i -= 2
    }
  }

  init {
    scroll.also {
      it.setViewportView(makePanel())
      it.verticalScrollBar.unitIncrement = 25
    }
    button.also {
      it.addActionListener {
        isExpanded = !isExpanded
        fireExpansionEvent()
      }
      add(it, BorderLayout.NORTH)
    }
  }
}

private class ExpansionEvent(source: Any?) : EventObject(source) {
  companion object {
    private const val serialVersionUID = 1L
  }
}

private fun interface ExpansionListener : EventListener {
  fun expansionStateChanged(e: ExpansionEvent)
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
