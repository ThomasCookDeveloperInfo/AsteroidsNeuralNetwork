package GUI

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

const val SQUARE_GRID_SIZE = 100
const val WIDTH = 1000.0
const val HEIGHT = 600.0
private const val TITLE = "Asteroids"
private const val ROOT_NAME = "sample.fxml"

class Asteroids : Application() {

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        val root = FXMLLoader.load<Parent>(javaClass.getResource(ROOT_NAME))
        primaryStage.title = TITLE
        primaryStage.scene = Scene(root, WIDTH, HEIGHT)
        primaryStage.show()
    }

    object Entry{
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Asteroids::class.java)
        }
    }
}