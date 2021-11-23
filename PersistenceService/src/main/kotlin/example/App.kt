package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Serializable
import java.net.URL
import javax.jnlp.BasicService
import javax.jnlp.PersistenceService
import javax.jnlp.ServiceManager
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(JTree()))
  it.preferredSize = Dimension(320, 240)
}

private open class LoadSaveTask(val windowState: WindowState) :
  SwingWorker<WindowListener?, Void?>() {
  public override fun doInBackground(): WindowListener? {
    val service: PersistenceService? = runCatching {
      ServiceManager.lookup("javax.jnlp.PersistenceService") as? PersistenceService
    }.getOrNull()
    val bs: BasicService? = runCatching {
      ServiceManager.lookup("javax.jnlp.BasicService") as? BasicService
    }.getOrNull()
    return if (service != null && bs != null) {
      val codebase = bs.codeBase
      loadWindowState(service, codebase, windowState)
      object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
          val frame = e.component
          if (frame is Frame && frame.extendedState == Frame.NORMAL) {
            windowState.size = frame.size
            windowState.location = frame.locationOnScreen
          }
          saveWindowState(service, codebase, windowState)
        }
      }
    } else {
      null
    }
  }

  companion object {
    private fun loadWindowState(ps: PersistenceService, codebase: URL, windowState: WindowState) {
      runCatching {
        val fc = ps[codebase]
        XMLDecoder(BufferedInputStream(fc.inputStream)).use { d ->
          (d.readObject() as? Map<*, *>)?.also {
            windowState.size = it["size"] as? Dimension ?: Dimension()
            windowState.location = it["location"] as? Point ?: Point()
          }
        }
      }.onFailure {
        val size = runCatching { ps.create(codebase, 64_000) }.getOrElse { 0 }
        println("Cache created - size: $size")
      }
    }

    protected fun saveWindowState(ps: PersistenceService, codebase: URL, windowState: WindowState) {
      runCatching {
        val fc = ps[codebase]
        XMLEncoder(BufferedOutputStream(fc.getOutputStream(true))).use { e ->
          val map = HashMap<String, Serializable>()
          map["size"] = windowState.size as Serializable
          map["location"] = windowState.location as Serializable
          e.writeObject(map)
          e.flush()
        }
      }
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    val windowState = WindowState()
    val worker = object : LoadSaveTask(windowState) {
      override fun done() {
        runCatching {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }.onFailure {
          it.printStackTrace()
          Toolkit.getDefaultToolkit().beep()
        }
        val frame = JFrame("@title@")
        runCatching {
          val windowListener = get()
          if (windowListener != null) {
            frame.addWindowListener(windowListener)
          }
        }.onFailure {
          if (it is InterruptedException) {
            Thread.currentThread().interrupt()
          }
          it.printStackTrace()
          Toolkit.getDefaultToolkit().beep()
        }
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.contentPane.add(makeUI())
        frame.size = windowState.size
        frame.location = windowState.location
        frame.isVisible = true
      }
    }
    worker.execute()
  }
}

class WindowState : Serializable {
  var location = Point()
  var size = Dimension(320, 240)

  companion object {
    private const val serialVersionUID = 1415435143L
  }
}
